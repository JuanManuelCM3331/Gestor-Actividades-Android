const express = require('express');
const crypto = require('crypto');
const { pool } = require('../config/db');
const { authenticate, requireRole, authorizeCourseAccess } = require('../middleware/auth');
const { createRateLimiter } = require('../middleware/rateLimiter');
const { calcularResumenCalificacion } = require('../utils/grades');
const { safeUnlink } = require('../utils/filePolicy');

const router = express.Router();
router.use(authenticate);

// SEC-03: Límite contra enumeración de códigos de acceso de 6 caracteres
const joinCourseLimiter = createRateLimiter({
  windowMs: 15 * 60 * 1000,
  max: 15,
  message: 'Demasiados intentos con códigos de acceso. Espera unos minutos antes de volver a intentarlo.',
});

function generarCodigoAcceso() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let out = '';
  for (let i = 0; i < 6; i += 1) {
    out += chars[crypto.randomInt(0, chars.length)];
  }
  return out;
}

async function generarCodigoUnico() {
  for (let attempt = 0; attempt < 10; attempt += 1) {
    const codigo = generarCodigoAcceso();
    const [existing] = await pool.query('SELECT id FROM cursos WHERE codigo_acceso = ?', [codigo]);
    if (existing.length === 0) return codigo;
  }
  return `${generarCodigoAcceso()}${crypto.randomInt(10, 99)}`;
}

async function calcularResumenCursoEstudiante(cursoId, aprendizId) {
  const mapPorAprendiz = await calcularResumenesCursoPorAprendiz(cursoId, [aprendizId]);
  return mapPorAprendiz.get(Number(aprendizId)) || calcularResumenCalificacion([], new Map());
}

/**
 * PERF-04: Calcula las calificaciones de todos los aprendices de un curso con solo 2 consultas SQL
 * (1 para actividades y 1 para todas las entregas del curso), eliminando el problema N+1.
 */
async function calcularResumenesCursoPorAprendiz(cursoId, aprendizIds) {
  const cleanIds = (aprendizIds || []).map((id) => Number(id)).filter((id) => Number.isInteger(id) && id > 0);
  const result = new Map();
  if (cleanIds.length === 0) return result;

  const [actividades] = await pool.query(
    'SELECT id, titulo, fecha_limite, puntaje_maximo FROM actividades WHERE curso_id = ? ORDER BY fecha_limite ASC',
    [cursoId]
  );

  const entregasPorAprendiz = new Map();
  if (actividades.length > 0) {
    const [entregas] = await pool.query(
      `SELECT e.* FROM entregas e
       INNER JOIN actividades a ON e.actividad_id = a.id
       WHERE a.curso_id = ? AND e.aprendiz_id IN (?)`,
      [cursoId, cleanIds]
    );
    for (const row of entregas) {
      let mapAct = entregasPorAprendiz.get(row.aprendiz_id);
      if (!mapAct) {
        mapAct = new Map();
        entregasPorAprendiz.set(row.aprendiz_id, mapAct);
      }
      mapAct.set(row.actividad_id, row);
    }
  }

  for (const aprendizId of cleanIds) {
    const mapAct = entregasPorAprendiz.get(aprendizId) || new Map();
    result.set(aprendizId, calcularResumenCalificacion(actividades, mapAct));
  }
  return result;
}

/**
 * PERF-04: Calcula los resúmenes de múltiples cursos para un único aprendiz con solo 2 consultas SQL.
 */
