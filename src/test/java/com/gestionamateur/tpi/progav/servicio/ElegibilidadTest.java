package com.gestionamateur.tpi.progav.servicio;

import com.gestionamateur.tpi.progav.modelo.Categoria;
import com.gestionamateur.tpi.progav.modelo.Jugador;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Reglas de Elegibilidad por Edad (CU-02.1 / Categoria)")
public class ElegibilidadTest {

    private JugadorService jugadorService;
    private Date fechaInicioTorneo;

    @BeforeEach
    void setUp() {
        jugadorService = new JugadorService();
        // Fijamos fecha de inicio del torneo: 1 de Junio de 2026
        LocalDate ld = LocalDate.of(2026, 6, 1);
        fechaInicioTorneo = Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date crearFecha(int anio, int mes, int dia) {
        LocalDate ld = LocalDate.of(anio, mes, dia);
        return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    @Test
    @DisplayName("Sub-20 (16 a 20 años): Acepta jugador de 18 años, rechaza jugador de 15 o de 22 años")
    void testElegibilidadSub20() throws TorneoExcepcion {
        Categoria sub20 = new Categoria("Sub-20", "Masculino", 16, 20);

        // Jugador de 18 años
        Jugador j18 = new Jugador("Joven Promesa", 45000001, crearFecha(2008, 5, 10), "Delantero", TiposDatos.EstadoJugador.ACTIVO);
        assertTrue(jugadorService.verificarElegibilidadCategoria(j18, sub20, fechaInicioTorneo));

        // Jugador de 15 años
        Jugador j15 = new Jugador("Menor", 48000001, crearFecha(2011, 2, 1), "Defensor", TiposDatos.EstadoJugador.ACTIVO);
        assertFalse(jugadorService.verificarElegibilidadCategoria(j15, sub20, fechaInicioTorneo));

        // Jugador de 22 años
        Jugador j22 = new Jugador("Veterano Sub20", 43000001, crearFecha(2004, 3, 15), "Arquero", TiposDatos.EstadoJugador.ACTIVO);
        assertFalse(jugadorService.verificarElegibilidadCategoria(j22, sub20, fechaInicioTorneo));
    }

    @Test
    @DisplayName("Límites exactos en Sub-20: Edad mínima exacta (16) y edad máxima exacta (20)")
    void testLimitesExactosSub20() throws TorneoExcepcion {
        Categoria sub20 = new Categoria("Sub-20", "Masculino", 16, 20);

        // Cumple 16 años exactamente el 1 de Junio de 2026 (Nacido el 1/6/2010) -> Elegible
        Jugador j16Exacto = new Jugador("Exacto16", 49000001, crearFecha(2010, 6, 1), "Medio", TiposDatos.EstadoJugador.ACTIVO);
        assertTrue(jugadorService.verificarElegibilidadCategoria(j16Exacto, sub20, fechaInicioTorneo));

        // Cumple 16 años un día después (Nacido el 2/6/2010, tiene 15 al iniciar torneo) -> No elegible
        Jugador j15UnDiaDespues = new Jugador("Casi16", 49000002, crearFecha(2010, 6, 2), "Medio", TiposDatos.EstadoJugador.ACTIVO);
        assertFalse(jugadorService.verificarElegibilidadCategoria(j15UnDiaDespues, sub20, fechaInicioTorneo));

        // Cumple 20 años exactamente el 1 de Junio de 2026 (Nacido el 1/6/2006) -> Elegible
        Jugador j20Exacto = new Jugador("Exacto20", 43000002, crearFecha(2006, 6, 1), "Defensa", TiposDatos.EstadoJugador.ACTIVO);
        assertTrue(jugadorService.verificarElegibilidadCategoria(j20Exacto, sub20, fechaInicioTorneo));

        // Cumplió 21 años un día antes (Nacido el 31/5/2005) -> No elegible (tiene 21 al iniciar torneo)
        Jugador j21UnDiaAntes = new Jugador("Pasado20", 43000003, crearFecha(2005, 5, 31), "Defensa", TiposDatos.EstadoJugador.ACTIVO);
        assertFalse(jugadorService.verificarElegibilidadCategoria(j21UnDiaAntes, sub20, fechaInicioTorneo));
    }

    @Test
    @DisplayName("Cumpleaños posterior a la fecha de inicio del torneo: Se evalúa estrictamente al día de inicio")
    void testCumpleanosPosteriorAlInicio() throws TorneoExcepcion {
        Categoria sub20 = new Categoria("Sub-20", "Masculino", 16, 20);

        // Nació el 15 de Julio de 2005. Al 1 de Junio de 2026 tiene 20 años exactos.
        // Cumplirá 21 el 15/7/2026 (durante el torneo). Es ELEGIBLE porque al inicio tenía 20.
        Jugador jCumpleDuranteTorneo = new Jugador("CumpleEnJulio", 42500001, crearFecha(2005, 7, 15), "Delantero", TiposDatos.EstadoJugador.ACTIVO);
        assertTrue(jugadorService.verificarElegibilidadCategoria(jCumpleDuranteTorneo, sub20, fechaInicioTorneo));
    }

    @Test
    @DisplayName("Libre (16 años sin límite superior): Acepta cualquier edad >= 16 sin techo")
    void testElegibilidadLibre() throws TorneoExcepcion {
        Categoria libre = new Categoria("Libre", "Masculino", 16, null);

        // 16 años exactos
        Jugador j16 = new Jugador("Pibe", 46000001, crearFecha(2010, 6, 1), "Volante", TiposDatos.EstadoJugador.ACTIVO);
        assertTrue(jugadorService.verificarElegibilidadCategoria(j16, libre, fechaInicioTorneo));

        // 55 años
        Jugador j55 = new Jugador("Veterano55", 25000001, crearFecha(1971, 1, 10), "Central", TiposDatos.EstadoJugador.ACTIVO);
        assertTrue(jugadorService.verificarElegibilidadCategoria(j55, libre, fechaInicioTorneo));

        // 15 años y 364 días -> No elegible
        Jugador j15 = new Jugador("Niño", 50000001, crearFecha(2010, 6, 2), "Delantero", TiposDatos.EstadoJugador.ACTIVO);
        assertFalse(jugadorService.verificarElegibilidadCategoria(j15, libre, fechaInicioTorneo));
    }

    @Test
    @DisplayName("Senior (35 años sin límite superior): Acepta >= 35, rechaza < 35")
    void testElegibilidadSenior() throws TorneoExcepcion {
        Categoria senior = new Categoria("Senior", "Masculino", 35, null);

        // 35 años exactos (nacido 1/6/1991)
        Jugador j35 = new Jugador("Veterano35", 35000001, crearFecha(1991, 6, 1), "Arquero", TiposDatos.EstadoJugador.ACTIVO);
        assertTrue(jugadorService.verificarElegibilidadCategoria(j35, senior, fechaInicioTorneo));

        // 34 años (cumplirá 35 el 2/6/1991) -> No elegible
        Jugador j34 = new Jugador("Treintañero", 35000002, crearFecha(1991, 6, 2), "Volante", TiposDatos.EstadoJugador.ACTIVO);
        assertFalse(jugadorService.verificarElegibilidadCategoria(j34, senior, fechaInicioTorneo));
    }

    @Test
    @DisplayName("Fecha de inicio obligatoria: lanza TorneoExcepcion si se omite la fecha de inicio del torneo")
    void testFechaInicioObligatoria() throws TorneoExcepcion {
        Categoria sub20 = new Categoria("Sub-20", "Masculino", 16, 20);
        Jugador j = new Jugador("JugadorPrueba", 45000099, crearFecha(2008, 1, 1), "Delantero", TiposDatos.EstadoJugador.ACTIVO);

        assertThrows(TorneoExcepcion.class, () -> {
            jugadorService.verificarElegibilidadCategoria(j, sub20, null);
        });
    }
}
