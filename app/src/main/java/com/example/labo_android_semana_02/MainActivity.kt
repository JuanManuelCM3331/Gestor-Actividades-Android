package com.example.labo_android_semana_02

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.labo_android_semana_02.ui.navigation.AppNavHost
import com.example.labo_android_semana_02.ui.theme.Labo_android_semana_02Theme
import com.example.labo_android_semana_02.ui.viewmodel.TemaViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val temaViewModel: TemaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val temaPersistido by temaViewModel.temaOscuro.collectAsStateWithLifecycle()
            val temaOscuro = temaPersistido ?: isSystemInDarkTheme()

            Labo_android_semana_02Theme(darkTheme = temaOscuro) {
                AppNavHost(
                    temaOscuro = temaOscuro,
                    onToggleTema = temaViewModel::onToggleTema
                )
            }
        }
    }
}
