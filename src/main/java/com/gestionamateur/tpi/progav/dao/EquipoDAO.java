package com.gestionamateur.tpi.progav.dao;

import com.gestionamateur.tpi.progav.modelo.Cancha;
import com.gestionamateur.tpi.progav.modelo.Categoria;
import com.gestionamateur.tpi.progav.modelo.Equipo;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class EquipoDAO {

    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final CanchaDAO canchaDAO = new CanchaDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public Long insertar(Equipo equipo, Long idCapitan) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return insertar(conn, equipo, idCapitan);
        }
    }

    public Long insertar(Connection conn, Equipo equipo, Long idCapitan) throws SQLException {
        String sql = "INSERT INTO equipos (nombre, alias, id_categoria, color_camiseta, id_sede_habitual, id_capitan) "
                   + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id_equipo";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, equipo.getNombre());
            ps.setString(2, equipo.getAlias());
            ps.setLong(3, equipo.getCategoria().getId());
            ps.setString(4, equipo.getColorCamiseta());
            if (equipo.getSedeHabitual() != null && equipo.getSedeHabitual().getIdCancha() != null) {
                ps.setLong(5, equipo.getSedeHabitual().getIdCancha());
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            ps.setLong(6, idCapitan != null ? idCapitan : equipo.getCapitan().getId());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    equipo.setIdEquipo(id);
                    return id;
                }
            }
        }
        throw new SQLException("Error al insertar equipo: no se obtuvo ID autogenerado.");
    }

    /** CU-01: verifica si ya existe un equipo con el mismo nombre en la misma categoria. */
    public boolean existePorNombreYCategoria(Connection conn, String nombre, Long idCategoria, Long idEquipoExcluido)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM equipos WHERE lower(nombre) = lower(?) AND id_categoria = ?"
                   + (idEquipoExcluido != null ? " AND id_equipo <> ?" : "");
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setLong(2, idCategoria);
            if (idEquipoExcluido != null) {
                ps.setLong(3, idEquipoExcluido);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /** CU-01: actualiza los datos de un equipo ya registrado. */
    public void actualizar(Connection conn, Equipo equipo, Long idCapitan) throws SQLException {
        String sql = "UPDATE equipos SET nombre = ?, alias = ?, id_categoria = ?, color_camiseta = ?, "
                   + "id_sede_habitual = ?, id_capitan = ? WHERE id_equipo = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, equipo.getNombre());
            ps.setString(2, equipo.getAlias());
            ps.setLong(3, equipo.getCategoria().getId());
            ps.setString(4, equipo.getColorCamiseta());
            if (equipo.getSedeHabitual() != null && equipo.getSedeHabitual().getIdCancha() != null) {
                ps.setLong(5, equipo.getSedeHabitual().getIdCancha());
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            ps.setLong(6, idCapitan != null ? idCapitan : equipo.getCapitan().getId());
            ps.setLong(7, equipo.getIdEquipo());
            ps.executeUpdate();
        }
    }

    public Equipo buscarPorId(Long idEquipo) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarPorId(conn, idEquipo);
        }
    }

    public Equipo buscarPorId(Connection conn, Long idEquipo) throws SQLException {
        String sql = "SELECT id_equipo, nombre, alias, id_categoria, color_camiseta, id_sede_habitual, id_capitan "
                   + "FROM equipos WHERE id_equipo = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idEquipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearEquipo(conn, rs);
                }
            }
        }
        return null;
    }

    public Equipo buscarPorCapitan(Long idCapitan) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarPorCapitan(conn, idCapitan);
        }
    }

    public Equipo buscarPorCapitan(Connection conn, Long idCapitan) throws SQLException {
        String sql = "SELECT id_equipo, nombre, alias, id_categoria, color_camiseta, id_sede_habitual, id_capitan "
                   + "FROM equipos WHERE id_capitan = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idCapitan);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearEquipo(conn, rs);
                }
            }
        }
        return null;
    }

    public List<Equipo> buscarTodos() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarTodos(conn);
        }
    }

    public List<Equipo> buscarTodos(Connection conn) throws SQLException {
        String sql = "SELECT id_equipo, nombre, alias, id_categoria, color_camiseta, id_sede_habitual, id_capitan "
                   + "FROM equipos ORDER BY nombre";
        List<Equipo> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearEquipo(conn, rs));
            }
        }
        return lista;
    }

    public List<Equipo> buscarPorCategoria(Connection conn, Long idCategoria) throws SQLException {
        String sql = "SELECT id_equipo, nombre, alias, id_categoria, color_camiseta, id_sede_habitual, id_capitan "
                   + "FROM equipos WHERE id_categoria = ? ORDER BY nombre";
        List<Equipo> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idCategoria);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearEquipo(conn, rs));
                }
            }
        }
        return lista;
    }

    private Equipo mapearEquipo(Connection conn, ResultSet rs) throws SQLException {
        try {
            Long id = rs.getLong("id_equipo");
            String nombre = rs.getString("nombre");
            String alias = rs.getString("alias");
            Long idCat = rs.getLong("id_categoria");
            String color = rs.getString("color_camiseta");
            long idSedeVal = rs.getLong("id_sede_habitual");
            Long idSede = rs.wasNull() ? null : idSedeVal;
            Long idCap = rs.getLong("id_capitan");

            Categoria cat = categoriaDAO.buscarPorId(conn, idCat);
            Cancha sede = idSede != null ? canchaDAO.buscarPorId(conn, idSede) : null;
            Usuario cap = usuarioDAO.buscarPorId(conn, idCap);

            Equipo e = new Equipo(nombre, cat, color, sede, cap);
            e.setIdEquipo(id);
            e.setAlias(alias);
            return e;
        } catch (TorneoExcepcion te) {
            throw new SQLException("Error al mapear equipo: " + te.getMessage(), te);
        }
    }
}
