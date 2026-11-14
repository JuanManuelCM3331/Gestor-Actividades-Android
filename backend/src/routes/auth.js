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

const router = express.Router();
const ROLES_VALIDOS = new Set(['INSTRUCTOR', 'APRENDIZ']);

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

module.exports = router;
