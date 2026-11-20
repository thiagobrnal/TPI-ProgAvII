package com.gestionamateur.tpi.progav.dao;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Regresión de las protecciones de ejecución segura (E1 -> E2 / INC-1):
 * - La aplicación exige una URL explícita ({@code TPI_DB_URL}), sin valores por defecto ni
 *   menciones a bases reservadas.
 * - Ninguna base reservada del proyecto puede usarse en ejecución ni en pruebas.
 * - Los modos {@code APLICACION} y {@code PRUEBAS} están estrictamente aislados entre sí.
 */
@DisplayName("Configuracion segura de la conexion de ejecucion de la aplicacion")
class ConfiguracionEjecucionSeguraTest {

    private static final String BASE_PROPIA = "tpi_pa2_r1_base_de_ejecucion_local";
    private static final String BASE_PRUEBAS_AISLADA = "tpi_pa2_e2_pruebas_locales";

    private String prevPropModo;
    private String prevPropDbUrl;
    private String prevPropTestDbUrl;

    @BeforeEach
    void guardarYLimpiarModoDeEjecucion() {
        prevPropModo = System.getProperty(ConexionBD.PROP_MODO_EJECUCION);
        prevPropDbUrl = System.getProperty(ConexionBD.ENV_DB_URL);
        prevPropTestDbUrl = System.getProperty(ConexionBD.ENV_TEST_DB_URL);
        System.clearProperty(ConexionBD.PROP_MODO_EJECUCION);
        System.clearProperty(ConexionBD.ENV_DB_URL);
        System.clearProperty(ConexionBD.ENV_TEST_DB_URL);
        ConexionBD.restablecerModoPorDefecto();
    }

    @AfterEach
    void restaurarModoDeEjecucion() {
        restaurarPropiedad(ConexionBD.PROP_MODO_EJECUCION, prevPropModo);
        restaurarPropiedad(ConexionBD.ENV_DB_URL, prevPropDbUrl);
        restaurarPropiedad(ConexionBD.ENV_TEST_DB_URL, prevPropTestDbUrl);
        ConexionBD.restablecerModoPorDefecto();
    }

    private static void restaurarPropiedad(String clave, String valor) {
        if (valor == null) {
            System.clearProperty(clave);
        } else {
            System.setProperty(clave, valor);
        }
    }

    private Map<String, String> entornoConUrl(String url) {
        Map<String, String> entorno = new HashMap<>();
        if (url != null) {
            entorno.put(ConexionBD.ENV_DB_URL, url);
        }
        return entorno;
    }

    @Test
    @DisplayName("Sin TPI_DB_URL la aplicacion falla de forma cerrada y no menciona ninguna base reservada")
    void testAplicacionSinUrlFallaCerrada() {
        SQLException e = assertThrows(SQLException.class,
                () -> ConexionBD.leerYValidarParametros(entornoConUrl(null),
                        ConexionBD.ENV_DB_URL, ConexionBD.ENV_DB_USER, ConexionBD.ENV_DB_PASSWORD));
        assertTrue(e.getMessage().contains(ConexionBD.ENV_DB_URL),
                "El mensaje debe identificar la variable faltante: " + e.getMessage());
        assertFalse(e.getMessage().contains("tpi_pa2_r1_demo"),
                "El mensaje no debe sugerir ni mencionar una base reservada: " + e.getMessage());
        assertTrue(e.getMessage().contains("valor por defecto"),
                "El mensaje debe indicar que no se aplica ningun valor por defecto: " + e.getMessage());
    }

    @Test
    @DisplayName("Ninguna base reservada del proyecto puede ser destino de la ejecucion")
    void testAplicacionRechazaBasesReservadas() {
        for (String base : ConexionBD.BASES_PROTEGIDAS) {
            String url = "jdbc:postgresql://127.0.0.1:55432/" + base;
            SQLException e = assertThrows(SQLException.class,
                    () -> ConexionBD.leerYValidarParametros(entornoConUrl(url),
                            ConexionBD.ENV_DB_URL, ConexionBD.ENV_DB_USER, ConexionBD.ENV_DB_PASSWORD));
            assertTrue(e.getMessage().contains("reservada"),
                    "El mensaje debe explicar que la base esta reservada (" + base + "): " + e.getMessage());
            assertThrows(SQLException.class, () -> ConexionBD.validarBaseDeEjecucion(base));
        }
    }

    @Test
    @DisplayName("Un nombre de base de ejecucion nulo o vacio se rechaza")
    void testAplicacionRechazaBaseIndeterminable() {
        assertThrows(SQLException.class, () -> ConexionBD.validarBaseDeEjecucion(null));
        assertThrows(SQLException.class, () -> ConexionBD.validarBaseDeEjecucion("   "));
    }

    @Test
    @DisplayName("Una base de ejecucion propia y no reservada se acepta")
    void testAplicacionAceptaBasePropia() {
        String url = "jdbc:postgresql://127.0.0.1:55432/" + BASE_PROPIA;
        assertDoesNotThrow(() -> ConexionBD.leerYValidarParametros(entornoConUrl(url),
                ConexionBD.ENV_DB_URL, ConexionBD.ENV_DB_USER, ConexionBD.ENV_DB_PASSWORD));
        assertDoesNotThrow(() -> ConexionBD.validarBaseDeEjecucion(BASE_PROPIA));
    }

    @Test
    @DisplayName("La URL de referencia documentada no apunta a ninguna base reservada")
    void testUrlDeReferenciaNoReservada() {
        String base = ConexionBD.extraerNombreBase(ConexionBD.URL_REFERENCIA);
        assertNotNull(base, "La URL de referencia debe permitir extraer el nombre de la base.");
        assertFalse(ConexionBD.BASES_PROTEGIDAS.contains(base.toLowerCase(Locale.ROOT)),
                "La URL de referencia no debe apuntar a una base reservada: " + base);
    }

    @Test
    @DisplayName("El conjunto de bases reservadas cubre todas las bases historicas del proyecto")
    void testConjuntoDeBasesReservadasCompleto() {
        for (String base : new String[]{"tpi_pa2_r1_demo", "tpi_pa2_inf06_test",
                "tpi_pa2_r1_func_test_20261111_192226", "tpi_pa2_r1_fix_test_20261111",
                "tpi_pa2_r1_reports_verify_20261112", "tpi_pa2_r1_pdfzip_fix_test_20261112",
                "tpi_pa2_r1_netbeans_test_20261112", "tpi_pa2_r1_netbeans_run_20261112",
                "tpi_pa2_r1_delivery_audit_test_20261112"}) {
            assertTrue(ConexionBD.BASES_PROTEGIDAS.contains(base),
                    "La base historica " + base + " debe estar protegida.");
        }
    }

