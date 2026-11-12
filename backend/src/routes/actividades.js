const express = require('express');
const { pool } = require('../config/db');
const { authenticate, requireRole, authorizeCourseAccess } = require('../middleware/auth');
const { createMulterUpload, safeUnlink, cleanupUploadedFiles } = require('../utils/filePolicy');

const router = express.Router();
router.use(authenticate);

const uploadAdjuntos = createMulterUpload('actividades');
const uploadEntregas = createMulterUpload('entregas');

function mapAdjunto(row) {
  return {
    id: row.id,
    actividad_id: row.actividad_id,
    nombre_original: row.nombre_original,
    mime_type: row.mime_type,
    tamano_bytes: Number(row.tamano_bytes),
    url_descarga: `/api/archivos/adjunto/${row.id}`,
  };
}

function mapEntregaArchivo(row) {
  return {
    id: row.id,
    entrega_id: row.entrega_id,
    nombre_original: row.nombre_original,
    mime_type: row.mime_type,
    tamano_bytes: Number(row.tamano_bytes),
    url_descarga: `/api/archivos/entrega/${row.id}`,
  };
}

async function getActividadWithCourse(actividadId) {
  const id = Number(actividadId);
  if (!Number.isInteger(id) || id <= 0) return null;
  const [rows] = await pool.query('SELECT * FROM actividades WHERE id = ?', [id]);
  return rows[0] || null;
}

router.get('/curso/:cursoId', async (req, res, next) => {
  try {
    const access = await authorizeCourseAccess(req.params.cursoId, req.user);
    if (access.error) return res.status(access.status).json({ error: access.error });

    const [actividades] = await pool.query(
      'SELECT * FROM actividades WHERE curso_id = ? ORDER BY fecha_limite ASC',
      [access.curso.id]
    );
    if (actividades.length === 0) return res.json([]);

    const actIds = actividades.map((a) => a.id);
    const [adjuntos] = await pool.query(
      'SELECT * FROM actividad_adjuntos WHERE actividad_id IN (?) ORDER BY id ASC',
      [actIds]
    );
    const adjuntosByAct = new Map();
    for (const adj of adjuntos) {
      const list = adjuntosByAct.get(adj.actividad_id) || [];
      list.push(mapAdjunto(adj));
      adjuntosByAct.set(adj.actividad_id, list);
    }

    if (req.user.rol === 'INSTRUCTOR') {
      const [stats] = await pool.query(
        `SELECT actividad_id,
                COUNT(*) AS total_entregas,
                SUM(CASE WHEN estado IN ('ENTREGADA', 'TARDIA') THEN 1 ELSE 0 END) AS por_calificar
         FROM entregas
         WHERE actividad_id IN (?)
         GROUP BY actividad_id`,
        [actIds]
      );
      const statsMap = new Map(stats.map((s) => [s.actividad_id, s]));

      return res.json(actividades.map((a) => {
        const st = statsMap.get(a.id);
        return {
          ...a,
          puntaje_maximo: Number(a.puntaje_maximo),
          adjuntos: adjuntosByAct.get(a.id) || [],
          total_entregas: st ? Number(st.total_entregas) : 0,
          por_calificar: st ? Number(st.por_calificar) : 0,
          mi_entrega: null,
        };
      }));
    }

    const [entregas] = await pool.query(
      'SELECT * FROM entregas WHERE actividad_id IN (?) AND aprendiz_id = ?',
      [actIds, req.user.id]
    );
    const entregaIds = entregas.map((e) => e.id);
    const archivosByEntrega = new Map();
    if (entregaIds.length > 0) {
      const [archivos] = await pool.query(
        'SELECT * FROM entrega_archivos WHERE entrega_id IN (?) ORDER BY id ASC',
        [entregaIds]
      );
      for (const file of archivos) {
        const list = archivosByEntrega.get(file.entrega_id) || [];
        list.push(mapEntregaArchivo(file));
        archivosByEntrega.set(file.entrega_id, list);
      }
    }
    const entregaByAct = new Map(entregas.map((e) => [e.actividad_id, e]));

    return res.json(actividades.map((a) => {
      const e = entregaByAct.get(a.id);
      const estadoEfectivo = e
        ? e.estado
        : (Date.now() > Number(a.fecha_limite) ? 'TARDIA' : 'PENDIENTE');
      return {
        ...a,
        puntaje_maximo: Number(a.puntaje_maximo),
        adjuntos: adjuntosByAct.get(a.id) || [],
        total_entregas: 0,
        por_calificar: 0,
        estado_aprendiz: estadoEfectivo,
        mi_entrega: e ? {
          id: e.id,
          actividad_id: e.actividad_id,
          aprendiz_id: e.aprendiz_id,
          descripcion: e.descripcion,
          nota: e.nota !== null ? Number(e.nota) : null,
          retroalimentacion: e.retroalimentacion,
          exenta: Boolean(e.exenta),
          estado: e.estado,
          fecha_entrega: e.fecha_entrega ? Number(e.fecha_entrega) : null,
          fecha_calificacion: e.fecha_calificacion ? Number(e.fecha_calificacion) : null,
          archivos: archivosByEntrega.get(e.id) || [],
        } : null,
      };
    }));
  } catch (err) {
    return next(err);
  }
});

