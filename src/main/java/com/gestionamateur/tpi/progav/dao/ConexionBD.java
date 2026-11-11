package com.gestionamateur.tpi.progav.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Componente centralizado de infraestructura JDBC para obtener conexiones
 * hacia la base de datos PostgreSQL local en Release 1 (Desktop).
 */
public final class ConexionBD {

    private static final Logger LOGGER = Logger.getLogger(ConexionBD.class.getName());

    public static final String ENV_DB_URL = "TPI_DB_URL";
    public static final String ENV_DB_USER = "TPI_DB_USER";
    public static final String ENV_DB_PASSWORD = "TPI_DB_PASSWORD";

    public static final String ENV_TEST_DB_URL = "TPI_TEST_DB_URL";
    public static final String ENV_TEST_DB_USER = "TPI_TEST_DB_USER";
    public static final String ENV_TEST_DB_PASSWORD = "TPI_TEST_DB_PASSWORD";

    public static final String URL_REFERENCIA = "jdbc:postgresql://127.0.0.1:55432/tpi_pa2_r1_demo";
    private static final String PREFIJO_JDBC_POSTGRESQL = "jdbc:postgresql://";
    private static final String DRIVER_POSTGRESQL = "org.postgresql.Driver";
    private static final int TIMEOUT_VALIDACION_SEGUNDOS = 5;

    private ConexionBD() {}

    public static void validarConfiguracion() throws SQLException {
        leerYValidarParametros(System.getenv(), ENV_DB_URL, ENV_DB_USER, ENV_DB_PASSWORD);
    }

    public static void validarConfiguracionTest() throws SQLException {
        leerYValidarParametros(System.getenv(), ENV_TEST_DB_URL, ENV_TEST_DB_USER, ENV_TEST_DB_PASSWORD);
    }

    public static Connection obtenerConexion() throws SQLException {
        // En entorno de prueba o desarrollo, si está seteada la URL de test se prioriza para no contaminar
        String testUrl = System.getenv(ENV_TEST_DB_URL);
        if (testUrl == null || testUrl.trim().isEmpty()) {
            testUrl = System.getProperty(ENV_TEST_DB_URL);
        }
        if (testUrl != null && !testUrl.trim().isEmpty()) {
            return obtenerConexionTest();
        }
        return abrirConexion(ENV_DB_URL, ENV_DB_USER, ENV_DB_PASSWORD);
    }

    public static Connection obtenerConexionTest() throws SQLException {
        return abrirConexion(ENV_TEST_DB_URL, ENV_TEST_DB_USER, ENV_TEST_DB_PASSWORD);
    }

    private static Connection abrirConexion(String envUrl, String envUser, String envPass) throws SQLException {
        ParametrosConexion params = leerYValidarParametros(System.getenv(), envUrl, envUser, envPass);
        asegurarDriverCargado();

        String urlSanitizada = sanitizarUrl(params.url);
        try {
            Connection conexion = DriverManager.getConnection(params.url, params.usuario, params.password);
            if (conexion == null) {
                throw new SQLException("DriverManager devolvió una conexión nula para la URL '" + urlSanitizada + "'.");
            }
            return conexion;
        } catch (SQLException e) {
            String mensajeDiagnostico = construirMensajeErrorConexion(urlSanitizada, params.usuario, e);
            LOGGER.log(Level.WARNING, "{0} [SQLState={1}, ErrorCode={2}]",
                    new Object[]{mensajeDiagnostico, e.getSQLState(), e.getErrorCode()});
            throw new SQLException(mensajeDiagnostico, e.getSQLState(), e.getErrorCode(), e);
        }
    }

