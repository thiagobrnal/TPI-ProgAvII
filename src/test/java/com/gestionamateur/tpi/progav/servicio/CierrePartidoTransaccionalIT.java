package com.gestionamateur.tpi.progav.servicio;

import com.gestionamateur.tpi.progav.dao.*;
import com.gestionamateur.tpi.progav.modelo.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de Integración - Protocolo Atómico de Cierre de Partido, Variante A y Reclamos")
public class CierrePartidoTransaccionalIT {

    private static PartidoService partidoService;
    private static ReclamoService reclamoService;
    private static DisciplinaService disciplinaService;
    private static TorneoDAO torneoDAO;
    private static FaseDAO faseDAO;
    private static CanchaDAO canchaDAO;
    private static ArbitroDAO arbitroDAO;
    private static UsuarioDAO usuarioDAO;
    private static CategoriaDAO categoriaDAO;
    private static EquipoDAO equipoDAO;
    private static JugadorDAO jugadorDAO;
    private static PartidoDAO partidoDAO;
    private static PartidoEquipoDAO partidoEquipoDAO;
    private static CronologiaDAO cronologiaDAO;
    private static SancionDAO sancionDAO;
    private static ReclamoDAO reclamoDAO;

    @BeforeAll
    static void setUp() throws SQLException {
        assertTrue(ConexionBD.comprobarConexion(), "PostgreSQL debe estar activo en puerto 55432");

        partidoService = new PartidoService();
        reclamoService = new ReclamoService();
        disciplinaService = new DisciplinaService();
        torneoDAO = new TorneoDAO();
        faseDAO = new FaseDAO();
        canchaDAO = new CanchaDAO();
        arbitroDAO = new ArbitroDAO();
        usuarioDAO = new UsuarioDAO();
        categoriaDAO = new CategoriaDAO();
        equipoDAO = new EquipoDAO();
        jugadorDAO = new JugadorDAO();
        partidoDAO = new PartidoDAO();
        partidoEquipoDAO = new PartidoEquipoDAO();
        cronologiaDAO = new CronologiaDAO();
        sancionDAO = new SancionDAO();
        reclamoDAO = new ReclamoDAO();
    }

