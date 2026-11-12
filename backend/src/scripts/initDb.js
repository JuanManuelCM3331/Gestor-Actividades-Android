require('dotenv').config();
const fs = require('fs');
const path = require('path');
const bcrypt = require('bcryptjs');
const mysql = require('mysql2/promise');

async function seedDemoData(connection) {
  const now = Date.now();
  const instructorHash = await bcrypt.hash('Instructor123!', 10);
  const aprendizHash = await bcrypt.hash('Aprendiz123!', 10);

  const [instRes] = await connection.query(
    'INSERT INTO usuarios (nombre, email, password_hash, rol) VALUES (?, ?, ?, ?)',
    ['Laura Gómez (Instructora)', 'instructor@ctma.edu.co', instructorHash, 'INSTRUCTOR']
  );
  const instructorId = instRes.insertId;

  const [ap1Res] = await connection.query(
    'INSERT INTO usuarios (nombre, email, password_hash, rol) VALUES (?, ?, ?, ?)',
    ['Carlos Pérez', 'aprendiz1@ctma.edu.co', aprendizHash, 'APRENDIZ']
  );
  const aprendiz1Id = ap1Res.insertId;

  const [ap2Res] = await connection.query(
    'INSERT INTO usuarios (nombre, email, password_hash, rol) VALUES (?, ?, ?, ?)',
    ['María Rodríguez', 'aprendiz2@ctma.edu.co', aprendizHash, 'APRENDIZ']
  );
  const aprendiz2Id = ap2Res.insertId;

  const [cursoRes] = await connection.query(
    `INSERT INTO cursos (instructor_id, nombre, descripcion, estado, codigo_acceso, codigo_activo, created_at, updated_at)
     VALUES (?, ?, ?, 'ACTIVO', 'CTMA2026', 1, ?, ?)`,
    [
      instructorId,
      'Desarrollo de Aplicaciones Móviles ADSO',
      'Curso práctico de arquitectura MVVM, APIs REST en Node.js y persistencia en MySQL y Room.',
      now,
      now,
    ]
  );
  const cursoId = cursoRes.insertId;

  await connection.query(
    'INSERT INTO inscripciones (curso_id, aprendiz_id, fecha_union) VALUES (?, ?, ?), (?, ?, ?)',
    [cursoId, aprendiz1Id, now, cursoId, aprendiz2Id, now]
  );

  const enCincoDias = now + 5 * 24 * 60 * 60 * 1000;
  const enDiezDias = now + 10 * 24 * 60 * 60 * 1000;

  const [act1Res] = await connection.query(
    `INSERT INTO actividades (curso_id, titulo, descripcion, fecha_limite, puntaje_maximo, created_at, updated_at)
     VALUES (?, ?, ?, ?, 5.00, ?, ?)`,
    [
      cursoId,
      'Taller 1: Diseño de Base de Datos Relacional',
      'Construir el modelo entidad-relación normalizado hasta 3FN y adjuntar script SQL en PDF o ZIP.',
      enCincoDias,
      now,
      now,
    ]
  );

  await connection.query(
    `INSERT INTO actividades (curso_id, titulo, descripcion, fecha_limite, puntaje_maximo, created_at, updated_at)
     VALUES (?, ?, ?, ?, 5.00, ?, ?)`,
    [
      cursoId,
      'Reto 2: Consumo de API REST con Hilt y Retrofit',
      'Implementar el repositorio de cursos con manejo de errores, estado Loading y caché en Room.',
      enDiezDias,
      now,
      now,
    ]
  );

  // Entrega de muestra para aprendiz 1 en Taller 1
  await connection.query(
    `INSERT INTO entregas (actividad_id, aprendiz_id, descripcion, nota, retroalimentacion, exenta, estado, fecha_entrega, fecha_calificacion, updated_at)
     VALUES (?, ?, ?, 4.50, ?, 0, 'CALIFICADA', ?, ?, ?)`,
    [
      act1Res.insertId,
      aprendiz1Id,
      'Adjunto diagrama ER normalizado y script SQL validado en MySQL.',
      'Excelente normalización e integridad referencial en llaves foráneas.',
      now,
      now,
      now,
    ]
  );

  await connection.query(
    `INSERT INTO material_apoyo (curso_id, titulo, descripcion, tipo, enlace_url, created_at)
     VALUES (?, ?, ?, 'ENLACE', ?, ?)`,
    [
      cursoId,
      'Guía oficial de Arquitectura Android (MVVM)',
      'Documentación recomendada sobre ViewModel, StateFlow y capa de datos.',
      'https://developer.android.com/topic/architecture',
      now,
    ]
  );

  const [hiloPubRes] = await connection.query(
    `INSERT INTO foro_hilos (curso_id, autor_id, aprendiz_objetivo_id, tipo, titulo, contenido, created_at)
     VALUES (?, ?, NULL, 'PUBLICO', ?, ?, ?)`,
    [
      cursoId,
      instructorId,
      'Bienvenida al curso ADSO CTMA',
      'Bienvenidos aprendices. Usen este foro público para dudas generales del curso y el foro privado para consultas personales.',
      now,
    ]
  );

  await connection.query(
    `INSERT INTO foro_mensajes (hilo_id, autor_id, contenido, created_at)
     VALUES (?, ?, ?, ?)`,
    [
      hiloPubRes.insertId,
      aprendiz1Id,
      '¡Muchas gracias instructora! Ya revisé el Taller 1.',
      now + 1000,
    ]
  );

  await connection.query(
    `INSERT INTO foro_hilos (curso_id, autor_id, aprendiz_objetivo_id, tipo, titulo, contenido, created_at)
     VALUES (?, ?, ?, 'PRIVADO', ?, ?, ?)`,
    [
      cursoId,
      aprendiz1Id,
      aprendiz1Id,
      'Consulta privada sobre entrega del Taller 1',
      'Hola instructora, ¿puedo subir los scripts adicionales en formato ZIP?',
      now + 2000,
    ]
  );
}

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
    await seedDemoData(connection);
    console.log('Base de datos MySQL inicializada y poblada correctamente.');
    console.log('Cuentas demo -> Instructor: instructor@ctma.edu.co / Instructor123!');
    console.log('Cuentas demo -> Aprendiz 1: aprendiz1@ctma.edu.co / Aprendiz123!');
    console.log('Cuentas demo -> Aprendiz 2: aprendiz2@ctma.edu.co / Aprendiz123!');
    console.log('Código de acceso del curso demo: CTMA2026');
  } finally {
    await connection.end();
  }
}

initDatabase().catch((err) => {
  console.error('Error al inicializar la base de datos MySQL:', err.message);
  process.exit(1);
});