async function calcularResumenesCursosDeAprendiz(cursoIds, aprendizId) {
  const cleanCursoIds = (cursoIds || []).map((id) => Number(id)).filter((id) => Number.isInteger(id) && id > 0);
  const result = new Map();
  if (cleanCursoIds.length === 0) return result;

  const [actividades] = await pool.query(
    'SELECT id, curso_id, titulo, fecha_limite, puntaje_maximo FROM actividades WHERE curso_id IN (?) ORDER BY fecha_limite ASC',
    [cleanCursoIds]
  );
  const [entregas] = await pool.query(
    `SELECT e.*, a.curso_id FROM entregas e
     INNER JOIN actividades a ON e.actividad_id = a.id
     WHERE a.curso_id IN (?) AND e.aprendiz_id = ?`,
    [cleanCursoIds, aprendizId]
  );

  const actsPorCurso = new Map();
  for (const act of actividades) {
    const list = actsPorCurso.get(act.curso_id) || [];
    list.push(act);
    actsPorCurso.set(act.curso_id, list);
  }

  const entregasPorCurso = new Map();
  for (const row of entregas) {
    let mapAct = entregasPorCurso.get(row.curso_id);
    if (!mapAct) {
      mapAct = new Map();
      entregasPorCurso.set(row.curso_id, mapAct);
    }
    mapAct.set(row.actividad_id, row);
  }

  for (const cId of cleanCursoIds) {
    const acts = actsPorCurso.get(cId) || [];
    const mapAct = entregasPorCurso.get(cId) || new Map();
    result.set(cId, calcularResumenCalificacion(acts, mapAct));
  }
  return result;
}

router.get('/', async (req, res, next) => {
  try {
    if (req.user.rol === 'INSTRUCTOR') {
      const [rows] = await pool.query(
        `SELECT c.*, u.nombre AS instructor_nombre,
                (SELECT COUNT(*) FROM inscripciones i WHERE i.curso_id = c.id) AS total_estudiantes,
                (SELECT COUNT(*) FROM actividades a WHERE a.curso_id = c.id) AS total_actividades
         FROM cursos c
         INNER JOIN usuarios u ON u.id = c.instructor_id
         WHERE c.instructor_id = ?
         ORDER BY c.updated_at DESC`,
        [req.user.id]
      );
      return res.json(rows.map((r) => ({
        ...r,
        codigo_activo: Boolean(r.codigo_activo),
        total_estudiantes: Number(r.total_estudiantes),
        total_actividades: Number(r.total_actividades),
      })));
    }

    const [rows] = await pool.query(
      `SELECT c.id, c.instructor_id, c.nombre, c.descripcion, c.estado,
              c.created_at, c.updated_at, u.nombre AS instructor_nombre,
              (SELECT COUNT(*) FROM inscripciones i2 WHERE i2.curso_id = c.id) AS total_estudiantes,
              (SELECT COUNT(*) FROM actividades a WHERE a.curso_id = c.id) AS total_actividades
       FROM cursos c
       INNER JOIN inscripciones i ON i.curso_id = c.id
       INNER JOIN usuarios u ON u.id = c.instructor_id
       WHERE i.aprendiz_id = ?
       ORDER BY i.fecha_union DESC`,
      [req.user.id]
    );

    const resumenesPorCurso = await calcularResumenesCursosDeAprendiz(
      rows.map((r) => r.id),
      req.user.id
    );
    const enriched = rows.map((r) => {
      const resumen = resumenesPorCurso.get(r.id);
      return {
        ...r,
        codigo_acceso: null,
        codigo_activo: false,
        total_estudiantes: Number(r.total_estudiantes),
        total_actividades: Number(r.total_actividades),
        nota_0_5: resumen ? resumen.nota_0_5 : null,
        nota_0_100: resumen ? resumen.nota_0_100 : null,
      };
    });
    return res.json(enriched);
  } catch (err) {
    return next(err);
  }
});

router.post('/', requireRole('INSTRUCTOR'), async (req, res, next) => {
  try {
    const { nombre, descripcion = '', estado = 'ACTIVO' } = req.body;
    if (!nombre || typeof nombre !== 'string' || nombre.trim().length < 3 || nombre.trim().length > 120) {
      return res.status(400).json({ error: 'El nombre del curso es obligatorio (entre 3 y 120 caracteres).' });
    }
    const cleanEstado = estado === 'ARCHIVADO' ? 'ARCHIVADO' : 'ACTIVO';
    const codigo = await generarCodigoUnico();
    const now = Date.now();

    const [result] = await pool.query(
      `INSERT INTO cursos (instructor_id, nombre, descripcion, estado, codigo_acceso, codigo_activo, created_at, updated_at)
       VALUES (?, ?, ?, ?, ?, 1, ?, ?)`,
      [req.user.id, nombre.trim(), String(descripcion).trim(), cleanEstado, codigo, now, now]
    );

    return res.status(201).json({
      id: result.insertId,
      instructor_id: req.user.id,
      instructor_nombre: req.user.nombre,
      nombre: nombre.trim(),
      descripcion: String(descripcion).trim(),
      estado: cleanEstado,
      codigo_acceso: codigo,
      codigo_activo: true,
      total_estudiantes: 0,
      total_actividades: 0,
      created_at: now,
      updated_at: now,
    });
  } catch (err) {
    return next(err);
  }
});

