# Changelog

Todos los cambios notables de **CTMA Cursos (Gestor de Actividades CTMA)** se documentan en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto sigue [Semantic Versioning](https://semver.org/lang/es/).

---

## [2.3.0] - 2026-11-14

### Agregado (`feat`)
- **Gestión integral de cuenta y cumplimiento Google Play (`Android` + `Backend`):**
  - Endpoint `DELETE /api/auth/me` y `POST /api/auth/eliminar-cuenta` con verificación de contraseña, revocación de sesión y borrado físico de archivos adjuntos del usuario antes de eliminar el registro en cascada.
  - Botón y diálogo **«Mi cuenta»** en la barra superior de `CursosScreen` para actualizar el nombre de perfil (`PUT /api/auth/perfil`), cambiar la contraseña (`PUT /api/auth/password`) o eliminar definitivamente la cuenta.
- **Recuperación de contraseña mediante código OTP (`Android` + `Backend`):**
  - Endpoints `POST /api/auth/recuperar/solicitar` y `POST /api/auth/recuperar/confirmar` con generación de código numérico de 6 dígitos y expiración de 10 minutos.
  - Diálogo interactivo **«¿Olvidaste tu contraseña?»** en `AuthScreen`.
- **Visor de imágenes con Zoom y compresión automática (`Android`):**
  - Diálogo a pantalla completa `DialogoVisorImagenZoom` con soporte para *pinch-to-zoom* (`1x` a `5x`), paneo libre y *doble toque* al tocar cualquier imagen del chat.
  - Compresión automática en segundo plano (`comprimirImagenSiEsNecesario` en `MultipartHelper.kt`) que escala imágenes grandes a un máximo de `1600 px` y comprime en `JPEG (82%)` antes de subirlas.
- **Sincronización en tiempo real y resiliencia en el Foro (`Android`):**
  - Sincronización automática silenciosa cada **5 segundos** (`iniciarSincronizacionTiempoReal` en `CursoPanelViewModel`) mientras el usuario permanece en la pestaña Foro.
  - Reintento automático con *backoff* al enviar mensajes del foro en caso de fallos transitorios de conexión.
- **Notificaciones locales del sistema y registro de fallos (`Android`):**
  - `NotificadorLocal` (`NotificationCompat`) con canal `ctma_lms_eventos` y solicitud de permiso `POST_NOTIFICATIONS` en Android 13+ para alertar sobre nuevas actividades, calificaciones y mensajes del foro.
  - Manejador local de excepciones no capturadas (`instalarManejadorFallosLocal`) que guarda trazas de diagnóstico en `filesDir/crash_logs/ultimo_crash.log`.
- **Licencia y Documentación (`Docs`):**
  - Archivo `LICENSE` oficial bajo **GNU General Public License v3.0 (GPL-3.0)**.
  - Creación de `CHANGELOG.md` y actualización integral de `README.md`.

### Seguridad (`fix`)
- **`SEC-01` — Cifrado de JWT en `AndroidKeyStore` y protección de Backups:**
  - Implementación de `TokenCryptoManager` con cifrado autenticado `AES-256-GCM` en `AndroidKeyStore` para proteger el token JWT almacenado en `DataStore`.
  - Exclusión explícita de `datastore` y `database` en `backup_rules.xml` y `data_extraction_rules.xml`.
- **`SEC-02` — Restricción de registro de rol `INSTRUCTOR`:**
  - Validación obligatoria de dominio institucional autorizado (`@sena.edu.co`, `@ctma.edu.co`, `@soy.sena.edu.co`) en cliente y servidor, más soporte para código de invitación docente (`INSTRUCTOR_INVITE_CODE`).
- **`SEC-03` — Protección contra Fuerza Bruta y DoS (`Rate Limiting`):**
  - Nuevo middleware `rateLimit.js` por ventana deslizante aplicado globalmente y con límites estrictos en `/api/auth/login`, `/api/auth/register`, `/api/auth/recuperar/*` y `/api/cursos/unirse`.
- **`SEC-04` — Autorización previa a Multer e inspección de Magic Bytes:**
  - Verificación de pertenencia al curso y rol (`requireCourseAccessMiddleware`) ejecutada **antes** de que `Multer` procese o escriba archivos en disco.
  - Inspección binaria de cabeceras (*magic bytes*) en `filePolicy.js` para detectar, borrar y rechazar ejecutables o scripts camuflados (`MZ`, `ELF`, `Mach-O`, `#!/`, `<?php`, `<script`).
- **`SEC-05` — Revocación de sesiones JWT, CORS restringido y `/api/health` seguro:**
  - Reducción de vigencia por defecto del JWT a `12h`, inclusión de identificador único `jti` y endpoint `POST /api/auth/logout` con lista de revocación en servidor.
  - Lista blanca de orígenes en `CORS` y ocultamiento de detalles internos de MySQL en `/api/health`.
- **`SEC-06` — Limpieza automática de archivos temporales al cerrar sesión:**
  - `FileDownloadManager.limpiarArchivosTemporales()` elimina recursivamente `cacheDir/descargas` y `cacheDir/grabaciones_foro` al cerrar sesión o cambiar de usuario.

### Rendimiento (`refactor`)
- **`PERF-01` — Subida de archivos por Streaming en Android:**
  - Implementación de `ContentUriRequestBody` en `MultipartHelper.kt` para transmitir adjuntos en bloques de `8 KB` sin cargar el archivo completo en la memoria RAM.
- **`PERF-02` — Eliminación de consultas SQL `N+1` en Backend:**
  - Optimización de `GET /api/cursos/:cursoId/actividades` y `GET /api/cursos/:cursoId/calificaciones-resumen` agrupando adjuntos y estadísticas mediante consultas `WHERE ... IN (...)`.
- **`PERF-03` — Caché en memoria `LruCache` y submuestreo de imágenes en el Chat:**
  - `RemoteImageAttachmentPreview` incorpora un `LruCache` de `20 MB` y decodificación con `inSampleSize` para un desplazamiento fluido a `60 fps`.
- **`PERF-04` — Paginación de mensajes en el Foro:**
  - Soporte para parámetros `limit` (por defecto `100`, máx. `200`) y `beforeId` en `GET /api/cursos/:cursoId/foro`, junto con índice compuesto `idx_foro_curso_dest_fecha`.
- **`PERF-05` — Lectura no bloqueante del token JWT en OkHttp:**
  - Reemplazo de `runBlocking` en cada petición HTTP por lectura instantánea desde variable `@Volatile` sincronizada en segundo plano en `PreferenciasDataStore`.
- **`PERF-06` — Configuración de compilación Release en Android:**
  - Activación de R8 (`isMinifyEnabled = true`, `isShrinkResources = true`) y reglas `proguard-rules.pro` para Retrofit, Kotlinx Serialization y Room.

---

## [2.2.0] - 2026-11-14

### Agregado (`feat`)
- **Sección de Notas (Libro de Calificaciones) y Exportación Excel (`.xlsx`):**
  - Nueva pestaña **«Notas»** ubicada a la derecha de **«Personas»** en `CursoPanelScreen`.
  - Tabla matricial con todas las actividades del curso y el promedio total de cada integrante en escala sobre `5.0` y sobre `100`.
  - Generador nativo de archivos OpenXML `.xlsx` (`ExcelCalificacionesExporter`) compatible con Microsoft Excel, Google Sheets y LibreOffice Calc.
- **Rediseño de la barra de composición del Foro:**
  - Menú desplegable compacto con botón `+` a la izquierda del campo de texto que agrupa los botones **Imagen**, **Archivo**, **Audio** y **URL**.
  - Ubicación dedicada para el botón de grabación de **Nota de voz (`Voz`)** a la derecha del campo de texto y a la izquierda del botón de enviar.
- **Selector de formato de calificación al crear actividades:**
  - Selección directa entre **Formato 5 puntos (`0 - 5`)** y **Formato 100 puntos (`0 - 100`)** al crear o editar una actividad.

### Eliminado (`refactor`)
- Eliminación del conversor manual de escala en la sección de Personas por redundancia con la nueva pestaña de Notas.

---

## [2.1.0] - 2026-11-13

### Agregado (`feat`)
- **Foro de Clase estilo Chat Convencional y Mensajería Privada 1 a 1:**
  - Canal grupal del curso y conversaciones privadas directas (`1 a 1`) entre instructor y aprendices.
  - Soporte para adjuntar imágenes con vista previa, documentos descargables, archivos de audio, grabación de notas de voz (`.m4a`) con reproductor integrado y enlaces URL.
  - Fijado de mensajes importantes (*pin*) por parte del instructor y respuestas citando mensajes.
- **Diseño Visual Warm Palette y Microinteracciones:**
  - Paleta cálida terracota/café (`Warm Light` y `Warm Dark`) con colores semánticos por estado de entrega.
  - Controles táctiles con animación *spring* y respuesta háptica.

---

## [2.0.0] - 2026-11-12

### Agregado (`feat`)
- **Migración completa a Arquitectura LMS Cliente-Servidor:**
  - Reestructuración del proyecto Android bajo **MVVM + Clean Architecture + Dagger Hilt + Navigation Compose + StateFlow**.
  - Creación del backend RESTful en **Node.js + Express** y base de datos relacional **MySQL (`ctma_lms`)** con 10 tablas normalizadas, transacciones ACID y scripts de inicialización (`initDb.js`) y poblado (`seed.js`).
  - Implementación de módulos completos de **Autenticación JWT**, **Cursos por código de acceso**, **Trabajo de Clase (Actividades, Entregas con archivos y Calificaciones con escala/exención)**, **Materiales de apoyo** y **Personas**.
