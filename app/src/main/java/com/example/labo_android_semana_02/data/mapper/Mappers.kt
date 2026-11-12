package com.example.labo_android_semana_02.data.mapper

import com.example.labo_android_semana_02.data.local.db.ActividadCursoEntity
import com.example.labo_android_semana_02.data.local.db.CursoEntity
import com.example.labo_android_semana_02.data.local.db.MaterialEntity
import com.example.labo_android_semana_02.data.local.db.PersonaCursoEntity
import com.example.labo_android_semana_02.data.remote.dto.ActividadDto
import com.example.labo_android_semana_02.data.remote.dto.ActividadEntregasGrupoDto
import com.example.labo_android_semana_02.data.remote.dto.ArchivoAdjuntoDto
import com.example.labo_android_semana_02.data.remote.dto.AuthResponseDto
import com.example.labo_android_semana_02.data.remote.dto.CursoDto
import com.example.labo_android_semana_02.data.remote.dto.DetalleCalificacionActividadDto
import com.example.labo_android_semana_02.data.remote.dto.EntregaDetalleDto
import com.example.labo_android_semana_02.data.remote.dto.EntregaInstructorItemDto
import com.example.labo_android_semana_02.data.remote.dto.HiloForoDetalleResponseDto
import com.example.labo_android_semana_02.data.remote.dto.HiloForoDto
import com.example.labo_android_semana_02.data.remote.dto.MaterialApoyoDto
import com.example.labo_android_semana_02.data.remote.dto.MensajeForoDto
import com.example.labo_android_semana_02.data.remote.dto.PersonaCursoDto
import com.example.labo_android_semana_02.data.remote.dto.PersonaSimpleDto
import com.example.labo_android_semana_02.data.remote.dto.PersonasCursoResponseDto
import com.example.labo_android_semana_02.data.remote.dto.ResumenCalificacionEstudianteDto
import com.example.labo_android_semana_02.data.remote.dto.UsuarioDto
import com.example.labo_android_semana_02.domain.model.Actividad
import com.example.labo_android_semana_02.domain.model.ActividadEntregasGrupo
import com.example.labo_android_semana_02.domain.model.ArchivoAdjunto
import com.example.labo_android_semana_02.domain.model.AuthSession
import com.example.labo_android_semana_02.domain.model.Curso
import com.example.labo_android_semana_02.domain.model.DetalleCalificacionActividad
import com.example.labo_android_semana_02.domain.model.EntregaDetalle
import com.example.labo_android_semana_02.domain.model.EntregaInstructorItem
import com.example.labo_android_semana_02.domain.model.EstadoCurso
import com.example.labo_android_semana_02.domain.model.EstadoEntrega
import com.example.labo_android_semana_02.domain.model.HiloForo
import com.example.labo_android_semana_02.domain.model.HiloForoDetalle
import com.example.labo_android_semana_02.domain.model.MaterialApoyo
import com.example.labo_android_semana_02.domain.model.MensajeForo
import com.example.labo_android_semana_02.domain.model.PersonaCurso
import com.example.labo_android_semana_02.domain.model.PersonasCurso
import com.example.labo_android_semana_02.domain.model.ResumenCalificacionEstudiante
import com.example.labo_android_semana_02.domain.model.RolUsuario
import com.example.labo_android_semana_02.domain.model.TipoHiloForo
import com.example.labo_android_semana_02.domain.model.TipoMaterial
import com.example.labo_android_semana_02.domain.model.Usuario
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val roomJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

private fun parseRol(value: String?): RolUsuario =
    if (value.equals("INSTRUCTOR", ignoreCase = true)) RolUsuario.INSTRUCTOR else RolUsuario.APRENDIZ

private fun parseEstadoCurso(value: String?): EstadoCurso =
    if (value.equals("ARCHIVADO", ignoreCase = true)) EstadoCurso.ARCHIVADO else EstadoCurso.ACTIVO

private fun parseEstadoEntrega(value: String?): EstadoEntrega =
    runCatching { EstadoEntrega.valueOf(value.orEmpty()) }.getOrDefault(EstadoEntrega.PENDIENTE)

fun UsuarioDto.toDomain(): Usuario = Usuario(
    id = id,
    nombre = nombre,
    email = email,
    rol = parseRol(rol),
)

fun AuthResponseDto.toDomain(): AuthSession = AuthSession(
    token = token,
    usuario = usuario.toDomain(),
)

