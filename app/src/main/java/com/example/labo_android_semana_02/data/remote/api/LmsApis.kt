package com.example.labo_android_semana_02.data.remote.api

import com.example.labo_android_semana_02.data.remote.dto.ActividadDto
import com.example.labo_android_semana_02.data.remote.dto.ActividadEntregasGrupoDto
import com.example.labo_android_semana_02.data.remote.dto.AuthResponseDto
import com.example.labo_android_semana_02.data.remote.dto.CalificarRequestDto
import com.example.labo_android_semana_02.data.remote.dto.CalificarResponseDto
import com.example.labo_android_semana_02.data.remote.dto.CodigoEstadoRequestDto
import com.example.labo_android_semana_02.data.remote.dto.CodigoResponseDto
import com.example.labo_android_semana_02.data.remote.dto.CrearHiloRequestDto
import com.example.labo_android_semana_02.data.remote.dto.CursoDto
import com.example.labo_android_semana_02.data.remote.dto.CursoUpsertRequestDto
import com.example.labo_android_semana_02.data.remote.dto.EntregaDetalleDto
import com.example.labo_android_semana_02.data.remote.dto.HiloForoDetalleResponseDto
import com.example.labo_android_semana_02.data.remote.dto.HiloForoDto
import com.example.labo_android_semana_02.data.remote.dto.LoginRequestDto
import com.example.labo_android_semana_02.data.remote.dto.MaterialApoyoDto
import com.example.labo_android_semana_02.data.remote.dto.MensajeForoDto
import com.example.labo_android_semana_02.data.remote.dto.PersonasCursoResponseDto
import com.example.labo_android_semana_02.data.remote.dto.RegisterRequestDto
import com.example.labo_android_semana_02.data.remote.dto.ResponderHiloRequestDto
import com.example.labo_android_semana_02.data.remote.dto.ResumenCalificacionEstudianteDto
import com.example.labo_android_semana_02.data.remote.dto.UnirseCursoRequestDto
import com.example.labo_android_semana_02.data.remote.dto.UnirseCursoResponseDto
import com.example.labo_android_semana_02.data.remote.dto.UsuarioDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): AuthResponseDto

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): AuthResponseDto

    @GET("api/auth/me")
    suspend fun me(): UsuarioDto
}

interface CursoApi {
    @GET("api/cursos")
    suspend fun listar(): List<CursoDto>

    @POST("api/cursos")
    suspend fun crear(@Body request: CursoUpsertRequestDto): CursoDto

    @GET("api/cursos/{cursoId}")
    suspend fun obtener(@Path("cursoId") cursoId: Int): CursoDto

    @PUT("api/cursos/{cursoId}")
    suspend fun actualizar(
        @Path("cursoId") cursoId: Int,
        @Body request: CursoUpsertRequestDto,
    ): CursoDto

    @DELETE("api/cursos/{cursoId}")
    suspend fun eliminar(@Path("cursoId") cursoId: Int): Response<Unit>

    @POST("api/cursos/unirse")
    suspend fun unirse(@Body request: UnirseCursoRequestDto): UnirseCursoResponseDto

    @DELETE("api/cursos/{cursoId}/salirse")
    suspend fun salirse(@Path("cursoId") cursoId: Int): Response<Unit>

    @POST("api/cursos/{cursoId}/codigo/regenerar")
    suspend fun regenerarCodigo(@Path("cursoId") cursoId: Int): CodigoResponseDto

    @PATCH("api/cursos/{cursoId}/codigo/estado")
    suspend fun cambiarEstadoCodigo(
        @Path("cursoId") cursoId: Int,
        @Body request: CodigoEstadoRequestDto,
    ): CodigoResponseDto

    @GET("api/cursos/{cursoId}/personas")
    suspend fun listarPersonas(@Path("cursoId") cursoId: Int): PersonasCursoResponseDto

    @DELETE("api/cursos/{cursoId}/personas/{aprendizId}")
    suspend fun expulsarEstudiante(
        @Path("cursoId") cursoId: Int,
        @Path("aprendizId") aprendizId: Int,
    ): Response<Unit>

    @GET("api/cursos/{cursoId}/calificaciones")
    suspend fun obtenerMiCalificacion(@Path("cursoId") cursoId: Int): ResumenCalificacionEstudianteDto

