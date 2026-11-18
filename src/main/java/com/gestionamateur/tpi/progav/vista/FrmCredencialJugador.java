package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.controlador.GestorJugadores;
import com.gestionamateur.tpi.progav.dao.EquipoDAO;
import com.gestionamateur.tpi.progav.modelo.Equipo;
import com.gestionamateur.tpi.progav.modelo.Jugador;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.seguridad.PoliticaPermisos;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * CU-02.2: emision e impresion de la credencial del jugador con codigo de barras.
 * La credencial se dibuja con Java2D y se imprime con el mecanismo de impresion
 * del sistema (javax.print), sin dependencias externas.
 */
public class FrmCredencialJugador extends JFrame {

    private final Usuario operador;
    private final GestorJugadores gestorJugadores = new GestorJugadores();
    private final List<Jugador> jugadores = new ArrayList<>();
    private final JComboBox<String> cmbJugador = new JComboBox<>();
    private final JLabel lblEstadoAcceso = new JLabel(" ");
    private final JButton btnRefrescar = new JButton("Actualizar");
    private final JButton btnImprimir = new JButton("Imprimir credencial");
    private final PanelCredencial panelCredencial = new PanelCredencial();

    public FrmCredencialJugador() {
        this(null);
    }

    public FrmCredencialJugador(Usuario operador) {
        this.operador = operador;
        setTitle("Credencial de Jugador (CU-02.2)");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        cargarJugadores();
        setLocationRelativeTo(null);
    }

    private boolean tieneAccesoAutorizado() {
        return PoliticaPermisos.tienePermisoOperacion(operador, PoliticaPermisos.ModuloMenu.IMPRIMIR_CREDENCIALES);
    }

    private void initComponents() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        EstilosUI.estilizarVentana(panel);

        JPanel panelEncabezado = EstilosUI.crearEncabezado(
                "Credencial de jugador",
                "Emisión e impresión de credencial oficial con código de barras Code 39 (CU-02.2)");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;

        JPanel panelSup = new JPanel(new GridBagLayout());
        EstilosUI.estilizarTarjeta(panelSup);

        JLabel lblJugador = new JLabel("Jugador habilitado:");
        EstilosUI.estilizarEtiquetaCampo(lblJugador);
        gbc.gridx = 0;
        gbc.gridy = 0;
        panelSup.add(lblJugador, gbc);

        EstilosUI.estilizarCombo(cmbJugador);
        cmbJugador.setPreferredSize(new Dimension(290, 30));
        cmbJugador.addActionListener(e -> panelCredencial.setJugador(jugadorSeleccionado()));
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelSup.add(cmbJugador, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        EstilosUI.estilizarBotonNeutro(btnRefrescar);
        btnRefrescar.addActionListener(e -> cargarJugadores());
        panelSup.add(btnRefrescar, gbc);

        EstilosUI.estilizarEstadoInfo(lblEstadoAcceso);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelSup.add(lblEstadoAcceso, gbc);

        // Superficie imprimible intencionalmente blanca para impresión física fiel
        panelCredencial.setPreferredSize(new Dimension(560, 260));

        EstilosUI.estilizarBotonPrimario(btnImprimir);
        btnImprimir.addActionListener(e -> imprimir());

        JButton btnVolver = new JButton("Volver al menú");
        EstilosUI.estilizarBotonNeutro(btnVolver);
        btnVolver.addActionListener(e -> dispose());

        JPanel panelAcciones = EstilosUI.crearBarraAcciones(
                new JComponent[]{btnImprimir},
                new JComponent[]{btnVolver});

        JPanel panelCentro = new JPanel(new BorderLayout(12, 12));
        panelCentro.setBackground(EstilosUI.BG_CANVAS);
        panelCentro.add(panelSup, BorderLayout.NORTH);
        panelCentro.add(panelCredencial, BorderLayout.CENTER);

        panel.add(panelEncabezado, BorderLayout.NORTH);
        panel.add(panelCentro, BorderLayout.CENTER);
        panel.add(panelAcciones, BorderLayout.SOUTH);
        setContentPane(panel);
        pack();
        setMinimumSize(new Dimension(680, 540));
    }

    private void cargarJugadores() {
        jugadores.clear();
        boolean autorizado = tieneAccesoAutorizado();
        cmbJugador.setEnabled(autorizado);
        btnRefrescar.setEnabled(autorizado);
        btnImprimir.setEnabled(autorizado);

        if (!autorizado) {
            cmbJugador.setModel(new DefaultComboBoxModel<>());
            panelCredencial.setJugador(null);
            lblEstadoAcceso.setText("Acceso denegado: requiere sesión de Administrador u Organizador autenticado.");
            return;
        }

        try {
            // DEF-010: solo se ofrecen para credencial los jugadores habilitados (ACTIVO).
            jugadores.addAll(jugadoresHabilitadosParaCredencial(
                    gestorJugadores.listarJugadoresParaCredencial(operador)));
            lblEstadoAcceso.setText(jugadores.isEmpty()
                    ? "No hay jugadores habilitados disponibles."
                    : jugadores.size() + " jugador(es) habilitado(s) para emisión de credencial.");
        } catch (TorneoExcepcion ex) {
            lblEstadoAcceso.setText(ex.getMessage());
        } catch (Exception e) {
            lblEstadoAcceso.setText("No se pudieron consultar los jugadores habilitados.");
        }
        DefaultComboBoxModel<String> modelo = new DefaultComboBoxModel<>();
        for (Jugador j : jugadores) {
            modelo.addElement(j.getNombre() + " (DNI " + j.getDni() + ")");
        }
        cmbJugador.setModel(modelo);
        panelCredencial.setJugador(jugadorSeleccionado());
    }

