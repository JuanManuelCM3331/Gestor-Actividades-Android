package com.example.labo_android_semana_02.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.labo_android_semana_02.data.remote.FileDownloadManager
import com.example.labo_android_semana_02.data.remote.MultipartHelper
import com.example.labo_android_semana_02.domain.model.Actividad
import com.example.labo_android_semana_02.domain.model.ArchivoAdjunto
import com.example.labo_android_semana_02.domain.model.ArchivoLocal
import com.example.labo_android_semana_02.domain.model.Curso
import com.example.labo_android_semana_02.domain.model.HiloForo
import com.example.labo_android_semana_02.domain.model.MaterialApoyo
import com.example.labo_android_semana_02.domain.model.MensajeForo
import com.example.labo_android_semana_02.domain.model.PersonasCurso
import com.example.labo_android_semana_02.domain.model.ResumenCalificacionEstudiante
import com.example.labo_android_semana_02.domain.model.RolUsuario
import com.example.labo_android_semana_02.domain.model.TipoHiloForo
import com.example.labo_android_semana_02.domain.model.TipoMaterial
import com.example.labo_android_semana_02.domain.model.Usuario
import com.example.labo_android_semana_02.domain.repository.ActividadRepository
import com.example.labo_android_semana_02.domain.repository.AuthRepository
import com.example.labo_android_semana_02.domain.repository.CursoRepository
import com.example.labo_android_semana_02.domain.repository.ForoRepository
import com.example.labo_android_semana_02.domain.repository.MaterialRepository
import com.example.labo_android_semana_02.domain.usecase.CalcularCalificacionCursoUseCase
import com.example.labo_android_semana_02.domain.usecase.GenerarExcelCalificacionesUseCase
import com.example.labo_android_semana_02.domain.usecase.ValidarFormulariosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CursoPanelUiState(
    val cursoId: Int = 0,
    val usuario: Usuario? = null,
    val curso: Curso? = null,
    val actividades: List<Actividad> = emptyList(),
    val materiales: List<MaterialApoyo> = emptyList(),
    val hilosPublicos: List<HiloForo> = emptyList(),
    val hilosPrivados: List<HiloForo> = emptyList(),
    val mensajesChatPublico: List<MensajeForo> = emptyList(),
    val mensajesChatPrivado: List<MensajeForo> = emptyList(),
    val modoForoPrivado: Boolean = false,
    val filtroAprendizForoId: Int? = null,
    val chatTexto: String = "",
    val chatEnlaceUrl: String = "",
    val mostrarCampoEnlaceChat: Boolean = false,
    val chatArchivosPendientes: List<ArchivoLocal> = emptyList(),
    val enviandoMensajeChat: Boolean = false,
    val personas: PersonasCurso? = null,
    val miCalificacion: ResumenCalificacionEstudiante? = null,
    val calificacionesCurso: List<ResumenCalificacionEstudiante> = emptyList(),
    val cargando: Boolean = false,
    val procesandoArchivo: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null,
)

