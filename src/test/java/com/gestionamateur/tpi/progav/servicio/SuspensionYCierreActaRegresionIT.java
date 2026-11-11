package com.gestionamateur.tpi.progav.servicio;

import com.gestionamateur.tpi.progav.controlador.GestorPartidos;
import com.gestionamateur.tpi.progav.dao.*;
import com.gestionamateur.tpi.progav.modelo.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de Regresión TDD - Corrección de DEF-001 y DEF-002")
public class SuspensionYCierreActaRegresionIT {

    private static PartidoService partidoService;
    private static GestorPartidos gestorPartidos;
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
    private static MotivoDAO motivoDAO;

    @BeforeAll
    static void setUp() throws SQLException {
        assertTrue(ConexionBD.comprobarConexion(), "PostgreSQL debe estar activo en puerto 55432 y accesible");

        partidoService = new PartidoService();
        gestorPartidos = new GestorPartidos();
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
        motivoDAO = new MotivoDAO();
    }

    private String sufijo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static class FixturePartido {
        Long idPartido;
        Long idTorneo;
        Long idFase;
        Long idLocal;
        Long idVisitante;
        Long idJugadorLocal;
        Long idJugadorVisitante;
        Arbitro arbitro;
        Usuario organizador;
        Usuario capitanLocal;
    }

    private FixturePartido crearFixturePartido(String s) throws Exception {
        FixturePartido f = new FixturePartido();

        // 1. Torneo y Fase
        Torneo torneo = new Torneo("Torneo IT " + s, "LIGA", 8, new Date());
        f.idTorneo = torneoDAO.insertar(torneo);
        Fase fase = new Fase("Fase Regular " + s, 1, false);
        f.idFase = faseDAO.insertar(fase, f.idTorneo);

        // 2. Cancha y Árbitro
        Cancha cancha = new Cancha("Cancha IT " + s, "Av. Siempre Viva 123", "Césped Sintético");
        Long idCancha = canchaDAO.insertar(cancha);

        f.arbitro = new Arbitro("arb_" + s, "pass123", "arb_" + s + "@test.com", "Martín", "Árbitro", "Nacional", "3764001122");
        Long idArbitro = arbitroDAO.insertar(f.arbitro, AuthService.hashearSha256("pass123"));
        f.arbitro.setId(idArbitro);

        // 3. Operador Organizador y Capitán
        f.organizador = new Usuario("org_" + s, "pass123", "org_" + s + "@torneo.com", "Carlos", "Organizador", TiposDatos.Rol.ORGANIZADOR);
        Long idOrg = usuarioDAO.insertar(f.organizador, AuthService.hashearSha256("pass123"), 2);
        f.organizador.setId(idOrg);

        f.capitanLocal = new Usuario("cap_" + s, "pass123", "cap_" + s + "@torneo.com", "Esteban", "Capitán", TiposDatos.Rol.CAPITAN);
        Long idCap = usuarioDAO.insertar(f.capitanLocal, AuthService.hashearSha256("pass123"), 4);
        f.capitanLocal.setId(idCap);

        // 4. Equipos y Jugadores
        Categoria catLibre = categoriaDAO.buscarPorId(2L);
        Equipo eqLocal = new Equipo("Local " + s, catLibre, "Azul", cancha, f.capitanLocal);
        f.idLocal = equipoDAO.insertar(eqLocal, idCap);

        Usuario capVis = new Usuario("cap_vis_" + s, "pass123", "cap_vis_" + s + "@torneo.com", "Mateo", "Capitán", TiposDatos.Rol.CAPITAN);
        Long idCapVis = usuarioDAO.insertar(capVis, AuthService.hashearSha256("pass123"), 4);
        Equipo eqVis = new Equipo("Visitante " + s, catLibre, "Amarillo", cancha, capVis);
        f.idVisitante = equipoDAO.insertar(eqVis, idCapVis);

        int dniBase = 30000000 + (int)(Math.random() * 8000000);
        Jugador jLoc = new Jugador(f.idLocal, "Jugador Local " + s, dniBase + 1, new Date(98, 3, 10), "Delantero", TiposDatos.EstadoJugador.ACTIVO);
        f.idJugadorLocal = jugadorDAO.insertar(jLoc, f.idLocal);

        Jugador jVis = new Jugador(f.idVisitante, "Jugador Visitante " + s, dniBase + 2, new Date(97, 7, 20), "Delantero", TiposDatos.EstadoJugador.ACTIVO);
        f.idJugadorVisitante = jugadorDAO.insertar(jVis, f.idVisitante);

        // 5. Partido Programado
        Date fechaHora = new Date(System.currentTimeMillis() + 86400000L); // Mañana
        Partido p = partidoService.programarPartido(f.idFase, f.idLocal, f.idVisitante, idCancha, idArbitro, fechaHora, 90, f.organizador);
        f.idPartido = p.getIdPartido();

        return f;
    }

