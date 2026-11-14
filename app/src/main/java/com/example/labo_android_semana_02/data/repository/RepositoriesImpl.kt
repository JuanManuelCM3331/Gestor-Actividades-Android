package com.example.labo_android_semana_02.data.repository

import com.example.labo_android_semana_02.data.local.datastore.PreferenciasDataStore
import com.example.labo_android_semana_02.data.local.db.ActividadDao
import com.example.labo_android_semana_02.data.local.db.CursoDao
import com.example.labo_android_semana_02.data.local.db.MaterialDao
import com.example.labo_android_semana_02.data.local.db.PersonaDao
import com.example.labo_android_semana_02.data.mapper.toDomain
import com.example.labo_android_semana_02.data.mapper.toEntity
import com.example.labo_android_semana_02.data.remote.MultipartHelper
import com.example.labo_android_semana_02.data.remote.api.ActividadApi
import com.example.labo_android_semana_02.data.remote.api.AuthApi
import com.example.labo_android_semana_02.data.remote.api.CursoApi
import com.example.labo_android_semana_02.data.remote.api.ForoApi
import com.example.labo_android_semana_02.data.remote.api.MaterialApi
import com.example.labo_android_semana_02.data.remote.dto.CalificarRequestDto
import com.example.labo_android_semana_02.data.remote.dto.CodigoEstadoRequestDto
import com.example.labo_android_semana_02.data.remote.dto.CrearHiloRequestDto
import com.example.labo_android_semana_02.data.remote.dto.CursoUpsertRequestDto
import com.example.labo_android_semana_02.data.remote.dto.LoginRequestDto
import com.example.labo_android_semana_02.data.remote.dto.RegisterRequestDto
import com.example.labo_android_semana_02.data.remote.dto.ResponderHiloRequestDto
import com.example.labo_android_semana_02.data.remote.dto.UnirseCursoRequestDto
import com.example.labo_android_semana_02.data.remote.safeApiCall
import com.example.labo_android_semana_02.data.remote.safeVoidCall
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
import com.example.labo_android_semana_02.domain.repository.ActividadRepository
import com.example.labo_android_semana_02.domain.repository.AuthRepository
import com.example.labo_android_semana_02.domain.repository.CursoRepository
import com.example.labo_android_semana_02.domain.repository.ForoRepository
import com.example.labo_android_semana_02.domain.repository.MaterialRepository
import com.example.labo_android_semana_02.domain.repository.PreferenciasRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenciasRepositoryImpl @Inject constructor(
    private val dataStore: PreferenciasDataStore,
) : PreferenciasRepository {
    override val temaOscuroFlow: Flow<Boolean> = dataStore.temaOscuroFlow
    override suspend fun guardarTemaOscuro(oscuro: Boolean) {
        dataStore.guardarTemaOscuro(oscuro)
    }
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val dataStore: PreferenciasDataStore,
    private val cursoDao: CursoDao,
    private val actividadDao: ActividadDao,
    private val materialDao: MaterialDao,
    private val personaDao: PersonaDao,
) : AuthRepository {
    override val sessionFlow: Flow<AuthSession?> = dataStore.sessionFlow

    override suspend fun login(email: String, password: String): Result<AuthSession> {
        return safeApiCall {
            authApi.login(LoginRequestDto(email = email.trim(), password = password)).toDomain()
        }.onSuccess { session ->
            limpiarCachesLocales()
            dataStore.saveSession(session)
        }
    }

    override suspend fun register(
        nombre: String,
        email: String,
        password: String,
        rol: RolUsuario,
    ): Result<AuthSession> {
        return safeApiCall {
            authApi.register(
                RegisterRequestDto(
                    nombre = nombre.trim(),
                    email = email.trim(),
                    password = password,
                    rol = rol.name,
                )
            ).toDomain()
        }.onSuccess { session ->
            limpiarCachesLocales()
            dataStore.saveSession(session)
        }
    }

    override suspend fun refreshMe(): Result<Usuario> {
        return safeApiCall { authApi.me().toDomain() }.onSuccess {
            dataStore.updateUsuario(it)
        }
    }

    override suspend fun logout() {
        dataStore.clearSession()
        limpiarCachesLocales()
    }

    private suspend fun limpiarCachesLocales() = withContext(Dispatchers.IO) {
        cursoDao.eliminarTodo()
        actividadDao.eliminarTodo()
        materialDao.eliminarTodo()
        personaDao.eliminarTodo()
    }
}

