/**
 * Calcula la calificación total del curso para un estudiante.
 * Regla:
 * - Escala principal Colombia: 0.00 a 5.00 + conversión automática a escala 0 a 100.
 * - Las actividades marcadas como exentas (entregadas por fuera del curso) se excluyen.
 * - Las demás actividades sin calificar cuentan como 0.00.
 */
function calcularResumenCalificacion(actividades, entregasPorActividadId) {
  let sumaNormalizada05 = 0;
  let evaluables = 0;
  let calificadas = 0;
  let exentas = 0;
  const detalle = [];

  for (const act of actividades) {
    const entrega = entregasPorActividadId.get(act.id) || null;
    const puntajeMax = Number(act.puntaje_maximo) > 0 ? Number(act.puntaje_maximo) : 5.0;
    const esExenta = Boolean(entrega && (entrega.exenta === 1 || entrega.exenta === true || entrega.estado === 'EXENTA'));

    if (esExenta) {
      exentas += 1;
      detalle.push({
        actividad_id: act.id,
        titulo: act.titulo,
        puntaje_maximo: puntajeMax,
        nota: null,
        nota_escala_0_5: null,
        estado: 'EXENTA',
        exenta: true,
        retroalimentacion: entrega?.retroalimentacion || null,
      });
      continue;
    }

    evaluables += 1;
    let nota05 = 0;
    let estado = entrega?.estado || 'PENDIENTE';
    const notaOriginal = entrega && entrega.nota !== null && entrega.nota !== undefined
      ? Number(entrega.nota)
      : null;

    if (estado === 'CALIFICADA' && notaOriginal !== null && !Number.isNaN(notaOriginal)) {
      calificadas += 1;
      nota05 = Math.max(0, Math.min(5, (notaOriginal / puntajeMax) * 5));
    } else if (!entrega && Date.now() > Number(act.fecha_limite)) {
      estado = 'TARDIA';
    }

    sumaNormalizada05 += nota05;
    detalle.push({
      actividad_id: act.id,
      titulo: act.titulo,
      puntaje_maximo: puntajeMax,
      nota: notaOriginal,
      nota_escala_0_5: Math.round(nota05 * 100) / 100,
      estado,
      exenta: false,
      retroalimentacion: entrega?.retroalimentacion || null,
    });
  }

  const promedio05 = evaluables > 0
    ? Math.round((sumaNormalizada05 / evaluables) * 100) / 100
    : 0;
  const promedio0100 = Math.round(((promedio05 / 5) * 100) * 100) / 100;

  return {
    nota_0_5: promedio05,
    nota_0_100: promedio0100,
    total_actividades: actividades.length,
    actividades_evaluables: evaluables,
    actividades_calificadas: calificadas,
    actividades_exentas: exentas,
    detalle,
  };
}

module.exports = {
  calcularResumenCalificacion,
};
