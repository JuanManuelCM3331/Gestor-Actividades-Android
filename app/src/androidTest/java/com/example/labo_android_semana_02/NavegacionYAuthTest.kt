package com.example.labo_android_semana_02

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.example.labo_android_semana_02.domain.model.RolUsuario
import com.example.labo_android_semana_02.ui.screens.AuthScreen
import com.example.labo_android_semana_02.ui.theme.Labo_android_semana_02Theme
import com.example.labo_android_semana_02.ui.viewmodel.AuthUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NavegacionYAuthTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pantallaAuthMuestraRolesYNoMuestraSeccionesEliminadas() {
        composeRule.setContent {
            Labo_android_semana_02Theme {
                AuthScreen(
                    state = AuthUiState(esRegistro = true, rolSeleccionado = RolUsuario.APRENDIZ),
                    onNombreChange = {},
                    onEmailChange = {},
                    onPasswordChange = {},
                    onRolChange = {},
                    onAlternarModo = {},
                    onEnviar = {},
                    onLimpiarError = {},
                    onAlternarTema = {},
                    temaOscuro = false,
                )
            }
        }

        composeRule.onNodeWithText("CTMA Cursos").assertIsDisplayed()
        composeRule.onNodeWithText("Crear cuenta").assertIsDisplayed()
        composeRule.onNodeWithText("Aprendiz").assertIsDisplayed()
        composeRule.onNodeWithText("Instructor").assertIsDisplayed()

        // Verificar que las secciones eliminadas ya no existan en la UI
        assertTrue(composeRule.onAllNodesWithText("Manifiesto Ágil", substring = true).fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Scrum Framework", substring = true).fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Testing", substring = false).fetchSemanticsNodes().isEmpty())
    }
}
