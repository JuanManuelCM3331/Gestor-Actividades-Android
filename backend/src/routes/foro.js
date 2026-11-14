const express = require('express');
const { pool } = require('../config/db');
const { authenticate, authorizeCourseAccess } = require('../middleware/auth');
const { createMulterUpload, cleanupUploadedFiles } = require('../utils/filePolicy');

const router = express.Router();
const uploadForo = createMulterUpload('foro');
router.use(authenticate);

let schemaChecked = false;
async function ensureForoChatSchema() {
  if (schemaChecked) return;
  try {
    const [cols] = await pool.query("SHOW COLUMNS FROM foro_mensajes LIKE 'enlace_url'");
    if (cols.length === 0) {
      await pool.query('ALTER TABLE foro_mensajes ADD COLUMN enlace_url VARCHAR(512) NULL AFTER contenido');
    }
    await pool.query(`
      CREATE TABLE IF NOT EXISTS foro_archivos (
        id INT AUTO_INCREMENT PRIMARY KEY,
        mensaje_id INT NOT NULL,
        nombre_original VARCHAR(255) NOT NULL,
        nombre_archivo VARCHAR(255) NOT NULL,
        mime_type VARCHAR(140) NOT NULL,
        tamano_bytes BIGINT NOT NULL,
        created_at BIGINT NOT NULL,
        CONSTRAINT fk_foro_archivos_mensaje
          FOREIGN KEY (mensaje_id) REFERENCES foro_mensajes(id)
          ON UPDATE CASCADE
          ON DELETE CASCADE,
        INDEX idx_foro_archivos_mensaje (mensaje_id)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    `);
    schemaChecked = true;
  } catch (_err) {
    // Si la tabla ya existe o hay concurrencia, continuar
    schemaChecked = true;
  }
}

function optionalMultipart(req, res, next) {
  const ct = String(req.headers['content-type'] || '').toLowerCase();
  if (ct.includes('multipart/form-data')) {
    return uploadForo.array('archivos', 10)(req, res, next);
  }
  return next();
}

function canAccessThread(hilo, curso, user) {
  if (hilo.tipo === 'PUBLICO') return true;
  return user.id === curso.instructor_id || user.id === hilo.aprendiz_objetivo_id;
}

function normalizeUrl(raw) {
  const clean = String(raw || '').trim();
  if (!clean) return null;
  if (!/^https?:\/\/.+/i.test(clean)) {
    return { error: 'El enlace debe comenzar por http:// o https://' };
  }
  return { url: clean };
}

async function attachFilesToMessages(mensajes) {
  if (!mensajes || mensajes.length === 0) return [];
  const ids = mensajes.map((m) => m.id).filter((id) => Number.isInteger(id) && id > 0);
  const filesByMsg = new Map();
  if (ids.length > 0) {
    const [rows] = await pool.query(
      'SELECT * FROM foro_archivos WHERE mensaje_id IN (?) ORDER BY id ASC',
      [ids]
    );
    for (const r of rows) {
      if (!filesByMsg.has(r.mensaje_id)) filesByMsg.set(r.mensaje_id, []);
      filesByMsg.get(r.mensaje_id).push({
        id: r.id,
        nombre_original: r.nombre_original,
        mime_type: r.mime_type,
        tamano_bytes: Number(r.tamano_bytes),
        url_descarga: `/api/archivos/foro/${r.id}`,
      });
    }
  }
  return mensajes.map((m) => ({
    id: Number(m.id),
    hilo_id: Number(m.hilo_id),
    autor_id: Number(m.autor_id),
    autor_nombre: m.autor_nombre,
    autor_rol: m.autor_rol,
    contenido: m.contenido || '',
    enlace_url: m.enlace_url || null,
    archivos: filesByMsg.get(m.id) || [],
    created_at: Number(m.created_at),
  }));
}