    private String sufijo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    @DisplayName("IT-CIERRE-01: Protocolo de cierre de acta de partido en 11 pasos atómicos")
    void testCierreActaPartido11PasosAtomics() throws Exception {
        String s = sufijo();

        // 1. Torneo y Fase
        Torneo torneo = new Torneo("Torneo Cierre " + s, "LIGA", 8, new Date());
        Long idTorneo = torneoDAO.insertar(torneo);
        Fase fase = new Fase("Fase Única", 1, false);
        Long idFase = faseDAO.insertar(fase, idTorneo);

        // 2. Cancha y Árbitro
        Cancha cancha = new Cancha("Estadio Central " + s, "Ruta 12", "Césped Natural");
        Long idCancha = canchaDAO.insertar(cancha);

        Arbitro arbitro = new Arbitro("arb_cierre_" + s, "pass123", "arb_" + s + "@test.com", "Juan", "López", "Provincial", "3764112233");
        Long idArbitro = arbitroDAO.insertar(arbitro, AuthService.hashearSha256("pass123"));

        // 3. Equipos y Jugadores
        Categoria catLibre = categoriaDAO.buscarPorId(2L);
        Usuario capLocal = new Usuario("cap_loc_" + s, "1234", "cap_loc_" + s + "@club.com", "Lucas", "Pratto", TiposDatos.Rol.CAPITAN);
        Long idCapLocal = usuarioDAO.insertar(capLocal, AuthService.hashearSha256("1234"), 4);
        Equipo eqLocal = new Equipo("Local FC " + s, catLibre, "Rojo", cancha, capLocal);
        Long idEqLocal = equipoDAO.insertar(eqLocal, idCapLocal);

        Usuario capVis = new Usuario("cap_vis_" + s, "1234", "cap_vis_" + s + "@club.com", "Ignacio", "Scocco", TiposDatos.Rol.CAPITAN);
        Long idCapVis = usuarioDAO.insertar(capVis, AuthService.hashearSha256("1234"), 4);
        Equipo eqVis = new Equipo("Visitante CA " + s, catLibre, "Blanco", cancha, capVis);
        Long idEqVis = equipoDAO.insertar(eqVis, idCapVis);

        // Jugadores
        int dniBase = 30000000 + (int)(Math.random() * 8000000);
        Jugador jLoc1 = new Jugador(idEqLocal, "Goleador Local " + s, dniBase + 1, new Date(95, 2, 10), "Delantero", TiposDatos.EstadoJugador.ACTIVO);
        Long idJLoc1 = jugadorDAO.insertar(jLoc1, idEqLocal);

        Jugador jVis1 = new Jugador(idEqVis, "Goleador Vis " + s, dniBase + 2, new Date(96, 5, 12), "Delantero", TiposDatos.EstadoJugador.ACTIVO);
        Long idJVis1 = jugadorDAO.insertar(jVis1, idEqVis);

        Jugador jVisExpulsado = new Jugador(idEqVis, "Expulsado Vis " + s, dniBase + 3, new Date(94, 7, 20), "Defensor", TiposDatos.EstadoJugador.ACTIVO);
        Long idJVisExp = jugadorDAO.insertar(jVisExpulsado, idEqVis);

        // 4. Programar partido
        Partido partido = partidoService.programarPartido(idFase, idEqLocal, idEqVis, idCancha, idArbitro, new Date(), 90);
        Long idPartido = partido.getIdPartido();
        assertNotNull(idPartido);

        // 5. Registrar incidencias antes del cierre:
        // Local marca 2 goles (minutos 15 y 40)
        partidoService.registrarIncidencia(idPartido, idJLoc1, 15, TiposDatos.TipoAccion.GOL, null, "Gol de penal");
        partidoService.registrarIncidencia(idPartido, idJLoc1, 40, TiposDatos.TipoAccion.GOL, null, "Gol de cabeza");
        // Visitante marca 1 gol (minuto 60)
        partidoService.registrarIncidencia(idPartido, idJVis1, 60, TiposDatos.TipoAccion.GOL, null, "Gol de tiro libre");

        // Expulsión por Roja Directa para jugador visitante (minuto 75)
        partidoService.registrarIncidencia(idPartido, idJVisExp, 75, TiposDatos.TipoAccion.TARJETA_ROJA, "DIRECTA", "Falta violenta");

        // 6. Ejecutar PROTOCOLO ATÓMICO DE CIERRE DE ACTA
        Date finReal = new Date();
        assertDoesNotThrow(() -> {
            partidoService.cerrarActaPartido(idPartido, finReal, null);
        });

        // 7. Verificaciones de los 11 pasos atómicos:
        try (Connection conn = ConexionBD.obtenerConexionTest()) {
            // Paso 3 & 4: Estado FINALIZADO y fecha_hora_finalizacion_real no nula
            Partido cerrado = partidoDAO.buscarPorId(conn, idPartido);
            assertNotNull(cerrado);
            assertEquals(TiposDatos.EstadoPartido.FINALIZADO, cerrado.getEstadoPartido());
            assertNotNull(cerrado.getFechaHoraFinalizacionReal());

            // Paso 6: Marcador oficial consolidado (Local 2 - 1 Visitante)
            PartidoEquipo peLoc = partidoEquipoDAO.buscarLocal(conn, idPartido);
            PartidoEquipo peVis = partidoEquipoDAO.buscarVisitante(conn, idPartido);
            assertEquals(2, peLoc.getGoles(), "Local debe tener 2 goles");
            assertEquals(1, peVis.getGoles(), "Visitante debe tener 1 gol");

            // Paso 7: Sanción automática por ROJA_DIRECTA generada e idempotente
            Long idSancion = sancionDAO.buscarIdPorEventoOrigen(conn, idJVisExp, idTorneo, idPartido, "ROJA_DIRECTA");
            assertNotNull(idSancion, "Debe existir sanción automática por ROJA_DIRECTA");
            Sancion sRoja = sancionDAO.buscarPorId(conn, idSancion);
            assertEquals("PENDIENTE", sRoja.getEstado());
            assertEquals("GRAVE", sRoja.getGravedad());

            // Paso 9: Estado del jugador expulsado actualizado a SUSPENDIDO
            Jugador jExpPost = jugadorDAO.buscarPorId(conn, idJVisExp);
            assertEquals(TiposDatos.EstadoJugador.SUSPENDIDO, jExpPost.getEstado());

            // Verificar rechazo si se intenta volver a cerrar (idempotencia y protección de estado)
            assertThrows(TorneoExcepcion.class, () -> {
                partidoService.cerrarActaPartido(idPartido, new Date(), null);
            });
        }
    }

