package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.dao.ConsultaPublicaDAO;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.servicio.ConsultaPublicaService;
import com.gestionamateur.tpi.progav.vista.FrmConsultaPublica.EstadoSeccion;
import com.gestionamateur.tpi.progav.vista.FrmConsultaPublica.SeccionPublica;
import org.junit.jupiter.api.Assumptions;
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
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.UnresolvedAddressException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias e integrales para la inicializacion offline y recuperacion de
 * {@link FrmConsultaPublica} sin base de datos PostgreSQL real.
 */
@DisplayName("FrmConsultaPublica: Inicialización offline, clasificación de errores y recuperación")
public class FrmConsultaPublicaInicializacionOfflineTest {

    // ------------------------------------------------------------------
    // 1. Pruebas unitarias compatibles con headless
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Clasificador esErrorDeConectividad: SQLState 08xxx retorna true")
    void testClasificadorSqlState08RetornaTrue() {
        assertTrue(FrmConsultaPublica.esErrorDeConectividad(new SQLException("conn refused", "08001")));
        assertTrue(FrmConsultaPublica.esErrorDeConectividad(new SQLException("rejected", "08004")));
        assertTrue(FrmConsultaPublica.esErrorDeConectividad(new SQLException("failure", "08006")));
        assertTrue(FrmConsultaPublica.esErrorDeConectividad(
                new TorneoExcepcion("Error al consultar torneos", new SQLException("link down", "08003"))));
    }

    @Test
    @DisplayName("Clasificador esErrorDeConectividad: excepciones de red/socket en cadena causal retornan true")
    void testClasificadorExcepcionesRedRetornanTrue() {
        assertTrue(FrmConsultaPublica.esErrorDeConectividad(
                new TorneoExcepcion("wrap", new SQLException("io", new ConnectException("Connection refused")))));
        assertTrue(FrmConsultaPublica.esErrorDeConectividad(
                new TorneoExcepcion("wrap", new SQLException("io", new SocketTimeoutException("Read timed out")))));
        assertTrue(FrmConsultaPublica.esErrorDeConectividad(
                new TorneoExcepcion("wrap", new SQLException("io", new SocketException("Socket closed")))));
        assertTrue(FrmConsultaPublica.esErrorDeConectividad(
                new TorneoExcepcion("wrap", new SQLException("io", new UnknownHostException("db.local")))));
        assertTrue(FrmConsultaPublica.esErrorDeConectividad(
                new TorneoExcepcion("wrap", new SQLException("io", new UnresolvedAddressException()))));
    }

    @Test
    @DisplayName("Clasificador esErrorDeConectividad: errores SQL no-conectividad y negocio retornan false")
    void testClasificadorNoConectividadRetornaFalse() {
        assertFalse(FrmConsultaPublica.esErrorDeConectividad(null));
        assertFalse(FrmConsultaPublica.esErrorDeConectividad(new SQLException("tabla inexistente", "42P01")));
        assertFalse(FrmConsultaPublica.esErrorDeConectividad(new SQLException("sintaxis", "42601")));
        assertFalse(FrmConsultaPublica.esErrorDeConectividad(new SQLException("auth failed", "28P01")));
        assertFalse(FrmConsultaPublica.esErrorDeConectividad(new SQLException("auth", "28000")));
        assertFalse(FrmConsultaPublica.esErrorDeConectividad(new SQLException("db missing", "3D000")));
        assertFalse(FrmConsultaPublica.esErrorDeConectividad(new SQLException("unique", "23505")));
        assertFalse(FrmConsultaPublica.esErrorDeConectividad(new SQLException("generic")));
        assertFalse(FrmConsultaPublica.esErrorDeConectividad(new TorneoExcepcion("Debe seleccionar un equipo.")));
    }

    @Test
    @DisplayName("Clasificador esErrorDeConectividad: resiste ciclos causales sin bucle infinito")
    void testClasificadorCicloCausalNoCuelga() {
        SQLException e1 = new SQLException("err1", "42P01");
        SQLException e2 = new SQLException("err2", "42P01", e1);
        e1.setNextException(e2);
        assertFalse(FrmConsultaPublica.esErrorDeConectividad(e2));
    }

    @Test
    @DisplayName("Sanitización de mensajes: oculta URLs JDBC, credenciales (incluidas con comillas) y trazas de pila")
    void testSanitizarMensajeParaUsuario() {
        assertEquals("", FrmConsultaPublica.sanitizarMensajeParaUsuario(null));
        assertEquals("", FrmConsultaPublica.sanitizarMensajeParaUsuario("   "));

        String raw = "No se pudo conectar en 'jdbc:postgresql://127.0.0.1:55432/tpi_pa2?user=postgres&password=MiPassword123' "
                + "con pwd=secreto, password='QuotedSingle', passwd=\"QuotedDouble\"\n"
                + "\tat com.gestionamateur.Ejemplo.metodo(Ejemplo.java:10)";
        String sanitizado = FrmConsultaPublica.sanitizarMensajeParaUsuario(raw);

        assertFalse(sanitizado.contains("jdbc:postgresql"));
        assertFalse(sanitizado.contains("MiPassword123"));
        assertFalse(sanitizado.contains("secreto"));
        assertFalse(sanitizado.contains("QuotedSingle"));
        assertFalse(sanitizado.contains("QuotedDouble"));
        assertFalse(sanitizado.contains("Ejemplo.java"));
        assertTrue(sanitizado.contains("[URL JDBC omitida]"));
        assertTrue(sanitizado.contains("pwd=***"));
        assertTrue(sanitizado.contains("password=***"));
        assertTrue(sanitizado.contains("passwd=***"));
    }

