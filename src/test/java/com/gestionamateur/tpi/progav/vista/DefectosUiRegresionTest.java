package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.modelo.Jugador;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.AbstractButton;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Window;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de regresión DB-free para los 5 defectos UI identificados en la validación
 * interactiva post migración Dark Stadium Slate.
 */
@DisplayName("Regresión UI - 5 defectos corregidos post migración")
public class DefectosUiRegresionTest {

    @Test
    @DisplayName("DEF-UI-01: FrmTribunal alinea Gravedad: en la misma fila que txtGravedad")
    void testFrmTribunalAlineacionGravedad() {
        runEdt(() -> {
            FrmTribunal frm = new FrmTribunal(usuarioAdmin());
            try {
                frm.pack();
                JLabel lblGravedad = buscarEtiqueta(frm.getContentPane(), "Gravedad:");
                assertNotNull(lblGravedad, "Debe existir la etiqueta Gravedad:");
                JTextField txtGravedad = obtenerCampo(frm, "txtGravedad", JTextField.class);
                assertNotNull(txtGravedad, "Debe existir txtGravedad");

                Container contenedor = lblGravedad.getParent();
                assertSame(contenedor, txtGravedad.getParent(),
                        "lblGravedad y txtGravedad deben compartir el mismo contenedor");
                assertTrue(contenedor.getLayout() instanceof GridBagLayout);
                GridBagLayout layout = (GridBagLayout) contenedor.getLayout();
                GridBagConstraints cLbl = layout.getConstraints(lblGravedad);
                GridBagConstraints cTxt = layout.getConstraints(txtGravedad);

                assertEquals(cLbl.gridy, cTxt.gridy, "lblGravedad y txtGravedad deben compartir fila");
                assertEquals(0, cLbl.gridx, "lblGravedad debe ubicarse en la columna de etiquetas");
                assertEquals(1, cTxt.gridx, "txtGravedad debe iniciar en la columna de campos");

                Point pLbl = SwingUtilities.convertPoint(lblGravedad.getParent(), lblGravedad.getLocation(), frm.getContentPane());
                Point pTxt = SwingUtilities.convertPoint(txtGravedad.getParent(), txtGravedad.getLocation(), frm.getContentPane());
                assertTrue(Math.abs(pLbl.y - pTxt.y) <= 10,
                        "La coordenada vertical visual de Gravedad: y txtGravedad debe coincidir");
            } finally {
                liberar(frm);
            }
        });
    }