    private static class EstadoPartidoDB {
        String estado;
        Long idMotivo;
        String textoMotivo;
    }

    private EstadoPartidoDB consultarEstadoDirecto(Long idPartido) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexionTest();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT p.estado, p.id_motivo_suspension, m.motivo " +
                     "FROM partidos p LEFT JOIN motivos m ON p.id_motivo_suspension = m.id_motivo " +
                     "WHERE p.id_partido = ?")) {
            ps.setLong(1, idPartido);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    EstadoPartidoDB res = new EstadoPartidoDB();
                    res.estado = rs.getString("estado");
                    long idM = rs.getLong("id_motivo_suspension");
                    res.idMotivo = rs.wasNull() ? null : idM;
                    res.textoMotivo = rs.getString("motivo");
                    return res;
                }
            }
        }
        return null;
    }

    private int consultarGolesDirecto(Long idPartido, Long idEquipo) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexionTest();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT goles FROM partido_equipos WHERE id_partido = ? AND id_equipo = ?")) {
            ps.setLong(1, idPartido);
            ps.setLong(2, idEquipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("goles");
                }
            }
        }
        throw new SQLException("Registro en partido_equipos no encontrado");
    }

    // =========================================================================
    // PRUEBAS DEF-001: Persistencia de Suspensiones y Postergaciones
    // =========================================================================

    @Test
    @DisplayName("DEF-001-01: Postergación válida persiste en PostgreSQL como POSTERGADO con motivo asociado")
    void testDEF001_PostergacionValidaPersisteEnPostgreSQL() throws Exception {
        String s = sufijo();
        FixturePartido f = crearFixturePartido(s);

        String motivoTexto = "Condiciones climáticas adversas " + s;
        String resultado = gestorPartidos.procesarSuspension(f.idPartido, motivoTexto, f.organizador);

        assertTrue(resultado.startsWith("ÉXITO"), "La postergación debió ser exitosa: " + resultado);
        assertTrue(resultado.contains("pospuesto"), "Debe preservar el mensaje semántico de postergación");

        // Verificación directa en PostgreSQL con conexión fresca
        EstadoPartidoDB dbState = consultarEstadoDirecto(f.idPartido);
        assertNotNull(dbState, "El partido debe existir en PostgreSQL");
        assertEquals("POSTERGADO", dbState.estado, "El estado persistido debe ser POSTERGADO");
        assertNotNull(dbState.idMotivo, "Debe tener un motivo de suspensión/postergación asociado");
        assertEquals(motivoTexto, dbState.textoMotivo, "El texto del motivo debe coincidir");
    }

    @Test
    @DisplayName("DEF-001-02: Suspensión diferenciada persiste formalmente como SUSPENDIDO")
    void testDEF001_SuspensionDiferenciadaPersisteEstadoSuspendido() throws Exception {
        String s = sufijo();
        FixturePartido f = crearFixturePartido(s);

        String motivoTexto = "Incidentes de seguridad graves " + s;
        partidoService.suspenderPartido(f.idPartido, motivoTexto, f.organizador);

        EstadoPartidoDB dbState = consultarEstadoDirecto(f.idPartido);
        assertNotNull(dbState, "El partido debe existir en PostgreSQL");
        assertEquals("SUSPENDIDO", dbState.estado, "El estado persistido debe ser SUSPENDIDO");
        assertNotNull(dbState.idMotivo, "Debe tener un id_motivo_suspension asociado");
        assertEquals(motivoTexto, dbState.textoMotivo);
    }

    @Test
    @DisplayName("DEF-001-03: Reutilización concurrente de motivos entre partidos independientes")
    void testDEF001_ReutilizacionConcurrenteDeMotivos() throws Exception {
        String s = sufijo();
        FixturePartido f1 = crearFixturePartido(s + "_1");
        FixturePartido f2 = crearFixturePartido(s + "_2");

        String motivoCompartido = "Falta de iluminación general en la sede " + s;

        // Ejecución concurrente real con dos hilos independientes y conexiones separadas
        int numHilos = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numHilos);
        CountDownLatch latchInicio = new CountDownLatch(1);
        CountDownLatch latchFin = new CountDownLatch(numHilos);
        AtomicInteger exitos = new AtomicInteger(0);
        AtomicReference<Exception> error = new AtomicReference<>();

        executor.submit(() -> {
            try {
                latchInicio.await();
                partidoService.posponerPartido(f1.idPartido, motivoCompartido, f1.organizador);
                exitos.incrementAndGet();
            } catch (Exception e) {
                error.set(e);
            } finally {
                latchFin.countDown();
            }
        });

        executor.submit(() -> {
            try {
                latchInicio.await();
                partidoService.suspenderPartido(f2.idPartido, motivoCompartido, f2.organizador);
                exitos.incrementAndGet();
            } catch (Exception e) {
                error.set(e);
            } finally {
                latchFin.countDown();
            }
        });

        latchInicio.countDown();
        boolean completado = latchFin.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completado, "Las operaciones concurrentes debieron completarse antes del timeout");
        assertNull(error.get(), "No debió ocurrir ninguna excepción por restricción de clave duplicada");
        assertEquals(2, exitos.get(), "Ambas operaciones concurrentes debieron tener éxito");

        // Comprobación de que ambos partidos referencian exactamente el mismo id_motivo
        EstadoPartidoDB db1 = consultarEstadoDirecto(f1.idPartido);
        EstadoPartidoDB db2 = consultarEstadoDirecto(f2.idPartido);

        assertNotNull(db1);
        assertNotNull(db2);
        assertEquals("POSTERGADO", db1.estado);
        assertEquals("SUSPENDIDO", db2.estado);
        assertNotNull(db1.idMotivo);
        assertEquals(db1.idMotivo, db2.idMotivo, "Ambos partidos deben compartir el mismo registro de motivo");
    }

    @Test
    @DisplayName("DEF-001-04: Rechazo estricto de operador nulo o con rol no autorizado")
    void testDEF001_RechazoOperadorNuloONoAutorizado() throws Exception {
        String s = sufijo();
        FixturePartido f = crearFixturePartido(s);

        // 1. Operador nulo
        TorneoExcepcion exNull = assertThrows(TorneoExcepcion.class, () ->
                partidoService.posponerPartido(f.idPartido, "Motivo válido de prueba", null)
        );
        assertTrue(exNull.getMessage().contains("Acceso denegado"), "Debe rechazar operador nulo");

        // 2. Operador con rol Capitán
        TorneoExcepcion exCap = assertThrows(TorneoExcepcion.class, () ->
                partidoService.suspenderPartido(f.idPartido, "Motivo válido de prueba", f.capitanLocal)
        );
        assertTrue(exCap.getMessage().contains("Acceso denegado"), "Debe rechazar rol Capitán");

        // Comprobar que en PostgreSQL el estado sigue intacto (PROGRAMADO)
        EstadoPartidoDB db = consultarEstadoDirecto(f.idPartido);
        assertEquals("PROGRAMADO", db.estado);
        assertNull(db.idMotivo);
    }

    @Test
    @DisplayName("DEF-001-05: Rechazo de postergación o suspensión sobre partido FINALIZADO")
    void testDEF001_RechazoTransicionDesdeFinalizado() throws Exception {
        String s = sufijo();
        FixturePartido f = crearFixturePartido(s);

        // Finalizar el partido formalmente (0 - 0)
        gestorPartidos.procesarActa(f.idPartido, "0", "0", f.arbitro);

        EstadoPartidoDB previo = consultarEstadoDirecto(f.idPartido);
        assertEquals("FINALIZADO", previo.estado);

        // Intentar posponer
        TorneoExcepcion exPos = assertThrows(TorneoExcepcion.class, () ->
                partidoService.posponerPartido(f.idPartido, "Motivo extemporáneo", f.organizador)
        );
        assertTrue(exPos.getMessage().contains("finalizado"));

        // Intentar suspender
        TorneoExcepcion exSusp = assertThrows(TorneoExcepcion.class, () ->
                partidoService.suspenderPartido(f.idPartido, "Motivo extemporáneo", f.organizador)
        );
        assertTrue(exSusp.getMessage().contains("finalizado"));

        // Comprobar que permanece FINALIZADO
        EstadoPartidoDB post = consultarEstadoDirecto(f.idPartido);
        assertEquals("FINALIZADO", post.estado);
    }

    // =========================================================================
    // PRUEBAS DEF-002: Consistencia de Acta contra Cronología (Alternativa A1)
    // =========================================================================

    @Test
    @DisplayName("DEF-002-01: Cierre de acta exitoso cuando el marcador coincide con la cronología")
    void testDEF002_CierreActaConsistenteConGolesCronologia() throws Exception {
        String s = sufijo();
        FixturePartido f = crearFixturePartido(s);

        // Registrar incidencias individuales: 2 goles local, 1 gol visitante
        partidoService.registrarIncidencia(f.idPartido, f.idJugadorLocal, 15, TiposDatos.TipoAccion.GOL, null, "Gol de jugada");
        partidoService.registrarIncidencia(f.idPartido, f.idJugadorLocal, 35, TiposDatos.TipoAccion.GOL, null, "Gol de cabeza");
        partidoService.registrarIncidencia(f.idPartido, f.idJugadorVisitante, 60, TiposDatos.TipoAccion.GOL, null, "Gol de penal");

        // Cierre con marcador coincidente: 2 - 1
        String resultado = gestorPartidos.procesarActa(f.idPartido, "2", "1", f.arbitro);
        assertTrue(resultado.startsWith("ÉXITO"), "El cierre debe ser exitoso: " + resultado);

        // Verificación en PostgreSQL
        EstadoPartidoDB dbState = consultarEstadoDirecto(f.idPartido);
        assertEquals("FINALIZADO", dbState.estado, "El partido debe quedar en estado FINALIZADO");

        int golesLoc = consultarGolesDirecto(f.idPartido, f.idLocal);
        int golesVis = consultarGolesDirecto(f.idPartido, f.idVisitante);
        assertEquals(2, golesLoc, "Los goles del local deben consolidarse en 2");
        assertEquals(1, golesVis, "Los goles del visitante deben consolidarse en 1");
    }

    @Test
    @DisplayName("DEF-002-02: Cierre de acta rechazado ante discrepancia sin grabar 0-0 falso y revirtiendo transacción")
    void testDEF002_CierreActaRechazaDiscrepanciaSinGrabarCeroCero() throws Exception {
        String s = sufijo();
        FixturePartido f = crearFixturePartido(s);

        // Cronología vacía (0 - 0), pero se declara 3 - 1
        String resultado = gestorPartidos.procesarActa(f.idPartido, "3", "1", f.arbitro);

        assertTrue(resultado.startsWith("ERROR DE VALIDACIÓN"), "Debe devolver error de validación: " + resultado);
        assertTrue(resultado.contains("Discrepancia en acta") || resultado.contains("no coincide"),
                "El mensaje debe explicar la discrepancia con la cronología");

        // Verificación en PostgreSQL: NO debe estar FINALIZADO ni registrarse 0-0 falso
        EstadoPartidoDB dbState = consultarEstadoDirecto(f.idPartido);
        assertEquals("PROGRAMADO", dbState.estado, "El partido debe permanecer PROGRAMADO tras el rollback");

        int golesLoc = consultarGolesDirecto(f.idPartido, f.idLocal);
        int golesVis = consultarGolesDirecto(f.idPartido, f.idVisitante);
        assertEquals(0, golesLoc);
        assertEquals(0, golesVis);
    }

    @Test
    @DisplayName("DEF-002-03: Rechazo de parámetros nulos, vacíos, alfanuméricos o negativos en goles")
    void testDEF002_CierreActaRechazaParametrosInvalidos() throws Exception {
        String s = sufijo();
        FixturePartido f = crearFixturePartido(s);

        // Null
        String rNull = gestorPartidos.procesarActa(f.idPartido, null, "1", f.arbitro);
        assertTrue(rNull.contains("ERROR DE VALIDACIÓN"), "Debe rechazar null: " + rNull);

        // Vacío o espacios
        String rVacio = gestorPartidos.procesarActa(f.idPartido, "  ", "2", f.arbitro);
        assertTrue(rVacio.contains("ERROR DE VALIDACIÓN"), "Debe rechazar vacío: " + rVacio);

        // Alfanumérico
        String rAlfa = gestorPartidos.procesarActa(f.idPartido, "dos", "1", f.arbitro);
        assertTrue(rAlfa.contains("ERROR DE VALIDACIÓN"), "Debe rechazar no numérico: " + rAlfa);

        // Negativo
        String rNeg = gestorPartidos.procesarActa(f.idPartido, "-1", "0", f.arbitro);
        assertTrue(rNeg.contains("ERROR DE VALIDACIÓN"), "Debe rechazar negativo: " + rNeg);

        // Overflow
        String rOver = gestorPartidos.procesarActa(f.idPartido, "999999999999999999", "0", f.arbitro);
        assertTrue(rOver.contains("ERROR DE VALIDACIÓN"), "Debe rechazar overflow: " + rOver);

        // Operador nulo
        String rOpNull = gestorPartidos.procesarActa(f.idPartido, "0", "0", null);
        assertTrue(rOpNull.contains("ERROR DE VALIDACIÓN"), "Debe rechazar operador nulo: " + rOpNull);

        // Comprobar que en PostgreSQL el estado sigue intacto (PROGRAMADO)
        EstadoPartidoDB dbState = consultarEstadoDirecto(f.idPartido);
        assertEquals("PROGRAMADO", dbState.estado);
    }

    @Test
    @DisplayName("DEF-002-04: Árbitro no designado es rechazado formalmente al intentar cerrar acta")
    void testDEF002_CierreActaRechazaArbitroNoDesignado() throws Exception {
        String s = sufijo();
        FixturePartido f = crearFixturePartido(s);

        // Crear otro árbitro no asignado al encuentro
        Arbitro arbitroIntruso = new Arbitro("arb_int_" + s, "pass123", "arb_int_" + s + "@test.com", "Juan", "Intruso", "Nacional", "3764998877");
        Long idIntruso = arbitroDAO.insertar(arbitroIntruso, AuthService.hashearSha256("pass123"));
        arbitroIntruso.setId(idIntruso);

        String resultado = gestorPartidos.procesarActa(f.idPartido, "0", "0", arbitroIntruso);
        assertTrue(resultado.contains("ERROR DE VALIDACIÓN") || resultado.contains("Acceso denegado"),
                "Debe rechazar al árbitro intruso: " + resultado);
        assertTrue(resultado.contains("no es el árbitro oficial designado"),
                "Debe indicar explícitamente la falta de designación arbitral");

        EstadoPartidoDB dbState = consultarEstadoDirecto(f.idPartido);
        assertEquals("PROGRAMADO", dbState.estado);
    }
}