@Singleton
class CursoRepositoryImpl @Inject constructor(
    private val cursoApi: CursoApi,
    private val cursoDao: CursoDao,
    private val personaDao: PersonaDao,
) : CursoRepository {

    override fun observarCursos(usuarioId: Int, rol: RolUsuario): Flow<List<Curso>> =
        cursoDao.observarPorUsuario(usuarioId, rol.name).map { list -> list.map { it.toDomain() } }

    override suspend fun sincronizarCursos(usuarioId: Int, rol: RolUsuario): Result<List<Curso>> {
        return safeApiCall {
            val remotos = cursoApi.listar().map { it.toDomain() }
            withContext(Dispatchers.IO) {
                cursoDao.reemplazarPorUsuario(usuarioId, rol.name, remotos.map { it.toEntity(usuarioId, rol) })
            }
            remotos
        }
    }

    override suspend fun obtenerCurso(cursoId: Int): Result<Curso> =
        safeApiCall { cursoApi.obtener(cursoId).toDomain() }

    override suspend fun crearCurso(nombre: String, descripcion: String, estado: EstadoCurso): Result<Curso> =
        safeApiCall {
            cursoApi.crear(CursoUpsertRequestDto(nombre.trim(), descripcion.trim(), estado.name)).toDomain()
        }

    override suspend fun actualizarCurso(
        cursoId: Int,
        nombre: String,
        descripcion: String,
        estado: EstadoCurso,
    ): Result<Curso> = safeApiCall {
        cursoApi.actualizar(cursoId, CursoUpsertRequestDto(nombre.trim(), descripcion.trim(), estado.name)).toDomain()
    }

    override suspend fun eliminarCurso(cursoId: Int): Result<Unit> =
        safeVoidCall { cursoApi.eliminar(cursoId) }

    override suspend fun unirseACurso(codigoAcceso: String): Result<Int> =
        safeApiCall { cursoApi.unirse(UnirseCursoRequestDto(codigoAcceso.trim().uppercase())).cursoId }

    override suspend fun salirseDeCurso(cursoId: Int): Result<Unit> =
        safeVoidCall { cursoApi.salirse(cursoId) }

    override suspend fun regenerarCodigo(cursoId: Int): Result<Pair<String, Boolean>> =
        safeApiCall {
            val res = cursoApi.regenerarCodigo(cursoId)
            res.codigoAcceso to res.codigoActivo
        }

    override suspend fun cambiarEstadoCodigo(cursoId: Int, activo: Boolean): Result<Pair<String, Boolean>> =
        safeApiCall {
            val res = cursoApi.cambiarEstadoCodigo(cursoId, CodigoEstadoRequestDto(activo))
            res.codigoAcceso to res.codigoActivo
        }

    override fun observarPersonas(cursoId: Int): Flow<List<PersonaCurso>> =
        personaDao.observarPorCurso(cursoId).map { list -> list.map { it.toDomain() } }

    override suspend fun obtenerPersonas(cursoId: Int): Result<PersonasCurso> =
        safeApiCall {
            val personas = cursoApi.listarPersonas(cursoId).toDomain()
            val todas = listOf(personas.instructor) + personas.aprendices
            withContext(Dispatchers.IO) {
                personaDao.reemplazarPorCurso(cursoId, todas.map { it.toEntity(cursoId) })
            }
            personas
        }

    override suspend fun expulsarEstudiante(cursoId: Int, aprendizId: Int): Result<Unit> =
        safeVoidCall { cursoApi.expulsarEstudiante(cursoId, aprendizId) }

    override suspend fun obtenerMiCalificacion(cursoId: Int): Result<ResumenCalificacionEstudiante> =
        safeApiCall { cursoApi.obtenerMiCalificacion(cursoId).toDomain() }

    override suspend fun obtenerCalificacionesCurso(cursoId: Int): Result<List<ResumenCalificacionEstudiante>> =
        safeApiCall { cursoApi.obtenerCalificacionesCurso(cursoId).map { it.toDomain() } }
}

