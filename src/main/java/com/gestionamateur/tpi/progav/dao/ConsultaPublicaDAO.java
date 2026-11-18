package com.gestionamateur.tpi.progav.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Consultas de solo lectura para el acceso publico (CU-08 a CU-11).
 *
 * <p>Este DAO no expone datos personales: no selecciona DNIs, correos electronicos, hashes de
 * contrasena, estados de salud (LESIONADO / INHABILITADO) ni informacion disciplinaria
 * reservada (sanciones, reclamos y expedientes del tribunal).
 */
public class ConsultaPublicaDAO {

    /** CU-08: fixture del torneo con equipos, sede, horario y resultado. */
    public List<Object[]> fixtureDeTorneo(Connection conn, Long idTorneo) throws SQLException {
        String sql = "SELECT to_char(p.fecha_hora, 'DD/MM/YYYY HH24:MI') AS fecha_hora, "
                   + "       p.estado, "
                   + "       el.nombre AS equipo_local, "
                   + "       ev.nombre AS equipo_visitante, "
                   + "       COALESCE(pel.goles, 0) || ' - ' || COALESCE(pev.goles, 0) AS resultado, "
                   + "       c.nombre AS sede, "
                   + "       f.nombre AS fase, "
                   + "       t.nombre AS torneo "
                   + "FROM partidos p "
                   + "LEFT JOIN partido_equipos pel ON pel.id_partido = p.id_partido AND pel.es_local = TRUE "
                   + "LEFT JOIN partido_equipos pev ON pev.id_partido = p.id_partido AND pev.es_local = FALSE "
                   + "LEFT JOIN equipos el ON el.id_equipo = pel.id_equipo "
                   + "LEFT JOIN equipos ev ON ev.id_equipo = pev.id_equipo "
                   + "LEFT JOIN canchas c ON c.id_cancha = p.id_cancha "
                   + "LEFT JOIN fases f ON f.id_fase = p.id_fase "
                   + "LEFT JOIN torneos t ON t.id_torneo = f.id_torneo "
                   + "WHERE (p.id_fase IN (SELECT id_fase FROM fases WHERE id_torneo = ?) OR ? IS NULL) "
                   + "ORDER BY p.fecha_hora ASC, p.id_partido ASC";
        return consultar(conn, sql, idTorneo, idTorneo);
    }

    /** CU-09: estadisticas publicas consolidadas de un equipo. */
    public List<Object[]> estadisticasDeEquipo(Connection conn, Long idEquipo) throws SQLException {
        String sql = "SELECT e.nombre, "
                   + "       c.nombre AS categoria, "
                   + "       COUNT(p.id_partido) AS partidos_jugados, "
                   + "       COALESCE(SUM(CASE WHEN pe.goles > peo.goles THEN 1 ELSE 0 END), 0) AS ganados, "
                   + "       COALESCE(SUM(CASE WHEN pe.goles = peo.goles THEN 1 ELSE 0 END), 0) AS empatados, "
                   + "       COALESCE(SUM(CASE WHEN pe.goles < peo.goles THEN 1 ELSE 0 END), 0) AS perdidos, "
                   + "       COALESCE(SUM(pe.goles), 0) AS goles_favor, "
                   + "       COALESCE(SUM(peo.goles), 0) AS goles_contra, "
                   + "       (SELECT count(*) FROM jugadores j WHERE j.id_equipo = e.id_equipo) AS jugadores_plantel "
                   + "FROM equipos e "
                   + "LEFT JOIN categorias c ON c.id_categoria = e.id_categoria "
                   + "LEFT JOIN partido_equipos pe ON pe.id_equipo = e.id_equipo "
                   + "LEFT JOIN partidos p ON p.id_partido = pe.id_partido AND p.estado = 'FINALIZADO' "
                   + "LEFT JOIN partido_equipos peo ON peo.id_partido = pe.id_partido "
                   + "                           AND peo.id_partido_equipo <> pe.id_partido_equipo "
                   + "WHERE e.id_equipo = ? "
                   + "GROUP BY e.id_equipo, e.nombre, c.nombre";
        return consultar(conn, sql, idEquipo);
    }

