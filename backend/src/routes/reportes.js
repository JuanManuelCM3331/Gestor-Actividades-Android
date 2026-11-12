const express = require('express');
const { pool } = require('../config/db');

const router = express.Router();

function mapReporteRow(row) {
  return {
    id: row.id,
    titulo: row.titulo,
    descripcion: row.descripcion ?? '',
    fecha: Number(row.fecha),
    categoria_id: row.categoria_id,
    categoria_nombre: row.categoria_nombre ?? null,
    categoria_icono: row.categoria_icono ?? null,
    resuelto: Boolean(row.resuelto),
    evidencia_foto_uri: row.evidencia_foto_uri ?? null,
    evidencia_archivo_uri: row.evidencia_archivo_uri ?? null,
  };
}

async function syncReporteEvidencias(connection, reporteId, fotoUri, archivoUri) {
  await connection.query('DELETE FROM evidencias WHERE reporte_id = ?', [reporteId]);
  if (fotoUri) {
    await connection.query(
      "INSERT INTO evidencias (reporte_id, tipo, uri) VALUES (?, 'FOTO', ?)",
      [reporteId, fotoUri]
    );
  }
  if (archivoUri) {
    await connection.query(
      "INSERT INTO evidencias (reporte_id, tipo, uri) VALUES (?, 'ARCHIVO', ?)",
      [reporteId, archivoUri]
    );
  }
}

router.get('/', async (req, res, next) => {
  try {
    const categoriaId = req.query.categoria_id ? Number(req.query.categoria_id) : null;
    const baseSql = `
      SELECT r.id, r.titulo, r.descripcion, r.fecha, r.categoria_id,
             r.resuelto, r.evidencia_foto_uri, r.evidencia_archivo_uri,
             c.nombre AS categoria_nombre, c.icono AS categoria_icono
      FROM reportes r
      INNER JOIN categorias c ON r.categoria_id = c.id
    `;
    const [rows] = categoriaId
      ? await pool.query(`${baseSql} WHERE r.categoria_id = ? ORDER BY r.fecha DESC`, [categoriaId])
      : await pool.query(`${baseSql} ORDER BY r.fecha DESC`);

    res.json(rows.map(mapReporteRow));
  } catch (err) {
    next(err);
  }
});

router.get('/:id', async (req, res, next) => {
  try {
    const id = Number(req.params.id);
    if (!Number.isInteger(id) || id <= 0) {
      return res.status(400).json({ error: 'ID de reporte inválido.' });
    }
    const [rows] = await pool.query(
      `SELECT r.id, r.titulo, r.descripcion, r.fecha, r.categoria_id,
              r.resuelto, r.evidencia_foto_uri, r.evidencia_archivo_uri,
              c.nombre AS categoria_nombre, c.icono AS categoria_icono
       FROM reportes r
       INNER JOIN categorias c ON r.categoria_id = c.id
       WHERE r.id = ?`,
      [id]
    );
    if (rows.length === 0) {
      return res.status(404).json({ error: 'Reporte no encontrado.' });
    }
    res.json(mapReporteRow(rows[0]));
  } catch (err) {
    next(err);
  }
});

