-- ========================================================
-- Base de datos: rpg_grupal_db
-- Actividad Unidad 5.2: JDBC y MVC aplicados al Simulador RPG
-- ========================================================

CREATE DATABASE IF NOT EXISTS rpg_grupal_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE rpg_grupal_db;

-- Tabla heroes para persistencia
CREATE TABLE IF NOT EXISTS heroes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    tipo VARCHAR(20) NOT NULL,
    vida_maxima INT NOT NULL,
    nivel INT NOT NULL DEFAULT 1,
    CONSTRAINT chk_tipo CHECK (LOWER(tipo) IN ('guerrero', 'mago')),
    CONSTRAINT chk_vida CHECK (vida_maxima > 0),
    CONSTRAINT chk_nivel CHECK (nivel >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Registros iniciales de prueba (opcionales)
INSERT INTO heroes (nombre, tipo, vida_maxima, nivel)
VALUES 
    ('Aragorn', 'guerrero', 120, 1),
    ('Gandalf', 'mago', 80, 1)
ON DUPLICATE KEY UPDATE id=id;
