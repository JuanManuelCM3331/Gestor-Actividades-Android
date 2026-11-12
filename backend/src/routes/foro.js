const express = require('express');
const { pool } = require('../config/db');
const { authenticate, authorizeCourseAccess } = require('../middleware/auth');

const router = express.Router();
router.use(authenticate);

function canAccessThread(hilo, curso, user) {
  if (hilo.tipo === 'PUBLICO') return true;
  // Foro privado: estrictamente 1 a 1 entre el instructor del curso y el aprendiz objetivo
  return user.id === curso.instructor_id || user.id === hilo.aprendiz_objetivo_id;
}

router.get('/curso/:cursoId', async (req, res, next) => {
  try {
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
      if (req.user.rol === 'APRENDIZ') {
        if (aprendiz_objetivo_id && Number(aprendiz_objetivo_id) !== req.user.id) {
          return res.status(403).json({ error: 'No puedes crear conversaciones privadas en nombre de otro aprendiz.' });
        }
        targetAprendizId = req.user.id;
      } else {
        targetAprendizId = Number(aprendiz_objetivo_id);
        if (!Number.isInteger(targetAprendizId) || targetAprendizId <= 0) {
          return res.status(400).json({ error: 'Debes seleccionar el aprendiz destinatario para el foro privado.' });
        }
        const [insc] = await pool.query(
          'SELECT id FROM inscripciones WHERE curso_id = ? AND aprendiz_id = ?',
          [access.curso.id, targetAprendizId]
        );
        if (insc.length === 0) {
          return res.status(400).json({ error: 'El aprendiz seleccionado no está inscrito en este curso.' });
        }
      }
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

    return res.json({
      hilo: { ...hilo, created_at: Number(hilo.created_at), total_mensajes: mensajes.length },
      mensajes: mensajes.map((m) => ({ ...m, created_at: Number(m.created_at) })),
    });
  } catch (err) {
    return next(err);
  }
});

router.post('/:hiloId/mensajes', async (req, res, next) => {
  try {
    const hiloId = Number(req.params.hiloId);
    const [hilos] = await pool.query('SELECT * FROM foro_hilos WHERE id = ?', [hiloId]);
    if (hilos.length === 0) return res.status(404).json({ error: 'Hilo no encontrado.' });
    const hilo = hilos[0];

    const access = await authorizeCourseAccess(hilo.curso_id, req.user);
    if (access.error) return res.status(access.status).json({ error: access.error });

    if (!canAccessThread(hilo, access.curso, req.user)) {
      return res.status(403).json({ error: 'No tienes permiso para responder en este hilo privado.' });
    }

    const contenido = String(req.body?.contenido || '').trim();
    if (contenido.length < 1) {
      return res.status(400).json({ error: 'El mensaje no puede estar vacío.' });
    }

    const now = Date.now();
    const [result] = await pool.query(
      'INSERT INTO foro_mensajes (hilo_id, autor_id, contenido, created_at) VALUES (?, ?, ?, ?)',
      [hiloId, req.user.id, contenido, now]
    );

    return res.status(201).json({
      id: result.insertId,
      hilo_id: hiloId,
      autor_id: req.user.id,
      autor_nombre: req.user.nombre,
      autor_rol: req.user.rol,
      contenido,
      created_at: now,
    });
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
