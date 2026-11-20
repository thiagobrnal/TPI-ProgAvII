package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.dao.*;
import com.gestionamateur.tpi.progav.modelo.*;
import com.gestionamateur.tpi.progav.servicio.*;
import com.gestionamateur.tpi.progav.vista.FrmConsultaPublica.EstadoSeccion;
import com.gestionamateur.tpi.progav.vista.FrmConsultaPublica.SeccionPublica;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integración real CU-08..CU-11 contra PostgreSQL 17 (aislamiento fail-closed).
 */
@DisplayName("Integración PostgreSQL 17: CU-08..CU-11 DAO -> Service -> FrmConsultaPublica")
public class ConsultaPublicaIntegradaIT {

    private static final List<String> COLUMNAS_SQL_CU08 = List.of(
            "fecha_hora", "estado", "equipo_local", "equipo_visitante", "resultado", "sede", "fase", "torneo"
    );

    private static final String[] ENCABEZADOS_UI_CU08 = {
            "Fecha y hora", "Estado", "Local", "Visitante", "Resultado", "Sede", "Fase", "Torneo"
    };

    private static final Pattern PATRON_FECHA_HORA = Pattern.compile("^\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}$");
    private static final Pattern PATRON_RESULTADO = Pattern.compile("^\\d+ - \\d+$");

    @BeforeAll
    static void verificarEntornoFailClosedPostgres17() throws Exception {
        ControlEjecucionIntegracion.exigirHabilitacionExplicita("ConsultaPublicaIntegradaIT");
        ConexionBD.activarModoPruebas();
        String baseEfectiva = ConexionBD.validarDestinoDePruebas();
        assertNotNull(baseEfectiva, "Debe existir una base efectiva de pruebas");
        assertFalse(baseEfectiva.isBlank(), "El nombre de la base efectiva no puede estar vacío");
        assertFalse(ConexionBD.BASES_PROTEGIDAS.contains(baseEfectiva.toLowerCase(Locale.ROOT)),
                "La base efectiva no puede pertenecer a BASES_PROTEGIDAS: " + baseEfectiva);

        try (Connection conn = ConexionBD.obtenerConexionTest();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT version()")) {
            assertTrue(rs.next(), "PostgreSQL debe devolver version()");
            String version = rs.getString(1);
            assertNotNull(version);
            assertTrue(version.contains("PostgreSQL 17"),
                    "Se esperaba PostgreSQL 17.x, pero se obtuvo: " + version);
        }

        asegurarFixtureConsultaPublica();
    }

    private static void asegurarFixtureConsultaPublica() throws Exception {
        ConsultaPublicaService servicio = new ConsultaPublicaService();
        List<Object[]> torneos = servicio.torneos();
        List<Object[]> equipos = servicio.equipos();
        boolean faltaDatos = torneos.isEmpty() || equipos.isEmpty();
        if (!faltaDatos) {
            Long idPrimero = ((Number) torneos.get(0)[0]).longValue();
            faltaDatos = servicio.fixture(idPrimero).isEmpty()
                    || servicio.estadisticasDeJugadores(idPrimero).isEmpty()
                    || servicio.avanceDeFases(idPrimero).isEmpty();
        }
        try (Connection conn = ConexionBD.obtenerConexionTest()) {
            if (!faltaDatos) {
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT count(*) FROM canchas")) {
                    if (!rs.next() || rs.getInt(1) == 0) {
                        faltaDatos = true;
                    }
                }
            }
            if (!faltaDatos && buscarTorneoConMultiplesPartidos(conn) == null) {
                faltaDatos = true;
            }
        }
        if (!faltaDatos) {
            return;
        }

        String s = UUID.randomUUID().toString().substring(0, 8);
        UsuarioDAO usuarioDAO = new UsuarioDAO();
        ArbitroDAO arbitroDAO = new ArbitroDAO();
        CategoriaDAO categoriaDAO = new CategoriaDAO();
        CanchaDAO canchaDAO = new CanchaDAO();
        JugadorDAO jugadorDAO = new JugadorDAO();

