package com.gestionamateur.tpi.progav.servicio;

import com.gestionamateur.tpi.progav.controlador.GestorCanchas;
import com.gestionamateur.tpi.progav.controlador.GestorUsuarios;
import com.gestionamateur.tpi.progav.dao.*;
import com.gestionamateur.tpi.progav.modelo.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de Integración - Matriz de Autorización y Pertenencia de Recursos (ETAPA C)")
public class AutorizacionServiciosIT {

    private static PartidoService partidoService;
    private static TorneoService torneoService;
    private static EquipoService equipoService;
    private static JugadorService jugadorService;
    private static DisciplinaService disciplinaService;
    private static ReclamoService reclamoService;

    private static ArbitroDAO arbitroDAO;
    private static TorneoDAO torneoDAO;
    private static FaseDAO faseDAO;
    private static CanchaDAO canchaDAO;
    private static UsuarioDAO usuarioDAO;
    private static CategoriaDAO categoriaDAO;
    private static EquipoDAO equipoDAO;
    private static JugadorDAO jugadorDAO;
    private static SancionDAO sancionDAO;

    @BeforeAll
    static void setUp() throws SQLException {
        assertTrue(ConexionBD.comprobarConexion(), "PostgreSQL debe estar activo en puerto 55432");

        partidoService = new PartidoService();
        torneoService = new TorneoService();
        equipoService = new EquipoService();
        jugadorService = new JugadorService();
        disciplinaService = new DisciplinaService();
        reclamoService = new ReclamoService();

        arbitroDAO = new ArbitroDAO();
        torneoDAO = new TorneoDAO();
        faseDAO = new FaseDAO();
        canchaDAO = new CanchaDAO();
        usuarioDAO = new UsuarioDAO();
        categoriaDAO = new CategoriaDAO();
        equipoDAO = new EquipoDAO();
        jugadorDAO = new JugadorDAO();
        sancionDAO = new SancionDAO();
    }

    private String sufijo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Usuario crearUsuario(String username, TiposDatos.Rol rol) throws Exception {
        if (rol == TiposDatos.Rol.ARBITRO) {
            Arbitro arb = new Arbitro(username, "ClaveSegura123!", username + "@test.local", "Nombre", "Apellido", "Nacional", "12345678");
            Long id = arbitroDAO.insertar(arb, AuthService.hashearBCrypt("ClaveSegura123!"));
            arb.setId(id);
            return arb;
        }

        Usuario u = new Usuario(username, "ClaveSegura123!", username + "@test.local", "Nombre", "Apellido", rol);
        int idRol = 1;
        if (rol == TiposDatos.Rol.ORGANIZADOR) idRol = 2;
        else if (rol == TiposDatos.Rol.CAPITAN) idRol = 4;
        else if (rol == TiposDatos.Rol.PUBLICO) idRol = 5;

        Long id = usuarioDAO.insertar(u, AuthService.hashearBCrypt("ClaveSegura123!"), idRol);
        u.setId(id);
        return u;
    }

