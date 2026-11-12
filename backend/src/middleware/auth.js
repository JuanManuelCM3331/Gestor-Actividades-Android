const jwt = require('jsonwebtoken');
const { pool } = require('../config/db');
const { JWT_SECRET } = require('../config/env');

async function authenticate(req, res, next) {
  try {
    const authHeader = req.headers.authorization || '';
    if (!authHeader.startsWith('Bearer ')) {
      return res.status(401).json({ error: 'Debes iniciar sesión para continuar.' });
    }
    const token = authHeader.slice(7).trim();
    const payload = jwt.verify(token, JWT_SECRET);

    const [rows] = await pool.query(
      'SELECT id, nombre, email, rol FROM usuarios WHERE id = ?',
      [payload.sub]
    );
    if (rows.length === 0) {
      return res.status(401).json({ error: 'Sesión inválida o usuario inexistente.' });
    }
    req.user = rows[0];
    return next();
  } catch (err) {
    return res.status(401).json({ error: 'Token inválido o expirado.' });
  }
}

function requireRole(...roles) {
  return (req, res, next) => {
    if (!req.user || !roles.includes(req.user.rol)) {
      return res.status(403).json({ error: 'No tienes permisos para realizar esta acción.' });
    }
    return next();
  };
}

async function authorizeCourseAccess(cursoId, user, { requireInstructorOwner = false } = {}) {
  const id = Number(cursoId);
  if (!Number.isInteger(id) || id <= 0) {
    return { status: 400, error: 'ID de curso inválido.' };
  }

  const [cursoRows] = await pool.query('SELECT * FROM cursos WHERE id = ?', [id]);
  if (cursoRows.length === 0) {
    return { status: 404, error: 'Curso no encontrado.' };
  }

  const curso = cursoRows[0];
  const isOwner = curso.instructor_id === user.id;

  if (requireInstructorOwner) {
    if (user.rol !== 'INSTRUCTOR' || !isOwner) {
      return { status: 403, error: 'Solo el instructor propietario puede administrar este curso.' };
    }
    return { curso, isOwner: true };
  }

  if (user.rol === 'INSTRUCTOR') {
    if (!isOwner) {
      return { status: 403, error: 'No tienes acceso a cursos de otros instructores.' };
    }
    return { curso, isOwner: true };
  }

  const [inscripciones] = await pool.query(
    'SELECT id FROM inscripciones WHERE curso_id = ? AND aprendiz_id = ?',
    [id, user.id]
  );
  if (inscripciones.length === 0) {
    return { status: 403, error: 'No estás inscrito en este curso.' };
  }
  return { curso, isOwner: false };
}

module.exports = {
  authenticate,
  requireRole,
  authorizeCourseAccess,
};