router.post('/', async (req, res, next) => {
  const connection = await pool.getConnection();
  try {
    const {
      titulo,
      descripcion = '',
      fecha,
      categoria_id = 1,
      resuelto = false,
      evidencia_foto_uri = null,
      evidencia_archivo_uri = null,
    } = req.body;

    if (!titulo || typeof titulo !== 'string' || titulo.trim().length < 3 || titulo.trim().length > 80) {
      return res.status(400).json({ error: 'El título es obligatorio (entre 3 y 80 caracteres).' });
    }
    if (typeof descripcion === 'string' && descripcion.length > 240) {
      return res.status(400).json({ error: 'La descripción no puede superar los 240 caracteres.' });
    }

    const fechaNum = Number(fecha) || Date.now();
    const categoriaIdNum = Number(categoria_id) || 1;
    const resueltoFlag = resuelto ? 1 : 0;

    await connection.beginTransaction();

    const [catRows] = await connection.query('SELECT id FROM categorias WHERE id = ?', [categoriaIdNum]);
    if (catRows.length === 0) {
      await connection.rollback();
      return res.status(400).json({ error: `La categoría ${categoriaIdNum} no existe.` });
    }

    const [result] = await connection.query(
      `INSERT INTO reportes (titulo, descripcion, fecha, categoria_id, resuelto, evidencia_foto_uri, evidencia_archivo_uri)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
      [titulo.trim(), descripcion.trim(), fechaNum, categoriaIdNum, resueltoFlag, evidencia_foto_uri, evidencia_archivo_uri]
    );

    const newId = result.insertId;
    await syncReporteEvidencias(connection, newId, evidencia_foto_uri, evidencia_archivo_uri);
    await connection.commit();

    const [created] = await pool.query(
      `SELECT r.id, r.titulo, r.descripcion, r.fecha, r.categoria_id,
              r.resuelto, r.evidencia_foto_uri, r.evidencia_archivo_uri,
              c.nombre AS categoria_nombre, c.icono AS categoria_icono
       FROM reportes r
       INNER JOIN categorias c ON r.categoria_id = c.id
       WHERE r.id = ?`,
      [newId]
    );
    res.status(201).json(mapReporteRow(created[0]));
  } catch (err) {
    await connection.rollback();
    next(err);
  } finally {
    connection.release();
  }
});

router.put('/:id', async (req, res, next) => {
  const connection = await pool.getConnection();
  try {
    const id = Number(req.params.id);
    if (!Number.isInteger(id) || id <= 0) {
      return res.status(400).json({ error: 'ID de reporte inválido.' });
    }

    const {
      titulo,
      descripcion = '',
      fecha,
      categoria_id = 1,
      resuelto = false,
      evidencia_foto_uri = null,
      evidencia_archivo_uri = null,
    } = req.body;

    if (!titulo || typeof titulo !== 'string' || titulo.trim().length < 3 || titulo.trim().length > 80) {
      return res.status(400).json({ error: 'El título es obligatorio (entre 3 y 80 caracteres).' });
    }

    const fechaNum = Number(fecha) || Date.now();
    const categoriaIdNum = Number(categoria_id) || 1;
    const resueltoFlag = resuelto ? 1 : 0;

    await connection.beginTransaction();

    const [result] = await connection.query(
      `UPDATE reportes
       SET titulo = ?, descripcion = ?, fecha = ?, categoria_id = ?, resuelto = ?,
           evidencia_foto_uri = ?, evidencia_archivo_uri = ?
       WHERE id = ?`,
      [titulo.trim(), descripcion.trim(), fechaNum, categoriaIdNum, resueltoFlag, evidencia_foto_uri, evidencia_archivo_uri, id]
    );

    if (result.affectedRows === 0) {
      await connection.rollback();
      return res.status(404).json({ error: 'Reporte no encontrado.' });
    }

    await syncReporteEvidencias(connection, id, evidencia_foto_uri, evidencia_archivo_uri);
    await connection.commit();

    const [updated] = await pool.query(
      `SELECT r.id, r.titulo, r.descripcion, r.fecha, r.categoria_id,
              r.resuelto, r.evidencia_foto_uri, r.evidencia_archivo_uri,
              c.nombre AS categoria_nombre, c.icono AS categoria_icono
       FROM reportes r
       INNER JOIN categorias c ON r.categoria_id = c.id
       WHERE r.id = ?`,
      [id]
    );
    res.json(mapReporteRow(updated[0]));
  } catch (err) {
    await connection.rollback();
    next(err);
  } finally {
    connection.release();
  }
});

router.delete('/:id', async (req, res, next) => {
  try {
    const id = Number(req.params.id);
    if (!Number.isInteger(id) || id <= 0) {
      return res.status(400).json({ error: 'ID de reporte inválido.' });
    }
    const [result] = await pool.query('DELETE FROM reportes WHERE id = ?', [id]);
    if (result.affectedRows === 0) {
      return res.status(404).json({ error: 'Reporte no encontrado.' });
    }
    res.status(204).send();
  } catch (err) {
    next(err);
  }
});

module.exports = router;
