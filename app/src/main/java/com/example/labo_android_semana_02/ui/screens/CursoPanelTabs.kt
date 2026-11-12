package com.example.labo_android_semana_02.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.labo_android_semana_02.domain.model.Actividad
import com.example.labo_android_semana_02.domain.model.ArchivoAdjunto
import com.example.labo_android_semana_02.domain.model.EstadoEntrega
import com.example.labo_android_semana_02.domain.model.HiloForo
import com.example.labo_android_semana_02.domain.model.MaterialApoyo
import com.example.labo_android_semana_02.domain.model.PersonaCurso
import com.example.labo_android_semana_02.domain.model.RolUsuario
import com.example.labo_android_semana_02.domain.model.TipoHiloForo
import com.example.labo_android_semana_02.domain.model.TipoMaterial
import com.example.labo_android_semana_02.ui.components.DialogoConfirmacion
import com.example.labo_android_semana_02.ui.components.EstadoEntregaChip
import com.example.labo_android_semana_02.ui.components.EstadoVacioTarjeta
import com.example.labo_android_semana_02.ui.components.UiFormatters
import com.example.labo_android_semana_02.ui.components.WrapRow
import com.example.labo_android_semana_02.ui.viewmodel.CursoPanelUiState

@Composable
fun TabActividadesContenido(
    state: CursoPanelUiState,
    esInstructor: Boolean,
    onNuevaActividad: () -> Unit,
    onEditarActividad: (Actividad) -> Unit,
    onEliminarActividad: (Int) -> Unit,
    onAbrirEntregasInstructor: (Int) -> Unit,
    onAbrirEntregaAprendiz: (Int) -> Unit,
    onAbrirAdjunto: (ArchivoAdjunto) -> Unit,
) {
    var actividadAEliminar by remember { mutableStateOf<Actividad?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (esInstructor) {
            Button(
                onClick = onNuevaActividad,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Publicar nueva actividad")
            }
        }

        if (state.actividades.isEmpty() && !state.cargando) {
            EstadoVacioTarjeta(
                titulo = "Sin actividades publicadas",
                descripcion = if (esInstructor) {
                    "Crea la primera actividad con fecha límite, puntaje máximo y adjuntos."
                } else {
                    "Tu instructor aún no ha publicado actividades en este curso."
                },
            )
        }

        state.actividades.forEach { act ->
            Card(
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (esInstructor) onAbrirEntregasInstructor(act.id)
                        else onAbrirEntregaAprendiz(act.id)
                    },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = act.titulo,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        if (!esInstructor && act.estadoAprendiz != null) {
                            EstadoEntregaChip(estado = act.estadoAprendiz)
                        }
                    }

                    if (act.descripcion.isNotBlank()) {
                        Text(
                            text = act.descripcion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Text(
                        text = "Límite: ${UiFormatters.formatearFechaHora(act.fechaLimite)} • Puntaje máx: ${UiFormatters.formatearNota(act.puntajeMaximo)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    if (act.adjuntos.isNotEmpty()) {
                        WrapRow(
                            horizontalSpacing = 6.dp,
                            verticalSpacing = 6.dp,
                        ) {
                            act.adjuntos.forEach { adj ->
                                AssistChip(
                                    onClick = { onAbrirAdjunto(adj) },
                                    label = {
                                        Text("${adj.nombreOriginal} (${UiFormatters.formatearTamano(adj.tamanoBytes)})")
                                    },
                                )
                            }
                        }
                    }

                    if (esInstructor) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedButton(onClick = { onAbrirEntregasInstructor(act.id) }) {
                                Text("Entregas (${act.totalEntregas}) • Por calificar: ${act.porCalificar}")
                            }
                            Row {
                                IconButton(onClick = { onEditarActividad(act) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                                }
                                IconButton(onClick = { actividadAEliminar = act }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    } else {
                        val entrega = act.miEntrega
                        if (entrega != null && entrega.estado == EstadoEntrega.CALIFICADA && entrega.nota != null) {
                            Text(
                                text = "Calificación: ${UiFormatters.formatearNota(entrega.nota)} / ${UiFormatters.formatearNota(act.puntajeMaximo)}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        OutlinedButton(
                            onClick = { onAbrirEntregaAprendiz(act.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            val textoBtn = when (act.estadoAprendiz) {
                                EstadoEntrega.CALIFICADA, EstadoEntrega.EXENTA -> "Ver calificación y retroalimentación"
                                EstadoEntrega.ENTREGADA -> "Ver o editar entrega"
                                else -> "Entregar evidencia"
                            }
                            Text(textoBtn)
                        }
                    }
                }
            }
        }
    }

    actividadAEliminar?.let { act ->
        DialogoConfirmacion(
            titulo = "Eliminar actividad",
            mensaje = "¿Deseas eliminar '${act.titulo}' junto con todas sus entregas?",
            textoConfirmar = "Eliminar",
            onConfirmar = {
                onEliminarActividad(act.id)
                actividadAEliminar = null
            },
            onCancelar = { actividadAEliminar = null },
        )
    }
}

@Composable
fun TabMaterialContenido(
    state: CursoPanelUiState,
    esInstructor: Boolean,
    onNuevoMaterial: () -> Unit,
    onEliminarMaterial: (Int) -> Unit,
    onAbrirMaterial: (MaterialApoyo) -> Unit,
) {
    var materialAEliminar by remember { mutableStateOf<MaterialApoyo?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (esInstructor) {
            Button(onClick = onNuevoMaterial, modifier = Modifier.fillMaxWidth()) {
                Text("Subir archivo o enlace de apoyo")
            }
        }

        if (state.materiales.isEmpty() && !state.cargando) {
            EstadoVacioTarjeta(
                titulo = "Sin material de apoyo",
                descripcion = if (esInstructor) {
                    "Comparte guías PDF, presentaciones, videos o enlaces de referencia."
                } else {
                    "Tu instructor aún no ha compartido material de apoyo."
                },
            )
        }

        state.materiales.forEach { mat ->
            Card(
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mat.titulo,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (mat.tipo == TipoMaterial.ENLACE) {
                                    "Enlace externo"
                                } else {
                                    "Archivo: ${mat.nombreOriginal ?: ""} (${UiFormatters.formatearTamano(mat.tamanoBytes)})"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (esInstructor) {
                            IconButton(onClick = { materialAEliminar = mat }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar material",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }

                    if (mat.descripcion.isNotBlank()) {
                        Text(
                            text = mat.descripcion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    OutlinedButton(
                        onClick = { onAbrirMaterial(mat) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (mat.tipo == TipoMaterial.ENLACE) "Abrir enlace" else "Descargar y abrir")
                    }
                }
            }
        }
    }

    materialAEliminar?.let { mat ->
        DialogoConfirmacion(
            titulo = "Eliminar material",
            mensaje = "¿Deseas eliminar '${mat.titulo}'?",
            textoConfirmar = "Eliminar",
            onConfirmar = {
                onEliminarMaterial(mat.id)
                materialAEliminar = null
            },
            onCancelar = { materialAEliminar = null },
        )
    }
}

@Composable
fun TabForoContenido(
    state: CursoPanelUiState,
    esInstructor: Boolean,
    onCambiarModoForo: (Boolean) -> Unit,
    onCambiarFiltroAprendiz: (Int?) -> Unit,
    onNuevoHilo: () -> Unit,
    onAbrirHilo: (Int) -> Unit,
) {
    val hilos = if (state.modoForoPrivado) state.hilosPrivados else state.hilosPublicos

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FilterChip(
                selected = !state.modoForoPrivado,
                onClick = { onCambiarModoForo(false) },
                label = { Text("Foro público") },
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = state.modoForoPrivado,
                onClick = { onCambiarModoForo(true) },
                label = { Text("Foro privado 1 a 1") },
                modifier = Modifier.weight(1f),
            )
        }

        if (state.modoForoPrivado && esInstructor) {
            val aprendices = state.personas?.aprendices.orEmpty()
            if (aprendices.isNotEmpty()) {
                Text("Filtrar conversación por aprendiz:", style = MaterialTheme.typography.labelMedium)
                WrapRow(horizontalSpacing = 6.dp, verticalSpacing = 6.dp) {
                    FilterChip(
                        selected = state.filtroAprendizForoId == null,
                        onClick = { onCambiarFiltroAprendiz(null) },
                        label = { Text("Todos") },
                    )
                    aprendices.forEach { est ->
                        FilterChip(
                            selected = state.filtroAprendizForoId == est.id,
                            onClick = { onCambiarFiltroAprendiz(est.id) },
                            label = { Text(est.nombre) },
                        )
                    }
                }
            }
        }

        Button(onClick = onNuevoHilo, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (state.modoForoPrivado) "Nuevo mensaje privado" else "Nuevo tema público"
            )
        }

        if (hilos.isEmpty() && !state.cargando) {
            EstadoVacioTarjeta(
                titulo = if (state.modoForoPrivado) "Sin conversaciones privadas" else "Foro público sin temas",
                descripcion = if (state.modoForoPrivado) {
                    "Las conversaciones privadas solo son visibles entre el instructor y el aprendiz destinatario."
                } else {
                    "Publica un hilo para compartir dudas o anuncios con todo el curso."
                },
            )
        }

        hilos.forEach { hilo ->
            TarjetaHiloForo(hilo = hilo, onClick = { onAbrirHilo(hilo.id) })
        }
    }
}

@Composable
private fun TarjetaHiloForo(hilo: HiloForo, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = hilo.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    color = if (hilo.tipo == TipoHiloForo.PRIVADO) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = if (hilo.tipo == TipoHiloForo.PRIVADO) "Privado 1:1" else "Público",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
            Text(
                text = hilo.contenido,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val contextoPrivado = if (hilo.tipo == TipoHiloForo.PRIVADO && hilo.aprendizObjetivoNombre != null) {
                " • Con: ${hilo.aprendizObjetivoNombre}"
            } else {
                ""
            }
            Text(
                text = "Por ${hilo.autorNombre} (${if (hilo.autorRol == RolUsuario.INSTRUCTOR) "Instructor" else "Aprendiz"})$contextoPrivado • ${hilo.totalMensajes} respuestas",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun TabPersonasContenido(
    state: CursoPanelUiState,
    esInstructor: Boolean,
    convertir05A100: (Double) -> Double,
    onExpulsarEstudiante: (Int) -> Unit,
    onSalirseDelCurso: () -> Unit,
) {
    var estudianteAExpulsar by remember { mutableStateOf<PersonaCurso?>(null) }
    var confirmarSalida by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Conversor rápido de escala 0-5 a 0-100
        ConversorEscalaCard(convertir05A100 = convertir05A100)

        state.personas?.instructor?.let { inst ->
            Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(inst.nombre, fontWeight = FontWeight.Bold)
                        Text("Instructor • ${inst.email}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        val aprendices = state.personas?.aprendices.orEmpty()
        Text(
            text = "Aprendices inscritos (${aprendices.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        if (aprendices.isEmpty() && !state.cargando) {
            EstadoVacioTarjeta(
                titulo = "Sin aprendices inscritos",
                descripcion = "Comparte el código de acceso para que los aprendices ingresen al curso.",
            )
        }

        aprendices.forEach { est ->
            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(est.nombre, fontWeight = FontWeight.Medium)
                        Text(
                            text = est.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (est.nota05 != null && est.nota0100 != null) {
                            Text(
                                text = "Promedio: ${UiFormatters.formatearNota(est.nota05)} / 5.0  (${UiFormatters.formatearNota(est.nota0100)} / 100)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (esInstructor) {
                        OutlinedButton(onClick = { estudianteAExpulsar = est }) {
                            Text("Expulsar", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        if (!esInstructor) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            OutlinedButton(
                onClick = { confirmarSalida = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Salirse de este curso", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    estudianteAExpulsar?.let { est ->
        DialogoConfirmacion(
            titulo = "Expulsar aprendiz",
            mensaje = "¿Seguro que deseas expulsar a ${est.nombre} del curso?",
            textoConfirmar = "Expulsar",
            onConfirmar = {
                onExpulsarEstudiante(est.id)
                estudianteAExpulsar = null
            },
            onCancelar = { estudianteAExpulsar = null },
        )
    }

    if (confirmarSalida) {
        DialogoConfirmacion(
            titulo = "Salirse del curso",
            mensaje = "¿Deseas abandonar este curso?",
            textoConfirmar = "Salirse",
            onConfirmar = {
                confirmarSalida = false
                onSalirseDelCurso()
            },
            onCancelar = { confirmarSalida = false },
        )
    }
}

@Composable
private fun ConversorEscalaCard(convertir05A100: (Double) -> Double) {
    var valorSlider by remember { mutableFloatStateOf(3.5f) }
    val nota05 = ((valorSlider * 10f).toInt() / 10.0)
    val nota0100 = convertir05A100(nota05)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Conversor de escala (0.0–5.0  ⇄  0–100)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Slider(
                value = valorSlider,
                onValueChange = { valorSlider = it },
                valueRange = 0f..5f,
            )
            Text(
                text = "${UiFormatters.formatearNota(nota05)} / 5.0  =  ${UiFormatters.formatearNota(nota0100)} / 100",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