    @Test
    @DisplayName("Aislamiento estricto entre activarModoAplicacion y activarModoPruebas sin contaminacion cruzada")
    void testSeleccionYAislamientoDeModos() throws Exception {
        Map<String, String> ambosEntornos = new HashMap<>();
        ambosEntornos.put(ConexionBD.ENV_DB_URL, "jdbc:postgresql://127.0.0.1:55432/" + BASE_PROPIA);
        ambosEntornos.put(ConexionBD.ENV_TEST_DB_URL, "jdbc:postgresql://127.0.0.1:55432/" + BASE_PRUEBAS_AISLADA);

        // 1. Modo aplicacion: usa exclusivamente TPI_DB_URL aunque TPI_TEST_DB_URL este definida.
        ConexionBD.activarModoAplicacion();
        assertFalse(ConexionBD.esProcesoDePruebas(), "En modo aplicacion esProcesoDePruebas debe ser false.");
        ConexionBD.ParametrosConexion paramsApp = ConexionBD.resolverParametrosActivos(ambosEntornos);
        assertEquals(BASE_PROPIA, ConexionBD.extraerNombreBase(paramsApp.url));

        // 2. Modo aplicacion con TPI_DB_URL faltante: falla cerrado incluso si TPI_TEST_DB_URL existe.
        Map<String, String> soloTest = new HashMap<>();
        soloTest.put(ConexionBD.ENV_TEST_DB_URL, "jdbc:postgresql://127.0.0.1:55432/" + BASE_PRUEBAS_AISLADA);
        SQLException exAppSinUrl = assertThrows(SQLException.class,
                () -> ConexionBD.resolverParametrosActivos(soloTest));
        assertTrue(exAppSinUrl.getMessage().contains(ConexionBD.ENV_DB_URL));

        // 3. Modo aplicacion rechaza coincidir con la base configurada para pruebas.
        Map<String, String> mismaBase = new HashMap<>();
        mismaBase.put(ConexionBD.ENV_DB_URL, "jdbc:postgresql://127.0.0.1:55432/" + BASE_PRUEBAS_AISLADA);
        mismaBase.put(ConexionBD.ENV_TEST_DB_URL, "jdbc:postgresql://127.0.0.1:55432/" + BASE_PRUEBAS_AISLADA);
        SQLException exColision = assertThrows(SQLException.class,
                () -> ConexionBD.resolverParametrosActivos(mismaBase));
        assertTrue(exColision.getMessage().contains(ConexionBD.ENV_TEST_DB_URL),
                "Debe rechazar ejecutar la aplicacion contra la base de pruebas: " + exColision.getMessage());

        // 4. Modo pruebas tras restaurar explicitamente: usa exclusivamente TPI_TEST_DB_URL aunque TPI_DB_URL este definida.
        ConexionBD.restablecerModoPorDefecto();
        ConexionBD.activarModoPruebas();
        assertTrue(ConexionBD.esProcesoDePruebas(), "En modo pruebas esProcesoDePruebas debe ser true.");
        ConexionBD.ParametrosConexion paramsTest = ConexionBD.resolverParametrosActivos(ambosEntornos);
        assertEquals(BASE_PRUEBAS_AISLADA, ConexionBD.extraerNombreBase(paramsTest.url));

        // 5. Modo pruebas sin TPI_TEST_DB_URL: falla cerrado incluso si TPI_DB_URL existe.
        SQLException exTestSinUrl = assertThrows(SQLException.class,
                () -> ConexionBD.resolverParametrosActivos(entornoConUrl("jdbc:postgresql://127.0.0.1:55432/" + BASE_PROPIA)));
        assertTrue(exTestSinUrl.getMessage().contains(ConexionBD.ENV_TEST_DB_URL));

        // 6. Modo pruebas rechaza coincidir con la base configurada para la aplicacion.
        SQLException exColisionPruebas = assertThrows(SQLException.class,
                () -> ConexionBD.leerParametrosDePruebas(mismaBase));
        assertTrue(exColisionPruebas.getMessage().contains(ConexionBD.ENV_DB_URL));
    }

    @Test
    @DisplayName("INC-1-CORR-01: NO_DEFINIDO rechaza cualquier conexion aun con TPI_TEST_DB_URL o TPI_DB_URL definidas")
    void testModoNoDefinidoRechazaTodaConexion() {
        ConexionBD.restablecerModoParaPruebas();
        assertFalse(ConexionBD.esProcesoDePruebas());

        Map<String, String> ambosEntornos = new HashMap<>();
        ambosEntornos.put(ConexionBD.ENV_DB_URL, "jdbc:postgresql://127.0.0.1:55432/" + BASE_PROPIA);
        ambosEntornos.put(ConexionBD.ENV_TEST_DB_URL, "jdbc:postgresql://127.0.0.1:55432/" + BASE_PRUEBAS_AISLADA);

        Map<String, String> soloTest = new HashMap<>();
        soloTest.put(ConexionBD.ENV_TEST_DB_URL, "jdbc:postgresql://127.0.0.1:55432/" + BASE_PRUEBAS_AISLADA);

        SQLException exAmbos = assertThrows(SQLException.class,
                () -> ConexionBD.resolverParametrosActivos(ambosEntornos));
        assertTrue(exAmbos.getMessage().contains("Modo de ejecucion no definido"),
                "Debe rechazar por modo no definido: " + exAmbos.getMessage());

        assertThrows(SQLException.class, () -> ConexionBD.resolverParametrosActivos(soloTest));
        assertThrows(SQLException.class,
                () -> ConexionBD.resolverParametrosActivos(entornoConUrl("jdbc:postgresql://127.0.0.1:55432/" + BASE_PROPIA)));
        assertThrows(SQLException.class, () -> ConexionBD.resolverParametrosActivos(new HashMap<>()));
        assertThrows(SQLException.class, ConexionBD::obtenerConexion);
        assertThrows(SQLException.class, ConexionBD::obtenerConexionTest);
        assertThrows(SQLException.class, ConexionBD::comprobarConexion);
    }

    @Test
    @DisplayName("INC-1-CORR-01: Un valor invalido en tpi.conexion.modo rechaza toda configuracion y conexion")
    void testModoEjecucionInvalidoRechazaConexion() {
        System.setProperty(ConexionBD.PROP_MODO_EJECUCION, "modo_invalido");
        Map<String, String> ambosEntornos = new HashMap<>();
        ambosEntornos.put(ConexionBD.ENV_DB_URL, "jdbc:postgresql://127.0.0.1:55432/" + BASE_PROPIA);
        ambosEntornos.put(ConexionBD.ENV_TEST_DB_URL, "jdbc:postgresql://127.0.0.1:55432/" + BASE_PRUEBAS_AISLADA);

        SQLException ex = assertThrows(SQLException.class,
                () -> ConexionBD.resolverParametrosActivos(ambosEntornos));
        assertTrue(ex.getMessage().contains("invalido"),
                "Debe rechazar el modo invalido de forma explicita: " + ex.getMessage());
        assertThrows(SQLException.class, ConexionBD::obtenerConexion);
    }

    @Test
    @DisplayName("INC-1-CORR-01: TPIProgAv activa APLICACION antes de GUI y todas las suites *IT activan PRUEBAS antes de conectar")
    void testActivacionExplicitaEnDesktopYIntegracion() throws IOException {
        Path rutaMainDesktop = Paths.get("src/main/java/com/gestionamateur/tpi/progav/TPIProgAv.java");
        String codigoMain = Files.readString(rutaMainDesktop, StandardCharsets.UTF_8);
        int idxActivar = codigoMain.indexOf("ConexionBD.activarModoAplicacion()");
        int idxEstilos = codigoMain.indexOf("EstilosUI.inicializarTema()");
        int idxLogin = codigoMain.indexOf("new FrmLogin()");
        assertTrue(idxActivar > 0, "TPIProgAv.main debe invocar ConexionBD.activarModoAplicacion()");
        assertTrue(idxActivar < idxEstilos && idxActivar < idxLogin,
                "TPIProgAv.main debe activar el modo APLICACION antes de inicializar GUI y FrmLogin");

        Path raizTests = Paths.get("src/test/java");
        List<Path> suitesIT;
        try (Stream<Path> walk = Files.walk(raizTests)) {
            suitesIT = walk.filter(p -> p.getFileName().toString().endsWith("IT.java"))
                    .sorted()
                    .collect(Collectors.toList());
        }
        assertEquals(11, suitesIT.size(), "Se esperaban exactamente 11 suites *IT en el proyecto");
        String srcControlIT = Files.readString(
                Paths.get("src/test/java/com/gestionamateur/tpi/progav/dao/ControlEjecucionIntegracion.java"),
                StandardCharsets.UTF_8);
        assertTrue(srcControlIT.contains("tpi.it.habilitado"),
                "ControlEjecucionIntegracion debe exigir explicitamente tpi.it.habilitado=true");
        String srcAislamientoIT = Files.readString(
                Paths.get("src/test/java/com/gestionamateur/tpi/progav/dao/AislamientoBasePruebasIT.java"),
                StandardCharsets.UTF_8);
        assertTrue(srcAislamientoIT.contains("ControlEjecucionIntegracion.exigirHabilitacionExplicita("),
                "AislamientoBasePruebasIT debe quedar bloqueada mediante ControlEjecucionIntegracion sin -Dtpi.it.habilitado=true");

        for (Path suite : suitesIT) {
            String src = Files.readString(suite, StandardCharsets.UTF_8);
            int idxActivarTest = src.indexOf("ConexionBD.activarModoPruebas()");
            int idxValidar = src.indexOf("ConexionBD.validarDestinoDePruebas()");
            assertTrue(idxActivarTest >= 0,
                    suite.getFileName() + " debe invocar explicitamente ConexionBD.activarModoPruebas() en @BeforeAll");
            assertTrue(idxValidar > idxActivarTest,
                    suite.getFileName() + " debe invocar ConexionBD.validarDestinoDePruebas() despues de activarModoPruebas()");

            int idxComprobar = src.indexOf("ConexionBD.comprobarConexion()");
            int idxObtener = src.indexOf("ConexionBD.obtenerConexion()");
            int idxObtenerTest = src.indexOf("ConexionBD.obtenerConexionTest()");
            if (idxComprobar >= 0) {
                assertTrue(idxValidar < idxComprobar,
                        suite.getFileName() + " invoca comprobarConexion() antes de activar y validar modo PRUEBAS");
            }
            if (idxObtener >= 0) {
                assertTrue(idxValidar < idxObtener,
                        suite.getFileName() + " invoca obtenerConexion() antes de activar y validar modo PRUEBAS");
            }
            if (idxObtenerTest >= 0) {
                assertTrue(idxValidar < idxObtenerTest,
                        suite.getFileName() + " invoca obtenerConexionTest() antes de activar y validar modo PRUEBAS");
            }
            assertTrue(src.contains("@AfterAll") && src.contains("ConexionBD.restablecerModoPorDefecto()"),
                    suite.getFileName() + " debe restaurar el modo de ejecucion en @AfterAll");
        }
    }

