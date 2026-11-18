-- ============================================================================
-- MIGRACION ADITIVA Y COMPATIBLE: V002 — Soft delete y estados CRUD (Etapa Cierre)
-- Proyecto: TPI Programacion Avanzada II — Gestion de Torneo de Futbol Amateur
--
-- Reglas de seguridad:
--   - Idempotente y no destructiva (no elimina tablas ni registros historicos).
--   - Compatible con datos preexistentes: los equipos previos quedan activos (TRUE).
--   - Habilita estado 'CANCELADO' para baja logica de torneos preservando historial.
--
-- Reversion manual segura (solo si no existen torneos en estado 'CANCELADO'):
--   DROP INDEX IF EXISTS idx_equipos_activo;
--   ALTER TABLE equipos DROP COLUMN IF EXISTS activo;
--   ALTER TABLE torneos DROP CONSTRAINT IF EXISTS chk_torneo_estado_valido;
--   ALTER TABLE torneos ADD CONSTRAINT chk_torneo_estado_valido
--       CHECK (estado IN ('CONFIGURACION', 'EN_CURSO', 'FINALIZADO'));
-- ============================================================================

BEGIN;

ALTER TABLE equipos
    ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_equipos_activo ON equipos(activo);

ALTER TABLE torneos
    DROP CONSTRAINT IF EXISTS chk_torneo_estado_valido;

ALTER TABLE torneos
    ADD CONSTRAINT chk_torneo_estado_valido
        CHECK (estado IN ('CONFIGURACION', 'EN_CURSO', 'FINALIZADO', 'CANCELADO'));

COMMIT;