    @Test
    @DisplayName("IT-VARIANTE-A: Reglas de acumulación de amarillas y extinción por doble amarilla")
    void testVarianteAAcumulacionAmarillas() throws Exception {
        String s = sufijo();

        // Crear Torneo y Fase 1 (limpia_amarillas = false)
        Torneo torneo = new Torneo("Torneo VarA " + s, "LIGA", 8, new Date());
        Long idTorneo = torneoDAO.insertar(torneo);
        Fase fase1 = new Fase("Fase Regular", 1, false);
        Long idFase1 = faseDAO.insertar(fase1, idTorneo);

        Cancha cancha = new Cancha("Predio VarA " + s, "Calle 10", "Sintético");
        Long idCancha = canchaDAO.insertar(cancha);

        Categoria catLibre = categoriaDAO.buscarPorId(2L);
        Usuario cap = new Usuario("cap_vara_" + s, "1234", "cap_vara_" + s + "@test.com", "Diego", "Milito", TiposDatos.Rol.CAPITAN);
        Long idCap = usuarioDAO.insertar(cap, AuthService.hashearSha256("1234"), 4);

        Equipo eq1 = new Equipo("Eq1 " + s, catLibre, "Verde", cancha, cap);
        Long idEq1 = equipoDAO.insertar(eq1, idCap);
        Equipo eq2 = new Equipo("Eq2 " + s, catLibre, "Blanco", cancha, cap);
        Long idEq2 = equipoDAO.insertar(eq2, idCap);

        int dni = 35000000 + (int)(Math.random() * 8000000);
        Jugador jDobleAmarilla = new Jugador(idEq1, "Jugador Doble Amarilla " + s, dni + 1, new Date(95, 1, 1), "Volante", TiposDatos.EstadoJugador.ACTIVO);
        Long idJDoble = jugadorDAO.insertar(jDobleAmarilla, idEq1);

        Jugador jAcumulador = new Jugador(idEq1, "Jugador Acumulador " + s, dni + 2, new Date(95, 2, 2), "Defensor", TiposDatos.EstadoJugador.ACTIVO);
        Long idJAcum = jugadorDAO.insertar(jAcumulador, idEq1);

        // CASO 1: Jugador recibe doble amarilla en el mismo partido -> Expulsado por DOBLE_AMARILLA
        // Según Variante A: Esas 2 amarillas NO deben sumarse a su cómputo de acumulación general
        Partido p1 = partidoService.programarPartido(idFase1, idEq1, idEq2, idCancha, null, new Date(), 90);
        Long idP1 = p1.getIdPartido();

        // Registrar 2 amarillas y la consecuente roja por doble amarilla
        partidoService.registrarIncidencia(idP1, idJDoble, 20, TiposDatos.TipoAccion.TARJETA_AMARILLA, null, "Primera amonestación");
        partidoService.registrarIncidencia(idP1, idJDoble, 55, TiposDatos.TipoAccion.TARJETA_AMARILLA, null, "Segunda amonestación");
        partidoService.registrarIncidencia(idP1, idJDoble, 55, TiposDatos.TipoAccion.TARJETA_ROJA, "DOBLE_AMARILLA", "Expulsión por 2 amarillas");

        // También registrar amarilla para el acumulador
        partidoService.registrarIncidencia(idP1, idJAcum, 30, TiposDatos.TipoAccion.TARJETA_AMARILLA, null, "Amonestación");

        partidoService.cerrarActaPartido(idP1, new Date(), null);

        try (Connection conn = ConexionBD.obtenerConexionTest()) {
            // Sanción por doble amarilla debe estar generada (1 fecha, ACTIVA)
            Long idSancionDoble = sancionDAO.buscarIdPorEventoOrigen(conn, idJDoble, idTorneo, idP1, "DOBLE_AMARILLA");
            assertNotNull(idSancionDoble, "Debe existir sanción por DOBLE_AMARILLA");
            Sancion sDoble = sancionDAO.buscarPorId(conn, idSancionDoble);
            assertEquals("ACTIVA", sDoble.getEstado());
            assertEquals(1, sDoble.getPartidosSuspension());

            // VERIFICACIÓN CLAVE VARIANTE A:
            // El jugador con doble amarilla tiene 0 amarillas acumulables para el ciclo de 5 amarillas
            int amarillasJDoble = cronologiaDAO.contarAmarillasVarianteA(conn, idJDoble, idTorneo, 1);
            assertEquals(0, amarillasJDoble, "Variante A: Las 2 amarillas de un partido con expulsión por doble amarilla deben extinguirse.");

            // El jugador acumulador debe tener 1 amarilla acumulada
            int amarillasJAcum = cronologiaDAO.contarAmarillasVarianteA(conn, idJAcum, idTorneo, 1);
            assertEquals(1, amarillasJAcum, "El jugador sin doble amarilla debe tener 1 amarilla acumulada");
        }

        // CASO 2: Jugador acumula 5 amarillas en 5 partidos distintos y recibe sanción automática
        for (int i = 2; i <= 5; i++) {
            Partido pSig = partidoService.programarPartido(idFase1, idEq1, idEq2, idCancha, null, new Date(), 90);
            partidoService.registrarIncidencia(pSig.getIdPartido(), idJAcum, 40, TiposDatos.TipoAccion.TARJETA_AMARILLA, null, "Amonestación fecha " + i);
            partidoService.cerrarActaPartido(pSig.getIdPartido(), new Date(), null);
        }

        try (Connection conn = ConexionBD.obtenerConexionTest()) {
            // El acumulador tiene 5 amarillas
            int amarillasTotal = cronologiaDAO.contarAmarillasVarianteA(conn, idJAcum, idTorneo, 1);
            assertEquals(5, amarillasTotal);

            // Debe haberse generado la sanción por ACUMULACION_AMARILLAS
            List<Sancion> sanciones = sancionDAO.buscarPorJugador(conn, idJAcum);
            boolean tieneSancionAcumulacion = false;
            for (Sancion san : sanciones) {
                if ("ACUMULACION_AMARILLAS".equals(san.getTipoOrigen())) {
                    tieneSancionAcumulacion = true;
                    assertEquals("ACTIVA", san.getEstado());
                    assertEquals(1, san.getPartidosSuspension());
                }
            }
            assertTrue(tieneSancionAcumulacion, "Debe haberse generado sanción por acumulación de 5 amarillas.");
        }
    }