    @Test
    @DisplayName("DEF-UI-02: FrmSuspenderPartido corrige título y envuelve txtMotivo en JScrollPane")
    void testFrmSuspenderPartidoTituloYScroll() {
        runEdt(() -> {
            FrmSuspenderPartido frm = new FrmSuspenderPartido(usuarioAdmin());
            try {
                frm.pack();
                assertNotNull(buscarEtiqueta(frm.getContentPane(), "Gestión de encuentros"),
                        "El encabezado debe decir 'Gestión de encuentros'");
                assertTrue(buscarEtiquetasPorTexto(frm.getContentPane(), "encontros").isEmpty(),
                        "No debe quedar el typo 'encontros'");

                JTextArea txtMotivo = obtenerCampo(frm, "txtMotivo", JTextArea.class);
                assertNotNull(txtMotivo);
                JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, txtMotivo);
                assertNotNull(scroll, "txtMotivo debe estar envuelto en un JScrollPane");
                assertEquals(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, scroll.getVerticalScrollBarPolicy());
                assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER, scroll.getHorizontalScrollBarPolicy());
                assertTrue(txtMotivo.getLineWrap());
                assertTrue(txtMotivo.getWrapStyleWord());
            } finally {
                liberar(frm);
            }
        });
    }

    @Test
    @DisplayName("DEF-UI-03: FrmReclamos envuelve txtJustificacion en JScrollPane vertical")
    void testFrmReclamosScrollJustificacion() {
        runEdt(() -> {
            FrmReclamos frm = new FrmReclamos(usuarioAdmin());
            try {
                frm.pack();
                JTextArea txtJustificacion = obtenerCampo(frm, "txtJustificacion", JTextArea.class);
                assertNotNull(txtJustificacion);
                JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, txtJustificacion);
                assertNotNull(scroll, "txtJustificacion debe estar envuelto en un JScrollPane");
                assertEquals(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, scroll.getVerticalScrollBarPolicy());
                assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER, scroll.getHorizontalScrollBarPolicy());
                assertTrue(scroll.getPreferredSize().height >= 90,
                        "El JScrollPane debe reservar altura suficiente en GridBagLayout");
                assertTrue(txtJustificacion.getLineWrap());
                assertTrue(txtJustificacion.getWrapStyleWord());
            } finally {
                liberar(frm);
            }
        });
    }

    @Test
    @DisplayName("DEF-UI-04: FrmCredencialJugador registra un único ActionListener tras recargas y preserva DEF-010")
    void testFrmCredencialListenerUnicoYDef010() {
        runEdt(() -> {
            FrmCredencialJugador frm = new FrmCredencialJugador();
            try {
                JComboBox<?> cmbJugador = obtenerCampo(frm, "cmbJugador", JComboBox.class);
                assertEquals(1, cmbJugador.getActionListeners().length,
                        "cmbJugador debe tener exactamente 1 ActionListener tras iniciar");

                Method cargarJugadores = FrmCredencialJugador.class.getDeclaredMethod("cargarJugadores");
                cargarJugadores.setAccessible(true);
                cargarJugadores.invoke(frm);
                cargarJugadores.invoke(frm);

                assertEquals(1, cmbJugador.getActionListeners().length,
                        "Recargar jugadores no debe acumular ActionListeners duplicados");

                Jugador habilitado = new Jugador();
                habilitado.setNombre("Habilitado");
                habilitado.setEstado(TiposDatos.EstadoJugador.ACTIVO);
                Jugador suspendido = new Jugador();
                suspendido.setNombre("Suspendido");
                suspendido.setEstado(TiposDatos.EstadoJugador.SUSPENDIDO);

                List<Jugador> filtrados = FrmCredencialJugador.jugadoresHabilitadosParaCredencial(
                        List.of(habilitado, suspendido));
                assertEquals(1, filtrados.size());
                assertEquals("Habilitado", filtrados.get(0).getNombre());
            } finally {
                liberar(frm);
            }
        });
    }

    @Test
    @DisplayName("DEF-UI-05: FrmConsultaPublica mantiene un selector independiente y sincronizado por pestaña")
    void testFrmConsultaPublicaSelectoresPorPestana() {
        AtomicReference<Timer> killer = new AtomicReference<>();
        runEdt(() -> {
            // Descartar automáticamente el aviso de BD no disponible que emite cargarSelectores() offline.
            Timer timer = new Timer(40, e -> cerrarDialogosModales());
            timer.setRepeats(true);
            timer.start();
            killer.set(timer);

            FrmConsultaPublica frm = new FrmConsultaPublica();
            try {
                frm.pack();
                JTabbedPane tabs = buscarComponente(frm.getContentPane(), JTabbedPane.class);
                assertNotNull(tabs);
                assertEquals(4, tabs.getTabCount());

                JComboBox<?> cmbTab0 = buscarComponente(tabs.getComponentAt(0), JComboBox.class);
                JComboBox<?> cmbTab1 = buscarComponente(tabs.getComponentAt(1), JComboBox.class);
                JComboBox<?> cmbTab2 = buscarComponente(tabs.getComponentAt(2), JComboBox.class);
                JComboBox<?> cmbTab3 = buscarComponente(tabs.getComponentAt(3), JComboBox.class);

                assertNotNull(cmbTab0, "Fixture (CU-08) debe conservar su selector de torneo");
                assertNotNull(cmbTab1, "Perfil público (CU-09) debe conservar su selector de equipo");
                assertNotNull(cmbTab2, "Estadísticas (CU-10) debe conservar su selector de torneo");
                assertNotNull(cmbTab3, "Avance de fases (CU-11) debe conservar su selector de torneo");

                assertNotSame(cmbTab0, cmbTab2);
                assertNotSame(cmbTab0, cmbTab3);
                assertNotSame(cmbTab2, cmbTab3);

                ComboBoxModel<?> modelo0 = cmbTab0.getModel();
                assertSame(modelo0, cmbTab2.getModel());
                assertSame(modelo0, cmbTab3.getModel());

                @SuppressWarnings("unchecked")
                DefaultComboBoxModel<String> modeloTorneos = (DefaultComboBoxModel<String>) modelo0;
                modeloTorneos.removeAllElements();
                modeloTorneos.addElement("Apertura 2026 (LIGA)");
                modeloTorneos.addElement("Clausura 2026 (ELIMINATORIA)");

                cmbTab2.setSelectedIndex(1);
                assertEquals(1, cmbTab0.getSelectedIndex());
                assertEquals(1, cmbTab3.getSelectedIndex());
                assertNotNull(cmbTab0.getParent());
                assertNotNull(cmbTab2.getParent());
                assertNotNull(cmbTab3.getParent());
                assertFalse(cmbTab0.getParent() == cmbTab2.getParent());
            } finally {
                if (killer.get() != null) {
                    killer.get().stop();
                }
                liberar(frm);
            }
        });
    }

    @FunctionalInterface
    private interface EdtAction {
        void run() throws Exception;
    }

    private static void runEdt(EdtAction action) {
        assertDoesNotThrow(() -> {
            AtomicReference<Throwable> error = new AtomicReference<>();
            try {
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        EstilosUI.inicializarTema();
                        action.run();
                    } catch (Throwable t) {
                        error.set(t);
                    }
                });
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof HeadlessException) {
                    return;
                }
                throw e;
            }
            if (error.get() != null) {
                if (error.get() instanceof HeadlessException) {
                    return;
                }
                if (error.get() instanceof RuntimeException re) {
                    throw re;
                }
                throw new RuntimeException(error.get());
            }
        });
    }

    private static Usuario usuarioAdmin() {
        try {
            Usuario u = new Usuario("admin_test", "123", "admin@test.com", "Admin", "Test", TiposDatos.Rol.ADMINISTRADOR);
            u.setId(1L);
            return u;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T obtenerCampo(Object target, String fieldName, Class<T> type) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return type.cast(f.get(target));
    }

    private static JLabel buscarEtiqueta(Component root, String exactText) {
        if (root instanceof JLabel lbl && exactText.equals(lbl.getText())) {
            return lbl;
        }
        if (root instanceof Container c) {
            for (Component child : c.getComponents()) {
                JLabel found = buscarEtiqueta(child, exactText);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static List<JLabel> buscarEtiquetasPorTexto(Component root, String fragment) {
        List<JLabel> out = new ArrayList<>();
        if (root instanceof JLabel lbl && lbl.getText() != null && lbl.getText().contains(fragment)) {
            out.add(lbl);
        }
        if (root instanceof Container c) {
            for (Component child : c.getComponents()) {
                out.addAll(buscarEtiquetasPorTexto(child, fragment));
            }
        }
        return out;
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

    private static void cerrarDialogosModales() {
        for (Window w : Window.getWindows()) {
            if (w instanceof JDialog d && d.isShowing() && d.getModalityType() != Dialog.ModalityType.MODELESS) {
                AbstractButton btnOk = buscarBoton(d, "Aceptar", "OK", "Cerrar");
                if (btnOk != null) {
                    btnOk.doClick();
                } else {
                    d.dispose();
                }
            }
        }
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

    private static void liberar(JFrame ventana) {
        if (ventana == null) {
            return;
        }
        ventana.dispose();
    }
}
