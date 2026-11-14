package com.example.labo_android_semana_02.ui.screens

import com.example.labo_android_semana_02.ui.components.AppAssistChip
import com.example.labo_android_semana_02.ui.components.AppButton
import com.example.labo_android_semana_02.ui.components.AppFilterChip
import com.example.labo_android_semana_02.ui.components.AppIconButton
import com.example.labo_android_semana_02.ui.components.AppTextButton

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.labo_android_semana_02.ui.components.IconoTintado
import com.example.labo_android_semana_02.ui.components.TituloConIcono
import com.example.labo_android_semana_02.ui.theme.coloresExtendidos
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.labo_android_semana_02.BuildConfig
import com.example.labo_android_semana_02.domain.model.RolUsuario
import com.example.labo_android_semana_02.ui.components.BannerMensaje
import com.example.labo_android_semana_02.ui.components.WrapRow
import com.example.labo_android_semana_02.ui.viewmodel.AuthUiState

@Composable
fun AuthScreen(
    state: AuthUiState,
    onNombreChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRolChange: (RolUsuario) -> Unit,
    onAlternarModo: () -> Unit,
    onEnviar: () -> Unit,
    onLimpiarError: () -> Unit,
    onAlternarTema: () -> Unit,
    temaOscuro: Boolean,
) {
    var mostrarPassword by remember { mutableStateOf(false) }

    Scaffold(containerColor = Color.Transparent) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                )
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconoTintado(
                            icono = Icons.Filled.School,
                            color = MaterialTheme.colorScheme.primary,
                            tamano = 52.dp,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "CTMA Cursos",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = "Plataforma académica para instructores y aprendices",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    AppIconButton(onClick = onAlternarTema) {
                        Icon(
                            imageVector = if (temaOscuro) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = if (temaOscuro) "Cambiar a modo claro" else "Cambiar a modo oscuro",
                            tint = if (temaOscuro) MaterialTheme.coloresExtendidos.advertencia
                            else MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TituloConIcono(
                            texto = if (state.esRegistro) "Crear cuenta" else "Iniciar sesión",
                            icono = if (state.esRegistro) Icons.Filled.PersonAdd else Icons.AutoMirrored.Filled.Login,
                        )

                        if (state.error != null) {
                            BannerMensaje(
                                texto = state.error,
                                esError = true,
                                onDismiss = onLimpiarError,
                            )
                        }

                        if (state.esRegistro) {
                            OutlinedTextField(
                                value = state.nombre,
                                onValueChange = onNombreChange,
                                label = { Text("Nombre completo") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Text(
                                text = "Selecciona tu rol",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                AppFilterChip(
                                    selected = state.rolSeleccionado == RolUsuario.APRENDIZ,
                                    onClick = { onRolChange(RolUsuario.APRENDIZ) },
                                    icon = Icons.Filled.School,
                                    label = { Text("Aprendiz") },
                                    modifier = Modifier.weight(1f),
                                )
                                AppFilterChip(
                                    selected = state.rolSeleccionado == RolUsuario.INSTRUCTOR,
                                    onClick = { onRolChange(RolUsuario.INSTRUCTOR) },
                                    icon = Icons.Filled.Badge,
                                    label = { Text("Instructor") },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        OutlinedTextField(
                            value = state.email,
                            onValueChange = onEmailChange,
                            label = { Text("Correo electrónico") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Email,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        OutlinedTextField(
                            value = state.password,
                            onValueChange = onPasswordChange,
                            label = { Text("Contraseña") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                            trailingIcon = {
                                AppIconButton(onClick = { mostrarPassword = !mostrarPassword }) {
                                    Icon(
                                        imageVector = if (mostrarPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (mostrarPassword) "Ocultar contraseña" else "Mostrar contraseña",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                            },
                            visualTransformation = if (mostrarPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        AppButton(
                            onClick = onEnviar,
                            enabled = !state.cargando,
                            icon = if (state.cargando) null
                            else if (state.esRegistro) Icons.Filled.PersonAdd
                            else Icons.AutoMirrored.Filled.Login,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                        ) {
                            if (state.cargando) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.height(20.dp),
                                )
                            } else {
                                Text(if (state.esRegistro) "Registrarme" else "Ingresar")
                            }
                        }

                        AppTextButton(
                            onClick = onAlternarModo,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text(
                                if (state.esRegistro) "¿Ya tienes cuenta? Inicia sesión"
                                else "¿No tienes cuenta? Regístrate aquí"
                            )
                        }

                        if (BuildConfig.DEBUG && !state.esRegistro) {
                            Text(
                                text = "Autocompletar cuentas demo (servidor local):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            WrapRow(
                                horizontalSpacing = 8.dp,
                                verticalSpacing = 6.dp,
                            ) {
                                AppAssistChip(
                                    onClick = {
                                        onEmailChange("instructor@ctma.edu.co")
                                        onPasswordChange("Instructor123!")
                                    },
                                    icon = Icons.Filled.Badge,
                                    label = { Text("Instructor demo") },
                                )
                                AppAssistChip(
                                    onClick = {
                                        onEmailChange("aprendiz1@ctma.edu.co")
                                        onPasswordChange("Aprendiz123!")
                                    },
                                    icon = Icons.Filled.School,
                                    label = { Text("Aprendiz demo") },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