    @Test
    @DisplayName("AUT-01: Programación de partidos restringida a ADMINISTRADOR y ORGANIZADOR")
    void testAutorizacionProgramacionPartidos() throws Exception {
        String s = sufijo();
        Usuario admin = crearUsuario("adm_part_" + s, TiposDatos.Rol.ADMINISTRADOR);
        Usuario org = crearUsuario("org_part_" + s, TiposDatos.Rol.ORGANIZADOR);
        Usuario arb = crearUsuario("arb_part_" + s, TiposDatos.Rol.ARBITRO);
        Usuario cap = crearUsuario("cap_part_" + s, TiposDatos.Rol.CAPITAN);
        Usuario pub = new Usuario("pub_part_" + s, "Clave123!", "pub@test.com", "Pub", "Invitado", TiposDatos.Rol.PUBLICO);

        Torneo t = torneoService.crearTorneo("Torneo Aut Part " + s, "LIGA", 8, new Date(), admin);
        Fase f = torneoService.listarFasesDeTorneo(t.getIdTorneo()).get(0);
        Cancha c = new Cancha("Predio Aut " + s, "Calle 1", "Sintético");
        Long idC = canchaDAO.insertar(c);
        Categoria cat = categoriaDAO.buscarPorId(2L);

        Equipo eq1 = equipoService.registrarEquipo("Eq1 " + s, "E1", cat.getId(), "Rojo", c, cap.getId(), admin);
        Equipo eq2 = equipoService.registrarEquipo("Eq2 " + s, "E2", cat.getId(), "Azul", c, cap.getId(), admin);

        // Permitido para ADMINISTRADOR y ORGANIZADOR
        assertNotNull(partidoService.programarPartido(f.getIdFase(), eq1.getIdEquipo(), eq2.getIdEquipo(), idC, null, new Date(), 90, admin));
        assertNotNull(partidoService.programarPartido(f.getIdFase(), eq1.getIdEquipo(), eq2.getIdEquipo(), idC, null, new Date(), 90, org));

        // Denegado para ARBITRO, CAPITAN, PUBLICO
        TorneoExcepcion exArb = assertThrows(TorneoExcepcion.class, () ->
            partidoService.programarPartido(f.getIdFase(), eq1.getIdEquipo(), eq2.getIdEquipo(), idC, null, new Date(), 90, arb));
        assertTrue(exArb.getMessage().contains("Acceso denegado"));

        TorneoExcepcion exCap = assertThrows(TorneoExcepcion.class, () ->
            partidoService.programarPartido(f.getIdFase(), eq1.getIdEquipo(), eq2.getIdEquipo(), idC, null, new Date(), 90, cap));
        assertTrue(exCap.getMessage().contains("Acceso denegado"));

        TorneoExcepcion exPub = assertThrows(TorneoExcepcion.class, () ->
            partidoService.programarPartido(f.getIdFase(), eq1.getIdEquipo(), eq2.getIdEquipo(), idC, null, new Date(), 90, pub));
        assertTrue(exPub.getMessage().contains("Acceso denegado"));
    }

    @Test
    @DisplayName("AUT-02: Gestión de torneos restringida a ADMINISTRADOR y ORGANIZADOR")
    void testAutorizacionGestionTorneos() throws Exception {
        String s = sufijo();
        Usuario admin = crearUsuario("adm_tor_" + s, TiposDatos.Rol.ADMINISTRADOR);
        Usuario org = crearUsuario("org_tor_" + s, TiposDatos.Rol.ORGANIZADOR);
        Usuario cap = crearUsuario("cap_tor_" + s, TiposDatos.Rol.CAPITAN);
        Usuario arb = crearUsuario("arb_tor_" + s, TiposDatos.Rol.ARBITRO);

        // Creación por roles autorizados
        Torneo tAdmin = torneoService.crearTorneo("Torneo Adm " + s, "LIGA", 6, new Date(), admin);
        assertNotNull(tAdmin.getIdTorneo());

        Torneo tOrg = torneoService.crearTorneo("Torneo Org " + s, "LIGA", 6, new Date(), org);
        assertNotNull(tOrg.getIdTorneo());

        // Rechazo para roles no autorizados
        assertThrows(TorneoExcepcion.class, () ->
            torneoService.crearTorneo("Torneo Cap " + s, "LIGA", 6, new Date(), cap));
        assertThrows(TorneoExcepcion.class, () ->
            torneoService.crearTorneo("Torneo Arb " + s, "LIGA", 6, new Date(), arb));

        // Agregar fase
        assertNotNull(torneoService.agregarFase(tAdmin.getIdTorneo(), "Playoffs", 2, true, admin));
        assertThrows(TorneoExcepcion.class, () ->
            torneoService.agregarFase(tAdmin.getIdTorneo(), "Cuartos", 3, false, cap));
    }

