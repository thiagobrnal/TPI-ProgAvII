package com.gestionamateur.tpi.progav.servicio;

import com.gestionamateur.tpi.progav.dao.*;
import com.gestionamateur.tpi.progav.modelo.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("INF-05: Suite de Integración de Endurecimiento Crítico y Reglas Disciplinarias")
public class EndurecimientoInf05IT {

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
        sancionDAO = new SancionDAO();
        reclamoDAO = new ReclamoDAO();
    }

    private String sufijo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private int randomDni() {
        return 40000000 + (int)(Math.random() * 8000000);
    }

    @Test
    @DisplayName("H-01 / CU-14: Validación de ventana perentoria de apelación calculada desde fecha_publicacion_formal")
    void testValidacionVentanaApelacionCU14() throws Exception {
        String s = sufijo();

        Torneo torneo = new Torneo("Torneo CU14 " + s, "LIGA", 8, new Date());
        Long idTorneo = torneoDAO.insertar(torneo);
        Fase fase = new Fase("Fase CU14", 1, false);
        Long idFase = faseDAO.insertar(fase, idTorneo);

        Cancha cancha = new Cancha("Predio CU14 " + s, "Calle 1", "Natural");
        Long idCancha = canchaDAO.insertar(cancha);

        Categoria catLibre = categoriaDAO.buscarPorId(2L);
        Usuario cap = new Usuario("cap_cu14_" + s, "1234", "cap_cu14_" + s + "@test.com", "Capitan", "CU14", TiposDatos.Rol.CAPITAN);
        Long idCap = usuarioDAO.insertar(cap, AuthService.hashearSha256("1234"), 4);

        Equipo eq1 = new Equipo("Eq1 CU14 " + s, catLibre, "Rojo", cancha, cap);
        Long idEq1 = equipoDAO.insertar(eq1, idCap);
        Equipo eq2 = new Equipo("Eq2 CU14 " + s, catLibre, "Azul", cancha, cap);
        Long idEq2 = equipoDAO.insertar(eq2, idCap);

        Partido p = partidoService.programarPartido(idFase, idEq1, idEq2, idCancha, null, new Date(), 90);
        partidoService.cerrarActaPartido(p.getIdPartido(), new Date(), null);

        // Tres jugadores distintos para evitar uq_sancion_evento_origen en el mismo partido
        Jugador jSanSinPub = new Jugador(idEq1, "Jugador SinPub " + s, randomDni(), new Date(95, 1, 1), "Delantero", TiposDatos.EstadoJugador.SUSPENDIDO);
        Long idJSanSinPub = jugadorDAO.insertar(jSanSinPub, idEq1);

        Jugador jSanEnPlazo = new Jugador(idEq1, "Jugador EnPlazo " + s, randomDni(), new Date(95, 1, 2), "Mediocampista", TiposDatos.EstadoJugador.SUSPENDIDO);
        Long idJSanEnPlazo = jugadorDAO.insertar(jSanEnPlazo, idEq1);

        Jugador jSanVencida = new Jugador(idEq1, "Jugador Vencido " + s, randomDni(), new Date(95, 1, 3), "Defensor", TiposDatos.EstadoJugador.SUSPENDIDO);
        Long idJSanVencida = jugadorDAO.insertar(jSanVencida, idEq1);

        // CASO A: Sanción SIN publicar formalmente (fecha_publicacion_formal = NULL) -> debe ser RECHAZADA
        Sancion sanSinPublicar = new Sancion(
            new Date(), null, "Sanción sin formalizar tribunal", "MEDIA",
            jSanSinPub, p, torneo, "TORNEO", "ROJA_DIRECTA", 2, 0, "PENDIENTE"
        );
        sanSinPublicar.setFechaPublicacionFormal(null);
        Long idSanSinPub = sancionDAO.insertar(sanSinPublicar);

        TorneoExcepcion exSinPub = assertThrows(TorneoExcepcion.class, () -> {
            reclamoService.apelarSancion(idSanSinPub, idCap, "Apelación prematura de sanción que aún no fue formalizada");
        });
        assertTrue(exSinPub.getMessage().contains("aún no ha sido formalmente publicada"),
                "Debe indicar que la sanción no ha sido formalmente publicada");

        // CASO B: Sanción formalizada dentro del plazo (hace 2 días) -> APELACIÓN ACEPTADA
        Calendar cal2DiasAtras = Calendar.getInstance();
        cal2DiasAtras.add(Calendar.DAY_OF_YEAR, -2);

        Sancion sanEnPlazo = new Sancion(
            cal2DiasAtras.getTime(), null, "Sanción formalizada en plazo", "MEDIA",
            jSanEnPlazo, p, torneo, "TORNEO", "ROJA_DIRECTA", 2, 0, "ACTIVA"
        );
        sanEnPlazo.setFechaPublicacionFormal(cal2DiasAtras.getTime());
        Long idSanEnPlazo = sancionDAO.insertar(sanEnPlazo);

        Reclamo apelacion = reclamoService.apelarSancion(idSanEnPlazo, idCap, "Apelación formal fundamentada presentada dentro de plazo legal");
        assertNotNull(apelacion.getId());
        assertEquals("PENDIENTE", apelacion.getEstado());

        // CASO C: Sanción formalizada fuera del plazo perentorio de 5 días (hace 7 días) -> RECHAZADA
        Calendar cal7DiasAtras = Calendar.getInstance();
        cal7DiasAtras.add(Calendar.DAY_OF_YEAR, -7);

        Sancion sanVencida = new Sancion(
            cal7DiasAtras.getTime(), null, "Sanción con plazo formal vencido", "MEDIA",
            jSanVencida, p, torneo, "TORNEO", "ROJA_DIRECTA", 2, 0, "ACTIVA"
        );
        sanVencida.setFechaPublicacionFormal(cal7DiasAtras.getTime());
        Long idSanVencida = sancionDAO.insertar(sanVencida);

        TorneoExcepcion exVencida = assertThrows(TorneoExcepcion.class, () -> {
            reclamoService.apelarSancion(idSanVencida, idCap, "Apelación tardía presentada fuera de los 5 días hábiles");
        });
        assertTrue(exVencida.getMessage().contains("Plazo de apelación vencido"),
                "Debe rechazar por plazo de apelación vencido (más de 5 días corridos)");
    }

    @Test
    @DisplayName("H-02: Desacoplamiento de estado intrínseco de jugador vs sanciones (LESIONADO / INHABILITADO no se borran)")
    void testDesacoplamientoEstadoJugadorSanciones() throws Exception {
        String s = sufijo();

        Torneo torneo = new Torneo("Torneo Desacople " + s, "LIGA", 8, new Date());
        Long idTorneo = torneoDAO.insertar(torneo);
        Fase fase = new Fase("Fase Desacople", 1, false);
        Long idFase = faseDAO.insertar(fase, idTorneo);

        Cancha cancha = new Cancha("Predio Desacople " + s, "Calle 2", "Sintético");
        Long idCancha = canchaDAO.insertar(cancha);

        Categoria catLibre = categoriaDAO.buscarPorId(2L);
        Usuario cap = new Usuario("cap_des_" + s, "1234", "cap_des_" + s + "@test.com", "Capitan", "Desacople", TiposDatos.Rol.CAPITAN);
        Long idCap = usuarioDAO.insertar(cap, AuthService.hashearSha256("1234"), 4);

        Equipo eq1 = new Equipo("Eq1 Desacople " + s, catLibre, "Verde", cancha, cap);
        Long idEq1 = equipoDAO.insertar(eq1, idCap);
        Equipo eq2 = new Equipo("Eq2 Desacople " + s, catLibre, "Blanco", cancha, cap);
        Long idEq2 = equipoDAO.insertar(eq2, idCap);

        Partido p = partidoService.programarPartido(idFase, idEq1, idEq2, idCancha, null, new Date(), 90);
        partidoService.cerrarActaPartido(p.getIdPartido(), new Date(), null);

        // 1. Jugador con estado LESIONADO
        Jugador jLesionado = new Jugador(idEq1, "Jugador Lesionado " + s, randomDni(), new Date(95, 2, 2), "Mediocampista", TiposDatos.EstadoJugador.LESIONADO);
        Long idJLes = jugadorDAO.insertar(jLesionado, idEq1);

        Sancion sanLes = new Sancion(
            new Date(), null, "Sanción a jugador lesionado", "LEVE",
            jLesionado, p, torneo, "TORNEO", "DOBLE_AMARILLA", 1, 0, "ACTIVA"
        );
        sanLes.setFechaPublicacionFormal(new Date());
        Long idSanLes = sancionDAO.insertar(sanLes);

        // Al anular la sanción disciplinaria, NO debe transformar al jugador LESIONADO en ACTIVO
        disciplinaService.anularSancion(idSanLes, "Error administrativo comprobado en acta");

        try (Connection conn = ConexionBD.obtenerConexion()) {
            Jugador postAnulacion = jugadorDAO.buscarPorId(conn, idJLes);
            assertEquals(TiposDatos.EstadoJugador.LESIONADO, postAnulacion.getEstado(),
                    "El jugador lesionado debe seguir en estado LESIONADO, no mutar a ACTIVO.");
        }

        // Tampoco debe estar habilitado para jugar mientras esté LESIONADO
        assertFalse(disciplinaService.esJugadorHabilitado(idJLes, idTorneo),
                "Un jugador LESIONADO nunca debe considerarse habilitado deportivamente aunque no tenga sanciones");

        // 2. Jugador con estado INHABILITADO
        Jugador jInhabilitado = new Jugador(idEq1, "Jugador Inhabilitado " + s, randomDni(), new Date(95, 3, 3), "Defensor", TiposDatos.EstadoJugador.INHABILITADO);
        Long idJInhab = jugadorDAO.insertar(jInhabilitado, idEq1);

        Sancion sanInhab = new Sancion(
            new Date(), null, "Sanción a inhabilitado", "LEVE",
            jInhabilitado, p, torneo, "TORNEO", "DOBLE_AMARILLA", 1, 0, "ACTIVA"
        );
        sanInhab.setFechaPublicacionFormal(new Date());
        Long idSanInhab = sancionDAO.insertar(sanInhab);

        disciplinaService.anularSancion(idSanInhab, "Anulación por tribunal");

        try (Connection conn = ConexionBD.obtenerConexion()) {
            Jugador postInhab = jugadorDAO.buscarPorId(conn, idJInhab);
            assertEquals(TiposDatos.EstadoJugador.INHABILITADO, postInhab.getEstado(),
                    "El jugador inhabilitado debe seguir INHABILITADO.");
        }
        assertFalse(disciplinaService.esJugadorHabilitado(idJInhab, idTorneo));

        // 3. Jugador con estado SUSPENDIDO -> Al anular su sanción sí debe retornar a ACTIVO
        Jugador jSuspendido = new Jugador(idEq1, "Jugador Suspendido " + s, randomDni(), new Date(95, 4, 4), "Delantero", TiposDatos.EstadoJugador.SUSPENDIDO);
        Long idJSusp = jugadorDAO.insertar(jSuspendido, idEq1);

        Sancion sanSusp = new Sancion(
            new Date(), null, "Sanción legítima", "LEVE",
            jSuspendido, p, torneo, "TORNEO", "DOBLE_AMARILLA", 1, 0, "ACTIVA"
        );
        sanSusp.setFechaPublicacionFormal(new Date());
        Long idSanSusp = sancionDAO.insertar(sanSusp);

        disciplinaService.anularSancion(idSanSusp, "Absolución disciplinaria completa");

        try (Connection conn = ConexionBD.obtenerConexion()) {
            Jugador postSusp = jugadorDAO.buscarPorId(conn, idJSusp);
            assertEquals(TiposDatos.EstadoJugador.ACTIVO, postSusp.getEstado(),
                    "El jugador que estaba SUSPENDIDO debe retornar a ACTIVO al anularse su sanción.");
        }
        assertTrue(disciplinaService.esJugadorHabilitado(idJSusp, idTorneo));
    }

    @Test
    @DisplayName("H-03: Contrato de idempotencia estricto ON CONFLICT DO NOTHING (preserva datos originales)")
    void testContratoIdempotenciaDoNothing() throws Exception {
        String s = sufijo();

        Torneo torneo = new Torneo("Torneo Idemp " + s, "LIGA", 8, new Date());
        Long idTorneo = torneoDAO.insertar(torneo);
        Fase fase = new Fase("Fase Idemp", 1, false);
        Long idFase = faseDAO.insertar(fase, idTorneo);

        Cancha cancha = new Cancha("Predio Idemp " + s, "Calle 3", "Césped");
        Long idCancha = canchaDAO.insertar(cancha);

        Categoria catLibre = categoriaDAO.buscarPorId(2L);
        Usuario cap = new Usuario("cap_idm_" + s, "1234", "cap_idm_" + s + "@test.com", "Capitan", "Idemp", TiposDatos.Rol.CAPITAN);
        Long idCap = usuarioDAO.insertar(cap, AuthService.hashearSha256("1234"), 4);

        Equipo eq1 = new Equipo("Eq1 Idemp " + s, catLibre, "Amarillo", cancha, cap);
        Long idEq1 = equipoDAO.insertar(eq1, idCap);
        Equipo eq2 = new Equipo("Eq2 Idemp " + s, catLibre, "Negro", cancha, cap);
        Long idEq2 = equipoDAO.insertar(eq2, idCap);

        Partido p = partidoService.programarPartido(idFase, idEq1, idEq2, idCancha, null, new Date(), 90);

        Jugador jug = new Jugador(idEq1, "Jugador Idempotencia " + s, randomDni(), new Date(95, 5, 5), "Arquero", TiposDatos.EstadoJugador.ACTIVO);
        Long idJug = jugadorDAO.insertar(jug, idEq1);

        // Primera inserción de sanción por ROJA_DIRECTA
        Sancion sanOriginal = new Sancion(
            new Date(), null, "Motivo Original Inviolable", "GRAVE",
            jug, p, torneo, "TORNEO", "ROJA_DIRECTA", 3, 0, "PENDIENTE"
        );

        Long idPrimero;
        try (Connection conn = ConexionBD.obtenerConexion()) {
            idPrimero = sancionDAO.insertarIdempotente(conn, sanOriginal);
            assertNotNull(idPrimero);
        }

        // Segunda inserción sobre el mismo evento con datos diferentes (intento de sobreescritura)
        Sancion sanDuplicada = new Sancion(
            new Date(), null, "Motivo Ilegítimo Sobreescrito", "LEVE",
            jug, p, torneo, "TORNEO", "ROJA_DIRECTA", 1, 0, "ACTIVA"
        );

        Long idSegundo;
        try (Connection conn = ConexionBD.obtenerConexion()) {
            idSegundo = sancionDAO.insertarIdempotente(conn, sanDuplicada);
            assertEquals(idPrimero, idSegundo, "Debe retornar el ID existente");

            // Verificar en base de datos que NO se sobreescribió el motivo ni la gravedad
            Sancion enBd = sancionDAO.buscarPorId(conn, idPrimero);
            assertEquals("Motivo Original Inviolable", enBd.getMotivo(),
                    "El contrato DO NOTHING debe preservar el motivo original sin mutarlo.");
            assertEquals("GRAVE", enBd.getGravedad(),
                    "La gravedad original debe permanecer inalterada.");
            assertEquals(3, enBd.getPartidosSuspension());
            assertEquals("PENDIENTE", enBd.getEstado());
        }
    }

    @Test
    @DisplayName("H-04: Restricción CHECK chk_cronologia_tipo_expulsion (rechaza NULL en roja y no-roja con tipo)")
    void testCheckCronologiaTipoExpulsion() throws Exception {
        String s = sufijo();

        Torneo torneo = new Torneo("Torneo Check " + s, "LIGA", 8, new Date());
        Long idTorneo = torneoDAO.insertar(torneo);
        Fase fase = new Fase("Fase Check", 1, false);
        Long idFase = faseDAO.insertar(fase, idTorneo);

        Cancha cancha = new Cancha("Predio Check " + s, "Calle 4", "Césped");
        Long idCancha = canchaDAO.insertar(cancha);

        Categoria catLibre = categoriaDAO.buscarPorId(2L);
        Usuario cap = new Usuario("cap_chk_" + s, "1234", "cap_chk_" + s + "@test.com", "Capitan", "Check", TiposDatos.Rol.CAPITAN);
        Long idCap = usuarioDAO.insertar(cap, AuthService.hashearSha256("1234"), 4);

        Equipo eq1 = new Equipo("Eq1 Check " + s, catLibre, "Azul", cancha, cap);
        Long idEq1 = equipoDAO.insertar(eq1, idCap);
        Equipo eq2 = new Equipo("Eq2 Check " + s, catLibre, "Blanco", cancha, cap);
        Long idEq2 = equipoDAO.insertar(eq2, idCap);

        Partido p = partidoService.programarPartido(idFase, idEq1, idEq2, idCancha, null, new Date(), 90);
        Jugador jug = new Jugador(idEq1, "Jugador Check " + s, randomDni(), new Date(95, 6, 6), "Defensa", TiposDatos.EstadoJugador.ACTIVO);
        Long idJug = jugadorDAO.insertar(jug, idEq1);

        try (Connection conn = ConexionBD.obtenerConexion()) {
            // CASO A: TARJETA_ROJA con tipo_expulsion NULL -> DEBE FALLAR con PSQLException (check constraint violation)
            String sqlViolacionRoja = "INSERT INTO cronologias (id_partido, id_jugador, minuto_partido, accion, tipo_expulsion, observacion) "
                                    + "VALUES (?, ?, 45, 'TARJETA_ROJA', NULL, 'Roja con tipo nulo ilegal')";
            try (PreparedStatement ps = conn.prepareStatement(sqlViolacionRoja)) {
                ps.setLong(1, p.getIdPartido());
                ps.setLong(2, idJug);
                PSQLException ex = assertThrows(PSQLException.class, () -> ps.executeUpdate(),
                        "La inserción de TARJETA_ROJA con tipo_expulsion NULL debe violar chk_cronologia_tipo_expulsion");
                assertTrue(ex.getMessage().contains("chk_cronologia_tipo_expulsion"));
            }

            // CASO B: GOL con tipo_expulsion no nulo ('DIRECTA') -> DEBE FALLAR con PSQLException
            String sqlViolacionGol = "INSERT INTO cronologias (id_partido, id_jugador, minuto_partido, accion, tipo_expulsion, observacion) "
                                   + "VALUES (?, ?, 10, 'GOL', 'DIRECTA', 'Gol con tipo expulsion ilegal')";
            try (PreparedStatement ps = conn.prepareStatement(sqlViolacionGol)) {
                ps.setLong(1, p.getIdPartido());
                ps.setLong(2, idJug);
                PSQLException ex = assertThrows(PSQLException.class, () -> ps.executeUpdate(),
                        "La inserción de GOL con tipo_expulsion no nulo debe violar chk_cronologia_tipo_expulsion");
                assertTrue(ex.getMessage().contains("chk_cronologia_tipo_expulsion"));
            }

            // CASO C: TARJETA_ROJA con DIRECTA -> inserción exitosa
            String sqlValidaRoja = "INSERT INTO cronologias (id_partido, id_jugador, minuto_partido, accion, tipo_expulsion, observacion) "
                                 + "VALUES (?, ?, 70, 'TARJETA_ROJA', 'DIRECTA', 'Roja directa válida')";
            try (PreparedStatement ps = conn.prepareStatement(sqlValidaRoja)) {
                ps.setLong(1, p.getIdPartido());
                ps.setLong(2, idJug);
                int rows = ps.executeUpdate();
                assertEquals(1, rows, "La inserción con TARJETA_ROJA y DIRECTA debe prosperar.");
            }

            // CASO D: GOL con NULL -> inserción exitosa
            String sqlValidaGol = "INSERT INTO cronologias (id_partido, id_jugador, minuto_partido, accion, tipo_expulsion, observacion) "
                                + "VALUES (?, ?, 25, 'GOL', NULL, 'Gol válido')";
            try (PreparedStatement ps = conn.prepareStatement(sqlValidaGol)) {
                ps.setLong(1, p.getIdPartido());
                ps.setLong(2, idJug);
                int rows = ps.executeUpdate();
                assertEquals(1, rows, "La inserción con GOL y NULL debe prosperar.");
            }
        }
    }

    @Test
    @DisplayName("H-05: Reversión y consistencia ante fallo transaccional durante cierre de partido")
    void testRollbackTransaccionalCierrePartido() throws Exception {
        String s = sufijo();

        Torneo torneo = new Torneo("Torneo Rollback " + s, "LIGA", 8, new Date());
        Long idTorneo = torneoDAO.insertar(torneo);
        Fase fase = new Fase("Fase Rollback", 1, false);
        Long idFase = faseDAO.insertar(fase, idTorneo);

        Cancha cancha = new Cancha("Predio Rollback " + s, "Calle 5", "Natural");
        Long idCancha = canchaDAO.insertar(cancha);

        Categoria catLibre = categoriaDAO.buscarPorId(2L);
        Usuario cap = new Usuario("cap_rbk_" + s, "1234", "cap_rbk_" + s + "@test.com", "Capitan", "Rollback", TiposDatos.Rol.CAPITAN);
        Long idCap = usuarioDAO.insertar(cap, AuthService.hashearSha256("1234"), 4);

        Equipo eqLocal = new Equipo("Eq Loc " + s, catLibre, "Gris", cancha, cap);
        Long idEqLoc = equipoDAO.insertar(eqLocal, idCap);
        Equipo eqVis = new Equipo("Eq Vis " + s, catLibre, "Negro", cancha, cap);
        Long idEqVis = equipoDAO.insertar(eqVis, idCap);

        Partido p = partidoService.programarPartido(idFase, idEqLoc, idEqVis, idCancha, null, new Date(), 90);
        Long idPartido = p.getIdPartido();

        Jugador j = new Jugador(idEqLoc, "Jugador Rollback " + s, randomDni(), new Date(95, 7, 7), "Delantero", TiposDatos.EstadoJugador.ACTIVO);
        Long idJ = jugadorDAO.insertar(j, idEqLoc);

        // Registrar un gol válido previo
        partidoService.registrarIncidencia(idPartido, idJ, 30, TiposDatos.TipoAccion.GOL, null, "Gol regular");

        // Intentar cerrar el acta suministrando una incidencia inválida (TARJETA_ROJA con tipoExpulsion nulo)
        // Esto provocará una violación de restricción CHECK en el Paso 5 dentro de la transacción
        Cronologia incidenciaCorrupta = new Cronologia(80, TiposDatos.TipoAccion.TARJETA_ROJA, null, j, p, "Roja nula que detona rollback");
        List<Cronologia> incidenciasFinales = new ArrayList<>();
        incidenciasFinales.add(incidenciaCorrupta);

        TorneoExcepcion exCierre = assertThrows(TorneoExcepcion.class, () -> {
            partidoService.cerrarActaPartido(idPartido, new Date(), incidenciasFinales);
        });
        assertTrue(exCierre.getMessage().contains("Fallo en la transacción atómica de cierre de partido"));

        // Verificar que el rollback transaccional protegió la base de datos:
        try (Connection conn = ConexionBD.obtenerConexion()) {
            // 1. El partido sigue en estado PROGRAMADO y sin fecha de finalización real
            Partido pPost = partidoDAO.buscarPorId(conn, idPartido);
            assertEquals(TiposDatos.EstadoPartido.PROGRAMADO, pPost.getEstadoPartido(),
                    "El partido debe conservar su estado PROGRAMADO sin alteraciones tras el rollback.");
            assertNull(pPost.getFechaHoraFinalizacionReal(),
                    "La fecha de finalización real debe seguir nula.");

            // 2. El jugador no fue suspendido indebidamente
            Jugador jPost = jugadorDAO.buscarPorId(conn, idJ);
            assertEquals(TiposDatos.EstadoJugador.ACTIVO, jPost.getEstado(),
                    "El jugador debe permanecer ACTIVO ya que el acta nunca cerró con éxito.");

            // 3. No se consolidaron goles en partido_equipos (marcador permanece en 0)
            PartidoEquipo peLocal = partidoEquipoDAO.buscarLocal(conn, idPartido);
            assertEquals(0, peLocal.getGoles(),
                    "Los goles del equipo local no deben haber sido consolidados en partido_equipos.");

            // 4. La incidencia corrupta no quedó registrada en la base de datos
            String sqlCheck = "SELECT COUNT(*) FROM cronologias WHERE id_partido = ? AND minuto_partido = 80";
            try (PreparedStatement ps = conn.prepareStatement(sqlCheck)) {
                ps.setLong(1, idPartido);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt(1), "La incidencia corrupta del paso 5 debe haberse revertido.");
                }
            }
        }
    }
}