    @Test
    @DisplayName("INC-1-CORR-02: Desktop con propiedad aplicacion o ausente fija APLICACION y nunca resuelve TPI_TEST_DB_URL")
    void testDesktopAplicacionOPropiedadAusenteNuncaResuelvePruebas() throws Exception {
        Map<String, String> ambosEntornos = new HashMap<>();
        ambosEntornos.put(ConexionBD.ENV_DB_URL, "jdbc:postgresql://127.0.0.1:55432/" + BASE_PROPIA);
        ambosEntornos.put(ConexionBD.ENV_TEST_DB_URL, "jdbc:postgresql://127.0.0.1:55432/" + BASE_PRUEBAS_AISLADA);

        Map<String, String> soloTest = new HashMap<>();
        soloTest.put(ConexionBD.ENV_TEST_DB_URL, "jdbc:postgresql://127.0.0.1:55432/" + BASE_PRUEBAS_AISLADA);

        for (String valorProp : new String[]{null, "aplicacion"}) {
            System.clearProperty(ConexionBD.PROP_MODO_EJECUCION);
            ConexionBD.restablecerModoPorDefecto();
            if (valorProp != null) {
                System.setProperty(ConexionBD.PROP_MODO_EJECUCION, valorProp);
            }
            ConexionBD.activarModoAplicacion();
            assertFalse(ConexionBD.esProcesoDePruebas());
            ConexionBD.ParametrosConexion params = ConexionBD.resolverParametrosActivos(ambosEntornos);
            assertEquals(BASE_PROPIA, ConexionBD.extraerNombreBase(params.url));
            assertThrows(SQLException.class, () -> ConexionBD.resolverParametrosActivos(soloTest));
            assertThrows(SQLException.class, ConexionBD::obtenerConexionTest);
        }
    }

    @Test
    @DisplayName("INC-1-CORR-02: Desktop rechaza de forma segura propiedad pruebas, invalida o contradictoria")
    void testDesktopRechazaPropiedadPruebasInvalidaOContradictoria() {
        Map<String, String> ambosEntornos = new HashMap<>();
        ambosEntornos.put(ConexionBD.ENV_DB_URL, "jdbc:postgresql://127.0.0.1:55432/" + BASE_PROPIA);
        ambosEntornos.put(ConexionBD.ENV_TEST_DB_URL, "jdbc:postgresql://127.0.0.1:55432/" + BASE_PRUEBAS_AISLADA);

        // 1. Propiedad 'pruebas' al intentar activar modo Desktop
        System.setProperty(ConexionBD.PROP_MODO_EJECUCION, "pruebas");
        assertThrows(IllegalStateException.class, ConexionBD::activarModoAplicacion);
        assertThrows(SQLException.class, () -> ConexionBD.resolverParametrosActivos(ambosEntornos));
        assertThrows(SQLException.class, ConexionBD::obtenerConexion);

        // 2. Propiedad invalida al intentar activar modo Desktop
        System.setProperty(ConexionBD.PROP_MODO_EJECUCION, "invalido_xyz");
        ConexionBD.restablecerModoPorDefecto();
        assertThrows(IllegalStateException.class, ConexionBD::activarModoAplicacion);
        assertThrows(SQLException.class, () -> ConexionBD.resolverParametrosActivos(ambosEntornos));
        assertThrows(SQLException.class, ConexionBD::obtenerConexion);

        // 3. Cambio tardio de propiedad contradictoria tras activar Desktop
        System.clearProperty(ConexionBD.PROP_MODO_EJECUCION);
        ConexionBD.restablecerModoPorDefecto();
        ConexionBD.activarModoAplicacion();
        System.setProperty(ConexionBD.PROP_MODO_EJECUCION, "pruebas");
        assertThrows(SQLException.class, () -> ConexionBD.resolverParametrosActivos(ambosEntornos));
        assertThrows(SQLException.class, ConexionBD::obtenerConexion);
        assertThrows(SQLException.class, ConexionBD::obtenerConexionTest);
    }

    @Test
    @DisplayName("INC-1-CORR-02: Impide transicion silenciosa de APLICACION a PRUEBAS y rechaza contradicciones en pruebas")
    void testImpideTransicionSilenciosaAplicacionAPruebasYContradicciones() {
        Map<String, String> ambosEntornos = new HashMap<>();
        ambosEntornos.put(ConexionBD.ENV_DB_URL, "jdbc:postgresql://127.0.0.1:55432/" + BASE_PROPIA);
        ambosEntornos.put(ConexionBD.ENV_TEST_DB_URL, "jdbc:postgresql://127.0.0.1:55432/" + BASE_PRUEBAS_AISLADA);

        // 1. Desde APLICACION no se puede transicionar silenciosamente a PRUEBAS
        ConexionBD.activarModoAplicacion();
        assertThrows(IllegalStateException.class, ConexionBD::activarModoPruebas);
        assertFalse(ConexionBD.esProcesoDePruebas());
        assertThrows(SQLException.class, ConexionBD::validarDestinoDePruebas);
        assertThrows(SQLException.class, ConexionBD::obtenerConexionTest);

        // El modo sigue siendo APLICACION y solo resuelve TPI_DB_URL
        assertDoesNotThrow(() -> {
            ConexionBD.ParametrosConexion params = ConexionBD.resolverParametrosActivos(ambosEntornos);
            assertEquals(BASE_PROPIA, ConexionBD.extraerNombreBase(params.url));
        });

        // 2. Configuracion contradictoria: propiedad 'aplicacion' al activar PRUEBAS
        ConexionBD.restablecerModoPorDefecto();
        System.setProperty(ConexionBD.PROP_MODO_EJECUCION, "aplicacion");
        assertThrows(IllegalStateException.class, ConexionBD::activarModoPruebas);
        assertThrows(SQLException.class, ConexionBD::validarDestinoDePruebas);
        assertThrows(SQLException.class, ConexionBD::obtenerConexionTest);

        // 3. Desde PRUEBAS no se puede transicionar silenciosamente a APLICACION sin restablecer
        System.clearProperty(ConexionBD.PROP_MODO_EJECUCION);
        ConexionBD.restablecerModoPorDefecto();
        ConexionBD.activarModoPruebas();
        assertThrows(IllegalStateException.class, ConexionBD::activarModoAplicacion);
        assertTrue(ConexionBD.esProcesoDePruebas());
    }

