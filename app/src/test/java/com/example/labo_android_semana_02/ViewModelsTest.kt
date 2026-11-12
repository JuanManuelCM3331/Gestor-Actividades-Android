package com.example.labo_android_semana_02

import com.example.labo_android_semana_02.domain.model.AuthSession
import com.example.labo_android_semana_02.domain.model.Curso
import com.example.labo_android_semana_02.domain.model.EstadoCurso
import com.example.labo_android_semana_02.domain.model.PersonaCurso
import com.example.labo_android_semana_02.domain.model.PersonasCurso
import com.example.labo_android_semana_02.domain.model.ResumenCalificacionEstudiante
import com.example.labo_android_semana_02.domain.model.RolUsuario
import com.example.labo_android_semana_02.domain.model.Usuario
import com.example.labo_android_semana_02.domain.repository.AuthRepository
import com.example.labo_android_semana_02.domain.repository.CursoRepository
import com.example.labo_android_semana_02.ui.viewmodel.AuthSessionState
import com.example.labo_android_semana_02.ui.viewmodel.AuthViewModel
import com.example.labo_android_semana_02.ui.viewmodel.CursosViewModel
import com.example.labo_android_semana_02.ui.viewmodel.FiltroEstadoCurso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeAuthRepository : AuthRepository {
        val state = MutableStateFlow<AuthSession?>(null)
        override val sessionFlow: Flow<AuthSession?> = state

        override suspend fun login(email: String, password: String): Result<AuthSession> {
            val session = AuthSession(
                token = "jwt_fake",
                usuario = Usuario(1, "Instructora Demo", email, RolUsuario.INSTRUCTOR),
            )
            state.value = session
            return Result.success(session)
        }

        override suspend fun register(
            nombre: String,
            email: String,
            password: String,
            rol: RolUsuario,
        ): Result<AuthSession> {
            val session = AuthSession(
                token = "jwt_fake_reg",
                usuario = Usuario(2, nombre, email, rol),
            )
            state.value = session
            return Result.success(session)
        }

        override suspend fun refreshMe(): Result<Usuario> =
            state.value?.usuario?.let { Result.success(it) } ?: Result.failure(IllegalStateException("Sin sesión"))

        override suspend fun logout() {
            state.value = null
        }
    }

    private class FakeCursoRepository : CursoRepository {
        private val cursosFlow = MutableStateFlow(
            listOf(
                Curso(
                    id = 10, instructorId = 1, instructorNombre = "Laura",
                    nombre = "Bases de Datos MySQL", descripcion = "Modelado relacional",
                    estado = EstadoCurso.ACTIVO, codigoAcceso = "SQL123", codigoActivo = true,
                    totalEstudiantes = 12, totalActividades = 3, nota05 = null, nota0100 = null,
                    createdAt = 1L, updatedAt = 2L,
                ),
                Curso(
                    id = 11, instructorId = 1, instructorNombre = "Laura",
                    nombre = "Legado Archivado", descripcion = "Curso anterior",
                    estado = EstadoCurso.ARCHIVADO, codigoAcceso = "OLD999", codigoActivo = false,
                    totalEstudiantes = 5, totalActividades = 2, nota05 = null, nota0100 = null,
                    createdAt = 1L, updatedAt = 1L,
                ),
            )
        )

        override fun observarCursos(usuarioId: Int, rol: RolUsuario): Flow<List<Curso>> = cursosFlow
        override suspend fun sincronizarCursos(usuarioId: Int, rol: RolUsuario): Result<List<Curso>> =
            Result.success(cursosFlow.value)

        override suspend fun obtenerCurso(cursoId: Int): Result<Curso> =
            Result.success(cursosFlow.value.first { it.id == cursoId })

        override suspend fun crearCurso(nombre: String, descripcion: String, estado: EstadoCurso): Result<Curso> {
            val nuevo = Curso(
                id = 12, instructorId = 1, instructorNombre = "Laura", nombre = nombre, descripcion = descripcion,
                estado = estado, codigoAcceso = "NEW777", codigoActivo = true, totalEstudiantes = 0,
                totalActividades = 0, nota05 = null, nota0100 = null, createdAt = 3L, updatedAt = 3L,
            )
            cursosFlow.value = listOf(nuevo) + cursosFlow.value
            return Result.success(nuevo)
        }

        override suspend fun actualizarCurso(cursoId: Int, nombre: String, descripcion: String, estado: EstadoCurso): Result<Curso> =
            Result.failure(UnsupportedOperationException())

        override suspend fun eliminarCurso(cursoId: Int): Result<Unit> = Result.success(Unit)
        override suspend fun unirseACurso(codigoAcceso: String): Result<Int> = Result.success(10)
        override suspend fun salirseDeCurso(cursoId: Int): Result<Unit> = Result.success(Unit)
        override suspend fun regenerarCodigo(cursoId: Int): Result<Pair<String, Boolean>> = Result.success("ZZZ111" to true)
        override suspend fun cambiarEstadoCodigo(cursoId: Int, activo: Boolean): Result<Pair<String, Boolean>> =
            Result.success("SQL123" to activo)

        override fun observarPersonas(cursoId: Int): Flow<List<PersonaCurso>> = MutableStateFlow(emptyList())
        override suspend fun obtenerPersonas(cursoId: Int): Result<PersonasCurso> = Result.failure(UnsupportedOperationException())
        override suspend fun expulsarEstudiante(cursoId: Int, aprendizId: Int): Result<Unit> = Result.success(Unit)
        override suspend fun obtenerMiCalificacion(cursoId: Int): Result<ResumenCalificacionEstudiante> =
            Result.failure(UnsupportedOperationException())

        override suspend fun obtenerCalificacionesCurso(cursoId: Int): Result<List<ResumenCalificacionEstudiante>> =
            Result.success(emptyList())
    }

    @Test
    fun `AuthViewModel valida formulario y emite estado autenticado al iniciar sesion`() = runTest {
        val authRepo = FakeAuthRepository()
        val vm = AuthViewModel(authRepo)

        // Email inválido produce error sin llamar API
        vm.onEmailChange("correo-malo")
        vm.onPasswordChange("123456")
        vm.enviarFormulario()
        assertNotNull(vm.uiState.value.error)

        // Login válido actualiza sessionState a Authenticated
        vm.onEmailChange("instructor@ctma.edu.co")
        vm.onPasswordChange("Instructor123!")
        vm.enviarFormulario()
        advanceUntilIdle()

        val state = vm.sessionState.first { it !is AuthSessionState.Loading }
        assertTrue(state is AuthSessionState.Authenticated)
        assertEquals(RolUsuario.INSTRUCTOR, (state as AuthSessionState.Authenticated).session.usuario.rol)
    }

    @Test
    fun `CursosViewModel filtra busca y crea curso manteniendo StateFlow actualizado`() = runTest {
        val authRepo = FakeAuthRepository()
        authRepo.login("instructor@ctma.edu.co", "Instructor123!")
        val cursoRepo = FakeCursoRepository()
        val vm = CursosViewModel(authRepo, cursoRepo)
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.cursosFiltrados.size)

        vm.cambiarFiltro(FiltroEstadoCurso.ACTIVO)
        assertEquals(1, vm.uiState.value.cursosFiltrados.size)
        assertEquals("Bases de Datos MySQL", vm.uiState.value.cursosFiltrados.first().nombre)

        vm.cambiarFiltro(FiltroEstadoCurso.TODOS)
        vm.cambiarBusqueda("Legado")
        assertEquals(1, vm.uiState.value.cursosFiltrados.size)

        vm.cambiarBusqueda("")
        vm.crearCurso("Kotlin Compose", "Curso móvil nativo", EstadoCurso.ACTIVO)
        advanceUntilIdle()
        assertEquals(3, vm.uiState.value.todosLosCursos.size)
    }
}
