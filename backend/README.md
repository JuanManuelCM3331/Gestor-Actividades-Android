# Backend API REST (Node.js + Express + MySQL)

Backend relacional para la app nativa Android **ReportaCTMA**.

## 1. Preparar MySQL en XAMPP
1. Abre **XAMPP Control Panel** e inicia el módulo **MySQL** (puerto `3306`). *(Opcional: inicia **Apache** si quieres gestionar tablas desde `http://localhost/phpmyadmin`).*
2. Crea la base de datos y tablas relacionales ejecutando:
   ```powershell
   cd backend
   npm.cmd run init-db
   ```
   *(Alternativamente, importa `backend/sql/schema.sql` desde phpMyAdmin).*

## 2. Levantar el servidor API
```powershell
cd backend
npm.cmd install
npm.cmd start
```
Verifica en el navegador: `http://localhost:3000/api/health`

## 3. Conectar tu dispositivo físico Android
- **Opción A (Cable USB con `adb reverse` - recomendada):**
  Conecta tu celular con Depuración USB activa y ejecuta:
  ```powershell
  adb reverse tcp:3000 tcp:3000
  ```
  Y en `gradle.properties` usa `API_BASE_URL=http://127.0.0.1:3000/`.
- **Opción B (Wi-Fi local):**
  Usa la IP LAN de tu PC (`http://192.168.78.114:3000/`) en `API_BASE_URL` dentro de `gradle.properties` y asegúrate de permitir el puerto `3000` en el Firewall de Windows.

## 4. Despliegue a Producción (Play Store)
En producción (Render, Railway, Fly.io o VPS con Nginx + HTTPS):
1. Configura `DB_HOST`, `DB_USER`, `DB_PASSWORD`, `DB_NAME` y `API_BEARER_TOKEN` como variables de entorno.
2. Configura `API_RELEASE_BASE_URL=https://tu-dominio.com/` en `gradle.properties`.
