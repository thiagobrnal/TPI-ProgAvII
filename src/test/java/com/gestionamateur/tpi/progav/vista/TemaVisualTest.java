package com.gestionamateur.tpi.progav.vista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gestionamateur.tpi.progav.dao.ConexionBD;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.servicio.ConsultaPublicaService;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Verificación objetiva del sistema visual Dark Stadium Slate ({@link EstilosUI}):
 * ninguna ventana debe conservar superficies blancas accidentales y los textos visibles
 * y tokens cromáticos deben alcanzar el contraste mínimo de WCAG 2.1 AA.
 */
@DisplayName("Sistema visual: tema oscuro, sin superficies blancas y con contraste WCAG 2.1 AA")
class TemaVisualTest {

    /** Contraste mínimo WCAG 2.1 AA para texto normal. */
    private static final double MINIMO_TEXTO_NORMAL = 4.5;
    /** Contraste mínimo WCAG 2.1 AA para texto grande (>=18 pt, o >=14 pt en negrita) y elementos gráficos. */
    private static final double MINIMO_TEXTO_GRANDE_O_GRAFICO = 3.0;
    /** Luminancia relativa máxima admisible para superficies oscuras de pantalla. */
    private static final double LUMINANCIA_MAX_SUPERFICIE_OSCURA = 0.06;

    private static boolean entornoCapturado = false;
    private static String propModoPrevio;
    private static String propTestUrlPrevia;
    private static String propDbUrlPrevia;

    private final List<String> fallosConstruccion = new ArrayList<>();
    private final List<String> fallosContraste = new ArrayList<>();
    private final List<String> fallosSuperficie = new ArrayList<>();
    private int ventanasAnalizadas = 0;
    private int etiquetasAnalizadas = 0;

    @BeforeAll
    static void instalarTema() {
        propModoPrevio = System.getProperty(ConexionBD.PROP_MODO_EJECUCION);
        propTestUrlPrevia = System.getProperty(ConexionBD.ENV_TEST_DB_URL);
        propDbUrlPrevia = System.getProperty(ConexionBD.ENV_DB_URL);
        entornoCapturado = true;
        try {
            System.clearProperty(ConexionBD.PROP_MODO_EJECUCION);
            System.clearProperty(ConexionBD.ENV_TEST_DB_URL);
            System.clearProperty(ConexionBD.ENV_DB_URL);
            // Permanece explicitamente en NO_DEFINIDO (fail-closed) para impedir todo acceso JDBC aunque el SO defina URLs.
            ConexionBD.restablecerModoPorDefecto();
            inicializarTemaEnEdt();
        } catch (Throwable t) {
            restaurarEntorno();
            throw t;
        }
    }