    @Test
    @DisplayName("AUT-03: Fallos disciplinarios y anulaciones restringidas a ADMINISTRADOR y ORGANIZADOR")
    void testAutorizacionTribunalDisciplina() throws Exception {
        String s = sufijo();
        Usuario admin = crearUsuario("adm_disc_" + s, TiposDatos.Rol.ADMINISTRADOR);
        Usuario cap = crearUsuario("cap_disc_" + s, TiposDatos.Rol.CAPITAN);
        Usuario arb = crearUsuario("arb_disc_" + s, TiposDatos.Rol.ARBITRO);

        Torneo t = torneoService.crearTorneo("Torneo Disc " + s, "LIGA", 4, new Date(), admin);
        Fase f = torneoService.listarFasesDeTorneo(t.getIdTorneo()).get(0);
        Cancha c = new Cancha("Predio Disc " + s, "Calle 2", "Natural");
        canchaDAO.insertar(c);
        Categoria cat = categoriaDAO.buscarPorId(2L);
        Equipo eq = equipoService.registrarEquipo("Eq Disc " + s, "ED", cat.getId(), "Verde", c, cap.getId(), admin);
        Equipo eqRival = equipoService.registrarEquipo("Eq Rival Disc " + s, "ER", cat.getId(), "Azul", c, cap.getId(), admin);

        Jugador jug = jugadorService.registrarJugador("Jug Sancionado " + s, 33000000 + (int)(Math.random()*900000),
                new Date(98, 1, 1), "Mediocampista", eq.getIdEquipo(), admin);

        Partido p = partidoService.programarPartido(f.getIdFase(), eq.getIdEquipo(), eqRival.getIdEquipo(), c.getIdCancha(), arb.getId(), new Date(), 90);

        Sancion san = new Sancion(new Date(), null, "Informe arbitral", "MEDIA", jug, p, t, "TORNEO", "ROJA_DIRECTA", 2, 0, "PENDIENTE");
        Long idSan = sancionDAO.insertar(san);

        // Denegado para CAPITAN y ARBITRO
        assertThrows(TorneoExcepcion.class, () ->
            disciplinaService.emitirFalloTribunal(idSan, 2, "Fallo disciplinario formal por conducta antideportiva", "MEDIA", cap));
        assertThrows(TorneoExcepcion.class, () ->
            disciplinaService.emitirFalloTribunal(idSan, 2, "Fallo disciplinario formal por conducta antideportiva", "MEDIA", arb));

        // Permitido para ADMINISTRADOR
        assertDoesNotThrow(() ->
            disciplinaService.emitirFalloTribunal(idSan, 2, "Fallo disciplinario formal por conducta antideportiva", "MEDIA", admin));

        // Anular sanción
        assertThrows(TorneoExcepcion.class, () ->
            disciplinaService.anularSancion(idSan, "Anulación no autorizada", cap));
        assertDoesNotThrow(() ->
            disciplinaService.anularSancion(idSan, "Anulación justificada por prueba de video", admin));
    }