    @Test
    @DisplayName("IT-RECLAMO-01: Expedientes de reclamo (72h corridas) y apelación (5 días corridos)")
    void testReclamoPlazo72HorasYApelacion5Dias() throws Exception {
        String s = sufijo();

        Torneo torneo = new Torneo("Torneo Reclamos " + s, "LIGA", 8, new Date());
        Long idTorneo = torneoDAO.insertar(torneo);
        Fase fase = new Fase("Fase Clasificatoria", 1, false);
        Long idFase = faseDAO.insertar(fase, idTorneo);

        Cancha cancha = new Cancha("Predio Reclamos " + s, "Calle 9", "Natural");
        Long idCancha = canchaDAO.insertar(cancha);

        Categoria catLibre = categoriaDAO.buscarPorId(2L);
        Usuario cap = new Usuario("cap_rec_" + s, "1234", "cap_rec_" + s + "@test.com", "Ariel", "Ortega", TiposDatos.Rol.CAPITAN);
        Long idCap = usuarioDAO.insertar(cap, AuthService.hashearSha256("1234"), 4);

        Equipo eq1 = new Equipo("River " + s, catLibre, "Blanco y Rojo", cancha, cap);
        Long idEq1 = equipoDAO.insertar(eq1, idCap);
        Equipo eq2 = new Equipo("Boca " + s, catLibre, "Azul y Oro", cancha, cap);
        Long idEq2 = equipoDAO.insertar(eq2, idCap);

        Partido p = partidoService.programarPartido(idFase, idEq1, idEq2, idCancha, null, new Date(), 90);
        Long idPartido = p.getIdPartido();

        // Finalizar el partido ahora
        Date finReal = new Date();
        partidoService.cerrarActaPartido(idPartido, finReal, null);

        // 1. Presentar reclamo sobre el partido dentro de las 72 horas corridas (CU-13)
        Reclamo reclamoPartido = reclamoService.presentarReclamoPartido(
            idPartido, idCap, "Reclamo formal por inclusión indebida de jugador rival."
        );
        assertNotNull(reclamoPartido.getId());
        assertEquals("PENDIENTE", reclamoPartido.getEstado());

        // 2. Crear una sanción y apelar dentro de los 5 días corridos (CU-14)
        int dni = 39000000 + (int)(Math.random() * 8000000);
        Jugador jSan = new Jugador(idEq1, "Jugador Sancionado " + s, dni, new Date(95, 3, 3), "Delantero", TiposDatos.EstadoJugador.SUSPENDIDO);
        Long idJSan = jugadorDAO.insertar(jSan, idEq1);

        Sancion sancion = new Sancion(
            new Date(), null, "Sanción provisoria por informe arbitral", "MEDIA",
            jSan, p, torneo, "TORNEO", "ROJA_DIRECTA", 3, 0, "ACTIVA"
        );
        sancion.setFechaPublicacionFormal(new Date());
        Long idSancion = sancionDAO.insertar(sancion);

        Reclamo apelacion = reclamoService.apelarSancion(
            idSancion, idCap, "Apelación fundada: las imágenes de video demuestran que no hubo agresión física."
        );
        assertNotNull(apelacion.getId());
        assertEquals("PENDIENTE", apelacion.getEstado());

        // 3. Resolución favorable del Tribunal ("A FAVOR"):
        // Debe resolver el expediente y anular la sanción disciplinaria, rehabilitando al jugador
        reclamoService.resolverExpediente(
            apelacion.getId(),
            "A FAVOR",
            "El tribunal analizó el video aportado y concluye que existió error de identidad arbitral."
        );

        try (Connection conn = ConexionBD.obtenerConexionTest()) {
            Reclamo resuelto = reclamoDAO.buscarPorId(conn, apelacion.getId());
            assertEquals("RESUELTO", resuelto.getEstado());
            assertEquals("A FAVOR", resuelto.getVeredicto());

            // La sanción debe estar ANULADA
            Sancion sancionPost = sancionDAO.buscarPorId(conn, idSancion);
            assertEquals("ANULADA", sancionPost.getEstado());

            // El jugador debe haber vuelto a estar ACTIVO y habilitado
            Jugador jRehabilitado = jugadorDAO.buscarPorId(conn, idJSan);
            assertEquals(TiposDatos.EstadoJugador.ACTIVO, jRehabilitado.getEstado());
        }
    }

