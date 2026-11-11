-- ============================================================================
-- PROYECTO: TPI Programacion Avanzada II — Gestion de Torneo de Futbol Amateur
-- ARCHIVO: src/main/resources/db/schema.sql
-- ALCANCE: Release 1 (Aplicacion Desktop Java Swing / JDBC / PostgreSQL Local)
-- FECHA ENTREGA: 13/11/2026
-- EQUIPO: Thiago Bernal, Tomas Moroz, Lautaro Rodriguez
-- DOCENTE: Fernando Aguirre
-- DESCRIPCION:
--   Definicion DDL del esquema relacional consolidado para PostgreSQL local (INF-03).
--   Alineado con el modelo de dominio Java, diagramas UML y Casos de Uso (CU-01 a CU-22).
--
-- REGLAS DE SEGURIDAD Y DISENO:
--   - No destructivo: No incluye DROP DATABASE, DROP SCHEMA ni DROP TABLE.
--   - Idempotente: Utiliza CREATE TABLE IF NOT EXISTS e indices IF NOT EXISTS.
--   - Integridad referencial: Politicas ON DELETE RESTRICT para proteger la historia deportiva.
--   - Tipos de datos estandar PostgreSQL sin tipos ENUM globales, empleando
--     restricciones CHECK y tablas de catalogo para maxima portabilidad.
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- 1. TABLA: roles
-- Justificacion: Catalogo de perfiles de acceso al sistema (TiposDatos.Rol / CU-17).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS roles (
    id_rol SERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(50) NOT NULL,
    descripcion VARCHAR(150),
    CONSTRAINT chk_roles_codigo_no_vacio CHECK (length(trim(codigo)) > 0),
    CONSTRAINT chk_roles_nombre_no_vacio CHECK (length(trim(nombre)) > 0)
);

-- ----------------------------------------------------------------------------
-- 2. TABLA: usuarios
-- Justificacion: Cuentas de acceso, autenticacion y auditoria (Usuario.java / CU-17).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario BIGSERIAL PRIMARY KEY,
    usuario VARCHAR(50) NOT NULL UNIQUE,
    contrasena VARCHAR(255) NOT NULL,
    mail VARCHAR(100) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    id_rol INTEGER NOT NULL REFERENCES roles(id_rol) ON DELETE RESTRICT,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_usuario_formato CHECK (length(trim(usuario)) > 0 AND position(' ' in usuario) = 0),
    CONSTRAINT chk_usuario_contrasena_minima CHECK (length(contrasena) >= 3),
    CONSTRAINT chk_usuario_mail_valido CHECK (mail LIKE '_%@_%._%'),
    CONSTRAINT chk_usuario_nombre_no_vacio CHECK (length(trim(nombre)) > 0),
    CONSTRAINT chk_usuario_apellido_no_vacio CHECK (length(trim(apellido)) > 0)
);

-- ----------------------------------------------------------------------------
-- 3. TABLA: arbitros
-- Justificacion: Especializacion de Usuario (Arbitro.java extends Usuario / CU-03.1 / CU-22).
-- Modela herencia de clase mediante Class Table Inheritance (PK es a la vez FK).
-- Politica ON DELETE RESTRICT para impedir borrado de usuario con historial arbitral.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS arbitros (
    id_usuario BIGINT PRIMARY KEY REFERENCES usuarios(id_usuario) ON DELETE RESTRICT,
    categoria_arbitral VARCHAR(50) NOT NULL,
    telefono VARCHAR(50) NOT NULL,
    CONSTRAINT chk_arbitro_cat_no_vacia CHECK (length(trim(categoria_arbitral)) > 0),
    CONSTRAINT chk_arbitro_tel_no_vacio CHECK (length(trim(telefono)) > 0)
);