@HiltViewModel
class CursoPanelViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val cursoRepository: CursoRepository,
    private val actividadRepository: ActividadRepository,
    private val materialRepository: MaterialRepository,
    private val foroRepository: ForoRepository,
    private val multipartHelper: MultipartHelper,
    private val fileDownloadManager: FileDownloadManager,
) : ViewModel() {

    val cursoId: Int = checkNotNull(savedStateHandle["cursoId"])
    private val validador = ValidarFormulariosUseCase()
    private val calculadorNotas = CalcularCalificacionCursoUseCase()
    private val generadorExcel = GenerarExcelCalificacionesUseCase()

    private val _uiState = MutableStateFlow(CursoPanelUiState(cursoId = cursoId))
    val uiState: StateFlow<CursoPanelUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.sessionFlow.collect { session ->
                _uiState.update { it.copy(usuario = session?.usuario) }
            }
        }
        viewModelScope.launch {
            actividadRepository.observarActividades(cursoId).collect { list ->
                _uiState.update { it.copy(actividades = list) }
            }
        }
        viewModelScope.launch {
            materialRepository.observarMateriales(cursoId).collect { list ->
                _uiState.update { it.copy(materiales = list) }
            }
        }
        cargarTodo()
    }

    fun cargarTodo() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            val session = authRepository.sessionFlow.firstOrNull()
            val rol = session?.usuario?.rol ?: _uiState.value.usuario?.rol

            val cursoDef = async { cursoRepository.obtenerCurso(cursoId) }
            val actsDef = async { actividadRepository.sincronizarActividades(cursoId) }
            val matsDef = async { materialRepository.sincronizarMateriales(cursoId) }
            val persDef = async { cursoRepository.obtenerPersonas(cursoId) }
            val chatPubDef = async { foroRepository.listarMensajesChat(cursoId, TipoHiloForo.PUBLICO) }

            val cursoRes = cursoDef.await()
            val actsRes = actsDef.await()
            val matsRes = matsDef.await()
            val persRes = persDef.await()
            val chatPubRes = chatPubDef.await()

            val personasCargadas = persRes.getOrNull() ?: _uiState.value.personas
            val aprendizPrivadoId = if (rol == RolUsuario.INSTRUCTOR) {
                _uiState.value.filtroAprendizForoId ?: personasCargadas?.aprendices?.firstOrNull()?.id
            } else {
                null
            }

            val chatPrivRes = if (rol == RolUsuario.APRENDIZ || aprendizPrivadoId != null) {
                foroRepository.listarMensajesChat(cursoId, TipoHiloForo.PRIVADO, aprendizPrivadoId)
            } else {
                Result.success(emptyList())
            }

            var miCalif: ResumenCalificacionEstudiante? = _uiState.value.miCalificacion
            var listaCalifs: List<ResumenCalificacionEstudiante> = _uiState.value.calificacionesCurso
            if (rol == RolUsuario.APRENDIZ) {
                cursoRepository.obtenerMiCalificacion(cursoId).onSuccess { miCalif = it }
            }
            cursoRepository.obtenerCalificacionesCurso(cursoId).onSuccess { listaCalifs = it }
            if (listaCalifs.isEmpty() && miCalif != null) {
                listaCalifs = listOf(miCalif!!)
            }

            val primerError = listOf(cursoRes, actsRes, matsRes, persRes, chatPubRes, chatPrivRes)
                .firstOrNull { it.isFailure }
                ?.exceptionOrNull()
                ?.message

            _uiState.update { prev ->
                prev.copy(
                    curso = cursoRes.getOrNull() ?: prev.curso,
                    personas = personasCargadas,
                    filtroAprendizForoId = aprendizPrivadoId,
                    mensajesChatPublico = chatPubRes.getOrNull() ?: prev.mensajesChatPublico,
                    mensajesChatPrivado = chatPrivRes.getOrNull() ?: prev.mensajesChatPrivado,
                    miCalificacion = miCalif,
                    calificacionesCurso = listaCalifs,
                    cargando = false,
                    error = primerError ?: prev.error,
                )
            }
        }
    }

    fun convertir05A100(nota05: Double): Double = calculadorNotas.convertir05A100(nota05)

    fun validarYLeerArchivo(uri: Uri): ArchivoLocal? {
        val result = multipartHelper.leerYValidarUri(uri)
        if (result.isFailure) {
            _uiState.update { it.copy(error = result.exceptionOrNull()?.message) }
            return null
        }
        return result.getOrNull()
    }

    fun regenerarCodigo() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            cursoRepository.regenerarCodigo(cursoId).fold(
                onSuccess = { (codigo, activo) ->
                    _uiState.update { prev ->
                        prev.copy(
                            cargando = false,
                            curso = prev.curso?.copy(codigoAcceso = codigo, codigoActivo = activo),
                            mensaje = "Nuevo código generado: $codigo",
                        )
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(cargando = false, error = e.message) } },
            )
        }
    }

    fun cambiarEstadoCodigo(activo: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            cursoRepository.cambiarEstadoCodigo(cursoId, activo).fold(
                onSuccess = { (codigo, nextActivo) ->
                    _uiState.update { prev ->
                        prev.copy(
                            cargando = false,
                            curso = prev.curso?.copy(codigoAcceso = codigo, codigoActivo = nextActivo),
                            mensaje = if (nextActivo) "Código de acceso activado." else "Código de acceso desactivado.",
                        )
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(cargando = false, error = e.message) } },
            )
        }
    }

    fun guardarActividad(
        actividadExistenteId: Int?,
        titulo: String,
        descripcion: String,
        fechaLimiteMs: Long,
        puntajeMaximoTexto: String,
        adjuntos: List<ArchivoLocal>,
    ) {
        val errorVal = validador.validarActividad(titulo, puntajeMaximoTexto)
        if (errorVal != null) {
            _uiState.update { it.copy(error = errorVal) }
            return
        }
        val puntaje = puntajeMaximoTexto.replace(',', '.').toDouble()
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            val res = if (actividadExistenteId == null) {
                actividadRepository.crearActividad(cursoId, titulo, descripcion, fechaLimiteMs, puntaje, adjuntos)
            } else {
                actividadRepository.actualizarActividad(actividadExistenteId, titulo, descripcion, fechaLimiteMs, puntaje, adjuntos)
            }
            res.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            mensaje = if (actividadExistenteId == null) "Actividad publicada." else "Actividad actualizada."
                        )
                    }
                    cargarTodo()
                },
                onFailure = { e -> _uiState.update { it.copy(cargando = false, error = e.message) } },
            )
        }
    }

    fun eliminarActividad(actividadId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            actividadRepository.eliminarActividad(actividadId, cursoId).fold(
                onSuccess = {
                    _uiState.update { it.copy(mensaje = "Actividad eliminada.") }
                    cargarTodo()
                },
                onFailure = { e -> _uiState.update { it.copy(cargando = false, error = e.message) } },
            )
        }
    }

    fun crearMaterial(
        titulo: String,
        descripcion: String,
        esEnlace: Boolean,
        enlaceUrl: String,
        archivo: ArchivoLocal?,
    ) {
        val errorVal = validador.validarMaterial(titulo, esEnlace, enlaceUrl, archivo)
        if (errorVal != null) {
            _uiState.update { it.copy(error = errorVal) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            val res = if (esEnlace) {
                materialRepository.crearMaterialEnlace(cursoId, titulo, descripcion, enlaceUrl)
            } else {
                materialRepository.crearMaterialArchivo(cursoId, titulo, descripcion, archivo!!)
            }
            res.fold(
                onSuccess = {
                    _uiState.update { it.copy(mensaje = "Material publicado correctamente.") }
                    cargarTodo()
                },
                onFailure = { e -> _uiState.update { it.copy(cargando = false, error = e.message) } },
            )
        }
    }

    fun eliminarMaterial(materialId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            materialRepository.eliminarMaterial(materialId, cursoId).fold(
                onSuccess = {
                    _uiState.update { it.copy(mensaje = "Material eliminado.") }
                    cargarTodo()
                },
                onFailure = { e -> _uiState.update { it.copy(cargando = false, error = e.message) } },
            )
        }
    }

    fun abrirAdjunto(adjunto: ArchivoAdjunto) {
        viewModelScope.launch {
            _uiState.update { it.copy(procesandoArchivo = true) }
            fileDownloadManager.descargarYAbrir(
                urlRelativaOCompleta = adjunto.urlDescarga,
                nombreArchivo = adjunto.nombreOriginal,
                mimeType = adjunto.mimeType,
            ).onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: "No se pudo abrir el archivo.") }
            }
            _uiState.update { it.copy(procesandoArchivo = false) }
        }
    }

    fun abrirMaterial(material: MaterialApoyo) {
        if (material.tipo == TipoMaterial.ENLACE) {
            val link = material.enlaceUrl ?: material.urlDescarga
            fileDownloadManager.abrirEnlaceExterno(link).onFailure {
                _uiState.update { st -> st.copy(error = "No hay navegador o app disponible para abrir el enlace.") }
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(procesandoArchivo = true) }
            fileDownloadManager.descargarYAbrir(
                urlRelativaOCompleta = material.urlDescarga,
                nombreArchivo = material.nombreOriginal ?: material.titulo,
                mimeType = material.mimeType,
            ).onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: "No se pudo descargar el material.") }
            }
            _uiState.update { it.copy(procesandoArchivo = false) }
        }
    }

    fun abrirEnlaceExterno(url: String) {
        fileDownloadManager.abrirEnlaceExterno(url).onFailure {
            _uiState.update { st -> st.copy(error = "No hay navegador o aplicación disponible para abrir el enlace.") }
        }
    }

    suspend fun descargarArchivoEnCache(adjunto: ArchivoAdjunto): Result<File> =
        fileDownloadManager.descargarEnCache(adjunto.urlDescarga, "${adjunto.id}_${adjunto.nombreOriginal}")

    fun cambiarModoForo(privado: Boolean) {
        _uiState.update { prev ->
            val defaultAprendiz = if (privado && prev.usuario?.rol == RolUsuario.INSTRUCTOR && prev.filtroAprendizForoId == null) {
                prev.personas?.aprendices?.firstOrNull()?.id
            } else {
                prev.filtroAprendizForoId
            }
            prev.copy(modoForoPrivado = privado, filtroAprendizForoId = defaultAprendiz)
        }
        refrescarForo()
    }

    fun cambiarFiltroAprendizForo(aprendizId: Int?) {
        _uiState.update { it.copy(filtroAprendizForoId = aprendizId) }
        refrescarForo()
    }

    fun onChatTextoChange(texto: String) {
        _uiState.update { it.copy(chatTexto = texto, error = null) }
    }

    fun onChatEnlaceChange(url: String) {
        _uiState.update { it.copy(chatEnlaceUrl = url, error = null) }
    }

    fun alternarCampoEnlaceChat() {
        _uiState.update { prev ->
            val mostrar = !prev.mostrarCampoEnlaceChat
            val urlInicial = if (mostrar && prev.chatEnlaceUrl.isBlank()) "https://" else if (!mostrar) "" else prev.chatEnlaceUrl
            prev.copy(mostrarCampoEnlaceChat = mostrar, chatEnlaceUrl = urlInicial)
        }
    }

    fun agregarArchivoChatUri(uri: Uri) {
        val archivo = validarYLeerArchivo(uri) ?: return
        _uiState.update { prev ->
            prev.copy(chatArchivosPendientes = (prev.chatArchivosPendientes + archivo).take(10))
        }
    }

    fun agregarAudioGrabadoChat(file: File) {
        val res = multipartHelper.leerYValidarFile(file, "audio/mp4")
        res.fold(
            onSuccess = { archivo ->
                _uiState.update { prev ->
                    prev.copy(chatArchivosPendientes = (prev.chatArchivosPendientes + archivo).take(10))
                }
            },
            onFailure = { e -> _uiState.update { it.copy(error = e.message) } },
        )
    }

    fun quitarArchivoChat(index: Int) {
        _uiState.update { prev ->
            prev.copy(chatArchivosPendientes = prev.chatArchivosPendientes.filterIndexed { i, _ -> i != index })
        }
    }

    fun enviarMensajeChat() {
        val st = _uiState.value
        val esInstructor = st.usuario?.rol == RolUsuario.INSTRUCTOR
        val targetId = if (st.modoForoPrivado && esInstructor) st.filtroAprendizForoId else null
        val urlParaValidar = if (st.mostrarCampoEnlaceChat && st.chatEnlaceUrl.trim() != "https://") {
            st.chatEnlaceUrl.trim()
        } else {
            ""
        }

        val err = validador.validarMensajeChatForo(
            contenido = st.chatTexto,
            enlaceUrl = urlParaValidar,
            cantidadArchivos = st.chatArchivosPendientes.size,
            esPrivado = st.modoForoPrivado,
            esInstructor = esInstructor,
            aprendizObjetivoId = targetId,
        )
        if (err != null) {
            _uiState.update { it.copy(error = err) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(enviandoMensajeChat = true, error = null) }
            val tipo = if (st.modoForoPrivado) TipoHiloForo.PRIVADO else TipoHiloForo.PUBLICO
            foroRepository.enviarMensajeChat(
                cursoId = cursoId,
                tipo = tipo,
                contenido = st.chatTexto,
                enlaceUrl = urlParaValidar.ifEmpty { null },
                aprendizObjetivoId = targetId,
                archivos = st.chatArchivosPendientes,
            ).fold(
                onSuccess = { nuevo ->
                    _uiState.update { prev ->
                        if (tipo == TipoHiloForo.PRIVADO) {
                            prev.copy(
                                enviandoMensajeChat = false,
                                chatTexto = "",
                                chatEnlaceUrl = "",
                                mostrarCampoEnlaceChat = false,
                                chatArchivosPendientes = emptyList(),
                                mensajesChatPrivado = prev.mensajesChatPrivado + nuevo,
                            )
                        } else {
                            prev.copy(
                                enviandoMensajeChat = false,
                                chatTexto = "",
                                chatEnlaceUrl = "",
                                mostrarCampoEnlaceChat = false,
                                chatArchivosPendientes = emptyList(),
                                mensajesChatPublico = prev.mensajesChatPublico + nuevo,
                            )
                        }
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(enviandoMensajeChat = false, error = e.message) }
                },
            )
        }
    }

    private fun refrescarForo() {
        viewModelScope.launch {
            val st = _uiState.value
            val tipo = if (st.modoForoPrivado) TipoHiloForo.PRIVADO else TipoHiloForo.PUBLICO
            val filtro = if (st.modoForoPrivado && st.usuario?.rol == RolUsuario.INSTRUCTOR) st.filtroAprendizForoId else null
            if (st.modoForoPrivado && st.usuario?.rol == RolUsuario.INSTRUCTOR && filtro == null) {
                _uiState.update { it.copy(mensajesChatPrivado = emptyList()) }
                return@launch
            }
            foroRepository.listarMensajesChat(cursoId, tipo, filtro).fold(
                onSuccess = { mensajes ->
                    _uiState.update { prev ->
                        if (tipo == TipoHiloForo.PRIVADO) prev.copy(mensajesChatPrivado = mensajes)
                        else prev.copy(mensajesChatPublico = mensajes)
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } },
            )
        }
    }

    fun crearHiloForo(titulo: String, contenido: String, privado: Boolean, aprendizObjetivoId: Int?) {
        val esInstructor = _uiState.value.usuario?.rol == RolUsuario.INSTRUCTOR
        val err = validador.validarHiloForo(titulo, contenido, privado, esInstructor, aprendizObjetivoId)
        if (err != null) {
            _uiState.update { it.copy(error = err) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            val tipo = if (privado) TipoHiloForo.PRIVADO else TipoHiloForo.PUBLICO
            foroRepository.crearHilo(cursoId, tipo, titulo, contenido, aprendizObjetivoId).fold(
                onSuccess = {
                    _uiState.update { it.copy(cargando = false, mensaje = "Mensaje publicado en el foro.") }
                    refrescarForo()
                },
                onFailure = { e -> _uiState.update { it.copy(cargando = false, error = e.message) } },
            )
        }
    }

    fun expulsarEstudiante(aprendizId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            cursoRepository.expulsarEstudiante(cursoId, aprendizId).fold(
                onSuccess = {
                    _uiState.update { it.copy(mensaje = "Aprendiz expulsado del curso.") }
                    cargarTodo()
                },
                onFailure = { e -> _uiState.update { it.copy(cargando = false, error = e.message) } },
            )
        }
    }

    fun salirseDelCurso(onSalido: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            cursoRepository.salirseDeCurso(cursoId).fold(
                onSuccess = {
                    _uiState.update { it.copy(cargando = false) }
                    onSalido()
                },
                onFailure = { e -> _uiState.update { it.copy(cargando = false, error = e.message) } },
            )
        }
    }

    fun exportarCalificacionesExcel() {
        val st = _uiState.value
        val calificaciones = if (st.calificacionesCurso.isNotEmpty()) {
            st.calificacionesCurso
        } else {
            listOfNotNull(st.miCalificacion)
        }
        if (calificaciones.isEmpty()) {
            _uiState.update { it.copy(error = "No hay calificaciones registradas para exportar.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(procesandoArchivo = true, error = null) }
            val cursoNombre = st.curso?.nombre ?: "Curso_$cursoId"
            val instructorNombre = st.curso?.instructorNombre ?: st.personas?.instructor?.nombre ?: "Instructor"
            val bytes = generadorExcel(
                cursoNombre = cursoNombre,
                instructorNombre = instructorNombre,
                actividades = st.actividades,
                calificaciones = calificaciones,
            )
            val nombreLimpio = cursoNombre.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(40)
            val nombreArchivo = "Calificaciones_${nombreLimpio}.xlsx"
            fileDownloadManager.guardarYCompartirExcel(bytes, nombreArchivo).fold(
                onSuccess = { guardado ->
                    _uiState.update {
                        it.copy(
                            procesandoArchivo = false,
                            mensaje = "Excel exportado en Descargas: $guardado",
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            procesandoArchivo = false,
                            error = "Error al exportar Excel: ${e.message}",
                        )
                    }
                },
            )
        }
    }

    fun mostrarError(mensajeError: String) = _uiState.update { it.copy(error = mensajeError) }

    fun limpiarFeedback() = _uiState.update { it.copy(error = null, mensaje = null) }
}
