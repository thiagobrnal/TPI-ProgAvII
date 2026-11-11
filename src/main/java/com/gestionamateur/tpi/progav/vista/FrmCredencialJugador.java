package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.dao.EquipoDAO;
import com.gestionamateur.tpi.progav.dao.JugadorDAO;
import com.gestionamateur.tpi.progav.modelo.Equipo;
import com.gestionamateur.tpi.progav.modelo.Jugador;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
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

    private final JugadorDAO jugadorDAO = new JugadorDAO();
    private final List<Jugador> jugadores = new ArrayList<>();
    private final JComboBox<String> cmbJugador = new JComboBox<>();
    private final PanelCredencial panelCredencial = new PanelCredencial();

    public FrmCredencialJugador() {
        setTitle("Credencial de Jugador (CU-02.2)");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        cargarJugadores();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JPanel panelSup = new JPanel(new GridBagLayout());
        panelSup.setBackground(Color.WHITE);
        JLabel titulo = new JLabel("Credencial de jugador");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(new Color(0x12, 0x38, 0x5C));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        panelSup.add(titulo, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panelSup.add(new JLabel("Jugador habilitado:"), gbc);
        gbc.gridx = 1;
        cmbJugador.setPreferredSize(new Dimension(280, 26));
        panelSup.add(cmbJugador, gbc);
        gbc.gridx = 2;
        JButton btnRefrescar = new JButton("Actualizar");
        btnRefrescar.addActionListener(e -> cargarJugadores());
        panelSup.add(btnRefrescar, gbc);

        panelCredencial.setPreferredSize(new Dimension(560, 260));

        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton btnImprimir = new JButton("Imprimir credencial");
        btnImprimir.addActionListener(e -> imprimir());
        JButton btnVolver = new JButton("Volver al menú");
        btnVolver.addActionListener(e -> dispose());
        panelAcciones.add(btnImprimir);
        panelAcciones.add(btnVolver);

        JPanel panelCentro = new JPanel(new BorderLayout(8, 8));
        panelCentro.setBackground(Color.WHITE);
        panelCentro.add(panelSup, BorderLayout.NORTH);
        panelCentro.add(panelCredencial, BorderLayout.CENTER);

        panel.add(panelCentro, BorderLayout.CENTER);
        panel.add(panelAcciones, BorderLayout.SOUTH);
        setContentPane(panel);
        pack();
        setMinimumSize(new Dimension(620, 500));
    }

    private void cargarJugadores() {
        jugadores.clear();
        try {
            jugadores.addAll(jugadorDAO.buscarTodos());
        } catch (Exception e) {
            // Se informa al usuario al elegir el jugador.
        }
        DefaultComboBoxModel<String> modelo = new DefaultComboBoxModel<>();
        for (Jugador j : jugadores) {
            modelo.addElement(j.getNombre() + " (DNI " + j.getDni() + ")");
        }
        cmbJugador.setModel(modelo);
        cmbJugador.addActionListener(e -> panelCredencial.setJugador(jugadorSeleccionado()));
        panelCredencial.setJugador(jugadorSeleccionado());
    }

    private Jugador jugadorSeleccionado() {
        int idx = cmbJugador.getSelectedIndex();
        if (idx < 0 || idx >= jugadores.size()) {
            return null;
        }
        return jugadores.get(idx);
    }

    private void imprimir() {
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