-- ----------------------------------------------------------------------------
-- 4. TABLA: canchas
-- Justificacion: Sedes y canchas fisicas para partidos (Cancha.java / GestorCanchas / CU-16).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS canchas (
    id_cancha BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    direccion VARCHAR(200) NOT NULL,
    tipo_superficie VARCHAR(50) NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_cancha_nombre_no_vacio CHECK (length(trim(nombre)) > 0),
    CONSTRAINT chk_cancha_dir_no_vacia CHECK (length(trim(direccion)) > 0),
    CONSTRAINT chk_cancha_superficie_no_vacia CHECK (length(trim(tipo_superficie)) > 0)
);

-- ----------------------------------------------------------------------------
-- 5. TABLA: categorias
-- Justificacion: Categorias deportivas por edad y genero (Categoria.java / CU-01).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categorias (
    id_categoria BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    genero VARCHAR(20) NOT NULL,
    edad_minima INTEGER NOT NULL DEFAULT 0,
    edad_maxima INTEGER,
    CONSTRAINT chk_categoria_nombre_no_vacio CHECK (length(trim(nombre)) > 0),
    CONSTRAINT chk_categoria_genero_no_vacio CHECK (length(trim(genero)) > 0),
    CONSTRAINT chk_categoria_edad_min_no_negativa CHECK (edad_minima >= 0),
    CONSTRAINT chk_categoria_edad_max_no_negativa CHECK (edad_maxima IS NULL OR edad_maxima >= 0),
    CONSTRAINT chk_categoria_rango_edad_coherente CHECK (edad_maxima IS NULL OR edad_minima <= edad_maxima),
    CONSTRAINT uq_categoria_nombre_genero UNIQUE (nombre, genero)
);

-- ----------------------------------------------------------------------------
-- 6. TABLA: torneos
-- Justificacion: Estructura central del torneo deportivo (Torneo.java / CU-15).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS torneos (
    id_torneo BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    formato VARCHAR(30) NOT NULL,
    cupo_equipos INTEGER NOT NULL,
    fecha_inicio DATE NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'CONFIGURACION',
    CONSTRAINT chk_torneo_nombre_no_vacio CHECK (length(trim(nombre)) > 0),
    CONSTRAINT chk_torneo_formato_valido CHECK (formato IN ('LIGA', 'ELIMINATORIA', 'GRUPOS')),
    CONSTRAINT chk_torneo_cupo_minimo CHECK (cupo_equipos >= 2),
    CONSTRAINT chk_torneo_cupo_par CHECK (cupo_equipos % 2 = 0),
    CONSTRAINT chk_torneo_estado_valido CHECK (estado IN ('CONFIGURACION', 'EN_CURSO', 'FINALIZADO'))
);

-- ----------------------------------------------------------------------------
-- 7. TABLA: fases
-- Justificacion: Fases y etapas de ejecucion de un torneo (Fase.java / CU-11 / CU-15).
-- Incluye indicador configurable limpia_amarillas (DEFAULT FALSE) para playoffs.
-- Politica ON DELETE RESTRICT para proteger la estructura de fases configurada.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fases (
    id_fase BIGSERIAL PRIMARY KEY,
    id_torneo BIGINT NOT NULL REFERENCES torneos(id_torneo) ON DELETE RESTRICT,
    nombre VARCHAR(50) NOT NULL,
    orden INTEGER NOT NULL,
    limpia_amarillas BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_fase_nombre_no_vacio CHECK (length(trim(nombre)) > 0),
    CONSTRAINT chk_fase_orden_positivo CHECK (orden > 0),
    CONSTRAINT uq_fase_torneo_orden UNIQUE (id_torneo, orden),
    CONSTRAINT uq_fase_torneo_nombre UNIQUE (id_torneo, nombre)
);

-- ----------------------------------------------------------------------------
-- 8. TABLA: equipos
-- Justificacion: Equipos registrados para disputar torneos (Equipo.java / CU-01).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS equipos (
    id_equipo BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    alias VARCHAR(50),
    id_categoria BIGINT NOT NULL REFERENCES categorias(id_categoria) ON DELETE RESTRICT,
    color_camiseta VARCHAR(50) NOT NULL,
    id_sede_habitual BIGINT REFERENCES canchas(id_cancha) ON DELETE SET NULL,
    id_capitan BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE RESTRICT,
    fecha_registro TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_equipo_nombre_no_vacio CHECK (length(trim(nombre)) > 0),
    CONSTRAINT chk_equipo_color_no_vacio CHECK (length(trim(color_camiseta)) > 0),
    CONSTRAINT uq_equipo_nombre_por_categoria UNIQUE (nombre, id_categoria)
);

