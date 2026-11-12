package com.example.labo_android_semana_02

import com.example.labo_android_semana_02.domain.Categoria
import com.example.labo_android_semana_02.domain.Reporte
import com.example.labo_android_semana_02.domain.repository.CategoriaRepository
import com.example.labo_android_semana_02.domain.repository.PreferenciasRepository
import com.example.labo_android_semana_02.domain.repository.ReporteRepository
import com.example.labo_android_semana_02.ui.ListadoUiState
import com.example.labo_android_semana_02.ui.OperacionUiState
import com.example.labo_android_semana_02.ui.ReporteViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReporteViewModelTest {

    private class FakeReporteRepository : ReporteRepository {
        val reportesFlow = MutableStateFlow<List<Reporte>>(emptyList())
        var refreshResult: Result<Unit> = Result.success(Unit)

        override fun getReportes(): Flow<List<Reporte>> = reportesFlow
        override fun getReporteById(id: Int): Flow<Reporte?> =
            reportesFlow.map { list -> list.find { it.id == id } }

        override suspend fun refreshReportes(): Result<Unit> = refreshResult

        override suspend fun insertReporte(reporte: Reporte) {
            reportesFlow.value = reportesFlow.value.filter { it.id != reporte.id } + reporte
        }

        override suspend fun updateReporte(reporte: Reporte) {
            reportesFlow.value = reportesFlow.value.map { if (it.id == reporte.id) reporte else it }
        }

        override suspend fun deleteReporte(reporte: Reporte) {
            reportesFlow.value = reportesFlow.value.filter { it.id != reporte.id }
        }
    }

    private class FakeCategoriaRepository : CategoriaRepository {
        val flow = MutableStateFlow(
            listOf(
                Categoria(1, "General", "📋"),
                Categoria(2, "Infraestructura", "🏗️")
            )
        )
        override fun getCategorias(): Flow<List<Categoria>> = flow
        override suspend fun refreshCategorias(): Result<Unit> = Result.success(Unit)
    }

    private class FakePreferenciasRepository : PreferenciasRepository {
        val ordenFlow = MutableStateFlow("FECHA_DESC")
        override val ordenFiltro: Flow<String> = ordenFlow
        override suspend fun guardarOrdenFiltro(orden: String) {
            ordenFlow.value = orden
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `filtrar por categoria y busqueda emite solo los reportes coincidentes`() = runTest {
        val repo = FakeReporteRepository()
        val vm = ReporteViewModel(repo, FakePreferenciasRepository(), FakeCategoriaRepository())

        repo.reportesFlow.value = listOf(
            Reporte(1, "Proyector", "Sala 204", 1000L, categoriaId = 2),
            Reporte(2, "Acta", "Reunión", 2000L, categoriaId = 1),
            Reporte(3, "Luminaria", "Sala 204", 3000L, categoriaId = 2)
        )

        vm.onCategoriaSeleccionada(2)
        vm.onBusquedaChange("Proyector")

        val state = vm.uiState.first { it is ListadoUiState.Contenido } as ListadoUiState.Contenido
        assertEquals(1, state.reportes.size)
        assertEquals(1, state.reportes.first().id)
    }

    @Test
    fun `guardarReporte y sincronizarConServidor actualizan estados`() = runTest {
        val repo = FakeReporteRepository()
        repo.refreshResult = Result.failure(Exception("Sin conexión"))
        val vm = ReporteViewModel(repo, FakePreferenciasRepository(), FakeCategoriaRepository())

        assertEquals("Sin conexión", vm.syncEstado.value)

        repo.refreshResult = Result.success(Unit)
        vm.sincronizarConServidor()
        assertNull(vm.syncEstado.value)

        vm.guardarReporte(Reporte(10, "Nuevo reporte", "Detalle", 5000L, categoriaId = 1))
        assertTrue(vm.operacionState.value is OperacionUiState.Exito)
    }
}