async function resolvePrivateTargetAprendiz(curso, user, rawAprendizId, requireForInstructor) {
  if (user.rol === 'APRENDIZ') {
    if (rawAprendizId && Number(rawAprendizId) !== user.id) {
      return { status: 403, error: 'No puedes acceder al chat privado de otro aprendiz.' };
    }
    return { targetAprendizId: user.id };
  }
  if (!rawAprendizId) {
    if (requireForInstructor) {
      return { status: 400, error: 'Selecciona el aprendiz para el chat privado.' };
    }
    return { targetAprendizId: null };
  }
  const targetAprendizId = Number(rawAprendizId);
  if (!Number.isInteger(targetAprendizId) || targetAprendizId <= 0) {
    return { status: 400, error: 'Aprendiz destinatario inválido.' };
  }
  const [insc] = await pool.query(
    'SELECT id FROM inscripciones WHERE curso_id = ? AND aprendiz_id = ?',
    [curso.id, targetAprendizId]
  );
  if (insc.length === 0) {
    return { status: 400, error: 'El aprendiz seleccionado no está inscrito en este curso.' };
  }
  return { targetAprendizId };
}

async function getOrCreateChannelHilo(curso, user, cleanTipo, targetAprendizId) {
  let query = 'SELECT * FROM foro_hilos WHERE curso_id = ? AND tipo = ?';
  const params = [curso.id, cleanTipo];
  if (cleanTipo === 'PRIVADO') {
    query += ' AND aprendiz_objetivo_id = ?';
    params.push(targetAprendizId);
  }
  query += ' ORDER BY id ASC LIMIT 1';
  const [existing] = await pool.query(query, params);
  if (existing.length > 0) return existing[0];

  const now = Date.now();
  const titulo = cleanTipo === 'PRIVADO' ? '__CHAT_PRIVADO__' : '__CHAT_GRUPAL__';
  const [res] = await pool.query(
    `INSERT INTO foro_hilos (curso_id, autor_id, aprendiz_objetivo_id, tipo, titulo, contenido, created_at)
     VALUES (?, ?, ?, ?, ?, '', ?)`,
    [curso.id, user.id, cleanTipo === 'PRIVADO' ? targetAprendizId : null, cleanTipo, titulo, now]
  );
  return {
    id: res.insertId,
    curso_id: curso.id,
    autor_id: user.id,
    aprendiz_objetivo_id: cleanTipo === 'PRIVADO' ? targetAprendizId : null,
    tipo: cleanTipo,
    titulo,
    contenido: '',
    created_at: now,
  };
}

// ---------------------------------------------------------------------------
// Nuevos endpoints de Chat directo (Global del curso y Privado 1 a 1)
// ---------------------------------------------------------------------------
router.get('/curso/:cursoId/chat', async (req, res, next) => {
  try {
    await ensureForoChatSchema();
    const access = await authorizeCourseAccess(req.params.cursoId, req.user);
    if (access.error) return res.status(access.status).json({ error: access.error });

    const cleanTipo = req.query.tipo === 'PRIVADO' ? 'PRIVADO' : 'PUBLICO';
    let targetAprendizId = null;

    if (cleanTipo === 'PRIVADO') {
      const targetRes = await resolvePrivateTargetAprendiz(
        access.curso,
        req.user,
        req.query.aprendizId,
        false
      );
      if (targetRes.error) return res.status(targetRes.status).json({ error: targetRes.error });
      targetAprendizId = targetRes.targetAprendizId;
      if (!targetAprendizId) {
        return res.json([]);
      }
    }

    let hilosQuery = `
      SELECT h.*, u.nombre AS autor_nombre, u.rol AS autor_rol
      FROM foro_hilos h
      INNER JOIN usuarios u ON u.id = h.autor_id
      WHERE h.curso_id = ? AND h.tipo = ?
    `;
    const hilosParams = [access.curso.id, cleanTipo];
    if (cleanTipo === 'PRIVADO') {
      hilosQuery += ' AND h.aprendiz_objetivo_id = ?';
      hilosParams.push(targetAprendizId);
    }
    hilosQuery += ' ORDER BY h.created_at ASC';

    const [hilos] = await pool.query(hilosQuery, hilosParams);
    if (hilos.length === 0) return res.json([]);

    const hiloIds = hilos.map((h) => h.id);
    const [mensajesRows] = await pool.query(
      `SELECT m.*, u.nombre AS autor_nombre, u.rol AS autor_rol
       FROM foro_mensajes m
       INNER JOIN usuarios u ON u.id = m.autor_id
       WHERE m.hilo_id IN (?)
       ORDER BY m.created_at ASC, m.id ASC`,
      [hiloIds]
    );

    const enriched = await attachFilesToMessages(mensajesRows);

    // Incluir mensajes iniciales de hilos heredados (creados con título/contenido)
    const legacySeedMessages = hilos
      .filter((h) => h.contenido && String(h.contenido).trim().length > 0 && !String(h.titulo).startsWith('__CHAT_'))
      .map((h) => ({
        id: -Number(h.id),
        hilo_id: Number(h.id),
        autor_id: Number(h.autor_id),
        autor_nombre: h.autor_nombre,
        autor_rol: h.autor_rol,
        contenido: h.contenido,
        enlace_url: null,
        archivos: [],
        created_at: Number(h.created_at),
      }));

    const combined = [...legacySeedMessages, ...enriched].sort((a, b) => a.created_at - b.created_at);
    return res.json(combined);
  } catch (err) {
    return next(err);
  }
});