    @Test
    @DisplayName("AUT-04: Pertenencia de recursos en reclamos y apelaciones de capitanes")
    void testPertenenciaRecursoCapitanes() throws Exception {
        String s = sufijo();
        Usuario admin = crearUsuario("adm_rec_" + s, TiposDatos.Rol.ADMINISTRADOR);
        Usuario capLocal = crearUsuario("cap_loc_" + s, TiposDatos.Rol.CAPITAN);
        Usuario capVis = crearUsuario("cap_vis_" + s, TiposDatos.Rol.CAPITAN);
        Usuario capTercero = crearUsuario("cap_ter_" + s, TiposDatos.Rol.CAPITAN);

        Torneo t = torneoService.crearTorneo("Torneo Rec Pert " + s, "LIGA", 6, new Date(), admin);
        Fase f = torneoService.listarFasesDeTorneo(t.getIdTorneo()).get(0);
        Cancha c = new Cancha("Predio Rec " + s, "Calle 3", "Tierra");
        canchaDAO.insertar(c);
        Categoria cat = categoriaDAO.buscarPorId(2L);

        Equipo eqLocal = equipoService.registrarEquipo("Eq Loc " + s, "EL", cat.getId(), "Blanco", c, capLocal.getId(), capLocal);
        Equipo eqVis = equipoService.registrarEquipo("Eq Vis " + s, "EV", cat.getId(), "Negro", c, capVis.getId(), capVis);
        Equipo eqTercero = equipoService.registrarEquipo("Eq Ter " + s, "ET", cat.getId(), "Amarillo", c, capTercero.getId(), capTercero);

        Partido p = partidoService.programarPartido(f.getIdFase(), eqLocal.getIdEquipo(), eqVis.getIdEquipo(), c.getIdCancha(), null, new Date(), 90, admin);
        partidoService.cerrarActaPartido(p.getIdPartido(), new Date(), null, admin);

        // Capitán participante (local o visitante) puede reclamar
        assertNotNull(reclamoService.presentarReclamoPartido(
            p.getIdPartido(), capLocal.getId(), "Reclamo fundado por inclusión antirreglamentaria de jugador", capLocal));

        // Capitán ajeno a ambos equipos es rechazado por violación de pertenencia
        TorneoExcepcion exPertenencia = assertThrows(TorneoExcepcion.class, () ->
            reclamoService.presentarReclamoPartido(
                p.getIdPartido(), capTercero.getId(), "Reclamo fundado de un equipo no participante", capTercero));
        assertTrue(exPertenencia.getMessage().contains("no pertenece"));

        // Apelación de sanciones: jugador de eqLocal sancionado
        Jugador jLocal = jugadorService.registrarJugador("Jug Loc " + s, 34000000 + (int)(Math.random()*900000),
                new Date(97, 2, 2), "Arquero", eqLocal.getIdEquipo(), capLocal);

        Sancion san = new Sancion(new Date(), null, "Sancion apelable", "LEVE", jLocal, p, t, "TORNEO", "DOBLE_AMARILLA", 1, 0, "ACTIVA");
        san.setFechaPublicacionFormal(new Date());
        Long idSan = sancionDAO.insertar(san);

        // Capitán del jugador puede apelar
        assertNotNull(reclamoService.apelarSancion(idSan, capLocal.getId(), "Apelación fundada por error material manifiesto", capLocal));

        // Capitán de equipo rival/tercero rechazado por pertenencia
        TorneoExcepcion exApelacion = assertThrows(TorneoExcepcion.class, () ->
            reclamoService.apelarSancion(idSan, capVis.getId(), "Apelación improcedente de rival", capVis));
        assertTrue(exApelacion.getMessage().contains("no es el capitán del equipo del jugador sancionado"));
    }

    @Test
    @DisplayName("AUT-05: Un capitán no puede registrar equipos a nombre de otro usuario ni incorporar jugadores ajenos")
    void testCapitanRestriccionesPertenenciaRegistro() throws Exception {
        String s = sufijo();
        Usuario cap1 = crearUsuario("cap1_reg_" + s, TiposDatos.Rol.CAPITAN);
        Usuario cap2 = crearUsuario("cap2_reg_" + s, TiposDatos.Rol.CAPITAN);
        Categoria cat = categoriaDAO.buscarPorId(2L);
        Cancha c = new Cancha("Predio Reg " + s, "Calle 4", "Sintético");
        canchaDAO.insertar(c);

        // Cap1 intenta registrar equipo poniendo a Cap2 como capitán -> rechazado
        TorneoExcepcion exEq = assertThrows(TorneoExcepcion.class, () ->
            equipoService.registrarEquipo("Eq Falso " + s, "EF", cat.getId(), "Gris", c, cap2.getId(), cap1));
        assertTrue(exEq.getMessage().contains("designándose a sí mismo"));

        // Cap1 registra su equipo
        Equipo eq1 = equipoService.registrarEquipo("Eq Propio " + s, "EP", cat.getId(), "Gris", c, cap1.getId(), cap1);

        // Cap2 intenta agregar un jugador al plantel del equipo de Cap1 -> rechazado
        TorneoExcepcion exJug = assertThrows(TorneoExcepcion.class, () ->
            jugadorService.registrarJugador("Jug Ajeno " + s, 35000000 + (int)(Math.random()*900000),
                    new Date(96, 5, 5), "Defensor", eq1.getIdEquipo(), cap2));
        assertTrue(exJug.getMessage().contains("su propio equipo"));
    }

