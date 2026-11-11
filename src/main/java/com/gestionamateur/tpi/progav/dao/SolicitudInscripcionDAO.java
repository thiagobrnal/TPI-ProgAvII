package com.gestionamateur.tpi.progav.dao;

import com.gestionamateur.tpi.progav.modelo.Equipo;
import com.gestionamateur.tpi.progav.modelo.SolicitudInscripcion;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SolicitudInscripcionDAO {

    private final EquipoDAO equipoDAO = new EquipoDAO();

    public Long insertar(SolicitudInscripcion solicitud, Long idEquipo) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return insertar(conn, solicitud, idEquipo);
        }
    }

    public Long insertar(Connection conn, SolicitudInscripcion solicitud, Long idEquipo) throws SQLException {
        String sql = "INSERT INTO solicitudes_inscripcion (id_equipo, estado, motivo) "
                   + "VALUES (?, ?, ?) RETURNING id_solicitud";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idEquipo != null ? idEquipo : solicitud.getEquipoSolicitante().getIdEquipo());
            ps.setString(2, solicitud.getEstado() != null ? solicitud.getEstado() : "PENDIENTE");
            ps.setString(3, solicitud.getMotivo());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    solicitud.setIdSolicitud(id);
                    return id;
                }
            }
        }
        throw new SQLException("Error al insertar solicitud de inscripción: no se generó ID.");
    }

    public SolicitudInscripcion buscarPorId(Long idSolicitud) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarPorId(conn, idSolicitud);
        }
    }

    public SolicitudInscripcion buscarPorId(Connection conn, Long idSolicitud) throws SQLException {
        String sql = "SELECT id_solicitud, id_equipo, estado, motivo, fecha_solicitud "
                   + "FROM solicitudes_inscripcion WHERE id_solicitud = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idSolicitud);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearSolicitud(conn, rs);
                }
            }
        }
        return null;
    }

    public List<SolicitudInscripcion> buscarPorEquipo(Long idEquipo) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarPorEquipo(conn, idEquipo);
        }
    }

    public List<SolicitudInscripcion> buscarPorEquipo(Connection conn, Long idEquipo) throws SQLException {
        String sql = "SELECT id_solicitud, id_equipo, estado, motivo, fecha_solicitud "
                   + "FROM solicitudes_inscripcion WHERE id_equipo = ? ORDER BY fecha_solicitud DESC";
        List<SolicitudInscripcion> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idEquipo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearSolicitud(conn, rs));
                }
            }
        }
        return lista;
    }

    public List<SolicitudInscripcion> buscarPendientes() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarPendientes(conn);
        }
    }

    public List<SolicitudInscripcion> buscarPendientes(Connection conn) throws SQLException {
        String sql = "SELECT id_solicitud, id_equipo, estado, motivo, fecha_solicitud "
                   + "FROM solicitudes_inscripcion WHERE estado = 'PENDIENTE' ORDER BY fecha_solicitud ASC";
        List<SolicitudInscripcion> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearSolicitud(conn, rs));
            }
        }
        return lista;
    }

    public void actualizarEstado(Long idSolicitud, String estado, String motivo) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            actualizarEstado(conn, idSolicitud, estado, motivo);
        }
    }

    public void actualizarEstado(Connection conn, Long idSolicitud, String estado, String motivo) throws SQLException {
        String sql = "UPDATE solicitudes_inscripcion SET estado = ?, motivo = ? WHERE id_solicitud = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setString(2, motivo);
            ps.setLong(3, idSolicitud);
            ps.executeUpdate();
        }
    }

    private SolicitudInscripcion mapearSolicitud(Connection conn, ResultSet rs) throws SQLException {
        try {
            Long idSolicitud = rs.getLong("id_solicitud");
            Long idEquipo = rs.getLong("id_equipo");
            String estado = rs.getString("estado");
            String motivo = rs.getString("motivo");

            Equipo eq = equipoDAO.buscarPorId(conn, idEquipo);
            SolicitudInscripcion sol = new SolicitudInscripcion();
            sol.setIdSolicitud(idSolicitud);
            sol.setEquipoSolicitante(eq);
            sol.setEstado(estado);
            sol.setMotivo(motivo);
            return sol;
        } catch (TorneoExcepcion e) {
            throw new SQLException("Error de validación al mapear SolicitudInscripcion: " + e.getMessage(), e);
        }
    }
}
