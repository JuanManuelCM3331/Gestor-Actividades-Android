const express = require('express');
const crypto = require('crypto');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { pool } = require('../config/db');
const {
  JWT_SECRET,
  JWT_EXPIRES_IN,
  INSTRUCTOR_INVITE_CODE,
  ALLOWED_INSTRUCTOR_DOMAINS,
} = require('../config/env');
const { authenticate, revokeToken } = require('../middleware/auth');
const { createRateLimiter } = require('../middleware/rateLimiter');
const { safeUnlink } = require('../utils/filePolicy');

const router = express.Router();
const ROLES_VALIDOS = new Set(['INSTRUCTOR', 'APRENDIZ']);

// Almacén local en memoria de códigos OTP de recuperación (sin requerir servicios SMTP externos)
const resetOtpStore = new Map();
const OTP_TTL_MS = 10 * 60 * 1000;

// SEC-03: Rate limiting específico contra fuerza bruta en autenticación
const authLimiter = createRateLimiter({
  windowMs: 15 * 60 * 1000,
  max: 25,
  message: 'Demasiados intentos de autenticación. Espera unos minutos antes de volver a intentar.',
  keyGenerator: (req) => `${req.ip || 'ip'}:${String(req.body?.email || '').trim().toLowerCase()}`,
});

function signToken(user) {
  return jwt.sign(
    { sub: user.id, rol: user.rol, email: user.email, jti: crypto.randomUUID() },
    JWT_SECRET,
    { expiresIn: JWT_EXPIRES_IN }
  );
}

/**
 * SEC-02: Verifica que quien solicita registrarse como INSTRUCTOR posea un correo
 * con dominio institucional autorizado (@sena.edu.co, @ctma.edu.co, etc.) o presente
 * el código de invitación docente válido.
 */
function esInstructorAutorizado(cleanEmail, codigoInstructor) {
  if (codigoInstructor && String(codigoInstructor).trim() === INSTRUCTOR_INVITE_CODE) {
    return true;
  }
  const domain = cleanEmail.split('@')[1] || '';
  return ALLOWED_INSTRUCTOR_DOMAINS.some((allowed) => domain === allowed || domain.endsWith(`.${allowed}`));
}

router.post('/register', authLimiter, async (req, res, next) => {
  try {
    const { nombre, email, password, rol, codigo_instructor } = req.body;

    if (!nombre || typeof nombre !== 'string' || nombre.trim().length < 3) {
      return res.status(400).json({ error: 'El nombre es obligatorio (mínimo 3 caracteres).' });
    }
    if (!email || typeof email !== 'string' || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      return res.status(400).json({ error: 'Ingresa un correo electrónico válido.' });
    }
    if (!password || typeof password !== 'string' || password.length < 6) {
      return res.status(400).json({ error: 'La contraseña debe tener al menos 6 caracteres.' });
    }
    const cleanRol = typeof rol === 'string' ? rol.trim().toUpperCase() : '';
    if (!ROLES_VALIDOS.has(cleanRol)) {
      return res.status(400).json({ error: 'El rol debe ser INSTRUCTOR o APRENDIZ.' });
    }

    const cleanNombre = nombre.trim();
    const cleanEmail = email.trim().toLowerCase();

    // SEC-02: Impedir auto-asignación libre de rol INSTRUCTOR desde correos no institucionales sin código
    if (cleanRol === 'INSTRUCTOR' && !esInstructorAutorizado(cleanEmail, codigo_instructor)) {
      return res.status(403).json({
        error: 'Para registrarte como INSTRUCTOR debes usar un correo institucional autorizado (@sena.edu.co / @ctma.edu.co) o código docente.',
      });
    }

    const passwordHash = await bcrypt.hash(password, 10);

    const [result] = await pool.query(
      'INSERT INTO usuarios (nombre, email, password_hash, rol) VALUES (?, ?, ?, ?)',
      [cleanNombre, cleanEmail, passwordHash, cleanRol]
    );

    const usuario = {
      id: result.insertId,
      nombre: cleanNombre,
      email: cleanEmail,
      rol: cleanRol,
    };
    const token = signToken(usuario);
    return res.status(201).json({ token, usuario });
  } catch (err) {
    if (err.code === 'ER_DUP_ENTRY') {
      return res.status(409).json({ error: 'Ya existe una cuenta registrada con ese correo.' });
    }
    return next(err);
  }
});