-- ----------------------------------------------------------------------------
-- 9. TABLA: jugadores
-- Justificacion: Jugadores que integran los planteles (Jugador.java / CU-02 / CU-02.1).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS jugadores (
    id_jugador BIGSERIAL PRIMARY KEY,
    id_equipo BIGINT REFERENCES equipos(id_equipo) ON DELETE SET NULL,
    nombre VARCHAR(100) NOT NULL,
    dni INTEGER NOT NULL UNIQUE,
    fecha_nacimiento DATE NOT NULL,
    posicion VARCHAR(30) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    historial_tarjetas INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_jugador_nombre_no_vacio CHECK (length(trim(nombre)) > 0),
    CONSTRAINT chk_jugador_dni_valido CHECK (dni > 1000000),
    CONSTRAINT chk_jugador_posicion_no_vacia CHECK (length(trim(posicion)) > 0),
    CONSTRAINT chk_jugador_estado_valido CHECK (estado IN ('ACTIVO', 'SUSPENDIDO', 'LESIONADO', 'INHABILITADO')),
    CONSTRAINT chk_jugador_tarjetas_no_negativas CHECK (historial_tarjetas >= 0)
);

-- ----------------------------------------------------------------------------
-- 10. TABLA: solicitudes_inscripcion
-- Justificacion: Tramite de inscripcion de equipo/plantel (SolicitudInscripcion.java / CU-02.1 / CU-06 / CU-07).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS solicitudes_inscripcion (
    id_solicitud BIGSERIAL PRIMARY KEY,
    id_equipo BIGINT NOT NULL REFERENCES equipos(id_equipo) ON DELETE CASCADE,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    motivo VARCHAR(255),
    fecha_solicitud TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_solicitud_estado_valido CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA'))
);

-- ----------------------------------------------------------------------------
-- 11. TABLA: motivos
-- Justificacion: Justificacion formal de postergaciones/suspensiones (Motivo.java / CU-05).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS motivos (
    id_motivo BIGSERIAL PRIMARY KEY,
    motivo TEXT NOT NULL UNIQUE,
    fecha_registro TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_motivo_longitud_minima CHECK (length(trim(motivo)) >= 5)
);

-- ----------------------------------------------------------------------------
-- 12. TABLA: partidos
-- Justificacion: Encuentros programados y disputados (Partido.java / GestorPartidos / CU-03 / CU-04 / CU-05).
-- Incluye fecha_hora_finalizacion_real (deportiva) y obligatoriedad de id_motivo_suspension
-- para encuentros POSTERGADO o SUSPENDIDO.
-- Politicas ON DELETE RESTRICT en id_arbitro e id_motivo_suspension para preservar historia.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS partidos (
    id_partido BIGSERIAL PRIMARY KEY,
    id_fase BIGINT REFERENCES fases(id_fase) ON DELETE RESTRICT,
    id_cancha BIGINT NOT NULL REFERENCES canchas(id_cancha) ON DELETE RESTRICT,
    id_arbitro BIGINT REFERENCES arbitros(id_usuario) ON DELETE RESTRICT,
    id_motivo_suspension BIGINT REFERENCES motivos(id_motivo) ON DELETE RESTRICT,
    fecha_hora TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_hora_finalizacion_real TIMESTAMP WITH TIME ZONE,
    duracion_partido_minutos INTEGER NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PROGRAMADO',
    CONSTRAINT chk_partido_duracion_positiva CHECK (duracion_partido_minutos > 0),
    CONSTRAINT chk_partido_estado_valido CHECK (estado IN ('PROGRAMADO', 'EN_CURSO', 'FINALIZADO', 'SUSPENDIDO', 'POSTERGADO')),
    CONSTRAINT chk_partido_motivo_suspension CHECK (
        (estado IN ('POSTERGADO', 'SUSPENDIDO') AND id_motivo_suspension IS NOT NULL) OR
        (estado NOT IN ('POSTERGADO', 'SUSPENDIDO'))
    )
);