    @Test
    @DisplayName("CP-04: Exigencia estricta de fecha_hora_fin_real y control de autorización de operador")
    void testObligatoriedadFechaFinYAutorizacionOperador() throws Exception {
        String s = sufijo();
        Torneo torneo = new Torneo("Torneo Oblig " + s, "LIGA", 8, new Date());
        Long idTorneo = torneoDAO.insertar(torneo);
        Fase fase = new Fase("Fase Oblig", 1, false);
        Long idFase = faseDAO.insertar(fase, idTorneo);
        Cancha cancha = new Cancha("Cancha Oblig " + s, "Calle 1", "Sintético");
        Long idCancha = canchaDAO.insertar(cancha);
        Categoria catLibre = categoriaDAO.buscarPorId(2L);

        Usuario cap = new Usuario("cap_ob_" + s, "1234", "cap_ob_" + s + "@test.com", "Capitan", "Prueba", TiposDatos.Rol.CAPITAN);
        Long idCap = usuarioDAO.insertar(cap, AuthService.hashearSha256("1234"), 4);

        Equipo eq1 = new Equipo("Eq1 Oblig " + s, catLibre, "Blanco", cancha, cap);
        Long idEq1 = equipoDAO.insertar(eq1, idCap);
        Equipo eq2 = new Equipo("Eq2 Oblig " + s, catLibre, "Negro", cancha, cap);
        Long idEq2 = equipoDAO.insertar(eq2, idCap);

        Date fechaInicio = new Date(System.currentTimeMillis() - 7200000L); // 2 horas en el pasado
        Partido p = partidoService.programarPartido(idFase, idEq1, idEq2, idCancha, null, fechaInicio, 90);
        Long idPartido = p.getIdPartido();

        // 1. Intentar cerrar acta con fechaHoraFinReal == null debe ser rechazado explícitamente (sin fallback silencioso)
        TorneoExcepcion exFechaNula = assertThrows(TorneoExcepcion.class, () -> {
            partidoService.cerrarActaPartido(idPartido, null, null);
        });
        assertTrue(exFechaNula.getMessage().contains("fecha y hora real de finalización es obligatoria"));

        // 2. Control de autorización de operador: un CAPITAN no puede cerrar actas de partido
        TorneoExcepcion exCapitan = assertThrows(TorneoExcepcion.class, () -> {
            partidoService.cerrarActaPartido(idPartido, new Date(), null, cap);
        });
        assertTrue(exCapitan.getMessage().contains("no tiene permisos para cerrar actas"));

        // 3. Operador ARBITRO u ORGANIZADOR puede cerrar el acta exitosamente
        Usuario arbitro = new Usuario("arb_ob_" + s, "1234", "arb_ob_" + s + "@test.com", "Arbitro", "Oficial", TiposDatos.Rol.ARBITRO);
        partidoService.cerrarActaPartido(idPartido, new Date(fechaInicio.getTime() + 5400000L), null, arbitro);

        try (Connection conn = ConexionBD.obtenerConexionTest()) {
            Partido cerrado = partidoDAO.buscarPorId(conn, idPartido);
            assertEquals(TiposDatos.EstadoPartido.FINALIZADO, cerrado.getEstadoPartido());
            assertNotNull(cerrado.getFechaHoraFinalizacionReal());
        }
    }

