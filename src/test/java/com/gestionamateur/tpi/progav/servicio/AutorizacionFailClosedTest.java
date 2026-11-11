package com.gestionamateur.tpi.progav.servicio;

import com.gestionamateur.tpi.progav.modelo.Cronologia;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Autorizacion fail-closed: toda operacion protegida debe rechazarse cuando no
 * hay un operador autenticado o cuando el rol no alcanza.
 *
 * Estas pruebas no tocan PostgreSQL: el control de autorizacion se ejecuta antes
 * de abrir cualquier conexion, por lo que el rechazo es inmediato.
 */
class AutorizacionFailClosedTest {

    private static final String ESPERADO = "operador autenticado";

    @Test
    @DisplayName("EquipoService: sin operador se rechaza el alta, la edicion y la incorporacion al plantel")
    void equipoServiceRechazaSinOperador() {
        EquipoService servicio = new EquipoService();

        TorneoExcepcion e1 = assertThrows(TorneoExcepcion.class,
                () -> servicio.registrarEquipo("Equipo", "EQ", 1L, "Azul", null, 1L, null));
        assertTrue(e1.getMessage().contains(ESPERADO), e1.getMessage());

        TorneoExcepcion e2 = assertThrows(TorneoExcepcion.class,
                () -> servicio.agregarJugadorAPlantel(1L, 1L, null));
        assertTrue(e2.getMessage().contains(ESPERADO), e2.getMessage());

        TorneoExcepcion e3 = assertThrows(TorneoExcepcion.class,
                () -> servicio.actualizarEquipo(1L, "Equipo", "EQ", 1L, "Azul", 1L, null, null));
        assertTrue(e3.getMessage().contains(ESPERADO), e3.getMessage());

        TorneoExcepcion e4 = assertThrows(TorneoExcepcion.class,
                () -> servicio.resolverSolicitudInscripcion(1L, true, "ok", null));
        assertTrue(e4.getMessage().contains(ESPERADO), e4.getMessage());
    }

    @Test
    @DisplayName("JugadorService: sin operador se rechaza el alta de jugador")
    void jugadorServiceRechazaSinOperador() {
        JugadorService servicio = new JugadorService();

        TorneoExcepcion e = assertThrows(TorneoExcepcion.class,
                () -> servicio.registrarJugador("Jugador", 12345678, new Date(), "Delantero", 1L, null));
        assertTrue(e.getMessage().contains(ESPERADO), e.getMessage());
    }

    @Test
    @DisplayName("PartidoService: sin operador se rechaza programar y cerrar actas")
    void partidoServiceRechazaSinOperador() {
        PartidoService servicio = new PartidoService();
        Date ahora = new Date();
        List<Cronologia> incidencias = new ArrayList<>();

        TorneoExcepcion e1 = assertThrows(TorneoExcepcion.class,
                () -> servicio.programarPartido(1L, 1L, 2L, 1L, 1L, ahora, 90, null));
        assertTrue(e1.getMessage().contains(ESPERADO), e1.getMessage());

        TorneoExcepcion e2 = assertThrows(TorneoExcepcion.class,
                () -> servicio.cerrarActaPartido(1L, ahora, incidencias, null));
        assertTrue(e2.getMessage().contains(ESPERADO), e2.getMessage());

        TorneoExcepcion e3 = assertThrows(TorneoExcepcion.class,
                () -> servicio.cerrarActaPartido(1L, ahora, 1, 0, null));
        assertTrue(e3.getMessage().contains(ESPERADO), e3.getMessage());
    }

    @Test
    @DisplayName("PartidoService: sin operador se rechaza postergar y suspender encuentros")
    void partidoServiceRechazaSuspensionesSinOperador() {
        PartidoService servicio = new PartidoService();

        TorneoExcepcion e1 = assertThrows(TorneoExcepcion.class,
                () -> servicio.posponerPartido(1L, "Lluvia", null));
        assertTrue(e1.getMessage().contains(ESPERADO), e1.getMessage());

        TorneoExcepcion e2 = assertThrows(TorneoExcepcion.class,
                () -> servicio.suspenderPartido(1L, "Clima", null));
        assertTrue(e2.getMessage().contains(ESPERADO), e2.getMessage());
    }