    public static boolean comprobarConexion() throws SQLException {
        try (Connection conexion = obtenerConexion();
             Statement stmt = conexion.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {

            if (!conexion.isValid(TIMEOUT_VALIDACION_SEGUNDOS)) {
                throw new SQLException("La conexión JDBC se estableció pero fue reportada como inválida.");
            }
            if (rs.next() && rs.getInt(1) == 1) {
                LOGGER.info("Comprobación de conectividad PostgreSQL exitosa.");
                return true;
            }
            throw new SQLException("La consulta de comprobación 'SELECT 1' no devolvió el resultado esperado.");
        }
    }

    static ParametrosConexion leerYValidarParametros(Map<String, String> entorno,
                                                    String keyUrl, String keyUser, String keyPass) throws SQLException {
        String rawUrl = entorno != null ? entorno.get(keyUrl) : null;
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            rawUrl = System.getProperty(keyUrl);
        }
        String rawUser = entorno != null ? entorno.get(keyUser) : null;
        if (rawUser == null || rawUser.trim().isEmpty()) {
            rawUser = System.getProperty(keyUser);
        }
        String rawPassword = entorno != null ? entorno.get(keyPass) : null;
        if (rawPassword == null) {
            rawPassword = System.getProperty(keyPass);
        }

        // Fallback local controlado para entorno de desarrollo / prueba portable
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            if (ENV_TEST_DB_URL.equals(keyUrl)) {
                rawUrl = "jdbc:postgresql://127.0.0.1:55432/tpi_pa2_inf06_test";
            } else {
                rawUrl = "jdbc:postgresql://127.0.0.1:55432/tpi_pa2_r1_demo";
            }
        }
        if (rawUser == null || rawUser.trim().isEmpty()) {
            rawUser = "postgres";
        }
        if (rawPassword == null) {
            rawPassword = "";
        }

        String url = rawUrl.trim();
        String usuario = rawUser.trim();

        if (!url.startsWith(PREFIJO_JDBC_POSTGRESQL) || url.length() <= PREFIJO_JDBC_POSTGRESQL.length()) {
            String msg = "El valor de " + keyUrl + " ('" + sanitizarUrl(url)
                    + "') no tiene un formato JDBC de PostgreSQL válido. Formato esperado de referencia: "
                    + URL_REFERENCIA;
            LOGGER.warning(msg);
            throw new SQLException(msg);
        }

        return new ParametrosConexion(url, usuario, rawPassword);
    }

    private static void asegurarDriverCargado() throws SQLException {
        try {
            Class.forName(DRIVER_POSTGRESQL);
        } catch (ClassNotFoundException e) {
            String msg = "No se encontró el driver JDBC de PostgreSQL (" + DRIVER_POSTGRESQL
                    + ") en el classpath. Verifique las dependencias de Maven (org.postgresql:postgresql).";
            LOGGER.severe(msg);
            throw new SQLException(msg, e);
        }
    }

    private static String construirMensajeErrorConexion(String urlSanitizada, String usuario, SQLException e) {
        String sqlState = e.getSQLState();
        if ("08001".equals(sqlState) || "08004".equals(sqlState) || "08006".equals(sqlState)) {
            return "No se pudo conectar con el servidor PostgreSQL en '" + urlSanitizada
                    + "'. Verifique que PostgreSQL local esté instalado, en ejecución y aceptando conexiones en el puerto indicado.";
        }
        if ("28P01".equals(sqlState) || "28000".equals(sqlState)) {
            return "Autenticación rechazada por PostgreSQL para el usuario '" + usuario
                    + "'. Verifique el usuario y la contraseña configurados en las variables de entorno.";
        }
        if ("3D000".equals(sqlState)) {
            return "La base de datos indicada en '" + urlSanitizada
                    + "' no existe en el servidor PostgreSQL local. Verifique el nombre de la base o créela previamente.";
        }
        return "Error al establecer la conexión JDBC con PostgreSQL en '" + urlSanitizada
                + "' (SQLState: " + (sqlState != null ? sqlState : "N/D") + ").";
    }

    public static String sanitizarUrl(String url) {
        if (url == null) {
            return "";
        }
        return url.replaceAll("(?i)(password|passwd|pwd)=([^&;\\s]*)", "$1=***");
    }

    static final class ParametrosConexion {
        final String url;
        final String usuario;
        final String password;

        ParametrosConexion(String url, String usuario, String password) {
            this.url = url;
            this.usuario = usuario;
            this.password = password;
        }
    }
}
