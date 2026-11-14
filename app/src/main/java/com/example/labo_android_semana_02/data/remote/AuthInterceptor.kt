package com.example.labo_android_semana_02.data.remote

import com.example.labo_android_semana_02.data.local.datastore.SessionTokenHolder
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenHolder: SessionTokenHolder,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath

        if (
            path.endsWith("/api/auth/login") ||
            path.endsWith("/api/auth/register") ||
            path.contains("/api/auth/recuperar/")
        ) {
            return chain.proceed(original)
        }

        // PERF-03: Lectura atómica en memoria sin bloquear los hilos del dispatcher de OkHttp
        val token = tokenHolder.get()
        if (token.isNullOrBlank()) {
            return chain.proceed(original)
        }

        val authenticatedRequest = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(authenticatedRequest)
    }
}