    @Test
    @DisplayName("AUT-06: Un árbitro no puede cerrar actas de encuentros asignados a otro árbitro")
    void testArbitroNoPuedeCerrarPartidoAjeno() throws Exception {
        String s = sufijo();
        Usuario admin = crearUsuario("adm_arb_" + s, TiposDatos.Rol.ADMINISTRADOR);
        Usuario arb1 = crearUsuario("arb1_close_" + s, TiposDatos.Rol.ARBITRO);
        Usuario arb2 = crearUsuario("arb2_close_" + s, TiposDatos.Rol.ARBITRO);
        Usuario cap = crearUsuario("cap_arb_" + s, TiposDatos.Rol.CAPITAN);

        Torneo t = torneoService.crearTorneo("Torneo Arb " + s, "LIGA", 4, new Date(), admin);
        Fase f = torneoService.listarFasesDeTorneo(t.getIdTorneo()).get(0);
        Cancha c = new Cancha("Cancha Arb " + s, "Calle X", "Césped");
        canchaDAO.insertar(c);
        Categoria cat = categoriaDAO.buscarPorId(2L);

        Equipo e1 = equipoService.registrarEquipo("Eq Arb 1 " + s, "EA1", cat.getId(), "Verde", c, cap.getId(), admin);
        Equipo e2 = equipoService.registrarEquipo("Eq Arb 2 " + s, "EA2", cat.getId(), "Rojo", c, cap.getId(), admin);

        // Partido programado con arb1 designado
        Partido pConArb1 = partidoService.programarPartido(f.getIdFase(), e1.getIdEquipo(), e2.getIdEquipo(), c.getIdCancha(), arb1.getId(), new Date(), 90, admin);

        // arb2 (árbitro ajeno) intenta cerrar acta -> rechazado
        TorneoExcepcion exOtroArb = assertThrows(TorneoExcepcion.class, () ->
            partidoService.cerrarActaPartido(pConArb1.getIdPartido(), new Date(), null, arb2));
        assertTrue(exOtroArb.getMessage().contains("no es el árbitro oficial designado"));

        // arb1 (árbitro titular designado) sí puede cerrar acta
        assertDoesNotThrow(() ->
            partidoService.cerrarActaPartido(pConArb1.getIdPartido(), new Date(), null, arb1));
    }

