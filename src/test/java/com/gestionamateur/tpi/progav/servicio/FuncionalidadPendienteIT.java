package com.gestionamateur.tpi.progav.servicio;

import com.gestionamateur.tpi.progav.dao.ArbitroDAO;
import com.gestionamateur.tpi.progav.dao.CanchaDAO;
import com.gestionamateur.tpi.progav.dao.CategoriaDAO;
import com.gestionamateur.tpi.progav.dao.ConexionBD;
import com.gestionamateur.tpi.progav.dao.ControlEjecucionIntegracion;
import com.gestionamateur.tpi.progav.dao.EquipoDAO;
import com.gestionamateur.tpi.progav.dao.JugadorDAO;
import com.gestionamateur.tpi.progav.dao.UsuarioDAO;
import com.gestionamateur.tpi.progav.modelo.Arbitro;
import com.gestionamateur.tpi.progav.modelo.Cancha;
import com.gestionamateur.tpi.progav.modelo.Categoria;
import com.gestionamateur.tpi.progav.modelo.Equipo;
import com.gestionamateur.tpi.progav.modelo.Fase;
import com.gestionamateur.tpi.progav.modelo.Jugador;
import com.gestionamateur.tpi.progav.modelo.Partido;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TransporteCorreo;
import com.gestionamateur.tpi.progav.modelo.Torneo;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CU-03.2 (notificacion por correo) y CU-08 a CU-11 (consultas publicas).
 *
 * <p>Las notificaciones se prueban con un doble de transporte en memoria: no se envia ningun
 * correo real. Las consultas publicas se verifican ademas contra la regla de privacidad: ninguna
 * celda devuelta puede contener un correo electronico ni un DNI de la base.
 */
@DisplayName("Funcionalidad pendiente: CU-03.2 (correo) y CU-08 a CU-11 (consultas publicas)")
public class FuncionalidadPendienteIT {

    private static UsuarioDAO usuarioDAO;
    private static ArbitroDAO arbitroDAO;
    private static CanchaDAO canchaDAO;
    private static CategoriaDAO categoriaDAO;
    private static EquipoDAO equipoDAO;
    private static JugadorDAO jugadorDAO;
    private static AuthService authService;

    @BeforeAll
    static void setUp() throws SQLException {
        ControlEjecucionIntegracion.exigirHabilitacionExplicita("FuncionalidadPendienteIT");
        ConexionBD.activarModoPruebas();
        ConexionBD.validarDestinoDePruebas();
        usuarioDAO = new UsuarioDAO();
        arbitroDAO = new ArbitroDAO();
        canchaDAO = new CanchaDAO();
        categoriaDAO = new CategoriaDAO();
        equipoDAO = new EquipoDAO();
        jugadorDAO = new JugadorDAO();
        authService = new AuthService();
    }

    @AfterAll
    static void restaurarModoConexion() {
        ConexionBD.restablecerModoPorDefecto();
    }

    private String sufijo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Usuario crearUsuario(String usuario, TiposDatos.Rol rol) throws Exception {
        int idRol;
        switch (rol) {
            case ADMINISTRADOR: idRol = 1; break;
            case ORGANIZADOR: idRol = 2; break;
            case ARBITRO: idRol = 3; break;
            case CAPITAN: idRol = 4; break;
            default: idRol = 5;
        }
        return usuarioDAO.buscarPorId(usuarioDAO.insertar(
                new Usuario(usuario, "ClaveSegura123!", usuario + "@test.local", "Nombre", "Apellido", rol),
                AuthService.hashearBCrypt("ClaveSegura123!"), idRol));
    }

    /** Transporte de correo en memoria: registra los mensajes sin despachar nada. */
    static class TransporteFalso implements TransporteCorreo {
        final List<String[]> enviados = new ArrayList<>();
        boolean fallar;