    @Test
    @DisplayName("Cierre Partido: Encuentro programado a las 20:00, iniciado a las 19:45 y finalizado a las 20:55 se acepta exitosamente")
    void testCierrePartidoHorarioProgramadoVsFinalizacionReal() throws Exception {
        String s = UUID.randomUUID().toString().substring(0, 8);
        Torneo t = new Torneo("Torneo Horarios " + s, "Liga", 8, new Date());
        Long idTorneo = torneoDAO.insertar(t);
        Fase fase = new Fase("Fase Horarios", 1, false);
        Long idFase = faseDAO.insertar(fase, idTorneo);
        Cancha cancha = new Cancha("Predio Horarios " + s, "Av. Central", "Césped");
        Long idCancha = canchaDAO.insertar(cancha);
        Categoria catLibre = categoriaDAO.buscarPorId(2L);

        Usuario cap = new Usuario("cap_hr_" + s, "1234", "cap_hr_" + s + "@test.com", "Capitan", "Horario", TiposDatos.Rol.CAPITAN);
        Long idCap = usuarioDAO.insertar(cap, AuthService.hashearSha256("1234"), 4);

        Equipo eq1 = new Equipo("Eq1 Horarios " + s, catLibre, "Rojo", cancha, cap);
        Long idEq1 = equipoDAO.insertar(eq1, idCap);
        Equipo eq2 = new Equipo("Eq2 Horarios " + s, catLibre, "Azul", cancha, cap);
        Long idEq2 = equipoDAO.insertar(eq2, idCap);

        // Crear árbitro oficial y asignarlo al partido
        Arbitro arbUsr = new Arbitro("arb_hr_" + s, "1234", "arb_hr_" + s + "@test.com", "Arbitro", "Oficial", "Nacional", "12345678");
        Long idArb = arbitroDAO.insertar(arbUsr, AuthService.hashearSha256("1234"));
        arbUsr.setId(idArb);

        // Escenario del requerimiento: Encuentro programado a las 20:00 (calendario nominal en el pasado)
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(2026, java.util.Calendar.NOVEMBER, 1, 20, 0, 0);
        Date horarioProgramado2000 = cal.getTime();

        Partido p = partidoService.programarPartido(idFase, idEq1, idEq2, idCancha, idArb, horarioProgramado2000, 90);
        Long idPartido = p.getIdPartido();

        // El partido inició efectivamente a las 19:45 y finalizó a las 20:55
        cal.set(2026, java.util.Calendar.NOVEMBER, 1, 20, 55, 0);
        Date finReal2055 = cal.getTime();

        // Debe aceptarse el cierre sin error, preservando la atomicidad y la regla deportiva
        partidoService.cerrarActaPartido(idPartido, finReal2055, null, arbUsr);

        try (Connection conn = ConexionBD.obtenerConexionTest()) {
            Partido cerrado = partidoDAO.buscarPorId(conn, idPartido);
            assertNotNull(cerrado);
            assertEquals(TiposDatos.EstadoPartido.FINALIZADO, cerrado.getEstadoPartido());
            assertEquals(finReal2055.getTime(), cerrado.getFechaHoraFinalizacionReal().getTime());
        }

        // Caso límite adicional: Partido programado a las 20:00 pero finalizado a las 19:55 (partido suspendido/reducido anticipadamente)
        Partido pAnticipado = partidoService.programarPartido(idFase, idEq1, idEq2, idCancha, idArb, horarioProgramado2000, 90);
        cal.set(2026, java.util.Calendar.NOVEMBER, 1, 19, 55, 0);
        Date finAnticipado1955 = cal.getTime();

        // El cierre con fecha real de finalización 19:55 debe ser aceptado formalmente
        partidoService.cerrarActaPartido(pAnticipado.getIdPartido(), finAnticipado1955, null, arbUsr);

        try (Connection conn = ConexionBD.obtenerConexionTest()) {
            Partido cerradoAnt = partidoDAO.buscarPorId(conn, pAnticipado.getIdPartido());
            assertNotNull(cerradoAnt);
            assertEquals(TiposDatos.EstadoPartido.FINALIZADO, cerradoAnt.getEstadoPartido());
            assertEquals(finAnticipado1955.getTime(), cerradoAnt.getFechaHoraFinalizacionReal().getTime());
        }
    }

    @Test
    @DisplayName("Cierre Partido: Rechazar fecha y hora real de finalización futura respecto al momento actual (ADR-17 / H-04)")
    void testCierrePartidoRechazaFechaFinalizacionFutura() throws Exception {
        String s = UUID.randomUUID().toString().substring(0, 8);
        Torneo t = new Torneo("Torneo Futuro " + s, "Liga", 8, new Date());
        Long idTorneo = torneoDAO.insertar(t);
        Fase fase = new Fase("Fase Futuro", 1, false);
        Long idFase = faseDAO.insertar(fase, idTorneo);
        Cancha cancha = new Cancha("Predio Futuro " + s, "Av. Futuro", "Césped");
        Long idCancha = canchaDAO.insertar(cancha);
        Categoria catLibre = categoriaDAO.buscarPorId(2L);

        Usuario cap = new Usuario("cap_fut_" + s, "1234", "cap_fut_" + s + "@test.com", "Capitan", "Futuro", TiposDatos.Rol.CAPITAN);
        Long idCap = usuarioDAO.insertar(cap, AuthService.hashearSha256("1234"), 4);

        Equipo eq1 = new Equipo("Eq1 Futuro " + s, catLibre, "Rojo", cancha, cap);
        Long idEq1 = equipoDAO.insertar(eq1, idCap);
        Equipo eq2 = new Equipo("Eq2 Futuro " + s, catLibre, "Azul", cancha, cap);
        Long idEq2 = equipoDAO.insertar(eq2, idCap);

        Date fechaInicio = new Date(System.currentTimeMillis() - 7200000L);
        Partido p = partidoService.programarPartido(idFase, idEq1, idEq2, idCancha, null, fechaInicio, 90);

        // Intentar cerrar con fecha futura (1 hora en el futuro) debe arrojar TorneoExcepcion
        Date finFuturo = new Date(System.currentTimeMillis() + 3600000L);
        TorneoExcepcion exFuturo = assertThrows(TorneoExcepcion.class, () -> {
            partidoService.cerrarActaPartido(p.getIdPartido(), finFuturo, null);
        });
        assertTrue(exFuturo.getMessage().contains("no puede ser posterior al momento actual"));
    }

