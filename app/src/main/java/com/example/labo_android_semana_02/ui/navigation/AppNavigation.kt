package com.example.labo_android_semana_02.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.labo_android_semana_02.ui.components.EstadoCargandoPantalla
import com.example.labo_android_semana_02.ui.screens.ActividadEntregasScreen
import com.example.labo_android_semana_02.ui.screens.AuthScreen
import com.example.labo_android_semana_02.ui.screens.CursoPanelScreen
import com.example.labo_android_semana_02.ui.screens.CursosScreen
import com.example.labo_android_semana_02.ui.screens.EntregaAprendizScreen
import com.example.labo_android_semana_02.ui.screens.ForoHiloScreen
import com.example.labo_android_semana_02.ui.viewmodel.ActividadEntregasViewModel
import com.example.labo_android_semana_02.ui.viewmodel.AuthSessionState
import com.example.labo_android_semana_02.ui.viewmodel.AuthViewModel
import com.example.labo_android_semana_02.ui.viewmodel.CursoPanelViewModel
import com.example.labo_android_semana_02.ui.viewmodel.CursosViewModel
import com.example.labo_android_semana_02.ui.viewmodel.EntregaAprendizViewModel
import com.example.labo_android_semana_02.ui.viewmodel.ForoHiloViewModel

object AppRoutes {
    const val CURSOS = "cursos"
    const val CURSO_PANEL = "curso/{cursoId}"
    const val ACTIVIDAD_ENTREGAS = "actividad/{actividadId}/entregas"
    const val ENTREGA_APRENDIZ = "curso/{cursoId}/actividad/{actividadId}/entrega"
    const val FORO_HILO = "foro/{hiloId}"

    fun cursoPanel(cursoId: Int) = "curso/$cursoId"
    fun actividadEntregas(actividadId: Int) = "actividad/$actividadId/entregas"
    fun entregaAprendiz(cursoId: Int, actividadId: Int) = "curso/$cursoId/actividad/$actividadId/entrega"
    fun foroHilo(hiloId: Int) = "foro/$hiloId"
}

