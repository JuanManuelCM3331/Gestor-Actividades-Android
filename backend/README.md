# Backend API REST — CTMA Cursos (Node.js + Express + MySQL)

Backend relacional con autenticación JWT, control de acceso por roles (`INSTRUCTOR` / `APRENDIZ`), subida/descarga segura de archivos (hasta **20 MB**) y foro público/privado 1 a 1 para la aplicación Android **CTMA Cursos**.

## 1. Preparar MySQL en XAMPP
1. Abre **XAMPP Control Panel** e inicia **MySQL** (puerto `3306`). *(Apache es opcional si deseas usar `http://localhost/phpmyadmin`).*
2. Configura tus variables en `backend/.env` (puedes partir de `backend/.env.example`):
   ```env
   PORT=3000
   DB_HOST=127.0.0.1
   DB_PORT=3306
   DB_USER=root
   DB_PASSWORD=
   DB_NAME=gestor_actividades_ctma
   JWT_SECRET=cambia_este_secreto_en_produccion
   JWT_EXPIRES_IN=7d
   MAX_FILE_SIZE_MB=20
   ```
3. Inicializa la base de datos relacional (`backend/sql/schema.sql`, 10 tablas InnoDB) y los datos demo:
   ```powershell
   cd backend
   npm.cmd install
   npm.cmd run init-db
   ```
   Cuentas demo creadas por `init-db`:
   - **Instructor:** `instructor@ctma.edu.co` / `Instructor123!`
   - **Aprendiz 1:** `aprendiz1@ctma.edu.co` / `Aprendiz123!`
   - **Aprendiz 2:** `aprendiz2@ctma.edu.co` / `Aprendiz123!`
   - **Código de acceso curso demo:** `CTMA2026`

## 2. Levantar el servidor y ejecutar pruebas
```powershell
cd backend
npm.cmd start
```
Pruebas automatizadas del backend (`node:test`):
```powershell
cd backend
npm.cmd test
```
Verifica salud del servidor: `http://localhost:3000/api/health`

## 3. Política de archivos y notas
- **Tamaño máximo:** `20 MB` por archivo (`MAX_FILE_SIZE_MB=20`).
- **Extensiones permitidas:** `.pdf`, `.doc`, `.docx`, `.xls`, `.xlsx`, `.ppt`, `.pptx`, `.txt`, `.csv`, `.jpg`, `.jpeg`, `.png`, `.webp`, `.gif`, `.mp3`, `.wav`, `.m4a`, `.mp4`, `.webm`, `.zip`, `.rar`.
- **Escala de calificación:** Colombia `0.00–5.00` con conversión automática a `0–100`. Actividades sin calificar cuentan como `0`, salvo las marcadas como **EXENTA** (entregadas por fuera del curso), que se excluyen del promedio.

## 4. Endpoints principales
- `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`
- `GET/POST /api/cursos`, `POST /api/cursos/unirse`, `GET/PUT/DELETE /api/cursos/:cursoId`
- `POST /api/cursos/:cursoId/codigo/regenerar`, `PATCH /api/cursos/:cursoId/codigo/estado`
- `GET /api/cursos/:cursoId/personas`, `DELETE /api/cursos/:cursoId/personas/:aprendizId`, `DELETE /api/cursos/:cursoId/salirse`, `GET /api/cursos/:cursoId/calificaciones`
- `GET/POST /api/actividades/curso/:cursoId`, `PUT/DELETE /api/actividades/:actividadId`
- `GET /api/actividades/:actividadId/entregas`, `POST /api/actividades/:actividadId/entrega`, `PUT /api/actividades/:actividadId/entregas/:aprendizId/calificar`
- `GET/POST /api/materiales/curso/:cursoId`, `DELETE /api/materiales/:materialId`
- `GET/POST /api/foro/curso/:cursoId`, `GET/POST /api/foro/:hiloId/mensajes`
- `GET /api/archivos/:tipo/:id` (`adjunto` | `material` | `entrega`)