    @Test
    @DisplayName("Constructor package-private rechaza servicio nulo")
    void testConstructorRechazaServicioNulo() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Entorno headless sin display grafico");
        assertThrows(NullPointerException.class, () -> new FrmConsultaPublica(null, null));
    }

    // ------------------------------------------------------------------
    // 2. Pruebas integrales de flujo real en Swing EDT
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Inicialización offline (08001): 1 sola notificación modal real, 1 sola llamada fallida y estado consistente")
    void testInicializacionOfflineUnaSolaNotificacionYUnaLlamada() {
        StubServicioControlable stub = new StubServicioControlable();
        stub.modoTorneos = ModoFallo.CONECTIVIDAD_08001;

        List<DialogoCapturado> dialogos = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<FrmConsultaPublica> frmRef = new AtomicReference<>();

        try {
            runEdtConCapturaDeDialogos(dialogos, () -> {
                FrmConsultaPublica frm = new FrmConsultaPublica(null, stub);
                frmRef.set(frm);
            }, () -> {
                FrmConsultaPublica frm = frmRef.get();
                assertNotNull(frm);
                assertEquals(1, stub.llamadasTorneos.get(), "Solo debe intentar una conexion inicial (torneos)");
                assertEquals(0, stub.llamadasEquipos.get(), "No debe intentar equipos tras corte por conectividad");
                assertEquals(0, stub.llamadasFixture.get(), "No debe intentar fixture tras corte por conectividad");
                assertEquals(0, stub.llamadasPerfil.get(), "No debe llamar perfilPublicoDeEquipo(null) offline");
                assertEquals(0, stub.llamadasEstadisticas.get(), "No debe intentar estadisticas tras corte por conectividad");
                assertEquals(0, stub.llamadasFases.get(), "No debe intentar fases tras corte por conectividad");

                for (SeccionPublica sec : SeccionPublica.values()) {
                    assertEquals(EstadoSeccion.NO_DISPONIBLE_CONEXION, frm.getEstadoSeccion(sec),
                            "Seccion " + sec + " debe quedar en NO_DISPONIBLE_CONEXION");
                }

                JTabbedPane tabs = buscarComponente(frm.getContentPane(), JTabbedPane.class);
                assertNotNull(tabs);

                JComboBox<?> cmbTorneos = buscarComponente(tabs.getComponentAt(0), JComboBox.class);
                JComboBox<?> cmbEquipos = buscarComponente(tabs.getComponentAt(1), JComboBox.class);
                assertNotNull(cmbTorneos);
                assertNotNull(cmbEquipos);
                assertEquals(0, cmbTorneos.getItemCount(), "No debe haber items ficticios en combo de torneos");
                assertEquals(0, cmbEquipos.getItemCount(), "No debe haber items ficticios en combo de equipos");

                JTable tCu08 = buscarTabla(tabs.getComponentAt(0));
                JTable tCu09 = buscarTabla(tabs.getComponentAt(1));
                JTable tCu10 = buscarTabla(tabs.getComponentAt(2));
                JTable tCu11 = buscarTabla(tabs.getComponentAt(3));
                assertEquals(8, tCu08.getColumnCount());
                assertEquals(9, tCu09.getColumnCount());
                assertEquals(6, tCu10.getColumnCount());
                assertEquals(7, tCu11.getColumnCount());
                assertEquals(0, tCu08.getRowCount());
                assertEquals(0, tCu09.getRowCount());
                assertEquals(0, tCu10.getRowCount());
                assertEquals(0, tCu11.getRowCount());
                assertEquals("Datos no disponibles: sin conexión con PostgreSQL.", tCu08.getToolTipText());
            });
        } finally {
            disposeEnEdt(frmRef.get());
        }

        assertEquals(1, dialogos.size(), "Debe mostrarse exactamente 1 dialogo modal consolidado");
        DialogoCapturado dlg = dialogos.get(0);
        assertEquals(JOptionPane.WARNING_MESSAGE, dlg.tipoMensaje);
        assertTrue(dlg.mensaje.contains("PostgreSQL"), "Debe explicar indisponibilidad de PostgreSQL");
        assertFalse(dlg.mensaje.contains("jdbc:postgresql"), "No debe exponer URL JDBC");
        assertFalse(dlg.mensaje.contains("SecretaClave"), "No debe exponer credenciales");
        assertFalse(dlg.mensaje.contains("Debe seleccionar un equipo"), "No debe emitir falso error derivado de CU-09");
    }

    @Test
    @DisplayName("Diagnóstico técnico preservado en Logger para 08001 y 42P01")
    void testDiagnosticoTecnicoPreservadoEnLogger() {
        Logger logger = Logger.getLogger(FrmConsultaPublica.class.getName());
        List<LogRecord> registros = Collections.synchronizedList(new ArrayList<>());
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                registros.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);
        try {
            StubServicioControlable stubOffline = new StubServicioControlable();
            stubOffline.modoTorneos = ModoFallo.CONECTIVIDAD_08001;
            List<DialogoCapturado> d1 = Collections.synchronizedList(new ArrayList<>());
            AtomicReference<FrmConsultaPublica> f1 = new AtomicReference<>();
            try {
                runEdtConCapturaDeDialogos(d1, () -> f1.set(new FrmConsultaPublica(null, stubOffline)), () -> {
                    assertNotNull(f1.get());
                });
            } finally {
                disposeEnEdt(f1.get());
            }

            StubServicioControlable stubSql = new StubServicioControlable();
            stubSql.modoFixture = ModoFallo.SQL_42P01;
            List<DialogoCapturado> d2 = Collections.synchronizedList(new ArrayList<>());
            AtomicReference<FrmConsultaPublica> f2 = new AtomicReference<>();
            try {
                runEdtConCapturaDeDialogos(d2, () -> f2.set(new FrmConsultaPublica(null, stubSql)), () -> {
                    assertNotNull(f2.get());
                });
            } finally {
                disposeEnEdt(f2.get());
            }
        } finally {
            logger.removeHandler(handler);
        }

        boolean encontro08001 = registros.stream().anyMatch(r ->
                r.getLevel() == Level.WARNING && contieneSqlState(r.getThrown(), "08001"));
        boolean encontro42p01 = registros.stream().anyMatch(r ->
                r.getLevel() == Level.WARNING && contieneSqlState(r.getThrown(), "42P01"));
        assertTrue(encontro08001, "Debe registrarse en Logger la excepcion original con SQLState 08001");
        assertTrue(encontro42p01, "Debe registrarse en Logger la excepcion original con SQLState 42P01");
    }

    @Test
    @DisplayName("Error SQL no-conectividad (42P01 en fixture): no clasifica como desconexión y carga demás pestañas")
    void testErrorSql42P01EnFixtureNoEsDesconexion() {
        StubServicioControlable stub = new StubServicioControlable();
        stub.modoFixture = ModoFallo.SQL_42P01;

        List<DialogoCapturado> dialogos = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<FrmConsultaPublica> frmRef = new AtomicReference<>();

        try {
            runEdtConCapturaDeDialogos(dialogos, () -> frmRef.set(new FrmConsultaPublica(null, stub)), () -> {
                FrmConsultaPublica frm = frmRef.get();
                assertEquals(EstadoSeccion.CON_DATOS, frm.getEstadoSeccion(SeccionPublica.TORNEOS));
                assertEquals(EstadoSeccion.CON_DATOS, frm.getEstadoSeccion(SeccionPublica.EQUIPOS));
                assertEquals(EstadoSeccion.ERROR_CONSULTA, frm.getEstadoSeccion(SeccionPublica.FIXTURE));
                assertEquals(EstadoSeccion.CON_DATOS, frm.getEstadoSeccion(SeccionPublica.PERFIL_EQUIPO));
                assertEquals(EstadoSeccion.CON_DATOS, frm.getEstadoSeccion(SeccionPublica.ESTADISTICAS));
                assertEquals(EstadoSeccion.CON_DATOS, frm.getEstadoSeccion(SeccionPublica.FASES));

                JTabbedPane tabs = buscarComponente(frm.getContentPane(), JTabbedPane.class);
                JTable tCu08 = buscarTabla(tabs.getComponentAt(0));
                assertEquals("No se pudieron cargar los datos por un error de consulta.", tCu08.getToolTipText());
            });
        } finally {
            disposeEnEdt(frmRef.get());
        }

        assertEquals(1, dialogos.size(), "Debe emitir 1 notificacion consolidada de error tecnico");
        DialogoCapturado dlg = dialogos.get(0);
        assertEquals(JOptionPane.ERROR_MESSAGE, dlg.tipoMensaje);
        assertFalse(dlg.mensaje.toLowerCase().contains("sin conexión"), "No debe decir sin conexion");
        assertFalse(dlg.mensaje.toLowerCase().contains("no hay conexión"), "No debe decir no hay conexion");
        assertTrue(dlg.mensaje.contains("does not exist"), "Debe conservar el diagnostico SQL sanitizado");
        assertFalse(dlg.mensaje.contains("jdbc:postgresql"), "Debe sanitizar URLs JDBC si existieran");
    }

    @Test
    @DisplayName("Inicialización parcial: torneos carga bien y equipos cae por 08006")
    void testInicializacionParcialTorneosOkEquiposCae08006() {
        StubServicioControlable stub = new StubServicioControlable();
        stub.modoEquipos = ModoFallo.CONECTIVIDAD_08006;

        List<DialogoCapturado> dialogos = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<FrmConsultaPublica> frmRef = new AtomicReference<>();

        try {
            runEdtConCapturaDeDialogos(dialogos, () -> frmRef.set(new FrmConsultaPublica(null, stub)), () -> {
                FrmConsultaPublica frm = frmRef.get();
                assertEquals(1, stub.llamadasTorneos.get());
                assertEquals(1, stub.llamadasEquipos.get());
                assertEquals(0, stub.llamadasFixture.get());
                assertEquals(0, stub.llamadasPerfil.get());
                assertEquals(0, stub.llamadasEstadisticas.get());
                assertEquals(0, stub.llamadasFases.get());

                assertEquals(EstadoSeccion.CON_DATOS, frm.getEstadoSeccion(SeccionPublica.TORNEOS));
                assertEquals(EstadoSeccion.NO_DISPONIBLE_CONEXION, frm.getEstadoSeccion(SeccionPublica.EQUIPOS));
                assertEquals(EstadoSeccion.NO_DISPONIBLE_CONEXION, frm.getEstadoSeccion(SeccionPublica.FIXTURE));
                assertEquals(EstadoSeccion.NO_DISPONIBLE_CONEXION, frm.getEstadoSeccion(SeccionPublica.PERFIL_EQUIPO));
                assertEquals(EstadoSeccion.NO_DISPONIBLE_CONEXION, frm.getEstadoSeccion(SeccionPublica.ESTADISTICAS));
                assertEquals(EstadoSeccion.NO_DISPONIBLE_CONEXION, frm.getEstadoSeccion(SeccionPublica.FASES));

                JTabbedPane tabs = buscarComponente(frm.getContentPane(), JTabbedPane.class);
                JComboBox<?> cmbTorneos = buscarComponente(tabs.getComponentAt(0), JComboBox.class);
                assertEquals(2, cmbTorneos.getItemCount(), "Torneos cargados antes del corte deben conservarse");
            });
        } finally {
            disposeEnEdt(frmRef.get());
        }

        assertEquals(1, dialogos.size());
        assertEquals(JOptionPane.WARNING_MESSAGE, dialogos.get(0).tipoMensaje);
    }

    @Test
    @DisplayName("Inicialización online simulada con datos: 0 notificaciones y CU-08..CU-11 pobladas")
    void testInicializacionOnlineConDatosCeroNotificaciones() {
        StubServicioControlable stub = new StubServicioControlable();
        List<DialogoCapturado> dialogos = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<FrmConsultaPublica> frmRef = new AtomicReference<>();

        try {
            runEdtConCapturaDeDialogos(dialogos, () -> frmRef.set(new FrmConsultaPublica(null, stub)), () -> {
                FrmConsultaPublica frm = frmRef.get();
                for (SeccionPublica sec : SeccionPublica.values()) {
                    assertEquals(EstadoSeccion.CON_DATOS, frm.getEstadoSeccion(sec));
                }
                JTabbedPane tabs = buscarComponente(frm.getContentPane(), JTabbedPane.class);
                JTable tCu08 = buscarTabla(tabs.getComponentAt(0));
                JTable tCu09 = buscarTabla(tabs.getComponentAt(1));
                JTable tCu10 = buscarTabla(tabs.getComponentAt(2));
                JTable tCu11 = buscarTabla(tabs.getComponentAt(3));

                assertEquals(8, tCu08.getColumnCount());
                assertEquals(2, tCu08.getRowCount());
                assertEquals("Atletico Guarani", tCu08.getValueAt(0, 2));
                assertNull(tCu08.getToolTipText());

                assertEquals(9, tCu09.getColumnCount());
                assertEquals(1, tCu09.getRowCount());
                assertEquals(6, tCu10.getColumnCount());
                assertEquals(1, tCu10.getRowCount());
                assertEquals(7, tCu11.getColumnCount());
                assertEquals(1, tCu11.getRowCount());
            });
        } finally {
            disposeEnEdt(frmRef.get());
        }

        assertEquals(0, dialogos.size(), "Carga online exitosa no debe emitir notificaciones");
    }

    @Test
    @DisplayName("Resultado legítimamente vacío online: 0 notificaciones en apertura, estados VACIO_VALIDO y validación manual en CU-09")
    void testResultadoVacioOnlineCeroNotificacionesYValidacionManualCu09() {
        StubServicioControlable stub = new StubServicioControlable();
        stub.devolverVacio = true;

        List<DialogoCapturado> dialogosApertura = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<FrmConsultaPublica> frmRef = new AtomicReference<>();

        try {
            runEdtConCapturaDeDialogos(dialogosApertura, () -> frmRef.set(new FrmConsultaPublica(null, stub)), () -> {
                FrmConsultaPublica frm = frmRef.get();
                assertEquals(0, stub.llamadasPerfil.get(),
                        "No debe llamar perfilPublicoDeEquipo(null) en apertura cuando equipos es vacio");
                for (SeccionPublica sec : SeccionPublica.values()) {
                    assertEquals(EstadoSeccion.VACIO_VALIDO, frm.getEstadoSeccion(sec),
                            "Seccion " + sec + " debe distinguirse como VACIO_VALIDO");
                }
            });
            assertEquals(0, dialogosApertura.size(), "Resultado vacio legitimo no debe emitir error ni advertencia");

            // Verificar que al pulsar Consultar manualmente en CU-09 con equipos=[] si preserva la validacion
            List<DialogoCapturado> dialogosManual = Collections.synchronizedList(new ArrayList<>());
            runEdtConCapturaDeDialogos(dialogosManual, () -> {
                JTabbedPane tabs = buscarComponente(frmRef.get().getContentPane(), JTabbedPane.class);
                AbstractButton btnCu09 = buscarBoton(tabs.getComponentAt(1), "Consultar");
                assertNotNull(btnCu09);
                btnCu09.doClick();
            }, () -> {
                assertEquals(1, stub.llamadasPerfil.get(),
                        "Al pulsar Consultar manualmente en CU-09 con equipos=[] debe invocar servicio.perfilPublicoDeEquipo(null)");
            });

            assertEquals(1, dialogosManual.size());
            assertEquals("No se pudo consultar el equipo: Debe seleccionar un equipo.",
                    dialogosManual.get(0).mensaje,
                    "Debe preservar validacion de negocio con prefijo contextual al consultar manualmente sin equipo");
        } finally {
            disposeEnEdt(frmRef.get());
        }
    }

    @Test
    @DisplayName("Reapertura offline: segunda ventana independiente emite su propia notificación")
    void testReaperturaOfflineSegundaVentanaEmiteSuPropiaNotificacion() {
        StubServicioControlable stub = new StubServicioControlable();
        stub.modoTorneos = ModoFallo.CONECTIVIDAD_08001;

        List<DialogoCapturado> dialogos1 = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<FrmConsultaPublica> f1 = new AtomicReference<>();
        try {
            runEdtConCapturaDeDialogos(dialogos1, () -> f1.set(new FrmConsultaPublica(null, stub)), () -> {
                assertNotNull(f1.get());
            });
        } finally {
            disposeEnEdt(f1.get());
        }
        assertEquals(1, dialogos1.size(), "Primera instancia offline debe emitir 1 notificacion");

        List<DialogoCapturado> dialogos2 = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<FrmConsultaPublica> f2 = new AtomicReference<>();
        try {
            runEdtConCapturaDeDialogos(dialogos2, () -> f2.set(new FrmConsultaPublica(null, stub)), () -> {
                assertNotNull(f2.get());
            });
        } finally {
            disposeEnEdt(f2.get());
        }
        assertEquals(1, dialogos2.size(), "Segunda instancia offline debe emitir su propia notificacion");
    }

    @Test
    @DisplayName("Recuperación tras volver la conexión: Consultar en CU-08 y CU-09 recarga selectores y tablas")
    void testRecuperacionTrasVolverConexion() {
        StubServicioControlable stub = new StubServicioControlable();
        stub.modoTorneos = ModoFallo.CONECTIVIDAD_08001;
        stub.modoEquipos = ModoFallo.CONECTIVIDAD_08001;
        stub.modoFixture = ModoFallo.CONECTIVIDAD_08001;
        stub.modoPerfil = ModoFallo.CONECTIVIDAD_08001;

        List<DialogoCapturado> dialogosInit = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<FrmConsultaPublica> frmRef = new AtomicReference<>();

        try {
            runEdtConCapturaDeDialogos(dialogosInit, () -> frmRef.set(new FrmConsultaPublica(null, stub)), () -> {
                assertEquals(EstadoSeccion.NO_DISPONIBLE_CONEXION, frmRef.get().getEstadoSeccion(SeccionPublica.FIXTURE));
                assertEquals(EstadoSeccion.NO_DISPONIBLE_CONEXION, frmRef.get().getEstadoSeccion(SeccionPublica.PERFIL_EQUIPO));
            });
            assertEquals(1, dialogosInit.size());

            // Vuelve la conexion
            stub.modoTorneos = ModoFallo.NINGUNO;
            stub.modoEquipos = ModoFallo.NINGUNO;
            stub.modoFixture = ModoFallo.NINGUNO;
            stub.modoPerfil = ModoFallo.NINGUNO;

            List<DialogoCapturado> dialogosRecuperacion = Collections.synchronizedList(new ArrayList<>());
            runEdtConCapturaDeDialogos(dialogosRecuperacion, () -> {
                FrmConsultaPublica frm = frmRef.get();
                JTabbedPane tabs = buscarComponente(frm.getContentPane(), JTabbedPane.class);
                AbstractButton btnCu08 = buscarBoton(tabs.getComponentAt(0), "Consultar");
                AbstractButton btnCu09 = buscarBoton(tabs.getComponentAt(1), "Consultar");
                assertNotNull(btnCu08);
                assertNotNull(btnCu09);
                assertTrue(btnCu08.isEnabled());
                assertTrue(btnCu09.isEnabled());

                btnCu08.doClick();
                btnCu09.doClick();
            }, () -> {
                FrmConsultaPublica frm = frmRef.get();
                assertEquals(EstadoSeccion.CON_DATOS, frm.getEstadoSeccion(SeccionPublica.TORNEOS));
                assertEquals(EstadoSeccion.CON_DATOS, frm.getEstadoSeccion(SeccionPublica.FIXTURE));
                assertEquals(EstadoSeccion.CON_DATOS, frm.getEstadoSeccion(SeccionPublica.EQUIPOS));
                assertEquals(EstadoSeccion.CON_DATOS, frm.getEstadoSeccion(SeccionPublica.PERFIL_EQUIPO));

                JTabbedPane tabs = buscarComponente(frm.getContentPane(), JTabbedPane.class);
                JComboBox<?> cmbTorneos = buscarComponente(tabs.getComponentAt(0), JComboBox.class);
                JComboBox<?> cmbEquipos = buscarComponente(tabs.getComponentAt(1), JComboBox.class);
                JTable tCu08 = buscarTabla(tabs.getComponentAt(0));
                JTable tCu09 = buscarTabla(tabs.getComponentAt(1));

                assertEquals(2, cmbTorneos.getItemCount());
                assertEquals(1, cmbEquipos.getItemCount());
                assertEquals(2, tCu08.getRowCount());
                assertEquals(1, tCu09.getRowCount());
                assertNull(tCu08.getToolTipText());
                assertNull(tCu09.getToolTipText());
            });

            assertEquals(0, dialogosRecuperacion.size(), "La recuperacion exitosa no debe mostrar errores");
        } finally {
            disposeEnEdt(frmRef.get());
        }
    }

    @Test
    @DisplayName("Consultar manual permaneciendo offline (CU-08 y CU-09): 1 aviso limpio por consulta sin falsos errores")
    void testConsultarManualPermaneciendoOfflineSinFalsosErrores() {
        StubServicioControlable stub = new StubServicioControlable();
        stub.modoTorneos = ModoFallo.CONECTIVIDAD_08001;
        stub.modoEquipos = ModoFallo.CONECTIVIDAD_08001;
        stub.modoFixture = ModoFallo.CONECTIVIDAD_08001;
        stub.modoPerfil = ModoFallo.CONECTIVIDAD_08001;

        List<DialogoCapturado> dialogosInit = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<FrmConsultaPublica> frmRef = new AtomicReference<>();
        try {
            runEdtConCapturaDeDialogos(dialogosInit, () -> frmRef.set(new FrmConsultaPublica(null, stub)), () -> {
                assertEquals(EstadoSeccion.NO_DISPONIBLE_CONEXION, frmRef.get().getEstadoSeccion(SeccionPublica.FIXTURE));
            });
            assertEquals(1, dialogosInit.size());

            // Pulsar Consultar en CU-08 estando offline -> exactamente 1 aviso limpio y 0 llamadas a fixture(null)
            List<DialogoCapturado> dialogosCu08 = Collections.synchronizedList(new ArrayList<>());
            runEdtConCapturaDeDialogos(dialogosCu08, () -> {
                JTabbedPane tabs = buscarComponente(frmRef.get().getContentPane(), JTabbedPane.class);
                AbstractButton btnCu08 = buscarBoton(tabs.getComponentAt(0), "Consultar");
                assertNotNull(btnCu08);
                btnCu08.doClick();
            }, () -> {
                assertEquals(0, stub.llamadasFixture.get(),
                        "Si torneos fallo por conectividad, no debe invocar fixture(null)");
                assertEquals(EstadoSeccion.NO_DISPONIBLE_CONEXION, frmRef.get().getEstadoSeccion(SeccionPublica.FIXTURE));
            });
            assertEquals(1, dialogosCu08.size(), "CU-08 manual offline debe emitir exactamente 1 dialogo");
            assertEquals(JOptionPane.WARNING_MESSAGE, dialogosCu08.get(0).tipoMensaje);
            assertFalse(dialogosCu08.get(0).mensaje.contains("jdbc:postgresql"));
            assertFalse(dialogosCu08.get(0).mensaje.contains("SecretaClave"));

            // Pulsar Consultar en CU-09 estando offline -> exactamente 1 aviso limpio y 0 llamadas a perfilPublicoDeEquipo(null)
            List<DialogoCapturado> dialogosCu09 = Collections.synchronizedList(new ArrayList<>());
            runEdtConCapturaDeDialogos(dialogosCu09, () -> {
                JTabbedPane tabs = buscarComponente(frmRef.get().getContentPane(), JTabbedPane.class);
                AbstractButton btnCu09 = buscarBoton(tabs.getComponentAt(1), "Consultar");
                assertNotNull(btnCu09);
                btnCu09.doClick();
            }, () -> {
                assertEquals(0, stub.llamadasPerfil.get(),
                        "Si equipos fallo por conectividad, no debe invocar perfilPublicoDeEquipo(null)");
                assertEquals(EstadoSeccion.NO_DISPONIBLE_CONEXION,
                        frmRef.get().getEstadoSeccion(SeccionPublica.PERFIL_EQUIPO));
            });
            assertEquals(1, dialogosCu09.size(), "CU-09 manual offline debe emitir exactamente 1 dialogo");
            assertEquals(JOptionPane.WARNING_MESSAGE, dialogosCu09.get(0).tipoMensaje);
            assertFalse(dialogosCu09.get(0).mensaje.contains("Debe seleccionar un equipo"));
        } finally {
            disposeEnEdt(frmRef.get());
        }
    }

    @Test
    @DisplayName("Consultar manual tras caída de conexión (08001) vs error SQL (42P01)")
    void testConsultarManualConSelectorValidoAnteDesconexionYErrorSql() {
        StubServicioControlable stub = new StubServicioControlable();
        List<DialogoCapturado> dialogosInit = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<FrmConsultaPublica> frmRef = new AtomicReference<>();

        try {
            runEdtConCapturaDeDialogos(dialogosInit, () -> frmRef.set(new FrmConsultaPublica(null, stub)), () -> {
                assertEquals(EstadoSeccion.CON_DATOS, frmRef.get().getEstadoSeccion(SeccionPublica.FIXTURE));
            });
            assertEquals(0, dialogosInit.size());

            // Caida de conexion al consultar manualmente
            stub.modoFixture = ModoFallo.CONECTIVIDAD_08001;
            List<DialogoCapturado> dialogosOffline = Collections.synchronizedList(new ArrayList<>());
            runEdtConCapturaDeDialogos(dialogosOffline, () -> {
                JTabbedPane tabs = buscarComponente(frmRef.get().getContentPane(), JTabbedPane.class);
                buscarBoton(tabs.getComponentAt(0), "Consultar").doClick();
            }, () -> {
                JTabbedPane tabs = buscarComponente(frmRef.get().getContentPane(), JTabbedPane.class);
                JTable tCu08 = buscarTabla(tabs.getComponentAt(0));
                assertEquals(0, tCu08.getRowCount());
                assertEquals(EstadoSeccion.NO_DISPONIBLE_CONEXION, frmRef.get().getEstadoSeccion(SeccionPublica.FIXTURE));
            });
            assertEquals(1, dialogosOffline.size());
            assertEquals(JOptionPane.WARNING_MESSAGE, dialogosOffline.get(0).tipoMensaje);
            assertFalse(dialogosOffline.get(0).mensaje.contains("jdbc:postgresql"));

            // Error SQL 42P01 al consultar manualmente CU-08 (prefijo "No se pudo ")
            stub.modoFixture = ModoFallo.SQL_42P01;
            List<DialogoCapturado> dialogosSql = Collections.synchronizedList(new ArrayList<>());
            runEdtConCapturaDeDialogos(dialogosSql, () -> {
                JTabbedPane tabs = buscarComponente(frmRef.get().getContentPane(), JTabbedPane.class);
                buscarBoton(tabs.getComponentAt(0), "Consultar").doClick();
            }, () -> {
                assertEquals(EstadoSeccion.ERROR_CONSULTA, frmRef.get().getEstadoSeccion(SeccionPublica.FIXTURE));
            });
            assertEquals(1, dialogosSql.size());
            assertEquals(JOptionPane.ERROR_MESSAGE, dialogosSql.get(0).tipoMensaje);
            assertTrue(dialogosSql.get(0).mensaje.contains("does not exist"));
            assertTrue(dialogosSql.get(0).mensaje.startsWith("No se pudo consultar el fixture: ERROR:"),
                    "No debe duplicar el prefijo 'No se pudo consultar el fixture:' cuando ya viene del servicio");
            assertFalse(dialogosSql.get(0).mensaje.contains("jdbc:postgresql"));

            // Error SQL 42P01 al consultar manualmente CU-10 (prefijo "No se pudieron ")
            stub.modoEstadisticas = ModoFallo.SQL_42P01;
            List<DialogoCapturado> dialogosSqlCu10 = Collections.synchronizedList(new ArrayList<>());
            runEdtConCapturaDeDialogos(dialogosSqlCu10, () -> {
                JTabbedPane tabs = buscarComponente(frmRef.get().getContentPane(), JTabbedPane.class);
                buscarBoton(tabs.getComponentAt(2), "Consultar").doClick();
            }, () -> {
                assertEquals(EstadoSeccion.ERROR_CONSULTA, frmRef.get().getEstadoSeccion(SeccionPublica.ESTADISTICAS));
            });
            assertEquals(1, dialogosSqlCu10.size());
            assertEquals(JOptionPane.ERROR_MESSAGE, dialogosSqlCu10.get(0).tipoMensaje);
            assertTrue(dialogosSqlCu10.get(0).mensaje.contains("does not exist"));
            assertTrue(dialogosSqlCu10.get(0).mensaje.startsWith("No se pudieron consultar las estadisticas: ERROR:"),
                    "No debe duplicar el prefijo cuando el servicio inicia con 'No se pudieron '");
            assertFalse(dialogosSqlCu10.get(0).mensaje.contains("No se pudo consultar las estadísticas:"));
            assertFalse(dialogosSqlCu10.get(0).mensaje.contains("jdbc:postgresql"));
        } finally {
            disposeEnEdt(frmRef.get());
        }
    }

    @Test
    @DisplayName("Corte parcial en CU-10 (Estadísticas): secciones previas conservan datos y CU-11 queda cortada")
    void testInicializacionParcialCorteEnEstadisticas() {
        StubServicioControlable stub = new StubServicioControlable();
        stub.modoEstadisticas = ModoFallo.CONECTIVIDAD_08001;

        List<DialogoCapturado> dialogos = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<FrmConsultaPublica> frmRef = new AtomicReference<>();

        try {
            runEdtConCapturaDeDialogos(dialogos, () -> frmRef.set(new FrmConsultaPublica(null, stub)), () -> {
                FrmConsultaPublica frm = frmRef.get();
                assertEquals(0, stub.llamadasFases.get(), "No debe intentar CU-11 tras corte de conectividad en CU-10");
                assertEquals(EstadoSeccion.CON_DATOS, frm.getEstadoSeccion(SeccionPublica.TORNEOS));
                assertEquals(EstadoSeccion.CON_DATOS, frm.getEstadoSeccion(SeccionPublica.EQUIPOS));
                assertEquals(EstadoSeccion.CON_DATOS, frm.getEstadoSeccion(SeccionPublica.FIXTURE));
                assertEquals(EstadoSeccion.CON_DATOS, frm.getEstadoSeccion(SeccionPublica.PERFIL_EQUIPO));
                assertEquals(EstadoSeccion.NO_DISPONIBLE_CONEXION, frm.getEstadoSeccion(SeccionPublica.ESTADISTICAS));
                assertEquals(EstadoSeccion.NO_DISPONIBLE_CONEXION, frm.getEstadoSeccion(SeccionPublica.FASES));
            });
        } finally {
            disposeEnEdt(frmRef.get());
        }

        assertEquals(1, dialogos.size());
        assertEquals(JOptionPane.WARNING_MESSAGE, dialogos.get(0).tipoMensaje);
    }

    @Test
    @DisplayName("Consultar manual permaneciendo offline en CU-10 y CU-11: corta tras fallo de torneos sin llamadas redundantes")
    void testConsultarManualOfflineCu10YCu11SinLlamadaRedundante() {
        StubServicioControlable stub = new StubServicioControlable();
        stub.modoTorneos = ModoFallo.CONECTIVIDAD_08001;
        stub.modoEquipos = ModoFallo.CONECTIVIDAD_08001;
        stub.modoEstadisticas = ModoFallo.CONECTIVIDAD_08001;
        stub.modoFases = ModoFallo.CONECTIVIDAD_08001;

        List<DialogoCapturado> dialogosInit = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<FrmConsultaPublica> frmRef = new AtomicReference<>();
        try {
            runEdtConCapturaDeDialogos(dialogosInit, () -> frmRef.set(new FrmConsultaPublica(null, stub)), () -> {
                assertEquals(1, stub.llamadasTorneos.get());
                assertEquals(0, stub.llamadasEstadisticas.get());
                assertEquals(0, stub.llamadasFases.get());
            });
            assertEquals(1, dialogosInit.size());

            List<DialogoCapturado> dialogosCu10 = Collections.synchronizedList(new ArrayList<>());
            runEdtConCapturaDeDialogos(dialogosCu10, () -> {
                JTabbedPane tabs = buscarComponente(frmRef.get().getContentPane(), JTabbedPane.class);
                buscarBoton(tabs.getComponentAt(2), "Consultar").doClick();
            }, () -> {
                assertEquals(2, stub.llamadasTorneos.get());
                assertEquals(0, stub.llamadasEstadisticas.get(),
                        "CU-10 no debe intentar estadisticasDeJugadores si cargarTorneosSelector falla por conectividad");
                assertEquals(EstadoSeccion.NO_DISPONIBLE_CONEXION,
                        frmRef.get().getEstadoSeccion(SeccionPublica.ESTADISTICAS));
            });
            assertEquals(1, dialogosCu10.size());
            assertEquals(JOptionPane.WARNING_MESSAGE, dialogosCu10.get(0).tipoMensaje);

            List<DialogoCapturado> dialogosCu11 = Collections.synchronizedList(new ArrayList<>());
            runEdtConCapturaDeDialogos(dialogosCu11, () -> {
                JTabbedPane tabs = buscarComponente(frmRef.get().getContentPane(), JTabbedPane.class);
                buscarBoton(tabs.getComponentAt(3), "Consultar").doClick();
            }, () -> {
                assertEquals(3, stub.llamadasTorneos.get());
                assertEquals(0, stub.llamadasFases.get(),
                        "CU-11 no debe intentar avanceDeFases si cargarTorneosSelector falla por conectividad");
                assertEquals(EstadoSeccion.NO_DISPONIBLE_CONEXION,
                        frmRef.get().getEstadoSeccion(SeccionPublica.FASES));
            });
            assertEquals(1, dialogosCu11.size());
            assertEquals(JOptionPane.WARNING_MESSAGE, dialogosCu11.get(0).tipoMensaje);
        } finally {
            disposeEnEdt(frmRef.get());
        }
    }

    @Test
    @DisplayName("Error SQL 28P01 en equipos durante inicialización: marca ERROR_CONSULTA en EQUIPOS y PERFIL_EQUIPO sin llamar perfil(null)")
    void testErrorSql28P01EnEquiposNoClasificaComoDesconexion() {
        StubServicioControlable stub = new StubServicioControlable();
        stub.modoEquipos = ModoFallo.SQL_28P01;

        List<DialogoCapturado> dialogos = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<FrmConsultaPublica> frmRef = new AtomicReference<>();

        try {
            runEdtConCapturaDeDialogos(dialogos, () -> frmRef.set(new FrmConsultaPublica(null, stub)), () -> {
                FrmConsultaPublica frm = frmRef.get();
                assertEquals(0, stub.llamadasPerfil.get(),
                        "Si equipos fallo con 28P01, no debe llamar perfilPublicoDeEquipo(null)");
                assertEquals(EstadoSeccion.CON_DATOS, frm.getEstadoSeccion(SeccionPublica.TORNEOS));
                assertEquals(EstadoSeccion.ERROR_CONSULTA, frm.getEstadoSeccion(SeccionPublica.EQUIPOS));
                assertEquals(EstadoSeccion.CON_DATOS, frm.getEstadoSeccion(SeccionPublica.FIXTURE));
                assertEquals(EstadoSeccion.ERROR_CONSULTA, frm.getEstadoSeccion(SeccionPublica.PERFIL_EQUIPO));
                assertEquals(EstadoSeccion.CON_DATOS, frm.getEstadoSeccion(SeccionPublica.ESTADISTICAS));
                assertEquals(EstadoSeccion.CON_DATOS, frm.getEstadoSeccion(SeccionPublica.FASES));
            });
        } finally {
            disposeEnEdt(frmRef.get());
        }

        assertEquals(1, dialogos.size());
        assertEquals(JOptionPane.ERROR_MESSAGE, dialogos.get(0).tipoMensaje);
        assertFalse(dialogos.get(0).mensaje.contains("Debe seleccionar un equipo"));
        assertFalse(dialogos.get(0).mensaje.toLowerCase().contains("sin conexión"));
        assertTrue(dialogos.get(0).mensaje.contains("password authentication failed"));
    }

    @Test
    @DisplayName("Fallo mixto en inicialización (SQL 42P01 en torneos + 08001 en equipos): conserva ambos diagnósticos y estados diferenciados")
    void testFalloMixtoSqlEnTorneosYConectividadEnEquiposConservaAmbosDiagnosticos() {
        StubServicioControlable stub = new StubServicioControlable();
        stub.modoTorneos = ModoFallo.SQL_42P01;
        stub.modoEquipos = ModoFallo.CONECTIVIDAD_08001;

        List<DialogoCapturado> dialogos = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<FrmConsultaPublica> frmRef = new AtomicReference<>();

        try {
            runEdtConCapturaDeDialogos(dialogos, () -> frmRef.set(new FrmConsultaPublica(null, stub)), () -> {
                FrmConsultaPublica frm = frmRef.get();
                assertEquals(1, stub.llamadasTorneos.get());
                assertEquals(1, stub.llamadasEquipos.get());
                assertEquals(0, stub.llamadasFixture.get(), "Debe cortar tras fallo de conectividad en equipos");
                assertEquals(0, stub.llamadasPerfil.get());
                assertEquals(0, stub.llamadasEstadisticas.get());
                assertEquals(0, stub.llamadasFases.get());

                assertEquals(EstadoSeccion.ERROR_CONSULTA, frm.getEstadoSeccion(SeccionPublica.TORNEOS));
                assertEquals(EstadoSeccion.NO_DISPONIBLE_CONEXION, frm.getEstadoSeccion(SeccionPublica.EQUIPOS));
                assertEquals(EstadoSeccion.NO_DISPONIBLE_CONEXION, frm.getEstadoSeccion(SeccionPublica.FIXTURE));
                assertEquals(EstadoSeccion.NO_DISPONIBLE_CONEXION, frm.getEstadoSeccion(SeccionPublica.PERFIL_EQUIPO));
                assertEquals(EstadoSeccion.NO_DISPONIBLE_CONEXION, frm.getEstadoSeccion(SeccionPublica.ESTADISTICAS));
                assertEquals(EstadoSeccion.NO_DISPONIBLE_CONEXION, frm.getEstadoSeccion(SeccionPublica.FASES));
            });
        } finally {
            disposeEnEdt(frmRef.get());
        }

        assertEquals(1, dialogos.size(), "Debe emitir exactamente 1 notificación inicial consolidada");
        DialogoCapturado dlg = dialogos.get(0);
        assertEquals(JOptionPane.WARNING_MESSAGE, dlg.tipoMensaje);
        assertTrue(dlg.mensaje.contains("No hay conexión con la base de datos PostgreSQL."),
                "Debe informar el corte de conexión en secciones restantes");
        assertTrue(dlg.mensaje.contains("Equipos"), "Debe listar Equipos entre secciones no disponibles");
        assertTrue(dlg.mensaje.contains("Fixture (CU-08)"), "Debe listar Fixture entre secciones no disponibles");
        assertTrue(dlg.mensaje.contains("Avance de fases (CU-11)"),
                "Debe listar Avance de fases entre secciones no disponibles");
        assertTrue(dlg.mensaje.contains("Errores adicionales detectados:"),
                "Debe incluir el bloque de errores técnicos previos en fallo mixto");
        assertTrue(dlg.mensaje.contains("Torneos:"), "Debe identificar la sección Torneos en errores adicionales");
        assertTrue(dlg.mensaje.contains("does not exist"),
                "Debe conservar el diagnóstico SQL 42P01 sanitizado");
        assertFalse(dlg.mensaje.contains("jdbc:postgresql"), "No debe exponer URL JDBC");
        assertFalse(dlg.mensaje.contains("oculto"), "No debe exponer credenciales");
    }

    @Test
    @DisplayName("Observación A: Consultar manual en CU-08 continúa con fixture(null) ante error SQL 42P01 o lista vacía []")
    void testConsultarManualCu08ContinuaConFixtureNullAnteSql42P01OVacio() {
        // Caso 1: torneos() falla por error SQL no-conectividad (42P01) -> continúa hacia fixture(null)
        StubServicioControlable stubSql = new StubServicioControlable();
        stubSql.modoTorneos = ModoFallo.SQL_42P01;

        List<DialogoCapturado> dialogosSqlInit = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<FrmConsultaPublica> frmSqlRef = new AtomicReference<>();
        try {
            runEdtConCapturaDeDialogos(dialogosSqlInit, () -> frmSqlRef.set(new FrmConsultaPublica(null, stubSql)), () -> {
                assertEquals(EstadoSeccion.ERROR_CONSULTA, frmSqlRef.get().getEstadoSeccion(SeccionPublica.TORNEOS));
                assertEquals(1, stubSql.llamadasFixture.get());
            });
            assertEquals(1, dialogosSqlInit.size());

            List<DialogoCapturado> dialogosManual = Collections.synchronizedList(new ArrayList<>());
            runEdtConCapturaDeDialogos(dialogosManual, () -> {
                JTabbedPane tabs = buscarComponente(frmSqlRef.get().getContentPane(), JTabbedPane.class);
                buscarBoton(tabs.getComponentAt(0), "Consultar").doClick();
            }, () -> {
                assertEquals(2, stubSql.llamadasTorneos.get(), "Debe reintentar el selector nulo");
                assertEquals(2, stubSql.llamadasFixture.get(),
                        "Ante error SQL distinto de conectividad (42P01), debe continuar hacia fixture(null)");
                assertNull(stubSql.ultimoIdTorneoFixture.get(), "Debe invocar fixture(null) cuando torneos es nulo");
                assertEquals(EstadoSeccion.CON_DATOS, frmSqlRef.get().getEstadoSeccion(SeccionPublica.FIXTURE));
            });
            assertEquals(0, dialogosManual.size());
        } finally {
            disposeEnEdt(frmSqlRef.get());
        }

        // Caso 2: torneos es una lista vacía válida [] -> continúa hacia fixture(null) sin reintentar torneos
        StubServicioControlable stubVacio = new StubServicioControlable();
        stubVacio.devolverTorneosVacio = true;
        List<DialogoCapturado> dialogosVacioInit = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<FrmConsultaPublica> frmVacioRef = new AtomicReference<>();
        try {
            runEdtConCapturaDeDialogos(dialogosVacioInit, () -> frmVacioRef.set(new FrmConsultaPublica(null, stubVacio)), () -> {
                assertEquals(EstadoSeccion.VACIO_VALIDO, frmVacioRef.get().getEstadoSeccion(SeccionPublica.TORNEOS));
                assertEquals(1, stubVacio.llamadasFixture.get());
            });
            assertEquals(0, dialogosVacioInit.size());

            List<DialogoCapturado> dialogosVacioManual = Collections.synchronizedList(new ArrayList<>());
            runEdtConCapturaDeDialogos(dialogosVacioManual, () -> {
                JTabbedPane tabs = buscarComponente(frmVacioRef.get().getContentPane(), JTabbedPane.class);
                buscarBoton(tabs.getComponentAt(0), "Consultar").doClick();
            }, () -> {
                assertEquals(1, stubVacio.llamadasTorneos.get(), "Con lista vacia valida no debe recargar torneos");
                assertEquals(2, stubVacio.llamadasFixture.get(), "Debe ejecutar fixture(null) normalmente");
                assertNull(stubVacio.ultimoIdTorneoFixture.get());
                assertEquals(EstadoSeccion.CON_DATOS, frmVacioRef.get().getEstadoSeccion(SeccionPublica.FIXTURE));
            });
            assertEquals(0, dialogosVacioManual.size());
        } finally {
            disposeEnEdt(frmVacioRef.get());
        }
    }

    @Test
    @DisplayName("Observación B: latencia sintética controlada (~200 ms) en EDT no multiplica x5/x6 y notifica en EDT sin bloqueo")
    void testObservacionBLatenciaSinteticaEdtSinMultiplicacionYNotificacionEnEdt() {
        StubServicioControlable stub = new StubServicioControlable();
        stub.retardoMs = 200L;
        stub.modoTorneos = ModoFallo.CONECTIVIDAD_TIMEOUT;
        stub.modoEquipos = ModoFallo.CONECTIVIDAD_TIMEOUT;
        stub.modoFixture = ModoFallo.CONECTIVIDAD_TIMEOUT;
        stub.modoPerfil = ModoFallo.CONECTIVIDAD_TIMEOUT;
        stub.modoEstadisticas = ModoFallo.CONECTIVIDAD_TIMEOUT;
        stub.modoFases = ModoFallo.CONECTIVIDAD_TIMEOUT;

        AtomicBoolean torneosEnEdt = new AtomicBoolean(false);
        AtomicLong edtEventoPosteriorNs = new AtomicLong(0L);
        stub.alEntrarEnTorneos = () -> {
            torneosEnEdt.set(SwingUtilities.isEventDispatchThread());
            SwingUtilities.invokeLater(() -> edtEventoPosteriorNs.set(System.nanoTime()));
        };

        AtomicLong inicioConstruccionNs = new AtomicLong(0L);
        AtomicLong finConstruccionNs = new AtomicLong(0L);
        AtomicBoolean notificacionEnEdt = new AtomicBoolean(false);
        AtomicLong notificacionEjecutadaNs = new AtomicLong(0L);

        List<DialogoCapturado> dialogos = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<FrmConsultaPublica> frmRef = new AtomicReference<>();

        try {
            runEdtConCapturaDeDialogos(dialogos, () -> {
                inicioConstruccionNs.set(System.nanoTime());
                FrmConsultaPublica frm = new FrmConsultaPublica(null, stub) {
                    @Override
                    void mostrarDialogo(String titulo, String mensaje, int tipoMensaje) {
                        notificacionEnEdt.set(SwingUtilities.isEventDispatchThread());
                        notificacionEjecutadaNs.set(System.nanoTime());
                        super.mostrarDialogo(titulo, mensaje, tipoMensaje);
                    }
                };
                finConstruccionNs.set(System.nanoTime());
                frmRef.set(frm);
            }, () -> {
                FrmConsultaPublica frm = frmRef.get();
                assertNotNull(frm);

                long duracionMs = TimeUnit.NANOSECONDS.toMillis(finConstruccionNs.get() - inicioConstruccionNs.get());
                assertTrue(torneosEnEdt.get(), "La consulta inicial síncrona debe ejecutarse en el EDT");
                assertTrue(duracionMs >= 180L,
                        "La construcción en EDT debe esperar de forma síncrona el retardo sintético (actual=" + duracionMs + " ms)");
                assertTrue(duracionMs < 900L,
                        "El corte temprano no debe multiplicar la latencia x5/x6 (actual=" + duracionMs + " ms)");

                assertEquals(1, stub.llamadasTorneos.get(), "Solo debe ejecutar la llamada inicial de torneos");
                assertEquals(0, stub.llamadasEquipos.get(), "No debe multiplicar llamadas hacia equipos");
                assertEquals(0, stub.llamadasFixture.get(), "No debe multiplicar llamadas hacia fixture");
                assertEquals(0, stub.llamadasPerfil.get(), "No debe multiplicar llamadas hacia perfil");
                assertEquals(0, stub.llamadasEstadisticas.get(), "No debe multiplicar llamadas hacia estadisticas");
                assertEquals(0, stub.llamadasFases.get(), "No debe multiplicar llamadas hacia fases");

                assertTrue(edtEventoPosteriorNs.get() >= finConstruccionNs.get(),
                        "Un evento encolado en el EDT durante la llamada síncrona debe quedar retenido hasta finalizar el constructor");
                assertTrue(notificacionEnEdt.get(),
                        "La notificación consolidada debe ejecutarse en el EDT");
                assertTrue(notificacionEjecutadaNs.get() >= finConstruccionNs.get(),
                        "La notificación consolidada via invokeLater debe ejecutarse posteriormente a la construcción");

                for (SeccionPublica sec : SeccionPublica.values()) {
                    assertEquals(EstadoSeccion.NO_DISPONIBLE_CONEXION, frm.getEstadoSeccion(sec),
                            "Sección " + sec + " debe finalizar en NO_DISPONIBLE_CONEXION");
                }

                assertTrue(frm.isDisplayable(), "La ventana debe quedar inicializada y displayable antes de cerrar");
                frm.dispose();
                assertFalse(frm.isDisplayable(), "La ventana debe responder limpiamente al cierre con dispose()");
            });
        } finally {
            disposeEnEdt(frmRef.get());
        }

        assertEquals(1, dialogos.size(), "Debe emitirse exactamente 1 notificación consolidada sin bloqueo indefinido");
        assertEquals(JOptionPane.WARNING_MESSAGE, dialogos.get(0).tipoMensaje);
        assertTrue(dialogos.get(0).mensaje.contains("PostgreSQL"));
    }

    // ------------------------------------------------------------------
    // Helpers de Swing EDT y stubs
    // ------------------------------------------------------------------

    @FunctionalInterface
    private interface EdtAction {
        void run() throws Exception;
    }

    private static final class DialogoCapturado {
        final int tipoMensaje;
        final String mensaje;

        DialogoCapturado(int tipoMensaje, String mensaje) {
            this.tipoMensaje = tipoMensaje;
            this.mensaje = mensaje;
        }
    }

    private static void runEdtConCapturaDeDialogos(
            List<DialogoCapturado> dialogos,
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
                // Si accionEdt programo un JOptionPane via invokeLater, ese runnable se ejecuta antes de este
                // invokeAndWait y queda pausado en WaitDispatchSupport mientras el dialogo esta visible.
                // Al capturar y cerrar aqui, se desbloquea el dialogo sin destruir antes su ventana padre.
                SwingUtilities.invokeAndWait(() -> capturarYCerrarDialogosModales(dialogos));
                // Con cualquier invokeLater ya completado, ejecutar verificacion y limpieza final.
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

    private static void capturarYCerrarDialogosModales(List<DialogoCapturado> dialogos) {
        for (Window w : Window.getWindows()) {
            if (w instanceof JDialog d && d.isShowing() && d.getModalityType() != Dialog.ModalityType.MODELESS) {
                JOptionPane pane = buscarComponente(d, JOptionPane.class);
                if (pane != null) {
                    Object msg = pane.getMessage();
                    dialogos.add(new DialogoCapturado(pane.getMessageType(), msg != null ? String.valueOf(msg) : ""));
                } else {
                    dialogos.add(new DialogoCapturado(-1, ""));
                }
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

    private static boolean contieneSqlState(Throwable t, String esperado) {
        Throwable cur = t;
        int depth = 0;
        while (cur != null && depth < 16) {
            if (cur instanceof SQLException sql && esperado.equals(sql.getSQLState())) {
                return true;
            }
            cur = cur.getCause();
            depth++;
        }
        return false;
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
            // Limpieza best-effort en finally de pruebas
        }
    }

    private enum ModoFallo {
        NINGUNO,
        CONECTIVIDAD_08001,
        CONECTIVIDAD_08006,
        CONECTIVIDAD_TIMEOUT,
        SQL_42P01,
        SQL_28P01
    }

    private static final class StubServicioControlable extends ConsultaPublicaService {
        volatile ModoFallo modoTorneos = ModoFallo.NINGUNO;
        volatile ModoFallo modoEquipos = ModoFallo.NINGUNO;
        volatile ModoFallo modoFixture = ModoFallo.NINGUNO;
        volatile ModoFallo modoPerfil = ModoFallo.NINGUNO;
        volatile ModoFallo modoEstadisticas = ModoFallo.NINGUNO;
        volatile ModoFallo modoFases = ModoFallo.NINGUNO;
        volatile boolean devolverVacio = false;
        volatile boolean devolverTorneosVacio = false;
        volatile long retardoMs = 0L;
        volatile Runnable alEntrarEnTorneos = null;

        final AtomicInteger llamadasTorneos = new AtomicInteger();
        final AtomicInteger llamadasEquipos = new AtomicInteger();
        final AtomicInteger llamadasFixture = new AtomicInteger();
        final AtomicInteger llamadasPerfil = new AtomicInteger();
        final AtomicInteger llamadasEstadisticas = new AtomicInteger();
        final AtomicInteger llamadasFases = new AtomicInteger();
        final AtomicReference<Long> ultimoIdTorneoFixture = new AtomicReference<>();

        StubServicioControlable() {
            super(new ConsultaPublicaDAO());
        }

        private void dormirMinimoSiCorresponde() {
            long ms = retardoMs;
            if (ms <= 0L) {
                return;
            }
            long finNs = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(ms);
            while (true) {
                long restanteNs = finNs - System.nanoTime();
                if (restanteNs <= 0L) {
                    break;
                }
                try {
                    long sleepMs = Math.max(1L, TimeUnit.NANOSECONDS.toMillis(restanteNs));
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        private void lanzarSiCorresponde(ModoFallo modo, String accion) throws TorneoExcepcion {
            dormirMinimoSiCorresponde();
            String prefijoConsulta = (accion.startsWith("los ") || accion.startsWith("las "))
                    ? "No se pudieron consultar "
                    : "No se pudo consultar ";
            switch (modo) {
                case CONECTIVIDAD_08001:
                    throw new TorneoExcepcion(
                            prefijoConsulta + accion
                                    + ": No se pudo conectar con 'jdbc:postgresql://127.0.0.1:55432/tpi_pa2_r1_demo?password=SecretaClave'",
                            new SQLException("Connection refused", "08001", new ConnectException("Connection refused")));
                case CONECTIVIDAD_08006:
                    throw new TorneoExcepcion(
                            prefijoConsulta + accion + ": Connection failure",
                            new SQLException("Connection failure", "08006"));
                case CONECTIVIDAD_TIMEOUT:
                    throw new TorneoExcepcion(
                            prefijoConsulta + accion + ": Read timed out",
                            new SQLException("The connection attempt failed.", "08001",
                                    new SocketTimeoutException("connect timed out")));
                case SQL_42P01:
                    throw new TorneoExcepcion(
                            prefijoConsulta + accion
                                    + ": ERROR: relation \"partido\" does not exist (jdbc:postgresql://127.0.0.1:55432/demo?pwd=oculto)",
                            new SQLException("ERROR: relation \"partido\" does not exist", "42P01"));
                case SQL_28P01:
                    throw new TorneoExcepcion(
                            prefijoConsulta + accion
                                    + ": FATAL: password authentication failed for user \"postgres\"",
                            new SQLException("FATAL: password authentication failed", "28P01"));
                case NINGUNO:
                default:
                    break;
            }
        }

        @Override
        public List<Object[]> torneos() throws TorneoExcepcion {
            llamadasTorneos.incrementAndGet();
            Runnable hook = alEntrarEnTorneos;
            if (hook != null) {
                hook.run();
            }
            lanzarSiCorresponde(modoTorneos, "los torneos");
            if (devolverVacio || devolverTorneosVacio) {
                return List.of();
            }
            return List.of(
                    new Object[]{10L, "Torneo Inicial", "FINALIZADO", "LIGA", 8},
                    new Object[]{42L, "Apertura 2026", "EN_CURSO", "LIGA", 10}
            );
        }

        @Override
        public List<Object[]> equipos() throws TorneoExcepcion {
            llamadasEquipos.incrementAndGet();
            lanzarSiCorresponde(modoEquipos, "los equipos");
            if (devolverVacio) {
                return List.of();
            }
            return List.<Object[]>of(
                    new Object[]{7L, "Atletico Guarani", "Libre"}
            );
        }

        @Override
        public List<Object[]> fixture(Long idTorneo) throws TorneoExcepcion {
            llamadasFixture.incrementAndGet();
            ultimoIdTorneoFixture.set(idTorneo);
            lanzarSiCorresponde(modoFixture, "el fixture");
            if (devolverVacio) {
                return List.of();
            }
            return List.of(
                    new Object[]{"18/11/2026 20:30", "PROGRAMADO", "Atletico Guarani", "Deportivo Posadas",
                            "0 - 0", "Cancha Norte", "Fecha 1", "Apertura 2026"},
                    new Object[]{"25/11/2026 18:00", "FINALIZADO", "Club Misiones", "Real Obera",
                            "3 - 1", "Estadio Central", "Cuartos", "Clausura 2026"}
            );
        }

        @Override
        public List<Object[]> perfilPublicoDeEquipo(Long idEquipo) throws TorneoExcepcion {
            llamadasPerfil.incrementAndGet();
            if (idEquipo == null) {
                throw new TorneoExcepcion("Debe seleccionar un equipo.");
            }
            lanzarSiCorresponde(modoPerfil, "el equipo");
            if (devolverVacio) {
                return List.of();
            }
            return List.<Object[]>of(
                    new Object[]{"Atletico Guarani", "Libre", 4, 3, 1, 0, 9, 3, 18}
            );
        }

        @Override
        public List<Object[]> estadisticasDeJugadores(Long idTorneo) throws TorneoExcepcion {
            llamadasEstadisticas.incrementAndGet();
            lanzarSiCorresponde(modoEstadisticas, "las estadisticas");
            if (devolverVacio) {
                return List.of();
            }
            return List.<Object[]>of(
                    new Object[]{"Juan Perez", "Atletico Guarani", 5, 1, 0, 4}
            );
        }

        @Override
        public List<Object[]> avanceDeFases(Long idTorneo) throws TorneoExcepcion {
            llamadasFases.incrementAndGet();
            lanzarSiCorresponde(modoFases, "el avance de fases");
            if (devolverVacio) {
                return List.of();
            }
            return List.<Object[]>of(
                    new Object[]{"Apertura 2026", "EN_CURSO", "Fecha 1", 1, "No", 2, 1}
            );
        }
    }
}
