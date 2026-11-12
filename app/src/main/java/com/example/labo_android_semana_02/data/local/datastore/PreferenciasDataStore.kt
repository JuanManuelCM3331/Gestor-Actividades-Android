package com.example.labo_android_semana_02.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.labo_android_semana_02.domain.model.AuthSession
import com.example.labo_android_semana_02.domain.model.RolUsuario
import com.example.labo_android_semana_02.domain.model.Usuario
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ctma_lms_prefs")

@Singleton
class SessionTokenHolder @Inject constructor() {
    private val tokenRef = AtomicReference<String?>(null)

    fun get(): String? = tokenRef.get()
    fun set(token: String?) {
        tokenRef.set(token)
    }
}

@Singleton
class PreferenciasDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenHolder: SessionTokenHolder,
) {
    private object Keys {
        val TEMA_OSCURO = booleanPreferencesKey("tema_oscuro")
        val JWT_TOKEN = stringPreferencesKey("jwt_token")
        val USER_ID = intPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_ROLE = stringPreferencesKey("user_role")
    }

    val temaOscuroFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.TEMA_OSCURO] ?: false
    }

    val sessionFlow: Flow<AuthSession?> = context.dataStore.data.map { prefs ->
        val token = prefs[Keys.JWT_TOKEN]
        val userId = prefs[Keys.USER_ID]
        val nombre = prefs[Keys.USER_NAME]
        val email = prefs[Keys.USER_EMAIL]
        val rolStr = prefs[Keys.USER_ROLE]

        if (token.isNullOrBlank() || userId == null || nombre == null || email == null || rolStr == null) {
            tokenHolder.set(null)
            null
        } else {
            val rol = runCatching { RolUsuario.valueOf(rolStr) }.getOrNull()
            if (rol == null) {
                tokenHolder.set(null)
                null
            } else {
                tokenHolder.set(token)
                AuthSession(
                    token = token,
                    usuario = Usuario(id = userId, nombre = nombre, email = email, rol = rol),
                )
            }
        }
    }

    suspend fun saveSession(session: AuthSession) {
        tokenHolder.set(session.token)
        context.dataStore.edit { prefs ->
            prefs[Keys.JWT_TOKEN] = session.token
            prefs[Keys.USER_ID] = session.usuario.id
            prefs[Keys.USER_NAME] = session.usuario.nombre
            prefs[Keys.USER_EMAIL] = session.usuario.email
            prefs[Keys.USER_ROLE] = session.usuario.rol.name
        }
    }

    suspend fun updateUsuario(usuario: Usuario) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_ID] = usuario.id
            prefs[Keys.USER_NAME] = usuario.nombre
            prefs[Keys.USER_EMAIL] = usuario.email
            prefs[Keys.USER_ROLE] = usuario.rol.name
        }
    }

    suspend fun clearSession() {
        tokenHolder.set(null)
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.JWT_TOKEN)
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USER_NAME)
            prefs.remove(Keys.USER_EMAIL)
            prefs.remove(Keys.USER_ROLE)
        }
    }

    suspend fun loadTokenIfMissing(): String? {
        val cached = tokenHolder.get()
        if (!cached.isNullOrBlank()) return cached
        val stored = context.dataStore.data.first()[Keys.JWT_TOKEN]
        tokenHolder.set(stored)
        return stored
    }

    suspend fun guardarTemaOscuro(oscuro: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TEMA_OSCURO] = oscuro
        }
    }
}
