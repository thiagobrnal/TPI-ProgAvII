package com.gestionamateur.tpi.progav.servicio;

import com.gestionamateur.tpi.progav.controlador.GestorCanchas;
import com.gestionamateur.tpi.progav.controlador.GestorEquipos;
import com.gestionamateur.tpi.progav.controlador.GestorJugadores;
import com.gestionamateur.tpi.progav.controlador.GestorPartidos;
import com.gestionamateur.tpi.progav.controlador.GestorUsuarios;
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

@DisplayName("Pruebas Integrales de Cierre: Autorización Fail-Closed y CRUD Completo en PostgreSQL")
public class CierreSeguridadYCrudIT {

    private static UsuarioDAO usuarioDAO;
    private static ArbitroDAO arbitroDAO;
    private static CategoriaDAO categoriaDAO;
    private static CanchaDAO canchaDAO;
    private static EquipoDAO equipoDAO;
    private static JugadorDAO jugadorDAO;
    private static TorneoDAO torneoDAO;
    private static FaseDAO faseDAO;

    private static GestorCanchas gestorCanchas;
    private static GestorEquipos gestorEquipos;
    private static GestorJugadores gestorJugadores;
    private static GestorUsuarios gestorUsuarios;
    private static GestorPartidos gestorPartidos;
    private static TorneoService torneoService;
    private static PartidoService partidoService;
    private static UsuarioService usuarioService;
    private static ConfiguracionService configuracionService;
    private static ReporteService reporteService;
    private static DisciplinaService disciplinaService;
    private static ReclamoService reclamoService;

    @BeforeAll
    static void setUp() throws SQLException {
        assertTrue(ConexionBD.comprobarConexion(), "PostgreSQL debe estar activo para ejecutar los tests de integración.");
        ConexionBD.validarDestinoDePruebas();

        usuarioDAO = new UsuarioDAO();
        arbitroDAO = new ArbitroDAO();
        categoriaDAO = new CategoriaDAO();
        canchaDAO = new CanchaDAO();
        equipoDAO = new EquipoDAO();
        jugadorDAO = new JugadorDAO();
        torneoDAO = new TorneoDAO();
        faseDAO = new FaseDAO();

        gestorCanchas = new GestorCanchas();
        gestorEquipos = new GestorEquipos();
        gestorJugadores = new GestorJugadores();
        gestorUsuarios = new GestorUsuarios();
        gestorPartidos = new GestorPartidos();
        torneoService = new TorneoService();
        partidoService = new PartidoService();
        usuarioService = new UsuarioService();
        configuracionService = new ConfiguracionService();
        reporteService = new ReporteService();
        disciplinaService = new DisciplinaService();
        reclamoService = new ReclamoService();
    }

    private String sufijo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Usuario crearUsuario(String alias, TiposDatos.Rol rol) throws Exception {
        if (rol == TiposDatos.Rol.ARBITRO) {
            Arbitro arb = new Arbitro(alias, "ClaveSegura123!", alias + "@test.local", "Arb", "Test", "Regional", "3764000001");
            Long id = arbitroDAO.insertar(arb, AuthService.hashearBCrypt("ClaveSegura123!"));
            arb.setId(id);
            return arb;
        }
        int idRol = 1;
        if (rol == TiposDatos.Rol.ORGANIZADOR) idRol = 2;
        else if (rol == TiposDatos.Rol.CAPITAN) idRol = 4;
        else if (rol == TiposDatos.Rol.PUBLICO) idRol = 5;
        Usuario u = new Usuario(alias, "ClaveSegura123!", alias + "@test.local", "Nom", "Ape", rol);
        Long id = usuarioDAO.insertar(u, AuthService.hashearBCrypt("ClaveSegura123!"), idRol);
        u.setId(id);
        return u;
    }