    /** CU-10: estadisticas publicas de los jugadores (goles, tarjetas y partidos con incidencia). */
    public List<Object[]> estadisticasDeJugadores(Connection conn, Long idTorneo) throws SQLException {
        String sql = "SELECT j.nombre, "
                   + "       COALESCE(e.nombre, 'Sin equipo') AS equipo, "
                   + "       COALESCE(r.goles, 0) AS goles, "
                   + "       COALESCE(r.amarillas, 0) AS tarjetas_amarillas, "
                   + "       COALESCE(r.rojas, 0) AS tarjetas_rojas, "
                   + "       COALESCE(p.partidos, 0) AS partidos_con_incidencia "
                   + "FROM jugadores j "
                   + "LEFT JOIN equipos e ON e.id_equipo = j.id_equipo "
                   + "LEFT JOIN (SELECT c.id_jugador, "
                   + "                  COUNT(*) FILTER (WHERE c.accion = 'GOL') AS goles, "
                   + "                  COUNT(*) FILTER (WHERE c.accion = 'TARJETA_AMARILLA') AS amarillas, "
                   + "                  COUNT(*) FILTER (WHERE c.accion = 'TARJETA_ROJA') AS rojas, "
                   + "                  COUNT(DISTINCT c.id_partido) AS partidos "
                   + "             FROM cronologias c "
                   + "             JOIN partidos p2 ON p2.id_partido = c.id_partido "
                   + "             WHERE (? IS NULL OR p2.id_fase IN (SELECT id_fase FROM fases WHERE id_torneo = ?)) "
                   + "             GROUP BY c.id_jugador) r ON r.id_jugador = j.id_jugador "
                   + "LEFT JOIN (SELECT c2.id_jugador, COUNT(DISTINCT c2.id_partido) AS partidos "
                   + "             FROM cronologias c2 GROUP BY c2.id_jugador) p ON p.id_jugador = j.id_jugador "
                   + "WHERE r.id_jugador IS NOT NULL OR p.id_jugador IS NOT NULL "
                   + "ORDER BY goles DESC, tarjetas_amarillas DESC, tarjetas_rojas DESC, j.nombre ASC";
        return consultar(conn, sql, idTorneo, idTorneo);
    }

    /** CU-11: avance de fases del torneo con estado y equipos participantes. */
    public List<Object[]> avanceDeFases(Connection conn, Long idTorneo) throws SQLException {
        String sql = "SELECT t.nombre AS torneo, "
                   + "       t.estado AS estado_torneo, "
                   + "       f.nombre AS fase, "
                   + "       f.orden, "
                   + "       CASE WHEN f.limpia_amarillas THEN 'Si' ELSE 'No' END AS limpia_amarillas, "
                   + "       (SELECT count(*) FROM partidos p WHERE p.id_fase = f.id_fase) AS partidos_programados, "
                   + "       (SELECT count(*) FROM partidos p WHERE p.id_fase = f.id_fase AND p.estado = 'FINALIZADO') AS partidos_finalizados "
                   + "FROM torneos t "
                   + "JOIN fases f ON f.id_torneo = t.id_torneo "
                   + "WHERE (? IS NULL OR t.id_torneo = ?) "
                   + "ORDER BY t.nombre ASC, f.orden ASC";
        return consultar(conn, sql, idTorneo, idTorneo);
    }

    /** Lista de torneos con su estado, para el selector del acceso publico. */
    public List<Object[]> torneos(Connection conn) throws SQLException {
        String sql = "SELECT id_torneo, nombre, estado, formato, cupo_equipos "
                   + "FROM torneos WHERE estado <> 'CANCELADO' ORDER BY nombre ASC";
        return consultar(conn, sql);
    }

    /** Lista de equipos con su identificador y nombre, para el selector del acceso publico. */
    public List<Object[]> equipos(Connection conn) throws SQLException {
        String sql = "SELECT e.id_equipo, e.nombre, c.nombre AS categoria "
                   + "FROM equipos e LEFT JOIN categorias c ON c.id_categoria = e.id_categoria "
                   + "ORDER BY e.nombre ASC";
        return consultar(conn, sql);
    }

    private List<Object[]> consultar(Connection conn, String sql, Object... params) throws SQLException {
        List<Object[]> filas = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                // Los parametros de estas consultas son identificadores (BIGINT). Se tipifican
                // explicitamente para que PostgreSQL pueda resolver el tipo tambien cuando el
                // valor es NULL (patron "? IS NULL OR ...").
                ps.setObject(i + 1, params[i], java.sql.Types.BIGINT);
            }
            try (ResultSet rs = ps.executeQuery()) {
                int columnas = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    Object[] fila = new Object[columnas];
                    for (int c = 1; c <= columnas; c++) {
                        fila[c - 1] = rs.getObject(c);
                    }
                    filas.add(fila);
                }
            }
        }
        return filas;
    }
}
