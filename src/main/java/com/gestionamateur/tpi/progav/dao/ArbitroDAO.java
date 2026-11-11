package com.gestionamateur.tpi.progav.dao;

import com.gestionamateur.tpi.progav.modelo.Arbitro;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ArbitroDAO {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final RolDAO rolDAO = new RolDAO();

    public Long insertar(Arbitro arbitro, String contrasenaHash) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                Long id = insertar(conn, arbitro, contrasenaHash);
                conn.commit();
                return id;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public Long insertar(Connection conn, Arbitro arbitro, String contrasenaHash) throws SQLException {
        RolDAO.RolRegistro rol = rolDAO.buscarPorCodigo(conn, "ARBITRO");
        int idRol = rol != null ? rol.idRol : 3;

        Long idUsuario = usuarioDAO.insertar(conn, arbitro, contrasenaHash, idRol);

        String sql = "INSERT INTO arbitros (id_usuario, categoria_arbitral, telefono) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idUsuario);
            ps.setString(2, arbitro.getCategoriaArbitral());
            ps.setString(3, arbitro.getTelefono());
            ps.executeUpdate();
        }
        arbitro.setId(idUsuario);
        return idUsuario;
    }

    public Arbitro buscarPorId(Long idUsuario) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarPorId(conn, idUsuario);
        }
    }

    public Arbitro buscarPorId(Connection conn, Long idUsuario) throws SQLException {
        String sql = "SELECT u.id_usuario, u.usuario, u.contrasena, u.mail, u.nombre, u.apellido, "
                   + "a.categoria_arbitral, a.telefono "
                   + "FROM usuarios u JOIN arbitros a ON u.id_usuario = a.id_usuario "
                   + "WHERE u.id_usuario = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearArbitro(rs);
                }
            }
        }
        return null;
    }

    public List<Arbitro> buscarTodos() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarTodos(conn);
        }
    }

    public List<Arbitro> buscarTodos(Connection conn) throws SQLException {
        String sql = "SELECT u.id_usuario, u.usuario, u.contrasena, u.mail, u.nombre, u.apellido, "
                   + "a.categoria_arbitral, a.telefono "
                   + "FROM usuarios u JOIN arbitros a ON u.id_usuario = a.id_usuario "
                   + "WHERE u.activo = TRUE ORDER BY u.apellido, u.nombre";
        List<Arbitro> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearArbitro(rs));
            }
        }
        return lista;
    }

    private Arbitro mapearArbitro(ResultSet rs) throws SQLException {
        try {
            Long id = rs.getLong("id_usuario");
            String usr = rs.getString("usuario");
            String pass = rs.getString("contrasena");
            String mail = rs.getString("mail");
            String nom = rs.getString("nombre");
            String ape = rs.getString("apellido");
            String cat = rs.getString("categoria_arbitral");
            String tel = rs.getString("telefono");

            Arbitro a = new Arbitro(usr, pass, mail, nom, ape, cat, tel);
            a.setId(id);
            return a;
        } catch (TorneoExcepcion te) {
            throw new SQLException("Error al mapear árbitro: " + te.getMessage(), te);
        }
    }
}
