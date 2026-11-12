-- Esquema Relacional MySQL para Gestor de Actividades CTMA (ReportaCTMA)
-- Compatible con MySQL 8+ y MariaDB (XAMPP)

CREATE DATABASE IF NOT EXISTS gestor_actividades_ctma
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE gestor_actividades_ctma;

-- 1. Catálogo de categorías (relación 1:N con reportes y actividades)
CREATE TABLE IF NOT EXISTS categorias (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(80) NOT NULL UNIQUE,
  icono VARCHAR(32) NOT NULL DEFAULT '📋',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Tabla de reportes de formación / incidencias
CREATE TABLE IF NOT EXISTS reportes (
  id INT AUTO_INCREMENT PRIMARY KEY,
  titulo VARCHAR(80) NOT NULL,
  descripcion VARCHAR(240) NOT NULL DEFAULT '',
  fecha BIGINT NOT NULL,
  categoria_id INT NOT NULL,
  resuelto TINYINT(1) NOT NULL DEFAULT 0,
  evidencia_foto_uri VARCHAR(512) NULL,
  evidencia_archivo_uri VARCHAR(512) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_reportes_categoria
    FOREIGN KEY (categoria_id) REFERENCES categorias(id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  INDEX idx_reportes_categoria (categoria_id),
  INDEX idx_reportes_fecha (fecha),
  INDEX idx_reportes_resuelto (resuelto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Tabla de actividades formativas
CREATE TABLE IF NOT EXISTS actividades (
  id INT AUTO_INCREMENT PRIMARY KEY,
  titulo VARCHAR(120) NOT NULL,
  descripcion TEXT NOT NULL,
  progreso TINYINT UNSIGNED NOT NULL DEFAULT 0,
  fecha_entrega BIGINT NOT NULL,
  prioridad ENUM('ALTA', 'MEDIA', 'BAJA') NOT NULL DEFAULT 'MEDIA',
  resuelto TINYINT(1) NOT NULL DEFAULT 0,
  categoria_id INT NULL,
  evidencia_foto_uri VARCHAR(512) NULL,
  evidencia_archivo_uri VARCHAR(512) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_actividades_categoria
    FOREIGN KEY (categoria_id) REFERENCES categorias(id)
    ON UPDATE CASCADE
    ON DELETE SET NULL,
  CONSTRAINT chk_actividades_progreso CHECK (progreso <= 100),
  INDEX idx_actividades_fecha (fecha_entrega),
  INDEX idx_actividades_prioridad (prioridad)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Tabla normalizada de evidencias adjuntas (relación 1:N opcional con reportes o actividades)
CREATE TABLE IF NOT EXISTS evidencias (
  id INT AUTO_INCREMENT PRIMARY KEY,
  reporte_id INT NULL,
  actividad_id INT NULL,
  tipo ENUM('FOTO', 'ARCHIVO') NOT NULL,
  uri VARCHAR(512) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_evidencias_reporte
    FOREIGN KEY (reporte_id) REFERENCES reportes(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT fk_evidencias_actividad
    FOREIGN KEY (actividad_id) REFERENCES actividades(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  INDEX idx_evidencias_reporte (reporte_id),
  INDEX idx_evidencias_actividad (actividad_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Datos semilla iniciales (idempotentes)
INSERT IGNORE INTO categorias (id, nombre, icono) VALUES
  (1, 'General', '📋'),
  (2, 'Infraestructura', '🏗️'),
  (3, 'Académico', '📚'),
  (4, 'Soporte TI', '💻');

INSERT IGNORE INTO actividades (id, titulo, descripcion, progreso, fecha_entrega, prioridad, resuelto, categoria_id) VALUES
  (1, 'Arquitectura MVVM + Hilt', 'Estructurar capas UI, Dominio y Datos con inyección de dependencias.', 85, UNIX_TIMESTAMP(DATE_ADD(NOW(), INTERVAL 2 DAY)) * 1000, 'ALTA', 0, 3),
  (2, 'Migración a MySQL Relacional', 'Conectar API REST Express + MySQL con caché offline en Room.', 60, UNIX_TIMESTAMP(DATE_ADD(NOW(), INTERVAL 5 DAY)) * 1000, 'ALTA', 0, 4),
  (3, 'Navegación Jetpack Compose', 'Implementar rutas tipadas y NavHost desacoplado.', 100, UNIX_TIMESTAMP(DATE_ADD(NOW(), INTERVAL 7 DAY)) * 1000, 'MEDIA', 1, 3);

INSERT IGNORE INTO reportes (id, titulo, descripcion, fecha, categoria_id, resuelto) VALUES
  (1, 'Proyector Sala 204', 'Revisar cable HDMI y calibración de imagen.', UNIX_TIMESTAMP(DATE_ADD(NOW(), INTERVAL 1 DAY)) * 1000, 2, 0),
  (2, 'Acceso a Red Wi-Fi Lab', 'Validar conectividad en ambiente de desarrollo móvil.', UNIX_TIMESTAMP(DATE_ADD(NOW(), INTERVAL 3 DAY)) * 1000, 4, 0);