    @GET("api/cursos/{cursoId}/calificaciones")
    suspend fun obtenerCalificacionesCurso(@Path("cursoId") cursoId: Int): List<ResumenCalificacionEstudianteDto>
}

interface ActividadApi {
    @GET("api/actividades/curso/{cursoId}")
    suspend fun listarPorCurso(@Path("cursoId") cursoId: Int): List<ActividadDto>

    @Multipart
    @POST("api/actividades/curso/{cursoId}")
    suspend fun crear(
        @Path("cursoId") cursoId: Int,
        @Part("titulo") titulo: RequestBody,
        @Part("descripcion") descripcion: RequestBody,
        @Part("fecha_limite") fechaLimite: RequestBody,
        @Part("puntaje_maximo") puntajeMaximo: RequestBody,
        @Part adjuntos: List<MultipartBody.Part>,
    ): ActividadDto

    @Multipart
    @PUT("api/actividades/{actividadId}")
    suspend fun actualizar(
        @Path("actividadId") actividadId: Int,
        @Part("titulo") titulo: RequestBody,
        @Part("descripcion") descripcion: RequestBody,
        @Part("fecha_limite") fechaLimite: RequestBody,
        @Part("puntaje_maximo") puntajeMaximo: RequestBody,
        @Part adjuntos: List<MultipartBody.Part>,
    ): ActividadDto

    @DELETE("api/actividades/{actividadId}")
    suspend fun eliminar(@Path("actividadId") actividadId: Int): Response<Unit>

    @GET("api/actividades/{actividadId}/entregas")
    suspend fun listarEntregas(@Path("actividadId") actividadId: Int): ActividadEntregasGrupoDto

    @Multipart
    @POST("api/actividades/{actividadId}/entrega")
    suspend fun enviarEntrega(
        @Path("actividadId") actividadId: Int,
        @Part("descripcion") descripcion: RequestBody,
        @Part("conservar_archivos") conservarArchivos: RequestBody,
        @Part archivos: List<MultipartBody.Part>,
    ): EntregaDetalleDto

    @PUT("api/actividades/{actividadId}/entregas/{aprendizId}/calificar")
    suspend fun calificar(
        @Path("actividadId") actividadId: Int,
        @Path("aprendizId") aprendizId: Int,
        @Body request: CalificarRequestDto,
    ): CalificarResponseDto
}

interface MaterialApi {
    @GET("api/materiales/curso/{cursoId}")
    suspend fun listarPorCurso(@Path("cursoId") cursoId: Int): List<MaterialApoyoDto>

    @POST("api/materiales/curso/{cursoId}")
    suspend fun crearEnlace(
        @Path("cursoId") cursoId: Int,
        @Body body: Map<String, String>,
    ): MaterialApoyoDto

    @Multipart
    @POST("api/materiales/curso/{cursoId}")
    suspend fun crearArchivo(
        @Path("cursoId") cursoId: Int,
        @Part("titulo") titulo: RequestBody,
        @Part("descripcion") descripcion: RequestBody,
        @Part("tipo") tipo: RequestBody,
        @Part archivo: MultipartBody.Part,
    ): MaterialApoyoDto

    @DELETE("api/materiales/{materialId}")
    suspend fun eliminar(@Path("materialId") materialId: Int): Response<Unit>
}

interface ForoApi {
    @GET("api/foro/curso/{cursoId}")
    suspend fun listarHilos(
        @Path("cursoId") cursoId: Int,
        @Query("tipo") tipo: String,
        @Query("aprendizId") aprendizId: Int? = null,
    ): List<HiloForoDto>

    @POST("api/foro/curso/{cursoId}")
    suspend fun crearHilo(
        @Path("cursoId") cursoId: Int,
        @Body request: CrearHiloRequestDto,
    ): HiloForoDto

    @GET("api/foro/{hiloId}/mensajes")
    suspend fun obtenerDetalle(@Path("hiloId") hiloId: Int): HiloForoDetalleResponseDto

    @POST("api/foro/{hiloId}/mensajes")
    suspend fun responder(
        @Path("hiloId") hiloId: Int,
        @Body request: ResponderHiloRequestDto,
    ): MensajeForoDto
}
