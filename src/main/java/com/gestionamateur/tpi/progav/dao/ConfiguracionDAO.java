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

    /**
     * DEF-011: parametros cuyo dominio es un entero no negativo (plazos, umbrales y montos).
     * El resto de los parametros mantienen su contrato original.
     */
    private static final java.util.Set<String> REQUIERE_VALOR_NO_NEGATIVO = java.util.Collections.unmodifiableSet(
            new java.util.LinkedHashSet<>(java.util.Arrays.asList(
                    "EDAD_MINIMA_GENERAL",
                    "AMARILLAS_SUSPENSION",
                    "HORAS_LIMITE_RECLAMO",
                    "DIAS_LIMITE_APELACION",
                    "PARTIDOS_SUSPENSION_MAXIMA",
                    "DURACION_PARTIDO_MINUTOS",
                    "CUPO_EQUIPOS_MINIMO")));

    /** Expone el conjunto de claves con dominio no negativo (usado por las pruebas). */
    public static java.util.Set<String> clavesConValorNoNegativo() {
        return REQUIERE_VALOR_NO_NEGATIVO;
    }

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

    /**
     * Actualiza el valor de un parametro de configuracion.
     *
     * <p>DEF-011: la validacion no puede depender exclusivamente de la interfaz. Los parametros
     * cuyo dominio es un entero no negativo se rechazan en la capa de persistencia, de modo que
     * ningun llamante pueda dejar un valor invalido en la base.
     */
    public void actualizarValorInt(Connection conn, String clave, int valor) throws SQLException {
        if (valor < 0 && REQUIERE_VALOR_NO_NEGATIVO.contains(clave)) {
            throw new SQLException("El parametro de configuracion '" + clave + "' no admite valores negativos (valor recibido: "
                    + valor + ").");
        }
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