fun CursoDto.toDomain(): Curso = Curso(
    id = id,
    instructorId = instructorId,
    instructorNombre = instructorNombre,
    nombre = nombre,
    descripcion = descripcion,
    estado = parseEstadoCurso(estado),
    codigoAcceso = codigoAcceso,
    codigoActivo = codigoActivo,
    totalEstudiantes = totalEstudiantes,
    totalActividades = totalActividades,
    nota05 = nota05,
    nota0100 = nota0100,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Curso.toEntity(ownerUserId: Int, ownerRol: RolUsuario): CursoEntity = CursoEntity(
    id = id,
    ownerUserId = ownerUserId,
    ownerRol = ownerRol.name,
    instructorId = instructorId,
    instructorNombre = instructorNombre,
    nombre = nombre,
    descripcion = descripcion,
    estado = estado.name,
    codigoAcceso = codigoAcceso,
    codigoActivo = codigoActivo,
    totalEstudiantes = totalEstudiantes,
    totalActividades = totalActividades,
    nota05 = nota05,
    nota0100 = nota0100,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun CursoEntity.toDomain(): Curso = Curso(
    id = id,
    instructorId = instructorId,
    instructorNombre = instructorNombre,
    nombre = nombre,
    descripcion = descripcion,
    estado = parseEstadoCurso(estado),
    codigoAcceso = codigoAcceso,
    codigoActivo = codigoActivo,
    totalEstudiantes = totalEstudiantes,
    totalActividades = totalActividades,
    nota05 = nota05,
    nota0100 = nota0100,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ArchivoAdjuntoDto.toDomain(): ArchivoAdjunto = ArchivoAdjunto(
    id = id,
    nombreOriginal = nombreOriginal,
    mimeType = mimeType,
    tamanoBytes = tamanoBytes,
    urlDescarga = urlDescarga,
)

fun ArchivoAdjunto.toDto(): ArchivoAdjuntoDto = ArchivoAdjuntoDto(
    id = id,
    nombreOriginal = nombreOriginal,
    mimeType = mimeType,
    tamanoBytes = tamanoBytes,
    urlDescarga = urlDescarga,
)

fun EntregaDetalleDto.toDomain(): EntregaDetalle = EntregaDetalle(
    id = id,
    actividadId = actividadId,
    aprendizId = aprendizId,
    descripcion = descripcion,
    nota = nota,
    retroalimentacion = retroalimentacion,
    exenta = exenta,
    estado = parseEstadoEntrega(estado),
    fechaEntrega = fechaEntrega,
    fechaCalificacion = fechaCalificacion,
    archivos = archivos.map { it.toDomain() },
)

fun EntregaDetalle.toDto(): EntregaDetalleDto = EntregaDetalleDto(
    id = id,
    actividadId = actividadId,
    aprendizId = aprendizId,
    descripcion = descripcion,
    nota = nota,
    retroalimentacion = retroalimentacion,
    exenta = exenta,
    estado = estado.name,
    fechaEntrega = fechaEntrega,
    fechaCalificacion = fechaCalificacion,
    archivos = archivos.map { it.toDto() },
)

fun ActividadDto.toDomain(): Actividad = Actividad(
    id = id,
    cursoId = cursoId,
    titulo = titulo,
    descripcion = descripcion,
    fechaLimite = fechaLimite,
    puntajeMaximo = puntajeMaximo,
    adjuntos = adjuntos.map { it.toDomain() },
    totalEntregas = totalEntregas,
    porCalificar = porCalificar,
    estadoAprendiz = estadoAprendiz?.let(::parseEstadoEntrega),
    miEntrega = miEntrega?.toDomain(),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Actividad.toEntity(): ActividadCursoEntity = ActividadCursoEntity(
    id = id,
    cursoId = cursoId,
    titulo = titulo,
    descripcion = descripcion,
    fechaLimite = fechaLimite,
    puntajeMaximo = puntajeMaximo,
    adjuntosJson = roomJson.encodeToString(adjuntos.map { it.toDto() }),
    totalEntregas = totalEntregas,
    porCalificar = porCalificar,
    estadoAprendiz = estadoAprendiz?.name,
    miEntregaJson = miEntrega?.let { roomJson.encodeToString(it.toDto()) },
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ActividadCursoEntity.toDomain(): Actividad {
    val adj = runCatching {
        roomJson.decodeFromString<List<ArchivoAdjuntoDto>>(adjuntosJson).map { it.toDomain() }
    }.getOrDefault(emptyList())
    val entrega = miEntregaJson?.let { raw ->
        runCatching { roomJson.decodeFromString<EntregaDetalleDto>(raw).toDomain() }.getOrNull()
    }
    return Actividad(
        id = id,
        cursoId = cursoId,
        titulo = titulo,
        descripcion = descripcion,
        fechaLimite = fechaLimite,
        puntajeMaximo = puntajeMaximo,
        adjuntos = adj,
        totalEntregas = totalEntregas,
        porCalificar = porCalificar,
        estadoAprendiz = estadoAprendiz?.let(::parseEstadoEntrega),
        miEntrega = entrega,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun EntregaInstructorItemDto.toDomain(): EntregaInstructorItem = EntregaInstructorItem(
    aprendizId = aprendizId,
    aprendizNombre = aprendizNombre,
    aprendizEmail = aprendizEmail,
    entregaId = entregaId,
    descripcion = descripcion,
    nota = nota,
    retroalimentacion = retroalimentacion,
    exenta = exenta,
    estado = parseEstadoEntrega(estado),
    fechaEntrega = fechaEntrega,
    fechaCalificacion = fechaCalificacion,
    archivos = archivos.map { it.toDomain() },
)

fun ActividadEntregasGrupoDto.toDomain(): ActividadEntregasGrupo = ActividadEntregasGrupo(
    actividad = actividad.toDomain(),
    entregas = entregas.map { it.toDomain() },
)

fun MaterialApoyoDto.toDomain(): MaterialApoyo = MaterialApoyo(
    id = id,
    cursoId = cursoId,
    titulo = titulo,
    descripcion = descripcion,
    tipo = if (tipo.equals("ENLACE", ignoreCase = true)) TipoMaterial.ENLACE else TipoMaterial.ARCHIVO,
    enlaceUrl = enlaceUrl,
    nombreOriginal = nombreOriginal,
    mimeType = mimeType,
    tamanoBytes = tamanoBytes,
    urlDescarga = urlDescarga,
    createdAt = createdAt,
)

fun MaterialApoyo.toEntity(): MaterialEntity = MaterialEntity(
    id = id,
    cursoId = cursoId,
    titulo = titulo,
    descripcion = descripcion,
    tipo = tipo.name,
    enlaceUrl = enlaceUrl,
    nombreOriginal = nombreOriginal,
    mimeType = mimeType,
    tamanoBytes = tamanoBytes,
    urlDescarga = urlDescarga,
    createdAt = createdAt,
)

fun MaterialEntity.toDomain(): MaterialApoyo = MaterialApoyo(
    id = id,
    cursoId = cursoId,
    titulo = titulo,
    descripcion = descripcion,
    tipo = if (tipo.equals("ENLACE", ignoreCase = true)) TipoMaterial.ENLACE else TipoMaterial.ARCHIVO,
    enlaceUrl = enlaceUrl,
    nombreOriginal = nombreOriginal,
    mimeType = mimeType,
    tamanoBytes = tamanoBytes,
    urlDescarga = urlDescarga,
    createdAt = createdAt,
)

fun PersonaCursoDto.toDomain(): PersonaCurso = PersonaCurso(
    id = id,
    nombre = nombre,
    email = email,
    rol = parseRol(rol),
    fechaUnion = fechaUnion,
    nota05 = nota05,
    nota0100 = nota0100,
)

fun PersonaCurso.toEntity(cursoId: Int): PersonaCursoEntity = PersonaCursoEntity(
    id = id,
    cursoId = cursoId,
    nombre = nombre,
    email = email,
    rol = rol.name,
    fechaUnion = fechaUnion,
    nota05 = nota05,
    nota0100 = nota0100,
)

fun PersonaCursoEntity.toDomain(): PersonaCurso = PersonaCurso(
    id = id,
    nombre = nombre,
    email = email,
    rol = parseRol(rol),
    fechaUnion = fechaUnion,
    nota05 = nota05,
    nota0100 = nota0100,
)

fun PersonasCursoResponseDto.toDomain(): PersonasCurso = PersonasCurso(
    instructor = instructor.toDomain(),
    aprendices = aprendices.map { it.toDomain() },
)

fun PersonaSimpleDto.toDomain(): Usuario = Usuario(
    id = id,
    nombre = nombre,
    email = email,
    rol = RolUsuario.APRENDIZ,
)

fun DetalleCalificacionActividadDto.toDomain(): DetalleCalificacionActividad = DetalleCalificacionActividad(
    actividadId = actividadId,
    titulo = titulo,
    puntajeMaximo = puntajeMaximo,
    nota = nota,
    notaEscala05 = notaEscala05,
    estado = parseEstadoEntrega(estado),
    exenta = exenta,
    retroalimentacion = retroalimentacion,
)

fun ResumenCalificacionEstudianteDto.toDomain(): ResumenCalificacionEstudiante = ResumenCalificacionEstudiante(
    aprendiz = aprendiz?.toDomain(),
    nota05 = nota05,
    nota0100 = nota0100,
    totalActividades = totalActividades,
    actividadesEvaluables = actividadesEvaluables,
    actividadesCalificadas = actividadesCalificadas,
    actividadesExentas = actividadesExentas,
    detalle = detalle.map { it.toDomain() },
)

fun HiloForoDto.toDomain(): HiloForo = HiloForo(
    id = id,
    cursoId = cursoId,
    autorId = autorId,
    autorNombre = autorNombre,
    autorRol = parseRol(autorRol),
    aprendizObjetivoId = aprendizObjetivoId,
    aprendizObjetivoNombre = aprendizObjetivoNombre,
    tipo = if (tipo.equals("PRIVADO", ignoreCase = true)) TipoHiloForo.PRIVADO else TipoHiloForo.PUBLICO,
    titulo = titulo,
    contenido = contenido,
    createdAt = createdAt,
    totalMensajes = totalMensajes,
)

fun MensajeForoDto.toDomain(): MensajeForo = MensajeForo(
    id = id,
    hiloId = hiloId,
    autorId = autorId,
    autorNombre = autorNombre,
    autorRol = parseRol(autorRol),
    contenido = contenido,
    createdAt = createdAt,
)

fun HiloForoDetalleResponseDto.toDomain(): HiloForoDetalle = HiloForoDetalle(
    hilo = hilo.toDomain(),
    mensajes = mensajes.map { it.toDomain() },
)
