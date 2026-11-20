package com.gestionamateur.tpi.progav.servicio;

import com.gestionamateur.tpi.progav.controlador.GestorPartidos;
import com.gestionamateur.tpi.progav.dao.ArbitroDAO;
import com.gestionamateur.tpi.progav.dao.CanchaDAO;
import com.gestionamateur.tpi.progav.dao.CategoriaDAO;
import com.gestionamateur.tpi.progav.dao.ConfiguracionDAO;
import com.gestionamateur.tpi.progav.dao.ConexionBD;
import com.gestionamateur.tpi.progav.dao.ControlEjecucionIntegracion;
import com.gestionamateur.tpi.progav.dao.EquipoDAO;
import com.gestionamateur.tpi.progav.dao.FaseDAO;
import com.gestionamateur.tpi.progav.dao.JugadorDAO;
import com.gestionamateur.tpi.progav.dao.PartidoDAO;
import com.gestionamateur.tpi.progav.dao.SancionDAO;
import com.gestionamateur.tpi.progav.dao.UsuarioDAO;
import com.gestionamateur.tpi.progav.modelo.Arbitro;
import com.gestionamateur.tpi.progav.modelo.Cancha;
import com.gestionamateur.tpi.progav.modelo.Categoria;
import com.gestionamateur.tpi.progav.modelo.Cronologia;
import com.gestionamateur.tpi.progav.modelo.Equipo;
import com.gestionamateur.tpi.progav.modelo.Fase;
import com.gestionamateur.tpi.progav.modelo.Jugador;
import com.gestionamateur.tpi.progav.modelo.Partido;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.Torneo;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.vista.FrmCredencialJugador;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regresiones de los defectos corregidos en Release 1.
 *
 * <p>Cada prueba falla contra el codigo original y pasa contra el codigo corregido.
 * Se ejecuta exclusivamente contra la base de pruebas declarada en TPI_TEST_DB_URL.
 */
@DisplayName("Regresiones de los defectos corregidos (DEF-001, DEF-003, DEF-009, DEF-011, DEF-014)")
public class DefectosCorregidosIT {

    private static Connection conn;
    private static UsuarioDAO usuarioDAO;
    private static ArbitroDAO arbitroDAO;
    private static CanchaDAO canchaDAO;
    private static CategoriaDAO categoriaDAO;
    private static EquipoDAO equipoDAO;
    private static JugadorDAO jugadorDAO;
    private static SancionDAO sancionDAO;
    private static PartidoDAO partidoDAO;
    private static FaseDAO faseDAO;
    private static AuthService authService;

    @BeforeAll
    static void setUp() throws SQLException {
        ControlEjecucionIntegracion.exigirHabilitacionExplicita("DefectosCorregidosIT");
        // DEF-004: la suite verifica su base de destino antes de operar.
        ConexionBD.activarModoPruebas();
        ConexionBD.validarDestinoDePruebas();
        conn = ConexionBD.obtenerConexionTest();
        usuarioDAO = new UsuarioDAO();
        arbitroDAO = new ArbitroDAO();
        canchaDAO = new CanchaDAO();
        categoriaDAO = new CategoriaDAO();
        equipoDAO = new EquipoDAO();
        jugadorDAO = new JugadorDAO();
        sancionDAO = new SancionDAO();
        partidoDAO = new PartidoDAO();
        faseDAO = new FaseDAO();
        authService = new AuthService();
    }

