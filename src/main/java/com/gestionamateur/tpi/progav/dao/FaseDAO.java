package com.gestionamateur.tpi.progav.dao;

import com.gestionamateur.tpi.progav.modelo.Fase;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FaseDAO {

    public Long insertar(Fase fase, Long idTorneo) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return insertar(conn, fase, idTorneo);
        }
    }

    public Long insertar(Connection conn, Fase fase, Long idTorneo) throws SQLException {
        String sql = "INSERT INTO fases (id_torneo, nombre, orden, limpia_amarillas) "
                   + "VALUES (?, ?, ?, ?) RETURNING id_fase";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idTorneo);
            ps.setString(2, fase.getNombre());
            ps.setInt(3, fase.getOrden());
            ps.setBoolean(4, fase.isLimpiaAmarillas());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    fase.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Error al insertar fase: no se obtuvo ID.");
    }

    public Fase buscarPorId(Connection conn, Long idFase) throws SQLException {
        String sql = "SELECT id_fase, id_torneo, nombre, orden, limpia_amarillas FROM fases WHERE id_fase = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idFase);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearFase(rs);
                }
            }
        }
        return null;
    }

    public List<Fase> buscarPorTorneo(Long idTorneo) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarPorTorneo(conn, idTorneo);
        }
    }

    public List<Fase> buscarPorTorneo(Connection conn, Long idTorneo) throws SQLException {
        String sql = "SELECT id_fase, id_torneo, nombre, orden, limpia_amarillas FROM fases "
                   + "WHERE id_torneo = ? ORDER BY orden ASC";
        List<Fase> fases = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idTorneo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    fases.add(mapearFase(rs));
                }
            }
        }
        return fases;
    }

    /**
     * Calcula el orden de la fase donde inició la ventana de acumulación vigente
     * considerando la regla de reinicio/limpieza por playoffs (fases.limpia_amarillas = TRUE).
     */
    public int obtenerOrdenInicioVentana(Connection conn, Long idTorneo, int ordenFaseActual) throws SQLException {
        String sql = "SELECT COALESCE(MAX(f2.orden), 1) AS orden_inicio_ventana "
                   + "FROM fases f2 "
                   + "WHERE f2.id_torneo = ? AND f2.orden <= ? AND f2.limpia_amarillas = TRUE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idTorneo);
            ps.setInt(2, ordenFaseActual);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("orden_inicio_ventana");
                }
            }
        }
        return 1;
    }

    private Fase mapearFase(ResultSet rs) throws SQLException {
        try {
            Long id = rs.getLong("id_fase");
            String nombre = rs.getString("nombre");
            int orden = rs.getInt("orden");
            boolean limpiaAmarillas = rs.getBoolean("limpia_amarillas");

            Fase f = new Fase(nombre, orden, limpiaAmarillas);
            f.setId(id);
            return f;
        } catch (TorneoExcepcion te) {
            throw new SQLException("Error al mapear fase: " + te.getMessage(), te);
        }
    }
}
