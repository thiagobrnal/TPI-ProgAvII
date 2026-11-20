package com.gestionamateur.tpi.progav.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DEF-004 - Aislamiento fail-closed del entorno de pruebas.
 *
 * Verifica que la conexion de pruebas:
 *  1. no se resuelve por valor por defecto,
 *  2. rechaza explicitamente las bases reservadas del proyecto,
 *  3. rechaza la base que usa la aplicacion,
 *  4. y verifica la base realmente conectada antes de operar.
 */
@DisplayName("DEF-004: Aislamiento fail-closed de la base de datos de pruebas")
public class AislamientoBasePruebasTest {

    private String prevPropModo;
    private String prevPropDbUrl;
    private String prevPropTestDbUrl;

    @BeforeEach
    void prepararModoConexion() {
        prevPropModo = System.getProperty(ConexionBD.PROP_MODO_EJECUCION);
        prevPropDbUrl = System.getProperty(ConexionBD.ENV_DB_URL);
        prevPropTestDbUrl = System.getProperty(ConexionBD.ENV_TEST_DB_URL);
        System.clearProperty(ConexionBD.PROP_MODO_EJECUCION);
        System.clearProperty(ConexionBD.ENV_DB_URL);
        System.clearProperty(ConexionBD.ENV_TEST_DB_URL);
        ConexionBD.restablecerModoPorDefecto();
    }

    @AfterEach
    void restaurarModoConexion() {
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

    @Test
    @DisplayName("Extraccion del nombre de base desde distintos formatos de URL JDBC")
    void testExtraccionNombreBase() {
        assertEquals("mi_base", ConexionBD.extraerNombreBase("jdbc:postgresql://127.0.0.1:55432/mi_base"));
        assertEquals("mi_base", ConexionBD.extraerNombreBase("jdbc:postgresql://localhost/mi_base"));
        assertEquals("mi_base", ConexionBD.extraerNombreBase("jdbc:postgresql://127.0.0.1:55432/mi_base?ssl=true"));
        assertEquals("mi_base", ConexionBD.extraerNombreBase("jdbc:postgresql://127.0.0.1:55432/mi_base?user=postgres&password=x"));
        assertEquals(null, ConexionBD.extraerNombreBase("jdbc:postgresql://127.0.0.1:55432"));
        assertEquals(null, ConexionBD.extraerNombreBase(null));
    }

    @Test
    @DisplayName("Las bases reservadas del proyecto se rechazan como destino de pruebas")
    void testBasesProtegidasRechazadas() {
        for (String protegida : ConexionBD.BASES_PROTEGIDAS) {
            SQLException e = assertThrows(SQLException.class,
                    () -> ConexionBD.validarBaseDePruebas(protegida),
                    "La base reservada '" + protegida + "' debio rechazarse.");
            assertTrue(e.getMessage().contains("reservada"),
                    "El mensaje de rechazo debe identificar la base reservada: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("El rechazo de base protegida no distingue mayusculas ni espacios")
    void testBasesProtegidasCaseInsensitive() {
        assertThrows(SQLException.class, () -> ConexionBD.validarBaseDePruebas("TPI_PA2_R1_DEMO"));
        assertThrows(SQLException.class, () -> ConexionBD.validarBaseDePruebas("  tpi_pa2_inf06_test  "));
    }

    @Test
    @DisplayName("Una base de pruebas exclusiva es aceptada")
    void testBaseDePruebasExclusivaAceptada() {
        assertDoesNotThrow(() -> ConexionBD.validarBaseDePruebas("tpi_pa2_r1_base_descartable_local"));
        assertDoesNotThrow(() -> ConexionBD.validarBaseDePruebas("otra_base_descartable"));
    }

    @Test
    @DisplayName("Un nombre de base indeterminable se rechaza")
    void testNombreDeBaseIndeterminableRechazado() {
        assertThrows(SQLException.class, () -> ConexionBD.validarBaseDePruebas(null));
        assertThrows(SQLException.class, () -> ConexionBD.validarBaseDePruebas("   "));
    }

    @Test
    @DisplayName("Sin URL de pruebas explicita la corrida falla cerrada con mensaje accionable")
    void testSinUrlDePruebasFallaCerrada() {
        // Entorno vacio: debe fallar cerrada, sin abrir ninguna conexion.
        SQLException e = assertThrows(SQLException.class,
                () -> ConexionBD.leerParametrosDePruebas(new java.util.HashMap<>()));
        assertTrue(e.getMessage().contains(ConexionBD.ENV_TEST_DB_URL),
                "El mensaje debe indicar la variable que falta: " + e.getMessage());
        assertTrue(e.getMessage().contains("valor por defecto"),
                "El mensaje debe dejar claro que no hay valor por defecto: " + e.getMessage());
    }

    @Test
    @DisplayName("Una referencia de entorno sin resolver se trata como variable ausente")
    void testReferenciaSinResolverSeTrataComoAusente() {
        java.util.Map<String, String> entorno = new java.util.HashMap<>();
        entorno.put(ConexionBD.ENV_TEST_DB_URL, "${env.TPI_TEST_DB_URL}");
        SQLException e = assertThrows(SQLException.class, () -> ConexionBD.leerParametrosDePruebas(entorno));
        assertTrue(e.getMessage().contains(ConexionBD.ENV_TEST_DB_URL),
                "El mensaje debe indicar la variable que falta: " + e.getMessage());
    }

    @Test
    @DisplayName("Con URL explicita valida se resuelven los parametros de pruebas")
    void testUrlExplicitaValidaSeResuelve() {
        java.util.Map<String, String> entorno = new java.util.HashMap<>();
        entorno.put(ConexionBD.ENV_TEST_DB_URL, "jdbc:postgresql://127.0.0.1:55432/tpi_pa2_r1_base_descartable_local");
        entorno.put(ConexionBD.ENV_TEST_DB_USER, "postgres");
        entorno.put(ConexionBD.ENV_TEST_DB_PASSWORD, "");
        assertDoesNotThrow(() -> ConexionBD.leerParametrosDePruebas(entorno));
    }

    @Test
    @DisplayName("La suite unitaria AislamientoBasePruebasTest permanece aislada de PostgreSQL real")
    void testSuiteUnitariaNoAbreConexionesReales() {
        ConexionBD.activarModoPruebas();
        java.util.concurrent.atomic.AtomicInteger invocaciones = new java.util.concurrent.atomic.AtomicInteger(0);
        assertThrows(SQLException.class,
                () -> ConexionBD.obtenerConexionCon(new java.util.HashMap<>(), (u, usr, p) -> {
                    invocaciones.incrementAndGet();
                    return null;
                }),
                "Con mapa de entorno vacio en modo PRUEBAS debe fallar cerrado sin invocar al conector JDBC.");
        assertEquals(0, invocaciones.get(),
                "No debe invocar al conector JDBC ni depender de TPI_TEST_DB_URL del SO.");
        assertTrue(AislamientoBasePruebasIT.class.getSimpleName().endsWith("IT"),
                "La verificacion real contra PostgreSQL debe vivir separada en AislamientoBasePruebasIT.");
    }
}
