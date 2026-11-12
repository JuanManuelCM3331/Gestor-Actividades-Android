package com.example.labo_android_semana_02.data.remote

import javax.inject.Inject
import javax.inject.Singleton

interface TokenProvider {
    fun getToken(): String
}

@Singleton
class SessionTokenProvider @Inject constructor() : TokenProvider {
    override fun getToken(): String {
        // En entorno productivo con autenticación de usuario, leer desde DataStore/EncryptedPrefs.
        return "Bearer local_dev_session"
    }
}
