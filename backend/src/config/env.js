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

module.exports = {
  JWT_SECRET,
  JWT_EXPIRES_IN: process.env.JWT_EXPIRES_IN || '7d',
  MAX_FILE_SIZE_MB,
  MAX_FILE_SIZE_BYTES,
  UPLOAD_ROOT,
};
