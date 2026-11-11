package com.gestionamateur.tpi.progav.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de reportes de Release 1 (CU-19 a CU-22).
 *
 * Contiene unicamente consultas agregadas (GROUP BY) que calculan los datos
 * crudos de los cuatro reportes obligatorios. Cada metodo ofrece dos variantes,
 * siguiendo la convencion del proyecto: una que abre su propia conexion y otra
 * que recibe una Connection ya abierta para poder componerse en una transaccion.
 *
 * Los DAOs no tienen estado: se instancian con new ReporteDAO().
 */
public class ReporteDAO {

    /** Estado de partido que se considera disputado para el calculo de resultados. */
    private static final String ESTADO_FINALIZADO = "FINALIZADO";
    /** Estados de sancion considerados vigentes (mismo criterio que SancionDAO). */
    private static final String ESTADO_SANCION_PENDIENTE = "PENDIENTE";
    private static final String ESTADO_SANCION_ACTIVA = "ACTIVA";
    /** Texto mostrado cuando el jugador no tiene equipo asignado. */
    private static final String SIN_EQUIPO = "Sin equipo";

    // ------------------------------------------------------------------
    // CU-19: tabla de posiciones
    // ------------------------------------------------------------------

    /**
     * Tabla de posiciones: un renglon por equipo con partidos jugados, ganados,
     * empatados, perdidos, goles a favor, goles en contra, diferencia de goles y
     * puntos. Solo se consideran partidos con estado FINALIZADO.
     *
     * @return filas con el orden de columnas: equipo, PJ, PG, PE, PP, GF, GC, DG, puntos
     */
    public List<Object[]> tablaPosiciones() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return tablaPosiciones(conn);
        }
    }

    /**
     * Variante que reutiliza una conexion abierta.
     *
     * @return filas con el orden de columnas: equipo, PJ, PG, PE, PP, GF, GC, DG, puntos
     */
    public List<Object[]> tablaPosiciones(Connection conn) throws SQLException {
        String sql = "SELECT e.nombre AS equipo, "
                   + "       COUNT(p.id_partido) AS partidos_jugados, "
                   + "       SUM(CASE WHEN pe.goles > peo.goles THEN 1 ELSE 0 END) AS ganados, "
                   + "       SUM(CASE WHEN pe.goles = peo.goles THEN 1 ELSE 0 END) AS empatados, "
                   + "       SUM(CASE WHEN pe.goles < peo.goles THEN 1 ELSE 0 END) AS perdidos, "
                   + "       SUM(pe.goles) AS goles_favor, "
                   + "       SUM(peo.goles) AS goles_contra, "
                   + "       SUM(pe.goles) - SUM(peo.goles) AS diferencia_goles, "
                   + "       SUM(CASE WHEN pe.goles > peo.goles THEN 3 "
                   + "                WHEN pe.goles = peo.goles THEN 1 ELSE 0 END) AS puntos "
                   + "FROM partidos p "
                   + "JOIN partido_equipos pe ON pe.id_partido = p.id_partido "
                   + "JOIN partido_equipos peo ON peo.id_partido = pe.id_partido "
                   + "                  AND peo.id_partido_equipo <> pe.id_partido_equipo "
                   + "JOIN equipos e ON e.id_equipo = pe.id_equipo "
                   + "WHERE p.estado = ? "
                   + "GROUP BY e.id_equipo, e.nombre "
                   + "ORDER BY SUM(CASE WHEN pe.goles > peo.goles THEN 3 "
                   + "                   WHEN pe.goles = peo.goles THEN 1 ELSE 0 END) DESC, "
                   + "         SUM(pe.goles) - SUM(peo.goles) DESC, "
                   + "         SUM(pe.goles) DESC, "
                   + "         e.nombre ASC";
        List<Object[]> filas = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ESTADO_FINALIZADO);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new Object[]{
                            rs.getString("equipo"),
                            rs.getInt("partidos_jugados"),
                            rs.getInt("ganados"),
                            rs.getInt("empatados"),
                            rs.getInt("perdidos"),
                            rs.getInt("goles_favor"),
                            rs.getInt("goles_contra"),
                            rs.getInt("diferencia_goles"),
                            rs.getInt("puntos")
                    });
                }
            }
        }
        return filas;
    }

    // ------------------------------------------------------------------
    // CU-20: reporte de jugadores (goleadores, tarjetas y sancionados)
    // ------------------------------------------------------------------

    /**
     * Reporte de jugadores: por cada jugador con registros, informa equipo,
     * goles anotados, tarjetas amarillas, tarjetas rojas y sanciones vigentes.
     *
     * @return filas con el orden de columnas: jugador, equipo, goles, amarillas, rojas, sanciones
     */
    public List<Object[]> reporteJugadores() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return reporteJugadores(conn);
        }
    }

    /**
     * Variante que reutiliza una conexion abierta.
     *
     * @return filas con el orden de columnas: jugador, equipo, goles, amarillas, rojas, sanciones
     */
    public List<Object[]> reporteJugadores(Connection conn) throws SQLException {
        String sql = "WITH resumen_cronologias AS ( "
                   + "    SELECT c.id_jugador, "
                   + "           SUM(CASE WHEN c.accion = 'GOL' THEN 1 ELSE 0 END) AS goles, "
                   + "           SUM(CASE WHEN c.accion = 'TARJETA_AMARILLA' THEN 1 ELSE 0 END) AS amarillas, "
                   + "           SUM(CASE WHEN c.accion = 'TARJETA_ROJA' THEN 1 ELSE 0 END) AS rojas "
                   + "    FROM cronologias c "
                   + "    GROUP BY c.id_jugador "
                   + "), sanciones_vigentes AS ( "
                   + "    SELECT s.id_jugador, COUNT(*) AS sanciones "
                   + "    FROM sanciones s "
                   + "    WHERE s.estado IN (?, ?) "
                   + "    GROUP BY s.id_jugador "
                   + ") "
                   + "SELECT j.nombre AS jugador, "
                   + "       COALESCE(e.nombre, ?) AS equipo, "
                   + "       COALESCE(r.goles, 0) AS goles, "
                   + "       COALESCE(r.amarillas, 0) AS amarillas, "
                   + "       COALESCE(r.rojas, 0) AS rojas, "
                   + "       COALESCE(s.sanciones, 0) AS sanciones "
                   + "FROM jugadores j "
                   + "LEFT JOIN equipos e ON e.id_equipo = j.id_equipo "
                   + "LEFT JOIN resumen_cronologias r ON r.id_jugador = j.id_jugador "
                   + "LEFT JOIN sanciones_vigentes s ON s.id_jugador = j.id_jugador "
                   + "WHERE r.id_jugador IS NOT NULL OR s.id_jugador IS NOT NULL "
                   + "ORDER BY COALESCE(r.goles, 0) DESC, "
                   + "         COALESCE(r.amarillas, 0) DESC, "
                   + "         COALESCE(r.rojas, 0) DESC, "
                   + "         j.nombre ASC";
        List<Object[]> filas = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ESTADO_SANCION_PENDIENTE);
            ps.setString(2, ESTADO_SANCION_ACTIVA);
            ps.setString(3, SIN_EQUIPO);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new Object[]{
                            rs.getString("jugador"),
                            rs.getString("equipo"),
                            rs.getInt("goles"),
                            rs.getInt("amarillas"),
                            rs.getInt("rojas"),
                            rs.getInt("sanciones")
                    });
                }
            }
        }
        return filas;
    }

    // ------------------------------------------------------------------
    // CU-21: historial deportivo por equipo
    // ------------------------------------------------------------------

    /**
     * Historial deportivo por equipo: torqueos en los que participa, partidos
     * jugados, ganados, empatados, perdidos y goles a favor y en contra.
     * Los resultados se cuentan sobre partidos FINALIZADOS; la participacion en
     * torneos se cuenta sobre todos los partidos del equipo.
     *
     * @return filas con el orden de columnas: equipo, torneos, PJ, PG, PE, PP, GF, GC
     */
    public List<Object[]> historialEquipos() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return historialEquipos(conn);
        }
    }

    /**
     * Variante que reutiliza una conexion abierta.
     *
     * @return filas con el orden de columnas: equipo, torneos, PJ, PG, PE, PP, GF, GC
     */
    public List<Object[]> historialEquipos(Connection conn) throws SQLException {
        String sql = "WITH participacion AS ( "
                   + "    SELECT pe.id_equipo, COUNT(DISTINCT f.id_torneo) AS torneos "
                   + "    FROM partido_equipos pe "
                   + "    JOIN partidos p ON p.id_partido = pe.id_partido "
                   + "    JOIN fases f ON f.id_fase = p.id_fase "
                   + "    GROUP BY pe.id_equipo "
                   + "), resultados AS ( "
                   + "    SELECT pe.id_equipo, "
                   + "           COUNT(p.id_partido) AS partidos_jugados, "
                   + "           SUM(CASE WHEN pe.goles > peo.goles THEN 1 ELSE 0 END) AS ganados, "
                   + "           SUM(CASE WHEN pe.goles = peo.goles THEN 1 ELSE 0 END) AS empatados, "
                   + "           SUM(CASE WHEN pe.goles < peo.goles THEN 1 ELSE 0 END) AS perdidos, "
                   + "           SUM(pe.goles) AS goles_favor, "
                   + "           SUM(peo.goles) AS goles_contra "
                   + "    FROM partidos p "
                   + "    JOIN partido_equipos pe ON pe.id_partido = p.id_partido "
                   + "    JOIN partido_equipos peo ON peo.id_partido = pe.id_partido "
                   + "                      AND peo.id_partido_equipo <> pe.id_partido_equipo "
                   + "    WHERE p.estado = ? "
                   + "    GROUP BY pe.id_equipo "
                   + ") "
                   + "SELECT e.nombre AS equipo, "
                   + "       COALESCE(pa.torneos, 0) AS torneos, "
                   + "       COALESCE(r.partidos_jugados, 0) AS partidos_jugados, "
                   + "       COALESCE(r.ganados, 0) AS ganados, "
                   + "       COALESCE(r.empatados, 0) AS empatados, "
                   + "       COALESCE(r.perdidos, 0) AS perdidos, "
                   + "       COALESCE(r.goles_favor, 0) AS goles_favor, "
                   + "       COALESCE(r.goles_contra, 0) AS goles_contra "
                   + "FROM equipos e "
                   + "LEFT JOIN resultados r ON r.id_equipo = e.id_equipo "
                   + "LEFT JOIN participacion pa ON pa.id_equipo = e.id_equipo "
                   + "ORDER BY e.nombre ASC";
        List<Object[]> filas = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ESTADO_FINALIZADO);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new Object[]{
                            rs.getString("equipo"),
                            rs.getInt("torneos"),
                            rs.getInt("partidos_jugados"),
                            rs.getInt("ganados"),
                            rs.getInt("empatados"),
                            rs.getInt("perdidos"),
                            rs.getInt("goles_favor"),
                            rs.getInt("goles_contra")
                    });
                }
            }
        }
        return filas;
    }

    // ------------------------------------------------------------------
    // CU-21: historial deportivo por jugador
    // ------------------------------------------------------------------

    /**
     * Historial deportivo por jugador: equipo, partidos en los que registro
     * incidencia, goles, tarjetas amarillas, tarjetas rojas y sanciones vigentes.
     *
     * @return filas con el orden de columnas: jugador, equipo, partidos, goles, amarillas, rojas, sanciones
     */
    public List<Object[]> historialJugadores() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return historialJugadores(conn);
        }
    }

    /**
     * Variante que reutiliza una conexion abierta.
     *
     * @return filas con el orden de columnas: jugador, equipo, partidos, goles, amarillas, rojas, sanciones
     */
    public List<Object[]> historialJugadores(Connection conn) throws SQLException {
        String sql = "WITH participacion AS ( "
                   + "    SELECT c.id_jugador, COUNT(DISTINCT c.id_partido) AS partidos "
                   + "    FROM cronologias c "
                   + "    GROUP BY c.id_jugador "
                   + "), resumen_cronologias AS ( "
                   + "    SELECT c.id_jugador, "
                   + "           SUM(CASE WHEN c.accion = 'GOL' THEN 1 ELSE 0 END) AS goles, "
                   + "           SUM(CASE WHEN c.accion = 'TARJETA_AMARILLA' THEN 1 ELSE 0 END) AS amarillas, "
                   + "           SUM(CASE WHEN c.accion = 'TARJETA_ROJA' THEN 1 ELSE 0 END) AS rojas "
                   + "    FROM cronologias c "
                   + "    GROUP BY c.id_jugador "
                   + "), sanciones_vigentes AS ( "
                   + "    SELECT s.id_jugador, COUNT(*) AS sanciones "
                   + "    FROM sanciones s "
                   + "    WHERE s.estado IN (?, ?) "
                   + "    GROUP BY s.id_jugador "
                   + ") "
                   + "SELECT j.nombre AS jugador, "
                   + "       COALESCE(e.nombre, ?) AS equipo, "
                   + "       COALESCE(p.partidos, 0) AS partidos, "
                   + "       COALESCE(r.goles, 0) AS goles, "
                   + "       COALESCE(r.amarillas, 0) AS amarillas, "
                   + "       COALESCE(r.rojas, 0) AS rojas, "
                   + "       COALESCE(s.sanciones, 0) AS sanciones "
                   + "FROM jugadores j "
                   + "LEFT JOIN equipos e ON e.id_equipo = j.id_equipo "
                   + "LEFT JOIN participacion p ON p.id_jugador = j.id_jugador "
                   + "LEFT JOIN resumen_cronologias r ON r.id_jugador = j.id_jugador "
                   + "LEFT JOIN sanciones_vigentes s ON s.id_jugador = j.id_jugador "
                   + "ORDER BY j.nombre ASC";
        List<Object[]> filas = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ESTADO_SANCION_PENDIENTE);
            ps.setString(2, ESTADO_SANCION_ACTIVA);
            ps.setString(3, SIN_EQUIPO);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new Object[]{
                            rs.getString("jugador"),
                            rs.getString("equipo"),
                            rs.getInt("partidos"),
                            rs.getInt("goles"),
                            rs.getInt("amarillas"),
                            rs.getInt("rojas"),
                            rs.getInt("sanciones")
                    });
                }
            }
        }
        return filas;
    }

    // ------------------------------------------------------------------
    // CU-22: reporte de arbitros
    // ------------------------------------------------------------------

    /**
     * Reporte de arbitros: nombre y apellido, categoria arbitral, cantidad de
     * partidos dirigidos e incidencias registradas (filas de cronologias de esos
     * partidos). Se incluyen tambien los arbitros sin partidos asignados.
     *
     * @return filas con el orden de columnas: arbitro, categoria, partidos dirigidos, incidencias
     */
    public List<Object[]> reporteArbitros() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return reporteArbitros(conn);
        }
    }

    /**
     * Variante que reutiliza una conexion abierta.
     *
     * @return filas con el orden de columnas: arbitro, categoria, partidos dirigidos, incidencias
     */
    public List<Object[]> reporteArbitros(Connection conn) throws SQLException {
        String sql = "SELECT u.nombre || ' ' || u.apellido AS arbitro, "
                   + "       COALESCE(a.categoria_arbitral, ?) AS categoria, "
                   + "       COUNT(DISTINCT p.id_partido) AS partidos_dirigidos, "
                   + "       COUNT(c.id_cronologia) AS incidencias "
                   + "FROM arbitros a "
                   + "JOIN usuarios u ON u.id_usuario = a.id_usuario "
                   + "LEFT JOIN partidos p ON p.id_arbitro = u.id_usuario "
                   + "LEFT JOIN cronologias c ON c.id_partido = p.id_partido "
                   + "WHERE u.activo = ? "
                   + "GROUP BY u.id_usuario, u.nombre, u.apellido, a.categoria_arbitral "
                   + "ORDER BY COUNT(DISTINCT p.id_partido) DESC, "
                   + "         u.apellido ASC, u.nombre ASC";
        List<Object[]> filas = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "Sin categoria");
            ps.setBoolean(2, true);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new Object[]{
                            rs.getString("arbitro"),
                            rs.getString("categoria"),
                            rs.getInt("partidos_dirigidos"),
                            rs.getInt("incidencias")
                    });
                }
            }
        }
        return filas;
    }
}
