package com.gestionamateur.tpi.progav.dao;

import com.gestionamateur.tpi.progav.modelo.Partido;
import com.gestionamateur.tpi.progav.modelo.Reclamo;
import com.gestionamateur.tpi.progav.modelo.Sancion;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ReclamoDAO {

    private final SancionDAO sancionDAO = new SancionDAO();
    private final PartidoDAO partidoDAO = new PartidoDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public Long insertar(Reclamo reclamo, Long idSancion, Long idPartido, Long idCapitan) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return insertar(conn, reclamo, idSancion, idPartido, idCapitan);
        }
    }

    public Long insertar(Connection conn, Reclamo reclamo, Long idSancion, Long idPartido, Long idCapitan) throws SQLException {
        String sql = "INSERT INTO reclamos (id_sancion_apelada, id_partido, id_capitan, fecha_inicio, motivo, estado) "
                   + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id_reclamo";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (idSancion != null) {
                ps.setLong(1, idSancion);
            } else if (reclamo.getSancionApelada() != null && reclamo.getSancionApelada().getId() != null) {
                ps.setLong(1, reclamo.getSancionApelada().getId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }

            if (idPartido != null) {
                ps.setLong(2, idPartido);
            } else if (reclamo.getPartido() != null && reclamo.getPartido().getIdPartido() != null) {
                ps.setLong(2, reclamo.getPartido().getIdPartido());
            } else {
                ps.setNull(2, Types.BIGINT);
            }

            if (idCapitan != null) {
                ps.setLong(3, idCapitan);
            } else if (reclamo.getCapitan() != null && reclamo.getCapitan().getId() != null) {
                ps.setLong(3, reclamo.getCapitan().getId());
            } else {
                ps.setNull(3, Types.BIGINT);
            }

            Timestamp fi = reclamo.getFechaInicio() != null ? new Timestamp(reclamo.getFechaInicio().getTime()) : new Timestamp(System.currentTimeMillis());
            ps.setTimestamp(4, fi);
            ps.setString(5, reclamo.getMotivo());
            ps.setString(6, "PENDIENTE");

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    reclamo.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Error al insertar reclamo: no se generó ID.");
    }

    public Reclamo buscarPorId(Long idReclamo) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarPorId(conn, idReclamo);
        }
    }

    public Reclamo buscarPorId(Connection conn, Long idReclamo) throws SQLException {
        String sql = "SELECT id_reclamo, id_sancion_apelada, id_partido, id_capitan, "
                   + "fecha_inicio, fecha_fin, motivo, estado, veredicto, justificacion "
                   + "FROM reclamos WHERE id_reclamo = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idReclamo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearReclamo(conn, rs);
                }
            }
        }
        return null;
    }

    public List<Reclamo> buscarPendientes() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarPendientes(conn);
        }
    }

    public List<Reclamo> buscarPendientes(Connection conn) throws SQLException {
        String sql = "SELECT id_reclamo, id_sancion_apelada, id_partido, id_capitan, "
                   + "fecha_inicio, fecha_fin, motivo, estado, veredicto, justificacion "
                   + "FROM reclamos WHERE estado = 'PENDIENTE' ORDER BY fecha_inicio ASC";
        List<Reclamo> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearReclamo(conn, rs));
            }
        }
        return lista;
    }

    public List<Reclamo> buscarPorPartido(Connection conn, Long idPartido) throws SQLException {
        String sql = "SELECT id_reclamo, id_sancion_apelada, id_partido, id_capitan, "
                   + "fecha_inicio, fecha_fin, motivo, estado, veredicto, justificacion "
                   + "FROM reclamos WHERE id_partido = ? ORDER BY fecha_inicio DESC";
        List<Reclamo> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idPartido);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearReclamo(conn, rs));
                }
            }
        }
        return lista;
    }

    public void resolver(Connection conn, Long idReclamo, String veredicto, String justificacion) throws SQLException {
        String sql = "UPDATE reclamos SET estado = 'RESUELTO', fecha_fin = CURRENT_TIMESTAMP, "
                   + "veredicto = ?, justificacion = ? WHERE id_reclamo = ? AND estado = 'PENDIENTE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, veredicto);
            ps.setString(2, justificacion);
            ps.setLong(3, idReclamo);
            int filas = ps.executeUpdate();
            if (filas == 0) {
                throw new SQLException("No se pudo resolver el reclamo ID " + idReclamo + ": no existe o ya fue resuelto.");
            }
        }
    }

    /**
     * CU-13: Verifica si el reclamo sobre un partido está dentro de las horas hábiles reglamentarias (ej. 72 horas).
     */
    public boolean estaDentroDelPlazoPartido(Connection conn, Long idPartido, int horasLimite) throws SQLException {
        String sql = "SELECT (CURRENT_TIMESTAMP <= fecha_hora_finalizacion_real + (? || ' hours')::interval) "
                   + "FROM partidos WHERE id_partido = ? AND fecha_hora_finalizacion_real IS NOT NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, horasLimite);
            ps.setLong(2, idPartido);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        }
        return false;
    }

    /**
     * CU-14: Verifica si la apelación de una sanción está dentro de los días reglamentarios (ej. 5 días).
     */
    public boolean estaDentroDelPlazoApelacion(Connection conn, Long idSancion, int diasLimite) throws SQLException {
        String sql = "SELECT (CURRENT_TIMESTAMP <= fecha_publicacion_formal + (? || ' days')::interval) "
                   + "FROM sanciones WHERE id_sancion = ? AND fecha_publicacion_formal IS NOT NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, diasLimite);
            ps.setLong(2, idSancion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        }
        return false;
    }

    private Reclamo mapearReclamo(Connection conn, ResultSet rs) throws SQLException {
        try {
            Long id = rs.getLong("id_reclamo");
            Long idSancion = rs.getLong("id_sancion_apelada");
            if (rs.wasNull()) idSancion = null;
            Long idPartido = rs.getLong("id_partido");
            if (rs.wasNull()) idPartido = null;
            Long idCapitan = rs.getLong("id_capitan");
            if (rs.wasNull()) idCapitan = null;

            Timestamp fechaInicioTs = rs.getTimestamp("fecha_inicio");
            Timestamp fechaFinTs = rs.getTimestamp("fecha_fin");
            String motivo = rs.getString("motivo");
            String estado = rs.getString("estado");
            String veredicto = rs.getString("veredicto");
            String justificacion = rs.getString("justificacion");

            Sancion sancion = idSancion != null ? sancionDAO.buscarPorId(conn, idSancion) : null;
            Partido partido = idPartido != null ? partidoDAO.buscarPorId(conn, idPartido) : null;
            Usuario capitan = idCapitan != null ? usuarioDAO.buscarPorId(conn, idCapitan) : null;

            Reclamo r = new Reclamo();
            r.setId(id);
            if (fechaInicioTs != null) {
                r.setFechaInicio(new java.util.Date(fechaInicioTs.getTime()));
            }
            r.setMotivo(motivo);
            r.setSancionApelada(sancion);
            r.setPartido(partido);
            r.setCapitan(capitan);

            if ("RESUELTO".equalsIgnoreCase(estado)) {
                r.resolverReclamo(fechaFinTs != null ? new java.util.Date(fechaFinTs.getTime()) : new java.util.Date(), veredicto, justificacion);
            } else {
                r.setEstado("PENDIENTE");
            }

            return r;
        } catch (TorneoExcepcion e) {
            throw new SQLException("Error de validación al mapear Reclamo: " + e.getMessage(), e);
        }
    }
}
