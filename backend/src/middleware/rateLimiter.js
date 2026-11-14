/**
 * SEC-03: Middleware de Rate Limiting en memoria (ventana deslizante)
 * para mitigar ataques de fuerza bruta en login, registro y códigos de curso,
 * así como abuso general de la API.
 */
function createRateLimiter({
  windowMs = 15 * 60 * 1000,
  max = 20,
  message = 'Demasiadas solicitudes. Por favor intenta nuevamente más tarde.',
  keyGenerator = (req) => req.ip || req.socket?.remoteAddress || 'anon',
} = {}) {
  const hits = new Map();

  // Limpieza periódica de entradas expiradas para evitar fugas de memoria
  const cleanupTimer = setInterval(() => {
    const now = Date.now();
    for (const [key, entry] of hits.entries()) {
      if (now >= entry.resetAt) {
        hits.delete(key);
      }
    }
  }, Math.min(windowMs, 60_000));

  if (typeof cleanupTimer.unref === 'function') {
    cleanupTimer.unref();
  }

  return function rateLimiterMiddleware(req, res, next) {
    const now = Date.now();
    const key = String(keyGenerator(req) || 'anon');
    let entry = hits.get(key);

    if (!entry || now >= entry.resetAt) {
      entry = { count: 0, resetAt: now + windowMs };
      hits.set(key, entry);
    }

    entry.count += 1;
    const remaining = Math.max(0, max - entry.count);
    const retryAfterSec = Math.max(1, Math.ceil((entry.resetAt - now) / 1000));

    res.setHeader('RateLimit-Limit', String(max));
    res.setHeader('RateLimit-Remaining', String(remaining));
    res.setHeader('RateLimit-Reset', String(retryAfterSec));

    if (entry.count > max) {
      res.setHeader('Retry-After', String(retryAfterSec));
      return res.status(429).json({ error: message });
    }

    return next();
  };
}

module.exports = {
  createRateLimiter,
};