    @Test
    @DisplayName("AUT-07: Bloqueo de escalamiento de privilegios en GestorUsuarios sin operador administrador")
    void testRechazoRegistroPrivilegiadoSinAdmin() throws Exception {
        GestorUsuarios gu = new GestorUsuarios();
        String s = sufijo();

        // Intento de registrar ADMINISTRADOR con operador null -> rechazado
        String resp1 = gu.registrarUsuario("hack_adm_" + s, "Clave123!", "hack1@test.com", "Hacker", "Adm", TiposDatos.Rol.ADMINISTRADOR, null);
        assertTrue(resp1.startsWith("ERROR"));
        assertTrue(resp1.contains("Acceso denegado"));

        // Intento de registrar ORGANIZADOR con operador null -> rechazado
        String resp2 = gu.registrarUsuario("hack_org_" + s, "Clave123!", "hack2@test.com", "Hacker", "Org", TiposDatos.Rol.ORGANIZADOR, null);
        assertTrue(resp2.startsWith("ERROR"));
        assertTrue(resp2.contains("Acceso denegado"));

        // Intento de registrar ARBITRO con operador null -> rechazado
        String resp3 = gu.registrarUsuario("hack_arb_" + s, "Clave123!", "hack3@test.com", "Hacker", "Arb", TiposDatos.Rol.ARBITRO, null);
        assertTrue(resp3.startsWith("ERROR"));
        assertTrue(resp3.contains("Acceso denegado"));

        // Intento de registrar ARBITRO con operador CAPITAN -> rechazado
        Usuario cap = crearUsuario("cap_reg_op_" + s, TiposDatos.Rol.CAPITAN);
        String resp4 = gu.registrarUsuario("hack_arb2_" + s, "Clave123!", "hack4@test.com", "Hacker", "Arb2", TiposDatos.Rol.ARBITRO, cap);
        assertTrue(resp4.startsWith("ERROR"));
        assertTrue(resp4.contains("Acceso denegado"));

        // Con operador ADMINISTRADOR -> exitoso
        Usuario admin = crearUsuario("adm_op_" + s, TiposDatos.Rol.ADMINISTRADOR);
        String resp5 = gu.registrarUsuario("val_arb_" + s, "Clave123!", "val_arb_" + s + "@test.com", "Valido", "Arb", TiposDatos.Rol.ARBITRO, admin);
        assertTrue(resp5.startsWith("ÉXITO"));
    }

    @Test
    @DisplayName("AUT-08: Validación estricta de pertenencia de capitán en plantel, registro y apelaciones")
    void testCapitanValidacionEstrictaPertenencia() throws Exception {
        String s = sufijo();
        Usuario cap = crearUsuario("cap_own_" + s, TiposDatos.Rol.CAPITAN);
        Usuario admin = crearUsuario("adm_own_" + s, TiposDatos.Rol.ADMINISTRADOR);
        Categoria cat = categoriaDAO.buscarPorId(2L);
        Cancha c = new Cancha("Cancha Own " + s, "Calle Own", "Tierra");
        canchaDAO.insertar(c);

        // Capitan registra jugador con idEquipo = null -> debe ser rechazado
        TorneoExcepcion exSinEquipo = assertThrows(TorneoExcepcion.class, () ->
            jugadorService.registrarJugador("Jug Sin Equipo " + s, 36000000 + (int)(Math.random()*900000),
                new Date(95, 1, 1), "Delantero", null, cap));
        assertTrue(exSinEquipo.getMessage().contains("especificar su propio equipo"));

        // Jugador libre registrado sin equipo
        Jugador jLibre = jugadorService.registrarJugador("Jug Libre " + s, 37000000 + (int)(Math.random()*900000),
                new Date(95, 1, 1), "Delantero", null, admin);

        // Torneo y partido para generar sanción sobre jLibre
        Torneo t = torneoService.crearTorneo("Torneo Own " + s, "LIGA", 4, new Date(), admin);
        Fase f = torneoService.listarFasesDeTorneo(t.getIdTorneo()).get(0);
        Equipo eq1 = equipoService.registrarEquipo("Eq Own 1 " + s, "EO1", cat.getId(), "Negro", c, cap.getId(), cap);
        Equipo eq2 = equipoService.registrarEquipo("Eq Own 2 " + s, "EO2", cat.getId(), "Blanco", c, admin.getId(), admin);
        Partido p = partidoService.programarPartido(f.getIdFase(), eq1.getIdEquipo(), eq2.getIdEquipo(), c.getIdCancha(), null, new Date(), 90, admin);

        Sancion sanLibre = new Sancion(new Date(), null, "Sancion jugador libre", "GRAVE", jLibre, p, t, "TORNEO", "ROJA_DIRECTA", 2, 0, "ACTIVA");
        sanLibre.setFechaPublicacionFormal(new Date());
        Long idSanLibre = sancionDAO.insertar(sanLibre);

        // Capitan ajeno intenta apelar sanción de jugador libre sin equipo -> rechazado
        TorneoExcepcion exApelLibre = assertThrows(TorneoExcepcion.class, () ->
            reclamoService.apelarSancion(idSanLibre, cap.getId(), "Apelación de jugador no perteneciente", cap));
        assertTrue(exApelLibre.getMessage().contains("no pertenece a un equipo"));
    }

