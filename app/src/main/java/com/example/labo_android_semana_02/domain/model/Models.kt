package com.example.labo_android_semana_02.domain.model

enum class RolUsuario {
    INSTRUCTOR,
    APRENDIZ,
}

enum class EstadoCurso {
    ACTIVO,
    ARCHIVADO,
}

enum class EstadoEntrega {
    PENDIENTE,
    ENTREGADA,
    TARDIA,
    CALIFICADA,
    EXENTA,
}

enum class TipoMaterial {
    ARCHIVO,
    ENLACE,
}

enum class TipoHiloForo {
    PUBLICO,
    PRIVADO,
}

data class Usuario(
    val id: Int,
    val nombre: String,
    val email: String,
    val rol: RolUsuario,
)

data class AuthSession(
    val token: String,
    val usuario: Usuario,
)

data class ArchivoAdjunto(
    val id: Int,
    val nombreOriginal: String,
    val mimeType: String,
    val tamanoBytes: Long,
    val urlDescarga: String,
)

data class ArchivoLocal(
    val uriString: String,
    val nombre: String,
    val mimeType: String,
    val tamanoBytes: Long,
)

data class Curso(
    val id: Int,
    val instructorId: Int,
    val instructorNombre: String,
    val nombre: String,
    val descripcion: String,
    val estado: EstadoCurso,
    val codigoAcceso: String?,
    val codigoActivo: Boolean,
    val totalEstudiantes: Int,
    val totalActividades: Int,
    val nota05: Double?,
    val nota0100: Double?,
    val createdAt: Long,
    val updatedAt: Long,
)

data class EntregaDetalle(
    val id: Int,
    val actividadId: Int,
    val aprendizId: Int,
    val descripcion: String,
    val nota: Double?,
    val retroalimentacion: String?,
    val exenta: Boolean,
    val estado: EstadoEntrega,
    val fechaEntrega: Long?,
    val fechaCalificacion: Long?,
    val archivos: List<ArchivoAdjunto>,
)

data class Actividad(
    val id: Int,
    val cursoId: Int,
    val titulo: String,
    val descripcion: String,
    val fechaLimite: Long,
    val puntajeMaximo: Double,
    val adjuntos: List<ArchivoAdjunto>,
    val totalEntregas: Int,
    val porCalificar: Int,
    val estadoAprendiz: EstadoEntrega?,
    val miEntrega: EntregaDetalle?,
    val createdAt: Long,
    val updatedAt: Long,
)

data class EntregaInstructorItem(
    val aprendizId: Int,
    val aprendizNombre: String,
    val aprendizEmail: String,
    val entregaId: Int?,
    val descripcion: String,
    val nota: Double?,
    val retroalimentacion: String?,
    val exenta: Boolean,
    val estado: EstadoEntrega,
    val fechaEntrega: Long?,
    val fechaCalificacion: Long?,
    val archivos: List<ArchivoAdjunto>,
)

data class ActividadEntregasGrupo(
    val actividad: Actividad,
    val entregas: List<EntregaInstructorItem>,
)

data class MaterialApoyo(
    val id: Int,
    val cursoId: Int,
    val titulo: String,
    val descripcion: String,
    val tipo: TipoMaterial,
    val enlaceUrl: String?,
    val nombreOriginal: String?,
    val mimeType: String?,
    val tamanoBytes: Long?,
    val urlDescarga: String,
    val createdAt: Long,
)

data class PersonaCurso(
    val id: Int,
    val nombre: String,
    val email: String,
    val rol: RolUsuario,
    val fechaUnion: Long?,
    val nota05: Double?,
    val nota0100: Double?,
)

data class PersonasCurso(
    val instructor: PersonaCurso,
    val aprendices: List<PersonaCurso>,
)

data class DetalleCalificacionActividad(
    val actividadId: Int,
    val titulo: String,
    val puntajeMaximo: Double,
    val nota: Double?,
    val notaEscala05: Double?,
    val estado: EstadoEntrega,
    val exenta: Boolean,
    val retroalimentacion: String?,
)

data class ResumenCalificacionEstudiante(
    val aprendiz: Usuario?,
    val nota05: Double,
    val nota0100: Double,
    val totalActividades: Int,
    val actividadesEvaluables: Int,
    val actividadesCalificadas: Int,
    val actividadesExentas: Int,
    val detalle: List<DetalleCalificacionActividad>,
)

data class HiloForo(
    val id: Int,
    val cursoId: Int,
    val autorId: Int,
    val autorNombre: String,
    val autorRol: RolUsuario,
    val aprendizObjetivoId: Int?,
    val aprendizObjetivoNombre: String?,
    val tipo: TipoHiloForo,
    val titulo: String,
    val contenido: String,
    val createdAt: Long,
    val totalMensajes: Int,
)

data class MensajeForo(
    val id: Int,
    val hiloId: Int,
    val autorId: Int,
    val autorNombre: String,
    val autorRol: RolUsuario,
    val contenido: String,
    val enlaceUrl: String? = null,
    val archivos: List<ArchivoAdjunto> = emptyList(),
    val createdAt: Long,
)

data class HiloForoDetalle(
    val hilo: HiloForo,
    val mensajes: List<MensajeForo>,
)