router.post('/curso/:cursoId/chat', optionalMultipart, async (req, res, next) => {
  const uploadedFiles = req.files || [];
  try {
    await ensureForoChatSchema();
    const access = await authorizeCourseAccess(req.params.cursoId, req.user);
    if (access.error) {
      cleanupUploadedFiles('foro', uploadedFiles);
      return res.status(access.status).json({ error: access.error });
    }

    const cleanTipo = req.body?.tipo === 'PRIVADO' ? 'PRIVADO' : 'PUBLICO';
    const rawAprendizId = req.body?.aprendiz_objetivo_id;
    let targetAprendizId = null;

    if (cleanTipo === 'PRIVADO') {
      const targetRes = await resolvePrivateTargetAprendiz(access.curso, req.user, rawAprendizId, true);
      if (targetRes.error) {
        cleanupUploadedFiles('foro', uploadedFiles);
        return res.status(targetRes.status).json({ error: targetRes.error });
      }
      targetAprendizId = targetRes.targetAprendizId;
    }

    const contenido = String(req.body?.contenido || '').trim();
    const urlCheck = normalizeUrl(req.body?.enlace_url);
    if (urlCheck && urlCheck.error) {
      cleanupUploadedFiles('foro', uploadedFiles);
      return res.status(400).json({ error: urlCheck.error });
    }
    const enlaceUrl = urlCheck ? urlCheck.url : null;

    if (!contenido && !enlaceUrl && uploadedFiles.length === 0) {
      return res.status(400).json({
        error: 'Escribe un mensaje, comparte un enlace o adjunta un archivo/audio/imagen.',
      });
    }

    const hilo = await getOrCreateChannelHilo(access.curso, req.user, cleanTipo, targetAprendizId);
    const now = Date.now();

    const [msgResult] = await pool.query(
      'INSERT INTO foro_mensajes (hilo_id, autor_id, contenido, enlace_url, created_at) VALUES (?, ?, ?, ?, ?)',
      [hilo.id, req.user.id, contenido, enlaceUrl, now]
    );
    const mensajeId = msgResult.insertId;

    const archivosResp = [];
    for (const file of uploadedFiles) {
      const [fRes] = await pool.query(
        `INSERT INTO foro_archivos (mensaje_id, nombre_original, nombre_archivo, mime_type, tamano_bytes, created_at)
         VALUES (?, ?, ?, ?, ?, ?)`,
        [mensajeId, file.originalname, file.filename, file.mimetype, file.size, now]
      );
      archivosResp.push({
        id: fRes.insertId,
        nombre_original: file.originalname,
        mime_type: file.mimetype,
        tamano_bytes: Number(file.size),
        url_descarga: `/api/archivos/foro/${fRes.insertId}`,
      });
    }

    return res.status(201).json({
      id: mensajeId,
      hilo_id: Number(hilo.id),
      autor_id: req.user.id,
      autor_nombre: req.user.nombre,
      autor_rol: req.user.rol,
      contenido,
      enlace_url: enlaceUrl,
      archivos: archivosResp,
      created_at: now,
    });
  } catch (err) {
    cleanupUploadedFiles('foro', uploadedFiles);
    return next(err);
  }
});

