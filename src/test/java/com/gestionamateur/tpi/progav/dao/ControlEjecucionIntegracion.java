package com.gestionamateur.tpi.progav.dao;

import org.junit.jupiter.api.Assumptions;

/**
 * Guardia comun de autorizacion explicita para todas las suites de integracion (*IT).
 * Exige {@code -Dtpi.it.habilitado=true} antes de activar modo PRUEBAS, consultar DAOs/servicios
 * o validar conexiones PostgreSQL, evitando ejecuciones accidentales aun si {@code TPI_TEST_DB_URL}
 * estuviera definida en el entorno.
 */
public final class ControlEjecucionIntegracion {

    public static final String PROP_IT_HABILITADO = "tpi.it.habilitado";

    private ControlEjecucionIntegracion() {}

    public static void exigirHabilitacionExplicita(String nombreSuite) {
        Assumptions.assumeTrue(estaHabilitada(),
                nombreSuite + ": integracion omitida; requiere activacion explicita -D"
                        + PROP_IT_HABILITADO + "=true.");
    }

    static boolean estaHabilitada() {
        return "true".equalsIgnoreCase(System.getProperty(PROP_IT_HABILITADO));
    }
}
