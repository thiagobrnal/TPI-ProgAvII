package com.gestionamateur.tpi.progav.dao;

import com.gestionamateur.tpi.progav.modelo.Cancha;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CanchaDAO {

    public Long insertar(Cancha cancha) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return insertar(conn, cancha);
        }
    }

    public Long insertar(Connection conn, Cancha cancha) throws SQLException {
        String sql = "INSERT INTO canchas (nombre, direccion, tipo_superficie, activa) "
                   + "VALUES (?, ?, ?, TRUE) RETURNING id_cancha";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cancha.getNombre());
            ps.setString(2, cancha.getDireccion());
            ps.setString(3, cancha.getTipoSuperficie());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    cancha.setIdCancha(id);
                    return id;
                }
            }
        }
        throw new SQLException("Error al insertar cancha: no se obtuvo ID autogenerado.");
    }

    public Cancha buscarPorId(Long idCancha) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarPorId(conn, idCancha);
        }
    }

    public Cancha buscarPorId(Connection conn, Long idCancha) throws SQLException {
        String sql = "SELECT id_cancha, nombre, direccion, tipo_superficie, activa FROM canchas WHERE id_cancha = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idCancha);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearCancha(rs);
                }
            }
        }
        return null;
    }

    public List<Cancha> buscarTodas() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarTodas(conn);
        }
    }

    public List<Cancha> buscarTodas(Connection conn) throws SQLException {
        String sql = "SELECT id_cancha, nombre, direccion, tipo_superficie, activa FROM canchas WHERE activa = TRUE ORDER BY nombre";
        List<Cancha> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearCancha(rs));
            }
        }
        return lista;
    }

    /** CU-16: listado completo de sedes, incluidas las inhabilitadas. */
    public List<Cancha> buscarTodasIncluyendoInactivas() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            String sql = "SELECT id_cancha, nombre, direccion, tipo_superficie, activa FROM canchas ORDER BY nombre";
            List<Cancha> lista = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearCancha(rs));
                }
            }
            return lista;
        }
    }

    /** CU-16: verifica si ya existe una sede con el mismo nombre. */
    public boolean existePorNombre(String nombre) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return existePorNombre(conn, nombre);
        }
    }

    /** CU-16: verifica si ya existe una sede con el mismo nombre. */
    public boolean existePorNombre(Connection conn, String nombre) throws SQLException {
        String sql = "SELECT COUNT(*) FROM canchas WHERE lower(nombre) = lower(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /** CU-16 (alternativa A1): habilita o inhabilita una sede. */
    public void actualizarActiva(Connection conn, Long idCancha, boolean activa) throws SQLException {
        String sql = "UPDATE canchas SET activa = ? WHERE id_cancha = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, activa);
            ps.setLong(2, idCancha);
            ps.executeUpdate();
        }
    }

    private Cancha mapearCancha(ResultSet rs) throws SQLException {
        try {
            Long id = rs.getLong("id_cancha");
            String nombre = rs.getString("nombre");
            String direccion = rs.getString("direccion");
            String superficie = rs.getString("tipo_superficie");
            boolean activa = true;
            try {
                activa = rs.getBoolean("activa");
            } catch (SQLException ignore) {
                // La columna 'activa' no vino en la consulta: se asume activa.
            }

            Cancha c = new Cancha(nombre, direccion, superficie);
            c.setIdCancha(id);
            c.setActiva(activa);
            return c;
        } catch (TorneoExcepcion te) {
            throw new SQLException("Error al mapear cancha desde BD: " + te.getMessage(), te);
        }
    }
}