    private static void inicializarTemaEnEdt() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            EstilosUI.inicializarTema();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(EstilosUI::inicializarTema);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo inicializar el tema visual en el EDT", e);
        }
    }

    @AfterAll
    static void restaurarEntorno() {
        try {
            if (entornoCapturado) {
                restaurarPropiedad(ConexionBD.PROP_MODO_EJECUCION, propModoPrevio);
                restaurarPropiedad(ConexionBD.ENV_TEST_DB_URL, propTestUrlPrevia);
                restaurarPropiedad(ConexionBD.ENV_DB_URL, propDbUrlPrevia);
            }
        } finally {
            entornoCapturado = false;
            ConexionBD.restablecerModoPorDefecto();
        }
    }

    private static void restaurarPropiedad(String clave, String valor) {
        if (valor == null) {
            System.clearProperty(clave);
        } else {
            System.setProperty(clave, valor);
        }
    }

    @Test
    @DisplayName("Ninguna ventana conserva superficies blancas y todo el texto visible cumple WCAG 2.1 AA")
    void temaOscuroAplicadoEnTodasLasVentanas() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "TemaVisualTest requiere entorno grafico disponible (no headless).");
        assertFalse(ConexionBD.esProcesoDePruebas(),
                "TemaVisualTest debe ejecutarse en modo fail-closed NO_DEFINIDO sin activar PRUEBAS.");

        List<String> dialogosDetectados = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(SwingUtilities.isEventDispatchThread(),
                    "La inspeccion de componentes Swing debe ejecutarse dentro del EDT.");
            List<JFrame> ventanas = new ArrayList<>();
            try {
                ventanas.addAll(construirVentanas());
                for (JFrame ventana : ventanas) {
                    ventanasAnalizadas++;
                    revisar(ventana.getContentPane(), ventana.getTitle());
                }
            } finally {
                for (Window w : Window.getWindows()) {
                    if (w instanceof JDialog && w.isShowing()) {
                        dialogosDetectados.add(((JDialog) w).getTitle());
                    }
                }
                for (JFrame ventana : ventanas) {
                    ventana.dispose();
                }
                for (Window w : Window.getWindows()) {
                    if (w.isDisplayable()) {
                        w.dispose();
                    }
                }
            }
        });

        // Vaciado posterior del EDT para detectar cualquier invokeLater tardio (e.g. JOptionPane diferido)
        SwingUtilities.invokeAndWait(() -> {
            for (Window w : Window.getWindows()) {
                if (w instanceof JDialog && w.isShowing()) {
                    dialogosDetectados.add(((JDialog) w).getTitle());
                    w.dispose();
                }
            }
        });

        assertTrue(dialogosDetectados.isEmpty(),
                "No deben abrirse dialogos modales/informativos durante la inspeccion offline: " + dialogosDetectados);
        assertTrue(fallosConstruccion.isEmpty(),
                "Ventanas que fallaron al construirse en aislamiento offline: " + fallosConstruccion);
        assertEquals(17, ventanasAnalizadas,
                "Se esperaba construir e inspeccionar las 17 ventanas, se analizaron " + ventanasAnalizadas);
        assertTrue(fallosSuperficie.isEmpty(),
                "Componentes que conservan fondo blanco: " + fallosSuperficie);
        assertTrue(fallosContraste.isEmpty(),
                "Etiquetas con contraste insuficiente: " + fallosContraste);
        assertTrue(etiquetasAnalizadas > 50,
                "Se esperaban mas de 50 etiquetas analizadas, se analizaron " + etiquetasAnalizadas);
    }

    @Test
    @DisplayName("INC-1-CORR-03 (H-05/H-09): TemaVisualTest permanece en NO_DEFINIDO DB-free y restaura propiedades y modo de forma determinista")
    void aislamientoOfflineYRestauracionDeterminista() {
        assertFalse(ConexionBD.esProcesoDePruebas(),
                "TemaVisualTest no debe activar PRUEBAS durante su ciclo de vida.");
        java.sql.SQLException exConn = assertThrows(java.sql.SQLException.class, ConexionBD::obtenerConexion);
        assertTrue(exConn.getMessage().contains("Modo de ejecucion no definido"),
                "En modo NO_DEFINIDO toda conexion debe rechazarse sin intentar abrir sockets JDBC: " + exConn.getMessage());
        assertThrows(java.sql.SQLException.class, ConexionBD::obtenerConexionTest);

        // Verificar captura previa y restauracion determinista ante ciclo completo instalarTema/restaurarEntorno
        String backupModo = propModoPrevio;
        String backupTestUrl = propTestUrlPrevia;
        String backupDbUrl = propDbUrlPrevia;
        boolean backupCapturado = entornoCapturado;
        try {
            System.setProperty(ConexionBD.PROP_MODO_EJECUCION, "pruebas");
            System.setProperty(ConexionBD.ENV_TEST_DB_URL, "jdbc:postgresql://127.0.0.1:59999/prop_previa_test");
            System.setProperty(ConexionBD.ENV_DB_URL, "jdbc:postgresql://127.0.0.1:59999/prop_previa_app");
            ConexionBD.restablecerModoPorDefecto();
            assertTrue(ConexionBD.esProcesoDePruebas());

            instalarTema();
            assertNull(System.getProperty(ConexionBD.PROP_MODO_EJECUCION));
            assertNull(System.getProperty(ConexionBD.ENV_TEST_DB_URL));
            assertNull(System.getProperty(ConexionBD.ENV_DB_URL));
            assertFalse(ConexionBD.esProcesoDePruebas());

            restaurarEntorno();
            assertEquals("pruebas", System.getProperty(ConexionBD.PROP_MODO_EJECUCION));
            assertEquals("jdbc:postgresql://127.0.0.1:59999/prop_previa_test",
                    System.getProperty(ConexionBD.ENV_TEST_DB_URL));
            assertEquals("jdbc:postgresql://127.0.0.1:59999/prop_previa_app",
                    System.getProperty(ConexionBD.ENV_DB_URL));
            assertTrue(ConexionBD.esProcesoDePruebas());
        } finally {
            System.clearProperty(ConexionBD.PROP_MODO_EJECUCION);
            System.clearProperty(ConexionBD.ENV_TEST_DB_URL);
            System.clearProperty(ConexionBD.ENV_DB_URL);
            propModoPrevio = backupModo;
            propTestUrlPrevia = backupTestUrl;
            propDbUrlPrevia = backupDbUrl;
            entornoCapturado = backupCapturado;
            ConexionBD.restablecerModoPorDefecto();
        }
    }

    @Test
    @DisplayName("La paleta Dark Stadium Slate de EstilosUI mantiene superficies oscuras y cumple WCAG 2.1 AA")
    void paletaDelTemaCumpleWcag() {
        // Superficies principales genuinamente oscuras
        assertTrue(luminancia(EstilosUI.BG_CANVAS) < LUMINANCIA_MAX_SUPERFICIE_OSCURA);
        assertTrue(luminancia(EstilosUI.BG_HEADER) < LUMINANCIA_MAX_SUPERFICIE_OSCURA);
        assertTrue(luminancia(EstilosUI.BG_SURFACE) < LUMINANCIA_MAX_SUPERFICIE_OSCURA);
        assertTrue(luminancia(EstilosUI.BG_CONTROL) < LUMINANCIA_MAX_SUPERFICIE_OSCURA);
        assertTrue(luminancia(EstilosUI.BG_TABLE) < LUMINANCIA_MAX_SUPERFICIE_OSCURA);
        assertTrue(luminancia(EstilosUI.BG_TABLE_ALT) < LUMINANCIA_MAX_SUPERFICIE_OSCURA);
        assertTrue(luminancia(EstilosUI.BG_SELECTION) < LUMINANCIA_MAX_SUPERFICIE_OSCURA);

        // Texto normal primario sobre todas las superficies de lectura e interacción (>= 4.5:1)
        assertTrue(contraste(EstilosUI.TEXT_PRIMARY, EstilosUI.BG_CANVAS) >= MINIMO_TEXTO_NORMAL);
        assertTrue(contraste(EstilosUI.TEXT_PRIMARY, EstilosUI.BG_HEADER) >= MINIMO_TEXTO_NORMAL);
        assertTrue(contraste(EstilosUI.TEXT_PRIMARY, EstilosUI.BG_SURFACE) >= MINIMO_TEXTO_NORMAL);
        assertTrue(contraste(EstilosUI.TEXT_PRIMARY, EstilosUI.BG_CONTROL) >= MINIMO_TEXTO_NORMAL);
        assertTrue(contraste(EstilosUI.TEXT_PRIMARY, EstilosUI.BG_TABLE) >= MINIMO_TEXTO_NORMAL);
        assertTrue(contraste(EstilosUI.TEXT_PRIMARY, EstilosUI.BG_TABLE_ALT) >= MINIMO_TEXTO_NORMAL);
        assertTrue(contraste(EstilosUI.TEXT_PRIMARY, EstilosUI.BG_SELECTION) >= MINIMO_TEXTO_NORMAL);

        // Texto normal secundario sobre fondos de formulario/tarjeta (>= 4.5:1)
        assertTrue(contraste(EstilosUI.TEXT_SECONDARY, EstilosUI.BG_CANVAS) >= MINIMO_TEXTO_NORMAL);
        assertTrue(contraste(EstilosUI.TEXT_SECONDARY, EstilosUI.BG_HEADER) >= MINIMO_TEXTO_NORMAL);
        assertTrue(contraste(EstilosUI.TEXT_SECONDARY, EstilosUI.BG_SURFACE) >= MINIMO_TEXTO_NORMAL);
        assertTrue(contraste(EstilosUI.TEXT_SECONDARY, EstilosUI.BG_CONTROL) >= MINIMO_TEXTO_NORMAL);

        // Botones semánticos (texto 12pt bold -> umbral normal 4.5:1)
        Color bgNeutro = new Color(0x25, 0x33, 0x47);
        assertTrue(contraste(Color.WHITE, EstilosUI.ACTION_PRIMARY) >= MINIMO_TEXTO_NORMAL);
        assertTrue(contraste(Color.WHITE, EstilosUI.ACTION_SECONDARY) >= MINIMO_TEXTO_NORMAL);
        assertTrue(contraste(EstilosUI.TEXT_ON_LIGHT_ACCENT, EstilosUI.ACTION_WARNING) >= MINIMO_TEXTO_NORMAL);
        assertTrue(contraste(Color.WHITE, EstilosUI.ACTION_DANGER) >= MINIMO_TEXTO_NORMAL);
        assertTrue(contraste(EstilosUI.TEXT_PRIMARY, bgNeutro) >= MINIMO_TEXTO_NORMAL);

        // Indicadores de acento y estados en tabla/encabezado
        assertTrue(contraste(EstilosUI.ACTION_ACCENT, EstilosUI.BG_SURFACE) >= MINIMO_TEXTO_NORMAL);
        assertTrue(contraste(EstilosUI.ACTION_ACCENT, EstilosUI.BG_TABLE) >= MINIMO_TEXTO_NORMAL);
        assertTrue(contraste(EstilosUI.ACTION_WARNING, EstilosUI.BG_TABLE) >= MINIMO_TEXTO_NORMAL);
        assertTrue(contraste(EstilosUI.ACTION_ACCENT, EstilosUI.BG_HEADER) >= MINIMO_TEXTO_GRANDE_O_GRAFICO);

        assertNotNull(EstilosUI.ACTION_DANGER);
        assertNotEquals(EstilosUI.ACTION_DANGER, EstilosUI.BG_CANVAS);
    }

    @Test
    @DisplayName("INC-1-CORR-04 (IMP-01): FrmReportes en modo visual offline habilita controles sin lanzar SwingWorkers ni dialogos")
    void testModoPruebaFrmReportesSinWorkersNiDialogos() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Requiere entorno grafico disponible para construir FrmReportes.");
        List<Boolean> botonesHabilitados = new ArrayList<>();
        List<String> dialogos = new ArrayList<>();

        SwingUtilities.invokeAndWait(() -> {
            FrmReportes frame = new FrmReportes(operadorPersistido(), false);
            try {
                registrarEstadoBotones(frame.getContentPane(), botonesHabilitados);
                for (Window w : Window.getWindows()) {
                    if (w instanceof JDialog && w.isShowing()) {
                        dialogos.add(((JDialog) w).getTitle());
                    }
                }
            } finally {
                frame.dispose();
            }
        });

        SwingUtilities.invokeAndWait(() -> {
            for (Window w : Window.getWindows()) {
                if (w instanceof JDialog && w.isShowing()) {
                    dialogos.add(((JDialog) w).getTitle());
                    w.dispose();
                }
            }
        });

        assertFalse(botonesHabilitados.isEmpty(), "FrmReportes debe exponer botones de reporte para inspeccion visual.");
        assertTrue(botonesHabilitados.stream().allMatch(Boolean::booleanValue),
                "Con operadorPersistido() los botones de FrmReportes deben quedar habilitados.");
        assertTrue(dialogos.isEmpty(),
                "FrmReportes(op, false) no debe disparar carga asincrona ni dialogos de error: " + dialogos);
        assertThrows(java.sql.SQLException.class, ConexionBD::obtenerConexion,
                "La construccion offline no debe alterar el modo fail-closed NO_DEFINIDO.");
    }

    private static void registrarEstadoBotones(Container contenedor, List<Boolean> estados) {
        for (Component c : contenedor.getComponents()) {
            if (c instanceof JButton) {
                JButton b = (JButton) c;
                String texto = b.getText();
                if ("Actualizar".equals(texto) || "Exportar a PDF".equals(texto) || "Exportar a CSV".equals(texto)) {
                    estados.add(b.isEnabled());
                }
            }
            if (c instanceof Container) {
                registrarEstadoBotones((Container) c, estados);
            }
        }
    }

    private static Usuario operador() {
        try {
            return new Usuario("admin_visual_test", "clave_sintetica", "visual@test.local",
                    "Administrador", "Visual", TiposDatos.Rol.ADMINISTRADOR);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo construir operador sintetico de prueba", e);
        }
    }

    private static Usuario operadorPersistido() {
        Usuario u = operador();
        u.setId(1L);
        return u;
    }

    private static ConsultaPublicaService servicioPublicoOffline() {
        return new ConsultaPublicaService(null) {
            @Override
            public List<Object[]> fixture(Long idTorneo) {
                return Collections.emptyList();
            }

            @Override
            public List<Object[]> perfilPublicoDeEquipo(Long idEquipo) {
                return Collections.emptyList();
            }

            @Override
            public List<Object[]> estadisticasDeJugadores(Long idTorneo) {
                return Collections.emptyList();
            }

            @Override
            public List<Object[]> avanceDeFases(Long idTorneo) {
                return Collections.emptyList();
            }

            @Override
            public List<Object[]> torneos() {
                return Collections.emptyList();
            }

            @Override
            public List<Object[]> equipos() {
                return Collections.emptyList();
            }
        };
    }

    private List<JFrame> construirVentanas() {
        List<JFrame> ventanas = new ArrayList<>();
        agregar(ventanas, "FrmLogin", () -> new FrmLogin());
        agregar(ventanas, "FrmRegistroUsuario", () -> new FrmRegistroUsuario());
        agregar(ventanas, "FrmPrincipal", () -> new FrmPrincipal());
        agregar(ventanas, "FrmAltaEquipo", () -> new FrmAltaEquipo());
        agregar(ventanas, "FrmAltaJugador", () -> new FrmAltaJugador());
        agregar(ventanas, "FrmAltaCancha", () -> new FrmAltaCancha());
        agregar(ventanas, "FrmProgramarPartido", () -> new FrmProgramarPartido());
        agregar(ventanas, "FrmCargarResultado", () -> new FrmCargarResultado(operador()));
        agregar(ventanas, "FrmSuspenderPartido", () -> new FrmSuspenderPartido(operador()));
        agregar(ventanas, "FrmTribunal", () -> new FrmTribunal(operador()));
        agregar(ventanas, "FrmReclamos", () -> new FrmReclamos(operador()));
        agregar(ventanas, "FrmTorneos", () -> new FrmTorneos(operador()));
        agregar(ventanas, "FrmGestorUsuarios", () -> new FrmGestorUsuarios(operador()));
        agregar(ventanas, "FrmConfiguracion", () -> new FrmConfiguracion(operador()));
        agregar(ventanas, "FrmReportes", () -> new FrmReportes(operadorPersistido(), false));
        agregar(ventanas, "FrmConsultaPublica", () -> new FrmConsultaPublica(null, servicioPublicoOffline()));
        agregar(ventanas, "FrmCredencialJugador", () -> new FrmCredencialJugador());
        return ventanas;
    }

    private void agregar(List<JFrame> ventanas, String nombreVentana, java.util.function.Supplier<JFrame> constructor) {
        try {
            ventanas.add(constructor.get());
        } catch (RuntimeException e) {
            fallosConstruccion.add(nombreVentana + " -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private void revisar(Component componente, String contexto) {
        if (componente == null) {
            return;
        }
        if (esDocumentoImprimible(componente)) {
            return;
        }
        if (componente instanceof JPanel && componente.isOpaque()) {
            Color fondo = componente.getBackground();
            if (fondo != null && esBlanco(fondo)) {
                fallosSuperficie.add(contexto + " -> " + componente.getClass().getSimpleName());
            }
        }
        if (componente instanceof JLabel) {
            revisarEtiqueta((JLabel) componente, contexto);
        } else if (componente instanceof JButton && componente.isEnabled()) {
            revisarBoton((JButton) componente, contexto);
        }
        if (componente instanceof Container) {
            for (Component hijo : ((Container) componente).getComponents()) {
                revisar(hijo, contexto);
            }
        }
    }

    private static boolean esDocumentoImprimible(Component componente) {
        if ("PanelCredencial".equals(componente.getClass().getSimpleName())) {
            return true;
        }
        if (componente instanceof javax.swing.JComponent) {
            return Boolean.TRUE.equals(((javax.swing.JComponent) componente)
                    .getClientProperty("tpi.documento.imprimible"));
        }
        return false;
    }

    private void revisarEtiqueta(JLabel etiqueta, String contexto) {
        if (!etiqueta.isEnabled()) {
            return;
        }
        etiquetasAnalizadas++;
        Color primerColor = etiqueta.getForeground();
        if (primerColor == null) {
            primerColor = UIManager.getColor("Label.foreground");
        }
        Color fondo = fondoEfectivo(etiqueta);
        if (primerColor == null || fondo == null) {
            return;
        }
        double contraste = contraste(primerColor, fondo);
        double minimo = esTextoGrande(etiqueta.getFont())
                ? MINIMO_TEXTO_GRANDE_O_GRAFICO
                : MINIMO_TEXTO_NORMAL;
        if (contraste < minimo) {
            fallosContraste.add(String.format("%s -> '%s' contraste %.2f:1 (minimo %.1f:1) sobre %s",
                    contexto, etiqueta.getText(), contraste, minimo, toHex(fondo)));
        }
    }

    private void revisarBoton(JButton boton, String contexto) {
        String txt = boton.getText();
        if (txt == null || txt.isBlank()) {
            return;
        }
        Color fg = boton.getForeground();
        Color bg = boton.getBackground();
        if (fg == null || bg == null) {
            return;
        }
        double c = contraste(fg, bg);
        double min = esTextoGrande(boton.getFont()) ? MINIMO_TEXTO_GRANDE_O_GRAFICO : MINIMO_TEXTO_NORMAL;
        if (c < min && !fg.equals(EstilosUI.TEXT_DISABLED)) {
            fallosContraste.add(String.format("%s -> Boton '%s' contraste %.2f:1 (minimo %.1f:1) sobre %s",
                    contexto, txt, c, min, toHex(bg)));
        }
    }

    private static Color fondoEfectivo(Component componente) {
        Component actual = componente;
        while (actual != null) {
            if (actual.isOpaque()) {
                Color fondo = actual.getBackground();
                if (fondo != null) {
                    return fondo;
                }
            }
            actual = actual.getParent();
        }
        return UIManager.getColor("Panel.background");
    }

    private static boolean esTextoGrande(Font fuente) {
        if (fuente == null) {
            return false;
        }
        return fuente.getSize() >= 18 || (fuente.getSize() >= 14 && fuente.isBold());
    }

    private static boolean esBlanco(Color color) {
        return color.getRed() >= 245 && color.getGreen() >= 245 && color.getBlue() >= 245;
    }

    private static String toHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static double luminancia(Color color) {
        return 0.2126 * lineal(color.getRed() / 255.0)
                + 0.7152 * lineal(color.getGreen() / 255.0)
                + 0.0722 * lineal(color.getBlue() / 255.0);
    }

    private static double lineal(double componente) {
        if (componente <= 0.03928) {
            return componente / 12.92;
        }
        return Math.pow((componente + 0.055) / 1.055, 2.4);
    }

    static double contraste(Color primerPlano, Color fondo) {
        double l1 = luminancia(primerPlano);
        double l2 = luminancia(fondo);
        if (l1 < l2) {
            double temporal = l1;
            l1 = l2;
            l2 = temporal;
        }
        return (l1 + 0.05) / (l2 + 0.05);
    }
}
