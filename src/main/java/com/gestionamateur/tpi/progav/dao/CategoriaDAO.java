package com.gestionamateur.tpi.progav.dao;

import com.gestionamateur.tpi.progav.modelo.Categoria;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDAO {

    public List<Categoria> buscarTodas() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarTodas(conn);
        }
    }

    public List<Categoria> buscarTodas(Connection conn) throws SQLException {
        String sql = "SELECT id_categoria, nombre, genero, edad_minima, edad_maxima FROM categorias ORDER BY id_categoria";
        List<Categoria> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearCategoria(rs));
            }
        }
        return lista;
    }

    public Categoria buscarPorId(Long id) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarPorId(conn, id);
        }
    }

    public Categoria buscarPorId(Connection conn, Long id) throws SQLException {
        String sql = "SELECT id_categoria, nombre, genero, edad_minima, edad_maxima FROM categorias WHERE id_categoria = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearCategoria(rs);
                }
            }
        }
        return null;
    }

    public Long insertar(Connection conn, Categoria categoria) throws SQLException {
        String sql = "INSERT INTO categorias (nombre, genero, edad_minima, edad_maxima) "
                   + "VALUES (?, ?, ?, ?) RETURNING id_categoria";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, categoria.getNombre());
            ps.setString(2, categoria.getGenero());
            ps.setInt(3, categoria.getEdadMinima());
            if (categoria.getEdadMaxima() != null) {
                ps.setInt(4, categoria.getEdadMaxima());
            } else {
                ps.setNull(4, Types.INTEGER);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    categoria.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Error al insertar categoría: no se obtuvo ID.");
    }

    private Categoria mapearCategoria(ResultSet rs) throws SQLException {
        try {
            Long id = rs.getLong("id_categoria");
            String nombre = rs.getString("nombre");
            String genero = rs.getString("genero");
            int edadMinima = rs.getInt("edad_minima");
            int edadMaxVal = rs.getInt("edad_maxima");
            Integer edadMaxima = rs.wasNull() ? null : edadMaxVal;

            Categoria cat = new Categoria(nombre, genero, edadMinima, edadMaxima);
            cat.setId(id);
            return cat;
        } catch (TorneoExcepcion te) {
            throw new SQLException("Error al mapear categoría: " + te.getMessage(), te);
        }
    }
}
