const path = require('path');

const MAX_FILE_SIZE_MB = Number(process.env.MAX_FILE_SIZE_MB || 20);
const MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024;
const UPLOAD_ROOT = path.resolve(__dirname, '..', '..', 'uploads');

const JWT_SECRET = process.env.JWT_SECRET || (
  process.env.NODE_ENV === 'production'
    ? ''
    : 'ctma_dev_only_fallback_secret_change_me'
);

if (!JWT_SECRET) {
  throw new Error('JWT_SECRET debe configurarse en variables de entorno para producción.');
}

// SEC-02: Código de invitación institucional y dominios autorizados para el rol INSTRUCTOR
const INSTRUCTOR_INVITE_CODE = process.env.INSTRUCTOR_INVITE_CODE || 'CTMA-DOCENTE-2026';
const ALLOWED_INSTRUCTOR_DOMAINS = (
  process.env.ALLOWED_INSTRUCTOR_DOMAINS ||
  'sena.edu.co,misena.edu.co,ctma.edu.co,test.edu.co'
)
  .split(',')
  .map((d) => d.trim().toLowerCase())
  .filter(Boolean);

// SEC-05: Orígenes CORS permitidos en producción
const CORS_ORIGINS = process.env.CORS_ORIGINS
  ? process.env.CORS_ORIGINS.split(',').map((o) => o.trim()).filter(Boolean)
  : null;

module.exports = {
  JWT_SECRET,
  // SEC-05: Reducir ventana de validez del JWT de 7 días a 12 horas por defecto
  JWT_EXPIRES_IN: process.env.JWT_EXPIRES_IN || '12h',
  MAX_FILE_SIZE_MB,
  MAX_FILE_SIZE_BYTES,
  UPLOAD_ROOT,
  INSTRUCTOR_INVITE_CODE,
  ALLOWED_INSTRUCTOR_DOMAINS,
  CORS_ORIGINS,
};
