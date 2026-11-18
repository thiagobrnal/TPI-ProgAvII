package com.gestionamateur.tpi.progav.dao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        assertDoesNotThrow(() -> ConexionBD.validarBaseDePruebas("tpi_pa2_r1_fix_test_20261111"));
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
        entorno.put(ConexionBD.ENV_TEST_DB_URL, "jdbc:postgresql://127.0.0.1:55432/tpi_pa2_r1_fix_test_20261111");
        entorno.put(ConexionBD.ENV_TEST_DB_USER, "postgres");
        entorno.put(ConexionBD.ENV_TEST_DB_PASSWORD, "");
        assertDoesNotThrow(() -> ConexionBD.leerParametrosDePruebas(entorno));
    }

    @Test
    @DisplayName("La base de pruebas efectiva se verifica y nunca es una base reservada")
    void testBaseEfectivaVerificada() throws Exception {
        String url = System.getenv(ConexionBD.ENV_TEST_DB_URL);
        org.junit.jupiter.api.Assumptions.assumeTrue(url != null && !url.trim().isEmpty(),
                "Requiere TPI_TEST_DB_URL configurada para verificar la base efectiva.");
        String efectiva = ConexionBD.validarDestinoDePruebas();
        assertNotNull(efectiva);
        assertTrue(!ConexionBD.BASES_PROTEGIDAS.contains(efectiva.toLowerCase(java.util.Locale.ROOT)),
                "La base efectiva '" + efectiva + "' es una base reservada.");
        assertEquals(ConexionBD.extraerNombreBase(url), efectiva,
                "La base conectada debe coincidir exactamente con la solicitada.");
    }
}