    @Test
    @DisplayName("INC-1-CORR-02/03: validarDestinoDePruebas exige modo PRUEBAS explicito, valida con mapa sintetico sin fallback ni JDBC y restaura bien")
    void testContratoValidarDestinoDePruebasYRestauracion() throws Exception {
        // 1. En NO_DEFINIDO, validarDestinoDePruebas y validarConfiguracionTest rechazan por modo antes de leer entorno o abrir JDBC
        SQLException exNoDef = assertThrows(SQLException.class, ConexionBD::validarDestinoDePruebas);
        assertTrue(exNoDef.getMessage().contains("NO_DEFINIDO"),
                "Debe rechazar por modo NO_DEFINIDO sin intentar abrir conexion JDBC: " + exNoDef.getMessage());
        assertThrows(SQLException.class, ConexionBD::validarConfiguracionTest);
        assertFalse(ConexionBD.esProcesoDePruebas(),
                "validarDestinoDePruebas no debe activar implicitamente modo PRUEBAS.");

        // 2. En APLICACION, validarDestinoDePruebas y validarConfiguracionTest rechazan antes de abrir JDBC
        ConexionBD.activarModoAplicacion();
        SQLException exApp = assertThrows(SQLException.class, ConexionBD::validarDestinoDePruebas);
        assertTrue(exApp.getMessage().contains("APLICACION"));
        assertThrows(SQLException.class, ConexionBD::validarConfiguracionTest);
        assertFalse(ConexionBD.esProcesoDePruebas());

        // 3. Con propiedad invalida, validarDestinoDePruebas rechaza antes de abrir JDBC
        ConexionBD.restablecerModoPorDefecto();
        System.setProperty(ConexionBD.PROP_MODO_EJECUCION, "valor_invalido");
        SQLException exInv = assertThrows(SQLException.class, ConexionBD::validarDestinoDePruebas);
        assertTrue(exInv.getMessage().contains("INVALIDO"));
        assertFalse(ConexionBD.esProcesoDePruebas());

        // 4. Con activacion explicita de PRUEBAS, la validacion pura usa exclusivamente TPI_TEST_DB_URL del mapa sintetico
        System.clearProperty(ConexionBD.PROP_MODO_EJECUCION);
        ConexionBD.restablecerModoPorDefecto();
        ConexionBD.activarModoPruebas();
        assertTrue(ConexionBD.esProcesoDePruebas());

        Map<String, String> entornoMixto = new HashMap<>();
        entornoMixto.put(ConexionBD.ENV_DB_URL, "jdbc:postgresql://127.0.0.1:59999/" + BASE_PROPIA);
        entornoMixto.put(ConexionBD.ENV_TEST_DB_URL, "jdbc:postgresql://127.0.0.1:59999/" + BASE_PRUEBAS_AISLADA);
        ConexionBD.ParametrosConexion paramsTest = ConexionBD.validarConfiguracionDestinoDePruebas(entornoMixto);
        assertEquals(BASE_PRUEBAS_AISLADA, ConexionBD.extraerNombreBase(paramsTest.url));

        // 5. Si falta TPI_TEST_DB_URL en el mapa pero existe TPI_DB_URL, nunca hace fallback ni lee System.getenv()
        Map<String, String> soloApp = new HashMap<>();
        soloApp.put(ConexionBD.ENV_DB_URL, "jdbc:postgresql://127.0.0.1:59999/" + BASE_PROPIA);
        SQLException exSinTestUrl = assertThrows(SQLException.class,
                () -> ConexionBD.validarConfiguracionDestinoDePruebas(soloApp));
        assertTrue(exSinTestUrl.getMessage().contains(ConexionBD.ENV_TEST_DB_URL));
        assertFalse(exSinTestUrl.getMessage().contains(BASE_PROPIA));

        // 6. Al restaurar el modo por defecto, vuelve a NO_DEFINIDO y vuelve a rechazar conexiones/validaciones
        ConexionBD.restablecerModoPorDefecto();
        assertFalse(ConexionBD.esProcesoDePruebas());
        assertThrows(SQLException.class, () -> ConexionBD.validarConfiguracionDestinoDePruebas(entornoMixto));
        assertThrows(SQLException.class, ConexionBD::validarDestinoDePruebas);
        assertThrows(SQLException.class, ConexionBD::obtenerConexion);
    }

    @Test
    @DisplayName("INC-1-CORR-03 (H-01/H-02/H-06): Mapas sinteticos aislados de System.getenv y deteccion de colisiones con distinto host/parametros")
    void testConfiguracionSinteticaAisladaDelEntornoRealYColisionesDiferenteHostOParametros() throws Exception {
        // 1. Validar que un mapa sintetico no consulta System.getenv ni System.getProperty ni siquiera para colision con TPI_DB_URL
        System.setProperty(ConexionBD.ENV_DB_URL, "jdbc:postgresql://127.0.0.1:59999/" + BASE_PRUEBAS_AISLADA);
        System.setProperty(ConexionBD.ENV_TEST_DB_URL, "jdbc:postgresql://127.0.0.1:59999/" + BASE_PRUEBAS_AISLADA);

        ConexionBD.activarModoPruebas();
        SQLException exVacioTest = assertThrows(SQLException.class,
                () -> ConexionBD.validarConfiguracionDestinoDePruebas(new HashMap<>()));
        assertTrue(exVacioTest.getMessage().contains(ConexionBD.ENV_TEST_DB_URL));

        // Si el mapa sintetico solo define TPI_TEST_DB_URL (sin TPI_DB_URL), NO debe colisionar contra System.getProperty(TPI_DB_URL)
        Map<String, String> soloTestSintetico = new HashMap<>();
        soloTestSintetico.put(ConexionBD.ENV_TEST_DB_URL, "jdbc:postgresql://127.0.0.1:59999/" + BASE_PRUEBAS_AISLADA);
        ConexionBD.ParametrosConexion paramsAislados = assertDoesNotThrow(
                () -> ConexionBD.validarConfiguracionDestinoDePruebas(soloTestSintetico),
                "Un mapa sintetico no debe leer TPI_DB_URL desde System.getProperty() ni System.getenv()");
        assertEquals(BASE_PRUEBAS_AISLADA, ConexionBD.extraerNombreBase(paramsAislados.url));

        ConexionBD.restablecerModoPorDefecto();
        ConexionBD.activarModoAplicacion();
        SQLException exVacioApp = assertThrows(SQLException.class,
                () -> ConexionBD.resolverParametrosActivos(new HashMap<>()));
        assertTrue(exVacioApp.getMessage().contains(ConexionBD.ENV_DB_URL));

        // 2. Colision entre URLs equivalentes con distinto host, puerto o query params pero mismo nombre de base
        Map<String, String> colisionDistintoFormato = new HashMap<>();
        colisionDistintoFormato.put(ConexionBD.ENV_DB_URL,
                "jdbc:postgresql://localhost:55433/base_compartida_riesgo?sslmode=disable");
        colisionDistintoFormato.put(ConexionBD.ENV_TEST_DB_URL,
                "jdbc:postgresql://127.0.0.1:55432/BASE_COMPARTIDA_RIESGO?ApplicationName=suite");

        SQLException exColisionApp = assertThrows(SQLException.class,
                () -> ConexionBD.resolverParametrosActivos(colisionDistintoFormato));
        assertTrue(exColisionApp.getMessage().contains(ConexionBD.ENV_TEST_DB_URL));

        ConexionBD.restablecerModoPorDefecto();
        ConexionBD.activarModoPruebas();
        SQLException exColisionTest = assertThrows(SQLException.class,
                () -> ConexionBD.validarConfiguracionDestinoDePruebas(colisionDistintoFormato));
        assertTrue(exColisionTest.getMessage().contains(ConexionBD.ENV_DB_URL));

        // 3. validarConfiguracionDestinoDePruebas exige modo PRUEBAS aunque el mapa sea valido
        ConexionBD.restablecerModoPorDefecto();
        assertThrows(SQLException.class, () -> ConexionBD.validarConfiguracionDestinoDePruebas(soloTestSintetico));

        // 4. run_app.bat no borra TPI_TEST_DB_URL antes de lanzar Java y mantiene sus guardas
        String runAppBat = Files.readString(Paths.get("run_app.bat"), StandardCharsets.UTF_8);
        assertFalse(runAppBat.contains("set \"TPI_TEST_DB_URL=\""),
                "run_app.bat no debe limpiar TPI_TEST_DB_URL antes de invocar Java.");
        assertTrue(runAppBat.contains("if not defined TPI_DB_URL"),
                "run_app.bat debe mantener la verificacion inmediata de TPI_DB_URL.");
        assertTrue(runAppBat.contains("-Dtpi.conexion.modo=aplicacion"),
                "run_app.bat debe iniciar Java con -Dtpi.conexion.modo=aplicacion.");
    }

