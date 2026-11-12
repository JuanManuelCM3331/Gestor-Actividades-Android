const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const { checkDatabaseConnection } = require('./config/db');
const categoriasRouter = require('./routes/categorias');
const reportesRouter = require('./routes/reportes');
const actividadesRouter = require('./routes/actividades');

const app = express();

app.use(helmet());
app.use(cors());
app.use(express.json({ limit: '2mb' }));

// Autenticación opcional por Bearer Token (activa cuando se define API_BEARER_TOKEN en producción)
app.use('/api', (req, res, next) => {
  if (req.path === '/health') return next();
  const configuredToken = process.env.API_BEARER_TOKEN;
  if (!configuredToken) return next();

  const authHeader = req.headers.authorization || '';
  if (authHeader !== `Bearer ${configuredToken}`) {
    return res.status(401).json({ error: 'Token inválido o ausente.' });
  }
  return next();
});

app.get('/api/health', async (_req, res) => {
  try {
    await checkDatabaseConnection();
    res.json({ status: 'ok', database: 'mysql', timestamp: Date.now() });
  } catch (err) {
    res.status(503).json({
      status: 'error',
      database: 'unavailable',
      message: err.message,
    });
  }
});

app.use('/api/categorias', categoriasRouter);
app.use('/api/reportes', reportesRouter);
app.use('/api/actividades', actividadesRouter);

app.use((_req, res) => {
  res.status(404).json({ error: 'Endpoint no encontrado.' });
});

app.use((err, _req, res, _next) => {
  console.error('[API Error]', err);
  res.status(500).json({ error: 'Error interno del servidor.' });
});

module.exports = app;
