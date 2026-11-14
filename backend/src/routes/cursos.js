const express = require('express');
const crypto = require('crypto');
const { pool } = require('../config/db');
const { authenticate, requireRole, authorizeCourseAccess } = require('../middleware/auth');
const { calcularResumenCalificacion } = require('../utils/grades');
const { safeUnlink } = require('../utils/filePolicy');

const router = express.Router();
router.use(authenticate);

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
  const [actividades] = await pool.query(
    'SELECT id, titulo, fecha_limite, puntaje_maximo FROM actividades WHERE curso_id = ? ORDER BY fecha_limite ASC',
    [cursoId]
  );
  const [entregas] = await pool.query(
    `SELECT e.* FROM entregas e
     INNER JOIN actividades a ON e.actividad_id = a.id
     WHERE a.curso_id = ? AND e.aprendiz_id = ?`,
    [cursoId, aprendizId]
  );
  const map = new Map();
  for (const row of entregas) {
    map.set(row.actividad_id, row);
  }
  return calcularResumenCalificacion(actividades, map);
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

    const enriched = await Promise.all(
      rows.map(async (r) => {
        const resumen = await calcularResumenCursoEstudiante(r.id, req.user.id);
        return {
          ...r,
          codigo_acceso: null,
          codigo_activo: false,
          total_estudiantes: Number(r.total_estudiantes),
          total_actividades: Number(r.total_actividades),
          nota_0_5: resumen.nota_0_5,
          nota_0_100: resumen.nota_0_100,
        };
      })
    );
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

router.post('/unirse', requireRole('APRENDIZ'), async (req, res, next) => {
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

    const estudiantesConNota = await Promise.all(
      aprendices.map(async (est) => {
        if (access.isOwner || est.id === req.user.id) {
          const resumen = await calcularResumenCursoEstudiante(access.curso.id, est.id);
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
      })
    );

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

    const reporte = await Promise.all(
      aprendices.map(async (est) => {
        const resumen = await calcularResumenCursoEstudiante(access.curso.id, est.id);
        return { aprendiz: est, ...resumen };
      })
    );

    return res.json(reporte);
  } catch (err) {
    return next(err);
  }
});

module.exports = router;