@Composable
fun AppNavigation(
    temaOscuro: Boolean,
    onAlternarTema: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val sessionState by authViewModel.sessionState.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()

    when (sessionState) {
        AuthSessionState.Loading -> {
            EstadoCargandoPantalla("Iniciando CTMA Cursos...")
        }
        AuthSessionState.Unauthenticated -> {
            AuthScreen(
                state = authUiState,
                onNombreChange = authViewModel::onNombreChange,
                onEmailChange = authViewModel::onEmailChange,
                onPasswordChange = authViewModel::onPasswordChange,
                onRolChange = authViewModel::onRolChange,
                onAlternarModo = authViewModel::alternarModo,
                onEnviar = authViewModel::enviarFormulario,
                onLimpiarError = authViewModel::limpiarError,
                onAlternarTema = onAlternarTema,
                temaOscuro = temaOscuro,
            )
        }
        is AuthSessionState.Authenticated -> {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = AppRoutes.CURSOS,
            ) {
                composable(AppRoutes.CURSOS) {
                    val vm: CursosViewModel = hiltViewModel()
                    val st by vm.uiState.collectAsState()
                    CursosScreen(
                        state = st,
                        temaOscuro = temaOscuro,
                        onAlternarTema = onAlternarTema,
                        onRefrescar = vm::refrescar,
                        onFiltroChange = vm::cambiarFiltro,
                        onBusquedaChange = vm::cambiarBusqueda,
                        onAbrirCurso = { cursoId ->
                            navController.navigate(AppRoutes.cursoPanel(cursoId))
                        },
                        onCrearCurso = vm::crearCurso,
                        onEditarCurso = vm::editarCurso,
                        onEliminarCurso = vm::eliminarCurso,
                        onUnirseCurso = vm::unirseACurso,
                        onSalirseCurso = vm::salirseDeCurso,
                        onCerrarSesion = vm::cerrarSesion,
                        onLimpiarFeedback = vm::limpiarFeedback,
                    )
                }

                composable(
                    route = AppRoutes.CURSO_PANEL,
                    arguments = listOf(navArgument("cursoId") { type = NavType.IntType }),
                ) {
                    val vm: CursoPanelViewModel = hiltViewModel()
                    val st by vm.uiState.collectAsState()
                    CursoPanelScreen(
                        state = st,
                        onVolver = { navController.popBackStack() },
                        onRefrescar = vm::cargarTodo,
                        onRegenerarCodigo = vm::regenerarCodigo,
                        onCambiarEstadoCodigo = vm::cambiarEstadoCodigo,
                        onValidarYLeerArchivo = vm::validarYLeerArchivo,
                        onGuardarActividad = vm::guardarActividad,
                        onEliminarActividad = vm::eliminarActividad,
                        onAbrirEntregasInstructor = { actId ->
                            navController.navigate(AppRoutes.actividadEntregas(actId))
                        },
                        onAbrirEntregaAprendiz = { actId ->
                            navController.navigate(AppRoutes.entregaAprendiz(vm.cursoId, actId))
                        },
                        onAbrirAdjunto = vm::abrirAdjunto,
                        onCrearMaterial = vm::crearMaterial,
                        onEliminarMaterial = vm::eliminarMaterial,
                        onAbrirMaterial = vm::abrirMaterial,
                        onCambiarModoForo = vm::cambiarModoForo,
                        onCambiarFiltroAprendizForo = vm::cambiarFiltroAprendizForo,
                        onCrearHiloForo = vm::crearHiloForo,
                        onAbrirHiloForo = { hiloId ->
                            navController.navigate(AppRoutes.foroHilo(hiloId))
                        },
                        onExpulsarEstudiante = vm::expulsarEstudiante,
                        onSalirseDelCurso = {
                            vm.salirseDelCurso { navController.popBackStack() }
                        },
                        onConvertir05A100 = vm::convertir05A100,
                        onLimpiarFeedback = vm::limpiarFeedback,
                    )
                }

                composable(
                    route = AppRoutes.ACTIVIDAD_ENTREGAS,
                    arguments = listOf(navArgument("actividadId") { type = NavType.IntType }),
                ) {
                    val vm: ActividadEntregasViewModel = hiltViewModel()
                    val st by vm.uiState.collectAsState()
                    ActividadEntregasScreen(
                        state = st,
                        onVolver = { navController.popBackStack() },
                        onRefrescar = vm::cargarEntregas,
                        onAbrirArchivo = vm::abrirArchivo,
                        onCalificar = vm::calificar,
                        onLimpiarFeedback = vm::limpiarFeedback,
                    )
                }

                composable(
                    route = AppRoutes.ENTREGA_APRENDIZ,
                    arguments = listOf(
                        navArgument("cursoId") { type = NavType.IntType },
                        navArgument("actividadId") { type = NavType.IntType },
                    ),
                ) {
                    val vm: EntregaAprendizViewModel = hiltViewModel()
                    val st by vm.uiState.collectAsState()
                    EntregaAprendizScreen(
                        state = st,
                        onVolver = { navController.popBackStack() },
                        onDescripcionChange = vm::onDescripcionChange,
                        onConservarArchivosChange = vm::onConservarArchivosChange,
                        onAgregarUri = vm::agregarArchivo,
                        onQuitarArchivoNuevo = vm::quitarArchivoNuevo,
                        onEnviarEntrega = vm::enviarEntrega,
                        onAbrirArchivo = vm::abrirArchivo,
                        convertirNotaA05Y0100 = vm::convertirNotaA05Y0100,
                        onLimpiarFeedback = vm::limpiarFeedback,
                    )
                }

                composable(
                    route = AppRoutes.FORO_HILO,
                    arguments = listOf(navArgument("hiloId") { type = NavType.IntType }),
                ) {
                    val vm: ForoHiloViewModel = hiltViewModel()
                    val st by vm.uiState.collectAsState()
                    ForoHiloScreen(
                        state = st,
                        onVolver = { navController.popBackStack() },
                        onRefrescar = vm::cargar,
                        onRespuestaChange = vm::onRespuestaChange,
                        onEnviarRespuesta = vm::enviarRespuesta,
                        onLimpiarError = vm::limpiarError,
                    )
                }
            }
        }
    }
}
