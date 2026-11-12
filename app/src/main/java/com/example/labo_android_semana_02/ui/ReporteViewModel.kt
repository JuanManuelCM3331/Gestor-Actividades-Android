package com.example.labo_android_semana_02.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.labo_android_semana_02.domain.Categoria
import com.example.labo_android_semana_02.domain.Reporte
import com.example.labo_android_semana_02.domain.repository.CategoriaRepository
import com.example.labo_android_semana_02.domain.repository.PreferenciasRepository
import com.example.labo_android_semana_02.domain.repository.ReporteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReporteViewModel private constructor(
    private val repository: ReporteRepository,
    private val preferencias: PreferenciasRepository,
    private val categoriaRepository: CategoriaRepository?,
    autoSync: Boolean
) : ViewModel() {

    @Inject
    constructor(
        repository: ReporteRepository,
        preferencias: PreferenciasRepository,
        categoriaRepository: CategoriaRepository
    ) : this(repository, preferencias, categoriaRepository, autoSync = true)

    constructor(
        repository: ReporteRepository,
        preferencias: PreferenciasRepository
    ) : this(repository, preferencias, null, autoSync = false)

    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()

    private val _categoriaSeleccionadaId = MutableStateFlow<Int?>(null)
    val categoriaSeleccionadaId: StateFlow<Int?> = _categoriaSeleccionadaId.asStateFlow()

    private val _operacionState = MutableStateFlow<OperacionUiState>(OperacionUiState.Ideal)
    val operacionState: StateFlow<OperacionUiState> = _operacionState.asStateFlow()

    private val _syncEstado = MutableStateFlow<String?>(null)
    val syncEstado: StateFlow<String?> = _syncEstado.asStateFlow()

    private val _sincronizando = MutableStateFlow(false)
    val sincronizando: StateFlow<Boolean> = _sincronizando.asStateFlow()

    /**
     * HU-21: conjunto de IDs marcados como favoritos. Se mantiene en el ViewModel
     * (sobrevive a recomposiciones y rotaciones) y se proyecta sobre el listado
     * antes de emitir el UiState.
     */
    private val _favoritos = MutableStateFlow<Set<Int>>(emptySet())
    val favoritos: StateFlow<Set<Int>> = _favoritos.asStateFlow()

    val categorias: StateFlow<List<Categoria>> = (categoriaRepository?.getCategorias() ?: flowOf(emptyList()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val ordenActual: StateFlow<String> = preferencias.ordenFiltro
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "FECHA_DESC"
        )

    private data class EntradaListado(
        val reportes: List<Reporte>,
        val orden: String,
        val query: String,
        val favoritos: Set<Int>,
        val categoriaId: Int?
    )

    val uiState: StateFlow<ListadoUiState> = combine(
        repository.getReportes(),
        preferencias.ordenFiltro,
        _busqueda,
        _favoritos,
        _categoriaSeleccionadaId
    ) { reportes, orden, query, favoritos, categoriaId ->
        EntradaListado(reportes, orden, query, favoritos, categoriaId)
    }.mapLatest { entrada ->
        val filtrados = entrada.reportes.filter { reporte ->
            val coincideTexto = reporte.titulo.contains(entrada.query, ignoreCase = true) ||
                reporte.descripcion.contains(entrada.query, ignoreCase = true)
            val coincideCategoria = entrada.categoriaId == null || reporte.categoriaId == entrada.categoriaId
            coincideTexto && coincideCategoria
        }

        val ordenados = when (entrada.orden) {
            "FECHA_ASC" -> filtrados.sortedBy { it.fecha }
            else -> filtrados.sortedByDescending { it.fecha }
        }

        val conFavoritos = ordenados.map { it.copy(esFavorito = entrada.favoritos.contains(it.id)) }

        if (conFavoritos.isEmpty()) ListadoUiState.Vacio
        else ListadoUiState.Contenido(conFavoritos)
    }.catch { e ->
        emit(ListadoUiState.Error(e.message ?: "Error desconocido"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ListadoUiState.Cargando
    )

    init {
        if (autoSync) {
            sincronizarConServidor()
        }
    }

    fun sincronizarConServidor() {
        viewModelScope.launch {
            _sincronizando.value = true
            val result = repository.refreshReportes()
            _sincronizando.value = false
            result.fold(
                onSuccess = { _syncEstado.value = null },
                onFailure = { err ->
                    if (err is CancellationException) throw err
                    _syncEstado.value = err.message ?: "Modo offline activo"
                }
            )
        }
    }

    fun onBusquedaChange(query: String) {
        _busqueda.value = query
    }

    fun onCategoriaSeleccionada(categoriaId: Int?) {
        _categoriaSeleccionadaId.value = if (_categoriaSeleccionadaId.value == categoriaId) null else categoriaId
    }

    /**
     * HU-21 (CA-21.2): alterna de forma reactiva la bandera `esFavorito` del reporte.
     */
    fun alternarFavorito(reporte: Reporte) {
        _favoritos.update { actuales ->
            if (actuales.contains(reporte.id)) actuales - reporte.id
            else actuales + reporte.id
        }
    }

    fun guardarReporte(reporte: Reporte) {
        viewModelScope.launch {
            _operacionState.value = OperacionUiState.Cargando
            try {
                repository.insertReporte(reporte)
                _operacionState.value = OperacionUiState.Exito
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _operacionState.value = OperacionUiState.Error(e.message ?: "Fallo al guardar")
            }
        }
    }

    fun eliminarReporte(reporte: Reporte) {
        viewModelScope.launch {
            try {
                repository.deleteReporte(reporte)
                _favoritos.update { it - reporte.id }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Manejado por estado de sincronización si falla la red
            }
        }
    }

    fun actualizarOrden(nuevoOrden: String) {
        viewModelScope.launch {
            preferencias.guardarOrdenFiltro(nuevoOrden)
        }
    }

    fun resetOperacion() {
        _operacionState.value = OperacionUiState.Ideal
    }

    class Factory(
        private val repository: ReporteRepository,
        private val preferencias: PreferenciasRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReporteViewModel(repository, preferencias) as T
        }
    }
}
