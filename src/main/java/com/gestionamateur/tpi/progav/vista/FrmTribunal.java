package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.controlador.GestorDisciplina;
import com.gestionamateur.tpi.progav.dao.JugadorDAO;
import com.gestionamateur.tpi.progav.modelo.Jugador;
import com.gestionamateur.tpi.progav.modelo.Sancion;
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
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * CU-12: tribunal disciplinario. Permite emitir el fallo sobre las sanciones pendientes,
 * anular sanciones y registrar sanciones administrativas manuales.
 */
public class FrmTribunal extends JFrame {

    private static final SimpleDateFormat FORMATO_FECHA = new SimpleDateFormat("dd/MM/yyyy");

    private final Usuario operador;
    private final GestorDisciplina gestorDisciplina = new GestorDisciplina();
    private final JugadorDAO jugadorDAO = new JugadorDAO();

    private List<Sancion> sanciones = new ArrayList<>();
    private List<Jugador> jugadores = new ArrayList<>();

    private final JComboBox<String> cmbJugador = new JComboBox<>();
    private final JTextField txtPartidos = new JTextField("1", 4);
    private final JTextField txtMotivo = new JTextField(34);
    private final JTextField txtGravedad = new JTextField(16);
    private final JLabel lblInfo = new JLabel(" ");

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Jugador", "Origen", "Motivo", "Gravedad", "Fecha"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tablaSanciones = new JTable(modeloTabla);

    public FrmTribunal(Usuario operador) {
        this.operador = operador;
        setTitle("Tribunal Disciplinario (CU-12)");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        cargarDatos();
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
        JLabel titulo = new JLabel("Tribunal disciplinario");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(new Color(0x12, 0x38, 0x5C));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        panelSup.add(titulo, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panelSup.add(new JLabel("Jugador:"), gbc);
        gbc.gridx = 1;
        cmbJugador.setPreferredSize(new Dimension(260, 24));
        panelSup.add(cmbJugador, gbc);
        gbc.gridx = 2;
        panelSup.add(new JLabel("Fechas de suspensión:"), gbc);
        gbc.gridx = 3;
        panelSup.add(txtPartidos, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panelSup.add(new JLabel("Motivo:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panelSup.add(txtMotivo, gbc);
        gbc.gridx = 3;
        gbc.gridwidth = 1;
        panelSup.add(new JLabel("Gravedad:"), gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panelSup.add(new JLabel(" "), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        panelSup.add(txtGravedad, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 4;
        lblInfo.setForeground(new Color(0x3A, 0x55, 0x68));
        panelSup.add(lblInfo, gbc);

        JScrollPane scroll = new JScrollPane(tablaSanciones);
        scroll.setPreferredSize(new Dimension(900, 250));

        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton btnEmitir = new JButton("Emitir fallo (sancion seleccionada)");
        btnEmitir.addActionListener(e -> emitirFallo());
        JButton btnAnular = new JButton("Anular sancion (seleccionada)");
        btnAnular.addActionListener(e -> anular());
        JButton btnRegistrar = new JButton("Registrar sancion administrativa");
        btnRegistrar.addActionListener(e -> registrarAdministrativa());
        JButton btnVolver = new JButton("Volver al menú");
        btnVolver.addActionListener(e -> dispose());
        panelAcciones.add(btnEmitir);
        panelAcciones.add(btnAnular);
        panelAcciones.add(btnRegistrar);
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

    private void cargarDatos() {
        sanciones = gestorDisciplina.listarPendientes();
        modeloTabla.setRowCount(0);
        for (Sancion s : sanciones) {
            modeloTabla.addRow(new Object[]{
                    s.getId(),
                    s.getJugadorSancionado() == null ? "(desconocido)" : s.getJugadorSancionado().getNombre(),
                    s.getTipoOrigen(),
                    s.getMotivo(),
                    s.getGravedad(),
                    s.getFechaInicio() == null ? "" : FORMATO_FECHA.format(s.getFechaInicio())});
        }
        if (sanciones.isEmpty()) {
            lblInfo.setText("No hay sanciones pendientes de fallo en este momento.");
        } else {
            lblInfo.setText("Seleccione una fila de la tabla para emitir el fallo o anularla.");
        }

        jugadores = new ArrayList<>();
        try {
            jugadores.addAll(jugadorDAO.buscarTodos());
        } catch (Exception e) {
            lblInfo.setText("No se pudieron cargar los jugadores del torneo.");
        }
        DefaultComboBoxModel<String> modelo = new DefaultComboBoxModel<>();
        for (Jugador j : jugadores) {
            modelo.addElement(j.getNombre() + " (DNI " + j.getDni() + ")");
        }
        cmbJugador.setModel(modelo);
    }

    private int leerPartidos() {
        try {
            return Integer.parseInt(txtPartidos.getText().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void emitirFallo() {
        int fila = tablaSanciones.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione la sanción a resolver.", "Tribunal", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long idSancion = (Long) modeloTabla.getValueAt(fila, 0);
        int partidos = leerPartidos();
        String motivo = txtMotivo.getText() == null ? "" : txtMotivo.getText().trim();
        String gravedad = txtGravedad.getText() == null ? "" : txtGravedad.getText().trim();

        String respuesta = gestorDisciplina.emitirFallo(idSancion, partidos, motivo, gravedad, operador);
        mostrar(respuesta);
    }

    private void anular() {
        int fila = tablaSanciones.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione la sanción a anular.", "Tribunal", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long idSancion = (Long) modeloTabla.getValueAt(fila, 0);
        String motivo = txtMotivo.getText() == null ? "" : txtMotivo.getText().trim();
        if (motivo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Indique el motivo de la anulación.", "Tribunal", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String respuesta = gestorDisciplina.anular(idSancion, motivo, operador);
        mostrar(respuesta);
    }

    private void registrarAdministrativa() {
        int idx = cmbJugador.getSelectedIndex();
        if (idx < 0 || idx >= jugadores.size()) {
            JOptionPane.showMessageDialog(this, "Seleccione el jugador a sancionar.", "Tribunal", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int partidos = leerPartidos();
        String motivo = txtMotivo.getText() == null ? "" : txtMotivo.getText().trim();
        String gravedad = txtGravedad.getText() == null ? "" : txtGravedad.getText().trim();

        String respuesta = gestorDisciplina.registrarSancionAdministrativa(
                jugadores.get(idx).getId(), partidos, motivo, gravedad, operador);
        mostrar(respuesta);
    }

    private void mostrar(String respuesta) {
        if (respuesta.startsWith("ÉXITO")) {
            JOptionPane.showMessageDialog(this, respuesta, "Tribunal", JOptionPane.INFORMATION_MESSAGE);
            cargarDatos();
        } else {
            JOptionPane.showMessageDialog(this, respuesta, "No se pudo completar la operación", JOptionPane.ERROR_MESSAGE);
        }
    }
}
