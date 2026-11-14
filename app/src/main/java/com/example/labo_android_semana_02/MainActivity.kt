package com.example.labo_android_semana_02

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.labo_android_semana_02.ui.navigation.AppNavigation
import com.example.labo_android_semana_02.ui.theme.Labo_android_semana_02Theme
import com.example.labo_android_semana_02.ui.viewmodel.TemaViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val temaViewModel: TemaViewModel = hiltViewModel()
            val temaOscuro by temaViewModel.temaOscuro.collectAsState()

            // Los iconos de la barra de estado/navegacion siguen el tema de la app
            // (no solo el del sistema) para mantener el contraste.
            LaunchedEffect(temaOscuro) {
                val estilo = if (temaOscuro) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
            }

            Labo_android_semana_02Theme(darkTheme = temaOscuro) {
                AppNavigation(
                    temaOscuro = temaOscuro,
                    onAlternarTema = temaViewModel::alternarTema,
                )
            }
        }
    }
}