    @Test
    @DisplayName("SEC-01: Brechas fail-closed cerradas en Canchas, Incidencias, Reportes, Configuración, Tribunal, Reclamos y Jugadores")
    void testBrechasSeguridadFailClosed() throws Exception {
        String s = sufijo();
        Usuario admin = crearUsuario("sec_adm_" + s, TiposDatos.Rol.ADMINISTRADOR);
        Usuario org = crearUsuario("sec_org_" + s, TiposDatos.Rol.ORGANIZADOR);
        Usuario cap1 = crearUsuario("sec_cap1_" + s, TiposDatos.Rol.CAPITAN);
        Usuario cap2 = crearUsuario("sec_cap2_" + s, TiposDatos.Rol.CAPITAN);
        Usuario arb1 = crearUsuario("sec_arb1_" + s, TiposDatos.Rol.ARBITRO);
        Usuario arb2 = crearUsuario("sec_arb2_" + s, TiposDatos.Rol.ARBITRO);

        // 1. GestorCanchas.registrarCancha(null) nunca hace fallback silencioso a Administrador
        assertThrows(TorneoExcepcion.class,
                () -> gestorCanchas.registrarCancha("Cancha Sin Op " + s, "Dir", "Césped Natural", null));

        // 2. PartidoService.registrarIncidencia(6-arg sin operador) es fail-closed
        Categoria cat = categoriaDAO.buscarTodas().get(0);
        Cancha cancha = canchaDAO.buscarTodas().get(0);
        Torneo torneo = torneoService.crearTorneo("Torneo Sec " + s, "LIGA", 4, new Date(), admin);
        Fase fase = torneoService.listarFasesDeTorneo(torneo.getIdTorneo()).get(0);

        EquipoService eqSvc = new EquipoService();
        Equipo eq1 = eqSvc.registrarEquipo("Eq Sec1 " + s, "S1", cat.getId(), "Rojo", cancha, cap1.getId(), admin);
        Equipo eq2 = eqSvc.registrarEquipo("Eq Sec2 " + s, "S2", cat.getId(), "Azul", cancha, cap2.getId(), admin);
        Jugador j1 = new Jugador(eq1.getIdEquipo(), "Jug Cap1 " + s, 40000000 + (int) (Math.random() * 9000000),
                new Date(95, 1, 1), "Delantero", TiposDatos.EstadoJugador.ACTIVO);
        j1.setId(jugadorDAO.insertar(j1, eq1.getIdEquipo()));
        Jugador j2 = new Jugador(eq2.getIdEquipo(), "Jug Cap2 " + s, 40000000 + (int) (Math.random() * 9000000),
                new Date(95, 2, 2), "Defensor", TiposDatos.EstadoJugador.ACTIVO);
        j2.setId(jugadorDAO.insertar(j2, eq2.getIdEquipo()));

        Partido p = partidoService.programarPartido(fase.getIdFase(), eq1.getIdEquipo(), eq2.getIdEquipo(),
                cancha.getIdCancha(), arb1.getId(), new Date(System.currentTimeMillis() - 3600000L), 90, org);

        assertThrows(TorneoExcepcion.class, () ->
                partidoService.registrarIncidencia(p.getIdPartido(), j1.getId(), 10, TiposDatos.TipoAccion.GOL, null, "Sin op"));
        assertThrows(TorneoExcepcion.class, () ->
                partidoService.registrarIncidencia(p.getIdPartido(), j1.getId(), 10, TiposDatos.TipoAccion.GOL, null, "Otro arb", arb2));
        assertThrows(TorneoExcepcion.class, () ->
                partidoService.registrarIncidencia(p.getIdPartido(), j1.getId(), 10, TiposDatos.TipoAccion.GOL, null, "Capitan", cap1));
        assertDoesNotThrow(() ->
                partidoService.registrarIncidencia(p.getIdPartido(), j1.getId(), 10, TiposDatos.TipoAccion.GOL, null, "Arb designado", arb1));

        // 3. ReporteService sin operador y con rol no autorizado rechaza; Admin/Org permite
        assertThrows(TorneoExcepcion.class, () -> reporteService.obtenerTablaPosiciones());
        assertThrows(TorneoExcepcion.class, () -> reporteService.obtenerTablaPosiciones(cap1));
        assertThrows(TorneoExcepcion.class, () -> reporteService.obtenerTablaPosiciones(arb1));
        assertNotNull(reporteService.obtenerTablaPosiciones(org));

        // 4. ConfiguracionService: solo Administrador
        assertThrows(TorneoExcepcion.class, () -> configuracionService.obtenerValores(org));
        assertThrows(TorneoExcepcion.class, () -> configuracionService.obtenerValores(cap1));
        assertFalse(configuracionService.obtenerValores(admin).isEmpty());

        // 5. DisciplinaService y ReclamoService sin operador / rol no autorizado rechazan
        assertThrows(TorneoExcepcion.class, () -> disciplinaService.listarSancionesPendientesTribunal());
        assertThrows(TorneoExcepcion.class, () -> disciplinaService.listarJugadoresParaTribunal(cap1));
        assertFalse(disciplinaService.listarJugadoresParaTribunal(org).isEmpty());
        assertThrows(TorneoExcepcion.class, () -> reclamoService.listarReclamosPendientes(arb1));

        // 6. JugadorService: capitán solo ve jugadores de sus propios equipos (protección de DNI)
        List<Jugador> vistaCap1 = gestorJugadores.listarJugadoresAutorizados(cap1);
        assertTrue(vistaCap1.stream().anyMatch(j -> j.getId().equals(j1.getId())));
        assertFalse(vistaCap1.stream().anyMatch(j -> j.getId().equals(j2.getId())),
                "CAPITAN no debe acceder al listado/DNI de jugadores de otros equipos.");

        // 7. CU-17 GestorUsuarios: solo ADMINISTRADOR puede crear cualquier rol desde el gestor (incluido CAPITAN)
        String resOrgCap = gestorUsuarios.registrarUsuario("no_org_" + s, "ClaveFuerte123!", "n@" + s + ".com",
                "N", "O", TiposDatos.Rol.CAPITAN, org);
        assertTrue(resOrgCap.startsWith("ERROR DE SEGURIDAD"));

        // 8. Autenticación fail-closed ante cuenta desactivada y cierre de sesión
        assertNotNull(gestorUsuarios.autenticarUsuario(cap2.getNombreUsuario(), "ClaveSegura123!"));
        gestorUsuarios.cerrarSesion(cap2);
        String deact = gestorUsuarios.cambiarEstadoCuenta(cap2.getId(), false, admin);
        assertTrue(deact.startsWith("ÉXITO"));
        assertNull(gestorUsuarios.autenticarUsuario(cap2.getNombreUsuario(), "ClaveSegura123!"));
    }