router.post('/unirse', joinCourseLimiter, requireRole('APRENDIZ'), async (req, res, next) => {
  try {
    const codigoRaw = req.body?.codigo_acceso;
    if (!codigoRaw || typeof codigoRaw !== 'string' || codigoRaw.trim().length < 4) {
      return res.status(400).json({ error: 'Ingresa un código de acceso válido.' });
    }
    const codigo = codigoRaw.trim().toUpperCase();

    const [cursos] = await pool.query('SELECT * FROM cursos WHERE codigo_acceso = ?', [codigo]);
    if (cursos.length === 0) {
      return res.status(404).json({ error: 'No existe ningún curso con ese código.' });
    }
    const curso = cursos[0];
    if (!curso.codigo_activo) {
      return res.status(403).json({ error: 'El código de acceso de este curso está desactivado.' });
    }
    if (curso.estado !== 'ACTIVO') {
      return res.status(403).json({ error: 'No puedes unirte a un curso archivado.' });
    }

    const [existing] = await pool.query(
      'SELECT id FROM inscripciones WHERE curso_id = ? AND aprendiz_id = ?',
      [curso.id, req.user.id]
    );
    if (existing.length > 0) {
      return res.status(409).json({ error: 'Ya estás inscrito en este curso.' });
    }

    await pool.query(
      'INSERT INTO inscripciones (curso_id, aprendiz_id, fecha_union) VALUES (?, ?, ?)',
      [curso.id, req.user.id, Date.now()]
    );

    return res.status(201).json({ mensaje: 'Te uniste al curso correctamente.', curso_id: curso.id });
  } catch (err) {
    return next(err);
  }
});

router.get('/:cursoId', async (req, res, next) => {
  try {
    const access = await authorizeCourseAccess(req.params.cursoId, req.user);
    if (access.error) return res.status(access.status).json({ error: access.error });

    const [rows] = await pool.query(
      `SELECT c.*, u.nombre AS instructor_nombre,
              (SELECT COUNT(*) FROM inscripciones i WHERE i.curso_id = c.id) AS total_estudiantes,
              (SELECT COUNT(*) FROM actividades a WHERE a.curso_id = c.id) AS total_actividades
       FROM cursos c
       INNER JOIN usuarios u ON u.id = c.instructor_id
       WHERE c.id = ?`,
      [access.curso.id]
    );
    const curso = rows[0];
    const isOwner = access.isOwner;
    const resumen = !isOwner ? await calcularResumenCursoEstudiante(curso.id, req.user.id) : null;

    return res.json({
      id: curso.id,
      instructor_id: curso.instructor_id,
      instructor_nombre: curso.instructor_nombre,
      nombre: curso.nombre,
      descripcion: curso.descripcion,
      estado: curso.estado,
      codigo_acceso: isOwner ? curso.codigo_acceso : null,
      codigo_activo: isOwner ? Boolean(curso.codigo_activo) : false,
      total_estudiantes: Number(curso.total_estudiantes),
      total_actividades: Number(curso.total_actividades),
      nota_0_5: resumen?.nota_0_5 ?? null,
      nota_0_100: resumen?.nota_0_100 ?? null,
      created_at: curso.created_at,
      updated_at: curso.updated_at,
    });
  } catch (err) {
    return next(err);
  }
});

