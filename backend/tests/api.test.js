require('dotenv').config();
const { describe, it, before, after } = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const { pool } = require('../src/config/db');
const app = require('../src/app');
const { calcularResumenCalificacion } = require('../src/utils/grades');

describe('Cálculo de calificaciones (0-5 y 0-100)', () => {
  it('cuenta actividades sin calificar como 0 y excluye actividades exentas', () => {
    const futuro = Date.now() + 86_400_000;
    const pasado = Date.now() - 86_400_000;
    const actividades = [
      { id: 1, titulo: 'Act 1', puntaje_maximo: 5, fecha_limite: futuro },
      { id: 2, titulo: 'Act 2 (sin entregar)', puntaje_maximo: 100, fecha_limite: pasado },
      { id: 3, titulo: 'Act 3 (exenta)', puntaje_maximo: 5, fecha_limite: futuro },
    ];
    const entregasMap = new Map([
      [1, { actividad_id: 1, estado: 'CALIFICADA', nota: 4.0, exenta: 0, retroalimentacion: 'Bien' }],
      [3, { actividad_id: 3, estado: 'EXENTA', nota: null, exenta: 1, retroalimentacion: 'Entregada presencial' }],
    ]);

    const res = calcularResumenCalificacion(actividades, entregasMap);
    // Evaluables: Act 1 (4.0/5) y Act 2 (0/5) => promedio = 2.00 / 5.00 => 40.00 / 100
    assert.equal(res.total_actividades, 3);
    assert.equal(res.actividades_evaluables, 2);
    assert.equal(res.actividades_calificadas, 1);
    assert.equal(res.actividades_exentas, 1);
    assert.equal(res.nota_0_5, 2);
    assert.equal(res.nota_0_100, 40);
    assert.equal(res.detalle[1].estado, 'TARDIA');
    assert.equal(res.detalle[2].estado, 'EXENTA');
  });

  it('normaliza puntajes máximos distintos a escala 0-5 y convierte a 0-100', () => {
    const futuro = Date.now() + 86_400_000;
    const actividades = [
      { id: 10, titulo: 'Quiz 100', puntaje_maximo: 100, fecha_limite: futuro },
    ];
    const entregasMap = new Map([
      [10, { actividad_id: 10, estado: 'CALIFICADA', nota: 90, exenta: false }],
    ]);

    const res = calcularResumenCalificacion(actividades, entregasMap);
    assert.equal(res.nota_0_5, 4.5);
    assert.equal(res.nota_0_100, 90);
  });
});