    @Test
    @DisplayName("AUT-09: GestorCanchas restringe el registro de sedes a ADMINISTRADOR y ORGANIZADOR")
    void testGestorCanchasAutorizacion() throws Exception {
        GestorCanchas gc = new GestorCanchas();
        String s = sufijo();
        Usuario arb = crearUsuario("arb_cancha_" + s, TiposDatos.Rol.ARBITRO);
        Usuario cap = crearUsuario("cap_cancha_" + s, TiposDatos.Rol.CAPITAN);
        Usuario org = crearUsuario("org_cancha_" + s, TiposDatos.Rol.ORGANIZADOR);

        // ARBITRO rechazado
        TorneoExcepcion exArb = assertThrows(TorneoExcepcion.class, () ->
            gc.registrarCancha("Predio Arb " + s, "Av 1", "Césped", arb));
        assertTrue(exArb.getMessage().contains("Acceso denegado"));

        // CAPITAN rechazado
        TorneoExcepcion exCap = assertThrows(TorneoExcepcion.class, () ->
            gc.registrarCancha("Predio Cap " + s, "Av 2", "Césped", cap));
        assertTrue(exCap.getMessage().contains("Acceso denegado"));

        // ORGANIZADOR permitido
        String respOrg = gc.registrarCancha("Predio Org " + s, "Av 3", "Césped", org);
        assertTrue(respOrg.startsWith("ÉXITO"));
    }