@Singleton
class ActividadRepositoryImpl @Inject constructor(
    private val actividadApi: ActividadApi,
    private val actividadDao: ActividadDao,
    private val multipartHelper: MultipartHelper,
) : ActividadRepository {

    override fun observarActividades(cursoId: Int): Flow<List<Actividad>> =
        actividadDao.observarPorCurso(cursoId).map { list -> list.map { it.toDomain() } }

    override suspend fun sincronizarActividades(cursoId: Int): Result<List<Actividad>> =
        safeApiCall {
            val remotos = actividadApi.listarPorCurso(cursoId).map { it.toDomain() }
            withContext(Dispatchers.IO) {
                actividadDao.reemplazarPorCurso(cursoId, remotos.map { it.toEntity() })
            }
            remotos
        }

    override suspend fun crearActividad(
        cursoId: Int,
        titulo: String,
        descripcion: String,
        fechaLimite: Long,
        puntajeMaximo: Double,
        adjuntos: List<ArchivoLocal>,
    ): Result<Actividad> {
        val parts = mutableListOf<okhttp3.MultipartBody.Part>()
        for (archivo in adjuntos) {
            val partResult = multipartHelper.crearParteArchivo("adjuntos", archivo)
            if (partResult.isFailure) return Result.failure(partResult.exceptionOrNull()!!)
            parts.add(partResult.getOrThrow())
        }
        return safeApiCall {
            actividadApi.crear(
                cursoId = cursoId,
                titulo = multipartHelper.crearParteTexto(titulo.trim()),
                descripcion = multipartHelper.crearParteTexto(descripcion.trim()),
                fechaLimite = multipartHelper.crearParteTexto(fechaLimite.toString()),
                puntajeMaximo = multipartHelper.crearParteTexto(puntajeMaximo.toString()),
                adjuntos = parts,
            ).toDomain()
        }
    }

    override suspend fun actualizarActividad(
        actividadId: Int,
        titulo: String,
        descripcion: String,
        fechaLimite: Long,
        puntajeMaximo: Double,
        adjuntos: List<ArchivoLocal>,
    ): Result<Actividad> {
        val parts = mutableListOf<okhttp3.MultipartBody.Part>()
        for (archivo in adjuntos) {
            val partResult = multipartHelper.crearParteArchivo("adjuntos", archivo)
            if (partResult.isFailure) return Result.failure(partResult.exceptionOrNull()!!)
            parts.add(partResult.getOrThrow())
        }
        return safeApiCall {
            actividadApi.actualizar(
                actividadId = actividadId,
                titulo = multipartHelper.crearParteTexto(titulo.trim()),
                descripcion = multipartHelper.crearParteTexto(descripcion.trim()),
                fechaLimite = multipartHelper.crearParteTexto(fechaLimite.toString()),
                puntajeMaximo = multipartHelper.crearParteTexto(puntajeMaximo.toString()),
                adjuntos = parts,
            ).toDomain()
        }
    }

    override suspend fun eliminarActividad(actividadId: Int, cursoId: Int): Result<Unit> =
        safeVoidCall { actividadApi.eliminar(actividadId) }.onSuccess {
            sincronizarActividades(cursoId)
        }

    override suspend fun obtenerEntregasActividad(actividadId: Int): Result<ActividadEntregasGrupo> =
        safeApiCall { actividadApi.listarEntregas(actividadId).toDomain() }

    override suspend fun enviarEntrega(
        actividadId: Int,
        descripcion: String,
        conservarArchivos: Boolean,
        archivos: List<ArchivoLocal>,
    ): Result<EntregaDetalle> {
        val parts = mutableListOf<okhttp3.MultipartBody.Part>()
        for (archivo in archivos) {
            val partResult = multipartHelper.crearParteArchivo("archivos", archivo)
            if (partResult.isFailure) return Result.failure(partResult.exceptionOrNull()!!)
            parts.add(partResult.getOrThrow())
        }
        return safeApiCall {
            actividadApi.enviarEntrega(
                actividadId = actividadId,
                descripcion = multipartHelper.crearParteTexto(descripcion.trim()),
                conservarArchivos = multipartHelper.crearParteTexto(conservarArchivos.toString()),
                archivos = parts,
            ).toDomain()
        }
    }

    override suspend fun calificarEntrega(
        actividadId: Int,
        aprendizId: Int,
        nota: Double?,
        retroalimentacion: String?,
        exenta: Boolean,
    ): Result<Unit> = safeApiCall {
        actividadApi.calificar(
            actividadId = actividadId,
            aprendizId = aprendizId,
            request = CalificarRequestDto(
                nota = if (exenta) null else nota,
                retroalimentacion = retroalimentacion?.trim(),
                exenta = exenta,
            ),
        )
        Unit
    }
}