    @Test
    @DisplayName("INC-1-CORR-03 (H-03): Seleccion inicial desde propiedad queda fijada y rechaza mutacion dinamica sin restablecer")
    void testSeleccionInicialDesdePropiedadFijaModoYRechazaMutacionDinamica() throws Exception {
        Map<String, String> ambosEntornos = new HashMap<>();
        ambosEntornos.put(ConexionBD.ENV_DB_URL, "jdbc:postgresql://127.0.0.1:59999/" + BASE_PROPIA);
        ambosEntornos.put(ConexionBD.ENV_TEST_DB_URL, "jdbc:postgresql://127.0.0.1:59999/" + BASE_PRUEBAS_AISLADA);

        // 1. Partiendo de NO_DEFINIDO, definir propiedad 'aplicacion' y operar con ella fija APLICACION
        assertFalse(ConexionBD.esProcesoDePruebas());
        System.setProperty(ConexionBD.PROP_MODO_EJECUCION, "aplicacion");
        assertFalse(ConexionBD.esProcesoDePruebas());
        ConexionBD.ParametrosConexion paramsApp = ConexionBD.resolverParametrosActivos(ambosEntornos);
        assertEquals(BASE_PROPIA, ConexionBD.extraerNombreBase(paramsApp.url));

        // 2. Mutar dinamicamente la propiedad a 'pruebas' e invocar activarModoPruebas() debe fijar CONTRADICTORIO y rechazar
        System.setProperty(ConexionBD.PROP_MODO_EJECUCION, "pruebas");
        assertThrows(IllegalStateException.class, ConexionBD::activarModoPruebas);
        assertThrows(SQLException.class, () -> ConexionBD.resolverParametrosActivos(ambosEntornos));
        assertThrows(SQLException.class, ConexionBD::obtenerConexion);
        assertThrows(SQLException.class, ConexionBD::obtenerConexionTest);

        // 3. Limpiar la propiedad despues de la contradiccion mantiene el bloqueo hasta restablecerModoPorDefecto()
        System.clearProperty(ConexionBD.PROP_MODO_EJECUCION);
        assertThrows(SQLException.class, () -> ConexionBD.resolverParametrosActivos(ambosEntornos));
        assertThrows(IllegalStateException.class, ConexionBD::activarModoPruebas);
        assertThrows(IllegalStateException.class, ConexionBD::activarModoAplicacion);

        // 4. Solo tras restablecer explicitamente se habilita una nueva seleccion limpia en sentido inverso (pruebas -> aplicacion)
        ConexionBD.restablecerModoPorDefecto();
        System.setProperty(ConexionBD.PROP_MODO_EJECUCION, "pruebas");
        assertTrue(ConexionBD.esProcesoDePruebas());
        assertEquals(BASE_PRUEBAS_AISLADA,
                ConexionBD.extraerNombreBase(ConexionBD.resolverParametrosActivos(ambosEntornos).url));

        System.setProperty(ConexionBD.PROP_MODO_EJECUCION, "aplicacion");
        assertFalse(ConexionBD.esProcesoDePruebas());
        assertThrows(SQLException.class, () -> ConexionBD.resolverParametrosActivos(ambosEntornos));
        assertThrows(IllegalStateException.class, ConexionBD::activarModoAplicacion);
    }

    @Test
    @DisplayName("INC-1-CORR-03 (H-07): Rechaza bypass %XX, +, query params y doble codificacion; verificarBaseEfectiva valida nulos/vacios/protegidas")
    void testProteccionContraUrlsCodificadasPercentEncodingYVerificacionEfectivaDefensiva() throws Exception {
        // 1. Demostracion empirica offline: org.postgresql.Driver.parseURL decodifica %64 -> 'd' en PGDBNAME
        String urlCodificada = "jdbc:postgresql://127.0.0.1:55432/tpi_pa2_r1_%64emo";
        java.util.Properties driverProps = org.postgresql.Driver.parseURL(urlCodificada, null);
        assertNotNull(driverProps, "El driver PostgreSQL parsea URLs con secuencias %XX.");
        assertEquals("tpi_pa2_r1_demo", driverProps.getProperty("PGDBNAME"),
                "El driver PostgreSQL decodifica %64 a 'd' antes de conectar.");

        // ConexionBD debe decodificar igual y rechazar la base reservada en ambos modos
        assertEquals("tpi_pa2_r1_demo", ConexionBD.extraerNombreBase(urlCodificada));
        assertThrows(SQLException.class, () -> ConexionBD.validarBaseDeEjecucion("tpi_pa2_r1_%64emo"));
        assertThrows(SQLException.class, () -> ConexionBD.validarBaseDePruebas("tpi_pa2_r1_%64emo"));

        ConexionBD.activarModoAplicacion();
        Map<String, String> envApp = entornoConUrl(urlCodificada);
        assertThrows(SQLException.class, () -> ConexionBD.resolverParametrosActivos(envApp));

        ConexionBD.restablecerModoPorDefecto();
        ConexionBD.activarModoPruebas();
        Map<String, String> envTest = new HashMap<>();
        envTest.put(ConexionBD.ENV_TEST_DB_URL, urlCodificada);
        assertThrows(SQLException.class, () -> ConexionBD.validarConfiguracionDestinoDePruebas(envTest));

        // 2. Doble codificacion (%2564), escapes invalidos (%ZZ), '+' o override por query params (?dbname=, ?PGDBNAME=, ?service=) se rechazan
        assertNull(ConexionBD.extraerNombreBase("jdbc:postgresql://127.0.0.1:55432/tpi_pa2_r1_%2564emo"));
        assertNull(ConexionBD.extraerNombreBase("jdbc:postgresql://127.0.0.1:55432/base_%ZZ"));
        assertNull(ConexionBD.extraerNombreBase("jdbc:postgresql://127.0.0.1:55432/+tpi_pa2_r1_demo+"));
        assertNull(ConexionBD.extraerNombreBase("jdbc:postgresql://127.0.0.1:55432/base_inocente?dbname=tpi_pa2_r1_demo"));
        assertNull(ConexionBD.extraerNombreBase("jdbc:postgresql://127.0.0.1:55432/base_inocente?PGDBNAME=tpi_pa2_r1_demo"));
        assertNull(ConexionBD.extraerNombreBase("jdbc:postgresql://127.0.0.1:55432/base_inocente?service=local"));
        assertThrows(SQLException.class, () -> ConexionBD.validarBaseDeEjecucion("tpi_pa2_r1_%2564emo"));
        assertThrows(SQLException.class, () -> ConexionBD.validarBaseDePruebas("base_%ZZ"));
        assertThrows(SQLException.class, () -> ConexionBD.validarBaseDePruebas("+tpi_pa2_r1_demo+"));

        // 3. verificarBaseEfectiva en memoria via Proxy JDBC sin sockets reales
        String urlValida = "jdbc:postgresql://127.0.0.1:59999/" + BASE_PRUEBAS_AISLADA;
        assertEquals(BASE_PRUEBAS_AISLADA,
                ConexionBD.verificarBaseEfectiva(crearConexionSimulada(true, BASE_PRUEBAS_AISLADA), urlValida));
        assertThrows(SQLException.class,
                () -> ConexionBD.verificarBaseEfectiva(crearConexionSimulada(true, null), urlValida));
        assertThrows(SQLException.class,
                () -> ConexionBD.verificarBaseEfectiva(crearConexionSimulada(true, "   "), urlValida));
        assertThrows(SQLException.class,
                () -> ConexionBD.verificarBaseEfectiva(crearConexionSimulada(false, null), urlValida));
        assertThrows(SQLException.class,
                () -> ConexionBD.verificarBaseEfectiva(crearConexionSimulada(true, "tpi_pa2_r1_demo"),
                        "jdbc:postgresql://127.0.0.1:59999/tpi_pa2_r1_demo"));
        assertThrows(SQLException.class,
                () -> ConexionBD.verificarBaseEfectiva(crearConexionSimulada(true, "otra_base"), urlValida));
        assertThrows(SQLException.class,
                () -> ConexionBD.verificarBaseEfectiva(crearConexionSimulada(true, BASE_PRUEBAS_AISLADA),
                        "jdbc:postgresql://127.0.0.1:59999"));
    }

