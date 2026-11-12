package com.example.labo_android_semana_02

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

            Labo_android_semana_02Theme(darkTheme = temaOscuro) {
                AppNavigation(
                    temaOscuro = temaOscuro,
                    onAlternarTema = temaViewModel::alternarTema,
                )
            }
        }
    }
}