@Singleton
class MaterialRepositoryImpl @Inject constructor(
    private val materialApi: MaterialApi,
    private val materialDao: MaterialDao,
    private val multipartHelper: MultipartHelper,
) : MaterialRepository {

    override fun observarMateriales(cursoId: Int): Flow<List<MaterialApoyo>> =
        materialDao.observarPorCurso(cursoId).map { list -> list.map { it.toDomain() } }

    override suspend fun sincronizarMateriales(cursoId: Int): Result<List<MaterialApoyo>> =
        safeApiCall {
            val remotos = materialApi.listarPorCurso(cursoId).map { it.toDomain() }
            withContext(Dispatchers.IO) {
                materialDao.reemplazarPorCurso(cursoId, remotos.map { it.toEntity() })
            }
            remotos
        }

    override suspend fun crearMaterialEnlace(
        cursoId: Int,
        titulo: String,
        descripcion: String,
        enlaceUrl: String,
    ): Result<MaterialApoyo> = safeApiCall {
        materialApi.crearEnlace(
            cursoId = cursoId,
            body = mapOf(
                "titulo" to titulo.trim(),
                "descripcion" to descripcion.trim(),
                "tipo" to "ENLACE",
                "enlace_url" to enlaceUrl.trim(),
            ),
        ).toDomain()
    }

    override suspend fun crearMaterialArchivo(
        cursoId: Int,
        titulo: String,
        descripcion: String,
        archivo: ArchivoLocal,
    ): Result<MaterialApoyo> {
        val partResult = multipartHelper.crearParteArchivo("archivo", archivo)
        if (partResult.isFailure) return Result.failure(partResult.exceptionOrNull()!!)
        return safeApiCall {
            materialApi.crearArchivo(
                cursoId = cursoId,
                titulo = multipartHelper.crearParteTexto(titulo.trim()),
                descripcion = multipartHelper.crearParteTexto(descripcion.trim()),
                tipo = multipartHelper.crearParteTexto("ARCHIVO"),
                archivo = partResult.getOrThrow(),
            ).toDomain()
        }
    }

    override suspend fun eliminarMaterial(materialId: Int, cursoId: Int): Result<Unit> =
        safeVoidCall { materialApi.eliminar(materialId) }.onSuccess {
            sincronizarMateriales(cursoId)
        }
}