    @Test
    @DisplayName("Seguridad: Un árbitro no puede cerrar un partido si no es el árbitro oficial designado")
    void testCierrePartidoAutorizacionArbitroDesignado() throws Exception {
        String s = UUID.randomUUID().toString().substring(0, 8);
        Torneo t = new Torneo("Torneo ArbSec " + s, "Liga", 8, new Date());
        Long idTorneo = torneoDAO.insertar(t);
        Fase fase = new Fase("Fase ArbSec", 1, false);
        Long idFase = faseDAO.insertar(fase, idTorneo);
        Cancha cancha = new Cancha("Predio ArbSec " + s, "Av. Central", "Césped");
        Long idCancha = canchaDAO.insertar(cancha);
        Categoria catLibre = categoriaDAO.buscarPorId(2L);

        Usuario cap = new Usuario("cap_as_" + s, "1234", "cap_as_" + s + "@test.com", "Capitan", "Prueba", TiposDatos.Rol.CAPITAN);
        Long idCap = usuarioDAO.insertar(cap, AuthService.hashearSha256("1234"), 4);

        Equipo eq1 = new Equipo("Eq1 ArbSec " + s, catLibre, "Blanco", cancha, cap);
        Long idEq1 = equipoDAO.insertar(eq1, idCap);
        Equipo eq2 = new Equipo("Eq2 ArbSec " + s, catLibre, "Negro", cancha, cap);
        Long idEq2 = equipoDAO.insertar(eq2, idCap);

        Arbitro arbDesignado = new Arbitro("arb_des_" + s, "1234", "des_" + s + "@test.com", "Arbitro", "Designado", "Nacional", "12345678");
        Long idArbDes = arbitroDAO.insertar(arbDesignado, AuthService.hashearSha256("1234"));
        arbDesignado.setId(idArbDes);

        Arbitro arbAjeno = new Arbitro("arb_aje_" + s, "1234", "aje_" + s + "@test.com", "Arbitro", "Ajeno", "Provincial", "12345678");
        Long idArbAje = arbitroDAO.insertar(arbAjeno, AuthService.hashearSha256("1234"));
        arbAjeno.setId(idArbAje);

        Partido p = partidoService.programarPartido(idFase, idEq1, idEq2, idCancha, idArbDes, new Date(), 90);

        // Árbitro ajeno intenta cerrar el acta: debe ser rechazado
        TorneoExcepcion ex = assertThrows(TorneoExcepcion.class, () -> {
            partidoService.cerrarActaPartido(p.getIdPartido(), new Date(), null, arbAjeno);
        });
        assertTrue(ex.getMessage().contains("no es el árbitro oficial designado"));

        // Árbitro designado cierra el acta: debe ser exitoso
        partidoService.cerrarActaPartido(p.getIdPartido(), new Date(), null, arbDesignado);
        try (Connection conn = ConexionBD.obtenerConexionTest()) {
            Partido cerrado = partidoDAO.buscarPorId(conn, p.getIdPartido());
            assertEquals(TiposDatos.EstadoPartido.FINALIZADO, cerrado.getEstadoPartido());
        }
    }

