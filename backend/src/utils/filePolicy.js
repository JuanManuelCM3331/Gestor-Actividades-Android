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
  'application/octet-stream', // Validado junto con extensión estricta y magic bytes
]);

function ensureDir(dirPath) {
  if (!fs.existsSync(dirPath)) {
    fs.mkdirSync(dirPath, { recursive: true });
  }
}

/**
 * SEC-04: Inspección de Magic Bytes (firma binaria) para detectar ejecutables o scripts
 * camuflados con extensión permitida (ej. un .exe renombrado a .pdf o un script HTML/JS camuflado).
 */
function hasDangerousMagicBytes(filePath, ext) {
  try {
    const fd = fs.openSync(filePath, 'r');
    const buffer = Buffer.alloc(64);
    const bytesRead = fs.readSync(fd, buffer, 0, 64, 0);
    fs.closeSync(fd);

    if (bytesRead === 0) return 'El archivo subido está vacío (0 bytes).';
    const slice = buffer.subarray(0, bytesRead);

    // 1. Ejecutables Windows PE/MZ (0x4D 0x5A = 'MZ')
    if (bytesRead >= 2 && slice[0] === 0x4d && slice[1] === 0x5a) {
      return 'Contenido ejecutable (PE/MZ) detectado y bloqueado por seguridad.';
    }

    // 2. Ejecutables Linux ELF (0x7F 'E' 'L' 'F')
    if (
      bytesRead >= 4 &&
      slice[0] === 0x7f &&
      slice[1] === 0x45 &&
      slice[2] === 0x4c &&
      slice[3] === 0x46
    ) {
      return 'Binario ejecutable (ELF) detectado y bloqueado por seguridad.';
    }

    // 3. Mach-O binarios
    if (
      bytesRead >= 4 &&
      ((slice[0] === 0xfe && slice[1] === 0xed && slice[2] === 0xfa && (slice[3] === 0xce || slice[3] === 0xcf)) ||
        (slice[0] === 0xcf && slice[1] === 0xfa && slice[2] === 0xed && slice[3] === 0xfe))
    ) {
      return 'Binario ejecutable (Mach-O) detectado y bloqueado por seguridad.';
    }

    // 4. Scripts ejecutables o payloads HTML/XSS camuflados en documentos/imágenes
    const headAscii = slice.toString('utf8').trimStart().toLowerCase();
    if (
      headAscii.startsWith('#!/') ||
      headAscii.startsWith('<?php') ||
      headAscii.startsWith('<script') ||
      (ext !== '.txt' && (headAscii.startsWith('<!doctype html') || headAscii.startsWith('<html')))
    ) {
      return 'Script o contenido HTML ejecutable camuflado bloqueado por seguridad.';
    }

    return null;
  } catch (_err) {
    return 'No se pudo verificar la integridad binaria del archivo.';
  }
}

function validateUploadedFilesMagicBytes(subfolder, files) {
  const list = Array.isArray(files) ? files : (files ? [files] : []);
  for (const file of list) {
    const ext = path.extname(file.originalname || '').toLowerCase();
    const fullPath = file.path || path.join(UPLOAD_ROOT, subfolder, path.basename(file.filename || ''));
    const errorMsg = hasDangerousMagicBytes(fullPath, ext);
    if (errorMsg) {
      cleanupUploadedFiles(subfolder, list);
      const err = new Error(`Tipo de archivo no permitido: ${errorMsg}`);
      err.code = 'INVALID_FILE_TYPE';
      return err;
    }
  }
  return null;
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

  const baseMulter = multer({
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

  return {
    single(fieldName) {
      const mw = baseMulter.single(fieldName);
      return (req, res, next) => {
        mw(req, res, (err) => {
          if (err) return next(err);
          if (req.file) {
            const magicErr = validateUploadedFilesMagicBytes(subfolder, [req.file]);
            if (magicErr) return next(magicErr);
          }
          return next();
        });
      };
    },
    array(fieldName, maxCount) {
      const mw = baseMulter.array(fieldName, maxCount);
      return (req, res, next) => {
        mw(req, res, (err) => {
          if (err) return next(err);
          if (req.files && req.files.length > 0) {
            const magicErr = validateUploadedFilesMagicBytes(subfolder, req.files);
            if (magicErr) return next(magicErr);
          }
          return next();
        });
      };
    },
  };
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
  hasDangerousMagicBytes,
  validateUploadedFilesMagicBytes,
};