@Singleton
class ForoRepositoryImpl @Inject constructor(
    private val foroApi: ForoApi,
    private val multipartHelper: MultipartHelper,
) : ForoRepository {

    override suspend fun listarMensajesChat(
        cursoId: Int,
        tipo: TipoHiloForo,
        aprendizId: Int?,
    ): Result<List<MensajeForo>> = safeApiCall {
        foroApi.listarMensajesChat(cursoId, tipo.name, aprendizId).map { it.toDomain() }
    }

    override suspend fun enviarMensajeChat(
        cursoId: Int,
        tipo: TipoHiloForo,
        contenido: String,
        enlaceUrl: String?,
        aprendizObjetivoId: Int?,
        archivos: List<ArchivoLocal>,
    ): Result<MensajeForo> {
        val parts = mutableListOf<okhttp3.MultipartBody.Part>()
        for (archivo in archivos) {
            val partResult = multipartHelper.crearParteArchivo("archivos", archivo)
            if (partResult.isFailure) return Result.failure(partResult.exceptionOrNull()!!)
            parts.add(partResult.getOrThrow())
        }
        return safeApiCall {
            foroApi.enviarMensajeChat(
                cursoId = cursoId,
                tipo = multipartHelper.crearParteTexto(tipo.name),
                contenido = multipartHelper.crearParteTexto(contenido.trim()),
                enlaceUrl = enlaceUrl?.trim()?.takeIf { it.isNotEmpty() }?.let(multipartHelper::crearParteTexto),
                aprendizObjetivoId = aprendizObjetivoId?.toString()?.let(multipartHelper::crearParteTexto),
                archivos = parts,
            ).toDomain()
        }
    }

    override suspend fun listarHilos(
        cursoId: Int,
        tipo: TipoHiloForo,
        aprendizId: Int?,
    ): Result<List<HiloForo>> = safeApiCall {
        foroApi.listarHilos(cursoId, tipo.name, aprendizId).map { it.toDomain() }
    }

    override suspend fun crearHilo(
        cursoId: Int,
        tipo: TipoHiloForo,
        titulo: String,
        contenido: String,
        aprendizObjetivoId: Int?,
    ): Result<HiloForo> = safeApiCall {
        foroApi.crearHilo(
            cursoId = cursoId,
            request = CrearHiloRequestDto(
                tipo = tipo.name,
                titulo = titulo.trim(),
                contenido = contenido.trim(),
                aprendizObjetivoId = aprendizObjetivoId,
            ),
        ).toDomain()
    }

    override suspend fun obtenerDetalleHilo(hiloId: Int): Result<HiloForoDetalle> =
        safeApiCall { foroApi.obtenerDetalle(hiloId).toDomain() }

    override suspend fun responderHilo(
        hiloId: Int,
        contenido: String,
        enlaceUrl: String?,
        archivos: List<ArchivoLocal>,
    ): Result<MensajeForo> {
        if (archivos.isEmpty() && enlaceUrl.isNullOrBlank()) {
            return safeApiCall {
                foroApi.responder(hiloId, ResponderHiloRequestDto(contenido.trim())).toDomain()
            }
        }
        val parts = mutableListOf<okhttp3.MultipartBody.Part>()
        for (archivo in archivos) {
            val partResult = multipartHelper.crearParteArchivo("archivos", archivo)
            if (partResult.isFailure) return Result.failure(partResult.exceptionOrNull()!!)
            parts.add(partResult.getOrThrow())
        }
        return safeApiCall {
            foroApi.responderMultipart(
                hiloId = hiloId,
                contenido = multipartHelper.crearParteTexto(contenido.trim()),
                enlaceUrl = enlaceUrl?.trim()?.takeIf { it.isNotEmpty() }?.let(multipartHelper::crearParteTexto),
                archivos = parts,
            ).toDomain()
        }
    }
}
