package com.example.labo_android_semana_02.data.local.datastore

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.security.KeyStore
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ctma_lms_prefs")

/**
 * SEC-01: Cifrado simétrico autenticado AES-256-GCM respaldado por AndroidKeyStore
 * para proteger el token JWT en reposo dentro de DataStore.
 */
private object JwtKeystoreCrypto {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "ctma_jwt_aes_gcm_key_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val PREFIX = "enc:v1:"

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encrypt(plainText: String): String = runCatching {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherB64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        "$PREFIX$ivB64:$cipherB64"
    }.getOrDefault(plainText)

    fun decrypt(stored: String?): String? {
        if (stored.isNullOrBlank()) return null
        if (!stored.startsWith(PREFIX)) return stored
        return runCatching {
            val payload = stored.removePrefix(PREFIX)
            val parts = payload.split(":", limit = 2)
            if (parts.size != 2) return null
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        }.getOrNull()
    }
}

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
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // PERF-03: Pre-cargar el token en memoria en segundo plano al inicializar DataStore
        ioScope.launch {
            runCatching { loadTokenIfMissing() }
        }
    }

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
        val token = JwtKeystoreCrypto.decrypt(prefs[Keys.JWT_TOKEN])
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
        val encryptedToken = JwtKeystoreCrypto.encrypt(session.token)
        context.dataStore.edit { prefs ->
            prefs[Keys.JWT_TOKEN] = encryptedToken
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
        val stored = JwtKeystoreCrypto.decrypt(context.dataStore.data.first()[Keys.JWT_TOKEN])
        tokenHolder.set(stored)
        return stored
    }

    suspend fun guardarTemaOscuro(oscuro: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TEMA_OSCURO] = oscuro
        }
    }
}
