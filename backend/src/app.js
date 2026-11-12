const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const multer = require('multer');
const { checkDatabaseConnection } = require('./config/db');
const { MAX_FILE_SIZE_MB } = require('./config/env');
const authRouter = require('./routes/auth');
const cursosRouter = require('./routes/cursos');
const actividadesRouter = require('./routes/actividades');
const materialesRouter = require('./routes/materiales');
const foroRouter = require('./routes/foro');
const archivosRouter = require('./routes/archivos');

const app = express();

app.use(helmet());
app.use(cors());
app.use(express.json({ limit: '5mb' }));

app.get('/api/health', async (_req, res) => {
  try {
    await checkDatabaseConnection();
    res.json({ status: 'ok', database: 'mysql', maxFileSizeMb: MAX_FILE_SIZE_MB, timestamp: Date.now() });
  } catch (err) {
    res.status(503).json({
      status: 'error',
      database: 'unavailable',
      message: err.message,
    });
  }
});

app.use('/api/auth', authRouter);
app.use('/api/cursos', cursosRouter);
app.use('/api/actividades', actividadesRouter);
app.use('/api/materiales', materialesRouter);
app.use('/api/foro', foroRouter);
app.use('/api/archivos', archivosRouter);

app.use((_req, res) => {
  res.status(404).json({ error: 'Endpoint no encontrado.' });
});

app.use((err, _req, res, _next) => {
  if (err instanceof multer.MulterError) {
    if (err.code === 'LIMIT_FILE_SIZE') {
      return res.status(400).json({
        error: `Cada archivo puede pesar máximo ${MAX_FILE_SIZE_MB} MB.`,
      });
    }
    if (err.code === 'LIMIT_FILE_COUNT') {
      return res.status(400).json({ error: 'Se excedió la cantidad máxima de archivos por carga (10).' });
    }
    return res.status(400).json({ error: `Error al procesar archivo: ${err.message}` });
  }

  if (err && (err.code === 'INVALID_FILE_TYPE' || (typeof err.message === 'string' && (
    err.message.startsWith('Tipo de archivo no permitido') ||
    err.message.startsWith('MIME type no permitido')
  )))) {
    return res.status(400).json({ error: err.message });
  }

  console.error('[API Error]', err);
  return res.status(500).json({ error: 'Error interno del servidor.' });
});

module.exports = app;
