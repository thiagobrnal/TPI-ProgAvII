package com.gestionamateur.tpi.progav.dao;

import com.gestionamateur.tpi.progav.modelo.Cronologia;
import com.gestionamateur.tpi.progav.modelo.Jugador;
import com.gestionamateur.tpi.progav.modelo.Partido;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class CronologiaDAO {

    private final JugadorDAO jugadorDAO = new JugadorDAO();

    public Long insertar(Cronologia crono, Long idPartido, Long idJugador) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return insertar(conn, crono, idPartido, idJugador);
        }
    }

    public Long insertar(Connection conn, Cronologia crono, Long idPartido, Long idJugador) throws SQLException {
        String sql = "INSERT INTO cronologias (id_partido, id_jugador, minuto_partido, accion, tipo_expulsion, observacion) "
                   + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id_cronologia";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idPartido != null ? idPartido : crono.getPartido().getIdPartido());
            ps.setLong(2, idJugador != null ? idJugador : crono.getAutor().getId());
            ps.setInt(3, crono.getMinutoPartido() != null ? crono.getMinutoPartido() : 0);
            ps.setString(4, crono.getAccion().name());
            if (crono.getAccion() == TiposDatos.TipoAccion.TARJETA_ROJA && crono.getTipoExpulsion() != null) {
                ps.setString(5, crono.getTipoExpulsion());
            } else {
                ps.setNull(5, Types.VARCHAR);
            }
            ps.setString(6, crono.getObservacion());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    crono.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Error al insertar cronologia: no se generó ID.");
    }

    public List<Cronologia> buscarPorPartido(Long idPartido) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarPorPartido(conn, idPartido);
        }
    }

    public List<Cronologia> buscarPorPartido(Connection conn, Long idPartido) throws SQLException {
        String sql = "SELECT id_cronologia, id_partido, id_jugador, minuto_partido, accion, "
                   + "tipo_expulsion, observacion, momento_registro "
                   + "FROM cronologias WHERE id_partido = ? ORDER BY minuto_partido ASC, id_cronologia ASC";
        List<Cronologia> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idPartido);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearCronologia(conn, rs));
                }
            }
        }
        return lista;
    }

    public List<Cronologia> buscarPorJugador(Connection conn, Long idJugador) throws SQLException {
        String sql = "SELECT id_cronologia, id_partido, id_jugador, minuto_partido, accion, "
                   + "tipo_expulsion, observacion, momento_registro "
                   + "FROM cronologias WHERE id_jugador = ? ORDER BY momento_registro DESC";
        List<Cronologia> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idJugador);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearCronologia(conn, rs));
                }
            }
        }
        return lista;
    }

    /**
     * Variante A: Contabiliza tarjetas amarillas acumuladas en la ventana del torneo.
     * Excluye partidos donde el jugador fue expulsado por DOBLE_AMARILLA (ya que esa sanción extingue dichas 2 amarillas).
     */
    public int contarAmarillasVarianteA(Connection conn, Long idJugador, Long idTorneo, int ordenInicioVentana) throws SQLException {
        String sql = "SELECT COUNT(*) "
                   + "FROM cronologias c "
                   + "JOIN partidos p ON c.id_partido = p.id_partido "
                   + "JOIN fases f ON p.id_fase = f.id_fase "
                   + "WHERE c.id_jugador = ? "
                   + "  AND f.id_torneo = ? "
                   + "  AND f.orden >= ? "
                   + "  AND c.accion = 'TARJETA_AMARILLA' "
                   + "  AND c.id_partido NOT IN ( "
                   + "      SELECT c2.id_partido "
                   + "      FROM cronologias c2 "
                   + "      WHERE c2.id_jugador = c.id_jugador "
                   + "        AND c2.accion = 'TARJETA_ROJA' "
                   + "        AND c2.tipo_expulsion = 'DOBLE_AMARILLA' "
                   + "  )";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idJugador);
            ps.setLong(2, idTorneo);
            ps.setInt(3, ordenInicioVentana);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public int contarAmarillasEnPartido(Connection conn, Long idPartido, Long idJugador) throws SQLException {
        String sql = "SELECT COUNT(*) FROM cronologias WHERE id_partido = ? AND id_jugador = ? AND accion = 'TARJETA_AMARILLA'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idPartido);
            ps.setLong(2, idJugador);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public boolean huboDobleAmarillaEnPartido(Connection conn, Long idPartido, Long idJugador) throws SQLException {
        String sql = "SELECT 1 FROM cronologias WHERE id_partido = ? AND id_jugador = ? "
                   + "AND accion = 'TARJETA_ROJA' AND tipo_expulsion = 'DOBLE_AMARILLA'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idPartido);
            ps.setLong(2, idJugador);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean huboRojaDirectaEnPartido(Connection conn, Long idPartido, Long idJugador) throws SQLException {
        String sql = "SELECT 1 FROM cronologias WHERE id_partido = ? AND id_jugador = ? "
                   + "AND accion = 'TARJETA_ROJA' AND tipo_expulsion = 'DIRECTA'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idPartido);
            ps.setLong(2, idJugador);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int contarGolesPorEquipo(Connection conn, Long idPartido, Long idEquipo) throws SQLException {
        String sql = "SELECT COUNT(*) FROM cronologias c "
                   + "JOIN jugadores j ON c.id_jugador = j.id_jugador "
                   + "WHERE c.id_partido = ? AND j.id_equipo = ? AND c.accion = 'GOL'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idPartido);
            ps.setLong(2, idEquipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private Cronologia mapearCronologia(Connection conn, ResultSet rs) throws SQLException {
        try {
            Long id = rs.getLong("id_cronologia");
            Long idPartido = rs.getLong("id_partido");
            Long idJugador = rs.getLong("id_jugador");
            int minutoPartido = rs.getInt("minuto_partido");
            String accionStr = rs.getString("accion");
            String tipoExpulsion = rs.getString("tipo_expulsion");
            String obs = rs.getString("observacion");

            TiposDatos.TipoAccion accion = TiposDatos.TipoAccion.valueOf(accionStr);
            Jugador jugador = jugadorDAO.buscarPorId(conn, idJugador);
            Partido partido = new Partido();
            partido.setIdPartido(idPartido);

            Cronologia c = new Cronologia(minutoPartido, accion, tipoExpulsion, jugador, partido, obs);
            c.setId(id);
            return c;
        } catch (TorneoExcepcion e) {
            throw new SQLException("Error de validación al mapear Cronologia: " + e.getMessage(), e);
        }
    }
}
