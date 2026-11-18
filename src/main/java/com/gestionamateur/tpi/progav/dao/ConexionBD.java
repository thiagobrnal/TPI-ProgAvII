package com.gestionamateur.tpi.progav.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Componente centralizado de infraestructura JDBC para obtener conexiones
 * hacia la base de datos PostgreSQL local en Release 1 (Desktop).
 *
 * SEGURIDAD DE ENTORNO DE PRUEBAS (fail-closed):
 * la conexion de pruebas NUNCA puede resolverse por valor por defecto. Requiere una
 * URL explicita (variable de entorno o propiedad del sistema) y el nombre de base
 * resultante se valida contra la lista de bases protegidas antes de abrir la conexion.
 * Si la base efectiva no coincide con la solicitada, la operacion se aborta.
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

    /**
     * Bases de datos reservadas: nunca pueden ser destino de una conexion de pruebas.
     * - tpi_pa2_r1_demo  : base de demostracion de la aplicacion.
     * - tpi_pa2_inf06_test: base historica de pruebas reservada por el proyecto.
     * - tpi_pa2_r1_func_test_20261111_192226 : base temporal historica de la auditoria funcional.
     */
    public static final Set<String> BASES_PROTEGIDAS = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(
                    "tpi_pa2_r1_demo",
                    "tpi_pa2_inf06_test",
                    "tpi_pa2_r1_func_test_20261111_192226")));

    private ConexionBD() {}

    // ------------------------------------------------------------------
    // Configuracion de la aplicacion (base legitima de ejecucion)
    // ------------------------------------------------------------------

    public static void validarConfiguracion() throws SQLException {
        leerYValidarParametros(System.getenv(), ENV_DB_URL, ENV_DB_USER, ENV_DB_PASSWORD);
    }

    /**
     * Valida la configuracion de pruebas. Es fail-closed: si no hay URL explicita
     * de base de pruebas, falla con un mensaje accionable antes de abrir conexiones.
     */
    public static void validarConfiguracionTest() throws SQLException {
        leerParametrosDePruebas();
    }

    public static Connection obtenerConexion() throws SQLException {
        // Si el entorno define explicitamente una URL de pruebas, se prioriza para no contaminar
        // la base legitima de la aplicacion durante una corrida de pruebas.
        if (resolverExplicito(ENV_TEST_DB_URL) != null) {
            return obtenerConexionTest();
        }
        return abrirConexion(ENV_DB_URL, ENV_DB_USER, ENV_DB_PASSWORD);
    }

    /**
     * Conexion de pruebas. No admite valores por defecto: la URL debe Proveerse
     * de forma explicita y el nombre de base se valida contra las bases protegidas.
     */
    public static Connection obtenerConexionTest() throws SQLException {
        ParametrosConexion params = leerParametrosDePruebas();
        Connection conexion = abrirConexionConParametros(params);
        verificarBaseEfectiva(conexion, params.url);
        return conexion;
    }

    /**
     * Verifica que la base de pruebas efectiva sea exactamente la solicitada y que
     * no sea una base protegida. Debe invocarse al inicio de cada suite de integracion.
     *
     * @return nombre de la base de pruebas efectiva
     */
    public static String validarDestinoDePruebas() throws SQLException {
        ParametrosConexion params = leerParametrosDePruebas();
        try (Connection conexion = abrirConexionConParametros(params)) {
            return verificarBaseEfectiva(conexion, params.url);
        }
    }

    // ------------------------------------------------------------------
    // Validacion de base protegida
    // ------------------------------------------------------------------

    /**
     * Extrae el nombre de la base de una URL JDBC de PostgreSQL.
     */
    public static String extraerNombreBase(String url) {
        if (url == null) {
            return null;
        }
        String resto = url.trim();
        if (resto.startsWith(PREFIJO_JDBC_POSTGRESQL)) {
            resto = resto.substring(PREFIJO_JDBC_POSTGRESQL.length());
        }
        int corte = resto.indexOf('/');
        if (corte < 0) {
            return null;
        }
        String nombre = resto.substring(corte + 1);
        int interrogante = nombre.indexOf('?');
        if (interrogante >= 0) {
            nombre = nombre.substring(0, interrogante);
        }
        nombre = nombre.trim();
        return nombre.isEmpty() ? null : nombre;
    }

    /**
     * Verifica que el nombre de base indicado sea admisible como destino de pruebas.
     *
     * @throws SQLException si la base es reservada o no puede determinarse
     */
    public static void validarBaseDePruebas(String nombreBase) throws SQLException {
        if (nombreBase == null || nombreBase.trim().isEmpty()) {
            throw new SQLException("No se pudo determinar el nombre de la base de datos de pruebas a partir de la URL configurada.");
        }
        String normalizado = nombreBase.trim().toLowerCase(Locale.ROOT);
        if (BASES_PROTEGIDAS.contains(normalizado)) {
            throw new SQLException("Destino de pruebas rechazado: la base '" + nombreBase
                    + "' es una base reservada del proyecto y no puede utilizarse para ejecutar pruebas. "
                    + "Defina " + ENV_TEST_DB_URL + " con una base de pruebas exclusiva y descartable.");
        }
        String baseAplicacion = nombreBaseDeAplicacion();
        if (baseAplicacion != null && baseAplicacion.equalsIgnoreCase(nombreBase.trim())) {
            throw new SQLException("Destino de pruebas rechazado: la base '" + nombreBase
                    + "' es la misma que usa la aplicacion (" + ENV_DB_URL + "). "
                    + "Las pruebas requieren una base exclusiva y descartable.");
        }
    }

    private static String nombreBaseDeAplicacion() {
        String url = resolverExplicito(ENV_DB_URL);
        if (url == null) {
            return null;
        }
        return extraerNombreBase(url);
    }

    /**
     * Compara la base realmente conectada con la base solicitada.
     */
    private static String verificarBaseEfectiva(Connection conexion, String urlSolicitada) throws SQLException {
        String solicitada = extraerNombreBase(urlSolicitada);
        String efectiva;
        try (Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery("SELECT current_database()")) {
            rs.next();
            efectiva = rs.getString(1);
        }
        if (solicitada != null && efectiva != null && !solicitada.equalsIgnoreCase(efectiva)) {
            throw new SQLException("Destino de pruebas inesperado: se solicito '" + solicitada
                    + "' pero la conexion reporta '" + efectiva + "'. Se aborta la operacion.");
        }
        LOGGER.info("Base de pruebas efectiva verificada: " + efectiva);
        return efectiva;
    }

    // ------------------------------------------------------------------
    // Resolucion de parametros
    // ------------------------------------------------------------------

    /**
     * Resuelve un valor explicito desde el entorno o desde las propiedades del sistema.
     * Devuelve null cuando no esta definido o cuando el valor es una referencia sin resolver
     * (por ejemplo "${env.TPI_TEST_DB_URL}" cuando la variable de entorno no existe).
     */
    private static String resolverExplicito(String clave) {
        String valor = System.getenv(clave);
        if (valor == null || valor.trim().isEmpty()) {
            valor = System.getProperty(clave);
        }
        if (valor == null) {
            return null;
        }
        valor = valor.trim();
        if (valor.isEmpty() || valor.startsWith("${") || valor.contains("${")) {
            return null;
        }
        return valor;
    }

    /**
     * Resuelve los parametros de la conexion de pruebas a partir del entorno efectivo.
     * La URL debe estar definida de forma explicita: no existe ningun valor por defecto.
     */
    private static ParametrosConexion leerParametrosDePruebas() throws SQLException {
        Map<String, String> entorno = new java.util.HashMap<>();
        for (String clave : new String[]{ENV_TEST_DB_URL, ENV_TEST_DB_USER, ENV_TEST_DB_PASSWORD}) {
            String valor = System.getenv(clave);
            if (valor == null || valor.trim().isEmpty()) {
                valor = System.getProperty(clave);
            }
            if (valor != null) {
                entorno.put(clave, valor);
            }
        }
        return leerParametrosDePruebas(entorno);
    }

    /**
     * Resuelve los parametros de la conexion de pruebas usando exclusivamente el entorno indicado.
     * Pensado para verificar el comportamiento fail-closed de forma determinista.
     *
     * @throws SQLException si la URL no esta definida, no es valida o apunta a una base reservada
     */
    static ParametrosConexion leerParametrosDePruebas(Map<String, String> entorno) throws SQLException {
        String url = valorExplicito(entorno, ENV_TEST_DB_URL);
        if (url == null) {
            throw new SQLException("Configuracion de pruebas incompleta: no esta definida " + ENV_TEST_DB_URL
                    + ". Las pruebas requieren una URL explicita de una base de datos exclusiva y descartable; "
                    + "no se aplicara ningun valor por defecto. "
                    + "Ejemplo: " + ENV_TEST_DB_URL + "=jdbc:postgresql://127.0.0.1:55432/<base_de_pruebas>.");
        }
        String usuario = valorExplicito(entorno, ENV_TEST_DB_USER);
        if (usuario == null) {
            usuario = "postgres";
        }
        String password = valorExplicito(entorno, ENV_TEST_DB_PASSWORD);
        if (password == null) {
            password = "";
        }
        if (!url.startsWith(PREFIJO_JDBC_POSTGRESQL)) {
            throw new SQLException("El valor de " + ENV_TEST_DB_URL + " no tiene un formato JDBC de PostgreSQL valido.");
        }
        validarBaseDePruebas(extraerNombreBase(url));
        return new ParametrosConexion(url, usuario, password);
    }

    /**
     * Extrae un valor explicito del entorno indicado. Devuelve null si no esta definido,
     * esta vacio o es una referencia sin resolver (por ejemplo "${env.TPI_TEST_DB_URL}").
     */
    private static String valorExplicito(Map<String, String> entorno, String clave) {
        String valor = entorno != null ? entorno.get(clave) : null;
        if (valor == null) {
            return null;
        }
        valor = valor.trim();
        if (valor.isEmpty() || valor.contains("${")) {
            return null;
        }
        return valor;
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
                // Sin valor por defecto: la conexion de pruebas es fail-closed.
                throw new SQLException("Configuracion de pruebas incompleta: no esta definida " + ENV_TEST_DB_URL
                        + ". No se aplicara ningun valor por defecto.");
            }
            rawUrl = "jdbc:postgresql://127.0.0.1:55432/tpi_pa2_r1_demo";
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
            String msg = "El valor de " + keyUrl + " ('" + sanitizarUrl(url) + "') no tiene un formato JDBC de PostgreSQL v"
                    + "alido. Formato esperado de referencia: " + URL_REFERENCIA;
            LOGGER.warning(msg);
            throw new SQLException(msg);
        }

        return new ParametrosConexion(url, usuario, rawPassword);
    }

    private static Connection abrirConexion(String envUrl, String envUser, String envPass) throws SQLException {
        ParametrosConexion params = leerYValidarParametros(System.getenv(), envUrl, envUser, envPass);
        return abrirConexionConParametros(params);
    }

    private static Connection abrirConexionConParametros(ParametrosConexion params) throws SQLException {
        asegurarDriverCargado();

        String urlSanitizada = sanitizarUrl(params.url);
        try {
            Connection conexion = DriverManager.getConnection(params.url, params.usuario, params.password);
            if (conexion == null) {
                throw new SQLException("DriverManager devolvio una conexion nula para la URL '" + urlSanitizada + "'.");
            }
            return conexion;
        } catch (SQLException e) {
            String mensajeDiagnostico = construirMensajeErrorConexion(urlSanitizada, params.usuario, e);
            LOGGER.log(Level.WARNING, "{0} [SQLState={1}, ErrorCode={2}]",
                    new Object[]{mensajeDiagnostico, e.getSQLState(), e.getErrorCode()});
            throw new SQLException(mensajeDiagnostico, e.getSQLState(), e.getErrorCode(), e);
        }
    }

    private static void asegurarDriverCargado() throws SQLException {
        try {
            Class.forName(DRIVER_POSTGRESQL);
        } catch (ClassNotFoundException e) {
            String msg = "No se encontro el driver JDBC de PostgreSQL (" + DRIVER_POSTGRESQL + ") en el classpath. "
                    + "Verifique las dependencias de Maven (org.postgresql:postgresql).";
            LOGGER.severe(msg);
            throw new SQLException(msg);
        }
    }

    public static boolean comprobarConexion() throws SQLException {
        try (Connection conexion = obtenerConexion();
             Statement stmt = conexion.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {

            if (!conexion.isValid(TIMEOUT_VALIDACION_SEGUNDOS)) {
                throw new SQLException("La conexion JDBC se establecio pero fue reportada como invalida.");
            }
            if (rs.next() && rs.getInt(1) == 1) {
                LOGGER.info("Comprobacion de conectividad PostgreSQL exitosa.");
                return true;
            }
            throw new SQLException("La consulta de comprobacion 'SELECT 1' no devolvio el resultado esperado.");
        }
    }

    public static String sanitizarUrl(String url) {
        if (url == null) {
            return "";
        }
        return url.replaceAll("(?i)(password|passwd|pwd)=([^&;\\s]*)", "$1=***");
    }

    private static String construirMensajeErrorConexion(String urlSanitizada, String usuario, SQLException e) {
        String sqlState = e.getSQLState();
        if ("08001".equals(sqlState) || "08004".equals(sqlState) || "08006".equals(sqlState)) {
            return "No se pudo conectar con el servidor PostgreSQL en '" + urlSanitizada + "'. "
                    + "Verifique que PostgreSQL local este instalado, en ejecucion y aceptando conexiones en el puerto indicado.";
        }
        if ("28P01".equals(sqlState) || "28000".equals(sqlState)) {
            return "Autenticacion rechazada por PostgreSQL para el usuario '" + usuario + "'. "
                    + "Verifique el usuario y la contrasena configurados en las variables de entorno.";
        }
        if ("3D000".equals(sqlState)) {
            return "La base de datos indicada en '" + urlSanitizada + "' no existe en el servidor PostgreSQL local. "
                    + "Verifique el nombre de la base o creela previamente.";
        }
        return "No se pudo establecer la conexion JDBC a PostgreSQL en '" + urlSanitizada + "' "
                + "[SQLState: " + (sqlState != null ? sqlState : "N/D") + "].";
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
