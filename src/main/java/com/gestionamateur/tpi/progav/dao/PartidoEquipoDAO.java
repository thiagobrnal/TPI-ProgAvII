package com.gestionamateur.tpi.progav.dao;

import com.gestionamateur.tpi.progav.modelo.Equipo;
import com.gestionamateur.tpi.progav.modelo.Partido;
import com.gestionamateur.tpi.progav.modelo.PartidoEquipo;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PartidoEquipoDAO {

    private final EquipoDAO equipoDAO = new EquipoDAO();

    public Long insertar(PartidoEquipo pe, Long idPartido, Long idEquipo) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return insertar(conn, pe, idPartido, idEquipo);
        }
    }

    public Long insertar(Connection conn, PartidoEquipo pe, Long idPartido, Long idEquipo) throws SQLException {
        String sql = "INSERT INTO partido_equipos (id_partido, id_equipo, es_local, goles) "
                   + "VALUES (?, ?, ?, ?) RETURNING id_partido_equipo";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idPartido != null ? idPartido : pe.getPartido().getIdPartido());
            ps.setLong(2, idEquipo != null ? idEquipo : pe.getEquipo().getIdEquipo());
            ps.setBoolean(3, pe.isEsLocal());
            ps.setInt(4, pe.getGoles());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    pe.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Error al insertar partido_equipo: no se generó ID.");
    }

    public void asignarEquiposAPartido(Connection conn, Long idPartido, Long idLocal, Long idVisitante) throws SQLException {
        if (idLocal.equals(idVisitante)) {
            throw new SQLException("Un equipo no puede jugar contra sí mismo.");
        }
        String sql = "INSERT INTO partido_equipos (id_partido, id_equipo, es_local, goles) VALUES (?, ?, ?, 0)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // Local
            ps.setLong(1, idPartido);
            ps.setLong(2, idLocal);
            ps.setBoolean(3, true);
            ps.executeUpdate();

            // Visitante
            ps.setLong(1, idPartido);
            ps.setLong(2, idVisitante);
            ps.setBoolean(3, false);
            ps.executeUpdate();
        }
    }

    public void actualizarGoles(Connection conn, Long idPartido, Long idEquipo, int goles) throws SQLException {
        String sql = "UPDATE partido_equipos SET goles = ? WHERE id_partido = ? AND id_equipo = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, goles);
            ps.setLong(2, idPartido);
            ps.setLong(3, idEquipo);
            ps.executeUpdate();
        }
    }

    public List<PartidoEquipo> buscarPorPartido(Long idPartido) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarPorPartido(conn, idPartido);
        }
    }

    public List<PartidoEquipo> buscarPorPartido(Connection conn, Long idPartido) throws SQLException {
        String sql = "SELECT id_partido_equipo, id_partido, id_equipo, es_local, goles "
                   + "FROM partido_equipos WHERE id_partido = ? ORDER BY es_local DESC";
        List<PartidoEquipo> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idPartido);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearPartidoEquipo(conn, rs));
                }
            }
        }
        return lista;
    }

    public PartidoEquipo buscarLocal(Connection conn, Long idPartido) throws SQLException {
        String sql = "SELECT id_partido_equipo, id_partido, id_equipo, es_local, goles "
                   + "FROM partido_equipos WHERE id_partido = ? AND es_local = TRUE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idPartido);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearPartidoEquipo(conn, rs);
                }
            }
        }
        return null;
    }

    public PartidoEquipo buscarVisitante(Connection conn, Long idPartido) throws SQLException {
        String sql = "SELECT id_partido_equipo, id_partido, id_equipo, es_local, goles "
                   + "FROM partido_equipos WHERE id_partido = ? AND es_local = FALSE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idPartido);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearPartidoEquipo(conn, rs);
                }
            }
        }
        return null;
    }

    private PartidoEquipo mapearPartidoEquipo(Connection conn, ResultSet rs) throws SQLException {
        try {
            Long id = rs.getLong("id_partido_equipo");
            Long idPartido = rs.getLong("id_partido");
            Long idEquipo = rs.getLong("id_equipo");
            boolean esLocal = rs.getBoolean("es_local");
            int goles = rs.getInt("goles");

            Equipo eq = equipoDAO.buscarPorId(conn, idEquipo);
            Partido p = new Partido();
            p.setIdPartido(idPartido);

            PartidoEquipo pe = new PartidoEquipo(esLocal, goles, eq, p);
            pe.setId(id);
            return pe;
        } catch (TorneoExcepcion e) {
            throw new SQLException("Error de validación al mapear PartidoEquipo: " + e.getMessage(), e);
        }
    }
}