    /**
     * DEF-010: filtra los jugadores aptos para recibir credencial.
     *
     * <p>Regla de dominio (ADR-13): el estado del jugador modela su condición de salud y
     * aptitud administrativa. Solo un jugador {@code ACTIVO} puede recibir credencial; uno
     * suspendido, lesionado o inhabilitado no.
     *
     * @param jugadores lista completa de jugadores
     * @return los jugadores habilitados para credencial
     */
    public static List<Jugador> jugadoresHabilitadosParaCredencial(List<Jugador> jugadores) {
        List<Jugador> habilitados = new ArrayList<>();
        if (jugadores == null) {
            return habilitados;
        }
        for (Jugador j : jugadores) {
            if (j != null && j.getEstado() == TiposDatos.EstadoJugador.ACTIVO) {
                habilitados.add(j);
            }
        }
        return habilitados;
    }

    private Jugador jugadorSeleccionado() {
        int idx = cmbJugador.getSelectedIndex();
        if (idx < 0 || idx >= jugadores.size()) {
            return null;
        }
        return jugadores.get(idx);
    }

    private void imprimir() {
        if (!tieneAccesoAutorizado()) {
            JOptionPane.showMessageDialog(this,
                    "Acceso denegado: solo un Administrador u Organizador autenticado puede imprimir credenciales.",
                    "Acceso denegado", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Jugador jugador = jugadorSeleccionado();
        if (jugador == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un jugador para imprimir su credencial.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName("Credencial " + jugador.getNombre());
            job.setPrintable(new ImpresionCredencial(panelCredencial, jugador));
            if (job.printDialog()) {
                job.print();
                JOptionPane.showMessageDialog(this, "La credencial se envió a la impresora seleccionada.",
                        "Impresión", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo imprimir la credencial: " + ex.getMessage(),
                    "Error de impresión", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Dibuja la credencial del jugador. */
    private static class PanelCredencial extends JPanel {

        private Jugador jugador;

        PanelCredencial() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(new Color(0x12, 0x38, 0x5C), 2));
        }

        void setJugador(Jugador jugador) {
            this.jugador = jugador;
            repaint();
        }

        Jugador getJugador() {
            return jugador;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            int w = getWidth();
            int h = getHeight();

            g2.setColor(new Color(0x12, 0x38, 0x5C));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
            g2.drawString("TPI - Torneo de Fútbol Amateur", 18, 30);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.setColor(Color.DARK_GRAY);
            g2.drawString("Credencial de jugador - TPI Programación Avanzada II", 18, 50);

            if (jugador == null) {
                g2.drawString("Seleccione un jugador habilitado para generar su credencial.", 18, 90);
                return;
            }

            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
            g2.drawString(jugador.getNombre(), 18, 85);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            g2.drawString("DNI: " + jugador.getDni(), 18, 110);
            g2.drawString("Posición: " + jugador.getPosicion(), 18, 130);
            g2.drawString("Estado: " + jugador.getEstado(), 18, 150);

            String equipo = "(sin equipo)";
            try {
                for (Equipo e : new EquipoDAO().buscarTodos()) {
                    if (e.getIdEquipo() != null && e.getIdEquipo().equals(jugador.getIdEquipo())) {
                        equipo = e.getNombre() + " (" + e.getAlias() + ")";
                        break;
                    }
                }
            } catch (Exception ignore) {
                // Se muestra el identificador del equipo si no se puede resolver el nombre.
            }
            g2.drawString("Equipo: " + equipo, 18, 170);

            String contenido = "DNI" + jugador.getDni();
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            GeneradorCodigoBarras.dibujarConTexto(g2, contenido, 18, 190, w - 36, 42, 14);
        }
    }

    /** Envia la credencial a la impresora. */
    private static class ImpresionCredencial implements Printable {

        private final PanelCredencial panel;
        private final Jugador jugador;

        ImpresionCredencial(PanelCredencial panel, Jugador jugador) {
            this.panel = panel;
            this.jugador = jugador;
        }

        @Override
        public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
            if (pageIndex > 0) {
                return NO_SUCH_PAGE;
            }
            Graphics2D g2 = (Graphics2D) g;
            double ancho = pf.getImageableWidth();
            double alto = pf.getImageableHeight();

            // Escala la credencial a la pagina, manteniendo la proporcion.
            double escala = Math.min(ancho / panel.getWidth(), alto / panel.getHeight());
            g2.translate(pf.getImageableX(), pf.getImageableY());
            g2.scale(escala, escala);
            panel.printAll(g2);
            return PAGE_EXISTS;
        }
    }
}
