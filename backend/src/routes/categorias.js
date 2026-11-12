const express = require('express');
const { pool } = require('../config/db');

const router = express.Router();

router.get('/', async (_req, res, next) => {
  try {
    const [rows] = await pool.query(
      'SELECT id, nombre, icono FROM categorias ORDER BY id ASC'
    );
    res.json(rows);
  } catch (err) {
    next(err);
  }
});

router.post('/', async (req, res, next) => {
  try {
    const { nombre, icono } = req.body;
    if (!nombre || typeof nombre !== 'string' || nombre.trim().length < 2) {
      return res.status(400).json({ error: 'El nombre de la categoría es obligatorio (mín. 2 caracteres).' });
    }
    const cleanNombre = nombre.trim();
    const cleanIcono = (icono && typeof icono === 'string' && icono.trim()) || '📋';

    const [result] = await pool.query(
      'INSERT INTO categorias (nombre, icono) VALUES (?, ?)',
      [cleanNombre, cleanIcono]
    );
    res.status(201).json({
      id: result.insertId,
      nombre: cleanNombre,
      icono: cleanIcono,
    });
  } catch (err) {
    if (err.code === 'ER_DUP_ENTRY') {
      return res.status(409).json({ error: 'Ya existe una categoría con ese nombre.' });
    }
    next(err);
  }
});

module.exports = router;
