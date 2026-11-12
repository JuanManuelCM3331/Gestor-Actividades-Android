require('dotenv').config();
const fs = require('fs');
const path = require('path');
const mysql = require('mysql2/promise');

async function initDatabase() {
  const schemaPath = path.join(__dirname, '..', '..', 'sql', 'schema.sql');
  const sql = fs.readFileSync(schemaPath, 'utf8');

  const connection = await mysql.createConnection({
    host: process.env.DB_HOST || '127.0.0.1',
    port: Number(process.env.DB_PORT || 3306),
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD ?? '',
    multipleStatements: true,
  });

  try {
    await connection.query(sql);
    console.log('Base de datos MySQL inicializada correctamente desde schema.sql');
  } finally {
    await connection.end();
  }
}

initDatabase().catch((err) => {
  console.error('Error al inicializar la base de datos MySQL:', err.message);
  process.exit(1);
});
