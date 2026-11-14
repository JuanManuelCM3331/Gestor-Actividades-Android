package com.example.labo_android_semana_02.ui.screens

import com.example.labo_android_semana_02.ui.components.AppButton
import com.example.labo_android_semana_02.ui.components.AppFab
import com.example.labo_android_semana_02.ui.components.AppFilterChip
import com.example.labo_android_semana_02.ui.components.AppIconButton
import com.example.labo_android_semana_02.ui.components.AppOutlinedButton
import com.example.labo_android_semana_02.ui.components.AppTextButton

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import com.example.labo_android_semana_02.ui.components.IconoTintado
import com.example.labo_android_semana_02.ui.theme.coloresExtendidos
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.labo_android_semana_02.domain.model.Curso
import com.example.labo_android_semana_02.domain.model.EstadoCurso
import com.example.labo_android_semana_02.domain.model.RolUsuario
import com.example.labo_android_semana_02.ui.components.BannerMensaje
import com.example.labo_android_semana_02.ui.components.DialogoConfirmacion
import com.example.labo_android_semana_02.ui.components.EstadoCursoChip
import com.example.labo_android_semana_02.ui.components.EstadoVacioTarjeta
import com.example.labo_android_semana_02.ui.components.UiFormatters
import com.example.labo_android_semana_02.ui.components.WrapRow
import com.example.labo_android_semana_02.ui.viewmodel.CursosUiState
import com.example.labo_android_semana_02.ui.viewmodel.FiltroEstadoCurso

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CursosScreen(
    state: CursosUiState,
    temaOscuro: Boolean,
    onAlternarTema: () -> Unit,
    onRefrescar: () -> Unit,
    onFiltroChange: (FiltroEstadoCurso) -> Unit,
    onBusquedaChange: (String) -> Unit,
    onAbrirCurso: (Int) -> Unit,
    onCrearCurso: (String, String, EstadoCurso) -> Unit,
    onEditarCurso: (Int, String, String, EstadoCurso) -> Unit,
    onEliminarCurso: (Int) -> Unit,
    onUnirseCurso: (String) -> Unit,
    onSalirseCurso: (Int) -> Unit,
    onCerrarSesion: () -> Unit,
    onLimpiarFeedback: () -> Unit,
) {
    val esInstructor = state.usuario?.rol == RolUsuario.INSTRUCTOR
    var mostrarDialogoCrearEditar by remember { mutableStateOf<Pair<Boolean, Curso?>?>(null) }
    var mostrarDialogoUnirse by remember { mutableStateOf(false) }
    var cursoAEliminar by remember { mutableStateOf<Curso?>(null) }
    var cursoASalir by remember { mutableStateOf<Curso?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                navigationIcon = {
                    IconoTintado(
                        icono = if (esInstructor) Icons.Default.Class else Icons.Default.School,
                        color = MaterialTheme.colorScheme.primary,
                        tamano = 40.dp,
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                    )
                },
                title = {
                    Column {
                        Text(
                            text = if (esInstructor) "Mis cursos (Instructor)" else "Mis cursos (Aprendiz)",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        state.usuario?.let { u ->
                            Text(
                                text = u.nombre,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                actions = {
                    AppIconButton(onClick = onAlternarTema) {
                        Icon(
                            imageVector = if (temaOscuro) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (temaOscuro) "Cambiar a modo claro" else "Cambiar a modo oscuro",
                            tint = if (temaOscuro) MaterialTheme.coloresExtendidos.advertencia
                            else MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    AppIconButton(onClick = onRefrescar) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Actualizar",
                            tint = MaterialTheme.coloresExtendidos.informacion,
                        )
                    }
                    AppIconButton(onClick = onCerrarSesion) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            AppFab(
                onClick = {
                    if (esInstructor) {
                        mostrarDialogoCrearEditar = true to null
                    } else {
                        mostrarDialogoUnirse = true
                    }
                },
            ) {
                Icon(
                    imageVector = if (esInstructor) Icons.Default.Add else Icons.Default.Key,
                    contentDescription = if (esInstructor) "Crear curso" else "Unirse con código",
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxSize(),
            ) {
                if (state.cargando) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer,
                    )
                }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        if (state.error != null) {
                            BannerMensaje(
                                texto = state.error,
                                esError = true,
                                onDismiss = onLimpiarFeedback,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        } else if (state.mensaje != null) {
                            BannerMensaje(
                                texto = state.mensaje,
                                esError = false,
                                onDismiss = onLimpiarFeedback,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }

                        OutlinedTextField(
                            value = state.busqueda,
                            onValueChange = onBusquedaChange,
                            label = { Text("Buscar curso por nombre o instructor") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        WrapRow(horizontalSpacing = 8.dp, verticalSpacing = 6.dp) {
                            AppFilterChip(
                                selected = state.filtro == FiltroEstadoCurso.TODOS,
                                onClick = { onFiltroChange(FiltroEstadoCurso.TODOS) },
                                icon = Icons.Default.Class,
                                label = { Text("Todos") },
                            )
                            AppFilterChip(
                                selected = state.filtro == FiltroEstadoCurso.ACTIVO,
                                onClick = { onFiltroChange(FiltroEstadoCurso.ACTIVO) },
                                icon = Icons.Default.CheckCircle,
                                label = { Text("Activos") },
                            )
                            AppFilterChip(
                                selected = state.filtro == FiltroEstadoCurso.ARCHIVADO,
                                onClick = { onFiltroChange(FiltroEstadoCurso.ARCHIVADO) },
                                icon = Icons.Default.Archive,
                                label = { Text("Archivados") },
                            )
                        }
                    }

                    if (state.cursosFiltrados.isEmpty() && !state.cargando) {
                        item {
                            EstadoVacioTarjeta(
                                icono = if (esInstructor) Icons.Default.AddCircle else Icons.Default.Key,
                                titulo = if (esInstructor) "Aún no has creado cursos" else "No estás inscrito en cursos",
                                descripcion = if (esInstructor) {
                                    "Toca el botón + para crear un curso y obtener un código de acceso para tus aprendices."
                                } else {
                                    "Toca el botón + para unirte a un curso usando el código entregado por tu instructor."
                                },
                            )
                        }
                    }

                    items(state.cursosFiltrados, key = { it.id }) { curso ->
                        TarjetaCurso(
                            curso = curso,
                            esInstructor = esInstructor,
                            onAbrir = { onAbrirCurso(curso.id) },
                            onEditar = { mostrarDialogoCrearEditar = true to curso },
                            onEliminar = { cursoAEliminar = curso },
                            onSalirse = { cursoASalir = curso },
                        )
                    }

                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    mostrarDialogoCrearEditar?.let { (_, cursoEditar) ->
        DialogoCrearEditarCurso(
            curso = cursoEditar,
            onDismiss = { mostrarDialogoCrearEditar = null },
            onGuardar = { nombre, descripcion, estado ->
                if (cursoEditar == null) {
                    onCrearCurso(nombre, descripcion, estado)
                } else {
                    onEditarCurso(cursoEditar.id, nombre, descripcion, estado)
                }
                mostrarDialogoCrearEditar = null
            },
        )
    }

    if (mostrarDialogoUnirse) {
        DialogoUnirseCurso(
            onDismiss = { mostrarDialogoUnirse = false },
            onUnirse = { codigo ->
                onUnirseCurso(codigo)
                mostrarDialogoUnirse = false
            },
        )
    }

    cursoAEliminar?.let { curso ->
        DialogoConfirmacion(
            titulo = "Eliminar curso",
            mensaje = "¿Seguro que deseas eliminar '${curso.nombre}'? Se eliminarán sus actividades, entregas, materiales y foros.",
            textoConfirmar = "Eliminar",
            onConfirmar = {
                onEliminarCurso(curso.id)
                cursoAEliminar = null
            },
            onCancelar = { cursoAEliminar = null },
        )
    }

    cursoASalir?.let { curso ->
        DialogoConfirmacion(
            titulo = "Salirse del curso",
            mensaje = "¿Deseas salirte de '${curso.nombre}'? Podrás volver después si el código de acceso sigue activo.",
            textoConfirmar = "Salirse",
            onConfirmar = {
                onSalirseCurso(curso.id)
                cursoASalir = null
            },
            onCancelar = { cursoASalir = null },
        )
    }
}

@Composable
private fun TarjetaCurso(
    curso: Curso,
    esInstructor: Boolean,
    onAbrir: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onSalirse: () -> Unit,
) {
    val acento = colorAcentoCurso(curso.id)
    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
        border = BorderStroke(1.dp, acento.copy(alpha = 0.30f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAbrir),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconoTintado(
                    icono = Icons.AutoMirrored.Filled.MenuBook,
                    color = acento,
                    tamano = 46.dp,
                )
                Text(
                    text = curso.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                EstadoCursoChip(estado = curso.estado)
            }

            if (curso.descripcion.isNotBlank()) {
                Text(
                    text = curso.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.coloresExtendidos.informacion,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = " ${curso.totalEstudiantes}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        tint = MaterialTheme.coloresExtendidos.advertencia,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = " ${curso.totalActividades}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (!esInstructor) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = " ${curso.instructorNombre}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    val codigoActivo = curso.codigoActivo
                    Surface(
                        color = if (codigoActivo) MaterialTheme.coloresExtendidos.exitoContenedor
                        else MaterialTheme.coloresExtendidos.neutroContenedor,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        val textoCodigo = if (codigoActivo) {
                            curso.codigoAcceso ?: "—"
                        } else {
                            "${curso.codigoAcceso ?: "—"} (Inactivo)"
                        }
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = if (codigoActivo) MaterialTheme.coloresExtendidos.exito
                                else MaterialTheme.coloresExtendidos.neutro,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = textoCodigo,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Row {
                        AppIconButton(onClick = onEditar) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Editar curso",
                                tint = MaterialTheme.coloresExtendidos.informacion,
                            )
                        }
                        AppIconButton(onClick = onEliminar) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar curso",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val nota05 = curso.nota05 ?: 0.0
                    val nota0100 = curso.nota0100 ?: 0.0
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.coloresExtendidos.advertencia,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${UiFormatters.formatearNota(nota05)} / 5.0  (${UiFormatters.formatearNota(nota0100)} / 100)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    AppOutlinedButton(onClick = onSalirse, icon = Icons.AutoMirrored.Filled.ExitToApp) {
                        Text("Salirse")
                    }
                }
            }
        }
    }
}

@Composable
private fun colorAcentoCurso(id: Int): Color {
    val c = MaterialTheme.coloresExtendidos
    val paleta = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        c.exito,
        c.especial,
        c.advertencia,
        c.informacion,
    )
    return paleta[Math.floorMod(id, paleta.size)]
}

@Composable
private fun DialogoCrearEditarCurso(
    curso: Curso?,
    onDismiss: () -> Unit,
    onGuardar: (String, String, EstadoCurso) -> Unit,
) {
    var nombre by remember { mutableStateOf(curso?.nombre.orEmpty()) }
    var descripcion by remember { mutableStateOf(curso?.descripcion.orEmpty()) }
    var estado by remember { mutableStateOf(curso?.estado ?: EstadoCurso.ACTIVO) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (curso == null) Icons.Default.AddCircle else Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text(if (curso == null) "Nuevo curso" else "Editar curso") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del curso") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Estado", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppFilterChip(
                        selected = estado == EstadoCurso.ACTIVO,
                        onClick = { estado = EstadoCurso.ACTIVO },
                        icon = Icons.Default.CheckCircle,
                        label = { Text("Activo") },
                    )
                    AppFilterChip(
                        selected = estado == EstadoCurso.ARCHIVADO,
                        onClick = { estado = EstadoCurso.ARCHIVADO },
                        icon = Icons.Default.Archive,
                        label = { Text("Archivado") },
                    )
                }
            }
        },
        confirmButton = {
            AppButton(onClick = { onGuardar(nombre, descripcion, estado) }, icon = Icons.Default.CheckCircle) {
                Text("Guardar")
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun DialogoUnirseCurso(
    onDismiss: () -> Unit,
    onUnirse: (String) -> Unit,
) {
    var codigo by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        title = { Text("Unirse a un curso") },
        text = {
            OutlinedTextField(
                value = codigo,
                onValueChange = { codigo = it.uppercase() },
                label = { Text("Código de acceso (ej. CTMA2026)") },
                leadingIcon = {
                    Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            AppButton(onClick = { onUnirse(codigo) }, icon = Icons.Default.AddCircle) { Text("Unirse") }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
