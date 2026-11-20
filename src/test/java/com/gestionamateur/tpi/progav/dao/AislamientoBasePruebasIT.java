package com.gestionamateur.tpi.progav.dao;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verificacion de integracion real de la base de pruebas contra PostgreSQL.
 * Solo se ejecuta cuando se habilita explicitamente {@code -Dtpi.it.habilitado=true}
 * y existe {@code TPI_TEST_DB_URL}.
 */
class AislamientoBasePruebasIT {

    @BeforeAll
    static void prepararIntegracion() {
        ControlEjecucionIntegracion.exigirHabilitacionExplicita("AislamientoBasePruebasIT");
        ConexionBD.activarModoPruebas();
    }

    @AfterAll
    static void restablecerModo() {
        ConexionBD.restablecerModoPorDefecto();
    }

    @Test
    @DisplayName("La base de pruebas efectiva se verifica y nunca es una base reservada")
    void testBaseEfectivaVerificada() throws Exception {
        String efectiva = ConexionBD.validarDestinoDePruebas();
        String url = System.getenv(ConexionBD.ENV_TEST_DB_URL);
        if (url == null || url.trim().isEmpty()) {
            url = System.getProperty(ConexionBD.ENV_TEST_DB_URL);
        }
        assertNotNull(efectiva);
        assertTrue(!ConexionBD.BASES_PROTEGIDAS.contains(efectiva.toLowerCase(Locale.ROOT)),
                "La base efectiva '" + efectiva + "' es una base reservada.");
        String esperada = ConexionBD.extraerNombreBase(url);
        assertNotNull(esperada);
        assertTrue(esperada.equalsIgnoreCase(efectiva),
                "La base conectada ('" + efectiva + "') debe coincidir con la solicitada ('" + esperada + "').");
    }
}