        Usuario admin = new Usuario("cp_adm_" + s, "ClaveSegura123!", "cp_adm_" + s + "@test.local",
                "Admin", "Publica", TiposDatos.Rol.ADMINISTRADOR);
        admin.setId(usuarioDAO.insertar(admin, AuthService.hashearBCrypt("ClaveSegura123!"), 1));
        Usuario org = new Usuario("cp_org_" + s, "ClaveSegura123!", "cp_org_" + s + "@test.local",
                "Org", "Publica", TiposDatos.Rol.ORGANIZADOR);
        org.setId(usuarioDAO.insertar(org, AuthService.hashearBCrypt("ClaveSegura123!"), 2));
        Usuario cap1 = new Usuario("cp_cap1_" + s, "ClaveSegura123!", "cp_cap1_" + s + "@test.local",
                "Cap1", "Publica", TiposDatos.Rol.CAPITAN);
        cap1.setId(usuarioDAO.insertar(cap1, AuthService.hashearBCrypt("ClaveSegura123!"), 4));
        Usuario cap2 = new Usuario("cp_cap2_" + s, "ClaveSegura123!", "cp_cap2_" + s + "@test.local",
                "Cap2", "Publica", TiposDatos.Rol.CAPITAN);
        cap2.setId(usuarioDAO.insertar(cap2, AuthService.hashearBCrypt("ClaveSegura123!"), 4));
        Arbitro arb = new Arbitro("cp_arb_" + s, "ClaveSegura123!", "cp_arb_" + s + "@test.local",
                "Arb", "Publica", "Regional", "3764001122");
        arb.setId(arbitroDAO.insertar(arb, AuthService.hashearBCrypt("ClaveSegura123!")));

        Categoria cat = categoriaDAO.buscarPorId(2L);
        Cancha cancha = new Cancha("Cancha Consulta " + s, "Av. Publica 100", "Cesped Natural");
        canchaDAO.insertar(cancha);

        EquipoService equipoService = new EquipoService();
        Equipo eq1 = equipoService.registrarEquipo("Eq Consulta 1 " + s, "C1", cat.getId(), "Azul", cancha,
                cap1.getId(), admin);
        Equipo eq2 = equipoService.registrarEquipo("Eq Consulta 2 " + s, "C2", cat.getId(), "Rojo", cancha,
                cap2.getId(), admin);

        int dniBase = 31000000 + Math.abs(s.hashCode() % 8000000);
        Jugador j1 = new Jugador(eq1.getIdEquipo(), "Jug Consulta 1 " + s, dniBase,
                new Date(95, 3, 10), "Delantero", TiposDatos.EstadoJugador.ACTIVO);
        j1.setId(jugadorDAO.insertar(j1, eq1.getIdEquipo()));

        TorneoService torneoService = new TorneoService();
        // Prefijo "00 " garantiza orden inicial estable en cualquier collation para el selector CU-08..CU-11.
        Torneo torneo = torneoService.crearTorneo("00 Torneo Consulta Integrada " + s, "LIGA", 4, new Date(), admin);
        Fase fase = torneoService.listarFasesDeTorneo(torneo.getIdTorneo()).get(0);

        PartidoService partidoService = new PartidoService();
        Partido p1 = partidoService.programarPartido(fase.getIdFase(), eq1.getIdEquipo(), eq2.getIdEquipo(),
                cancha.getIdCancha(), arb.getId(), new Date(System.currentTimeMillis() - 3600000L), 90, org);
        partidoService.programarPartido(fase.getIdFase(), eq2.getIdEquipo(), eq1.getIdEquipo(),
                cancha.getIdCancha(), arb.getId(), new Date(System.currentTimeMillis() + 86400000L), 90, org);