-- ----------------------------------------------------------------------------
-- 13. TABLA: partido_equipos
-- Justificacion: Asignacion de equipos al encuentro (local/visitante) y goles (PartidoEquipo.java / CU-03 / CU-04).
-- Asegura que un partido tenga exactamente un local y un visitante, sin equipos duplicados.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS partido_equipos (
    id_partido_equipo BIGSERIAL PRIMARY KEY,
    id_partido BIGINT NOT NULL REFERENCES partidos(id_partido) ON DELETE CASCADE,
    id_equipo BIGINT NOT NULL REFERENCES equipos(id_equipo) ON DELETE RESTRICT,
    es_local BOOLEAN NOT NULL,
    goles INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_partido_equipos_goles_no_negativos CHECK (goles >= 0),
    CONSTRAINT uq_partido_equipo UNIQUE (id_partido, id_equipo),
    CONSTRAINT uq_partido_condicion_local UNIQUE (id_partido, es_local)
);

-- ----------------------------------------------------------------------------
-- 14. TABLA: cronologias
-- Justificacion: Incidencias del partido: goles, tarjetas y cambios (Cronologia.java / CU-04).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cronologias (
    id_cronologia BIGSERIAL PRIMARY KEY,
    id_partido BIGINT NOT NULL REFERENCES partidos(id_partido) ON DELETE CASCADE,
    id_jugador BIGINT NOT NULL REFERENCES jugadores(id_jugador) ON DELETE RESTRICT,
    minuto_partido INTEGER NOT NULL,
    momento_registro TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accion VARCHAR(30) NOT NULL,
    tipo_expulsion VARCHAR(20),
    observacion VARCHAR(255),
    CONSTRAINT chk_cronologia_minuto_no_negativo CHECK (minuto_partido >= 0),
    CONSTRAINT chk_cronologia_accion_valida CHECK (accion IN ('GOL', 'TARJETA_AMARILLA', 'TARJETA_ROJA', 'CAMBIO')),
    CONSTRAINT chk_cronologia_tipo_expulsion CHECK (
        (accion = 'TARJETA_ROJA' AND tipo_expulsion IS NOT NULL AND tipo_expulsion IN ('DIRECTA', 'DOBLE_AMARILLA')) OR
        (accion <> 'TARJETA_ROJA' AND tipo_expulsion IS NULL)
    )
);

