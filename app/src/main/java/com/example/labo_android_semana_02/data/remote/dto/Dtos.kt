package com.example.labo_android_semana_02.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequestDto(
    val nombre: String,
    val email: String,
    val password: String,
    val rol: String,
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

@Serializable
data class UsuarioDto(
    val id: Int,
    val nombre: String,
    val email: String,
    val rol: String,
)

@Serializable
data class AuthResponseDto(
    val token: String,
    val usuario: UsuarioDto,
)

@Serializable
data class CursoDto(
    val id: Int,
    @SerialName("instructor_id") val instructorId: Int,
    @SerialName("instructor_nombre") val instructorNombre: String = "",
    val nombre: String,
    val descripcion: String = "",
    val estado: String = "ACTIVO",
    @SerialName("codigo_acceso") val codigoAcceso: String? = null,
    @SerialName("codigo_activo") val codigoActivo: Boolean = false,
    @SerialName("total_estudiantes") val totalEstudiantes: Int = 0,
    @SerialName("total_actividades") val totalActividades: Int = 0,
    @SerialName("nota_0_5") val nota05: Double? = null,
    @SerialName("nota_0_100") val nota0100: Double? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class CursoUpsertRequestDto(
    val nombre: String,
    val descripcion: String,
    val estado: String,
)

@Serializable
data class UnirseCursoRequestDto(
    @SerialName("codigo_acceso") val codigoAcceso: String,
)

@Serializable
data class UnirseCursoResponseDto(
    val mensaje: String = "",
    @SerialName("curso_id") val cursoId: Int,
)

@Serializable
data class CodigoEstadoRequestDto(
    @SerialName("codigo_activo") val codigoActivo: Boolean,
)

@Serializable
data class CodigoResponseDto(
    @SerialName("codigo_acceso") val codigoAcceso: String,
    @SerialName("codigo_activo") val codigoActivo: Boolean,
)

@Serializable
data class ArchivoAdjuntoDto(
    val id: Int,
    @SerialName("nombre_original") val nombreOriginal: String,
    @SerialName("mime_type") val mimeType: String = "application/octet-stream",
    @SerialName("tamano_bytes") val tamanoBytes: Long = 0L,
    @SerialName("url_descarga") val urlDescarga: String,
)

@Serializable
data class EntregaDetalleDto(
    val id: Int,
    @SerialName("actividad_id") val actividadId: Int,
    @SerialName("aprendiz_id") val aprendizId: Int,
    val descripcion: String = "",
    val nota: Double? = null,
    val retroalimentacion: String? = null,
    val exenta: Boolean = false,
    val estado: String = "ENTREGADA",
    @SerialName("fecha_entrega") val fechaEntrega: Long? = null,
    @SerialName("fecha_calificacion") val fechaCalificacion: Long? = null,
    val archivos: List<ArchivoAdjuntoDto> = emptyList(),
)

@Serializable
data class ActividadDto(
    val id: Int,
    @SerialName("curso_id") val cursoId: Int,
    val titulo: String,
    val descripcion: String = "",
    @SerialName("fecha_limite") val fechaLimite: Long,
    @SerialName("puntaje_maximo") val puntajeMaximo: Double = 5.0,
    val adjuntos: List<ArchivoAdjuntoDto> = emptyList(),
    @SerialName("total_entregas") val totalEntregas: Int = 0,
    @SerialName("por_calificar") val porCalificar: Int = 0,
    @SerialName("estado_aprendiz") val estadoAprendiz: String? = null,
    @SerialName("mi_entrega") val miEntrega: EntregaDetalleDto? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class EntregaInstructorItemDto(
    @SerialName("aprendiz_id") val aprendizId: Int,
    @SerialName("aprendiz_nombre") val aprendizNombre: String,
    @SerialName("aprendiz_email") val aprendizEmail: String,
    @SerialName("entrega_id") val entregaId: Int? = null,
    val descripcion: String = "",
    val nota: Double? = null,
    val retroalimentacion: String? = null,
    val exenta: Boolean = false,
    val estado: String = "PENDIENTE",
    @SerialName("fecha_entrega") val fechaEntrega: Long? = null,
    @SerialName("fecha_calificacion") val fechaCalificacion: Long? = null,
    val archivos: List<ArchivoAdjuntoDto> = emptyList(),
)

@Serializable
data class ActividadEntregasGrupoDto(
    val actividad: ActividadDto,
    val entregas: List<EntregaInstructorItemDto> = emptyList(),
)

@Serializable
data class CalificarRequestDto(
    val nota: Double? = null,
    val retroalimentacion: String? = null,
    val exenta: Boolean = false,
)

@Serializable
data class CalificarResponseDto(
    @SerialName("actividad_id") val actividadId: Int,
    @SerialName("aprendiz_id") val aprendizId: Int,
    val nota: Double? = null,
    val retroalimentacion: String? = null,
    val exenta: Boolean = false,
    val estado: String,
)

@Serializable
data class MaterialApoyoDto(
    val id: Int,
    @SerialName("curso_id") val cursoId: Int,
    val titulo: String,
    val descripcion: String = "",
    val tipo: String = "ARCHIVO",
    @SerialName("enlace_url") val enlaceUrl: String? = null,
    @SerialName("nombre_original") val nombreOriginal: String? = null,
    @SerialName("mime_type") val mimeType: String? = null,
    @SerialName("tamano_bytes") val tamanoBytes: Long? = null,
    @SerialName("url_descarga") val urlDescarga: String = "",
    @SerialName("created_at") val createdAt: Long = 0L,
)

@Serializable
data class PersonaCursoDto(
    val id: Int,
    val nombre: String,
    val email: String,
    val rol: String,
    @SerialName("fecha_union") val fechaUnion: Long? = null,
    @SerialName("nota_0_5") val nota05: Double? = null,
    @SerialName("nota_0_100") val nota0100: Double? = null,
)

@Serializable
data class PersonasCursoResponseDto(
    val instructor: PersonaCursoDto,
    val aprendices: List<PersonaCursoDto> = emptyList(),
)

@Serializable
data class PersonaSimpleDto(
    val id: Int,
    val nombre: String,
    val email: String,
)

@Serializable
data class DetalleCalificacionActividadDto(
    @SerialName("actividad_id") val actividadId: Int,
    val titulo: String,
    @SerialName("puntaje_maximo") val puntajeMaximo: Double,
    val nota: Double? = null,
    @SerialName("nota_escala_0_5") val notaEscala05: Double? = null,
    val estado: String,
    val exenta: Boolean = false,
    val retroalimentacion: String? = null,
)

@Serializable
data class ResumenCalificacionEstudianteDto(
    val aprendiz: PersonaSimpleDto? = null,
    @SerialName("nota_0_5") val nota05: Double = 0.0,
    @SerialName("nota_0_100") val nota0100: Double = 0.0,
    @SerialName("total_actividades") val totalActividades: Int = 0,
    @SerialName("actividades_evaluables") val actividadesEvaluables: Int = 0,
    @SerialName("actividades_calificadas") val actividadesCalificadas: Int = 0,
    @SerialName("actividades_exentas") val actividadesExentas: Int = 0,
    val detalle: List<DetalleCalificacionActividadDto> = emptyList(),
)

@Serializable
data class HiloForoDto(
    val id: Int,
    @SerialName("curso_id") val cursoId: Int,
    @SerialName("autor_id") val autorId: Int,
    @SerialName("autor_nombre") val autorNombre: String = "",
    @SerialName("autor_rol") val autorRol: String = "APRENDIZ",
    @SerialName("aprendiz_objetivo_id") val aprendizObjetivoId: Int? = null,
    @SerialName("aprendiz_objetivo_nombre") val aprendizObjetivoNombre: String? = null,
    val tipo: String = "PUBLICO",
    val titulo: String,
    val contenido: String,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("total_mensajes") val totalMensajes: Int = 0,
)

@Serializable
data class CrearHiloRequestDto(
    val tipo: String,
    val titulo: String,
    val contenido: String,
    @SerialName("aprendiz_objetivo_id") val aprendizObjetivoId: Int? = null,
)

@Serializable
data class MensajeForoDto(
    val id: Int,
    @SerialName("hilo_id") val hiloId: Int,
    @SerialName("autor_id") val autorId: Int,
    @SerialName("autor_nombre") val autorNombre: String = "",
    @SerialName("autor_rol") val autorRol: String = "APRENDIZ",
    val contenido: String = "",
    @SerialName("enlace_url") val enlaceUrl: String? = null,
    val archivos: List<ArchivoAdjuntoDto> = emptyList(),
    @SerialName("created_at") val createdAt: Long = 0L,
)

@Serializable
data class HiloForoDetalleResponseDto(
    val hilo: HiloForoDto,
    val mensajes: List<MensajeForoDto> = emptyList(),
)

@Serializable
data class ResponderHiloRequestDto(
    val contenido: String,
)
