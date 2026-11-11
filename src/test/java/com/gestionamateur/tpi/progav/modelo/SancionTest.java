package com.gestionamateur.tpi.progav.modelo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Modelo Sancion (INF-03R / INF-04)")
public class SancionTest {

    private Jugador jugador;
    private Partido partido;
    private Torneo torneo;

    @BeforeEach
    void setUp() throws TorneoExcepcion {
        jugador = new Jugador("Federico Romero", 35123456, new Date(), "Mediocampista", TiposDatos.EstadoJugador.ACTIVO);
        jugador.setId(5L);
        Cancha cancha = new Cancha("Predio A", "Calle 1", "Tierra");
        cancha.setIdCancha(2L);
        partido = new Partido(new Date(), cancha);
        partido.setIdPartido(20L);
        torneo = new Torneo("Torneo Apertura 2026", "LIGA", 8, new Date());
        torneo.setIdTorneo(1L);
    }

    @Test
    @DisplayName("Debe instanciar sanción disciplinaria completa con atributos relacionales")
    void testCrearSancionCompleta() throws TorneoExcepcion {
        Date ahora = new Date();
        Sancion s = new Sancion(
            ahora, null,
            "Suspensión por conducta antideportiva",
            "MEDIA", jugador, partido, torneo,
            "TORNEO", "ROJA_DIRECTA", 2, 0, "PENDIENTE"
        );

        assertNotNull(s);
        assertEquals(jugador, s.getJugadorSancionado());
        assertEquals(partido, s.getPartidoOrigen());
        assertEquals(torneo, s.getTorneo());
        assertEquals("TORNEO", s.getAlcance());
        assertEquals("ROJA_DIRECTA", s.getTipoOrigen());
        assertEquals(2, s.getPartidosSuspension());
        assertEquals(0, s.getFechasCumplidas());
        assertEquals("PENDIENTE", s.getEstado());
    }

    @Test
    @DisplayName("Debe rechazar motivo vacío o menor a 10 caracteres")
    void testMotivoInvalido() {
        assertThrows(TorneoExcepcion.class, () -> {
            new Sancion(new Date(), null, "Corto", "LEVE", jugador);
        });
    }

    @Test
    @DisplayName("Debe rechazar partidos de suspensión menores a 1")
    void testPartidosSuspensionInvalido() {
        assertThrows(TorneoExcepcion.class, () -> {
            new Sancion(
                new Date(), null, "Motivo reglamentario suficiente", "LEVE", jugador,
                partido, torneo, "TORNEO", "ADMINISTRATIVA", 0, 0, "ACTIVA"
            );
        });
    }

    @Test
    @DisplayName("Debe rechazar fechas cumplidas mayores a partidos de suspensión")
    void testFechasCumplidasExcedidas() {
        assertThrows(TorneoExcepcion.class, () -> {
            new Sancion(
                new Date(), null, "Motivo reglamentario suficiente", "LEVE", jugador,
                partido, torneo, "TORNEO", "ADMINISTRATIVA", 2, 3, "ACTIVA"
            );
        });
    }
}
