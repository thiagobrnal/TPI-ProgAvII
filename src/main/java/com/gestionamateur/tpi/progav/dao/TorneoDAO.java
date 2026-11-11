package com.gestionamateur.tpi.progav.dao;

import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.Torneo;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TorneoDAO {

    public Long insertar(Torneo torneo) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return insertar(conn, torneo);
        }
    }

    public Long insertar(Connection conn, Torneo torneo) throws SQLException {
        String sql = "INSERT INTO torneos (nombre, formato, cupo_equipos, fecha_inicio, estado) "
                   + "VALUES (?, ?, ?, ?, ?) RETURNING id_torneo";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, torneo.getNombre());
            ps.setString(2, torneo.getFormato().name());
            ps.setInt(3, torneo.getCupoEquipos());
            ps.setDate(4, new Date(torneo.getFechaInicio().getTime()));
            ps.setString(5, torneo.getEstado() != null ? torneo.getEstado() : "CONFIGURACION");

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    torneo.setIdTorneo(id);
                    return id;
                }
            }
        }
        throw new SQLException("Error al insertar torneo: no se obtuvo ID autogenerado.");
    }

    public Torneo buscarPorId(Long idTorneo) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarPorId(conn, idTorneo);
        }
    }

    public Torneo buscarPorId(Connection conn, Long idTorneo) throws SQLException {
        String sql = "SELECT id_torneo, nombre, formato, cupo_equipos, fecha_inicio, estado "
                   + "FROM torneos WHERE id_torneo = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idTorneo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearTorneo(rs);
                }
            }
        }
        return null;
    }

    public List<Torneo> buscarTodos() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarTodos(conn);
        }
    }

    public List<Torneo> buscarTodos(Connection conn) throws SQLException {
        String sql = "SELECT id_torneo, nombre, formato, cupo_equipos, fecha_inicio, estado "
                   + "FROM torneos ORDER BY fecha_inicio DESC, id_torneo DESC";
        List<Torneo> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearTorneo(rs));
            }
        }
        return lista;
    }

    public void actualizarEstado(Connection conn, Long idTorneo, String nuevoEstado) throws SQLException {
        String sql = "UPDATE torneos SET estado = ? WHERE id_torneo = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setLong(2, idTorneo);
            ps.executeUpdate();
        }
    }

    private Torneo mapearTorneo(ResultSet rs) throws SQLException {
        try {
            Long id = rs.getLong("id_torneo");
            String nombre = rs.getString("nombre");
            String formatoStr = rs.getString("formato");
            int cupo = rs.getInt("cupo_equipos");
            Date fechaInicio = rs.getDate("fecha_inicio");
            String estado = rs.getString("estado");

            TiposDatos.FormatoTorneo formato;
            try {
                formato = TiposDatos.FormatoTorneo.valueOf(formatoStr);
            } catch (Exception e) {
                formato = TiposDatos.FormatoTorneo.LIGA;
            }

            Torneo t = new Torneo(nombre, cupo, formato, fechaInicio);
            t.setIdTorneo(id);
            t.setEstado(estado);
            return t;
        } catch (TorneoExcepcion te) {
            throw new SQLException("Error al mapear torneo desde BD: " + te.getMessage(), te);
        }
    }
}