    @Test
    @DisplayName("Cierre Partido: Preservación de estados LESIONADO e INHABILITADO durante protocolo atómico (ADR-13 / H-05)")
    void testCierrePartidoPreservaEstadoJugadorNoActivo() throws Exception {
        String s = UUID.randomUUID().toString().substring(0, 8);
        Torneo t = new Torneo("Torneo ADR13 Cierre " + s, "Liga", 8, new Date());
        Long idTorneo = torneoDAO.insertar(t);
        Fase fase = new Fase("Fase ADR13", 1, false);
        Long idFase = faseDAO.insertar(fase, idTorneo);
        Cancha cancha = new Cancha("Predio ADR13 " + s, "Av. Central", "Césped");
        Long idCancha = canchaDAO.insertar(cancha);
        Categoria catLibre = categoriaDAO.buscarPorId(2L);

        Usuario cap = new Usuario("cap_adr13c_" + s, "1234", "cap_c_" + s + "@test.com", "Capitan", "ADR13", TiposDatos.Rol.CAPITAN);
        Long idCap = usuarioDAO.insertar(cap, AuthService.hashearSha256("1234"), 4);

        Equipo eq1 = new Equipo("Eq1 ADR13 " + s, catLibre, "Blanco", cancha, cap);
        Long idEq1 = equipoDAO.insertar(eq1, idCap);
        Equipo eq2 = new Equipo("Eq2 ADR13 " + s, catLibre, "Negro", cancha, cap);
        Long idEq2 = equipoDAO.insertar(eq2, idCap);

        int dniBase = 35000000 + (int)(Math.random() * 8000000);
        // Jugador 1: LESIONADO
        Jugador jLesionado = new Jugador(idEq1, "Jugador Lesionado " + s, dniBase + 1, new Date(95, 1, 1), "Delantero", TiposDatos.EstadoJugador.ACTIVO);
        Long idJLes = jugadorDAO.insertar(jLesionado, idEq1);
        try (Connection conn = ConexionBD.obtenerConexionTest()) {
            jugadorDAO.actualizarEstado(conn, idJLes, "LESIONADO");
        }

        // Jugador 2: INHABILITADO
        Jugador jInhabilitado = new Jugador(idEq1, "Jugador Inhabilitado " + s, dniBase + 2, new Date(95, 1, 2), "Mediocampista", TiposDatos.EstadoJugador.ACTIVO);
        Long idJInh = jugadorDAO.insertar(jInhabilitado, idEq1);
        try (Connection conn = ConexionBD.obtenerConexionTest()) {
            jugadorDAO.actualizarEstado(conn, idJInh, "INHABILITADO");
        }

        // Programar partido en el pasado
        Date fechaInicio = new Date(System.currentTimeMillis() - 7200000L);
        Partido p = partidoService.programarPartido(idFase, idEq1, idEq2, idCancha, null, fechaInicio, 90);
        Long idPartido = p.getIdPartido();

        // Registrar incidencia de expulsión por roja directa sobre jugador lesionado
        partidoService.registrarIncidencia(idPartido, idJLes, 30, TiposDatos.TipoAccion.TARJETA_ROJA, "DIRECTA", "Incidente en banco");

        // Registrar incidencia de expulsión por doble amarilla sobre jugador inhabilitado
        partidoService.registrarIncidencia(idPartido, idJInh, 45, TiposDatos.TipoAccion.TARJETA_ROJA, "DOBLE_AMARILLA", "Doble amonestación");

        // Cerrar acta
        Date finReal = new Date(fechaInicio.getTime() + 5400000L);
        partidoService.cerrarActaPartido(idPartido, finReal, null);

        // Verificaciones críticas de ADR-13 / H-05:
        try (Connection conn = ConexionBD.obtenerConexionTest()) {
            // Sanciones generadas en la tabla sanciones
            Long idSanLes = sancionDAO.buscarIdPorEventoOrigen(conn, idJLes, idTorneo, idPartido, "ROJA_DIRECTA");
            assertNotNull(idSanLes, "Debe registrarse la sanción en la tabla sanciones");
            Long idSanInh = sancionDAO.buscarIdPorEventoOrigen(conn, idJInh, idTorneo, idPartido, "DOBLE_AMARILLA");
            assertNotNull(idSanInh, "Debe registrarse la sanción por doble amarilla");

            // Estados intrínsecos de salud / administración NO deben ser sobreescritos a SUSPENDIDO
            Jugador postCierreLes = jugadorDAO.buscarPorId(conn, idJLes);
            assertEquals(TiposDatos.EstadoJugador.LESIONADO, postCierreLes.getEstado(),
                    "ADR-13: El jugador lesionado debe conservar su estado LESIONADO y no sobrescribirse a SUSPENDIDO");

            Jugador postCierreInh = jugadorDAO.buscarPorId(conn, idJInh);
            assertEquals(TiposDatos.EstadoJugador.INHABILITADO, postCierreInh.getEstado(),
                    "ADR-13: El jugador inhabilitado debe conservar su estado INHABILITADO y no sobrescribirse a SUSPENDIDO");
        }

        // Segundo partido: el equipo vuelve a jugar y se cumple la fecha de suspensión de doble amarilla
        Partido p2 = partidoService.programarPartido(idFase, idEq1, idEq2, idCancha, null, new Date(System.currentTimeMillis() - 3600000L), 90);
        partidoService.cerrarActaPartido(p2.getIdPartido(), new Date(System.currentTimeMillis() - 1800000L), null);

        // Al purgarse la sanción, el jugador inhabilitado NO debe volver a ACTIVO
        try (Connection conn = ConexionBD.obtenerConexionTest()) {
            Jugador postCumplimientoInh = jugadorDAO.buscarPorId(conn, idJInh);
            assertEquals(TiposDatos.EstadoJugador.INHABILITADO, postCumplimientoInh.getEstado(),
                    "ADR-13: Al purgar una sanción, un jugador inhabilitado jamás debe pasar erróneamente a ACTIVO");
        }
    }
}
