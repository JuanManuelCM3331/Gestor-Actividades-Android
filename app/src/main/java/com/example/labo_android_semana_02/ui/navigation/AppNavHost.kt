package com.example.labo_android_semana_02.ui.navigation

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.labo_android_semana_02.ui.ActividadViewModel
import com.example.labo_android_semana_02.ui.OperacionUiState
import com.example.labo_android_semana_02.ui.ReporteViewModel
import com.example.labo_android_semana_02.ui.screens.DetalleActividad
import com.example.labo_android_semana_02.ui.screens.DetalleReporteScreen
import com.example.labo_android_semana_02.ui.screens.FormularioActividad
import com.example.labo_android_semana_02.ui.screens.PantallaBienvenida
import com.example.labo_android_semana_02.ui.screens.PantallaPrincipal
import com.example.labo_android_semana_02.ui.viewmodel.DetalleActividadViewModel
import com.example.labo_android_semana_02.ui.viewmodel.DetalleReporteViewModel
import com.example.labo_android_semana_02.ui.viewmodel.FormularioReporteViewModel
import java.io.File
import java.io.FileOutputStream

@Composable
fun AppNavHost(
    temaOscuro: Boolean,
    onToggleTema: (Boolean) -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.WELCOME
    ) {
        composable(AppRoutes.WELCOME) {
            PantallaBienvenida(
                onIngresar = {
                    navController.navigate(AppRoutes.ACTIVITIES) {
                        popUpTo(AppRoutes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoutes.ACTIVITIES) {
            val reporteViewModel: ReporteViewModel = hiltViewModel()
            val actividadViewModel: ActividadViewModel = hiltViewModel()

            val uiState by reporteViewModel.uiState.collectAsStateWithLifecycle()
            val syncUiState by actividadViewModel.uiState.collectAsStateWithLifecycle()
            val busqueda by reporteViewModel.busqueda.collectAsStateWithLifecycle()
            val categorias by reporteViewModel.categorias.collectAsStateWithLifecycle()
            val categoriaSeleccionadaId by reporteViewModel.categoriaSeleccionadaId.collectAsStateWithLifecycle()
            val ordenActual by reporteViewModel.ordenActual.collectAsStateWithLifecycle()
            val syncEstado by reporteViewModel.syncEstado.collectAsStateWithLifecycle()
            val sincronizando by reporteViewModel.sincronizando.collectAsStateWithLifecycle()

            PantallaPrincipal(
                uiState = uiState,
                syncUiState = syncUiState,
                busqueda = busqueda,
                categorias = categorias,
                categoriaSeleccionadaId = categoriaSeleccionadaId,
                ordenActual = ordenActual,
                syncEstado = syncEstado,
                sincronizando = sincronizando,
                onBusquedaChange = reporteViewModel::onBusquedaChange,
                onCategoriaSeleccionada = reporteViewModel::onCategoriaSeleccionada,
                onToggleOrden = {
                    val siguienteOrden = if (ordenActual == "FECHA_DESC") "FECHA_ASC" else "FECHA_DESC"
                    reporteViewModel.actualizarOrden(siguienteOrden)
                },
                onSyncReportes = reporteViewModel::sincronizarConServidor,
                onReporteClick = { reporte ->
                    navController.navigate(AppRoutes.reporteDetail(reporte.id))
                },
                onAddClick = {
                    navController.navigate(AppRoutes.reporteForm())
                },
                onDelete = reporteViewModel::eliminarReporte,
                onEdit = { reporte ->
                    navController.navigate(AppRoutes.reporteForm(reporte.id))
                },
                onToggle = { reporte ->
                    reporteViewModel.guardarReporte(reporte.copy(resuelto = !reporte.resuelto))
                },
                onRetry = reporteViewModel::sincronizarConServidor,
                onRefreshActividades = actividadViewModel::refresh,
                onActividadClick = { actividad ->
                    navController.navigate(AppRoutes.actividadDetail(actividad.id))
                },
                temaOscuro = temaOscuro,
                onToggleTema = onToggleTema,
                onToggleFavorito = reporteViewModel::alternarFavorito
            )
        }

        composable(
            route = AppRoutes.REPORTE_FORM_PATTERN,
            arguments = listOf(
                navArgument("reporteId") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) {
            val formViewModel: FormularioReporteViewModel = hiltViewModel()
            val uiState by formViewModel.uiState.collectAsStateWithLifecycle()
            val operacionState by formViewModel.operacionState.collectAsStateWithLifecycle()
            val context = LocalContext.current

            val cameraLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.TakePicturePreview()
            ) { bitmap: Bitmap? ->
                if (bitmap != null) {
                    val file = File(context.filesDir, "evidencia_foto_${System.currentTimeMillis()}.jpg")
                    runCatching {
                        FileOutputStream(file).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }
                        formViewModel.onEvidenciaFotoChange(file.absolutePath)
                    }
                }
            }

            val imagePickerLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let { formViewModel.onEvidenciaFotoChange(it.toString()) }
            }

            val filePickerLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let { formViewModel.onEvidenciaArchivoChange(it.toString()) }
            }

            LaunchedEffect(operacionState) {
                if (operacionState is OperacionUiState.Exito) {
                    formViewModel.resetOperacion()
                    navController.popBackStack()
                }
            }

            FormularioActividad(
                uiState = uiState,
                onTituloChange = formViewModel::onTituloChange,
                onDescripcionChange = formViewModel::onDescripcionChange,
                onFechaChange = formViewModel::onFechaChange,
                onPrioridadChange = formViewModel::onPrioridadChange,
                onProgresoChange = formViewModel::onProgresoChange,
                onCategoriaChange = formViewModel::onCategoriaChange,
                onTomarFoto = { cameraLauncher.launch(null) },
                onSubirFoto = { imagePickerLauncher.launch("image/*") },
                onSubirArchivo = { filePickerLauncher.launch("*/*") },
                onLimpiar = formViewModel::limpiar,
                onGuardar = formViewModel::guardar,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = AppRoutes.REPORTE_DETAIL_PATTERN,
            arguments = listOf(
                navArgument("reporteId") {
                    type = NavType.IntType
                }
            )
        ) {
            val detailViewModel: DetalleReporteViewModel = hiltViewModel()
            val reporte by detailViewModel.reporte.collectAsStateWithLifecycle()

            DetalleReporteScreen(
                reporte = reporte,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = AppRoutes.ACTIVIDAD_DETAIL_PATTERN,
            arguments = listOf(
                navArgument("actividadId") {
                    type = NavType.IntType
                }
            )
        ) {
            val detailViewModel: DetalleActividadViewModel = hiltViewModel()
            val actividad by detailViewModel.actividad.collectAsStateWithLifecycle()

            DetalleActividad(
                actividad = actividad,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