        partidoService.registrarIncidencia(p1.getIdPartido(), j1.getId(), 15, TiposDatos.TipoAccion.GOL,
                null, "Gol semilla consulta publica", arb);
    }

    @AfterAll
    static void restaurarModoConexion() {
        ConexionBD.restablecerModoPorDefecto();
    }

    @Test
    @DisplayName("CU-08 DAO PostgreSQL 17: 8 columnas exactas, formatos, determinismo, torneo inexistente y rollback con nulos")
    void testCu08DaoPostgres17Real() throws SQLException {
        ConsultaPublicaDAO dao = new ConsultaPublicaDAO();

        try (Connection conn = ConexionBD.obtenerConexionTest()) {
            // 1. Inspeccionar ResultSetMetaData real en PostgreSQL 17
            ObservadorMetaData observador = new ObservadorMetaData(conn);
            List<Object[]> todos = dao.fixtureDeTorneo(observador.conexionObservada, null);

            assertEquals(8, observador.etiquetasColumnas.size(),
                    "ResultSetMetaData real en PostgreSQL 17 debe tener exactamente 8 columnas");
            assertFalse(observador.etiquetasColumnas.contains("id_partido"),
                    "id_partido nunca debe formar parte de la proyección CU-08");
            assertEquals(COLUMNAS_SQL_CU08, observador.etiquetasColumnas,
                    "El orden exacto de columnas SQL en PostgreSQL 17 debe respetar el contrato CU-08");

            assertFalse(todos.isEmpty(), "La base de pruebas debe contener partidos para validar formatos reales");
            for (Object[] fila : todos) {
                assertEquals(8, fila.length);
                assertNotNull(fila[0], "fecha_hora no debe ser nula en partidos válidos");
                assertTrue(PATRON_FECHA_HORA.matcher(String.valueOf(fila[0])).matches(),
                        "fecha_hora debe tener formato DD/MM/YYYY HH24:MI; actual=" + fila[0]);
                assertNotNull(fila[4], "resultado no debe ser nulo");
                assertTrue(PATRON_RESULTADO.matcher(String.valueOf(fila[4])).matches(),
                        "resultado debe tener formato N - M; actual=" + fila[4]);
            }

            // 2. Determinismo de ORDER BY p.fecha_hora ASC, p.id_partido ASC entre ejecuciones
            List<Object[]> segundaEjecucion = dao.fixtureDeTorneo(conn, null);
            assertFilasIguales(todos, segundaEjecucion);

            Long idTorneoMultiple = buscarTorneoConMultiplesPartidos(conn);
            if (idTorneoMultiple != null) {
                List<Object[]> f1 = dao.fixtureDeTorneo(conn, idTorneoMultiple);
                List<Object[]> f2 = dao.fixtureDeTorneo(conn, idTorneoMultiple);
                assertTrue(f1.size() >= 2);
                assertFilasIguales(f1, f2);
            }

            // 3. Torneo inexistente devuelve 0 filas sin error
            List<Object[]> inexistente = dao.fixtureDeTorneo(conn, -999999L);
            assertNotNull(inexistente);
            assertTrue(inexistente.isEmpty(), "Torneo inexistente debe devolver 0 filas sin error");

            // 4. Transacción temporal con rollback(): id_fase = NULL y visitante ausente sin corrimiento de columnas
            long idCancha;
            String nombreCancha;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id_cancha, nombre FROM canchas ORDER BY id_cancha ASC LIMIT 1")) {
                assertTrue(rs.next(), "Debe existir al menos una cancha en la base de pruebas");
                idCancha = rs.getLong(1);
                nombreCancha = rs.getString(2);
            }
            long idEquipoLocal;
            String nombreEquipoLocal;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id_equipo, nombre FROM equipos ORDER BY id_equipo ASC LIMIT 1")) {
                assertTrue(rs.next(), "Debe existir al menos un equipo en la base de pruebas");
                idEquipoLocal = rs.getLong(1);
                nombreEquipoLocal = rs.getString(2);
            }

            long idPartidoSinEquipos = -1L;
            long idPartidoSoloLocal = -1L;
            conn.setAutoCommit(false);
            try {
                String fechaSinEquipos;
                String fechaSoloLocal;

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO partidos (id_fase, id_cancha, fecha_hora, duracion_partido_minutos, estado) "
                                + "VALUES (NULL, ?, TIMESTAMP WITH TIME ZONE '2099-12-30 20:00:00-03', 90, 'PROGRAMADO') "
                                + "RETURNING id_partido, to_char(fecha_hora, 'DD/MM/YYYY HH24:MI')")) {
                    ps.setLong(1, idCancha);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next());
                        idPartidoSinEquipos = rs.getLong(1);
                        fechaSinEquipos = rs.getString(2);
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO partidos (id_fase, id_cancha, fecha_hora, duracion_partido_minutos, estado) "
                                + "VALUES (NULL, ?, TIMESTAMP WITH TIME ZONE '2099-12-31 21:15:00-03', 90, 'PROGRAMADO') "
                                + "RETURNING id_partido, to_char(fecha_hora, 'DD/MM/YYYY HH24:MI')")) {
                    ps.setLong(1, idCancha);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next());
                        idPartidoSoloLocal = rs.getLong(1);
                        fechaSoloLocal = rs.getString(2);
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO partido_equipos (id_partido, id_equipo, es_local, goles) VALUES (?, ?, TRUE, 2)")) {
                    ps.setLong(1, idPartidoSoloLocal);
                    ps.setLong(2, idEquipoLocal);
                    ps.executeUpdate();
                }

                List<Object[]> conTemporales = dao.fixtureDeTorneo(conn, null);
                Object[] filaSinEquipos = buscarFilaPorFechaYSede(conTemporales, fechaSinEquipos, nombreCancha);
                assertNotNull(filaSinEquipos, "Debe aparecer el partido temporal con id_fase = NULL y sin equipos");
                assertEquals(8, filaSinEquipos.length);
                assertEquals(fechaSinEquipos, filaSinEquipos[0]);
                assertEquals("PROGRAMADO", filaSinEquipos[1]);
                assertNull(filaSinEquipos[2], "Local debe ser null sin desplazar columnas");
                assertNull(filaSinEquipos[3], "Visitante debe ser null sin desplazar columnas");
                assertEquals("0 - 0", filaSinEquipos[4]);
                assertEquals(nombreCancha, filaSinEquipos[5]);
                assertNull(filaSinEquipos[6], "Fase debe ser null sin desplazar columnas");
                assertNull(filaSinEquipos[7], "Torneo debe ser null sin desplazar columnas");

                Object[] filaSoloLocal = buscarFilaPorFechaYSede(conTemporales, fechaSoloLocal, nombreCancha);
                assertNotNull(filaSoloLocal, "Debe aparecer el partido temporal con id_fase = NULL y visitante ausente");
                assertEquals(8, filaSoloLocal.length);
                assertEquals(fechaSoloLocal, filaSoloLocal[0]);
                assertEquals("PROGRAMADO", filaSoloLocal[1]);
                assertEquals(nombreEquipoLocal, filaSoloLocal[2]);
                assertNull(filaSoloLocal[3], "Visitante ausente debe quedar null sin desplazar Resultado, Sede, Fase ni Torneo");
                assertEquals("2 - 0", filaSoloLocal[4]);
                assertEquals(nombreCancha, filaSoloLocal[5]);
                assertNull(filaSoloLocal[6]);
                assertNull(filaSoloLocal[7]);
            } finally {
                conn.rollback();
                conn.setAutoCommit(true);
            }

            // Confirmar cero residuos persistidos tras rollback()
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT count(*) FROM partidos WHERE id_partido IN (?, ?)")) {
                ps.setLong(1, idPartidoSinEquipos);
                ps.setLong(2, idPartidoSoloLocal);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt(1), "El rollback() no debe dejar partidos temporales persistidos");
                }
            }
        }
    }

    @Test
    @DisplayName("CU-09..CU-11 PostgreSQL 17: conteo exacto de columnas (9, 6, 7, 5, 3) y ejecución vía ConsultaPublicaService")
    void testCu09ACu11Postgres17Real() throws Exception {
        ConsultaPublicaDAO dao = new ConsultaPublicaDAO();
        ConsultaPublicaService service = new ConsultaPublicaService();

        List<Object[]> torneos = service.torneos();
        List<Object[]> equipos = service.equipos();
        assertFalse(torneos.isEmpty());
        assertFalse(equipos.isEmpty());

        Long idTorneo = ((Number) torneos.get(0)[0]).longValue();
        Long idEquipo = ((Number) equipos.get(0)[0]).longValue();

        try (Connection conn = ConexionBD.obtenerConexionTest()) {
            ObservadorMetaData obsEq = new ObservadorMetaData(conn);
            dao.estadisticasDeEquipo(obsEq.conexionObservada, idEquipo);
            assertEquals(9, obsEq.etiquetasColumnas.size(), "CU-09 debe proyectar 9 columnas en PostgreSQL 17");

            ObservadorMetaData obsJug = new ObservadorMetaData(conn);
            dao.estadisticasDeJugadores(obsJug.conexionObservada, idTorneo);
            assertEquals(6, obsJug.etiquetasColumnas.size(), "CU-10 debe proyectar 6 columnas en PostgreSQL 17");

            ObservadorMetaData obsFases = new ObservadorMetaData(conn);
            dao.avanceDeFases(obsFases.conexionObservada, idTorneo);
            assertEquals(7, obsFases.etiquetasColumnas.size(), "CU-11 debe proyectar 7 columnas en PostgreSQL 17");

            ObservadorMetaData obsTorneos = new ObservadorMetaData(conn);
            dao.torneos(obsTorneos.conexionObservada);
            assertEquals(5, obsTorneos.etiquetasColumnas.size(), "Selector torneos debe proyectar 5 columnas");

            ObservadorMetaData obsEquipos = new ObservadorMetaData(conn);
            dao.equipos(obsEquipos.conexionObservada);
            assertEquals(3, obsEquipos.etiquetasColumnas.size(), "Selector equipos debe proyectar 3 columnas");
        }

        List<Object[]> perfil = service.perfilPublicoDeEquipo(idEquipo);
        List<Object[]> estadisticas = service.estadisticasDeJugadores(idTorneo);
        List<Object[]> fases = service.avanceDeFases(idTorneo);

        assertEquals(1, perfil.size());
        assertEquals(9, perfil.get(0).length);
        assertFalse(estadisticas.isEmpty());
        assertEquals(6, estadisticas.get(0).length);
        assertFalse(fases.isEmpty());
        assertEquals(7, fases.get(0).length);
        assertEquals(5, torneos.get(0).length);
        assertEquals(3, equipos.get(0).length);
    }

    @Test
    @DisplayName("Flujo integrado completo DAO -> Service -> FrmConsultaPublica contra PostgreSQL 17 real")
    void testFlujoIntegradoCompletoFrmConsultaPublicaPostgres17() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Entorno headless sin display grafico");

        ConsultaPublicaService servicioReal = new ConsultaPublicaService();
        List<Object[]> torneosEsperados = servicioReal.torneos();
        List<Object[]> equiposEsperados = servicioReal.equipos();
        assertFalse(torneosEsperados.isEmpty());
        assertFalse(equiposEsperados.isEmpty());

        Long idPrimerTorneo = ((Number) torneosEsperados.get(0)[0]).longValue();
        List<Object[]> fixtureInicialEsperado = servicioReal.fixture(idPrimerTorneo);
        assertFalse(fixtureInicialEsperado.isEmpty(),
                "El primer torneo del selector debe tener al menos un partido para validar alineación de celdas");

        Long idTorneoMulti;
        try (Connection conn = ConexionBD.obtenerConexionTest()) {
            idTorneoMulti = buscarTorneoConMultiplesPartidos(conn);
        }

        List<String> dialogos = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<FrmConsultaPublica> frmRef = new AtomicReference<>();

        try {
            runEdtConCapturaDeDialogos(dialogos, () -> {
                FrmConsultaPublica frm = new FrmConsultaPublica(null, servicioReal);
                frmRef.set(frm);
            }, () -> {
                FrmConsultaPublica frm = frmRef.get();
                assertNotNull(frm);

                for (SeccionPublica sec : SeccionPublica.values()) {
                    assertEquals(EstadoSeccion.CON_DATOS, frm.getEstadoSeccion(sec),
                            "Sección " + sec + " debe quedar en CON_DATOS contra PostgreSQL 17");
                }

                JTabbedPane tabs = buscarComponente(frm.getContentPane(), JTabbedPane.class);
                assertNotNull(tabs);

                JComboBox<?> cmbTorneo = buscarComponente(tabs.getComponentAt(0), JComboBox.class);
                JComboBox<?> cmbEquipo = buscarComponente(tabs.getComponentAt(1), JComboBox.class);
                assertNotNull(cmbTorneo);
                assertNotNull(cmbEquipo);
                assertEquals(torneosEsperados.size(), cmbTorneo.getItemCount());
                assertEquals(equiposEsperados.size(), cmbEquipo.getItemCount());

                // CU-08: 8 columnas exactas y alineación celda por celda
                JTable tCu08 = buscarTabla(tabs.getComponentAt(0));
                assertNotNull(tCu08);
                assertEquals(8, tCu08.getColumnCount());
                for (int c = 0; c < ENCABEZADOS_UI_CU08.length; c++) {
                    assertEquals(ENCABEZADOS_UI_CU08[c], tCu08.getColumnName(c));
                }
                assertEquals(fixtureInicialEsperado.size(), tCu08.getRowCount());
                verificarAlineacionCu08(tCu08, fixtureInicialEsperado);

                // Cambiar de torneo en selector y consultar manualmente CU-08
                if (idTorneoMulti != null) {
                    int idxMulti = buscarIndiceTorneo(torneosEsperados, idTorneoMulti);
                    if (idxMulti >= 0) {
                        cmbTorneo.setSelectedIndex(idxMulti);
                        buscarBoton(tabs.getComponentAt(0), "Consultar").doClick();
                        List<Object[]> fixtureMultiEsperado = servicioReal.fixture(idTorneoMulti);
                        assertEquals(fixtureMultiEsperado.size(), tCu08.getRowCount());
                        verificarAlineacionCu08(tCu08, fixtureMultiEsperado);
                    }
                }

                // CU-09..CU-11 operativas al consultar manualmente
                buscarBoton(tabs.getComponentAt(1), "Consultar").doClick();
                buscarBoton(tabs.getComponentAt(2), "Consultar").doClick();
                buscarBoton(tabs.getComponentAt(3), "Consultar").doClick();

                JTable tCu09 = buscarTabla(tabs.getComponentAt(1));
                JTable tCu10 = buscarTabla(tabs.getComponentAt(2));
                JTable tCu11 = buscarTabla(tabs.getComponentAt(3));
                assertEquals(9, tCu09.getColumnCount());
                assertEquals(1, tCu09.getRowCount());
                assertEquals(6, tCu10.getColumnCount());
                assertTrue(tCu10.getRowCount() > 0);
                assertEquals(7, tCu11.getColumnCount());
                assertTrue(tCu11.getRowCount() > 0);

                for (SeccionPublica sec : SeccionPublica.values()) {
                    assertEquals(EstadoSeccion.CON_DATOS, frm.getEstadoSeccion(sec));
                }
            });
        } finally {
            disposeEnEdt(frmRef.get());
        }

        assertEquals(0, dialogos.size(),
                "La carga real contra PostgreSQL 17 no debe emitir diálogos modales: " + dialogos);
    }

    private static void verificarAlineacionCu08(JTable tabla, List<Object[]> filasEsperadas) {
        for (int r = 0; r < filasEsperadas.size(); r++) {
            Object[] fila = filasEsperadas.get(r);
            assertEquals(8, fila.length);
            for (int c = 0; c < ENCABEZADOS_UI_CU08.length; c++) {
                int colIdx = tabla.getColumnModel().getColumnIndex(ENCABEZADOS_UI_CU08[c]);
                assertEquals(fila[c], tabla.getValueAt(r, colIdx),
                        "Desalineación en fila " + r + ", columna " + ENCABEZADOS_UI_CU08[c]);
            }
        }
    }

    private static int buscarIndiceTorneo(List<Object[]> torneos, Long idTorneo) {
        for (int i = 0; i < torneos.size(); i++) {
            Object id = torneos.get(i)[0];
            if (id != null && ((Number) id).longValue() == idTorneo) {
                return i;
            }
        }
        return -1;
    }

    private static Long buscarTorneoConMultiplesPartidos(Connection conn) throws SQLException {
        String sql = "SELECT f.id_torneo "
                + "FROM partidos p "
                + "JOIN fases f ON f.id_fase = p.id_fase "
                + "GROUP BY f.id_torneo "
                + "HAVING count(*) >= 2 "
                + "ORDER BY f.id_torneo ASC LIMIT 1";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : null;
        }
    }

    private static Object[] buscarFilaPorFechaYSede(List<Object[]> filas, String fechaHora, String sede) {
        for (Object[] fila : filas) {
            if (fechaHora.equals(fila[0]) && sede.equals(fila[5])) {
                return fila;
            }
        }
        return null;
    }

    private static void assertFilasIguales(List<Object[]> a, List<Object[]> b) {
        assertEquals(a.size(), b.size(), "Cantidad de filas debe ser determinista");
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).length, b.get(i).length);
            for (int c = 0; c < a.get(i).length; c++) {
                assertEquals(a.get(i)[c], b.get(i)[c],
                        "Diferencia de orden/contenido en fila " + i + ", col " + c);
            }
        }
    }

    /** Envoltorio delegante sobre Connection real para capturar ResultSetMetaData en PostgreSQL 17. */
    private static final class ObservadorMetaData {
        final List<String> etiquetasColumnas = new ArrayList<>();
        final Connection conexionObservada;

        ObservadorMetaData(Connection real) {
            this.conexionObservada = (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        Object res = invokeMethod(real, method, args);
                        if ("prepareStatement".equals(method.getName()) && res instanceof PreparedStatement ps) {
                            return envolverPreparedStatement(ps);
                        }
                        return res;
                    });
        }

        private PreparedStatement envolverPreparedStatement(PreparedStatement realPs) {
            return (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> {
                        Object res = invokeMethod(realPs, method, args);
                        if ("executeQuery".equals(method.getName()) && res instanceof ResultSet rs) {
                            ResultSetMetaData md = rs.getMetaData();
                            etiquetasColumnas.clear();
                            for (int c = 1; c <= md.getColumnCount(); c++) {
                                etiquetasColumnas.add(md.getColumnLabel(c));
                            }
                        }
                        return res;
                    });
        }

        private static Object invokeMethod(Object target, java.lang.reflect.Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }

    @FunctionalInterface
    private interface EdtAction {
        void run() throws Exception;
    }

    private static void runEdtConCapturaDeDialogos(
            List<String> dialogos,
            EdtAction accionEdt,
            EdtAction verificacionEdt) {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Entorno headless sin display grafico");

        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicReference<Timer> timerRef = new AtomicReference<>();

        assertDoesNotThrow(() -> {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    EstilosUI.inicializarTema();
                    Timer timer = new Timer(50, e -> capturarYCerrarDialogosModales(dialogos));
                    timer.setRepeats(true);
                    timer.start();
                    timerRef.set(timer);
                    try {
                        accionEdt.run();
                    } catch (Throwable t) {
                        error.set(t);
                    }
                });
                SwingUtilities.invokeAndWait(() -> capturarYCerrarDialogosModales(dialogos));
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        capturarYCerrarDialogosModales(dialogos);
                        if (error.get() == null) {
                            verificacionEdt.run();
                        }
                    } catch (Throwable t) {
                        error.set(t);
                    } finally {
                        Timer timer = timerRef.get();
                        if (timer != null) {
                            timer.stop();
                        }
                    }
                });
            } catch (InvocationTargetException e) {
                Timer timer = timerRef.get();
                if (timer != null) {
                    timer.stop();
                }
                if (contieneHeadlessException(e.getCause())) {
                    error.set(e.getCause());
                    return;
                }
                throw e;
            }
        });

        Throwable t = error.get();
        if (t != null) {
            if (contieneHeadlessException(t)) {
                Assumptions.assumeTrue(false, "Entorno headless sin display grafico");
            }
            if (t instanceof Error err) {
                throw err;
            }
            if (t instanceof RuntimeException re) {
                throw re;
            }
            throw new AssertionError("Excepcion inesperada en EDT", t);
        }
    }

    private static void capturarYCerrarDialogosModales(List<String> dialogos) {
        for (Window w : Window.getWindows()) {
            if (w instanceof JDialog d && d.isShowing() && d.getModalityType() != Dialog.ModalityType.MODELESS) {
                JOptionPane pane = buscarComponente(d, JOptionPane.class);
                dialogos.add(pane != null && pane.getMessage() != null ? String.valueOf(pane.getMessage()) : "<modal>");
                AbstractButton btnOk = buscarBoton(d, "Aceptar", "OK", "Cerrar");
                if (btnOk != null) {
                    btnOk.doClick();
                } else {
                    d.dispose();
                }
            }
        }
    }

    private static boolean contieneHeadlessException(Throwable t) {
        Throwable cur = t;
        int depth = 0;
        while (cur != null && depth < 16) {
            if (cur instanceof HeadlessException) {
                return true;
            }
            cur = cur.getCause();
            depth++;
        }
        return false;
    }

    private static void disposeEnEdt(FrmConsultaPublica frm) {
        if (frm == null || GraphicsEnvironment.isHeadless()) {
            return;
        }
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                frm.dispose();
            } else {
                SwingUtilities.invokeAndWait(frm::dispose);
            }
        } catch (Exception ignored) {
            // Limpieza best-effort en finally
        }
    }

    private static JTable buscarTabla(Component root) {
        JScrollPane scroll = buscarComponente(root, JScrollPane.class);
        if (scroll != null) {
            JViewport vp = scroll.getViewport();
            if (vp != null && vp.getView() instanceof JTable tabla) {
                return tabla;
            }
        }
        return buscarComponente(root, JTable.class);
    }

    private static <T extends Component> T buscarComponente(Component root, Class<T> type) {
        if (type.isInstance(root)) {
            return type.cast(root);
        }
        if (root instanceof Container c) {
            for (Component child : c.getComponents()) {
                T found = buscarComponente(child, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static AbstractButton buscarBoton(Component root, String... textos) {
        if (root instanceof AbstractButton btn && btn.getText() != null) {
            for (String t : textos) {
                if (btn.getText().equalsIgnoreCase(t)) {
                    return btn;
                }
            }
        }
        if (root instanceof Container c) {
            for (Component child : c.getComponents()) {
                AbstractButton found = buscarBoton(child, textos);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
