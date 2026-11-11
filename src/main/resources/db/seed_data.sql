-- ============================================================================
-- PROYECTO: TPI Programacion Avanzada II — Gestion de Torneo de Futbol Amateur
-- ARCHIVO: src/main/resources/db/seed_data.sql
-- ALCANCE: Release 1 (Aplicacion Desktop Java Swing / JDBC / PostgreSQL Local)
-- FECHA ENTREGA: 13/11/2026
-- EQUIPO: Thiago Bernal, Tomas Moroz, Lautaro Rodriguez
-- DOCENTE: Fernando Aguirre
-- DESCRIPCION:
--   Carga de datos de referencia y catalogos iniciales estrictamente indispensables.
--   Consolidado bajo especificaciones confirmadas de INF-03.
--
-- REGLAS DE SEGURIDAD Y DISENO:
--   - No destructivo: No incluye DROP, TRUNCATE ni DELETE masivos.
--   - Reejecutable / Idempotente: Emplea INSERT ... ON CONFLICT DO NOTHING sobre
--     columnas con restriccion UNIQUE.
--   - Sin cuentas ficticias: Por politicas de seguridad (INF-04), no se insertan
--     cuentas de usuario iniciales ni contrasenas en texto plano.
--   - Sin datos deportivos inventados: No se insertan torneos, equipos, jugadores,
--     partidos, cronologias, sanciones ni reclamos ficticios.
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- 1. CATALOGO DE ROLES DEL SISTEMA (TiposDatos.Rol / CU-17 / Consignas TPI)
-- ----------------------------------------------------------------------------
INSERT INTO roles (codigo, nombre, descripcion)
VALUES
    ('ADMINISTRADOR', 'Administrador del Sistema', 'Acceso total y configuracion general de la plataforma.'),
    ('ORGANIZADOR', 'Organizador del Torneo', 'Gestion operativa: equipos, partidos, arbitros y sanciones.'),
    ('ARBITRO', 'Árbitro Oficial', 'Carga de actas de partidos, resultados e incidencias deportivas.'),
    ('CAPITAN', 'Capitán de Equipo', 'Gestion de plantel y presentacion de reclamos reglamentarios.'),
    ('PUBLICO', 'Público General/Invitado', 'Consultas publicas de fixture, tablas de posiciones y estadisticas.')
ON CONFLICT (codigo) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 2. PARAMETROS DE CONFIGURACION GLOBAL (Configuracion.java / CU-18)
-- ----------------------------------------------------------------------------
INSERT INTO configuraciones (clave, valor, descripcion)
VALUES
    ('AMARILLAS_SUSPENSION', 5, 'Cantidad de tarjetas amarillas acumuladas para suspension automatica de 1 fecha (CU-04.1)'),
    ('HORAS_LIMITE_RECLAMO', 72, 'Plazo maximo en horas corridas posteriores a la finalizacion real para presentar reclamo (CU-13)'),
    ('DIAS_LIMITE_APELACION', 5, 'Plazo maximo en dias corridos posteriores a la publicacion formal para apelar una sancion (CU-14)'),
    ('EDAD_MINIMA_GENERAL', 16, 'Edad minima reglamentaria general para habilitacion de jugadores')
ON CONFLICT (clave) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 3. CATALOGO DE CATEGORIAS DEPORTIVAS BASE CONFIRMADAS (Categoria.java / CU-01)
-- Reglas confirmadas:
--   Sub-20: 16 a 20 anos
--   Libre: 16 anos en adelante (sin limite superior, edad_maxima = NULL)
--   Senior: 35 anos en adelante (sin limite superior, edad_maxima = NULL)
--   Generos independientes: Masculino, Femenino, Mixto (configurables segun torneo)
-- ----------------------------------------------------------------------------
INSERT INTO categorias (nombre, genero, edad_minima, edad_maxima)
VALUES
    ('Sub-20', 'Masculino', 16, 20),
    ('Libre', 'Masculino', 16, NULL),
    ('Senior', 'Masculino', 35, NULL)
ON CONFLICT (nombre, genero) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 4. CATALOGO DE MOTIVOS DE SUSPENSION / POSTERGACION (Motivo.java / CU-05)
-- ----------------------------------------------------------------------------
INSERT INTO motivos (motivo)
VALUES
    ('Condiciones climáticas adversas'),
    ('Falta de iluminación o desperfecto en sede'),
    ('Ausencia o no presentación de equipo'),
    ('Incidentes de seguridad o fuerza mayor')
ON CONFLICT (motivo) DO NOTHING;

COMMIT;