router.post('/login', authLimiter, async (req, res, next) => {
  try {
    const { email, password } = req.body;
    if (!email || !password) {
      return res.status(400).json({ error: 'Correo y contraseña obligatorios.' });
    }

    const cleanEmail = String(email).trim().toLowerCase();
    const [rows] = await pool.query(
      'SELECT id, nombre, email, password_hash, rol FROM usuarios WHERE email = ?',
      [cleanEmail]
    );

    if (rows.length === 0) {
      return res.status(401).json({ error: 'Credenciales incorrectas.' });
    }

    const userRow = rows[0];
    const match = await bcrypt.compare(String(password), userRow.password_hash);
    if (!match) {
      return res.status(401).json({ error: 'Credenciales incorrectas.' });
    }

    const usuario = {
      id: userRow.id,
      nombre: userRow.nombre,
      email: userRow.email,
      rol: userRow.rol,
    };
    const token = signToken(usuario);
    return res.json({ token, usuario });
  } catch (err) {
    return next(err);
  }
});

// SEC-05: Revocación explícita del token JWT al cerrar sesión
router.post('/logout', authenticate, async (req, res) => {
  const key = req.tokenPayload?.jti || req.rawToken;
  revokeToken(key, req.tokenPayload?.exp);
  return res.status(204).send();
});

router.get('/me', authenticate, async (req, res) => {
  return res.json(req.user);
});

// Editar nombre de perfil del usuario autenticado
router.put('/perfil', authenticate, async (req, res, next) => {
  try {
    const nombre = String(req.body?.nombre || '').trim();
    if (nombre.length < 3 || nombre.length > 120) {
      return res.status(400).json({ error: 'El nombre debe tener entre 3 y 120 caracteres.' });
    }
    await pool.query('UPDATE usuarios SET nombre = ? WHERE id = ?', [nombre, req.user.id]);
    const actualizado = {
      id: req.user.id,
      nombre,
      email: req.user.email,
      rol: req.user.rol,
    };
    return res.json(actualizado);
  } catch (err) {
    return next(err);
  }
});

// Cambiar contraseña estando autenticado
router.put('/password', authenticate, async (req, res, next) => {
  try {
    const passwordActual = String(req.body?.password_actual || '');
    const passwordNueva = String(req.body?.password_nueva || '');
    if (!passwordActual) {
      return res.status(400).json({ error: 'Ingresa tu contraseña actual.' });
    }
    if (passwordNueva.length < 6) {
      return res.status(400).json({ error: 'La nueva contraseña debe tener mínimo 6 caracteres.' });
    }

    const [rows] = await pool.query('SELECT password_hash FROM usuarios WHERE id = ?', [req.user.id]);
    if (rows.length === 0) {
      return res.status(404).json({ error: 'Usuario no encontrado.' });
    }
    const ok = await bcrypt.compare(passwordActual, rows[0].password_hash);
    if (!ok) {
      return res.status(401).json({ error: 'La contraseña actual no es correcta.' });
    }

    const nuevoHash = await bcrypt.hash(passwordNueva, 10);
    await pool.query('UPDATE usuarios SET password_hash = ? WHERE id = ?', [nuevoHash, req.user.id]);
    return res.json({ mensaje: 'Contraseña actualizada correctamente.' });
  } catch (err) {
    return next(err);
  }
});

// Solicitar código OTP local de 6 dígitos para recuperar contraseña (sin servicios externos)
router.post('/recuperar/solicitar', authLimiter, async (req, res, next) => {
  try {
    const cleanEmail = String(req.body?.email || '').trim().toLowerCase();
    if (!cleanEmail || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(cleanEmail)) {
      return res.status(400).json({ error: 'Ingresa un correo electrónico válido.' });
    }

    const [rows] = await pool.query('SELECT id, nombre FROM usuarios WHERE email = ?', [cleanEmail]);
    if (rows.length === 0) {
      return res.status(404).json({ error: 'No existe ninguna cuenta asociada a ese correo.' });
    }

    const codigo = String(crypto.randomInt(100000, 999999));
    resetOtpStore.set(cleanEmail, {
      codigo,
      userId: rows[0].id,
      expiresAt: Date.now() + OTP_TTL_MS,
      attempts: 0,
    });

    return res.json({
      mensaje: 'Código de recuperación generado (válido por 10 minutos).',
      codigo_otp: codigo,
    });
  } catch (err) {
    return next(err);
  }
});