    @Test
    @DisplayName("CRUD-01: Ciclo completo Equipos (Create con alias, Read, Update, Baja lógica, bloqueo con partido activo, Reactivación)")
    void testCrudCompletoEquipos() throws Exception {
        String s = sufijo();
        Usuario admin = crearUsuario("eq_adm_" + s, TiposDatos.Rol.ADMINISTRADOR);
        Usuario cap1 = crearUsuario("eq_cap1_" + s, TiposDatos.Rol.CAPITAN);
        Usuario cap2 = crearUsuario("eq_cap2_" + s, TiposDatos.Rol.CAPITAN);
        Categoria cat = categoriaDAO.buscarTodas().get(0);
        List<Cancha> canchas = canchaDAO.buscarTodas();
        Cancha c1 = canchas.get(0);

        // Create con alias
        String resCreate = gestorEquipos.registrarEquipo("Club Norte " + s, "CN" + s.substring(0, 4),
                cat, "Verde", c1, cap1, admin);
        assertTrue(resCreate.startsWith("ÉXITO"), resCreate);

        List<Equipo> lista = gestorEquipos.listarEquiposParaGestion(admin);
        Equipo creado = lista.stream().filter(e -> e.getNombre().equals("Club Norte " + s)).findFirst().orElse(null);
        assertNotNull(creado);
        assertEquals("CN" + s.substring(0, 4), creado.getAlias());
        assertTrue(creado.isActivo());

        // Update completo (nombre, alias, color, capitán, sin sede)
        String resUpd = gestorEquipos.actualizarEquipo(creado.getIdEquipo(), "Club Norte Edit " + s, "CNE",
                cat, "Azul Oscuro", null, cap2, admin);
        assertTrue(resUpd.startsWith("ÉXITO"), resUpd);

        Equipo reabierto = equipoDAO.buscarPorId(creado.getIdEquipo());
        assertNotNull(reabierto);
        assertEquals("Club Norte Edit " + s, reabierto.getNombre());
        assertEquals("CNE", reabierto.getAlias());
        assertEquals("Azul Oscuro", reabierto.getColorCamiseta());
        assertEquals(cap2.getId(), reabierto.getCapitan().getId());
        assertNull(reabierto.getSedeHabitual());

        // Delete lógico y verificación de filtrado en altas/programación
        String resBaja = gestorEquipos.cambiarEstadoEquipo(creado.getIdEquipo(), false, admin);
        assertTrue(resBaja.startsWith("ÉXITO"), resBaja);
        assertFalse(equipoDAO.buscarPorId(creado.getIdEquipo()).isActivo());
        assertFalse(GestorEquipos.getEquiposRegistrados().stream()
                .anyMatch(e -> e.getIdEquipo().equals(creado.getIdEquipo())),
                "Equipo inactivo no debe aparecer en listado operativo de programación/jugadores.");

        // Reactivación y protección con partido programado
        assertTrue(gestorEquipos.cambiarEstadoEquipo(creado.getIdEquipo(), true, admin).startsWith("ÉXITO"));
        Equipo otro = new EquipoService().registrarEquipo("Club Rival " + s, "RIV", cat.getId(), "Blanco", c1, cap1.getId(), admin);
        Torneo t = torneoService.crearTorneo("Torneo Eq " + s, "LIGA", 4, new Date(), admin);
        Fase f = torneoService.listarFasesDeTorneo(t.getIdTorneo()).get(0);
        partidoService.programarPartido(f.getIdFase(), creado.getIdEquipo(), otro.getIdEquipo(),
                c1.getIdCancha(), null, new Date(System.currentTimeMillis() + 86400000L), 90, admin);

        String bajaConPartido = gestorEquipos.cambiarEstadoEquipo(creado.getIdEquipo(), false, admin);
        assertTrue(bajaConPartido.startsWith("ERROR"), "No debe dar de baja un equipo con partidos pendientes: " + bajaConPartido);
    }

