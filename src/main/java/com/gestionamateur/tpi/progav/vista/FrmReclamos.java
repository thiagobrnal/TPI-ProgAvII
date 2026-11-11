package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.controlador.GestorReclamos;
import com.gestionamateur.tpi.progav.modelo.Reclamo;
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
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

/**
 * CU-14: bandeja de reclamos formales y resolucion por el organizador.
 */
public class FrmReclamos extends JFrame {

    private static final SimpleDateFormat FORMATO_FECHA = new SimpleDateFormat("dd/MM/yyyy");

    private final Usuario operador;
    private final GestorReclamos gestorReclamos = new GestorReclamos();

    private List<Reclamo> reclamos = new ArrayList<>();
    private final JComboBox<String> cmbVeredicto = new JComboBox<>();
    private final JTextArea txtJustificacion = new JTextArea(6, 42);
    private final JLabel lblInfo = new JLabel(" ");

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Fecha", "Motivo", "Partido", "Capitán", "Sanción apelada"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tablaReclamos = new JTable(modeloTabla);

    public FrmReclamos(Usuario operador) {
        this.operador = operador;
        setTitle("Reclamos Pendientes (CU-14)");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        cargarReclamos();
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
        JLabel titulo = new JLabel("Reclamos formales");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(new Color(0x12, 0x38, 0x5C));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        panelSup.add(titulo, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panelSup.add(new JLabel("Veredicto:"), gbc);
        gbc.gridx = 1;
        cmbVeredicto.addItem("A FAVOR");
        cmbVeredicto.addItem("EN CONTRA");
        cmbVeredicto.setPreferredSize(new Dimension(170, 24));
        panelSup.add(cmbVeredicto, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panelSup.add(new JLabel("Fundamentación (mínimo 15 caracteres):"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        txtJustificacion.setLineWrap(true);
        txtJustificacion.setWrapStyleWord(true);
        panelSup.add(txtJustificacion, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        lblInfo.setForeground(new Color(0x3A, 0x55, 0x68));
        panelSup.add(lblInfo, gbc);

        JScrollPane scroll = new JScrollPane(tablaReclamos);
        scroll.setPreferredSize(new Dimension(900, 240));

        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton btnResolver = new JButton("Resolver reclamo (seleccionado)");
        btnResolver.addActionListener(e -> resolver());
        JButton btnVolver = new JButton("Volver al menú");
        btnVolver.addActionListener(e -> dispose());
        panelAcciones.add(btnResolver);
        panelAcciones.add(btnVolver);

        JPanel panelCentro = new JPanel(new BorderLayout(6, 6));
        panelCentro.setBackground(Color.WHITE);
        panelCentro.add(panelSup, BorderLayout.NORTH);
        panelCentro.add(scroll, BorderLayout.CENTER);

        panel.add(panelCentro, BorderLayout.CENTER);
        panel.add(panelAcciones, BorderLayout.SOUTH);
        setContentPane(panel);
        pack();
        setMinimumSize(new Dimension(940, 620));
    }

    private void cargarReclamos() {
        reclamos = gestorReclamos.listarPendientes();
        modeloTabla.setRowCount(0);
        for (Reclamo r : reclamos) {
            modeloTabla.addRow(new Object[]{
                    r.getId(),
                    r.getFechaInicio() == null ? "" : FORMATO_FECHA.format(r.getFechaInicio()),
                    r.getMotivo(),
                    r.getPartido() == null ? "(sin partido)" : "#" + r.getPartido().getIdPartido(),
                    r.getCapitan() == null ? "(sin capitán)" : r.getCapitan().getNombreUsuario(),
                    r.getSancionApelada() == null ? "(sin sanción)" : "#" + r.getSancionApelada().getId()});
        }
        if (reclamos.isEmpty()) {
            lblInfo.setText("No hay reclamos pendientes de resolución en este momento.");
        } else {
            lblInfo.setText("Seleccione un reclamo de la tabla para resolverlo.");
        }
    }

    private void resolver() {
        int fila = tablaReclamos.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione el reclamo a resolver.", "Reclamos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long idReclamo = (Long) modeloTabla.getValueAt(fila, 0);
        String veredicto = (String) cmbVeredicto.getSelectedItem();
        String justificacion = txtJustificacion.getText() == null ? "" : txtJustificacion.getText().trim();
        if (justificacion.length() < 15) {
            JOptionPane.showMessageDialog(this, "La fundamentación del veredicto debe tener al menos 15 caracteres.",
                    "Reclamos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String respuesta = gestorReclamos.resolver(idReclamo, veredicto, justificacion, operador);
        if (respuesta.startsWith("ÉXITO")) {
            JOptionPane.showMessageDialog(this, respuesta, "Reclamos", JOptionPane.INFORMATION_MESSAGE);
            txtJustificacion.setText("");
            cargarReclamos();
        } else {
            JOptionPane.showMessageDialog(this, respuesta, "No se pudo resolver el reclamo", JOptionPane.ERROR_MESSAGE);
        }
    }
}