router.put('/:cursoId', async (req, res, next) => {
  try {
    const access = await authorizeCourseAccess(req.params.cursoId, req.user, { requireInstructorOwner: true });
    if (access.error) return res.status(access.status).json({ error: access.error });

    const { nombre, descripcion, estado } = req.body;
    const nextNombre = nombre !== undefined ? String(nombre).trim() : access.curso.nombre;
    if (nextNombre.length < 3 || nextNombre.length > 120) {
      return res.status(400).json({ error: 'El nombre del curso debe tener entre 3 y 120 caracteres.' });
    }
    const nextDesc = descripcion !== undefined ? String(descripcion).trim() : access.curso.descripcion;
    const nextEstado = estado === 'ARCHIVADO' ? 'ARCHIVADO' : (estado === 'ACTIVO' ? 'ACTIVO' : access.curso.estado);
    const now = Date.now();

    await pool.query(
      'UPDATE cursos SET nombre = ?, descripcion = ?, estado = ?, updated_at = ? WHERE id = ?',
      [nextNombre, nextDesc, nextEstado, now, access.curso.id]
    );

    return res.json({
      ...access.curso,
      nombre: nextNombre,
      descripcion: nextDesc,
      estado: nextEstado,
      codigo_activo: Boolean(access.curso.codigo_activo),
      updated_at: now,
    });
  } catch (err) {
    return next(err);
  }
});

router.delete('/:cursoId', async (req, res, next) => {
  try {
    const access = await authorizeCourseAccess(req.params.cursoId, req.user, { requireInstructorOwner: true });
    if (access.error) return res.status(access.status).json({ error: access.error });

    // Limpiar archivos físicos asociados antes de borrar en cascada
    const [adjuntos] = await pool.query(
      `SELECT aa.nombre_archivo FROM actividad_adjuntos aa
       INNER JOIN actividades a ON a.id = aa.actividad_id WHERE a.curso_id = ?`,
      [access.curso.id]
    );
    const [entregas] = await pool.query(
      `SELECT ea.nombre_archivo FROM entrega_archivos ea
       INNER JOIN entregas e ON e.id = ea.entrega_id
       INNER JOIN actividades a ON a.id = e.actividad_id WHERE a.curso_id = ?`,
      [access.curso.id]
    );
    const [materiales] = await pool.query(
      "SELECT nombre_archivo FROM material_apoyo WHERE curso_id = ? AND tipo = 'ARCHIVO'",
      [access.curso.id]
    );

    await pool.query('DELETE FROM cursos WHERE id = ?', [access.curso.id]);

    adjuntos.forEach((f) => safeUnlink('actividades', f.nombre_archivo));
    entregas.forEach((f) => safeUnlink('entregas', f.nombre_archivo));
    materiales.forEach((f) => safeUnlink('materiales', f.nombre_archivo));

    return res.status(204).send();
  } catch (err) {
    return next(err);
  }
});

router.post('/:cursoId/codigo/regenerar', async (req, res, next) => {
  try {
    const access = await authorizeCourseAccess(req.params.cursoId, req.user, { requireInstructorOwner: true });
    if (access.error) return res.status(access.status).json({ error: access.error });

    const nuevoCodigo = await generarCodigoUnico();
    const now = Date.now();
    await pool.query(
      'UPDATE cursos SET codigo_acceso = ?, codigo_activo = 1, updated_at = ? WHERE id = ?',
      [nuevoCodigo, now, access.curso.id]
    );
    return res.json({ codigo_acceso: nuevoCodigo, codigo_activo: true });
  } catch (err) {
    return next(err);
  }
});

router.patch('/:cursoId/codigo/estado', async (req, res, next) => {
  try {
    const access = await authorizeCourseAccess(req.params.cursoId, req.user, { requireInstructorOwner: true });
    if (access.error) return res.status(access.status).json({ error: access.error });

    const activo = Boolean(req.body?.codigo_activo);
    await pool.query(
      'UPDATE cursos SET codigo_activo = ?, updated_at = ? WHERE id = ?',
      [activo ? 1 : 0, Date.now(), access.curso.id]
    );
    return res.json({ codigo_acceso: access.curso.codigo_acceso, codigo_activo: activo });
  } catch (err) {
    return next(err);
  }
});