// Confirmar código OTP y establecer nueva contraseña
router.post('/recuperar/confirmar', authLimiter, async (req, res, next) => {
  try {
    const cleanEmail = String(req.body?.email || '').trim().toLowerCase();
    const codigo = String(req.body?.codigo || '').trim();
    const passwordNueva = String(req.body?.password_nueva || '');

    if (!cleanEmail || !codigo || passwordNueva.length < 6) {
      return res.status(400).json({
        error: 'Ingresa el correo, el código de 6 dígitos y una nueva contraseña de al menos 6 caracteres.',
      });
    }

    const entry = resetOtpStore.get(cleanEmail);
    if (!entry || Date.now() > entry.expiresAt) {
      resetOtpStore.delete(cleanEmail);
      return res.status(400).json({ error: 'El código de recuperación expiró o no existe. Solicita uno nuevo.' });
    }

    entry.attempts += 1;
    if (entry.attempts > 5) {
      resetOtpStore.delete(cleanEmail);
      return res.status(429).json({ error: 'Demasiados intentos fallidos. Solicita un nuevo código.' });
    }

    if (entry.codigo !== codigo) {
      return res.status(400).json({ error: 'El código de verificación es incorrecto.' });
    }

    const nuevoHash = await bcrypt.hash(passwordNueva, 10);
    await pool.query('UPDATE usuarios SET password_hash = ? WHERE id = ?', [nuevoHash, entry.userId]);
    resetOtpStore.delete(cleanEmail);

    return res.json({ mensaje: 'Contraseña restablecida correctamente. Ya puedes iniciar sesión.' });
  } catch (err) {
    return next(err);
  }
});

// Función compartida para eliminar cuenta y limpiar archivos físicos asociados
async function handleEliminarCuenta(req, res, next) {
  try {
    const password = String(req.body?.password || req.query?.password || '');
    if (!password) {
      return res.status(400).json({ error: 'Debes confirmar tu contraseña para eliminar la cuenta.' });
    }

    const [rows] = await pool.query('SELECT password_hash FROM usuarios WHERE id = ?', [req.user.id]);
    if (rows.length === 0) {
      return res.status(404).json({ error: 'Usuario no encontrado.' });
    }
    const ok = await bcrypt.compare(password, rows[0].password_hash);
    if (!ok) {
      return res.status(401).json({ error: 'Contraseña incorrecta. No se eliminó la cuenta.' });
    }

    // Recolectar archivos físicos asociados antes del borrado en cascada
    const filesToRemove = [];
    if (req.user.rol === 'INSTRUCTOR') {
      const [adj] = await pool.query(
        `SELECT aa.nombre_archivo FROM actividad_adjuntos aa
         INNER JOIN actividades a ON a.id = aa.actividad_id
         INNER JOIN cursos c ON c.id = a.curso_id
         WHERE c.instructor_id = ?`,
        [req.user.id]
      );
      adj.forEach((f) => filesToRemove.push(['actividades', f.nombre_archivo]));

      const [ent] = await pool.query(
        `SELECT ea.nombre_archivo FROM entrega_archivos ea
         INNER JOIN entregas e ON e.id = ea.entrega_id
         INNER JOIN actividades a ON a.id = e.actividad_id
         INNER JOIN cursos c ON c.id = a.curso_id
         WHERE c.instructor_id = ?`,
        [req.user.id]
      );
      ent.forEach((f) => filesToRemove.push(['entregas', f.nombre_archivo]));

      const [mat] = await pool.query(
        `SELECT m.nombre_archivo FROM material_apoyo m
         INNER JOIN cursos c ON c.id = m.curso_id
         WHERE c.instructor_id = ? AND m.tipo = 'ARCHIVO'`,
        [req.user.id]
      );
      mat.forEach((f) => filesToRemove.push(['materiales', f.nombre_archivo]));
    } else {
      const [ent] = await pool.query(
        `SELECT ea.nombre_archivo FROM entrega_archivos ea
         INNER JOIN entregas e ON e.id = ea.entrega_id
         WHERE e.aprendiz_id = ?`,
        [req.user.id]
      );
      ent.forEach((f) => filesToRemove.push(['entregas', f.nombre_archivo]));
    }

    await pool.query('DELETE FROM usuarios WHERE id = ?', [req.user.id]);
    filesToRemove.forEach(([folder, name]) => safeUnlink(folder, name));

    const key = req.tokenPayload?.jti || req.rawToken;
    revokeToken(key, req.tokenPayload?.exp);

    return res.status(204).send();
  } catch (err) {
    return next(err);
  }
}

router.delete('/me', authenticate, handleEliminarCuenta);
router.post('/eliminar-cuenta', authenticate, handleEliminarCuenta);

module.exports = router;