    @Test
    @DisplayName("ADR-13 / H-05: Emisión de fallo disciplinario preserva estado LESIONADO e INHABILITADO sin sobrescribir a SUSPENDIDO")
    void testTribunalPreservaEstadoLesionadoEInhabilitado() throws Exception {
        String s = sufijo();
        Usuario admin = crearUsuario("adm_adr13_" + s, TiposDatos.Rol.ADMINISTRADOR);
        Usuario cap = crearUsuario("cap_adr13_" + s, TiposDatos.Rol.CAPITAN);
        Usuario arb = crearUsuario("arb_adr13_" + s, TiposDatos.Rol.ARBITRO);

        Torneo t = torneoService.crearTorneo("Torneo ADR13 " + s, "LIGA", 4, new Date(), admin);
        Fase f = torneoService.listarFasesDeTorneo(t.getIdTorneo()).get(0);
        Cancha c = new Cancha("Predio ADR13 " + s, "Calle ADR13", "Natural");
        canchaDAO.insertar(c);
        Categoria cat = categoriaDAO.buscarPorId(2L);
        Equipo eq = equipoService.registrarEquipo("Eq ADR13 " + s, "EA", cat.getId(), "Verde", c, cap.getId(), admin);
        Equipo eqRival = equipoService.registrarEquipo("Eq Rival ADR13 " + s, "ER", cat.getId(), "Azul", c, cap.getId(), admin);

        Jugador jugLesionado = jugadorService.registrarJugador("Jug Lesionado " + s, 36000000 + (int)(Math.random()*900000),
                new Date(98, 1, 1), "Delantero", eq.getIdEquipo(), admin);

        // Cambiar estado a LESIONADO
        try (Connection conn = ConexionBD.obtenerConexionTest()) {
            jugadorDAO.actualizarEstado(conn, jugLesionado.getId(), "LESIONADO");
        }

        Partido p = partidoService.programarPartido(f.getIdFase(), eq.getIdEquipo(), eqRival.getIdEquipo(), c.getIdCancha(), arb.getId(), new Date(System.currentTimeMillis() - 7200000L), 90);

        Sancion san = new Sancion(new Date(), null, "Informe arbitral por agresión previa", "MEDIA", jugLesionado, p, t, "TORNEO", "ADMINISTRATIVA", 2, 0, "PENDIENTE");
        Long idSan = sancionDAO.insertar(san);

        // Administrador emite fallo disciplinario
        disciplinaService.emitirFalloTribunal(idSan, 2, "Fallo formal por conducta indebida", "MEDIA", admin);

        // Comprobación crítica ADR-13: El jugador DEBE permanecer LESIONADO, no sobrescribirse a SUSPENDIDO
        try (Connection conn = ConexionBD.obtenerConexionTest()) {
            Jugador postFallo = jugadorDAO.buscarPorId(conn, jugLesionado.getId());
            assertEquals(TiposDatos.EstadoJugador.LESIONADO, postFallo.getEstado(), "El jugador lesionado no debe ser sobrescrito a SUSPENDIDO");
        }

        // Comprobación de elegibilidad: No habilitado
        assertFalse(disciplinaService.esJugadorHabilitado(jugLesionado.getId(), t.getIdTorneo()));

        // Al anularse la sanción formalmente, el jugador DEBE permanecer LESIONADO y no regresar erróneamente a ACTIVO
        disciplinaService.anularSancion(idSan, "Anulación por error de identidad", admin);
        try (Connection conn = ConexionBD.obtenerConexionTest()) {
            Jugador postAnulacion = jugadorDAO.buscarPorId(conn, jugLesionado.getId());
            assertEquals(TiposDatos.EstadoJugador.LESIONADO, postAnulacion.getEstado(), "Al levantarse la sanción debe conservar su condición de LESIONADO");
        }
        assertFalse(disciplinaService.esJugadorHabilitado(jugLesionado.getId(), t.getIdTorneo()));

        // Caso B: Jugador INHABILITADO
        Jugador jugInhabilitado = jugadorService.registrarJugador("Jug Inhabilitado " + s, 37000000 + (int)(Math.random()*900000),
                new Date(97, 2, 2), "Defensor", eq.getIdEquipo(), admin);
        try (Connection conn = ConexionBD.obtenerConexionTest()) {
            jugadorDAO.actualizarEstado(conn, jugInhabilitado.getId(), "INHABILITADO");
        }
        Sancion sanInh = new Sancion(new Date(), null, "Informe arbitral por conducta antideportiva", "MEDIA", jugInhabilitado, p, t, "TORNEO", "ADMINISTRATIVA", 2, 0, "PENDIENTE");
        Long idSanInh = sancionDAO.insertar(sanInh);

        disciplinaService.emitirFalloTribunal(idSanInh, 2, "Fallo formal por conducta antideportiva", "MEDIA", admin);

        try (Connection conn = ConexionBD.obtenerConexionTest()) {
            Jugador postFalloInh = jugadorDAO.buscarPorId(conn, jugInhabilitado.getId());
            assertEquals(TiposDatos.EstadoJugador.INHABILITADO, postFalloInh.getEstado(), "El jugador inhabilitado no debe ser sobrescrito a SUSPENDIDO");
        }
        assertFalse(disciplinaService.esJugadorHabilitado(jugInhabilitado.getId(), t.getIdTorneo()));

        disciplinaService.anularSancion(idSanInh, "Anulación por reconsideración de pruebas", admin);
        try (Connection conn = ConexionBD.obtenerConexionTest()) {
            Jugador postAnulacionInh = jugadorDAO.buscarPorId(conn, jugInhabilitado.getId());
            assertEquals(TiposDatos.EstadoJugador.INHABILITADO, postAnulacionInh.getEstado(), "Al levantarse la sanción debe conservar su condición de INHABILITADO");
        }
        assertFalse(disciplinaService.esJugadorHabilitado(jugInhabilitado.getId(), t.getIdTorneo()));
    }
}
