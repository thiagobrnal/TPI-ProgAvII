package com.gestionamateur.tpi.progav.dao;

import com.gestionamateur.tpi.progav.modelo.Configuracion;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ConfiguracionDAO {

    public int obtenerValorInt(String clave, int valorDefecto) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return obtenerValorInt(conn, clave, valorDefecto);
        }
    }

    public int obtenerValorInt(Connection conn, String clave, int valorDefecto) throws SQLException {
        String sql = "SELECT valor FROM configuraciones WHERE clave = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clave);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("valor");
                }
            }
        }
        return valorDefecto;
    }

    public void actualizarValorInt(String clave, int valor) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            actualizarValorInt(conn, clave, valor);
        }
    }

    public void actualizarValorInt(Connection conn, String clave, int valor) throws SQLException {
        String sql = "INSERT INTO configuraciones (clave, valor, fecha_actualizacion) "
                   + "VALUES (?, ?, CURRENT_TIMESTAMP) "
                   + "ON CONFLICT (clave) DO UPDATE SET valor = EXCLUDED.valor, fecha_actualizacion = CURRENT_TIMESTAMP";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clave);
            ps.setInt(2, valor);
            ps.executeUpdate();
        }
    }

    public Map<String, Integer> obtenerMapa() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return obtenerMapa(conn);
        }
    }

    public Map<String, Integer> obtenerMapa(Connection conn) throws SQLException {
        String sql = "SELECT clave, valor FROM configuraciones";
        Map<String, Integer> mapa = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                mapa.put(rs.getString("clave"), rs.getInt("valor"));
            }
        }
        return mapa;
    }

    /**
     * Devuelve la descripcion asociada a cada parametro global (solo lectura).
     * Complementa a {@link #obtenerMapa()} para poder presentar clave, valor y descripcion
     * en la capa de presentacion de CU-18 (Gestionar configuracion general).
     */
    public Map<String, String> obtenerDescripciones() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return obtenerDescripciones(conn);
        }
    }

    public Map<String, String> obtenerDescripciones(Connection conn) throws SQLException {
        String sql = "SELECT clave, descripcion FROM configuraciones";
        Map<String, String> mapa = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                mapa.put(rs.getString("clave"), rs.getString("descripcion"));
            }
        }
        return mapa;
    }

    public Configuracion obtenerConfiguracion() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            Map<String, Integer> mapa = obtenerMapa(conn);
            Configuracion c = new Configuracion();
            try {
                c.setValores(mapa);
            } catch (TorneoExcepcion e) {
                throw new SQLException("Error al inicializar Configuracion: " + e.getMessage(), e);
            }
            return c;
        }
    }
}
