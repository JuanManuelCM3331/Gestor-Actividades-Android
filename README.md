# CTMA Cursos — Plataforma Académica Nativa Android

Aplicación móvil nativa para Android (**Jetpack Compose + Material 3**) respaldada por una API REST en **Node.js + Express** y base de datos relacional **MySQL** (XAMPP), con arquitectura **MVVM + Clean Layers (`ui/`, `domain/`, `data/`, `di/`)**, **Hilt**, **Navigation Compose**, **Room** (caché local), **DataStore** y **Retrofit/OkHttp**.

---

## Arquitectura y tecnologías

- **Presentación (`ui/`):** Jetpack Compose, Material 3 responsive en español, Navigation Compose, `@HiltViewModel`, `StateFlow` y Coroutines.
- **Dominio (`domain/`):** Modelos puros, contratos de repositorios y casos de uso (`CalcularCalificacionCursoUseCase`, `ValidarArchivoUseCase`, `ValidarFormulariosUseCase`).
- **Datos (`data/`):**
  - **MySQL + Node.js/Express (`backend/`):** 10 tablas relacionales InnoDB (`usuarios`, `cursos`, `inscripciones`, `actividades`, `actividad_adjuntos`, `entregas`, `entrega_archivos`, `material_apoyo`, `foro_hilos`, `foro_mensajes`).
  - **Room (`AppDatabase` v6):** caché local por usuario de cursos, actividades, material de apoyo y personas.
  - **DataStore:** sesión JWT segura, rol del usuario y preferencia de tema claro/oscuro.
  - **Retrofit + OkHttp + Multipart + FileProvider:** envío múltiple de evidencias, validación previa de archivos y descarga autenticada.
- **Inyección de dependencias (`di/`):** Hilt (`NetworkModule`, `DatabaseModule`, `RepositoryModule`).

---

## Roles y funcionalidades

### Roles y autenticación
- Registro e inicio de sesión con JWT y selección de rol estricta: **`INSTRUCTOR`** o **`APRENDIZ`**.
- Validación de permisos y propiedad en todos los endpoints del backend (`403` / `401`).

### Instructor
- CRUD completo de cursos (`ACTIVO` / `ARCHIVADO`).
- Código de acceso único generado automáticamente por curso, con opciones para **regenerar** o **desactivar/activar**.
- Ver y expulsar aprendices del curso; consultar el promedio general de cada estudiante.
- CRUD de actividades (título, descripción, fecha límite, puntaje máximo y archivos adjuntos).
- Revisar entregas por actividad, descargar evidencias, calificar con retroalimentación en texto o marcar una entrega como **Exenta** (entregada fuera del curso).
- Subir y eliminar material de apoyo (archivos hasta **20 MB** o enlaces web).
- Participar en el **foro público** del curso y en **foros privados 1 a 1** con cada aprendiz.

### Aprendiz
- Unirse a cursos activos mediante código de acceso o salirse del curso.
- Consultar actividades, fechas límite, archivos del instructor y estado (`Pendiente`, `Entregada`, `Tardía`, `Calificada`, `Exenta`).
- Subir y reenviar evidencias (múltiples archivos de hasta **20 MB** + descripción) mientras la entrega no haya sido calificada.
- Ver su calificación y retroalimentación por actividad y el **promedio total del curso**:
  - Escala principal Colombia **`0.00 – 5.00`** con conversor automático a escala **`0 – 100`**.
  - Actividades sin calificar cuentan como `0`, excepto las actividades marcadas como **Exenta** (excluidas del promedio).
- Descargar o abrir material de apoyo (solo lectura).
- Participar en el **foro público** y en su **foro privado 1 a 1** con el instructor (aislado en backend para que otros aprendices no puedan acceder).

---

## Cómo ejecutar la aplicación

### Requisitos previos
- **XAMPP** con **MySQL** encendido en el puerto `3306`.
- **Node.js** 18+ (`npm.cmd`).
- **Android Studio** o **Android SDK / Gradle** con un dispositivo físico (Depuración USB activa) o emulador.

### 1. Inicializar MySQL y levantar la API REST
```powershell
cd backend
npm.cmd install
npm.cmd run init-db
npm.cmd start
```
El comando `init-db` crea el esquema relacional y carga cuentas de prueba:
- **Instructor:** `instructor@ctma.edu.co` / `Instructor123!`
- **Aprendiz 1:** `aprendiz1@ctma.edu.co` / `Aprendiz123!`
- **Aprendiz 2:** `aprendiz2@ctma.edu.co` / `Aprendiz123!`
- **Código de acceso del curso demo:** `CTMA2026`

Verifica la API en `http://localhost:3000/api/health`.

### 2. Configurar URL del servidor en Android
En [gradle.properties](gradle.properties):
- **Dispositivo físico por cable USB (recomendado):**
  ```properties
  API_BASE_URL=http://127.0.0.1:3000/
  ```
  Redirige el puerto desde PowerShell:
  ```powershell
  & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:3000 tcp:3000
  ```
- **Emulador Android:** `API_BASE_URL=http://10.0.2.2:3000/`.
- **Red Wi-Fi local:** `API_BASE_URL=http://<TU_IP_LAN>:3000/`.

### 3. Compilar, probar e instalar
- **Pruebas del backend:** `cd backend; npm.cmd test`
- **Pruebas unitarias y APK Android:** `.\gradlew testDebugUnitTest compileDebugAndroidTestKotlin assembleDebug --no-daemon`
- **Instalar y abrir en el celular (PowerShell):**
  ```powershell
  .\gradlew installDebug --no-daemon
  & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -n com.example.labo_android_semana_02/.MainActivity
  ```
- **Android Studio:** ejecuta **File -> Sync Project with Gradle Files** y pulsa **Run ▶** sobre la configuración `app`.
