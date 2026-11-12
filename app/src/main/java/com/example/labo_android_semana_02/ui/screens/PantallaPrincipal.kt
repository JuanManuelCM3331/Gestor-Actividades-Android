package com.example.labo_android_semana_02.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.labo_android_semana_02.domain.ActividadFormativa
import com.example.labo_android_semana_02.domain.Categoria
import com.example.labo_android_semana_02.domain.Reporte
import com.example.labo_android_semana_02.ui.ActividadesUiState
import com.example.labo_android_semana_02.ui.ListadoUiState

enum class AgileTab(val title: String) {
    ACTIVIDADES("Reportes"),
    REMOTO("Sincro"),
    MANIFESTO("Manifiesto"),
    SCRUM("Scrum"),
    TESTING("Pruebas")
}

@Composable
fun PantallaPrincipal(
    uiState: ListadoUiState,
    syncUiState: ActividadesUiState,
    busqueda: String,
    categorias: List<Categoria>,
    categoriaSeleccionadaId: Int?,
    ordenActual: String,
    syncEstado: String?,
    sincronizando: Boolean,
    onBusquedaChange: (String) -> Unit,
    onCategoriaSeleccionada: (Int?) -> Unit,
    onToggleOrden: () -> Unit,
    onSyncReportes: () -> Unit,
    onReporteClick: (Reporte) -> Unit,
    onAddClick: () -> Unit,
    onDelete: (Reporte) -> Unit,
    onEdit: (Reporte) -> Unit,
    onToggle: (Reporte) -> Unit,
    onRetry: () -> Unit,
    onRefreshActividades: () -> Unit,
    onActividadClick: (ActividadFormativa) -> Unit,
    temaOscuro: Boolean,
    onToggleTema: (Boolean) -> Unit,
    onToggleFavorito: (Reporte) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    var selectedTab by rememberSaveable { mutableStateOf(AgileTab.ACTIVIDADES) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                AgileTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            selectedTab = tab
                        },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selectedTab == tab) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                AgileTab.ACTIVIDADES -> PantallaActividades(
                    uiState = uiState,
                    busqueda = busqueda,
                    onBusquedaChange = onBusquedaChange,
                    onReporteClick = onReporteClick,
                    onDeleteReporte = onDelete,
                    onEditReporte = onEdit,
                    onToggleStatus = onToggle,
                    onAddClick = onAddClick,
                    onRetry = onRetry,
                    temaOscuro = temaOscuro,
                    onToggleTema = onToggleTema,
                    onToggleFavorito = onToggleFavorito,
                    categorias = categorias,
                    categoriaSeleccionadaId = categoriaSeleccionadaId,
                    onCategoriaSeleccionada = onCategoriaSeleccionada,
                    ordenActual = ordenActual,
                    onToggleOrden = onToggleOrden,
                    syncEstado = syncEstado,
                    sincronizando = sincronizando,
                    onSyncClick = onSyncReportes
                )
                AgileTab.REMOTO -> PantallaActividadesRemotas(
                    uiState = syncUiState,
                    onRefresh = onRefreshActividades,
                    onActividadClick = onActividadClick
                )
                AgileTab.MANIFESTO -> SeccionManifiesto()
                AgileTab.SCRUM -> SeccionScrum()
                AgileTab.TESTING -> SeccionPruebas()
            }
        }
    }
}
