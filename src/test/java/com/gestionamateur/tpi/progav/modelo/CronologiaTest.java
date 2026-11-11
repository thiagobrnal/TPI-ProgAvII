package com.gestionamateur.tpi.progav.modelo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Modelo Cronologia y Distinción de Expulsiones (INF-03R)")
public class CronologiaTest {

    private Jugador jugador;
    private Partido partido;

    @BeforeEach
    void setUp() throws TorneoExcepcion {
        jugador = new Jugador("Lucas Gómez", 38112233, new Date(), "Delantero", TiposDatos.EstadoJugador.ACTIVO);
        jugador.setId(1L);
        Cancha cancha = new Cancha("Cancha 1", "Av. Principal 100", "Sintético");
        cancha.setIdCancha(1L);
        partido = new Partido(new Date(), cancha);
        partido.setIdPartido(10L);
    }

    @Test
    @DisplayName("Debe permitir registrar gol e incidencia básica")
    void testRegistrarGol() throws TorneoExcepcion {
        Cronologia gol = new Cronologia(23, TiposDatos.TipoAccion.GOL, null, jugador, partido, "Gol de jugada");
        assertNotNull(gol);
        assertEquals(23, gol.getMinutoPartido());
        assertEquals(TiposDatos.TipoAccion.GOL, gol.getAccion());
        assertNull(gol.getTipoExpulsion());
    }

    @Test
    @DisplayName("Debe permitir registrar TARJETA_ROJA con tipo DIRECTA")
    void testRojaDirecta() throws TorneoExcepcion {
        Cronologia roja = new Cronologia(65, TiposDatos.TipoAccion.TARJETA_ROJA, "DIRECTA", jugador, partido, "Falta de último recurso");
        assertEquals("DIRECTA", roja.getTipoExpulsion());
        assertEquals(TiposDatos.TipoAccion.TARJETA_ROJA, roja.getAccion());
    }

    @Test
    @DisplayName("Debe permitir registrar TARJETA_ROJA con tipo DOBLE_AMARILLA")
    void testDobleAmarilla() throws TorneoExcepcion {
        Cronologia rojaDoble = new Cronologia(82, TiposDatos.TipoAccion.TARJETA_ROJA, "DOBLE_AMARILLA", jugador, partido, "Segunda amonestación");
        assertEquals("DOBLE_AMARILLA", rojaDoble.getTipoExpulsion());
    }

    @Test
    @DisplayName("Debe rechazar tipo de expulsión inválido")
    void testTipoExpulsionInvalido() {
        assertThrows(TorneoExcepcion.class, () -> {
            new Cronologia(45, TiposDatos.TipoAccion.TARJETA_ROJA, "TRIPLEMENTE_AMARILLA", jugador, partido, "Invalido");
        });
    }

    @Test
    @DisplayName("Debe rechazar minuto de juego negativo")
    void testMinutoNegativo() {
        assertThrows(TorneoExcepcion.class, () -> {
            new Cronologia(-5, TiposDatos.TipoAccion.TARJETA_AMARILLA, null, jugador, partido, "Minuto negativo");
        });
    }
}
