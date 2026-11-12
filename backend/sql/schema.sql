-- Esquema Relacional MySQL para Gestor de Cursos y Actividades CTMA (LMS)
-- Compatible con MySQL 8+ y MariaDB (XAMPP)

DROP DATABASE IF EXISTS gestor_actividades_ctma;
CREATE DATABASE gestor_actividades_ctma
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE gestor_actividades_ctma;

-- 1. Usuarios con rol estricto
CREATE TABLE usuarios (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  rol ENUM('INSTRUCTOR', 'APRENDIZ') NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_usuarios_rol (rol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Cursos creados por instructores
CREATE TABLE cursos (
  id INT AUTO_INCREMENT PRIMARY KEY,
  instructor_id INT NOT NULL,
  nombre VARCHAR(120) NOT NULL,
  descripcion TEXT NOT NULL,
  estado ENUM('ACTIVO', 'ARCHIVADO') NOT NULL DEFAULT 'ACTIVO',
  codigo_acceso VARCHAR(16) NOT NULL UNIQUE,
  codigo_activo TINYINT(1) NOT NULL DEFAULT 1,
  created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL,
  CONSTRAINT fk_cursos_instructor
    FOREIGN KEY (instructor_id) REFERENCES usuarios(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  INDEX idx_cursos_instructor (instructor_id),
  INDEX idx_cursos_estado (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Inscripciones de aprendices en cursos (N:M)
CREATE TABLE inscripciones (
  id INT AUTO_INCREMENT PRIMARY KEY,
  curso_id INT NOT NULL,
  aprendiz_id INT NOT NULL,
  fecha_union BIGINT NOT NULL,
  CONSTRAINT fk_inscripciones_curso
    FOREIGN KEY (curso_id) REFERENCES cursos(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT fk_inscripciones_aprendiz
    FOREIGN KEY (aprendiz_id) REFERENCES usuarios(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  UNIQUE KEY uk_curso_aprendiz (curso_id, aprendiz_id),
  INDEX idx_inscripciones_aprendiz (aprendiz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Actividades evaluables de un curso
CREATE TABLE actividades (
  id INT AUTO_INCREMENT PRIMARY KEY,
  curso_id INT NOT NULL,
  titulo VARCHAR(140) NOT NULL,
  descripcion TEXT NOT NULL,
  fecha_limite BIGINT NOT NULL,
  puntaje_maximo DECIMAL(6,2) NOT NULL DEFAULT 5.00,
  created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL,
  CONSTRAINT fk_actividades_curso
    FOREIGN KEY (curso_id) REFERENCES cursos(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT chk_puntaje_maximo CHECK (puntaje_maximo > 0),
  INDEX idx_actividades_curso_fecha (curso_id, fecha_limite)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Adjuntos creados por el instructor para una actividad
CREATE TABLE actividad_adjuntos (
  id INT AUTO_INCREMENT PRIMARY KEY,
  actividad_id INT NOT NULL,
  nombre_original VARCHAR(255) NOT NULL,
  nombre_archivo VARCHAR(255) NOT NULL,
  mime_type VARCHAR(140) NOT NULL,
  tamano_bytes BIGINT NOT NULL,
  created_at BIGINT NOT NULL,
  CONSTRAINT fk_adjuntos_actividad
    FOREIGN KEY (actividad_id) REFERENCES actividades(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  INDEX idx_adjuntos_actividad (actividad_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Entregas y calificaciones por actividad y aprendiz
CREATE TABLE entregas (
  id INT AUTO_INCREMENT PRIMARY KEY,
  actividad_id INT NOT NULL,
  aprendiz_id INT NOT NULL,
  descripcion TEXT NOT NULL,
  nota DECIMAL(6,2) NULL,
  retroalimentacion TEXT NULL,
  exenta TINYINT(1) NOT NULL DEFAULT 0,
  estado ENUM('PENDIENTE', 'ENTREGADA', 'TARDIA', 'CALIFICADA', 'EXENTA') NOT NULL DEFAULT 'ENTREGADA',
  fecha_entrega BIGINT NULL,
  fecha_calificacion BIGINT NULL,
  updated_at BIGINT NOT NULL,
  CONSTRAINT fk_entregas_actividad
    FOREIGN KEY (actividad_id) REFERENCES actividades(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT fk_entregas_aprendiz
    FOREIGN KEY (aprendiz_id) REFERENCES usuarios(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  UNIQUE KEY uk_actividad_aprendiz (actividad_id, aprendiz_id),
  INDEX idx_entregas_aprendiz (aprendiz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Evidencias (múltiples archivos por entrega)
CREATE TABLE entrega_archivos (
  id INT AUTO_INCREMENT PRIMARY KEY,
  entrega_id INT NOT NULL,
  nombre_original VARCHAR(255) NOT NULL,
  nombre_archivo VARCHAR(255) NOT NULL,
  mime_type VARCHAR(140) NOT NULL,
  tamano_bytes BIGINT NOT NULL,
  created_at BIGINT NOT NULL,
  CONSTRAINT fk_archivos_entrega
    FOREIGN KEY (entrega_id) REFERENCES entregas(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  INDEX idx_archivos_entrega (entrega_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Material de apoyo por curso (archivos y enlaces)
CREATE TABLE material_apoyo (
  id INT AUTO_INCREMENT PRIMARY KEY,
  curso_id INT NOT NULL,
  titulo VARCHAR(140) NOT NULL,
  descripcion TEXT NOT NULL,
  tipo ENUM('ARCHIVO', 'ENLACE') NOT NULL,
  enlace_url VARCHAR(512) NULL,
  nombre_original VARCHAR(255) NULL,
  nombre_archivo VARCHAR(255) NULL,
  mime_type VARCHAR(140) NULL,
  tamano_bytes BIGINT NULL,
  created_at BIGINT NOT NULL,
  CONSTRAINT fk_material_curso
    FOREIGN KEY (curso_id) REFERENCES cursos(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  INDEX idx_material_curso (curso_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Hilos de foro (público para todo el curso o privado 1-a-1 instructor <-> aprendiz)
CREATE TABLE foro_hilos (
  id INT AUTO_INCREMENT PRIMARY KEY,
  curso_id INT NOT NULL,
  autor_id INT NOT NULL,
  aprendiz_objetivo_id INT NULL,
  tipo ENUM('PUBLICO', 'PRIVADO') NOT NULL DEFAULT 'PUBLICO',
  titulo VARCHAR(160) NOT NULL,
  contenido TEXT NOT NULL,
  created_at BIGINT NOT NULL,
  CONSTRAINT fk_hilo_curso
    FOREIGN KEY (curso_id) REFERENCES cursos(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT fk_hilo_autor
    FOREIGN KEY (autor_id) REFERENCES usuarios(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT fk_hilo_aprendiz_objetivo
    FOREIGN KEY (aprendiz_objetivo_id) REFERENCES usuarios(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  INDEX idx_foro_curso_tipo (curso_id, tipo, aprendiz_objetivo_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Respuestas dentro de un hilo de foro
CREATE TABLE foro_mensajes (
  id INT AUTO_INCREMENT PRIMARY KEY,
  hilo_id INT NOT NULL,
  autor_id INT NOT NULL,
  contenido TEXT NOT NULL,
  created_at BIGINT NOT NULL,
  CONSTRAINT fk_mensaje_hilo
    FOREIGN KEY (hilo_id) REFERENCES foro_hilos(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT fk_mensaje_autor
    FOREIGN KEY (autor_id) REFERENCES usuarios(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  INDEX idx_mensaje_hilo (hilo_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