        @Override
        public boolean enviar(String destinatario, String asunto, String cuerpo) throws Exception {
            if (fallar) {
                throw new Exception("fallo simulado del servicio de correo");
            }
            enviados.add(new String[]{destinatario, asunto, cuerpo});
            return true;
        }

        @Override
        public boolean estaConfigurado() {
            return true;
        }

        @Override
        public String descripcion() {
            return "Transporte de prueba en memoria";
        }
    }

    // ==================================================================
    // CU-03.2
    // ==================================================================

    @Test
    @DisplayName("CU-03.2: La programacion de un partido notifica a los capitanes y al arbitro")
    void testNotificacionDeProgramacion() throws Exception {
        String s = sufijo();
        Usuario admin = crearUsuario("cu032_adm_" + s, TiposDatos.Rol.ADMINISTRADOR);
        Usuario cap1 = crearUsuario("cu032_cap1_" + s, TiposDatos.Rol.CAPITAN);
        Usuario cap2 = crearUsuario("cu032_cap2_" + s, TiposDatos.Rol.CAPITAN);
        Arbitro arbitro = new Arbitro("cu032_arb_" + s, "ClaveSegura123!", "cu032_arb_" + s + "@test.local",
                "Arb", "Oficial", "Nacional", "1122334455");
        arbitro.setId(arbitroDAO.insertar(arbitro, authService.hashearBCrypt("ClaveSegura123!")));

        Categoria cat = categoriaDAO.buscarPorId(2L);
        Cancha cancha = new Cancha("Cancha Cu032 " + s, "Calle 9", "Sintetico");
        canchaDAO.insertar(cancha);

        EquipoService equipoService = new EquipoService();
        Equipo e1 = equipoService.registrarEquipo("Eq Cu032 A " + s, "A", cat.getId(), "Rojo", cancha,
                cap1.getId(), admin);
        Equipo e2 = equipoService.registrarEquipo("Eq Cu032 B " + s, "B", cat.getId(), "Azul", cancha,
                cap2.getId(), admin);

        TorneoService torneoService = new TorneoService();
        Torneo torneo = torneoService.crearTorneo("Torneo Cu032 " + s, "LIGA", 4, new Date(), admin);
        Fase fase = torneoService.listarFasesDeTorneo(torneo.getIdTorneo()).get(0);

        PartidoService partidoService = new PartidoService();
        Partido partido = partidoService.programarPartido(fase.getIdFase(), e1.getIdEquipo(), e2.getIdEquipo(),
                cancha.getIdCancha(), arbitro.getId(), new Date(System.currentTimeMillis() + 86400000L), 90, admin);

        TransporteFalso transporte = new TransporteFalso();
        NotificacionCorreoService servicio = new NotificacionCorreoService(transporte);
        NotificacionCorreoService.Resultado resultado = servicio.notificarProgramacion(partido.getIdPartido());

        assertEquals(3, resultado.destinatarios,
                "Deben notificarse los dos capitanes y el arbitro (" + resultado + ")");
        assertEquals(3, resultado.enviados, "Los tres mensajes deben despacharse (" + resultado + ")");
        assertEquals(0, resultado.fallidos, "No debe haber fallos de envio (" + resultado + ")");

        Set<String> destinatarios = new HashSet<>();
        for (String[] m : transporte.enviados) {
            destinatarios.add(m[0]);
            assertTrue(m[1].contains("Partido programado"), "El asunto debe identificar la programacion.");
            assertTrue(m[2].contains("Fecha y hora"), "El cuerpo debe incluir los datos del encuentro.");
        }
        assertTrue(destinatarios.contains(cap1.getMail()), "Debe notificarse al capitan local.");
        assertTrue(destinatarios.contains(cap2.getMail()), "Debe notificarse al capitan visitante.");
        assertTrue(destinatarios.contains(arbitro.getMail()), "Debe notificarse al arbitro asignado.");
    }

