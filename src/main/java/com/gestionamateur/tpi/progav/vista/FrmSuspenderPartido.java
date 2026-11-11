package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.controlador.GestorPartidos;
import com.gestionamateur.tpi.progav.dao.PartidoDAO;
import com.gestionamateur.tpi.progav.modelo.Partido;
import com.gestionamateur.tpi.progav.modelo.Usuario;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.SimpleDateFormat;
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
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * CU-05: postergacion y suspension de partidos programados, con motivo obligatorio.
 */
public class FrmSuspenderPartido extends JFrame {

    private static final SimpleDateFormat FORMATO_FECHA = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private final Usuario operador;
    private final GestorPartidos gestorPartidos = new GestorPartidos();
    private final PartidoDAO partidoDAO = new PartidoDAO();

    private List<Partido> partidos = new ArrayList<>();
    private final JComboBox<String> cmbPartido = new JComboBox<>();
    private final JTextArea txtMotivo = new JTextArea(5, 40);
    private final JLabel lblInfo = new JLabel(" ");

    public FrmSuspenderPartido(Usuario operador) {
        this.operador = operador;
        setTitle("Postergar o Suspender Partido (CU-05)");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        cargarPartidos();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        panel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        JPanel panelSup = new JPanel(new GridBagLayout());
        panelSup.setBackground(Color.WHITE);
        JLabel titulo = new JLabel("Gestión de encontros");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(new Color(0x12, 0x38, 0x5C));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panelSup.add(titulo, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panelSup.add(new JLabel("Partido programado:"), gbc);
        gbc.gridx = 1;
        cmbPartido.setPreferredSize(new Dimension(430, 26));
        panelSup.add(cmbPartido, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panelSup.add(new JLabel("Motivo (obligatorio):"), gbc);
        gbc.gridx = 1;
        txtMotivo.setLineWrap(true);
        txtMotivo.setWrapStyleWord(true);
        panelSup.add(txtMotivo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        lblInfo.setForeground(new Color(0x3A, 0x55, 0x68));
        panelSup.add(lblInfo, gbc);

        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        JButton btnPostergar = new JButton("Postergar partido");
        btnPostergar.addActionListener(e -> ejecutar("POSTERGAR"));
        JButton btnSuspender = new JButton("Suspender partido");
        btnSuspender.addActionListener(e -> ejecutar("SUSPENDER"));
        JButton btnVolver = new JButton("Volver al menú");
        btnVolver.addActionListener(e -> dispose());
        panelAcciones.add(btnPostergar);
        panelAcciones.add(btnSuspender);
        panelAcciones.add(btnVolver);

        panel.add(panelSup, BorderLayout.CENTER);
        panel.add(panelAcciones, BorderLayout.SOUTH);
        setContentPane(panel);
        pack();
        setMinimumSize(new Dimension(620, 420));
    }

    private void cargarPartidos() {
        partidos = new ArrayList<>();
        try {
            for (Partido p : partidoDAO.buscarPendientesDeFinalizacion()) {
                if (p.getEstadoPartido() == com.gestionamateur.tpi.progav.modelo.TiposDatos.EstadoPartido.PROGRAMADO) {
                    partidos.add(p);
                }
            }
        } catch (Exception e) {
            lblInfo.setText("No se pudieron consultar los partidos programados.");
        }
        DefaultComboBoxModel<String> modelo = new DefaultComboBoxModel<>();
        for (Partido p : partidos) {
            modelo.addElement("#" + p.getIdPartido() + " - " + FORMATO_FECHA.format(p.getFechaHora())
                    + (p.getArbitroDesignado() == null ? " - sin arbitro" : " - arbitro: " + p.getArbitroDesignado().getNombre()));
        }
        cmbPartido.setModel(modelo);
        if (partidos.isEmpty()) {
            lblInfo.setText("No hay partidos programados disponibles para postergar o suspender.");
        }
    }

    private void ejecutar(String operacion) {
        int idx = cmbPartido.getSelectedIndex();
        if (idx < 0 || idx >= partidos.size()) {
            JOptionPane.showMessageDialog(this, "Seleccione el partido a modificar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String motivo = txtMotivo.getText() == null ? "" : txtMotivo.getText().trim();
        if (motivo.length() < 10) {
            JOptionPane.showMessageDialog(this, "Indique el motivo de la decisión (mínimo 10 caracteres).",
                    "Motivo requerido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long idPartido = partidos.get(idx).getIdPartido();
        String respuesta = "POSTERGAR".equals(operacion)
                ? gestorPartidos.procesarSuspension(idPartido, motivo, operador)
                : gestorPartidos.procesarSuspensionPorIncidencias(idPartido, motivo, operador);

        if (respuesta.startsWith("ÉXITO")) {
            JOptionPane.showMessageDialog(this, respuesta, operacion, JOptionPane.INFORMATION_MESSAGE);
            txtMotivo.setText("");
            cargarPartidos();
        } else {
            JOptionPane.showMessageDialog(this, respuesta, "No se pudo registrar la decisión", JOptionPane.ERROR_MESSAGE);
        }
    }
}
