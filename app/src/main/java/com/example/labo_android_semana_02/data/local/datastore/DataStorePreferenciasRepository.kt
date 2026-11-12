package com.example.labo_android_semana_02.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.labo_android_semana_02.domain.repository.PreferenciasRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

@Singleton
class DataStorePreferenciasRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferenciasRepository {

    companion object {
        val ORDEN_FILTRO = stringPreferencesKey("orden_filtro")
        val TEMA_OSCURO = booleanPreferencesKey("tema_oscuro")
    }

    override val ordenFiltro: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ORDEN_FILTRO] ?: "FECHA_DESC"
    }

    override val temaOscuro: Flow<Boolean?> = context.dataStore.data.map { preferences ->
        preferences[TEMA_OSCURO]
    }

    override suspend fun guardarOrdenFiltro(orden: String) {
        context.dataStore.edit { preferences ->
            preferences[ORDEN_FILTRO] = orden
        }
    }

    override suspend fun guardarTemaOscuro(oscuro: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[TEMA_OSCURO] = oscuro
        }
    }
}
