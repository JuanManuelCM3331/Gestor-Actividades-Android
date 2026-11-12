package com.example.labo_android_semana_02.di

import com.example.labo_android_semana_02.BuildConfig
import com.example.labo_android_semana_02.data.remote.AuthInterceptor
import com.example.labo_android_semana_02.data.remote.api.ActividadApi
import com.example.labo_android_semana_02.data.remote.api.AuthApi
import com.example.labo_android_semana_02.data.remote.api.CursoApi
import com.example.labo_android_semana_02.data.remote.api.ForoApi
import com.example.labo_android_semana_02.data.remote.api.MaterialApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideCursoApi(retrofit: Retrofit): CursoApi = retrofit.create(CursoApi::class.java)

    @Provides
    @Singleton
    fun provideActividadApi(retrofit: Retrofit): ActividadApi = retrofit.create(ActividadApi::class.java)

    @Provides
    @Singleton
    fun provideMaterialApi(retrofit: Retrofit): MaterialApi = retrofit.create(MaterialApi::class.java)

    @Provides
    @Singleton
    fun provideForoApi(retrofit: Retrofit): ForoApi = retrofit.create(ForoApi::class.java)
}