    @Test
    @DisplayName("CU-03.2: Un fallo del servicio de correo no invalida el partido ya persistido")
    void testFalloDeCorreoNoInvalidaElPartido() throws Exception {
        String s = sufijo();
        Usuario admin = crearUsuario("cu032f_adm_" + s, TiposDatos.Rol.ADMINISTRADOR);
        Usuario cap1 = crearUsuario("cu032f_cap1_" + s, TiposDatos.Rol.CAPITAN);
        Usuario cap2 = crearUsuario("cu032f_cap2_" + s, TiposDatos.Rol.CAPITAN);
        Categoria cat = categoriaDAO.buscarPorId(2L);
        Cancha cancha = new Cancha("Cancha Cu032f " + s, "Calle 10", "Sintetico");
        canchaDAO.insertar(cancha);
        EquipoService equipoService = new EquipoService();
        Equipo e1 = equipoService.registrarEquipo("Eq Cu032f A " + s, "A", cat.getId(), "Rojo", cancha,
                cap1.getId(), admin);
        Equipo e2 = equipoService.registrarEquipo("Eq Cu032f B " + s, "B", cat.getId(), "Azul", cancha,
                cap2.getId(), admin);
        TorneoService torneoService = new TorneoService();
        Torneo torneo = torneoService.crearTorneo("Torneo Cu032f " + s, "LIGA", 4, new Date(), admin);
        Fase fase = torneoService.listarFasesDeTorneo(torneo.getIdTorneo()).get(0);

        PartidoService partidoService = new PartidoService();
        Partido partido = partidoService.programarPartido(fase.getIdFase(), e1.getIdEquipo(), e2.getIdEquipo(),
                cancha.getIdCancha(), null, new Date(System.currentTimeMillis() + 86400000L), 90, admin);

        TransporteFalso transporte = new TransporteFalso();
        transporte.fallar = true;
        NotificacionCorreoService.Resultado resultado =
                new NotificacionCorreoService(transporte).notificarProgramacion(partido.getIdPartido());
        assertTrue(resultado.fallidos > 0, "El fallo del correo debe registrarse.");
        assertTrue(resultado.errores.size() >= resultado.fallidos, "Los errores deben quedar registrados.");

        // El partido debe seguir persistido y programado.
        try (Connection conn = ConexionBD.obtenerConexionTest();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT estado FROM partidos WHERE id_partido = " + partido.getIdPartido())) {
            assertTrue(rs.next(), "El partido debe seguir persistido.");
            assertEquals("PROGRAMADO", rs.getString(1),
                    "El fallo del correo no debe alterar el estado deportivo del partido.");
        }
    }

    @Test
    @DisplayName("CU-03.2: Sin transporte configurado la notificacion se omite y queda registrada")
    void testNotificacionSinTransporteConfigurado() throws Exception {
        TransporteFalso sinConfigurar = new TransporteFalso() {
            @Override
            public boolean estaConfigurado() {
                return false;
            }
        };
        NotificacionCorreoService.Resultado resultado =
                new NotificacionCorreoService(sinConfigurar).notificarProgramacion(1L);
        assertFalse(resultado.transporteConfigurado);
        assertEquals(0, resultado.enviados, "Sin transporte configurado no se despacha ningun mensaje.");
    }

    // ==================================================================
    // CU-08 a CU-11: consultas publicas y privacidad
    // ==================================================================

