package com.example.labo_android_semana_02.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                    Column {
                        Text(
                            text = "CTMA Cursos",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Plataforma académica para instructores y aprendices",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onAlternarTema) {
                        Text(if (temaOscuro) "Claro" else "Oscuro")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = if (state.esRegistro) "Crear cuenta" else "Iniciar sesión",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
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
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
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
                                FilterChip(
                                    selected = state.rolSeleccionado == RolUsuario.APRENDIZ,
                                    onClick = { onRolChange(RolUsuario.APRENDIZ) },
                                    label = { Text("Aprendiz") },
                                    modifier = Modifier.weight(1f),
                                )
                                FilterChip(
                                    selected = state.rolSeleccionado == RolUsuario.INSTRUCTOR,
                                    onClick = { onRolChange(RolUsuario.INSTRUCTOR) },
                                    label = { Text("Instructor") },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        OutlinedTextField(
                            value = state.email,
                            onValueChange = onEmailChange,
                            label = { Text("Correo electrónico") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        OutlinedTextField(
                            value = state.password,
                            onValueChange = onPasswordChange,
                            label = { Text("Contraseña") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                TextButton(onClick = { mostrarPassword = !mostrarPassword }) {
                                    Text(if (mostrarPassword) "Ocultar" else "Ver")
                                }
                            },
                            visualTransformation = if (mostrarPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Button(
                            onClick = onEnviar,
                            enabled = !state.cargando,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
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

                        TextButton(
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
                                AssistChip(
                                    onClick = {
                                        onEmailChange("instructor@ctma.edu.co")
                                        onPasswordChange("Instructor123!")
                                    },
                                    label = { Text("Instructor demo") },
                                )
                                AssistChip(
                                    onClick = {
                                        onEmailChange("aprendiz1@ctma.edu.co")
                                        onPasswordChange("Aprendiz123!")
                                    },
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