    @AfterAll
    static void restaurarModoConexion() throws SQLException {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } finally {
            ConexionBD.restablecerModoPorDefecto();
        }
    }

    private String sufijo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Usuario crearUsuario(String usuario, TiposDatos.Rol rol) throws Exception {
        return usuarioDAO.buscarPorId(usuarioDAO.insertar(
                new Usuario(usuario, "ClaveSegura123!", usuario + "@test.local", "Nombre", "Apellido", rol),
                AuthService.hashearBCrypt("ClaveSegura123!"), idRol(rol)));
    }

    private int idRol(TiposDatos.Rol rol) {
        switch (rol) {
            case ADMINISTRADOR: return 1;
            case ORGANIZADOR: return 2;
            case ARBITRO: return 3;
            case CAPITAN: return 4;
            default: return 5;
        }
    }

    // ==================================================================
    // DEF-001: control de pertenencia de recursos al editar un equipo
    // ==================================================================

    @Test
    @DisplayName("DEF-001: Un capitan no puede editar ni tomar el control del equipo de otro capitan")
    void testCapitanNoPuedeEditarEquipoAjeno() throws Exception {
        String s = sufijo();
        Usuario admin = crearUsuario("def001_adm_" + s, TiposDatos.Rol.ADMINISTRADOR);
        Usuario cap1 = crearUsuario("def001_cap1_" + s, TiposDatos.Rol.CAPITAN);
        Usuario cap2 = crearUsuario("def001_cap2_" + s, TiposDatos.Rol.CAPITAN);

        Categoria cat = categoriaDAO.buscarPorId(2L);
        Cancha cancha = new Cancha("Cancha Def001 " + s, "Calle 1", "Sintetico");
        canchaDAO.insertar(cancha);

        EquipoService equipoService = new EquipoService();
        // Equipo de cap2
        Equipo ajeno = equipoService.registrarEquipo("Eq Ajeno " + s, "AJ", cat.getId(), "Rojo", cancha,
                cap2.getId(), admin);
        long capitanAjenoAntes = capitanEnBD(ajeno.getIdEquipo());

        // cap1 intenta editar el equipo de cap2 designandose a si mismo como capitan.
        TorneoExcepcion ex = assertThrows(TorneoExcepcion.class, () ->
                equipoService.actualizarEquipo(ajeno.getIdEquipo(), "Eq Secuestrado " + s, "AJ2", cat.getId(),
                        "Azul", cap1.getId(), cancha, cap1));
        assertTrue(ex.getMessage().contains("capit") || ex.getMessage().contains("capit"),
                "El mensaje debe indicar el problema de pertenencia: " + ex.getMessage());

        // El equipo NO debe cambiar de capitan ni de nombre.
        assertEquals(capitanAjenoAntes, capitanEnBD(ajeno.getIdEquipo()),
                "El capitan del equipo ajeno no debe cambiar.");
        assertEquals("Eq Ajeno " + s, nombreEnBD(ajeno.getIdEquipo()),
                "El nombre del equipo ajeno no debe cambiar.");

        // El capitan legitimo SI puede editar su propio equipo.
        assertDoesNotThrow(() ->
                equipoService.actualizarEquipo(ajeno.getIdEquipo(), "Eq Ajeno Editado " + s, "AJ", cat.getId(),
                        "Verde", cap2.getId(), cancha, cap2));
        assertEquals("Eq Ajeno Editado " + s, nombreEnBD(ajeno.getIdEquipo()));
    }

    @Test
    @DisplayName("DEF-005: La edicion de un equipo preserva el alias que la interfaz no permite modificar")
    void testEdicionPreservaAlias() throws Exception {
        String s = sufijo();
        Usuario admin = crearUsuario("def005_adm_" + s, TiposDatos.Rol.ADMINISTRADOR);
        Usuario cap = crearUsuario("def005_cap_" + s, TiposDatos.Rol.CAPITAN);
        Categoria cat = categoriaDAO.buscarPorId(2L);
        Cancha cancha = new Cancha("Cancha Def005 " + s, "Calle 2", "Sintetico");
        canchaDAO.insertar(cancha);

        EquipoService equipoService = new EquipoService();
        Equipo equipo = equipoService.registrarEquipo("Eq Alias " + s, "ALIAS-" + s, cat.getId(), "Rojo",
                cancha, cap.getId(), admin);
        String aliasInicial = aliasEnBD(equipo.getIdEquipo());
        assertEquals("ALIAS-" + s, aliasInicial);

        // Edicion desde el controlador: no envia alias (la interfaz no tiene el campo).
        new com.gestionamateur.tpi.progav.controlador.GestorEquipos().actualizarEquipo(
                equipo.getIdEquipo(), "Eq Alias Editado " + s, cat, "Verde", cancha, cap, admin);

        assertEquals(aliasInicial, aliasEnBD(equipo.getIdEquipo()),
                "El alias no debe eliminarse al editar el equipo.");
    }

    // ==================================================================
    // DEF-003: la disponibilidad arbitral falla de forma cerrada
    // ==================================================================

    @Test
    @DisplayName("DEF-003: Si no se puede verificar la disponibilidad de arbitros no se asigna ninguno")
    void testDisponibilidadArbitralFallaCerrada() throws Exception {
        GestorPartidos gestor = new GestorPartidos();
        Field campoDao = GestorPartidos.class.getDeclaredField("partidoDAO");
        campoDao.setAccessible(true);
        campoDao.set(gestor, new PartidoDAO() {
            @Override
            public List<Partido> buscarPendientesDeFinalizacion() throws SQLException {
                throw new SQLException("fallo simulado de la consulta de disponibilidad");
            }
        });

        MethodCall call = new MethodCall(gestor, "buscarArbitroDisponible", Date.class, new Date());
        assertTrue(call.lanzoExcepcion(), "La consulta de disponibilidad fallida debe propagar el error.");
        assertTrue(call.mensaje().contains("disponibilidad"),
                "El mensaje debe indicar que no se pudo verificar la disponibilidad: " + call.mensaje());
    }

    // ==================================================================
    // DEF-009: no se programa en una cancha no habilitada
    // ==================================================================

    @Test
    @DisplayName("DEF-009: La programacion en una cancha inhabilitada es rechazada por el servicio")
    void testProgramacionEnCanchaInhabilitada() throws Exception {
        String s = sufijo();
        Usuario admin = crearUsuario("def009_adm_" + s, TiposDatos.Rol.ADMINISTRADOR);
        Usuario cap = crearUsuario("def009_cap_" + s, TiposDatos.Rol.CAPITAN);
        Categoria cat = categoriaDAO.buscarPorId(2L);
        Cancha cancha = new Cancha("Cancha Def009 " + s, "Calle 3", "Sintetico");
        Long idCancha = canchaDAO.insertar(cancha);
        Cancha inactiva = canchaDAO.buscarPorId(idCancha);
        inactiva.setActiva(false);
        try (Connection c = ConexionBD.obtenerConexionTest()) {
            canchaDAO.actualizarActiva(c, idCancha, false);
        }

        EquipoService equipoService = new EquipoService();
        Equipo e1 = equipoService.registrarEquipo("Eq Def009 A " + s, "A", cat.getId(), "Rojo", inactiva,
                cap.getId(), admin);
        Equipo e2 = equipoService.registrarEquipo("Eq Def009 B " + s, "B", cat.getId(), "Azul", inactiva,
                cap.getId(), admin);

        TorneoService torneoService = new TorneoService();
        Torneo torneo = torneoService.crearTorneo("Torneo Def009 " + s, "LIGA", 4, new Date(), admin);
        Fase fase = torneoService.listarFasesDeTorneo(torneo.getIdTorneo()).get(0);

        PartidoService partidoService = new PartidoService();
        TorneoExcepcion ex = assertThrows(TorneoExcepcion.class, () ->
                partidoService.programarPartido(fase.getIdFase(), e1.getIdEquipo(), e2.getIdEquipo(), idCancha,
                        null, new Date(System.currentTimeMillis() + 86400000L), 90, admin));
        assertTrue(ex.getMessage().contains("habilitada") || ex.getMessage().contains("habilitada"),
                "El mensaje debe indicar que la sede no esta habilitada: " + ex.getMessage());
    }

    // ==================================================================
    // DEF-011: los parametros de configuracion no admiten valores negativos
    // ==================================================================

    @Test
    @DisplayName("DEF-011: La capa de persistencia rechaza valores negativos en los parametros que no los admiten")
    void testConfiguracionRechazaValoresNegativos() throws Exception {
        String s = sufijo();
        ConfiguracionDAO dao = new ConfiguracionDAO();
        String clave = "EDAD_MINIMA_GENERAL";
        int original = dao.obtenerValorInt(clave, 16);
        try {
            assertThrows(SQLException.class, () -> dao.actualizarValorInt(clave, -1),
                    "Un valor negativo debe rechazarse en la capa de persistencia.");
            assertEquals(original, dao.obtenerValorInt(clave, 16),
                    "El valor original no debe alterarse tras el rechazo.");
            // Una clave no restringida si admite el valor (contrato propio de la clave).
            assertDoesNotThrow(() -> dao.actualizarValorInt("CLAVE_DE_PRUEBA_LIBRE_" + s, -5));
        } finally {
            try (Connection c = ConexionBD.obtenerConexionTest();
                 java.sql.PreparedStatement ps = c.prepareStatement("DELETE FROM configuraciones WHERE clave LIKE ?")) {
                ps.setString(1, "CLAVE_DE_PRUEBA_LIBRE_%");
                ps.executeUpdate();
            }
        }
    }

    // ==================================================================
    // DEF-014: la sancion pendiente no se cumple sola
    // ==================================================================

    @Test
    @DisplayName("DEF-014: Una sancion PENDIENTE no consume fechas de suspension hasta ser oficializada")
    void testSancionPendienteNoSeCumpleSola() throws Exception {
        String s = sufijo();
        Usuario admin = crearUsuario("def014_adm_" + s, TiposDatos.Rol.ADMINISTRADOR);
        Usuario cap = crearUsuario("def014_cap_" + s, TiposDatos.Rol.CAPITAN);
        Arbitro arbitro = new Arbitro("def014_arb_" + s, "ClaveSegura123!", "def014_arb_" + s + "@test.local",
                "Arb", "Test", "Nacional", "1122334455");
        arbitro.setId(arbitroDAO.insertar(arbitro, AuthService.hashearBCrypt("ClaveSegura123!")));

        Categoria cat = categoriaDAO.buscarPorId(2L);
        Cancha cancha = new Cancha("Cancha Def014 " + s, "Calle 4", "Sintetico");
        canchaDAO.insertar(cancha);

        EquipoService equipoService = new EquipoService();
        Equipo e1 = equipoService.registrarEquipo("Eq Def014 A " + s, "A", cat.getId(), "Rojo", cancha,
                cap.getId(), admin);
        Equipo e2 = equipoService.registrarEquipo("Eq Def014 B " + s, "B", cat.getId(), "Azul", cancha,
                cap.getId(), admin);

        TorneoService torneoService = new TorneoService();
        Torneo torneo = torneoService.crearTorneo("Torneo Def014 " + s, "LIGA", 4, new Date(), admin);
        Fase fase = torneoService.listarFasesDeTorneo(torneo.getIdTorneo()).get(0);

        // Jugador con equipo, para que la cronologia le adjudique la roja.
        Date nac = Date.from(java.time.LocalDate.of(1998, 5, 10)
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        Jugador jugador = new Jugador(e1.getIdEquipo(), "Jug Def014 " + s, 51000000 + (int) (Math.random() * 900000),
                nac, "Delantero", TiposDatos.EstadoJugador.ACTIVO);
        jugador.setId(jugadorDAO.insertar(jugador, e1.getIdEquipo()));

        PartidoService partidoService = new PartidoService();
        Partido p1 = partidoService.programarPartido(fase.getIdFase(), e1.getIdEquipo(), e2.getIdEquipo(),
                cancha.getIdCancha(), arbitro.getId(), new Date(System.currentTimeMillis() - 7200000L), 90, admin);
        partidoService.registrarIncidencia(p1.getIdPartido(), jugador.getId(), 30,
                TiposDatos.TipoAccion.TARJETA_ROJA, "DIRECTA", "Roja directa de prueba", admin);
        partidoService.cerrarActaPartido(p1.getIdPartido(), new Date(), 0, 0, admin);

        // La sancion por roja directa queda PENDIENTE de fallo del tribunal.
        String estadoTrasRoja = estadoSancion(jugador.getId(), "ROJA_DIRECTA");
        assertEquals("PENDIENTE", estadoTrasRoja,
                "La sancion por roja directa debe quedar PENDIENTE hasta el fallo del tribunal.");

        // Partido posterior del mismo equipo: la sancion NO debe consumirse sola.
        Partido p2 = partidoService.programarPartido(fase.getIdFase(), e1.getIdEquipo(), e2.getIdEquipo(),
                cancha.getIdCancha(), arbitro.getId(), new Date(System.currentTimeMillis() - 3600000L), 90, admin);
        partidoService.cerrarActaPartido(p2.getIdPartido(), new Date(), 0, 0, admin);

        assertEquals("PENDIENTE", estadoSancion(jugador.getId(), "ROJA_DIRECTA"),
                "La sancion PENDIENTE no debe cumplirse por el mero hecho de disputar otro partido.");
        assertEquals("SUSPENDIDO", estadoJugador(jugador.getId()),
                "El jugador debe permanecer suspendido hasta que el tribunal se expida.");

        // Fallo del tribunal: la sancion se oficializa y pasa a ser exigible.
        Long idSancion = idSancion(jugador.getId(), "ROJA_DIRECTA");
        DisciplinaService disciplinaService = new DisciplinaService();
        disciplinaService.emitirFalloTribunal(idSancion, 1, "Fallo formal por conducta antideportiva", "GRAVE", admin);
        assertEquals("ACTIVA", estadoSancion(jugador.getId(), "ROJA_DIRECTA"),
                "Tras el fallo del tribunal la sancion debe quedar ACTIVA.");

        // Partido siguiente: ahora si se consume la fecha de suspension.
        Partido p3 = partidoService.programarPartido(fase.getIdFase(), e1.getIdEquipo(), e2.getIdEquipo(),
                cancha.getIdCancha(), arbitro.getId(), new Date(System.currentTimeMillis() - 1800000L), 90, admin);
        partidoService.cerrarActaPartido(p3.getIdPartido(), new Date(), 0, 0, admin);

        assertEquals("CUMPLIDA", estadoSancion(jugador.getId(), "ROJA_DIRECTA"),
                "La sancion oficializada debe cumplirse al alcanzar la cantidad de fechas.");
    }

    // ==================================================================
    // DEF-010: la credencial solo se ofrece para jugadores habilitados
    // ==================================================================

    @Test
    @DisplayName("DEF-010: La credencial solo se ofrece para jugadores en estado ACTIVO")
    void testCredencialSoloParaJugadoresHabilitados() throws Exception {
        List<Jugador> todos = jugadorDAO.buscarTodos();
        List<Jugador> habilitados = new ArrayList<>();
        for (Jugador j : todos) {
            if (j.getEstado() == TiposDatos.EstadoJugador.ACTIVO) {
                habilitados.add(j);
            }
        }
        List<Jugador> filtrados = FrmCredencialJugador.jugadoresHabilitadosParaCredencial(todos);
        assertNotNull(filtrados);
        assertTrue(filtrados.size() <= habilitados.size(),
                "El listado filtrado no puede ser mayor que el de habilitados.");
        for (Jugador j : filtrados) {
            assertEquals(TiposDatos.EstadoJugador.ACTIVO, j.getEstado(),
                    "La credencial no debe ofrecerse para un jugador no habilitado.");
        }
        // Si la base tiene jugadores no habilitados, deben quedar fuera del listado.
        boolean hayNoHabilitados = todos.size() > habilitados.size();
        if (hayNoHabilitados) {
            assertEquals(habilitados.size(), filtrados.size(),
                    "El listado de la credencial debe excluir a los jugadores no habilitados.");
        }
    }

    // ==================================================================
    // DEF-002: el calculo de edad con fechas JDBC
    // ==================================================================

    @Test
    @DisplayName("DEF-002: El calculo de edad admite fechas JDBC (java.sql.Date) sin lanzar excepcion")
    void testCalculoEdadConFechaSql() {
        JugadorService jugadorService = new JugadorService();
        java.sql.Date nacimiento = java.sql.Date.valueOf("2008-05-10");
        java.sql.Date referencia = java.sql.Date.valueOf("2026-06-01");
        int edad = jugadorService.calcularEdad(nacimiento, referencia);
        assertEquals(18, edad, "La edad debe calcularse sobre la fecha local, sin desplazamiento de zona horaria.");

        // Cumpleanio todavia no ocurrido en el anio de referencia.
        java.sql.Date ref2 = java.sql.Date.valueOf("2026-05-09");
        assertEquals(17, jugadorService.calcularEdad(nacimiento, ref2),
                "Si el cumpleanos no ocurrio todavia, la edad debe ser la del anio anterior.");

        // Fecha nula: 0, sin excepcion.
        assertEquals(0, jugadorService.calcularEdad(null, referencia));
        assertEquals(0, jugadorService.calcularEdad(nacimiento, null));
    }

    // ==================================================================
    // Utilidades
    // ==================================================================

    private long capitanEnBD(Long idEquipo) throws SQLException {
        try (Connection c = ConexionBD.obtenerConexionTest();
             java.sql.PreparedStatement ps = c.prepareStatement("SELECT id_capitan FROM equipos WHERE id_equipo = ?")) {
            ps.setLong(1, idEquipo);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private String nombreEnBD(Long idEquipo) throws SQLException {
        try (Connection c = ConexionBD.obtenerConexionTest();
             java.sql.PreparedStatement ps = c.prepareStatement("SELECT nombre FROM equipos WHERE id_equipo = ?")) {
            ps.setLong(1, idEquipo);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    private String aliasEnBD(Long idEquipo) throws SQLException {
        try (Connection c = ConexionBD.obtenerConexionTest();
             java.sql.PreparedStatement ps = c.prepareStatement("SELECT alias FROM equipos WHERE id_equipo = ?")) {
            ps.setLong(1, idEquipo);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    private String estadoSancion(Long idJugador, String tipoOrigen) throws SQLException {
        try (Connection c = ConexionBD.obtenerConexionTest();
             java.sql.PreparedStatement ps = c.prepareStatement(
                     "SELECT estado FROM sanciones WHERE id_jugador = ? AND tipo_origen = ? ORDER BY id_sancion DESC LIMIT 1")) {
            ps.setLong(1, idJugador);
            ps.setString(2, tipoOrigen);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private Long idSancion(Long idJugador, String tipoOrigen) throws SQLException {
        try (Connection c = ConexionBD.obtenerConexionTest();
             java.sql.PreparedStatement ps = c.prepareStatement(
                     "SELECT id_sancion FROM sanciones WHERE id_jugador = ? AND tipo_origen = ? ORDER BY id_sancion DESC LIMIT 1")) {
            ps.setLong(1, idJugador);
            ps.setString(2, tipoOrigen);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    private String estadoJugador(Long idJugador) throws SQLException {
        try (Connection c = ConexionBD.obtenerConexionTest();
             java.sql.PreparedStatement ps = c.prepareStatement("SELECT estado FROM jugadores WHERE id_jugador = ?")) {
            ps.setLong(1, idJugador);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    /** Invoca por reflection un metodo privado y captura la excepcion que produce. */
    private static final class MethodCall {
        private boolean excepcion;
        private String mensaje;

        MethodCall(Object objetivo, String metodo, Class<?> tipoParametro, Object valor) {
            try {
                java.lang.reflect.Method m = objetivo.getClass().getDeclaredMethod(metodo, tipoParametro);
                m.setAccessible(true);
                m.invoke(objetivo, valor);
            } catch (java.lang.reflect.InvocationTargetException e) {
                excepcion = true;
                Throwable causa = e.getCause();
                mensaje = causa == null ? "" : String.valueOf(causa.getMessage());
            } catch (ReflectiveOperationException e) {
                excepcion = false;
                mensaje = "error de reflexion: " + e.getMessage();
            }
        }

        boolean lanzoExcepcion() {
            return excepcion;
        }

        String mensaje() {
            return mensaje;
        }
    }
}
