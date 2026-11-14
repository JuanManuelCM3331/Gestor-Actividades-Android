const express = require('express');
const fs = require('fs');
const path = require('path');
const { pool } = require('../config/db');
const { authenticate, authorizeCourseAccess } = require('../middleware/auth');
const { UPLOAD_ROOT } = require('../utils/filePolicy');

const router = express.Router();
router.use(authenticate);

function sendSafeFile(res, subfolder, nombreArchivo, nombreOriginal, mimeType) {
  const safeBase = path.basename(nombreArchivo);
  const fullPath = path.join(UPLOAD_ROOT, subfolder, safeBase);
  if (!fs.existsSync(fullPath)) {
    return res.status(404).json({ error: 'El archivo no existe en el servidor.' });
  }
  res.setHeader('Content-Type', mimeType || 'application/octet-stream');
  return res.download(fullPath, nombreOriginal || safeBase);
}

router.get('/:tipo/:id', async (req, res, next) => {
  try {
    const tipo = req.params.tipo;
    const id = Number(req.params.id);
    if (!Number.isInteger(id) || id <= 0) {
      return res.status(400).json({ error: 'Identificador de archivo inválido.' });
    }

    if (tipo === 'adjunto') {
      const [rows] = await pool.query(
        `SELECT aa.*, a.curso_id FROM actividad_adjuntos aa
         INNER JOIN actividades a ON a.id = aa.actividad_id
         WHERE aa.id = ?`,
        [id]
      );
      if (rows.length === 0) return res.status(404).json({ error: 'Adjunto no encontrado.' });
      const access = await authorizeCourseAccess(rows[0].curso_id, req.user);
      if (access.error) return res.status(access.status).json({ error: access.error });
      return sendSafeFile(res, 'actividades', rows[0].nombre_archivo, rows[0].nombre_original, rows[0].mime_type);
    }

    if (tipo === 'material') {
      const [rows] = await pool.query('SELECT * FROM material_apoyo WHERE id = ?', [id]);
      if (rows.length === 0 || rows[0].tipo !== 'ARCHIVO') {
        return res.status(404).json({ error: 'Material no encontrado.' });
      }
      const access = await authorizeCourseAccess(rows[0].curso_id, req.user);
      if (access.error) return res.status(access.status).json({ error: access.error });
      return sendSafeFile(res, 'materiales', rows[0].nombre_archivo, rows[0].nombre_original, rows[0].mime_type);
    }

    if (tipo === 'entrega') {
      const [rows] = await pool.query(
        `SELECT ea.*, e.aprendiz_id, a.curso_id
         FROM entrega_archivos ea
         INNER JOIN entregas e ON e.id = ea.entrega_id
         INNER JOIN actividades a ON a.id = e.actividad_id
         WHERE ea.id = ?`,
        [id]
      );
      if (rows.length === 0) return res.status(404).json({ error: 'Evidencia no encontrada.' });
      const access = await authorizeCourseAccess(rows[0].curso_id, req.user);
      if (access.error) return res.status(access.status).json({ error: access.error });
      if (!access.isOwner && rows[0].aprendiz_id !== req.user.id) {
        return res.status(403).json({ error: 'No tienes permiso para descargar evidencias de otro aprendiz.' });
      }
      return sendSafeFile(res, 'entregas', rows[0].nombre_archivo, rows[0].nombre_original, rows[0].mime_type);
    }

    if (tipo === 'foro') {
      const [rows] = await pool.query(
        `SELECT fa.*, h.curso_id, h.tipo AS hilo_tipo, h.aprendiz_objetivo_id
         FROM foro_archivos fa
         INNER JOIN foro_mensajes m ON m.id = fa.mensaje_id
         INNER JOIN foro_hilos h ON h.id = m.hilo_id
         WHERE fa.id = ?`,
        [id]
      );
      if (rows.length === 0) return res.status(404).json({ error: 'Archivo del foro no encontrado.' });
      const row = rows[0];
      const access = await authorizeCourseAccess(row.curso_id, req.user);
      if (access.error) return res.status(access.status).json({ error: access.error });
      if (
        row.hilo_tipo === 'PRIVADO' &&
        req.user.id !== access.curso.instructor_id &&
        req.user.id !== row.aprendiz_objetivo_id
      ) {
        return res.status(403).json({ error: 'No tienes permiso para acceder a archivos de este chat privado.' });
      }
      return sendSafeFile(res, 'foro', row.nombre_archivo, row.nombre_original, row.mime_type);
    }

    return res.status(400).json({ error: 'Tipo de archivo inválido.' });
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