// ---------------------------------------------------------------------------
// Endpoints de hilos (compatibilidad e historial)
// ---------------------------------------------------------------------------
router.get('/curso/:cursoId', async (req, res, next) => {
  try {
    await ensureForoChatSchema();
    const access = await authorizeCourseAccess(req.params.cursoId, req.user);
    if (access.error) return res.status(access.status).json({ error: access.error });

    const tipo = req.query.tipo === 'PRIVADO' ? 'PRIVADO' : 'PUBLICO';
    const baseQuery = `
      SELECT h.*, u.nombre AS autor_nombre, u.rol AS autor_rol,
             ua.nombre AS aprendiz_objetivo_nombre,
             (SELECT COUNT(*) FROM foro_mensajes m WHERE m.hilo_id = h.id) AS total_mensajes
      FROM foro_hilos h
      INNER JOIN usuarios u ON u.id = h.autor_id
      LEFT JOIN usuarios ua ON ua.id = h.aprendiz_objetivo_id
      WHERE h.curso_id = ? AND h.tipo = ?
    `;

    let rows;
    if (tipo === 'PUBLICO') {
      [rows] = await pool.query(`${baseQuery} ORDER BY h.created_at DESC`, [access.curso.id, 'PUBLICO']);
    } else if (req.user.rol === 'APRENDIZ') {
      if (req.query.aprendizId && Number(req.query.aprendizId) !== req.user.id) {
        return res.status(403).json({ error: 'No puedes acceder al foro privado de otro aprendiz.' });
      }
      [rows] = await pool.query(
        `${baseQuery} AND h.aprendiz_objetivo_id = ? ORDER BY h.created_at DESC`,
        [access.curso.id, 'PRIVADO', req.user.id]
      );
    } else if (req.query.aprendizId) {
      [rows] = await pool.query(
        `${baseQuery} AND h.aprendiz_objetivo_id = ? ORDER BY h.created_at DESC`,
        [access.curso.id, 'PRIVADO', Number(req.query.aprendizId)]
      );
    } else {
      [rows] = await pool.query(`${baseQuery} ORDER BY h.created_at DESC`, [access.curso.id, 'PRIVADO']);
    }

    return res.json(rows.map((r) => ({
      ...r,
      created_at: Number(r.created_at),
      total_mensajes: Number(r.total_mensajes),
    })));
  } catch (err) {
    return next(err);
  }
});

