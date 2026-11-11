package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.controlador.GestorCanchas;
import com.gestionamateur.tpi.progav.modelo.Cancha;
import com.gestionamateur.tpi.progav.modelo.Usuario;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * CU-16: alta de sedes y canchas, con listado e inhabilitacion de venues.
 */
public class FrmAltaCancha extends JFrame {

    private final GestorCanchas gestorCanchas = new GestorCanchas();
    private final Usuario operador;

    private final JTextField txtNombre = new JTextField(22);
    private final JTextField txtDireccion = new JTextField(26);
    private final JComboBox<String> cmbSuperficie = new JComboBox<>();
    private final JLabel lblInfo = new JLabel(" ");

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Sede", "Dirección", "Superficie", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tablaCanchas = new JTable(modeloTabla);

    public FrmAltaCancha() {
        this(null);
    }

    public FrmAltaCancha(Usuario operador) {
        this.operador = operador;
        setTitle("Gestión de Sedes y Canchas (CU-16)");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        cargarCanchas();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBackground(Color.WHITE);

        JLabel titulo = new JLabel("Sedes y canchas");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(new Color(0x12, 0x38, 0x5C));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        panelForm.add(titulo, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panelForm.add(new JLabel("Nombre del predio:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panelForm.add(txtNombre, gbc);
        gbc.gridx = 3;
        gbc.gridwidth = 1;
        panelForm.add(new JLabel("Dirección física:"), gbc);
        gbc.gridx = 4;
        panelForm.add(txtDireccion, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panelForm.add(new JLabel("Tipo de superficie:"), gbc);
        gbc.gridx = 1;
        cmbSuperficie.addItem("Césped Natural");
        cmbSuperficie.addItem("Césped Sintético");
        cmbSuperficie.addItem("Tierra / Arena");
        cmbSuperficie.addItem("Cemento");
        cmbSuperficie.setPreferredSize(new Dimension(180, 24));
        panelForm.add(cmbSuperficie, gbc);
        gbc.gridx = 2;
        gbc.gridwidth = 3;
        lblInfo.setForeground(new Color(0x3A, 0x55, 0x68));
        panelForm.add(lblInfo, gbc);

        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton btnGuardar = new JButton("Registrar sede");
        btnGuardar.addActionListener(e -> guardar());
        JButton btnInhabilitar = new JButton("Inhabilitar seleccionada");
        btnInhabilitar.addActionListener(e -> cambiarEstado(false));
        JButton btnHabilitar = new JButton("Habilitar seleccionada");
        btnHabilitar.addActionListener(e -> cambiarEstado(true));
        JButton btnActualizar = new JButton("Actualizar listado");
        btnActualizar.addActionListener(e -> cargarCanchas());
        JButton btnCancelar = new JButton("Volver al menú");
        btnCancelar.addActionListener(e -> dispose());
        panelAcciones.add(btnGuardar);
        panelAcciones.add(btnInhabilitar);
        panelAcciones.add(btnHabilitar);
        panelAcciones.add(btnActualizar);
        panelAcciones.add(btnCancelar);

        JScrollPane scroll = new JScrollPane(tablaCanchas);
        scroll.setPreferredSize(new Dimension(860, 200));

        JPanel panelCentro = new JPanel(new BorderLayout(8, 8));
        panelCentro.setBackground(Color.WHITE);
        panelCentro.add(panelForm, BorderLayout.NORTH);
        panelCentro.add(scroll, BorderLayout.CENTER);

        panel.add(panelCentro, BorderLayout.CENTER);
        panel.add(panelAcciones, BorderLayout.SOUTH);
        setContentPane(panel);
        pack();
        setMinimumSize(new Dimension(900, 540));
    }

    private void cargarCanchas() {
        List<Cancha> canchas = GestorCanchas.getCanchasRegistradasConInactivas();
        modeloTabla.setRowCount(0);
        for (Cancha c : canchas) {
            modeloTabla.addRow(new Object[]{
                    c.getIdCancha(),
                    c.getNombre(),
                    c.getDireccion(),
                    c.getTipoSuperficie(),
                    c.isActiva() ? "Activa" : "Inhabilitada"});
        }
        if (canchas.isEmpty()) {
            lblInfo.setText("Todavía no hay sedes registradas.");
        } else {
            lblInfo.setText(canchas.size() + " sede(s) registrada(s).");
        }
    }

    private void guardar() {
        String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();
        String direccion = txtDireccion.getText() == null ? "" : txtDireccion.getText().trim();
        String superficie = (String) cmbSuperficie.getSelectedItem();

        if (nombre.isEmpty() || direccion.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Complete el nombre y la dirección de la sede.",
                    "Campos vacíos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            String respuesta = gestorCanchas.registrarCancha(nombre, direccion, superficie, operador);
            if (respuesta.startsWith("ERROR")) {
                JOptionPane.showMessageDialog(this, respuesta, "No se pudo registrar la sede", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, respuesta, "Operación Exitosa", JOptionPane.INFORMATION_MESSAGE);
                txtNombre.setText("");
                txtDireccion.setText("");
                cargarCanchas();
            }
        } catch (com.gestionamateur.tpi.progav.modelo.TorneoExcepcion ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo registrar la sede", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cambiarEstado(boolean activa) {
        int fila = tablaCanchas.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione la sede de la tabla.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long idCancha = (Long) modeloTabla.getValueAt(fila, 0);
        String respuesta = gestorCanchas.cambiarEstado(idCancha, activa, operador);
        if (respuesta.startsWith("ERROR")) {
            JOptionPane.showMessageDialog(this, respuesta, "No se pudo modificar la sede", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, respuesta, "Operación Exitosa", JOptionPane.INFORMATION_MESSAGE);
            cargarCanchas();
        }
    }
}