    @Test
    @DisplayName("DisciplinaService: sin operador se rechaza emitir o anular sanciones")
    void disciplinaServiceRechazaSinOperador() {
        DisciplinaService servicio = new DisciplinaService();

        TorneoExcepcion e1 = assertThrows(TorneoExcepcion.class,
                () -> servicio.emitirFalloTribunal(1L, 1, "Conducta antideportiva", "GRAVE", null));
        assertTrue(e1.getMessage().contains(ESPERADO), e1.getMessage());

        TorneoExcepcion e2 = assertThrows(TorneoExcepcion.class,
                () -> servicio.anularSancion(1L, "Error de carga", null));
        assertTrue(e2.getMessage().contains(ESPERADO), e2.getMessage());

        TorneoExcepcion e3 = assertThrows(TorneoExcepcion.class,
                () -> servicio.registrarSancionAdministrativa(1L, 1, "Conducta antideportiva", "GRAVE", null));
        assertTrue(e3.getMessage().contains(ESPERADO), e3.getMessage());
    }

    @Test
    @DisplayName("ReclamoService: sin operador se rechaza presentar, apelar y resolver reclamos")
    void reclamoServiceRechazaSinOperador() {
        ReclamoService servicio = new ReclamoService();

        TorneoExcepcion e1 = assertThrows(TorneoExcepcion.class,
                () -> servicio.presentarReclamoPartido(1L, 1L, "Falta dudosa", null));
        assertTrue(e1.getMessage().contains(ESPERADO), e1.getMessage());

        TorneoExcepcion e2 = assertThrows(TorneoExcepcion.class,
                () -> servicio.apelarSancion(1L, 1L, "Sancion injusta", null));
        assertTrue(e2.getMessage().contains(ESPERADO), e2.getMessage());

        TorneoExcepcion e3 = assertThrows(TorneoExcepcion.class,
                () -> servicio.resolverExpediente(1L, "A FAVOR", "Fundamentacion del veredicto", null));
        assertTrue(e3.getMessage().contains(ESPERADO), e3.getMessage());
    }

    @Test
    @DisplayName("TorneoService: sin operador se rechaza crear torneo, agregar fases y cambiar el estado")
    void torneoServiceRechazaSinOperador() {
        TorneoService servicio = new TorneoService();
        Date inicio = new Date();

        TorneoExcepcion e1 = assertThrows(TorneoExcepcion.class,
                () -> servicio.crearTorneo("Torneo", "LIGA", 8, inicio, null));
        assertTrue(e1.getMessage().contains(ESPERADO), e1.getMessage());

        TorneoExcepcion e2 = assertThrows(TorneoExcepcion.class,
                () -> servicio.agregarFase(1L, "Fecha 1", 1, true, null));
        assertTrue(e2.getMessage().contains(ESPERADO), e2.getMessage());

        TorneoExcepcion e3 = assertThrows(TorneoExcepcion.class,
                () -> servicio.actualizarEstadoTorneo(1L, "EN_CURSO", null));
        assertTrue(e3.getMessage().contains(ESPERADO), e3.getMessage());
    }

    @Test
    @DisplayName("Un rol insuficiente tambien queda rechazado")
    void rolInsuficienteEsRechazado() throws TorneoExcepcion {
        Usuario arbitro = new Usuario("arbitro", "ClaveDePrueba1", "arbitro@prueba.local", "Ar", "Bit", TiposDatos.Rol.ARBITRO);

        TorneoExcepcion e1 = assertThrows(TorneoExcepcion.class,
                () -> new TorneoService().crearTorneo("Torneo", "LIGA", 8, new Date(), arbitro));
        assertTrue(e1.getMessage().contains("no tiene permisos"), e1.getMessage());

        TorneoExcepcion e2 = assertThrows(TorneoExcepcion.class,
                () -> new DisciplinaService().emitirFalloTribunal(1L, 1, "Conducta antideportiva", "GRAVE", arbitro));
        assertTrue(e2.getMessage().contains("no tiene permisos"), e2.getMessage());

        TorneoExcepcion e3 = assertThrows(TorneoExcepcion.class,
                () -> new PartidoService().programarPartido(1L, 1L, 2L, 1L, 1L, new Date(), 90, arbitro));
        assertTrue(e3.getMessage().contains("no tiene permisos"), e3.getMessage());
    }
}
