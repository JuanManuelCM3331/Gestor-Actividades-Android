package com.example.labo_android_semana_02.domain.repository

import com.example.labo_android_semana_02.domain.model.Actividad
import com.example.labo_android_semana_02.domain.model.ActividadEntregasGrupo
import com.example.labo_android_semana_02.domain.model.ArchivoLocal
import com.example.labo_android_semana_02.domain.model.AuthSession
import com.example.labo_android_semana_02.domain.model.Curso
import com.example.labo_android_semana_02.domain.model.EntregaDetalle
import com.example.labo_android_semana_02.domain.model.EstadoCurso
import com.example.labo_android_semana_02.domain.model.HiloForo
import com.example.labo_android_semana_02.domain.model.HiloForoDetalle
import com.example.labo_android_semana_02.domain.model.MaterialApoyo
import com.example.labo_android_semana_02.domain.model.MensajeForo
import com.example.labo_android_semana_02.domain.model.PersonaCurso
import com.example.labo_android_semana_02.domain.model.PersonasCurso
import com.example.labo_android_semana_02.domain.model.ResumenCalificacionEstudiante
import com.example.labo_android_semana_02.domain.model.RolUsuario
import com.example.labo_android_semana_02.domain.model.TipoHiloForo
import com.example.labo_android_semana_02.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val sessionFlow: Flow<AuthSession?>
    suspend fun login(email: String, password: String): Result<AuthSession>
    suspend fun register(nombre: String, email: String, password: String, rol: RolUsuario): Result<AuthSession>
    suspend fun refreshMe(): Result<Usuario>
    suspend fun logout()
}

interface PreferenciasRepository {
    val temaOscuroFlow: Flow<Boolean>
    suspend fun guardarTemaOscuro(oscuro: Boolean)
}

interface CursoRepository {
    fun observarCursos(usuarioId: Int, rol: RolUsuario): Flow<List<Curso>>
    suspend fun sincronizarCursos(usuarioId: Int, rol: RolUsuario): Result<List<Curso>>
    suspend fun obtenerCurso(cursoId: Int): Result<Curso>
    suspend fun crearCurso(nombre: String, descripcion: String, estado: EstadoCurso): Result<Curso>
    suspend fun actualizarCurso(cursoId: Int, nombre: String, descripcion: String, estado: EstadoCurso): Result<Curso>
    suspend fun eliminarCurso(cursoId: Int): Result<Unit>
    suspend fun unirseACurso(codigoAcceso: String): Result<Int>
    suspend fun salirseDeCurso(cursoId: Int): Result<Unit>
    suspend fun regenerarCodigo(cursoId: Int): Result<Pair<String, Boolean>>
    suspend fun cambiarEstadoCodigo(cursoId: Int, activo: Boolean): Result<Pair<String, Boolean>>
    fun observarPersonas(cursoId: Int): Flow<List<PersonaCurso>>
    suspend fun obtenerPersonas(cursoId: Int): Result<PersonasCurso>
    suspend fun expulsarEstudiante(cursoId: Int, aprendizId: Int): Result<Unit>
    suspend fun obtenerMiCalificacion(cursoId: Int): Result<ResumenCalificacionEstudiante>
    suspend fun obtenerCalificacionesCurso(cursoId: Int): Result<List<ResumenCalificacionEstudiante>>
}

interface ActividadRepository {
    fun observarActividades(cursoId: Int): Flow<List<Actividad>>
    suspend fun sincronizarActividades(cursoId: Int): Result<List<Actividad>>
    suspend fun crearActividad(
        cursoId: Int,
        titulo: String,
        descripcion: String,
        fechaLimite: Long,
        puntajeMaximo: Double,
        adjuntos: List<ArchivoLocal>,
    ): Result<Actividad>
    suspend fun actualizarActividad(
        actividadId: Int,
        titulo: String,
        descripcion: String,
        fechaLimite: Long,
        puntajeMaximo: Double,
        adjuntos: List<ArchivoLocal>,
    ): Result<Actividad>
    suspend fun eliminarActividad(actividadId: Int, cursoId: Int): Result<Unit>
    suspend fun obtenerEntregasActividad(actividadId: Int): Result<ActividadEntregasGrupo>
    suspend fun enviarEntrega(
        actividadId: Int,
        descripcion: String,
        conservarArchivos: Boolean,
        archivos: List<ArchivoLocal>,
    ): Result<EntregaDetalle>
    suspend fun calificarEntrega(
        actividadId: Int,
        aprendizId: Int,
        nota: Double?,
        retroalimentacion: String?,
        exenta: Boolean,
    ): Result<Unit>
}

interface MaterialRepository {
    fun observarMateriales(cursoId: Int): Flow<List<MaterialApoyo>>
    suspend fun sincronizarMateriales(cursoId: Int): Result<List<MaterialApoyo>>
    suspend fun crearMaterialEnlace(
        cursoId: Int,
        titulo: String,
        descripcion: String,
        enlaceUrl: String,
    ): Result<MaterialApoyo>
    suspend fun crearMaterialArchivo(
        cursoId: Int,
        titulo: String,
        descripcion: String,
        archivo: ArchivoLocal,
    ): Result<MaterialApoyo>
    suspend fun eliminarMaterial(materialId: Int, cursoId: Int): Result<Unit>
}

interface ForoRepository {
    suspend fun listarMensajesChat(
        cursoId: Int,
        tipo: TipoHiloForo,
        aprendizId: Int? = null,
    ): Result<List<MensajeForo>>
    suspend fun enviarMensajeChat(
        cursoId: Int,
        tipo: TipoHiloForo,
        contenido: String,
        enlaceUrl: String? = null,
        aprendizObjetivoId: Int? = null,
        archivos: List<ArchivoLocal> = emptyList(),
    ): Result<MensajeForo>
    suspend fun listarHilos(
        cursoId: Int,
        tipo: TipoHiloForo,
        aprendizId: Int? = null,
    ): Result<List<HiloForo>>
    suspend fun crearHilo(
        cursoId: Int,
        tipo: TipoHiloForo,
        titulo: String,
        contenido: String,
        aprendizObjetivoId: Int? = null,
    ): Result<HiloForo>
    suspend fun obtenerDetalleHilo(hiloId: Int): Result<HiloForoDetalle>
    suspend fun responderHilo(
        hiloId: Int,
        contenido: String,
        enlaceUrl: String? = null,
        archivos: List<ArchivoLocal> = emptyList(),
    ): Result<MensajeForo>
}