router.post('/curso/:cursoId', async (req, res, next) => {
  try {
    await ensureForoChatSchema();
    const access = await authorizeCourseAccess(req.params.cursoId, req.user);
    if (access.error) return res.status(access.status).json({ error: access.error });

    const { tipo = 'PUBLICO', titulo, contenido, aprendiz_objetivo_id } = req.body;
    const cleanTipo = tipo === 'PRIVADO' ? 'PRIVADO' : 'PUBLICO';

    if (!titulo || typeof titulo !== 'string' || titulo.trim().length < 3 || titulo.trim().length > 160) {
      return res.status(400).json({ error: 'El título del hilo es obligatorio (entre 3 y 160 caracteres).' });
    }
    if (!contenido || typeof contenido !== 'string' || contenido.trim().length < 2) {
      return res.status(400).json({ error: 'El contenido del mensaje es obligatorio.' });
    }

    let targetAprendizId = null;
    if (cleanTipo === 'PRIVADO') {
      const targetRes = await resolvePrivateTargetAprendiz(access.curso, req.user, aprendiz_objetivo_id, true);
      if (targetRes.error) return res.status(targetRes.status).json({ error: targetRes.error });
      targetAprendizId = targetRes.targetAprendizId;
    }

    const now = Date.now();
    const [result] = await pool.query(
      `INSERT INTO foro_hilos (curso_id, autor_id, aprendiz_objetivo_id, tipo, titulo, contenido, created_at)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
      [access.curso.id, req.user.id, targetAprendizId, cleanTipo, titulo.trim(), contenido.trim(), now]
    );

    const [created] = await pool.query(
      `SELECT h.*, u.nombre AS autor_nombre, u.rol AS autor_rol,
              ua.nombre AS aprendiz_objetivo_nombre, 0 AS total_mensajes
       FROM foro_hilos h
       INNER JOIN usuarios u ON u.id = h.autor_id
       LEFT JOIN usuarios ua ON ua.id = h.aprendiz_objetivo_id
       WHERE h.id = ?`,
      [result.insertId]
    );
    return res.status(201).json({ ...created[0], created_at: Number(created[0].created_at), total_mensajes: 0 });
  } catch (err) {
    return next(err);
  }
});

router.get('/:hiloId/mensajes', async (req, res, next) => {
  try {
    await ensureForoChatSchema();
    const hiloId = Number(req.params.hiloId);
    const [hilos] = await pool.query(
      `SELECT h.*, u.nombre AS autor_nombre, u.rol AS autor_rol, ua.nombre AS aprendiz_objetivo_nombre
       FROM foro_hilos h
       INNER JOIN usuarios u ON u.id = h.autor_id
       LEFT JOIN usuarios ua ON ua.id = h.aprendiz_objetivo_id
       WHERE h.id = ?`,
      [hiloId]
    );
    if (hilos.length === 0) return res.status(404).json({ error: 'Hilo no encontrado.' });
    const hilo = hilos[0];

    const access = await authorizeCourseAccess(hilo.curso_id, req.user);
    if (access.error) return res.status(access.status).json({ error: access.error });

    if (!canAccessThread(hilo, access.curso, req.user)) {
      return res.status(403).json({ error: 'Acceso denegado: este hilo privado es exclusivo entre el instructor y su aprendiz.' });
    }

    const [mensajes] = await pool.query(
      `SELECT m.*, u.nombre AS autor_nombre, u.rol AS autor_rol
       FROM foro_mensajes m
       INNER JOIN usuarios u ON u.id = m.autor_id
       WHERE m.hilo_id = ?
       ORDER BY m.created_at ASC`,
      [hiloId]
    );

    const enriched = await attachFilesToMessages(mensajes);

    return res.json({
      hilo: { ...hilo, created_at: Number(hilo.created_at), total_mensajes: enriched.length },
      mensajes: enriched,
    });
  } catch (err) {
    return next(err);
  }
});

router.post('/:hiloId/mensajes', optionalMultipart, async (req, res, next) => {
  const uploadedFiles = req.files || [];
  try {
    await ensureForoChatSchema();
    const hiloId = Number(req.params.hiloId);
    const [hilos] = await pool.query('SELECT * FROM foro_hilos WHERE id = ?', [hiloId]);
    if (hilos.length === 0) {
      cleanupUploadedFiles('foro', uploadedFiles);
      return res.status(404).json({ error: 'Hilo no encontrado.' });
    }
    const hilo = hilos[0];

    const access = await authorizeCourseAccess(hilo.curso_id, req.user);
    if (access.error) {
      cleanupUploadedFiles('foro', uploadedFiles);
      return res.status(access.status).json({ error: access.error });
    }

    if (!canAccessThread(hilo, access.curso, req.user)) {
      cleanupUploadedFiles('foro', uploadedFiles);
      return res.status(403).json({ error: 'No tienes permiso para responder en este hilo privado.' });
    }

    const contenido = String(req.body?.contenido || '').trim();
    const urlCheck = normalizeUrl(req.body?.enlace_url);
    if (urlCheck && urlCheck.error) {
      cleanupUploadedFiles('foro', uploadedFiles);
      return res.status(400).json({ error: urlCheck.error });
    }
    const enlaceUrl = urlCheck ? urlCheck.url : null;

    if (!contenido && !enlaceUrl && uploadedFiles.length === 0) {
      return res.status(400).json({ error: 'El mensaje no puede estar vacío.' });
    }

    const now = Date.now();
    const [result] = await pool.query(
      'INSERT INTO foro_mensajes (hilo_id, autor_id, contenido, enlace_url, created_at) VALUES (?, ?, ?, ?, ?)',
      [hiloId, req.user.id, contenido, enlaceUrl, now]
    );
    const mensajeId = result.insertId;

    const archivosResp = [];
    for (const file of uploadedFiles) {
      const [fRes] = await pool.query(
        `INSERT INTO foro_archivos (mensaje_id, nombre_original, nombre_archivo, mime_type, tamano_bytes, created_at)
         VALUES (?, ?, ?, ?, ?, ?)`,
        [mensajeId, file.originalname, file.filename, file.mimetype, file.size, now]
      );
      archivosResp.push({
        id: fRes.insertId,
        nombre_original: file.originalname,
        mime_type: file.mimetype,
        tamano_bytes: Number(file.size),
        url_descarga: `/api/archivos/foro/${fRes.insertId}`,
      });
    }

    return res.status(201).json({
      id: mensajeId,
      hilo_id: hiloId,
      autor_id: req.user.id,
      autor_nombre: req.user.nombre,
      autor_rol: req.user.rol,
      contenido,
      enlace_url: enlaceUrl,
      archivos: archivosResp,
      created_at: now,
    });
  } catch (err) {
    cleanupUploadedFiles('foro', uploadedFiles);
    return next(err);
  }
});

module.exports = router;