-- ----------------------------------------------------------------------------
-- 15. TABLA: sanciones
-- Justificacion: Medidas disciplinarias aplicadas por tribunal a jugadores (Sancion.java / CU-04.1 / CU-12).
-- Incluye id_partido_origen e id_torneo, tipo_origen, fechas_cumplidas y ciclo de vida de estado.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sanciones (
    id_sancion BIGSERIAL PRIMARY KEY,
    id_jugador BIGINT NOT NULL REFERENCES jugadores(id_jugador) ON DELETE RESTRICT,
    id_partido_origen BIGINT REFERENCES partidos(id_partido) ON DELETE RESTRICT,
    id_torneo BIGINT REFERENCES torneos(id_torneo) ON DELETE RESTRICT,
    alcance VARCHAR(20) NOT NULL DEFAULT 'TORNEO',
    tipo_origen VARCHAR(30) NOT NULL DEFAULT 'ADMINISTRATIVA',
    motivo TEXT NOT NULL,
    gravedad VARCHAR(30) NOT NULL,
    partidos_suspension INTEGER NOT NULL DEFAULT 1,
    fechas_cumplidas INTEGER NOT NULL DEFAULT 0,
    fecha_inicio DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_fin DATE,
    fecha_publicacion_formal TIMESTAMP WITH TIME ZONE,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    CONSTRAINT chk_sancion_alcance CHECK (alcance IN ('TORNEO', 'GLOBAL')),
    CONSTRAINT chk_sancion_tipo_origen CHECK (tipo_origen IN ('ROJA_DIRECTA', 'DOBLE_AMARILLA', 'ACUMULACION_AMARILLAS', 'ADMINISTRATIVA')),
    CONSTRAINT chk_sancion_origen_partido CHECK (
        (tipo_origen IN ('ROJA_DIRECTA', 'DOBLE_AMARILLA', 'ACUMULACION_AMARILLAS') AND id_partido_origen IS NOT NULL AND id_torneo IS NOT NULL AND alcance = 'TORNEO')
        OR (tipo_origen = 'ADMINISTRATIVA')
    ),
    CONSTRAINT chk_sancion_alcance_torneo CHECK (
        (alcance = 'TORNEO' AND id_torneo IS NOT NULL)
        OR (alcance = 'GLOBAL' AND id_torneo IS NULL AND id_partido_origen IS NULL AND tipo_origen = 'ADMINISTRATIVA')
    ),
    CONSTRAINT chk_sancion_gravedad_no_vacia CHECK (length(trim(gravedad)) > 0),
    CONSTRAINT chk_sancion_motivo_longitud CHECK (length(trim(motivo)) >= 10),
    CONSTRAINT chk_sancion_partidos_minimo CHECK (partidos_suspension >= 1),
    CONSTRAINT chk_sancion_cumplidas_rango CHECK (fechas_cumplidas >= 0 AND fechas_cumplidas <= partidos_suspension),
    CONSTRAINT chk_sancion_fechas_coherentes CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio),
    CONSTRAINT chk_sancion_estado_valido CHECK (estado IN ('PENDIENTE', 'ACTIVA', 'CUMPLIDA', 'ANULADA')),
    CONSTRAINT chk_sancion_estado_fechas CHECK (
        (estado = 'PENDIENTE' AND fechas_cumplidas = 0) OR
        (estado = 'ACTIVA'    AND fechas_cumplidas < partidos_suspension) OR
        (estado = 'CUMPLIDA'  AND fechas_cumplidas = partidos_suspension) OR
        (estado = 'ANULADA'   AND fechas_cumplidas <= partidos_suspension)
    ),
    CONSTRAINT uq_sancion_evento_origen UNIQUE (id_jugador, id_torneo, id_partido_origen, tipo_origen)
);

-- ----------------------------------------------------------------------------
-- 16. TABLA: reclamos
-- Justificacion: Expedientes de reclamo iniciados por capitanes y resueltos por organizacion (Reclamo.java / CU-13 / CU-14).
-- Estados normalizados a PENDIENTE y RESUELTO, con politicas ON DELETE RESTRICT.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reclamos (
    id_reclamo BIGSERIAL PRIMARY KEY,
    id_sancion_apelada BIGINT REFERENCES sanciones(id_sancion) ON DELETE RESTRICT,
    id_partido BIGINT REFERENCES partidos(id_partido) ON DELETE RESTRICT,
    id_capitan BIGINT REFERENCES usuarios(id_usuario) ON DELETE RESTRICT,
    fecha_inicio TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_fin TIMESTAMP WITH TIME ZONE,
    motivo TEXT NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    veredicto VARCHAR(50),
    justificacion TEXT,
    CONSTRAINT chk_reclamo_fechas_coherentes CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio),
    CONSTRAINT chk_reclamo_motivo_longitud CHECK (length(trim(motivo)) >= 15),
    CONSTRAINT chk_reclamo_estado_valido CHECK (estado IN ('PENDIENTE', 'RESUELTO')),
    CONSTRAINT chk_reclamo_estado_resolucion CHECK (
        (estado = 'PENDIENTE' AND fecha_fin IS NULL AND veredicto IS NULL) OR
        (estado = 'RESUELTO' AND fecha_fin IS NOT NULL AND veredicto IS NOT NULL)
    )
);

