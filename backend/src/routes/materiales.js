const express = require('express');
const { pool } = require('../config/db');
const { authenticate, authorizeCourseAccess } = require('../middleware/auth');
const { createMulterUpload, safeUnlink, cleanupUploadedFiles } = require('../utils/filePolicy');

const router = express.Router();
router.use(authenticate);

const uploadMaterial = createMulterUpload('materiales');

function mapMaterial(row) {
  return {
    id: row.id,
    curso_id: row.curso_id,
    titulo: row.titulo,
    descripcion: row.descripcion,
    tipo: row.tipo,
    enlace_url: row.enlace_url,
    nombre_original: row.nombre_original,
    mime_type: row.mime_type,
    tamano_bytes: row.tamano_bytes !== null ? Number(row.tamano_bytes) : null,
    url_descarga: row.tipo === 'ARCHIVO' ? `/api/archivos/material/${row.id}` : row.enlace_url,
    created_at: Number(row.created_at),
  };
}

router.get('/curso/:cursoId', async (req, res, next) => {
  try {
    const access = await authorizeCourseAccess(req.params.cursoId, req.user);
    if (access.error) return res.status(access.status).json({ error: access.error });

    const [rows] = await pool.query(
      'SELECT * FROM material_apoyo WHERE curso_id = ? ORDER BY created_at DESC',
      [access.curso.id]
    );
    return res.json(rows.map(mapMaterial));
  } catch (err) {
    return next(err);
  }
});

router.post('/curso/:cursoId', uploadMaterial.single('archivo'), async (req, res, next) => {
  try {
    const access = await authorizeCourseAccess(req.params.cursoId, req.user, { requireInstructorOwner: true });
    if (access.error) {
      if (req.file) cleanupUploadedFiles('materiales', [req.file]);
      return res.status(access.status).json({ error: access.error });
    }

    const { titulo, descripcion = '', tipo, enlace_url = null } = req.body;
    if (!titulo || typeof titulo !== 'string' || titulo.trim().length < 3 || titulo.trim().length > 140) {
      if (req.file) cleanupUploadedFiles('materiales', [req.file]);
      return res.status(400).json({ error: 'El título del material es obligatorio (entre 3 y 140 caracteres).' });
    }

    const cleanTipo = req.file ? 'ARCHIVO' : (tipo === 'ENLACE' ? 'ENLACE' : 'ARCHIVO');
    const now = Date.now();

    if (cleanTipo === 'ENLACE') {
      const cleanUrl = String(enlace_url || '').trim();
      if (!/^https?:\/\/.+/i.test(cleanUrl)) {
        return res.status(400).json({ error: 'Ingresa un enlace HTTP o HTTPS válido.' });
      }
      const [result] = await pool.query(
        `INSERT INTO material_apoyo (curso_id, titulo, descripcion, tipo, enlace_url, created_at)
         VALUES (?, ?, ?, 'ENLACE', ?, ?)`,
        [access.curso.id, titulo.trim(), String(descripcion).trim(), cleanUrl, now]
      );
      const [created] = await pool.query('SELECT * FROM material_apoyo WHERE id = ?', [result.insertId]);
      return res.status(201).json(mapMaterial(created[0]));
    }

    if (!req.file) {
      return res.status(400).json({ error: 'Debes adjuntar un archivo o especificar un enlace válido.' });
    }

    const [result] = await pool.query(
      `INSERT INTO material_apoyo (curso_id, titulo, descripcion, tipo, nombre_original, nombre_archivo, mime_type, tamano_bytes, created_at)
       VALUES (?, ?, ?, 'ARCHIVO', ?, ?, ?, ?, ?)`,
      [
        access.curso.id,
        titulo.trim(),
        String(descripcion).trim(),
        req.file.originalname,
        req.file.filename,
        req.file.mimetype,
        req.file.size,
        now,
      ]
    );
    const [created] = await pool.query('SELECT * FROM material_apoyo WHERE id = ?', [result.insertId]);
    return res.status(201).json(mapMaterial(created[0]));
  } catch (err) {
    if (req.file) cleanupUploadedFiles('materiales', [req.file]);
    return next(err);
  }
});

router.delete('/:materialId', async (req, res, next) => {
  try {
    const id = Number(req.params.materialId);
    const [rows] = await pool.query('SELECT * FROM material_apoyo WHERE id = ?', [id]);
    if (rows.length === 0) return res.status(404).json({ error: 'Material no encontrado.' });

    const material = rows[0];
    const access = await authorizeCourseAccess(material.curso_id, req.user, { requireInstructorOwner: true });
    if (access.error) return res.status(access.status).json({ error: access.error });

    await pool.query('DELETE FROM material_apoyo WHERE id = ?', [id]);
    if (material.tipo === 'ARCHIVO' && material.nombre_archivo) {
      safeUnlink('materiales', material.nombre_archivo);
    }
    return res.status(204).send();
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