    @Test
    @DisplayName("CU-08 a CU-11: Las consultas publicas devuelven informacion y no exponen datos privados")
    void testConsultasPublicasYPrivacidad() throws Exception {
        ConsultaPublicaService servicio = new ConsultaPublicaService();
        if (servicio.equipos().isEmpty() || servicio.torneos().isEmpty()) {
            asegurarDatosConsultaPublica();
        }

        List<Object[]> fixture = servicio.fixture(null);
        List<Object[]> estadisticas = servicio.estadisticasDeJugadores(null);
        List<Object[]> fases = servicio.avanceDeFases(null);
        List<Object[]> equipos = servicio.equipos();
        List<Object[]> torneos = servicio.torneos();
        assertNotNull(fixture);
        assertNotNull(estadisticas);
        assertNotNull(fases);
        assertNotNull(equipos);
        assertNotNull(torneos);
        assertFalse(equipos.isEmpty(), "Debe existir al menos un equipo para la consulta publica.");
        assertFalse(torneos.isEmpty(), "Debe existir al menos un torneo para la consulta publica.");

        // Perfil publico de un equipo concreto.
        Long idEquipo = ((Number) equipos.get(0)[0]).longValue();
        List<Object[]> perfil = servicio.perfilPublicoDeEquipo(idEquipo);
        assertNotNull(perfil);
        assertEquals(1, perfil.size(), "El perfil publico debe devolver una fila por equipo.");

        // Privacidad: ninguna celda puede contener un correo electronico ni un DNI.
        Set<String> correos = correosRegistrados();
        Set<String> dnis = dnisRegistrados();
        List<Object[]> todas = new ArrayList<>();
        todas.addAll(fixture);
        todas.addAll(estadisticas);
        todas.addAll(fases);
        todas.addAll(perfil);
        for (Object[] fila : todas) {
            for (Object celda : fila) {
                if (celda == null) {
                    continue;
                }
                String texto = String.valueOf(celda);
                assertFalse(texto.contains("@"),
                        "La consulta publica no debe exponer correos electronicos: " + texto);
                for (String dni : dnis) {
                    assertFalse(texto.equals(dni),
                            "La consulta publica no debe exponer el DNI de un jugador.");
                }
                for (String correo : correos) {
                    assertFalse(texto.equals(correo),
                            "La consulta publica no debe exponer el correo de un usuario.");
                }
            }
        }
    }

    private Set<String> correosRegistrados() throws SQLException {
        Set<String> correos = new HashSet<>();
        try (Connection conn = ConexionBD.obtenerConexionTest();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT mail FROM usuarios")) {
            while (rs.next()) {
                correos.add(rs.getString(1));
            }
        }
        return correos;
    }

    private Set<String> dnisRegistrados() throws SQLException {
        Set<String> dnis = new HashSet<>();
        try (Connection conn = ConexionBD.obtenerConexionTest();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT dni::text FROM jugadores")) {
            while (rs.next()) {
                dnis.add(rs.getString(1));
            }
        }
        return dnis;
    }

    private void asegurarDatosConsultaPublica() throws Exception {
        String s = sufijo();
        Usuario admin = crearUsuario("cu_pub_adm_" + s, TiposDatos.Rol.ADMINISTRADOR);
        Usuario cap1 = crearUsuario("cu_pub_cap1_" + s, TiposDatos.Rol.CAPITAN);
        Usuario cap2 = crearUsuario("cu_pub_cap2_" + s, TiposDatos.Rol.CAPITAN);
        Categoria cat = categoriaDAO.buscarPorId(2L);
        Cancha cancha = new Cancha("Cancha Pub " + s, "Calle Pub 1", "Sintetico");
        canchaDAO.insertar(cancha);

        EquipoService equipoService = new EquipoService();
        Equipo e1 = equipoService.registrarEquipo("Eq Pub A " + s, "PA", cat.getId(), "Verde", cancha,
                cap1.getId(), admin);
        Equipo e2 = equipoService.registrarEquipo("Eq Pub B " + s, "PB", cat.getId(), "Blanco", cancha,
                cap2.getId(), admin);

        TorneoService torneoService = new TorneoService();
        Torneo torneo = torneoService.crearTorneo("Torneo Pub " + s, "LIGA", 4, new Date(), admin);
        Fase fase = torneoService.listarFasesDeTorneo(torneo.getIdTorneo()).get(0);
        new PartidoService().programarPartido(fase.getIdFase(), e1.getIdEquipo(), e2.getIdEquipo(),
                cancha.getIdCancha(), null, new Date(System.currentTimeMillis() + 86400000L), 90, admin);
    }
}
