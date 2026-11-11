package com.gestionamateur.tpi.progav.dao;

import com.gestionamateur.tpi.progav.modelo.Jugador;
import com.gestionamateur.tpi.progav.modelo.Partido;
import com.gestionamateur.tpi.progav.modelo.Sancion;
import com.gestionamateur.tpi.progav.modelo.Torneo;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class SancionDAO {

    private final JugadorDAO jugadorDAO = new JugadorDAO();
    private final TorneoDAO torneoDAO = new TorneoDAO();

    public Long insertar(Sancion sancion) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return insertar(conn, sancion);
        }
    }

    public Long insertar(Connection conn, Sancion sancion) throws SQLException {
        String sql = "INSERT INTO sanciones (id_jugador, id_partido_origen, id_torneo, alcance, tipo_origen, "
                   + "motivo, gravedad, partidos_suspension, fechas_cumplidas, fecha_inicio, fecha_fin, fecha_publicacion_formal, estado) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id_sancion";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            configurarPreparedStatement(ps, sancion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    sancion.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Error al insertar sancion: no se generó ID.");
    }

    /**
     * Inserción idempotente estricta con ON CONFLICT DO NOTHING sobre uq_sancion_evento_origen.
     * Si la sanción ya existe para el evento deportivo de origen, no se altera ningún dato previo.
     * Retorna el ID existente o el recién autogenerado.
     */
    public Long insertarIdempotente(Connection conn, Sancion sancion) throws SQLException {
        String sql = "INSERT INTO sanciones (id_jugador, id_partido_origen, id_torneo, alcance, tipo_origen, "
                   + "motivo, gravedad, partidos_suspension, fechas_cumplidas, fecha_inicio, fecha_fin, fecha_publicacion_formal, estado) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                   + "ON CONFLICT (id_jugador, id_torneo, id_partido_origen, tipo_origen) DO NOTHING "
                   + "RETURNING id_sancion";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            configurarPreparedStatement(ps, sancion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    sancion.setId(id);
                    return id;
                }
            }
        }
        // Conflicto detectado (DO NOTHING): recuperar el ID existente sin mutaciones
        Long idExistente = buscarIdPorEventoOrigen(conn,
                sancion.getJugadorSancionado().getId(),
                sancion.getTorneo() != null ? sancion.getTorneo().getIdTorneo() : null,
                sancion.getPartidoOrigen() != null ? sancion.getPartidoOrigen().getIdPartido() : null,
                sancion.getTipoOrigen());
        if (idExistente != null) {
            sancion.setId(idExistente);
            return idExistente;
        }
        throw new SQLException("Conflicto de idempotencia en sanción, pero no se pudo recuperar el ID existente.");
    }

    public Long buscarIdPorEventoOrigen(Connection conn, Long idJugador, Long idTorneo, Long idPartidoOrigen, String tipoOrigen) throws SQLException {
        String sql = "SELECT id_sancion FROM sanciones WHERE id_jugador = ? AND id_torneo = ? AND id_partido_origen = ? AND tipo_origen = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idJugador);
            ps.setLong(2, idTorneo);
            ps.setLong(3, idPartidoOrigen);
            ps.setString(4, tipoOrigen);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return null;
    }

    public boolean existeSancionEventoOrigen(Connection conn, Long idJugador, Long idTorneo, Long idPartidoOrigen, String tipoOrigen) throws SQLException {
        return buscarIdPorEventoOrigen(conn, idJugador, idTorneo, idPartidoOrigen, tipoOrigen) != null;
    }

    public Sancion buscarPorId(Long idSancion) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarPorId(conn, idSancion);
        }
    }

    public Sancion buscarPorId(Connection conn, Long idSancion) throws SQLException {
        String sql = "SELECT id_sancion, id_jugador, id_partido_origen, id_torneo, alcance, tipo_origen, "
                   + "motivo, gravedad, partidos_suspension, fechas_cumplidas, fecha_inicio, fecha_fin, fecha_publicacion_formal, estado "
                   + "FROM sanciones WHERE id_sancion = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idSancion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearSancion(conn, rs);
                }
            }
        }
        return null;
    }

    public List<Sancion> buscarPorJugador(Connection conn, Long idJugador) throws SQLException {
        String sql = "SELECT id_sancion, id_jugador, id_partido_origen, id_torneo, alcance, tipo_origen, "
                   + "motivo, gravedad, partidos_suspension, fechas_cumplidas, fecha_inicio, fecha_fin, fecha_publicacion_formal, estado "
                   + "FROM sanciones WHERE id_jugador = ? ORDER BY fecha_inicio DESC";
        List<Sancion> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idJugador);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearSancion(conn, rs));
                }
            }
        }
        return lista;
    }

    public List<Sancion> buscarActivasPorJugador(Connection conn, Long idJugador, Long idTorneo) throws SQLException {
        String sql = "SELECT id_sancion, id_jugador, id_partido_origen, id_torneo, alcance, tipo_origen, "
                   + "motivo, gravedad, partidos_suspension, fechas_cumplidas, fecha_inicio, fecha_fin, fecha_publicacion_formal, estado "
                   + "FROM sanciones "
                   + "WHERE id_jugador = ? "
                   + "  AND estado IN ('PENDIENTE', 'ACTIVA') "
                   + "  AND (alcance = 'GLOBAL' OR id_torneo = ?) "
                   + "ORDER BY fecha_inicio ASC";
        List<Sancion> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idJugador);
            if (idTorneo != null) {
                ps.setLong(2, idTorneo);
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearSancion(conn, rs));
                }
            }
        }
        return lista;
    }

    public List<Sancion> buscarPendientesTribunal(Connection conn) throws SQLException {
        String sql = "SELECT id_sancion, id_jugador, id_partido_origen, id_torneo, alcance, tipo_origen, "
                   + "motivo, gravedad, partidos_suspension, fechas_cumplidas, fecha_inicio, fecha_fin, fecha_publicacion_formal, estado "
                   + "FROM sanciones WHERE estado = 'PENDIENTE' ORDER BY fecha_inicio ASC";
        List<Sancion> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearSancion(conn, rs));
            }
        }
        return lista;
    }

    public void oficializarSancion(Connection conn, Long idSancion, int partidosSuspension, String motivo, String gravedad) throws SQLException {
        oficializarYPublicarSancion(conn, idSancion, partidosSuspension, motivo, gravedad, new java.util.Date());
    }

    public void oficializarYPublicarSancion(Connection conn, Long idSancion, int partidosSuspension, String motivo, String gravedad, java.util.Date fechaPublicacion) throws SQLException {
        String sql = "UPDATE sanciones SET estado = 'ACTIVA', partidos_suspension = ?, motivo = ?, gravedad = ?, fecha_publicacion_formal = ? WHERE id_sancion = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, partidosSuspension);
            ps.setString(2, motivo);
            ps.setString(3, gravedad);
            if (fechaPublicacion != null) {
                ps.setTimestamp(4, new Timestamp(fechaPublicacion.getTime()));
            } else {
                ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            }
            ps.setLong(5, idSancion);
            ps.executeUpdate();
        }
    }

    public void registrarFechaCumplida(Connection conn, Long idSancion) throws SQLException {
        String sql = "UPDATE sanciones "
                   + "SET fechas_cumplidas = fechas_cumplidas + 1, "
                   + "    estado = CASE WHEN fechas_cumplidas + 1 >= partidos_suspension THEN 'CUMPLIDA' ELSE estado END, "
                   + "    fecha_fin = CASE WHEN fechas_cumplidas + 1 >= partidos_suspension THEN CURRENT_DATE ELSE fecha_fin END "
                   + "WHERE id_sancion = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idSancion);
            ps.executeUpdate();
        }
    }

    public void anularSancion(Connection conn, Long idSancion, String motivoAnulacion) throws SQLException {
        String sql = "UPDATE sanciones SET estado = 'ANULADA', motivo = motivo || ' [ANULADA: ' || ? || ']' WHERE id_sancion = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, motivoAnulacion);
            ps.setLong(2, idSancion);
            ps.executeUpdate();
        }
    }

    private void configurarPreparedStatement(PreparedStatement ps, Sancion sancion) throws SQLException {
        ps.setLong(1, sancion.getJugadorSancionado().getId());
        if (sancion.getPartidoOrigen() != null && sancion.getPartidoOrigen().getIdPartido() != null) {
            ps.setLong(2, sancion.getPartidoOrigen().getIdPartido());
        } else {
            ps.setNull(2, Types.BIGINT);
        }
        if (sancion.getTorneo() != null && sancion.getTorneo().getIdTorneo() != null) {
            ps.setLong(3, sancion.getTorneo().getIdTorneo());
        } else {
            ps.setNull(3, Types.BIGINT);
        }
        ps.setString(4, sancion.getAlcance() != null ? sancion.getAlcance() : "TORNEO");
        ps.setString(5, sancion.getTipoOrigen() != null ? sancion.getTipoOrigen() : "ADMINISTRATIVA");
        ps.setString(6, sancion.getMotivo());
        ps.setString(7, sancion.getGravedad());
        ps.setInt(8, sancion.getPartidosSuspension());
        ps.setInt(9, sancion.getFechasCumplidas());
        ps.setDate(10, new Date(sancion.getFechaInicio().getTime()));
        if (sancion.getFechaFin() != null) {
            ps.setDate(11, new Date(sancion.getFechaFin().getTime()));
        } else {
            ps.setNull(11, Types.DATE);
        }
        if (sancion.getFechaPublicacionFormal() != null) {
            ps.setTimestamp(12, new Timestamp(sancion.getFechaPublicacionFormal().getTime()));
        } else {
            ps.setNull(12, Types.TIMESTAMP);
        }
        ps.setString(13, sancion.getEstado() != null ? sancion.getEstado() : "PENDIENTE");
    }

    private Sancion mapearSancion(Connection conn, ResultSet rs) throws SQLException {
        try {
            Long id = rs.getLong("id_sancion");
            Long idJugador = rs.getLong("id_jugador");
            Long idPartidoOrigen = rs.getLong("id_partido_origen");
            if (rs.wasNull()) idPartidoOrigen = null;
            Long idTorneo = rs.getLong("id_torneo");
            if (rs.wasNull()) idTorneo = null;

            String alcance = rs.getString("alcance");
            String tipoOrigen = rs.getString("tipo_origen");
            String motivo = rs.getString("motivo");
            String gravedad = rs.getString("gravedad");
            int partidosSusp = rs.getInt("partidos_suspension");
            int fechasCumplidas = rs.getInt("fechas_cumplidas");
            Date fechaInicio = rs.getDate("fecha_inicio");
            Date fechaFin = rs.getDate("fecha_fin");
            Timestamp pubTs = rs.getTimestamp("fecha_publicacion_formal");
            String estado = rs.getString("estado");

            Jugador jugador = jugadorDAO.buscarPorId(conn, idJugador);
            Partido partido = null;
            if (idPartidoOrigen != null) {
                partido = new Partido();
                partido.setIdPartido(idPartidoOrigen);
            }
            Torneo torneo = idTorneo != null ? torneoDAO.buscarPorId(conn, idTorneo) : null;

            Sancion s = new Sancion(
                fechaInicio,
                fechaFin,
                motivo,
                gravedad,
                jugador,
                partido,
                torneo,
                alcance,
                tipoOrigen,
                partidosSusp,
                fechasCumplidas,
                estado
            );
            s.setId(id);
            if (pubTs != null) {
                s.setFechaPublicacionFormal(new java.util.Date(pubTs.getTime()));
            }
            return s;
        } catch (TorneoExcepcion e) {
            throw new SQLException("Error de validación al mapear Sancion: " + e.getMessage(), e);
        }
    }
}
