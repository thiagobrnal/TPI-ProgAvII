package com.gestionamateur.tpi.progav.dao;

import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    public Long insertar(Usuario usuario, String contrasenaHash, int idRol) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return insertar(conn, usuario, contrasenaHash, idRol);
        }
    }

    public Long insertar(Connection conn, Usuario usuario, String contrasenaHash, int idRol) throws SQLException {
        return insertarEnTabla(conn, "usuarios", usuario, contrasenaHash, idRol);
    }

    private Long insertarEnTabla(Connection conn, String tabla, Usuario usuario, String contrasenaHash, int idRol) throws SQLException {
        String sql = "INSERT INTO " + tabla + " (usuario, contrasena, mail, nombre, apellido, id_rol, activo) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id_usuario";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuario.getUsuario());
            ps.setString(2, contrasenaHash != null ? contrasenaHash : usuario.getContraseña());
            ps.setString(3, usuario.getMail());
            ps.setString(4, usuario.getNombre());
            ps.setString(5, usuario.getApellido());
            ps.setInt(6, idRol);
            ps.setBoolean(7, usuario.isActivo());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    usuario.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("No se pudo obtener el ID autogenerado al insertar el usuario.");
    }

    public Long insertarPrimerAdministradorAtomico(Usuario usuario, String contrasenaHash, int idRol) throws SQLException, TorneoExcepcion {
        return insertarPrimerAdministradorAtomico("usuarios", usuario, contrasenaHash, idRol);
    }

    public Long insertarPrimerAdministradorAtomico(String tabla, Usuario usuario, String contrasenaHash, int idRol) throws SQLException, TorneoExcepcion {
        if (tabla == null || (!tabla.equals("usuarios") && !tabla.equals("test_bootstrap_usuarios"))) {
            throw new IllegalArgumentException("Nombre de tabla no autorizado para bootstrap: " + tabla);
        }
        try (Connection conn = ConexionBD.obtenerConexion()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                // Bloqueo exclusivo a nivel de tabla para prevenir race conditions en inicialización
                try (Statement st = conn.createStatement()) {
                    st.execute("LOCK TABLE " + tabla + " IN EXCLUSIVE MODE");
                }
                int total;
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tabla)) {
                    total = rs.next() ? rs.getInt(1) : 0;
                }
                if (total > 0) {
                    conn.rollback();
                    throw new TorneoExcepcion("Inicialización rechazada: el sistema ya posee usuarios registrados.");
                }
                Long id = insertarEnTabla(conn, tabla, usuario, contrasenaHash, idRol);
                conn.commit();
                return id;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof TorneoExcepcion) {
                    throw (TorneoExcepcion) e;
                }
                throw new SQLException("Error durante la inicialización atómica del administrador: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        }
    }

    public Usuario buscarPorUsuario(String nombreUsuario) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarPorUsuario(conn, nombreUsuario);
        }
    }

    public Usuario buscarPorNombreUsuario(String nombreUsuario) throws SQLException {
        return buscarPorUsuario(nombreUsuario);
    }

    public Usuario buscarPorUsuario(Connection conn, String nombreUsuario) throws SQLException {
        String sql = "SELECT u.id_usuario, u.usuario, u.contrasena, u.mail, u.nombre, u.apellido, u.activo, r.codigo AS rol_codigo "
                   + "FROM usuarios u JOIN roles r ON u.id_rol = r.id_rol "
                   + "WHERE u.usuario = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombreUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
            }
        }
        return null;
    }

    public Usuario buscarPorNombreUsuario(Connection conn, String nombreUsuario) throws SQLException {
        return buscarPorUsuario(conn, nombreUsuario);
    }

    public Usuario buscarPorId(Long idUsuario) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarPorId(conn, idUsuario);
        }
    }

    public Usuario buscarPorId(Connection conn, Long idUsuario) throws SQLException {
        String sql = "SELECT u.id_usuario, u.usuario, u.contrasena, u.mail, u.nombre, u.apellido, u.activo, r.codigo AS rol_codigo "
                   + "FROM usuarios u JOIN roles r ON u.id_rol = r.id_rol "
                   + "WHERE u.id_usuario = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
            }
        }
        return null;
    }

    public void actualizarClaveHash(Connection conn, Long idUsuario, String nuevoHash) throws SQLException {
        String sql = "UPDATE usuarios SET contrasena = ? WHERE id_usuario = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoHash);
            ps.setLong(2, idUsuario);
            ps.executeUpdate();
        }
    }

    public void actualizarClaveHash(Long idUsuario, String nuevoHash) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            actualizarClaveHash(conn, idUsuario, nuevoHash);
        }
    }

    public List<Usuario> buscarPorRol(int idRol) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarPorRol(conn, idRol);
        }
    }

    public List<Usuario> buscarPorRol(Connection conn, int idRol) throws SQLException {
        String sql = "SELECT u.id_usuario, u.usuario, u.contrasena, u.mail, u.nombre, u.apellido, u.activo, r.codigo AS rol_codigo "
                   + "FROM usuarios u JOIN roles r ON u.id_rol = r.id_rol "
                   + "WHERE u.id_rol = ? AND u.activo = TRUE ORDER BY u.nombre, u.apellido";
        List<Usuario> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idRol);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearUsuario(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Lista todas las cuentas registradas (activas e inactivas) con su rol asociado.
     * Se utiliza desde la capa de presentacion de CU-17 (Gestionar Usuarios) para
     * poder exhibir el estado real de cada cuenta y reactivated las que fueron dadas de baja.
     */
    public List<Usuario> buscarTodos() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarTodos(conn);
        }
    }

    public List<Usuario> buscarTodos(Connection conn) throws SQLException {
        String sql = "SELECT u.id_usuario, u.usuario, u.contrasena, u.mail, u.nombre, u.apellido, u.activo, r.codigo AS rol_codigo "
                   + "FROM usuarios u JOIN roles r ON u.id_rol = r.id_rol "
                   + "ORDER BY u.apellido, u.nombre, u.id_usuario";
        List<Usuario> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearUsuario(rs));
            }
        }
        return lista;
    }

    /**
     * Activa o desactiva una cuenta de usuario (CU-17). No altera jamas el hash de contrasena.
     */
    public void actualizarActivo(Connection conn, Long idUsuario, boolean activo) throws SQLException {
        String sql = "UPDATE usuarios SET activo = ? WHERE id_usuario = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setLong(2, idUsuario);
            ps.executeUpdate();
        }
    }

    public void actualizarActivo(Long idUsuario, boolean activo) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            actualizarActivo(conn, idUsuario, activo);
        }
    }

    public int contarUsuarios(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuarios";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public int contarUsuarios() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return contarUsuarios(conn);
        }
    }

    private Usuario mapearUsuario(ResultSet rs) throws SQLException {
        try {
            String rolCodigo = rs.getString("rol_codigo");
            TiposDatos.Rol rol;
            try {
                rol = TiposDatos.Rol.valueOf(rolCodigo);
            } catch (IllegalArgumentException e) {
                rol = TiposDatos.Rol.PUBLICO;
            }

            Usuario u = new Usuario();
            u.setId(rs.getLong("id_usuario"));
            u.setUsuario(rs.getString("usuario"));
            u.setContraseña(rs.getString("contrasena"));
            u.setMail(rs.getString("mail"));
            u.setNombre(rs.getString("nombre"));
            u.setApellido(rs.getString("apellido"));
            u.setRol(rol);
            u.setActivo(rs.getBoolean("activo"));
            return u;
        } catch (TorneoExcepcion te) {
            throw new SQLException("Error al mapear usuario desde base de datos: " + te.getMessage(), te);
        }
    }
}