router.delete('/:cursoId/salirse', requireRole('APRENDIZ'), async (req, res, next) => {
  try {
    const access = await authorizeCourseAccess(req.params.cursoId, req.user);
    if (access.error) return res.status(access.status).json({ error: access.error });

    await pool.query(
      'DELETE FROM inscripciones WHERE curso_id = ? AND aprendiz_id = ?',
      [access.curso.id, req.user.id]
    );
    return res.status(204).send();
  } catch (err) {
    return next(err);
  }
});

router.get('/:cursoId/personas', async (req, res, next) => {
  try {
    const access = await authorizeCourseAccess(req.params.cursoId, req.user);
    if (access.error) return res.status(access.status).json({ error: access.error });

    const [instRows] = await pool.query(
      'SELECT id, nombre, email, rol FROM usuarios WHERE id = ?',
      [access.curso.instructor_id]
    );
    const [aprendices] = await pool.query(
      `SELECT u.id, u.nombre, u.email, u.rol, i.fecha_union
       FROM inscripciones i
       INNER JOIN usuarios u ON u.id = i.aprendiz_id
       WHERE i.curso_id = ?
       ORDER BY u.nombre ASC`,
      [access.curso.id]
    );

    // PERF-04: Calcular en lote las notas de los aprendices visibles en 2 consultas SQL fijas
    const idsVisibles = access.isOwner
      ? aprendices.map((est) => est.id)
      : aprendices.filter((est) => est.id === req.user.id).map((est) => est.id);
    const resumenesMap = await calcularResumenesCursoPorAprendiz(access.curso.id, idsVisibles);

    const estudiantesConNota = aprendices.map((est) => {
      const resumen = resumenesMap.get(est.id);
      if (resumen) {
        return {
          ...est,
          nota_0_5: resumen.nota_0_5,
          nota_0_100: resumen.nota_0_100,
        };
      }
      return {
        ...est,
        nota_0_5: null,
        nota_0_100: null,
      };
    });

    return res.json({
      instructor: instRows[0],
      aprendices: estudiantesConNota,
    });
  } catch (err) {
    return next(err);
  }
});

router.delete('/:cursoId/personas/:aprendizId', async (req, res, next) => {
  try {
    const access = await authorizeCourseAccess(req.params.cursoId, req.user, { requireInstructorOwner: true });
    if (access.error) return res.status(access.status).json({ error: access.error });

    const aprendizId = Number(req.params.aprendizId);
    if (!Number.isInteger(aprendizId) || aprendizId <= 0) {
      return res.status(400).json({ error: 'ID de aprendiz inválido.' });
    }

    const [result] = await pool.query(
      'DELETE FROM inscripciones WHERE curso_id = ? AND aprendiz_id = ?',
      [access.curso.id, aprendizId]
    );
    if (result.affectedRows === 0) {
      return res.status(404).json({ error: 'El aprendiz no pertenece a este curso.' });
    }
    return res.status(204).send();
  } catch (err) {
    return next(err);
  }
});

router.get('/:cursoId/calificaciones', async (req, res, next) => {
  try {
    const access = await authorizeCourseAccess(req.params.cursoId, req.user);
    if (access.error) return res.status(access.status).json({ error: access.error });

    if (req.user.rol === 'APRENDIZ' && req.query.tabla !== '1') {
      const resumen = await calcularResumenCursoEstudiante(access.curso.id, req.user.id);
      return res.json({
        aprendiz: { id: req.user.id, nombre: req.user.nombre, email: req.user.email },
        ...resumen,
      });
    }

    const [aprendices] = await pool.query(
      `SELECT u.id, u.nombre, u.email
       FROM inscripciones i
       INNER JOIN usuarios u ON u.id = i.aprendiz_id
       WHERE i.curso_id = ?
       ORDER BY u.nombre ASC`,
      [access.curso.id]
    );

    // PERF-04: Obtener el reporte de toda la clase en lote sin N+1 consultas
    const resumenesMap = await calcularResumenesCursoPorAprendiz(
      access.curso.id,
      aprendices.map((est) => est.id)
    );
    const reporte = aprendices.map((est) => {
      const resumen = resumenesMap.get(est.id) || calcularResumenCalificacion([], new Map());
      return { aprendiz: est, ...resumen };
    });

    return res.json(reporte);
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
