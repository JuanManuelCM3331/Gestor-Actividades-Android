const jwt = require('jsonwebtoken');
const { pool } = require('../config/db');
const { JWT_SECRET } = require('../config/env');

// SEC-05: Almacén en memoria de tokens revocados (por jti o firma) con expiración automática
const revokedTokens = new Map();

function revokeToken(key, expSeconds) {
  if (!key) return;
  const expiresAtMs = expSeconds ? expSeconds * 1000 : Date.now() + 12 * 3600 * 1000;
  revokedTokens.set(key, expiresAtMs);

  const now = Date.now();
  for (const [k, exp] of revokedTokens.entries()) {
    if (now >= exp) revokedTokens.delete(k);
  }
}

function isTokenRevoked(key) {
  if (!key) return false;
  const exp = revokedTokens.get(key);
  if (!exp) return false;
  if (Date.now() >= exp) {
    revokedTokens.delete(key);
    return false;
  }
  return true;
}

async function authenticate(req, res, next) {
  try {
    const authHeader = req.headers.authorization || '';
    if (!authHeader.startsWith('Bearer ')) {
      return res.status(401).json({ error: 'Debes iniciar sesión para continuar.' });
    }
    const token = authHeader.slice(7).trim();
    const payload = jwt.verify(token, JWT_SECRET);

    const tokenKey = payload.jti || token;
    if (isTokenRevoked(tokenKey)) {
      return res.status(401).json({ error: 'La sesión fue cerrada. Inicia sesión nuevamente.' });
    }

    const [rows] = await pool.query(
      'SELECT id, nombre, email, rol FROM usuarios WHERE id = ?',
      [payload.sub]
    );
    if (rows.length === 0) {
      return res.status(401).json({ error: 'Sesión inválida o usuario inexistente.' });
    }
    req.user = rows[0];
    req.tokenPayload = payload;
    req.rawToken = token;
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

/**
 * SEC-04: Middleware para autorizar acceso al curso ANTES de que Multer escriba archivos en disco.
 */
function requireCourseAccessMiddleware(paramOrOptions = 'cursoId', maybeOptions = {}) {
  const paramName = typeof paramOrOptions === 'string'
    ? paramOrOptions
    : (paramOrOptions?.paramName || 'cursoId');
  const requireInstructorOwner = typeof paramOrOptions === 'object' && paramOrOptions !== null
    ? Boolean(paramOrOptions.requireInstructorOwner)
    : Boolean(maybeOptions?.requireInstructorOwner);

  return async (req, res, next) => {
    try {
      const access = await authorizeCourseAccess(req.params[paramName], req.user, {
        requireInstructorOwner,
      });
      if (access.error) {
        return res.status(access.status).json({ error: access.error });
      }
      req.courseAccess = access;
      return next();
    } catch (err) {
      return next(err);
    }
  };
}

module.exports = {
  authenticate,
  requireRole,
  authorizeCourseAccess,
  requireCourseAccessMiddleware,
  revokeToken,
  isTokenRevoked,
};