router.post('/curso/:cursoId', uploadAdjuntos.array('adjuntos', 10), async (req, res, next) => {
  const connection = await pool.getConnection();
  try {
    const access = await authorizeCourseAccess(req.params.cursoId, req.user, { requireInstructorOwner: true });
    if (access.error) {
      cleanupUploadedFiles('actividades', req.files);
      return res.status(access.status).json({ error: access.error });
    }

    const { titulo, descripcion = '', fecha_limite, puntaje_maximo = 5 } = req.body;
    if (!titulo || typeof titulo !== 'string' || titulo.trim().length < 3 || titulo.trim().length > 140) {
      cleanupUploadedFiles('actividades', req.files);
      return res.status(400).json({ error: 'El título es obligatorio (entre 3 y 140 caracteres).' });
    }

    const fechaNum = Number(fecha_limite);
    if (!Number.isFinite(fechaNum) || fechaNum <= 0) {
      cleanupUploadedFiles('actividades', req.files);
      return res.status(400).json({ error: 'La fecha límite es obligatoria y válida.' });
    }

    const puntajeNum = Number(puntaje_maximo);
    if (!Number.isFinite(puntajeNum) || puntajeNum <= 0 || puntajeNum > 1000) {
      cleanupUploadedFiles('actividades', req.files);
      return res.status(400).json({ error: 'El puntaje máximo debe ser mayor a 0.' });
    }

    const now = Date.now();
    await connection.beginTransaction();
    const [result] = await connection.query(
      `INSERT INTO actividades (curso_id, titulo, descripcion, fecha_limite, puntaje_maximo, created_at, updated_at)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
      [access.curso.id, titulo.trim(), String(descripcion).trim(), fechaNum, puntajeNum, now, now]
    );
    const actividadId = result.insertId;

    const adjuntos = [];
    for (const file of req.files || []) {
      const [adjRes] = await connection.query(
        `INSERT INTO actividad_adjuntos (actividad_id, nombre_original, nombre_archivo, mime_type, tamano_bytes, created_at)
         VALUES (?, ?, ?, ?, ?, ?)`,
        [actividadId, file.originalname, file.filename, file.mimetype, file.size, now]
      );
      adjuntos.push({
        id: adjRes.insertId,
        actividad_id: actividadId,
        nombre_original: file.originalname,
        mime_type: file.mimetype,
        tamano_bytes: file.size,
        url_descarga: `/api/archivos/adjunto/${adjRes.insertId}`,
      });
    }

    await connection.commit();
    return res.status(201).json({
      id: actividadId,
      curso_id: access.curso.id,
      titulo: titulo.trim(),
      descripcion: String(descripcion).trim(),
      fecha_limite: fechaNum,
      puntaje_maximo: puntajeNum,
      adjuntos,
      created_at: now,
      updated_at: now,
    });
  } catch (err) {
    await connection.rollback();
    cleanupUploadedFiles('actividades', req.files);
    return next(err);
  } finally {
    connection.release();
  }
});

router.put('/:actividadId', uploadAdjuntos.array('adjuntos', 10), async (req, res, next) => {
  const connection = await pool.getConnection();
  try {
    const actividad = await getActividadWithCourse(req.params.actividadId);
    if (!actividad) {
      cleanupUploadedFiles('actividades', req.files);
      return res.status(404).json({ error: 'Actividad no encontrada.' });
    }
    const access = await authorizeCourseAccess(actividad.curso_id, req.user, { requireInstructorOwner: true });
    if (access.error) {
      cleanupUploadedFiles('actividades', req.files);
      return res.status(access.status).json({ error: access.error });
    }

    const { titulo, descripcion, fecha_limite, puntaje_maximo } = req.body;
    const nextTitulo = titulo !== undefined ? String(titulo).trim() : actividad.titulo;
    if (nextTitulo.length < 3 || nextTitulo.length > 140) {
      cleanupUploadedFiles('actividades', req.files);
      return res.status(400).json({ error: 'El título debe tener entre 3 y 140 caracteres.' });
    }
    const nextDesc = descripcion !== undefined ? String(descripcion).trim() : actividad.descripcion;
    const nextFecha = fecha_limite !== undefined ? Number(fecha_limite) : Number(actividad.fecha_limite);
    const nextPuntaje = puntaje_maximo !== undefined ? Number(puntaje_maximo) : Number(actividad.puntaje_maximo);
    if (!Number.isFinite(nextFecha) || nextFecha <= 0 || !Number.isFinite(nextPuntaje) || nextPuntaje <= 0) {
      cleanupUploadedFiles('actividades', req.files);
      return res.status(400).json({ error: 'Fecha límite o puntaje máximo inválido.' });
    }

    const now = Date.now();
    await connection.beginTransaction();
    await connection.query(
      `UPDATE actividades SET titulo = ?, descripcion = ?, fecha_limite = ?, puntaje_maximo = ?, updated_at = ?
       WHERE id = ?`,
      [nextTitulo, nextDesc, nextFecha, nextPuntaje, now, actividad.id]
    );

    for (const file of req.files || []) {
      await connection.query(
        `INSERT INTO actividad_adjuntos (actividad_id, nombre_original, nombre_archivo, mime_type, tamano_bytes, created_at)
         VALUES (?, ?, ?, ?, ?, ?)`,
        [actividad.id, file.originalname, file.filename, file.mimetype, file.size, now]
      );
    }
    await connection.commit();

    const [adjRows] = await pool.query('SELECT * FROM actividad_adjuntos WHERE actividad_id = ?', [actividad.id]);
    return res.json({
      ...actividad,
      titulo: nextTitulo,
      descripcion: nextDesc,
      fecha_limite: nextFecha,
      puntaje_maximo: nextPuntaje,
      adjuntos: adjRows.map(mapAdjunto),
      updated_at: now,
    });
  } catch (err) {
    await connection.rollback();
    cleanupUploadedFiles('actividades', req.files);
    return next(err);
  } finally {
    connection.release();
  }
});

router.delete('/:actividadId', async (req, res, next) => {
  try {
    const actividad = await getActividadWithCourse(req.params.actividadId);
    if (!actividad) return res.status(404).json({ error: 'Actividad no encontrada.' });

    const access = await authorizeCourseAccess(actividad.curso_id, req.user, { requireInstructorOwner: true });
    if (access.error) return res.status(access.status).json({ error: access.error });

    const [adjuntos] = await pool.query('SELECT nombre_archivo FROM actividad_adjuntos WHERE actividad_id = ?', [actividad.id]);
    const [archivosEntrega] = await pool.query(
      `SELECT ea.nombre_archivo FROM entrega_archivos ea
       INNER JOIN entregas e ON e.id = ea.entrega_id
       WHERE e.actividad_id = ?`,
      [actividad.id]
    );

    await pool.query('DELETE FROM actividades WHERE id = ?', [actividad.id]);

    adjuntos.forEach((f) => safeUnlink('actividades', f.nombre_archivo));
    archivosEntrega.forEach((f) => safeUnlink('entregas', f.nombre_archivo));

    return res.status(204).send();
  } catch (err) {
    return next(err);
  }
});

router.get('/:actividadId/entregas', async (req, res, next) => {
  try {
    const actividad = await getActividadWithCourse(req.params.actividadId);
    if (!actividad) return res.status(404).json({ error: 'Actividad no encontrada.' });

    const access = await authorizeCourseAccess(actividad.curso_id, req.user, { requireInstructorOwner: true });
    if (access.error) return res.status(access.status).json({ error: access.error });

    const [estudiantes] = await pool.query(
      `SELECT u.id AS aprendiz_id, u.nombre AS aprendiz_nombre, u.email AS aprendiz_email
       FROM inscripciones i
       INNER JOIN usuarios u ON u.id = i.aprendiz_id
       WHERE i.curso_id = ?
       ORDER BY u.nombre ASC`,
      [actividad.curso_id]
    );
    const [entregas] = await pool.query('SELECT * FROM entregas WHERE actividad_id = ?', [actividad.id]);
    const entregaIds = entregas.map((e) => e.id);
    const archivosByEntrega = new Map();
    if (entregaIds.length > 0) {
      const [archivos] = await pool.query('SELECT * FROM entrega_archivos WHERE entrega_id IN (?)', [entregaIds]);
      for (const f of archivos) {
        const list = archivosByEntrega.get(f.entrega_id) || [];
        list.push(mapEntregaArchivo(f));
        archivosByEntrega.set(f.entrega_id, list);
      }
    }

    const entregaByAprendiz = new Map(entregas.map((e) => [e.aprendiz_id, e]));
    const resultado = estudiantes.map((est) => {
      const e = entregaByAprendiz.get(est.aprendiz_id);
      const estado = e
        ? e.estado
        : (Date.now() > Number(actividad.fecha_limite) ? 'TARDIA' : 'PENDIENTE');
      return {
        aprendiz_id: est.aprendiz_id,
        aprendiz_nombre: est.aprendiz_nombre,
        aprendiz_email: est.aprendiz_email,
        entrega_id: e?.id || null,
        descripcion: e?.descripcion || '',
        nota: e && e.nota !== null ? Number(e.nota) : null,
        retroalimentacion: e?.retroalimentacion || null,
        exenta: e ? Boolean(e.exenta) : false,
        estado,
        fecha_entrega: e?.fecha_entrega ? Number(e.fecha_entrega) : null,
        fecha_calificacion: e?.fecha_calificacion ? Number(e.fecha_calificacion) : null,
        archivos: e ? (archivosByEntrega.get(e.id) || []) : [],
      };
    });

    return res.json({
      actividad: { ...actividad, puntaje_maximo: Number(actividad.puntaje_maximo) },
      entregas: resultado,
    });
  } catch (err) {
    return next(err);
  }
});

router.post('/:actividadId/entrega', requireRole('APRENDIZ'), uploadEntregas.array('archivos', 10), async (req, res, next) => {
  const connection = await pool.getConnection();
  try {
    const actividad = await getActividadWithCourse(req.params.actividadId);
    if (!actividad) {
      cleanupUploadedFiles('entregas', req.files);
      return res.status(404).json({ error: 'Actividad no encontrada.' });
    }
    const access = await authorizeCourseAccess(actividad.curso_id, req.user);
    if (access.error) {
      cleanupUploadedFiles('entregas', req.files);
      return res.status(access.status).json({ error: access.error });
    }
    if (access.curso.estado !== 'ACTIVO') {
      cleanupUploadedFiles('entregas', req.files);
      return res.status(403).json({ error: 'El curso está archivado; no admite nuevas entregas.' });
    }

    const descripcion = String(req.body?.descripcion || '').trim();
    const archivos = req.files || [];
    const conservarPrevios = req.body?.conservar_archivos === 'true' || req.body?.conservar_archivos === true;

    const [existentes] = await connection.query(
      'SELECT * FROM entregas WHERE actividad_id = ? AND aprendiz_id = ?',
      [actividad.id, req.user.id]
    );
    const existente = existentes[0] || null;

    if (existente && (existente.estado === 'CALIFICADA' || existente.estado === 'EXENTA')) {
      cleanupUploadedFiles('entregas', req.files);
      return res.status(409).json({ error: 'La entrega ya fue calificada o exenta y no puede modificarse.' });
    }

    if (!descripcion && archivos.length === 0 && !conservarPrevios) {
      cleanupUploadedFiles('entregas', req.files);
      return res.status(400).json({ error: 'Agrega una descripción o al menos un archivo de evidencia.' });
    }

    const now = Date.now();
    const estadoEntrega = now > Number(actividad.fecha_limite) ? 'TARDIA' : 'ENTREGADA';

    await connection.beginTransaction();

    let entregaId;
    const filesToDeleteAfterCommit = [];

    if (existente) {
      entregaId = existente.id;
      await connection.query(
        'UPDATE entregas SET descripcion = ?, estado = ?, fecha_entrega = ?, updated_at = ? WHERE id = ?',
        [descripcion, estadoEntrega, now, now, entregaId]
      );
      if (!conservarPrevios && archivos.length > 0) {
        const [prevFiles] = await connection.query(
          'SELECT nombre_archivo FROM entrega_archivos WHERE entrega_id = ?',
          [entregaId]
        );
        prevFiles.forEach((pf) => filesToDeleteAfterCommit.push(pf.nombre_archivo));
        await connection.query('DELETE FROM entrega_archivos WHERE entrega_id = ?', [entregaId]);
      }
    } else {
      const [ins] = await connection.query(
        `INSERT INTO entregas (actividad_id, aprendiz_id, descripcion, estado, fecha_entrega, updated_at)
         VALUES (?, ?, ?, ?, ?, ?)`,
        [actividad.id, req.user.id, descripcion, estadoEntrega, now, now]
      );
      entregaId = ins.insertId;
    }

    for (const file of archivos) {
      await connection.query(
        `INSERT INTO entrega_archivos (entrega_id, nombre_original, nombre_archivo, mime_type, tamano_bytes, created_at)
         VALUES (?, ?, ?, ?, ?, ?)`,
        [entregaId, file.originalname, file.filename, file.mimetype, file.size, now]
      );
    }

    await connection.commit();
    filesToDeleteAfterCommit.forEach((f) => safeUnlink('entregas', f));

    const [savedFiles] = await pool.query('SELECT * FROM entrega_archivos WHERE entrega_id = ?', [entregaId]);
    return res.status(existente ? 200 : 201).json({
      id: entregaId,
      actividad_id: actividad.id,
      aprendiz_id: req.user.id,
      descripcion,
      nota: null,
      retroalimentacion: null,
      exenta: false,
      estado: estadoEntrega,
      fecha_entrega: now,
      archivos: savedFiles.map(mapEntregaArchivo),
    });
  } catch (err) {
    await connection.rollback();
    cleanupUploadedFiles('entregas', req.files);
    return next(err);
  } finally {
    connection.release();
  }
});

router.put('/:actividadId/entregas/:aprendizId/calificar', async (req, res, next) => {
  try {
    const actividad = await getActividadWithCourse(req.params.actividadId);
    if (!actividad) return res.status(404).json({ error: 'Actividad no encontrada.' });

    const access = await authorizeCourseAccess(actividad.curso_id, req.user, { requireInstructorOwner: true });
    if (access.error) return res.status(access.status).json({ error: access.error });

    const aprendizId = Number(req.params.aprendizId);
    const [insc] = await pool.query(
      'SELECT id FROM inscripciones WHERE curso_id = ? AND aprendiz_id = ?',
      [actividad.curso_id, aprendizId]
    );
    if (insc.length === 0) {
      return res.status(404).json({ error: 'El aprendiz no está inscrito en este curso.' });
    }

    const exenta = Boolean(req.body?.exenta);
    const retroalimentacion = req.body?.retroalimentacion !== undefined
      ? String(req.body.retroalimentacion).trim()
      : null;

    let notaNum = null;
    if (!exenta) {
      notaNum = Number(req.body?.nota);
      const max = Number(actividad.puntaje_maximo);
      if (!Number.isFinite(notaNum) || notaNum < 0 || notaNum > max) {
        return res.status(400).json({ error: `La calificación debe estar entre 0 y ${max}.` });
      }
    }

    const now = Date.now();
    const nuevoEstado = exenta ? 'EXENTA' : 'CALIFICADA';

    await pool.query(
      `INSERT INTO entregas (actividad_id, aprendiz_id, descripcion, nota, retroalimentacion, exenta, estado, fecha_calificacion, updated_at)
       VALUES (?, ?, '', ?, ?, ?, ?, ?, ?)
       ON DUPLICATE KEY UPDATE
         nota = VALUES(nota),
         retroalimentacion = VALUES(retroalimentacion),
         exenta = VALUES(exenta),
         estado = VALUES(estado),
         fecha_calificacion = VALUES(fecha_calificacion),
         updated_at = VALUES(updated_at)`,
      [actividad.id, aprendizId, notaNum, retroalimentacion, exenta ? 1 : 0, nuevoEstado, now, now]
    );

    return res.json({
      actividad_id: actividad.id,
      aprendiz_id: aprendizId,
      nota: notaNum,
      retroalimentacion,
      exenta,
      estado: nuevoEstado,
      fecha_calificacion: now,
    });
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
