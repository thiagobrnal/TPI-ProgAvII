package com.gestionamateur.tpi.progav.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RolDAO {

    public static class RolRegistro {
        public final int idRol;
        public final String codigo;
        public final String nombre;
        public final String descripcion;

        public RolRegistro(int idRol, String codigo, String nombre, String descripcion) {
            this.idRol = idRol;
            this.codigo = codigo;
            this.nombre = nombre;
            this.descripcion = descripcion;
        }
    }

    public List<RolRegistro> buscarTodos() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarTodos(conn);
        }
    }

    public List<RolRegistro> buscarTodos(Connection conn) throws SQLException {
        String sql = "SELECT id_rol, codigo, nombre, descripcion FROM roles ORDER BY id_rol";
        List<RolRegistro> roles = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                roles.add(new RolRegistro(
                        rs.getInt("id_rol"),
                        rs.getString("codigo"),
                        rs.getString("nombre"),
                        rs.getString("descripcion")
                ));
            }
        }
        return roles;
    }

    public RolRegistro buscarPorCodigo(String codigo) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarPorCodigo(conn, codigo);
        }
    }

    public RolRegistro buscarPorCodigo(Connection conn, String codigo) throws SQLException {
        String sql = "SELECT id_rol, codigo, nombre, descripcion FROM roles WHERE codigo = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new RolRegistro(
                            rs.getInt("id_rol"),
                            rs.getString("codigo"),
                            rs.getString("nombre"),
                            rs.getString("descripcion")
                    );
                }
            }
        }
        return null;
    }
}