    @Test
    @DisplayName("CRUD-02: Ciclo completo Canchas (Create, Read, Update completo, Inhabilitación y Habilitación)")
    void testCrudCompletoCanchas() throws Exception {
        String s = sufijo();
        Usuario org = crearUsuario("can_org_" + s, TiposDatos.Rol.ORGANIZADOR);

        assertTrue(gestorCanchas.registrarCancha("Predio Alfa " + s, "Calle 10", "Césped Natural", org).startsWith("ÉXITO"));
        List<Cancha> todas = gestorCanchas.listarCanchasParaGestion(org);
        Cancha cancha = todas.stream().filter(c -> c.getNombre().equals("Predio Alfa " + s)).findFirst().orElse(null);
        assertNotNull(cancha);

        // Update completo
        String resUpd = gestorCanchas.actualizarCancha(cancha.getIdCancha(), "Predio Alfa Renovado " + s,
                "Av. Costanera 999", "Césped Sintético", org);
        assertTrue(resUpd.startsWith("ÉXITO"), resUpd);
        Cancha reabierta = canchaDAO.buscarPorId(cancha.getIdCancha());
        assertEquals("Predio Alfa Renovado " + s, reabierta.getNombre());
        assertEquals("Av. Costanera 999", reabierta.getDireccion());
        assertEquals("Césped Sintético", reabierta.getTipoSuperficie());

        // Soft delete / habilitar
        assertTrue(gestorCanchas.cambiarEstado(cancha.getIdCancha(), false, org).startsWith("ÉXITO"));
        assertFalse(canchaDAO.buscarPorId(cancha.getIdCancha()).isActiva());
        assertFalse(GestorCanchas.getCanchasRegistradas().stream()
                .anyMatch(c -> c.getIdCancha().equals(cancha.getIdCancha())));

        assertTrue(gestorCanchas.cambiarEstado(cancha.getIdCancha(), true, org).startsWith("ÉXITO"));
        assertTrue(canchaDAO.buscarPorId(cancha.getIdCancha()).isActiva());
    }

    @Test
    @DisplayName("CRUD-03: Ciclo completo Torneos y Fases (Create, Update, Editar/Quitar Fase, Reglas EN_CURSO, Cancelación CANCELADO)")
    void testCrudCompletoTorneosYFases() throws Exception {
        String s = sufijo();
        Usuario org = crearUsuario("tor_org_" + s, TiposDatos.Rol.ORGANIZADOR);

        Torneo t = torneoService.crearTorneo("Z_Copa Primavera " + s, "LIGA", 6, new Date(), org);
        assertEquals("CONFIGURACION", t.getEstado());

        // Update en CONFIGURACION (incluye cambio formato y cupo)
        Torneo actualizado = torneoService.actualizarTorneo(t.getIdTorneo(), "Z_Copa Primavera Edit " + s,
                "GRUPOS", 8, new Date(), org);
        assertEquals("GRUPOS", actualizado.getFormato().name());
        assertEquals(8, actualizado.getCupoEquipos());

        // Fases: agregar, editar y quitar fase sin uso
        Fase f2 = torneoService.agregarFase(t.getIdTorneo(), "Cuartos " + s, 2, true, org);
        Fase f2Edit = torneoService.actualizarFase(f2.getIdFase(), "Semifinales " + s, 2, false, org);
        assertEquals("Semifinales " + s, f2Edit.getNombre());
        assertFalse(f2Edit.isLimpiaAmarillas());
        torneoService.eliminarFase(f2.getIdFase(), org);
        assertEquals(1, faseDAO.buscarPorTorneo(t.getIdTorneo()).size());

        // En EN_CURSO no puede cambiar formato ni reducir cupo ni retroceder a CONFIGURACION
        torneoService.actualizarEstadoTorneo(t.getIdTorneo(), "EN_CURSO", org);
        assertThrows(TorneoExcepcion.class,
                () -> torneoService.actualizarTorneo(t.getIdTorneo(), "Z_Copa Primavera Edit " + s, "LIGA", 8, new Date(), org));
        assertThrows(TorneoExcepcion.class,
                () -> torneoService.actualizarTorneo(t.getIdTorneo(), "Z_Copa Primavera Edit " + s, "GRUPOS", 4, new Date(), org));
        assertThrows(TorneoExcepcion.class,
                () -> torneoService.actualizarEstadoTorneo(t.getIdTorneo(), "CONFIGURACION", org));

        // Baja lógica CANCELADO (irreversible)
        torneoService.cancelarTorneo(t.getIdTorneo(), org);
        assertEquals("CANCELADO", torneoDAO.buscarPorId(t.getIdTorneo()).getEstado());
        assertThrows(TorneoExcepcion.class,
                () -> torneoService.actualizarEstadoTorneo(t.getIdTorneo(), "EN_CURSO", org));
    }

