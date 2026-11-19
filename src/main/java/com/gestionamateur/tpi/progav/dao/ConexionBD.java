package com.gestionamateur.tpi.progav.dao;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
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
import java.util.Properties;
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

    /** Propiedad de sistema opcional (usada por run_app.bat) para fijar el modo de ejecucion. */
    public static final String PROP_MODO_EJECUCION = "tpi.conexion.modo";

    /**
     * URL de referencia para mensajes de diagnostico. No constituye un valor por defecto.
     */
    public static final String URL_REFERENCIA = "jdbc:postgresql://127.0.0.1:55432/<base_de_ejecucion>";
    private static final String PREFIJO_JDBC_POSTGRESQL = "jdbc:postgresql://";
    private static final String DRIVER_POSTGRESQL = "org.postgresql.Driver";
    private static final int TIMEOUT_VALIDACION_SEGUNDOS = 5;

    private enum ModoEjecucion {
        NO_DEFINIDO,
        APLICACION,
        PRUEBAS,
        INVALIDO,
        CONTRADICTORIO
    }

    private static volatile ModoEjecucion modoEjecucion = detectarModoInicial();

    /**
     * Bases de datos reservadas del proyecto: nunca pueden ser destino de una conexion de
     * pruebas ni de una conexion de ejecucion de la aplicacion.
     */
    public static final Set<String> BASES_PROTEGIDAS = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(
                    "tpi_pa2_r1_demo",
                    "tpi_pa2_inf06_test",
                    "tpi_pa2_r1_func_test_20261111_192226",
                    "tpi_pa2_r1_fix_test_20261111",
                    "tpi_pa2_r1_reports_verify_20261112",
                    "tpi_pa2_r1_pdfzip_fix_test_20261112",
                    "tpi_pa2_r1_netbeans_test_20261112",
                    "tpi_pa2_r1_netbeans_run_20261112",
                    "tpi_pa2_r1_delivery_audit_test_20261112")));

    private ConexionBD() {}

    private static ModoEjecucion detectarModoInicial() {
        String prop = System.getProperty(PROP_MODO_EJECUCION);
        if (prop == null || prop.trim().isEmpty()) {
            return ModoEjecucion.NO_DEFINIDO;
        }
        String normalizado = prop.trim();
        if ("aplicacion".equalsIgnoreCase(normalizado)) {
            return ModoEjecucion.APLICACION;
        }
        if ("pruebas".equalsIgnoreCase(normalizado)) {
            return ModoEjecucion.PRUEBAS;
        }
        return ModoEjecucion.INVALIDO;
    }

    private static synchronized ModoEjecucion obtenerModoEfectivo() {
        ModoEjecucion actual = modoEjecucion;
        if (actual == ModoEjecucion.CONTRADICTORIO || actual == ModoEjecucion.INVALIDO) {
            return actual;
        }
        ModoEjecucion propModo = detectarModoInicial();
        if (propModo == ModoEjecucion.INVALIDO) {
            modoEjecucion = ModoEjecucion.INVALIDO;
            return ModoEjecucion.INVALIDO;
        }
        if (actual != ModoEjecucion.NO_DEFINIDO) {
            if (propModo != ModoEjecucion.NO_DEFINIDO && propModo != actual) {
                modoEjecucion = ModoEjecucion.CONTRADICTORIO;
                return ModoEjecucion.CONTRADICTORIO;
            }
            return actual;
        }
        if (propModo == ModoEjecucion.APLICACION || propModo == ModoEjecucion.PRUEBAS) {
            modoEjecucion = propModo;
        }
        return propModo;
    }

    /** Activa explicitamente el modo de ejecucion de la aplicacion (nunca usa TPI_TEST_DB_URL). */
    public static void activarModoAplicacion() {
        activarModoExplicito(ModoEjecucion.APLICACION);
    }

    /** Activa explicitamente el modo de pruebas (resuelve exclusivamente contra TPI_TEST_DB_URL). */
    public static void activarModoPruebas() {
        activarModoExplicito(ModoEjecucion.PRUEBAS);
    }

    private static synchronized void activarModoExplicito(ModoEjecucion objetivo) {
        if (modoEjecucion == ModoEjecucion.CONTRADICTORIO) {
            throw new IllegalStateException("Configuracion contradictoria de modo de ejecucion detectada previamente.");
        }
        if (modoEjecucion == ModoEjecucion.INVALIDO) {
            throw new IllegalStateException("Modo de ejecucion en estado INVALIDO.");
        }
        ModoEjecucion propModo = detectarModoInicial();
        if (propModo == ModoEjecucion.INVALIDO) {
            modoEjecucion = ModoEjecucion.INVALIDO;
            throw new IllegalStateException("Modo de ejecucion invalido en propiedad '" + PROP_MODO_EJECUCION
                    + "'. Valores permitidos: 'aplicacion' o 'pruebas'.");
        }
        if (modoEjecucion != ModoEjecucion.NO_DEFINIDO && modoEjecucion != objetivo) {
            ModoEjecucion previo = modoEjecucion;
            if (propModo != ModoEjecucion.NO_DEFINIDO && propModo != previo) {
                modoEjecucion = ModoEjecucion.CONTRADICTORIO;
            }
            throw new IllegalStateException("Transicion silenciosa prohibida: no se puede cambiar el modo activo "
                    + previo + " a " + objetivo + " sin restablecer explicitamente.");
        }
        if (propModo != ModoEjecucion.NO_DEFINIDO && propModo != objetivo) {
            modoEjecucion = ModoEjecucion.CONTRADICTORIO;
            throw new IllegalStateException("Configuracion contradictoria: la propiedad '" + PROP_MODO_EJECUCION
                    + "' exige " + propModo + " pero se intento activar " + objetivo + ".");
        }
        modoEjecucion = objetivo;
    }

    /** @return {@code true} si el proceso actual esta marcado en modo de pruebas. */
    public static boolean esProcesoDePruebas() {
        return obtenerModoEfectivo() == ModoEjecucion.PRUEBAS;
    }

    /**
     * Restaura el modo de ejecucion al estado inicial fail-closed ({@code NO_DEFINIDO} salvo
     * propiedad explicita {@code tpi.conexion.modo}). Uso previsto para limpieza determinista en JUnit.
     */
    public static synchronized void restablecerModoPorDefecto() {
        modoEjecucion = detectarModoInicial();
    }

    /** Alias package-private de compatibilidad para pruebas del paquete dao. */
    static void restablecerModoParaPruebas() {
        restablecerModoPorDefecto();
    }

    // ------------------------------------------------------------------
    // Configuracion de la aplicacion (base legitima de ejecucion)
    // ------------------------------------------------------------------

    public static void validarConfiguracion() throws SQLException {
        ModoEjecucion modo = obtenerModoEfectivo();
        asegurarModoOperativo(modo);
        if (modo != ModoEjecucion.APLICACION) {
            throw new SQLException("Validacion de configuracion de aplicacion rechazada: el modo activo es " + modo + ".");
        }
        leerYValidarParametros(capturarEntornoEfectivo(), ENV_DB_URL, ENV_DB_USER, ENV_DB_PASSWORD);
    }

    /**
     * Valida la configuracion de pruebas. Es fail-closed: exige modo {@code PRUEBAS} y URL explicita
     * de base de pruebas antes de abrir conexiones.
     */
    public static void validarConfiguracionTest() throws SQLException {
        asegurarModoPruebasParaValidacion();
        validarConfiguracionDestinoDePruebas(capturarEntornoEfectivo());
    }

    public static Connection obtenerConexion() throws SQLException {
        return obtenerConexionCon(capturarEntornoEfectivo(), ConexionBD::conectarConDriverManager);
    }

    static Connection obtenerConexionCon(Map<String, String> entorno, ConectorJdbc conector) throws SQLException {
        ModoEjecucion modo = obtenerModoEfectivo();
        asegurarModoOperativo(modo);
        ParametrosConexion params = resolverParametrosPorModo(modo, entorno);
        if (modo == ModoEjecucion.PRUEBAS) {
            return abrirYVerificarPrueba(params, conector);
        }
        return abrirConexionConParametros(params, conector);
    }

    static ParametrosConexion resolverParametrosActivos(Map<String, String> entorno) throws SQLException {
        return resolverParametrosPorModo(obtenerModoEfectivo(), entorno);
    }

    private static void asegurarModoOperativo(ModoEjecucion modo) throws SQLException {
        if (modo == ModoEjecucion.INVALIDO) {
            throw new SQLException("Modo de ejecucion invalido en propiedad '" + PROP_MODO_EJECUCION
                    + "'. Valores permitidos: 'aplicacion' o 'pruebas'.");
        }
        if (modo == ModoEjecucion.CONTRADICTORIO) {
            throw new SQLException("Configuracion contradictoria en propiedad '" + PROP_MODO_EJECUCION
                    + "' respecto al modo de ejecucion activo.");
        }
        if (modo == ModoEjecucion.NO_DEFINIDO) {
            throw new SQLException("Modo de ejecucion no definido: active explicitamente "
                    + "ConexionBD.activarModoAplicacion() o ConexionBD.activarModoPruebas() (o defina -D"
                    + PROP_MODO_EJECUCION + "=aplicacion|pruebas) antes de resolver o abrir conexiones.");
        }
    }

    private static ParametrosConexion resolverParametrosPorModo(ModoEjecucion modo, Map<String, String> entorno)
            throws SQLException {
        asegurarModoOperativo(modo);
        if (modo == ModoEjecucion.APLICACION) {
            return leerYValidarParametros(entorno, ENV_DB_URL, ENV_DB_USER, ENV_DB_PASSWORD);
        }
        return leerParametrosDePruebas(entorno);
    }

    /**
     * Conexion de pruebas. Exige modo {@code PRUEBAS} explicito y URL explicita de base descartable.
     */
    public static Connection obtenerConexionTest() throws SQLException {
        return obtenerConexionTestCon(capturarEntornoEfectivo(), ConexionBD::conectarConDriverManager);
    }

    static Connection obtenerConexionTestCon(Map<String, String> entorno, ConectorJdbc conector) throws SQLException {
        ModoEjecucion modo = obtenerModoEfectivo();
        asegurarModoOperativo(modo);
        if (modo == ModoEjecucion.APLICACION) {
            throw new SQLException("Operacion de pruebas rechazada: el proceso esta en modo APLICACION.");
        }
        return abrirYVerificarPrueba(leerParametrosDePruebas(entorno), conector);
    }

    private static Connection abrirYVerificarPrueba(ParametrosConexion params, ConectorJdbc conector) throws SQLException {
        Connection conexion = abrirConexionConParametros(params, conector);
        try {
            verificarBaseEfectiva(conexion, params.url);
            return conexion;
        } catch (Throwable ex) {
            try {
                conexion.close();
            } catch (SQLException suppressed) {
                ex.addSuppressed(suppressed);
            }
            throw ex;
        }
    }

    private static void asegurarModoPruebasParaValidacion() throws SQLException {
        ModoEjecucion modo = obtenerModoEfectivo();
        if (modo != ModoEjecucion.PRUEBAS) {
            throw new SQLException("Validacion de destino de pruebas rechazada: el modo actual es " + modo
                    + ". Active explicitamente ConexionBD.activarModoPruebas() antes de validar el destino.");
        }
    }

    /**
     * Valida de forma pura la configuracion de pruebas usando exclusivamente el mapa indicado,
     * exigiendo que el proceso se encuentre en modo {@code PRUEBAS}. No abre conexiones JDBC.
     */
    static ParametrosConexion validarConfiguracionDestinoDePruebas(Map<String, String> entorno) throws SQLException {
        asegurarModoPruebasParaValidacion();
        return leerParametrosDePruebas(entorno);
    }

    /**
     * Valida que el proceso ya se encuentre en modo {@code PRUEBAS}, que la URL de pruebas
     * configurada no apunte a una base reservada ni de aplicacion y que la base efectiva conectada
     * coincida con la solicitada. No activa el modo por si mismo.
     *
     * @return nombre de la base de pruebas efectiva
     */
    public static String validarDestinoDePruebas() throws SQLException {
        asegurarModoPruebasParaValidacion();
        ParametrosConexion params = validarConfiguracionDestinoDePruebas(capturarEntornoEfectivo());
        try (Connection conexion = abrirConexionConParametros(params, ConexionBD::conectarConDriverManager)) {
            return verificarBaseEfectiva(conexion, params.url);
        }
    }

    // ------------------------------------------------------------------
    // Validacion de base protegida
    // ------------------------------------------------------------------

    /**
     * Extrae y decodifica el nombre efectivo de la base de una URL JDBC de PostgreSQL,
     * contrastando con {@code org.postgresql.Driver.parseURL()} en memoria y rechazando
     * autoridades vacias, dobles escapes, caracteres prohibidos o parametros de destino en query.
     */
    public static String extraerNombreBase(String url) {
        if (url == null || contieneCaracterControl(url)) {
            return null;
        }
        String trimmed = url.trim();
        if (!trimmed.startsWith(PREFIJO_JDBC_POSTGRESQL)) {
            return null;
        }
        int interrogante = trimmed.indexOf('?');
        String sinQuery = (interrogante >= 0) ? trimmed.substring(0, interrogante) : trimmed;
        String query = (interrogante >= 0) ? trimmed.substring(interrogante + 1) : "";

        String resto = sinQuery.substring(PREFIJO_JDBC_POSTGRESQL.length());
        int corte = resto.indexOf('/');
        if (corte <= 0 || resto.indexOf('/', corte + 1) >= 0) {
            return null;
        }
        String autoridad = resto.substring(0, corte).trim();
        if (autoridad.isEmpty()) {
            return null;
        }
        String segmentoBaseRaw = resto.substring(corte + 1);
        String baseDecodificada = decodificarPorcentaje(segmentoBaseRaw);
        if (baseDecodificada == null) {
            return null;
        }
        if (contieneParametroDestinoEnQuery(query)) {
            return null;
        }
        Properties defaultsSinIo = new Properties();
        defaultsSinIo.setProperty("password", "");
        Properties driverProps = org.postgresql.Driver.parseURL(trimmed, defaultsSinIo);
        if (driverProps == null || driverProps.getProperty("service") != null) {
            return null;
        }
        String pgDbName = driverProps.getProperty("PGDBNAME");
        String pgHost = driverProps.getProperty("PGHOST");
        String pgPort = driverProps.getProperty("PGPORT");
        if (pgDbName == null || !pgDbName.equals(baseDecodificada)
                || pgHost == null || pgHost.trim().isEmpty()
                || pgPort == null || pgPort.trim().isEmpty()) {
            return null;
        }
        return baseDecodificada;
    }

    private static boolean contieneCaracterControl(String texto) {
        for (int i = 0; i < texto.length(); i++) {
            if (texto.charAt(i) < 0x20) {
                return true;
            }
        }
        return false;
    }

    private static boolean contieneParametroDestinoEnQuery(String query) {
        if (query == null || query.isEmpty()) {
            return false;
        }
        for (String par : query.split("&")) {
            if (par.isEmpty()) {
                continue;
            }
            int eq = par.indexOf('=');
            String claveRaw = (eq >= 0 ? par.substring(0, eq) : par);
            String claveTrim = claveRaw.trim();
            if (claveTrim.isEmpty() || claveTrim.indexOf('%') >= 0 || contieneCaracterControl(claveRaw)) {
                return true;
            }
            String clave = claveTrim.toLowerCase(Locale.ROOT);
            if ("dbname".equals(clave) || "pgdbname".equals(clave)
                    || "host".equals(clave) || "pghost".equals(clave)
                    || "port".equals(clave) || "pgport".equals(clave)
                    || "service".equals(clave)) {
                return true;
            }
            if (eq >= 0 && decodificarPorcentajeValorQuery(par.substring(eq + 1)) == null) {
                return true;
            }
        }
        return false;
    }

    private static String decodificarPorcentajeValorQuery(String valorRaw) {
        if (valorRaw == null || contieneCaracterControl(valorRaw)) {
            return null;
        }
        if (valorRaw.indexOf('%') < 0) {
            return valorRaw;
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(valorRaw.length());
        for (int i = 0; i < valorRaw.length(); i++) {
            char c = valorRaw.charAt(i);
            if (c == '%') {
                if (i + 2 >= valorRaw.length()) {
                    return null;
                }
                int hi = Character.digit(valorRaw.charAt(i + 1), 16);
                int lo = Character.digit(valorRaw.charAt(i + 2), 16);
                if (hi < 0 || lo < 0) {
                    return null;
                }
                bytes.write((hi << 4) | lo);
                i += 2;
            } else {
                byte[] utf8 = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
                bytes.write(utf8, 0, utf8.length);
            }
        }
        String decodificado = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        return contieneCaracterControl(decodificado) ? null : decodificado;
    }

    private static String decodificarPorcentaje(String texto) {
        if (texto == null || contieneCaracterControl(texto)) {
            return null;
        }
        if (texto.indexOf('/') >= 0 || texto.indexOf('\\') >= 0 || texto.indexOf('+') >= 0) {
            return null;
        }
        String limpio = texto.trim();
        if (limpio.isEmpty()) {
            return null;
        }
        if (limpio.indexOf('%') < 0) {
            return limpio;
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(limpio.length());
        for (int i = 0; i < limpio.length(); i++) {
            char c = limpio.charAt(i);
            if (c == '%') {
                if (i + 2 >= limpio.length()) {
                    return null;
                }
                int hi = Character.digit(limpio.charAt(i + 1), 16);
                int lo = Character.digit(limpio.charAt(i + 2), 16);
                if (hi < 0 || lo < 0) {
                    return null;
                }
                bytes.write((hi << 4) | lo);
                i += 2;
            } else {
                byte[] utf8 = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
                bytes.write(utf8, 0, utf8.length);
            }
        }
        String decodificadoBruto = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        if (contieneCaracterControl(decodificadoBruto)
                || decodificadoBruto.indexOf('%') >= 0
                || decodificadoBruto.indexOf('/') >= 0
                || decodificadoBruto.indexOf('\\') >= 0
                || decodificadoBruto.indexOf('+') >= 0) {
            return null;
        }
        String decodificado = decodificadoBruto.trim();
        return decodificado.isEmpty() ? null : decodificado;
    }

    /**
     * Verifica que el nombre de base indicado sea admisible como destino de pruebas.
     *
     * @throws SQLException si la base es reservada o no puede determinarse
     */
    public static void validarBaseDePruebas(String nombreBase) throws SQLException {
        String decodificado = decodificarPorcentaje(nombreBase);
        if (decodificado == null) {
            throw new SQLException("No se pudo determinar el nombre de la base de datos de pruebas a partir de la URL configurada.");
        }
        String normalizado = decodificado.toLowerCase(Locale.ROOT);
        if (BASES_PROTEGIDAS.contains(normalizado)) {
            throw new SQLException("Destino de pruebas rechazado: la base '" + decodificado
                    + "' es una base reservada del proyecto y no puede utilizarse para ejecutar pruebas. "
                    + "Defina " + ENV_TEST_DB_URL + " con una base de pruebas exclusiva y descartable.");
        }
    }

    /**
     * Compara la base realmente conectada con la base solicitada.
     */
    static String verificarBaseEfectiva(Connection conexion, String urlSolicitada) throws SQLException {
        String solicitada = extraerNombreBase(urlSolicitada);
        if (solicitada == null || solicitada.trim().isEmpty()) {
            throw new SQLException("No se pudo determinar la base de pruebas solicitada en la URL configurada.");
        }
        String efectiva;
        try (Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery("SELECT current_database()")) {
            if (!rs.next()) {
                throw new SQLException("PostgreSQL no devolvio el nombre de la base efectiva en current_database().");
            }
            efectiva = rs.getString(1);
        }
        if (efectiva == null || efectiva.trim().isEmpty()) {
            throw new SQLException("PostgreSQL devolvio una base efectiva nula o vacia al validar el destino de pruebas.");
        }
        validarBaseDePruebas(efectiva);
        if (!solicitada.trim().equalsIgnoreCase(efectiva.trim())) {
            throw new SQLException("Destino de pruebas inesperado: se solicito '" + solicitada
                    + "' pero la conexion reporta '" + efectiva + "'. Se aborta la operacion.");
        }
        LOGGER.info("Base de pruebas efectiva verificada: " + efectiva);
        return efectiva.trim();
    }

    // ------------------------------------------------------------------
    // Resolucion de parametros
    // ------------------------------------------------------------------

    private static Map<String, String> capturarEntornoEfectivo() {
        Map<String, String> entorno = new java.util.HashMap<>();
        for (String clave : new String[]{
                ENV_DB_URL, ENV_DB_USER, ENV_DB_PASSWORD,
                ENV_TEST_DB_URL, ENV_TEST_DB_USER, ENV_TEST_DB_PASSWORD}) {
            String valor = System.getenv(clave);
            boolean esClavePassword = ENV_DB_PASSWORD.equals(clave) || ENV_TEST_DB_PASSWORD.equals(clave);
            if (valor == null || (!esClavePassword && valor.trim().isEmpty())) {
                String prop = System.getProperty(clave);
                if (prop != null) {
                    valor = prop;
                }
            }
            if (valor != null) {
                entorno.put(clave, valor);
            }
        }
        return entorno;
    }

    /**
     * Resuelve los parametros de la conexion de pruebas a partir del entorno efectivo.
     * La URL debe estar definida de forma explicita: no existe ningun valor por defecto.
     */
    private static ParametrosConexion leerParametrosDePruebas() throws SQLException {
        return leerParametrosDePruebas(capturarEntornoEfectivo());
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
        String password = valorPasswordExplicito(entorno, ENV_TEST_DB_PASSWORD);
        if (password == null) {
            password = "";
        }
        if (!url.startsWith(PREFIJO_JDBC_POSTGRESQL)) {
            throw new SQLException("El valor de " + ENV_TEST_DB_URL + " no tiene un formato JDBC de PostgreSQL valido.");
        }
        String nombreBase = extraerNombreBase(url);
        validarBaseDePruebas(nombreBase);
        String urlAppEnEntorno = valorExplicito(entorno, ENV_DB_URL);
        if (urlAppEnEntorno != null) {
            String baseApp = extraerNombreBase(urlAppEnEntorno);
            if (baseApp == null) {
                throw new SQLException("Destino de pruebas rechazado: no se pudo determinar la base configurada en "
                        + ENV_DB_URL + " para verificar colisiones de forma segura.");
            }
            if (baseApp.equalsIgnoreCase(nombreBase)) {
                throw new SQLException("Destino de pruebas rechazado: la base '" + nombreBase
                        + "' es la misma que usa la aplicacion (" + ENV_DB_URL + "). "
                        + "Las pruebas requieren una base exclusiva y descartable.");
            }
        }
        return new ParametrosConexion(url, usuario, password);
    }

    private static boolean esPlaceholderSinResolver(String valorTrimmed) {
        return valorTrimmed.matches("\\$\\{(?:env\\.)?[A-Za-z_][A-Za-z0-9_.-]*\\}");
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
        String trimmed = valor.trim();
        if (trimmed.isEmpty() || esPlaceholderSinResolver(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private static String valorPasswordExplicito(Map<String, String> entorno, String clave) {
        String valor = entorno != null ? entorno.get(clave) : null;
        if (valor == null) {
            return null;
        }
        String trimmed = valor.trim();
        if (esPlaceholderSinResolver(trimmed)) {
            return null;
        }
        return valor;
    }

    static ParametrosConexion leerYValidarParametros(Map<String, String> entorno,
                                                    String keyUrl, String keyUser, String keyPass) throws SQLException {
        if (!ENV_DB_URL.equals(keyUrl)) {
            throw new IllegalArgumentException("leerYValidarParametros solo admite " + ENV_DB_URL + " como clave de URL.");
        }
        String rawUrl = valorExplicito(entorno, keyUrl);
        String rawUser = valorExplicito(entorno, keyUser);
        String rawPassword = valorPasswordExplicito(entorno, keyPass);

        // Sin valor por defecto: la aplicacion nunca se conecta a una base
        // historica o reservada si falta la variable de entorno explicita.
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            throw new SQLException("Configuracion de la aplicacion incompleta: no esta definida " + keyUrl
                    + ". La aplicacion no aplica ningun valor por defecto y no se conecta a ninguna base de datos "
                    + "historica o reservada. Defina " + keyUrl + " en el entorno, por ejemplo: set " + keyUrl
                    + "=jdbc:postgresql://127.0.0.1:55432/<base_de_ejecucion>.");
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

        String nombreBase = extraerNombreBase(url);
        validarBaseDeEjecucion(nombreBase);
        String testUrl = valorExplicito(entorno, ENV_TEST_DB_URL);
        if (testUrl != null) {
            String basePruebas = extraerNombreBase(testUrl);
            if (basePruebas == null) {
                throw new SQLException("Destino de ejecucion rechazado: no se pudo determinar la base configurada en "
                        + ENV_TEST_DB_URL + " para verificar colisiones de forma segura.");
            }
            if (basePruebas.equalsIgnoreCase(nombreBase)) {
                throw new SQLException("Destino de ejecucion rechazado: la base '" + nombreBase
                        + "' coincide con la configurada para pruebas (" + ENV_TEST_DB_URL + ").");
            }
        }

        return new ParametrosConexion(url, usuario, rawPassword);
    }

    /**
     * Verifica que la base de ejecucion de la aplicacion no sea una base reservada del proyecto.
     *
     * @throws SQLException si la base esta reservada o no puede determinarse
     */
    public static void validarBaseDeEjecucion(String nombreBase) throws SQLException {
        String decodificado = decodificarPorcentaje(nombreBase);
        if (decodificado == null) {
            throw new SQLException("No se pudo determinar el nombre de la base de datos de ejecucion a partir de la URL configurada.");
        }
        String normalizado = decodificado.toLowerCase(Locale.ROOT);
        if (BASES_PROTEGIDAS.contains(normalizado)) {
            throw new SQLException("Destino de ejecucion rechazado: la base '" + decodificado
                    + "' es una base reservada del proyecto y no puede utilizarse para ejecutar la aplicacion. "
                    + "Defina " + ENV_DB_URL + " con una base de ejecucion exclusiva.");
        }
    }

    private static void autorizarParametrosAntesDeConectar(ParametrosConexion params) throws SQLException {
        if (params == null || params.url == null) {
            throw new SQLException("Parametros de conexion nulos.");
        }
        String nombreBase = extraerNombreBase(params.url);
        if (nombreBase == null) {
            throw new SQLException("Conexion abortada preventivamente: URL JDBC invalida, ambigua o no verificable.");
        }
        if (BASES_PROTEGIDAS.contains(nombreBase.toLowerCase(Locale.ROOT))) {
            throw new SQLException("Conexion abortada preventivamente: la base '" + nombreBase
                    + "' esta protegida.");
        }
    }

    private static Connection abrirConexionConParametros(ParametrosConexion params, ConectorJdbc conector)
            throws SQLException {
        autorizarParametrosAntesDeConectar(params);
        return conector.conectar(params.url, params.usuario, params.password);
    }

    private static Connection conectarConDriverManager(String url, String usuario, String password)
            throws SQLException {
        asegurarDriverCargado();

        String urlSanitizada = sanitizarUrl(url);
        try {
            Connection conexion = DriverManager.getConnection(url, usuario, password);
            if (conexion == null) {
                throw new SQLException("DriverManager devolvio una conexion nula para la URL '" + urlSanitizada + "'.");
            }
            return conexion;
        } catch (SQLException e) {
            String mensajeDiagnostico = construirMensajeErrorConexion(urlSanitizada, usuario, e);
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

    @FunctionalInterface
    interface ConectorJdbc {
        Connection conectar(String url, String usuario, String password) throws SQLException;
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