    @Test
    @DisplayName("INC-1-CORR-03 (H-08): BootstrapConcurrenteIT.tearDown no abre conexion si setUp fallo antes de crear la tabla")
    void testBootstrapConcurrenteTearDownNoConectaSiSetUpFalloAntesDeCrearTabla() throws Exception {
        ConexionBD.activarModoPruebas();
        Class<?> bootstrapClass = Class.forName("com.gestionamateur.tpi.progav.servicio.BootstrapConcurrenteIT");
        Method tearDown = bootstrapClass.getDeclaredMethod("tearDown");
        tearDown.setAccessible(true);

        // Si tablaCreada es false (setUp fallo), tearDown no debe lanzar SQLException secundaria ni fallar sin BD
        assertDoesNotThrow(() -> tearDown.invoke(null));
        assertFalse(ConexionBD.esProcesoDePruebas(),
                "tearDown debe restaurar el modo por defecto aun cuando no se creo la tabla.");
    }

    private static Connection crearConexionSimulada(boolean conFila, String baseEfectiva) {
        ResultSet rs = (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "next": return conFila;
                        case "getString": return baseEfectiva;
                        case "close": return null;
                        default: return null;
                    }
                });
        Statement st = (Statement) Proxy.newProxyInstance(
                Statement.class.getClassLoader(),
                new Class<?>[]{Statement.class},
                (proxy, method, args) -> {
                    if ("executeQuery".equals(method.getName())) {
                        return rs;
                    }
                    return null;
                });
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("createStatement".equals(method.getName())) {
                        return st;
                    }
                    return null;
                });
    }

    @Test
    @DisplayName("La propiedad de sistema de run_app.bat activa modo aplicacion e ignora TPI_TEST_DB_URL")
    void testPropiedadSistemaModoAplicacion() throws Exception {
        System.setProperty(ConexionBD.PROP_MODO_EJECUCION, "aplicacion");
        Map<String, String> entornoMixto = new HashMap<>();
        entornoMixto.put(ConexionBD.ENV_DB_URL, "jdbc:postgresql://127.0.0.1:55432/" + BASE_PROPIA);
        entornoMixto.put(ConexionBD.ENV_TEST_DB_URL, "jdbc:postgresql://127.0.0.1:55432/" + BASE_PRUEBAS_AISLADA);

        assertFalse(ConexionBD.esProcesoDePruebas());
        ConexionBD.ParametrosConexion params = ConexionBD.resolverParametrosActivos(entornoMixto);
        assertEquals(BASE_PROPIA, ConexionBD.extraerNombreBase(params.url));
    }

    @Test
    @DisplayName("INC-1-CORR-04 (CRIT-01): Rechaza URLs sin autoridad o con parametros de destino en query en APLICACION y PRUEBAS")
    void testCrit01BypassAutoridadVaciaYParametrosDestinoEnQuery() {
        String[] urlsProhibidas = {
            "jdbc:postgresql://?PGHOST=127.0.0.1&PGPORT=55432&PGDBNAME=tpi_pa2_r1_demo&ApplicationName=x/base_inocente",
            "jdbc:postgresql:///?PGDBNAME=tpi_pa2_r1_demo",
            "jdbc:postgresql://127.0.0.1:55432/?dbname=tpi_pa2_r1_demo",
            "jdbc:postgresql://127.0.0.1:55432/base_inocente?host=127.0.0.1",
            "jdbc:postgresql://127.0.0.1:55432/base_inocente?PGHOST=127.0.0.1",
            "jdbc:postgresql://127.0.0.1:55432/base_inocente?port=55432",
            "jdbc:postgresql://127.0.0.1:55432/base_inocente?PGPORT=55432",
            "jdbc:postgresql:tpi_pa2_r1_demo",
            "jdbc:postgresql://127.0.0.1:55432/base_inocente/extra"
        };

        for (String url : urlsProhibidas) {
            assertNull(ConexionBD.extraerNombreBase(url),
                    "extraerNombreBase debe rechazar URL ambigua o con bypass: " + url);

            ConexionBD.restablecerModoPorDefecto();
            ConexionBD.activarModoAplicacion();
            Map<String, String> envApp = entornoConUrl(url);
            assertThrows(SQLException.class, () -> ConexionBD.resolverParametrosActivos(envApp),
                    "Modo APLICACION debe rechazar preventivamente: " + url);

            ConexionBD.restablecerModoPorDefecto();
            ConexionBD.activarModoPruebas();
            Map<String, String> envTest = new HashMap<>();
            envTest.put(ConexionBD.ENV_TEST_DB_URL, url);
            assertThrows(SQLException.class, () -> ConexionBD.validarConfiguracionDestinoDePruebas(envTest),
                    "Modo PRUEBAS debe rechazar preventivamente: " + url);
        }

        // Una URL convencional valida con '/' dentro de ApplicationName debe seguir aceptandose
        assertEquals("mi_base", ConexionBD.extraerNombreBase(
                "jdbc:postgresql://127.0.0.1:55432/mi_base?ApplicationName=x/base_inocente"));
    }

    @Test
    @DisplayName("INC-1-CORR-04 (MOD-01): Rechaza NUL/%00, %2F, %5C y barras sin normalizacion previa que los oculte")
    void testMod01DecodificacionYCaracteresProhibidos() {
        String[] nombresProhibidos = {
            "%00tpi_pa2_r1_demo",
            "base%00",
            "  base%00  ",
            "base%00demo",
            "base%2Fextra",
            "base%5Cextra",
            "tpi_pa2_r1_demo/extra",
            "tpi_pa2_r1_demo\\extra",
            "base\0",
            "base_%2",
            "base_%GG"
        };
        for (String nombre : nombresProhibidos) {
            assertThrows(SQLException.class, () -> ConexionBD.validarBaseDeEjecucion(nombre),
                    "validarBaseDeEjecucion debe rechazar nombre prohibido: " + nombre);
            assertThrows(SQLException.class, () -> ConexionBD.validarBaseDePruebas(nombre),
                    "validarBaseDePruebas debe rechazar nombre prohibido: " + nombre);
        }

        assertNull(ConexionBD.extraerNombreBase("jdbc:postgresql://127.0.0.1:55432/base%00"));
        assertNull(ConexionBD.extraerNombreBase("jdbc:postgresql://127.0.0.1:55432/%00base"));
        assertNull(ConexionBD.extraerNombreBase("jdbc:postgresql://127.0.0.1:55432/base%2Fextra"));
        assertNull(ConexionBD.extraerNombreBase("jdbc:postgresql://127.0.0.1:55432/base%5Cextra"));
        assertNull(ConexionBD.extraerNombreBase("jdbc:postgresql://127.0.0.1:55432/base\\extra"));
    }

    @Test
    @DisplayName("INC-1-CORR-04 (MOD-02/MIN-01): INVALIDO queda enclavado tras lectura y mensajes conservan estado previo")
    void testMod02YMin01EnclavamientoInvalidoYMensajesCoherentes() {
        Map<String, String> ambosEntornos = new HashMap<>();
        ambosEntornos.put(ConexionBD.ENV_DB_URL, "jdbc:postgresql://127.0.0.1:59999/" + BASE_PROPIA);
        ambosEntornos.put(ConexionBD.ENV_TEST_DB_URL, "jdbc:postgresql://127.0.0.1:59999/" + BASE_PRUEBAS_AISLADA);

        // 1-3. Propiedad invalida consultada por obtenerModoEfectivo() enclava INVALIDO
        System.setProperty(ConexionBD.PROP_MODO_EJECUCION, "valor_invalido");
        SQLException exInv = assertThrows(SQLException.class,
                () -> ConexionBD.resolverParametrosActivos(ambosEntornos));
        assertTrue(exInv.getMessage().toUpperCase(Locale.ROOT).contains("INVALIDO")
                || exInv.getMessage().toLowerCase(Locale.ROOT).contains("invalido"));

        // 4-5. Corregir externamente la propiedad sin restablecer NO debe recuperar el modo
        System.setProperty(ConexionBD.PROP_MODO_EJECUCION, "pruebas");
        assertFalse(ConexionBD.esProcesoDePruebas(), "INVALIDO enclavado no debe recuperarse silenciosamente");
        assertThrows(SQLException.class, () -> ConexionBD.resolverParametrosActivos(ambosEntornos));
        assertThrows(SQLException.class, ConexionBD::obtenerConexion);
        assertThrows(IllegalStateException.class, ConexionBD::activarModoPruebas);

        // Tampoco limpiando la propiedad se recupera sin restablecer
        System.clearProperty(ConexionBD.PROP_MODO_EJECUCION);
        assertThrows(SQLException.class, () -> ConexionBD.resolverParametrosActivos(ambosEntornos));

        // 6-7. Recien tras restablecer explicitamente vuelve a operar
        ConexionBD.restablecerModoPorDefecto();
        ConexionBD.activarModoPruebas();
        assertTrue(ConexionBD.esProcesoDePruebas());

        // MIN-01: con APLICACION + propiedad 'aplicacion', activarModoPruebas() rechaza sin envenenar a CONTRADICTORIO
        ConexionBD.restablecerModoPorDefecto();
        System.setProperty(ConexionBD.PROP_MODO_EJECUCION, "aplicacion");
        ConexionBD.activarModoAplicacion();
        assertThrows(IllegalStateException.class, ConexionBD::activarModoPruebas);
        assertDoesNotThrow(() -> ConexionBD.resolverParametrosActivos(ambosEntornos));

        // MIN-01: si la propiedad muta a 'pruebas' tras fijar APLICACION, el mensaje reporta el modo previo APLICACION
        System.setProperty(ConexionBD.PROP_MODO_EJECUCION, "pruebas");
        IllegalStateException exTrans = assertThrows(IllegalStateException.class, ConexionBD::activarModoPruebas);
        assertTrue(exTrans.getMessage().contains("APLICACION"),
                "El mensaje debe reportar el modo previo APLICACION, no CONTRADICTORIO: " + exTrans.getMessage());
    }

    @Test
    @DisplayName("INC-1-CORR-04 (MOD-03): Rechaza configuracion si la URL contraparte de colision es invalida o ambigua")
    void testMod03UrlContraparteInvalidaFallaCerrada() {
        Map<String, String> appValidaTestAmbiguo = new HashMap<>();
        appValidaTestAmbiguo.put(ConexionBD.ENV_DB_URL, "jdbc:postgresql://127.0.0.1:59999/" + BASE_PROPIA);
        appValidaTestAmbiguo.put(ConexionBD.ENV_TEST_DB_URL,
                "jdbc:postgresql://?PGHOST=127.0.0.1&PGDBNAME=tpi_pa2_r1_demo&ApplicationName=x/y");

        ConexionBD.activarModoAplicacion();
        assertThrows(SQLException.class, () -> ConexionBD.resolverParametrosActivos(appValidaTestAmbiguo),
                "APLICACION debe rechazar si TPI_TEST_DB_URL es ambigua y no permite descartar colision");

        ConexionBD.restablecerModoPorDefecto();
        ConexionBD.activarModoPruebas();
        Map<String, String> testValidoAppAmbigua = new HashMap<>();
        testValidoAppAmbigua.put(ConexionBD.ENV_TEST_DB_URL, "jdbc:postgresql://127.0.0.1:59999/" + BASE_PRUEBAS_AISLADA);
        testValidoAppAmbigua.put(ConexionBD.ENV_DB_URL, "jdbc:postgresql://127.0.0.1:59999/base_x?dbname=otro");
        assertThrows(SQLException.class, () -> ConexionBD.validarConfiguracionDestinoDePruebas(testValidoAppAmbigua),
                "PRUEBAS debe rechazar si TPI_DB_URL es ambigua y no permite descartar colision");
    }

    @Test
    @DisplayName("INC-1-CORR-04 (MIN-03): Preserva passwords con secuencia literal '${' y rechaza placeholders reales sin resolver")
    void testMin03CredencialesConSecuenciaLiteralYPlaceholders() throws Exception {
        ConexionBD.activarModoAplicacion();
        Map<String, String> conPasswordLiteral = new HashMap<>();
        conPasswordLiteral.put(ConexionBD.ENV_DB_URL, "jdbc:postgresql://127.0.0.1:59999/" + BASE_PROPIA);
        conPasswordLiteral.put(ConexionBD.ENV_DB_USER, "usuario_app");
        conPasswordLiteral.put(ConexionBD.ENV_DB_PASSWORD, "P@ss${123}!");

        ConexionBD.ParametrosConexion paramsLiteral = ConexionBD.resolverParametrosActivos(conPasswordLiteral);
        assertEquals("P@ss${123}!", paramsLiteral.password,
                "Una password legitima que contiene '${' no debe truncarse a vacio.");

        Map<String, String> conPlaceholder = new HashMap<>();
        conPlaceholder.put(ConexionBD.ENV_DB_URL, "jdbc:postgresql://127.0.0.1:59999/" + BASE_PROPIA);
        conPlaceholder.put(ConexionBD.ENV_DB_PASSWORD, "${env.TPI_DB_PASSWORD}");
        ConexionBD.ParametrosConexion paramsPlaceholder = ConexionBD.resolverParametrosActivos(conPlaceholder);
        assertEquals("", paramsPlaceholder.password,
                "Un placeholder sin resolver ${env.*} debe tratarse como ausente.");
    }

    @Test
    @DisplayName("INC-1-CORR-04 (MIN-04 & Aislamiento): run_app.bat propaga exit code con expansion segura y *Test no consulta System.getenv(TPI_TEST_DB_URL)")
    void testMin04RunAppBatYAislamientoUnitario() throws IOException {
        String runAppBat = Files.readString(Paths.get("run_app.bat"), StandardCharsets.UTF_8);
        assertTrue(runAppBat.contains("EnableDelayedExpansion"),
                "run_app.bat debe habilitar expansion retardada para comparar variables de forma segura.");
        assertTrue(runAppBat.contains("exit /b %EXIT_CODE%"),
                "run_app.bat debe propagar explicitamente EXIT_CODE tras endlocal.");
        assertFalse(runAppBat.contains("set \"TPI_TEST_DB_URL=\""),
                "run_app.bat debe preservar TPI_TEST_DB_URL para deteccion de colisiones.");

        String srcUnit = Files.readString(
                Paths.get("src/test/java/com/gestionamateur/tpi/progav/dao/AislamientoBasePruebasTest.java"),
                StandardCharsets.UTF_8);
        assertFalse(srcUnit.contains("System.getenv("),
                "AislamientoBasePruebasTest (*Test) no debe leer variables del SO ni abrir conexiones reales.");
    }

    @Test
    @DisplayName("INC-1-CORR-04 (CRIT-01 Gate): ConectorJdbc recibe 0 invocaciones ante destinos prohibidos o ambiguos y 1 ante destino valido")
    void testGatePreventivoAntesDeConectorJdbc() throws Exception {
        java.util.concurrent.atomic.AtomicInteger invocaciones = new java.util.concurrent.atomic.AtomicInteger(0);
        ConexionBD.ConectorJdbc conectorEspia = (url, user, password) -> {
            invocaciones.incrementAndGet();
            return null;
        };

        ConexionBD.activarModoAplicacion();
        String[] urlsBloqueadasApp = {
                "jdbc:postgresql://127.0.0.1:59999/tpi_pa2_r1_demo",
                "jdbc:postgresql://?PGHOST=127.0.0.1&PGPORT=59999&PGDBNAME=tpi_pa2_r1_demo&ApplicationName=x/" + BASE_PROPIA,
                "jdbc:postgresql://127.0.0.1:59999/" + BASE_PROPIA + "?service=prod_svc"
        };
        for (String urlBloqueada : urlsBloqueadasApp) {
            Map<String, String> env = new HashMap<>();
            env.put(ConexionBD.ENV_DB_URL, urlBloqueada);
            assertThrows(SQLException.class, () -> ConexionBD.obtenerConexionCon(env, conectorEspia),
                    "Debe bloquear antes de invocar al conector JDBC en APLICACION: " + urlBloqueada);
        }
        assertEquals(0, invocaciones.get(),
                "Ninguna URL rechazada en APLICACION debe llegar a DriverManager.getConnection.");

        ConexionBD.restablecerModoPorDefecto();
        ConexionBD.activarModoPruebas();
        String[] urlsBloqueadasTest = {
                "jdbc:postgresql://127.0.0.1:59999/tpi_pa2_r1_demo",
                "jdbc:postgresql://?PGHOST=127.0.0.1&PGPORT=59999&PGDBNAME=postgres",
                "jdbc:postgresql://127.0.0.1:59999/" + BASE_PRUEBAS_AISLADA + "?dbname=tpi_pa2_r1_demo"
        };
        for (String urlBloqueada : urlsBloqueadasTest) {
            Map<String, String> env = new HashMap<>();
            env.put(ConexionBD.ENV_TEST_DB_URL, urlBloqueada);
            assertThrows(SQLException.class, () -> ConexionBD.obtenerConexionTestCon(env, conectorEspia),
                    "Debe bloquear antes de invocar al conector JDBC en PRUEBAS: " + urlBloqueada);
        }
        assertEquals(0, invocaciones.get(),
                "Ninguna URL rechazada en PRUEBAS debe llegar a DriverManager.getConnection.");

        ConexionBD.restablecerModoPorDefecto();
        ConexionBD.activarModoAplicacion();
        Map<String, String> envValido = new HashMap<>();
        envValido.put(ConexionBD.ENV_DB_URL, "jdbc:postgresql://127.0.0.1:59999/" + BASE_PROPIA);
        envValido.put(ConexionBD.ENV_DB_USER, "usuario_app");
        envValido.put(ConexionBD.ENV_DB_PASSWORD, "secreto");
        ConexionBD.obtenerConexionCon(envValido, conectorEspia);
        assertEquals(1, invocaciones.get(),
                "Con configuracion valida en APLICACION debe invocar exactamente una vez al conector JDBC.");
    }

    @Test
    @DisplayName("CORR-E2-MAVEN-IT-01 (OBS-VAL-02): pom.xml separa *Test por defecto de *IT en perfil explicito 'integracion'")
    void testSeparacionSurefireYPerfilIntegracionEnPom() throws Exception {
        String pom = Files.readString(Paths.get("pom.xml"), StandardCharsets.UTF_8);

        int idxBuild = pom.indexOf("<build>");
        int idxProfiles = pom.indexOf("<profiles>");
        assertTrue(idxBuild >= 0 && idxProfiles > idxBuild,
                "pom.xml debe definir <profiles> separado del <build> por defecto.");

        String buildDefault = pom.substring(idxBuild, idxProfiles);
        assertFalse(buildDefault.contains("**/*IT.java</include>"),
                "La configuracion Surefire por defecto no debe incluir **/*IT.java.");
        assertTrue(buildDefault.contains("<exclude>**/*IT.java</exclude>"),
                "La configuracion Surefire por defecto debe excluir explicitamente **/*IT.java.");
        assertTrue(buildDefault.contains("<tpi.it.habilitado>${tpi.it.habilitado}</tpi.it.habilitado>"),
                "pom.xml debe propagar la propiedad tpi.it.habilitado al proceso de pruebas.");

        String seccionProfiles = pom.substring(idxProfiles);
        assertTrue(seccionProfiles.contains("<id>integracion</id>"),
                "pom.xml debe declarar el perfil explicito 'integracion'.");
        assertFalse(seccionProfiles.contains("<activation>"),
                "El perfil 'integracion' no debe tener <activation> automatica por entorno o propiedades.");
        assertTrue(seccionProfiles.contains("<phase>integration-test</phase>"),
                "El perfil 'integracion' debe vincular las *IT a la fase integration-test.");
        assertTrue(seccionProfiles.contains("<include>**/*IT.java</include>"),
                "El perfil 'integracion' debe seleccionar **/*IT.java en su ejecucion dedicada.");
        assertTrue(seccionProfiles.contains("<parallel>none</parallel>")
                        && seccionProfiles.contains("<forkCount>1</forkCount>")
                        && seccionProfiles.contains("<runOrder>alphabetical</runOrder>"),
                "El perfil 'integracion' debe garantizar ejecucion secuencial determinista.");
    }

    @Test
    @DisplayName("CORR-E2-MAVEN-IT-01 (5.1/8.3): Las 11 suites *IT exigen -Dtpi.it.habilitado=true antes de activar modo o conectar")
    void testTodasLas11SuitesITExigenHabilitacionExplicitaAntesDeConexion() throws IOException {
        Path raizTests = Paths.get("src/test/java");
        List<Path> suitesIT;
        try (Stream<Path> walk = Files.walk(raizTests)) {
            suitesIT = walk.filter(p -> p.getFileName().toString().endsWith("IT.java"))
                    .sorted()
                    .collect(Collectors.toList());
        }
        assertEquals(11, suitesIT.size(), "Deben existir exactamente 11 suites *IT.");

        for (Path suite : suitesIT) {
            String src = Files.readString(suite, StandardCharsets.UTF_8);
            int idxGuardia = src.indexOf("ControlEjecucionIntegracion.exigirHabilitacionExplicita(");
            int idxActivar = src.indexOf("ConexionBD.activarModoPruebas()");
            assertTrue(idxGuardia >= 0,
                    suite.getFileName() + " debe exigir ControlEjecucionIntegracion.exigirHabilitacionExplicita(...) antes de operar.");
            assertTrue(idxActivar > idxGuardia,
                    suite.getFileName() + " debe ejecutar la guardia de habilitacion explicita antes de ConexionBD.activarModoPruebas().");
        }

        // Comportamiento en tiempo de ejecucion de la guardia (sin habilitar vs habilitada)
        String prevHabilitado = System.getProperty(ControlEjecucionIntegracion.PROP_IT_HABILITADO);
        String prevTestUrl = System.getProperty(ConexionBD.ENV_TEST_DB_URL);
        try {
            // 1. Aun con TPI_TEST_DB_URL configurada, sin tpi.it.habilitado=true debe abortarse por Assumptions
            System.setProperty(ConexionBD.ENV_TEST_DB_URL, "jdbc:postgresql://127.0.0.1:59999/" + BASE_PRUEBAS_AISLADA);
            System.clearProperty(ControlEjecucionIntegracion.PROP_IT_HABILITADO);
            assertThrows(org.opentest4j.TestAbortedException.class,
                    () -> ControlEjecucionIntegracion.exigirHabilitacionExplicita("SuitePrueba"));

            System.setProperty(ControlEjecucionIntegracion.PROP_IT_HABILITADO, "false");
            assertThrows(org.opentest4j.TestAbortedException.class,
                    () -> ControlEjecucionIntegracion.exigirHabilitacionExplicita("SuitePrueba"));

            // 2. Con habilitacion explicita, supera la guardia y aplica la validacion fail-closed de destino
            System.setProperty(ControlEjecucionIntegracion.PROP_IT_HABILITADO, "true");
            assertDoesNotThrow(() -> ControlEjecucionIntegracion.exigirHabilitacionExplicita("SuitePrueba"));
            ConexionBD.activarModoPruebas();
            assertThrows(SQLException.class, () -> ConexionBD.validarConfiguracionDestinoDePruebas(new HashMap<>()),
                    "Con habilitacion explicita y sin TPI_TEST_DB_URL debe rechazar de forma controlada.");
            Map<String, String> envProtegida = new HashMap<>();
            envProtegida.put(ConexionBD.ENV_TEST_DB_URL, "jdbc:postgresql://127.0.0.1:59999/tpi_pa2_r1_demo");
            assertThrows(SQLException.class, () -> ConexionBD.validarConfiguracionDestinoDePruebas(envProtegida),
                    "Con habilitacion explicita y base protegida debe rechazar de forma controlada.");
            Map<String, String> envDescartable = new HashMap<>();
            envDescartable.put(ConexionBD.ENV_TEST_DB_URL, "jdbc:postgresql://127.0.0.1:59999/" + BASE_PRUEBAS_AISLADA);
            assertDoesNotThrow(() -> ConexionBD.validarConfiguracionDestinoDePruebas(envDescartable),
                    "Con habilitacion explicita y destino descartable valido debe permitir continuar.");
        } finally {
            if (prevHabilitado == null) {
                System.clearProperty(ControlEjecucionIntegracion.PROP_IT_HABILITADO);
            } else {
                System.setProperty(ControlEjecucionIntegracion.PROP_IT_HABILITADO, prevHabilitado);
            }
            if (prevTestUrl == null) {
                System.clearProperty(ConexionBD.ENV_TEST_DB_URL);
            } else {
                System.setProperty(ConexionBD.ENV_TEST_DB_URL, prevTestUrl);
            }
            ConexionBD.restablecerModoPorDefecto();
        }
    }
}