describe('API LMS CTMA - Roles, permisos, archivos, notas y foro privado', () => {
  let server;
  let baseUrl;
  const createdUserIds = [];
  const suffix = `${Date.now()}_${Math.floor(Math.random() * 10000)}`;

  let instructorToken;
  let aprendizAToken;
  let aprendizBToken;
  let aprendizAId;
  let aprendizBId;
  let cursoId;
  let codigoAcceso;
  let act1Id;
  let act2Id;
  let hiloPrivadoId;

  async function api(pathUrl, { method = 'GET', token, body, formData } = {}) {
    const headers = {};
    if (token) headers.Authorization = `Bearer ${token}`;
    if (body !== undefined) headers['Content-Type'] = 'application/json';

    const response = await fetch(`${baseUrl}${pathUrl}`, {
      method,
      headers,
      body: formData || (body !== undefined ? JSON.stringify(body) : undefined),
    });

    const text = await response.text();
    let data = null;
    try {
      data = text ? JSON.parse(text) : null;
    } catch {
      data = text;
    }
    return { status: response.status, data };
  }

  before(async () => {
    server = http.createServer(app);
    await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
    const addr = server.address();
    baseUrl = `http://127.0.0.1:${addr.port}`;
  });

  after(async () => {
    if (createdUserIds.length > 0) {
      await pool.query('DELETE FROM usuarios WHERE id IN (?)', [createdUserIds]);
    }
    if (server) {
      await new Promise((resolve) => server.close(resolve));
    }
    await pool.end();
  });

  it('registra usuarios con roles estrictos y verifica aislamiento inicial', async () => {
    const rInst = await api('/api/auth/register', {
      method: 'POST',
      body: {
        nombre: 'Instructor Test',
        email: `inst_${suffix}@test.edu.co`,
        password: 'Password123',
        rol: 'INSTRUCTOR',
      },
    });
    assert.equal(rInst.status, 201);
    instructorToken = rInst.data.token;
    createdUserIds.push(rInst.data.usuario.id);

    const rApA = await api('/api/auth/register', {
      method: 'POST',
      body: {
        nombre: 'Aprendiz A',
        email: `ap_a_${suffix}@test.edu.co`,
        password: 'Password123',
        rol: 'APRENDIZ',
      },
    });
    assert.equal(rApA.status, 201);
    aprendizAToken = rApA.data.token;
    aprendizAId = rApA.data.usuario.id;
    createdUserIds.push(aprendizAId);

    const rApB = await api('/api/auth/register', {
      method: 'POST',
      body: {
        nombre: 'Aprendiz B',
        email: `ap_b_${suffix}@test.edu.co`,
        password: 'Password123',
        rol: 'APRENDIZ',
      },
    });
    assert.equal(rApB.status, 201);
    aprendizBToken = rApB.data.token;
    aprendizBId = rApB.data.usuario.id;
    createdUserIds.push(aprendizBId);

    // Aprendiz no puede crear cursos
    const denied = await api('/api/cursos', {
      method: 'POST',
      token: aprendizAToken,
      body: { nombre: 'Curso Ilegal', descripcion: 'No permitido' },
    });
    assert.equal(denied.status, 403);
  });

  it('permite crear curso, gestionar código y unir aprendices', async () => {
    const created = await api('/api/cursos', {
      method: 'POST',
      token: instructorToken,
      body: {
        nombre: 'Curso Pruebas Automatizadas',
        descripcion: 'Curso temporal de integración',
        estado: 'ACTIVO',
      },
    });
    assert.equal(created.status, 201);
    cursoId = created.data.id;
    codigoAcceso = created.data.codigo_acceso;
    assert.ok(codigoAcceso && codigoAcceso.length >= 6);

    // Desactivar código impide unirse
    const off = await api(`/api/cursos/${cursoId}/codigo/estado`, {
      method: 'PATCH',
      token: instructorToken,
      body: { codigo_activo: false },
    });
    assert.equal(off.status, 200);
    assert.equal(off.data.codigo_activo, false);

    const blockedJoin = await api('/api/cursos/unirse', {
      method: 'POST',
      token: aprendizAToken,
      body: { codigo_acceso: codigoAcceso },
    });
    assert.equal(blockedJoin.status, 403);

    // Regenerar reactiva código
    const regen = await api(`/api/cursos/${cursoId}/codigo/regenerar`, {
      method: 'POST',
      token: instructorToken,
    });
    assert.equal(regen.status, 200);
    codigoAcceso = regen.data.codigo_acceso;
    assert.equal(regen.data.codigo_activo, true);

    const joinA = await api('/api/cursos/unirse', {
      method: 'POST',
      token: aprendizAToken,
      body: { codigo_acceso: codigoAcceso },
    });
    assert.equal(joinA.status, 201);

    const joinB = await api('/api/cursos/unirse', {
      method: 'POST',
      token: aprendizBToken,
      body: { codigo_acceso: codigoAcceso },
    });
    assert.equal(joinB.status, 201);
  });

  it('valida subida segura de archivos, entregas, bloqueo tras calificar y exención', async () => {
    const fdAct1 = new FormData();
    fdAct1.append('titulo', 'Actividad Evaluable 1');
    fdAct1.append('descripcion', 'Resolver ejercicios y adjuntar evidencia');
    fdAct1.append('fecha_limite', String(Date.now() + 86_400_000));
    fdAct1.append('puntaje_maximo', '5');
    fdAct1.append('adjuntos', new Blob(['guia en txt'], { type: 'text/plain' }), 'guia.txt');

    const resAct1 = await api(`/api/actividades/curso/${cursoId}`, {
      method: 'POST',
      token: instructorToken,
      formData: fdAct1,
    });
    assert.equal(resAct1.status, 201);
    act1Id = resAct1.data.id;
    assert.equal(resAct1.data.adjuntos.length, 1);

    const fdAct2 = new FormData();
    fdAct2.append('titulo', 'Actividad Presencial Exenta');
    fdAct2.append('descripcion', 'Actividad sustentada presencialmente');
    fdAct2.append('fecha_limite', String(Date.now() + 86_400_000));
    fdAct2.append('puntaje_maximo', '5');

    const resAct2 = await api(`/api/actividades/curso/${cursoId}`, {
      method: 'POST',
      token: instructorToken,
      formData: fdAct2,
    });
    assert.equal(resAct2.status, 201);
    act2Id = resAct2.data.id;

    // Archivo peligroso .exe debe ser rechazado con 400
    const fdBad = new FormData();
    fdBad.append('descripcion', 'Intento inválido');
    fdBad.append('archivos', new Blob(['MZ'], { type: 'application/octet-stream' }), 'malware.exe');
    const badUpload = await api(`/api/actividades/${act1Id}/entrega`, {
      method: 'POST',
      token: aprendizAToken,
      formData: fdBad,
    });
    assert.equal(badUpload.status, 400);

    // Aprendiz A entrega evidencia válida
    const fdGood = new FormData();
    fdGood.append('descripcion', 'Entrega inicial');
    fdGood.append('archivos', new Blob(['solucion'], { type: 'text/plain' }), 'solucion.txt');
    const goodUpload = await api(`/api/actividades/${act1Id}/entrega`, {
      method: 'POST',
      token: aprendizAToken,
      formData: fdGood,
    });
    assert.equal(goodUpload.status, 201);
    assert.equal(goodUpload.data.estado, 'ENTREGADA');

    // Aprendiz A puede editar antes de calificar
    const fdEdit = new FormData();
    fdEdit.append('descripcion', 'Entrega corregida antes de calificar');
    fdEdit.append('conservar_archivos', 'true');
    const edited = await api(`/api/actividades/${act1Id}/entrega`, {
      method: 'POST',
      token: aprendizAToken,
      formData: fdEdit,
    });
    assert.equal(edited.status, 200);
    assert.equal(edited.data.descripcion, 'Entrega corregida antes de calificar');

    // Instructor califica Act 1 con 4.0 y marca Act 2 como EXENTA
    const grade1 = await api(`/api/actividades/${act1Id}/entregas/${aprendizAId}/calificar`, {
      method: 'PUT',
      token: instructorToken,
      body: { nota: 4.0, retroalimentacion: 'Buen trabajo', exenta: false },
    });
    assert.equal(grade1.status, 200);
    assert.equal(grade1.data.estado, 'CALIFICADA');

    // Intentar reenviar después de calificada debe fallar
    const fdLateEdit = new FormData();
    fdLateEdit.append('descripcion', 'Intento tras nota');
    const blockedResubmit = await api(`/api/actividades/${act1Id}/entrega`, {
      method: 'POST',
      token: aprendizAToken,
      formData: fdLateEdit,
    });
    assert.equal(blockedResubmit.status, 409);

    const exempt2 = await api(`/api/actividades/${act2Id}/entregas/${aprendizAId}/calificar`, {
      method: 'PUT',
      token: instructorToken,
      body: { exenta: true, retroalimentacion: 'Sustentó en laboratorio' },
    });
    assert.equal(exempt2.status, 200);
    assert.equal(exempt2.data.estado, 'EXENTA');

    // Promedio final de Aprendiz A debe ser 4.0/5 (80/100) al excluir Act 2
    const califA = await api(`/api/cursos/${cursoId}/calificaciones`, {
      token: aprendizAToken,
    });
    assert.equal(califA.status, 200);
    assert.equal(califA.data.nota_0_5, 4);
    assert.equal(califA.data.nota_0_100, 80);
    assert.equal(califA.data.actividades_exentas, 1);

    // Aprendiz B no entregó Act 1 ni está exento en Act 2 => 0.0 / 5.0
    const califB = await api(`/api/cursos/${cursoId}/calificaciones`, {
      token: aprendizBToken,
    });
    assert.equal(califB.status, 200);
    assert.equal(califB.data.nota_0_5, 0);
  });

  it('garantiza privacidad estricta 1 a 1 en el foro privado y permite expulsar estudiantes', async () => {
    const threadA = await api(`/api/foro/curso/${cursoId}`, {
      method: 'POST',
      token: aprendizAToken,
      body: {
        tipo: 'PRIVADO',
        titulo: 'Mensaje solo para instructor',
        contenido: 'Tengo una incapacidad médica esta semana.',
      },
    });
    assert.equal(threadA.status, 201);
    hiloPrivadoId = threadA.data.id;

    // Aprendiz B intenta ver los mensajes privados de A => 403
    const snoopList = await api(`/api/foro/curso/${cursoId}?tipo=PRIVADO&aprendizId=${aprendizAId}`, {
      token: aprendizBToken,
    });
    assert.equal(snoopList.status, 403);

    const snoopMsgs = await api(`/api/foro/${hiloPrivadoId}/mensajes`, {
      token: aprendizBToken,
    });
    assert.equal(snoopMsgs.status, 403);

    const snoopReply = await api(`/api/foro/${hiloPrivadoId}/mensajes`, {
      method: 'POST',
      token: aprendizBToken,
      body: { contenido: 'Intento intrusivo' },
    });
    assert.equal(snoopReply.status, 403);

    // El instructor SÍ puede ver y responder
    const instReply = await api(`/api/foro/${hiloPrivadoId}/mensajes`, {
      method: 'POST',
      token: instructorToken,
      body: { contenido: 'Recibido, revisaremos tu excusa.' },
    });
    assert.equal(instReply.status, 201);

    // Expulsar a Aprendiz B => luego pierde acceso al curso (403)
    const expelB = await api(`/api/cursos/${cursoId}/personas/${aprendizBId}`, {
      method: 'DELETE',
      token: instructorToken,
    });
    assert.equal(expelB.status, 204);

    const forbiddenAfterExpel = await api(`/api/cursos/${cursoId}`, {
      token: aprendizBToken,
    });
    assert.equal(forbiddenAfterExpel.status, 403);
  });
});