-- ----------------------------------------------------------------------------
-- 17. TABLA: configuraciones
-- Justificacion: Parametros globales del sistema y reglas de negocio del torneo (Configuracion.java / CU-18).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS configuraciones (
    id_configuracion SERIAL PRIMARY KEY,
    clave VARCHAR(50) NOT NULL UNIQUE,
    valor INTEGER NOT NULL,
    descripcion VARCHAR(200),
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_config_clave_no_vacia CHECK (length(trim(clave)) > 0)
);

-- ============================================================================
-- INDICES PARA OPTIMIZACION DE CLAVES FORANEAS, IDEMPOTENCIA Y CONSULTAS
-- ============================================================================

-- Usuarios y Roles
CREATE INDEX IF NOT EXISTS idx_usuarios_rol ON usuarios(id_rol);

-- Equipos
CREATE INDEX IF NOT EXISTS idx_equipos_categoria ON equipos(id_categoria);
CREATE INDEX IF NOT EXISTS idx_equipos_sede_habitual ON equipos(id_sede_habitual);
CREATE INDEX IF NOT EXISTS idx_equipos_capitan ON equipos(id_capitan);

-- Jugadores
CREATE INDEX IF NOT EXISTS idx_jugadores_equipo ON jugadores(id_equipo);
CREATE INDEX IF NOT EXISTS idx_jugadores_estado ON jugadores(estado);

-- Solicitudes de inscripcion
CREATE INDEX IF NOT EXISTS idx_solicitudes_equipo ON solicitudes_inscripcion(id_equipo);

-- Partidos
CREATE INDEX IF NOT EXISTS idx_partidos_fase ON partidos(id_fase);
CREATE INDEX IF NOT EXISTS idx_partidos_cancha ON partidos(id_cancha);
CREATE INDEX IF NOT EXISTS idx_partidos_arbitro ON partidos(id_arbitro);
CREATE INDEX IF NOT EXISTS idx_partidos_fecha_hora ON partidos(fecha_hora);
CREATE INDEX IF NOT EXISTS idx_partidos_finalizacion_real ON partidos(fecha_hora_finalizacion_real);
CREATE INDEX IF NOT EXISTS idx_partidos_estado ON partidos(estado);

-- Asignacion Partido - Equipos
CREATE INDEX IF NOT EXISTS idx_partido_equipos_equipo ON partido_equipos(id_equipo);

-- Cronologia e Incidencias
CREATE INDEX IF NOT EXISTS idx_cronologias_partido ON cronologias(id_partido);
CREATE INDEX IF NOT EXISTS idx_cronologias_jugador ON cronologias(id_jugador);
CREATE INDEX IF NOT EXISTS idx_cronologias_accion ON cronologias(accion);

-- Sanciones
CREATE INDEX IF NOT EXISTS idx_sanciones_jugador ON sanciones(id_jugador);
CREATE INDEX IF NOT EXISTS idx_sanciones_torneo ON sanciones(id_torneo);
CREATE INDEX IF NOT EXISTS idx_sanciones_partido_origen ON sanciones(id_partido_origen);
CREATE INDEX IF NOT EXISTS idx_sanciones_estado ON sanciones(estado);

-- Indices parciales de idempotencia y unicidad para sanciones disciplinarias
CREATE UNIQUE INDEX IF NOT EXISTS uq_sancion_acumulacion_vigente 
ON sanciones(id_jugador, id_torneo) 
WHERE tipo_origen = 'ACUMULACION_AMARILLAS' AND estado IN ('PENDIENTE', 'ACTIVA');

CREATE UNIQUE INDEX IF NOT EXISTS uq_sancion_jugador_partido_origen_tipo
ON sanciones(id_jugador, id_partido_origen, tipo_origen)
WHERE id_partido_origen IS NOT NULL;

-- Reclamos
CREATE INDEX IF NOT EXISTS idx_reclamos_sancion ON reclamos(id_sancion_apelada);
CREATE INDEX IF NOT EXISTS idx_reclamos_partido ON reclamos(id_partido);
CREATE INDEX IF NOT EXISTS idx_reclamos_capitan ON reclamos(id_capitan);
CREATE INDEX IF NOT EXISTS idx_reclamos_estado ON reclamos(estado);

COMMIT;
