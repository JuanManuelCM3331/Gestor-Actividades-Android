const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const multer = require('multer');
const { MAX_FILE_SIZE_BYTES, MAX_FILE_SIZE_MB, UPLOAD_ROOT } = require('../config/env');

const ALLOWED_EXTENSIONS = new Set([
  // Documentos
  '.pdf', '.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx', '.txt', '.csv',
  // Imágenes
  '.jpg', '.jpeg', '.png', '.webp', '.gif',
  // Audio y video
  '.mp3', '.wav', '.m4a', '.aac', '.ogg', '.opus', '.3gp', '.amr', '.mp4', '.webm',
  // Comprimidos
  '.zip', '.rar',
]);

const ALLOWED_MIMES = new Set([
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'application/vnd.ms-powerpoint',
  'application/vnd.openxmlformats-officedocument.presentationml.presentation',
  'text/plain',
  'text/csv',
  'image/jpeg',
  'image/png',
  'image/webp',
  'image/gif',
  'audio/mpeg',
  'audio/mp3',
  'audio/wav',
  'audio/x-wav',
  'audio/mp4',
  'audio/m4a',
  'audio/x-m4a',
  'audio/aac',
  'audio/ogg',
  'audio/opus',
  'audio/3gpp',
  'audio/amr',
  'video/3gpp',
  'video/mp4',
  'video/webm',
  'application/zip',
  'application/x-zip-compressed',
  'application/vnd.rar',
  'application/x-rar-compressed',
  'application/octet-stream', // Validado junto con extensión estricta
]);

function ensureDir(dirPath) {
  if (!fs.existsSync(dirPath)) {
    fs.mkdirSync(dirPath, { recursive: true });
  }
}

function createMulterUpload(subfolder) {
  const destDir = path.join(UPLOAD_ROOT, subfolder);
  ensureDir(destDir);

  const storage = multer.diskStorage({
    destination: (_req, _file, cb) => {
      ensureDir(destDir);
      cb(null, destDir);
    },
    filename: (_req, file, cb) => {
      const ext = path.extname(file.originalname || '').toLowerCase();
      const safeName = `${crypto.randomUUID()}${ext}`;
      cb(null, safeName);
    },
  });

  return multer({
    storage,
    limits: {
      fileSize: MAX_FILE_SIZE_BYTES,
      files: 10,
    },
    fileFilter: (_req, file, cb) => {
      const ext = path.extname(file.originalname || '').toLowerCase();
      const mime = (file.mimetype || '').toLowerCase();

      if (!ALLOWED_EXTENSIONS.has(ext)) {
        return cb(new Error(`Tipo de archivo no permitido (${ext || 'sin extensión'}).`));
      }
      if (!ALLOWED_MIMES.has(mime)) {
        return cb(new Error(`MIME type no permitido (${mime}).`));
      }
      return cb(null, true);
    },
  });
}

function safeUnlink(subfolder, filename) {
  if (!filename) return;
  const safeBase = path.basename(filename);
  const fullPath = path.join(UPLOAD_ROOT, subfolder, safeBase);
  if (fs.existsSync(fullPath)) {
    fs.unlink(fullPath, () => {});
  }
}

function cleanupUploadedFiles(subfolder, files) {
  if (!Array.isArray(files)) return;
  for (const file of files) {
    safeUnlink(subfolder, file.filename);
  }
}

module.exports = {
  ALLOWED_EXTENSIONS,
  MAX_FILE_SIZE_MB,
  MAX_FILE_SIZE_BYTES,
  UPLOAD_ROOT,
  createMulterUpload,
  safeUnlink,
  cleanupUploadedFiles,
};
