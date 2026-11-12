const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { pool } = require('../config/db');
const { JWT_SECRET, JWT_EXPIRES_IN } = require('../config/env');
const { authenticate } = require('../middleware/auth');

const router = express.Router();
const ROLES_VALIDOS = new Set(['INSTRUCTOR', 'APRENDIZ']);

function signToken(user) {
  return jwt.sign(
    { sub: user.id, rol: user.rol, email: user.email },
    JWT_SECRET,
    { expiresIn: JWT_EXPIRES_IN }
  );
}

router.post('/register', async (req, res, next) => {
  try {
    const { nombre, email, password, rol } = req.body;

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

router.post('/login', async (req, res, next) => {
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

router.get('/me', authenticate, async (req, res) => {
  return res.json(req.user);
});

module.exports = router;