    @Test
    @DisplayName("CRUD-04: Ciclo completo Usuarios y Árbitros (Alta, Edición, Cambio Rol/Perfil, Reset Password, Last-Admin & Self-Lockout Guard)")
    void testCrudCompletoUsuarios() throws Exception {
        String s = sufijo();
        Usuario admin1 = crearUsuario("usr_adm1_" + s, TiposDatos.Rol.ADMINISTRADOR);

        // Alta de árbitro desde GestorUsuarios con categoría y teléfono reales (sin hardcodeo)
        String resArb = gestorUsuarios.registrarUsuario("usr_arb_" + s, "ClaveInicial123!", "arb_" + s + "@t.com",
                "Pablo", "Lunati", TiposDatos.Rol.ARBITRO, "Primera A", "3764998877", admin1);
        assertTrue(resArb.startsWith("ÉXITO"), resArb);

        Usuario arb = usuarioDAO.buscarPorUsuario("usr_arb_" + s);
        assertNotNull(arb);
        Arbitro detalleArb = gestorUsuarios.obtenerDetalleArbitro(arb.getId(), admin1);
        assertNotNull(detalleArb);
        assertEquals("Primera A", detalleArb.getCategoriaArbitral());
        assertEquals("3764998877", detalleArb.getTelefono());

        // Update de usuario y datos arbitrales
        String resEdit = gestorUsuarios.actualizarUsuario(arb.getId(), "arb_edit_" + s + "@t.com",
                "Pablo E.", "Lunati", TiposDatos.Rol.ARBITRO, "Internacional FIFA", "3764112233", admin1);
        assertTrue(resEdit.startsWith("ÉXITO"), resEdit);
        Arbitro detalleEdit = gestorUsuarios.obtenerDetalleArbitro(arb.getId(), admin1);
        assertEquals("Internacional FIFA", detalleEdit.getCategoriaArbitral());
        assertEquals("3764112233", detalleEdit.getTelefono());

        // Restablecimiento seguro de contraseña: la anterior deja de funcionar y la nueva autentica
        String resPass = gestorUsuarios.restablecerContrasena(arb.getId(), "NuevaClave456!", admin1);
        assertTrue(resPass.startsWith("ÉXITO"), resPass);
        assertNull(gestorUsuarios.autenticarUsuario(arb.getNombreUsuario(), "ClaveInicial123!"));
        assertNotNull(gestorUsuarios.autenticarUsuario(arb.getNombreUsuario(), "NuevaClave456!"));

        // Guardias: admin no puede auto-desactivarse ni auto-degradarse
        String selfDeact = gestorUsuarios.cambiarEstadoCuenta(admin1.getId(), false, admin1);
        assertTrue(selfDeact.startsWith("ERROR"), selfDeact);
        String selfDemote = gestorUsuarios.actualizarUsuario(admin1.getId(), admin1.getMail(),
                admin1.getNombre(), admin1.getApellido(), TiposDatos.Rol.ORGANIZADOR, null, null, admin1);
        assertTrue(selfDemote.startsWith("ERROR"), selfDemote);

        // Desactivar y reactivar árbitro
        assertTrue(gestorUsuarios.cambiarEstadoCuenta(arb.getId(), false, admin1).startsWith("ÉXITO"));
        assertNull(gestorUsuarios.autenticarUsuario(arb.getNombreUsuario(), "NuevaClave456!"));
        assertTrue(gestorUsuarios.cambiarEstadoCuenta(arb.getId(), true, admin1).startsWith("ÉXITO"));
        assertNotNull(gestorUsuarios.autenticarUsuario(arb.getNombreUsuario(), "NuevaClave456!"));
    }
}
