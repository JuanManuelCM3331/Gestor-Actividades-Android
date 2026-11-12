const express = require('express');
const { pool } = require('../config/db');

const router = express.Router();

const PRIORIDADES_VALIDAS = new Set(['ALTA', 'MEDIA', 'BAJA']);

function mapActividadRow(row) {
  return {
    id: row.id,
    titulo: row.titulo,
    descripcion: row.descripcion ?? '',
    fecha_entrega: Number(row.fecha_entrega),
    progreso: Number(row.progreso),
    prioridad: row.prioridad || 'MEDIA',
    resuelto: Boolean(row.resuelto),
    categoria_id: row.categoria_id ?? null,
    evidencia_foto_uri: row.evidencia_foto_uri ?? null,
    evidencia_archivo_uri: row.evidencia_archivo_uri ?? null,
  };
}

router.get('/', async (_req, res, next) => {
  try {
    const [rows] = await pool.query(
      `SELECT id, titulo, descripcion, progreso, fecha_entrega, prioridad, resuelto,
              categoria_id, evidencia_foto_uri, evidencia_archivo_uri
       FROM actividades
       ORDER BY fecha_entrega ASC`
    );
    res.json(rows.map(mapActividadRow));
  } catch (err) {
    next(err);
  }
});

router.post('/', async (req, res, next) => {
  try {
    const {
      titulo,
      descripcion = '',
      progreso = 0,
      fecha_entrega,
      prioridad = 'MEDIA',
      resuelto,
      categoria_id = null,
      evidencia_foto_uri = null,
      evidencia_archivo_uri = null,
    } = req.body;

    if (!titulo || typeof titulo !== 'string' || titulo.trim().length < 3) {
      return res.status(400).json({ error: 'El título es obligatorio (mín. 3 caracteres).' });
    }

    const progNum = Math.max(0, Math.min(100, Number(progreso) || 0));
    const prio = PRIORIDADES_VALIDAS.has(prioridad) ? prioridad : 'MEDIA';
    const fechaNum = Number(fecha_entrega) || Date.now();
    const isResuelto = resuelto !== undefined ? Boolean(resuelto) : progNum === 100;

    const [result] = await connectionQuery(
      `INSERT INTO actividades (titulo, descripcion, progreso, fecha_entrega, prioridad, resuelto, categoria_id, evidencia_foto_uri, evidencia_archivo_uri)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [titulo.trim(), descripcion.trim(), progNum, fechaNum, prio, isResuelto ? 1 : 0, categoria_id, evidencia_foto_uri, evidencia_archivo_uri]
    );

    const [created] = await pool.query('SELECT * FROM actividades WHERE id = ?', [result.insertId]);
    res.status(201).json(mapActividadRow(created[0]));
  } catch (err) {
    next(err);
  }
});

async function connectionQuery(sql, params) {
  return pool.query(sql, params);
}

router.put('/:id', async (req, res, next) => {
  try {
    const id = Number(req.params.id);
    if (!Number.isInteger(id) || id <= 0) {
      return res.status(400).json({ error: 'ID de actividad inválido.' });
    }
    const {
      titulo,
      descripcion = '',
      progreso = 0,
      fecha_entrega,
      prioridad = 'MEDIA',
      resuelto,
      categoria_id = null,
      evidencia_foto_uri = null,
      evidencia_archivo_uri = null,
    } = req.body;

    const progNum = Math.max(0, Math.min(100, Number(progreso) || 0));
    const prio = PRIORIDADES_VALIDAS.has(prioridad) ? prioridad : 'MEDIA';
    const fechaNum = Number(fecha_entrega) || Date.now();
    const isResuelto = resuelto !== undefined ? Boolean(resuelto) : progNum === 100;

    const [result] = await pool.query(
      `UPDATE actividades
       SET titulo = ?, descripcion = ?, progreso = ?, fecha_entrega = ?, prioridad = ?,
           resuelto = ?, categoria_id = ?, evidencia_foto_uri = ?, evidencia_archivo_uri = ?
       WHERE id = ?`,
      [titulo.trim(), descripcion.trim(), progNum, fechaNum, prio, isResuelto ? 1 : 0, categoria_id, evidencia_foto_uri, evidencia_archivo_uri, id]
    );

    if (result.affectedRows === 0) {
      return res.status(404).json({ error: 'Actividad no encontrada.' });
    }

    const [updated] = await pool.query('SELECT * FROM actividades WHERE id = ?', [id]);
    res.json(mapActividadRow(updated[0]));
  } catch (err) {
    next(err);
  }
});

router.delete('/:id', async (req, res, next) => {
  try {
    const id = Number(req.params.id);
    if (!Number.isInteger(id) || id <= 0) {
      return res.status(400).json({ error: 'ID de actividad inválido.' });
    }
    const [result] = await pool.query('DELETE FROM actividades WHERE id = ?', [id]);
    if (result.affectedRows === 0) {
      return res.status(404).json({ error: 'Actividad no encontrada.' });
    }
    res.status(204).send();
  } catch (err) {
    next(err);
  }
});

module.exports = router;
