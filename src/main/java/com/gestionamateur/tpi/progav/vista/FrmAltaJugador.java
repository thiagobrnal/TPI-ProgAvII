package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.controlador.GestorEquipos;
import com.gestionamateur.tpi.progav.controlador.GestorJugadores;
import com.gestionamateur.tpi.progav.dao.JugadorDAO;
import com.gestionamateur.tpi.progav.modelo.Equipo;
import com.gestionamateur.tpi.progav.modelo.Jugador;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
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
import java.util.Date;
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
 * CU-02 / CU-02.1: alta de jugadores (con equipo, estado y control de elegibilidad)
 * y listado de los jugadores registrados.
 */
public class FrmAltaJugador extends JFrame {

    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");

    private final GestorJugadores gestorJugadores = new GestorJugadores();
    private final Usuario operador;

    private List<Equipo> equiposCargados = new ArrayList<>();
    private List<Jugador> jugadoresCargados = new ArrayList<>();

    private final JTextField txtNombre = new JTextField(20);
    private final JTextField txtDni = new JTextField(10);
    private final JTextField txtFechaNac = new JTextField(10);
    private final JComboBox<String> cmbPosicion = new JComboBox<>();
    private final JComboBox<String> cmbEquipo = new JComboBox<>();
    private final JComboBox<String> cmbEstado = new JComboBox<>();
    private final JLabel lblInfo = new JLabel(" ");

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"DNI", "Jugador", "Equipo", "Posición", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tablaJugadores = new JTable(modeloTabla);

    public FrmAltaJugador() {
        this(null);
    }

    public FrmAltaJugador(Usuario operador) {
        this.operador = operador;
        setTitle("Alta de Jugadores (CU-02)");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        cargarPosiciones();
        cargarEquipos();
        cargarEstado();
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

        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBackground(Color.WHITE);

        JLabel titulo = new JLabel("Alta de jugador");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(new Color(0x12, 0x38, 0x5C));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        panelForm.add(titulo, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panelForm.add(new JLabel("Nombre completo:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panelForm.add(txtNombre, gbc);
        gbc.gridx = 3;
        gbc.gridwidth = 1;
        panelForm.add(new JLabel("DNI:"), gbc);
        gbc.gridx = 4;
        panelForm.add(txtDni, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panelForm.add(new JLabel("Fecha de nacimiento:"), gbc);
        gbc.gridx = 1;
        panelForm.add(txtFechaNac, gbc);
        gbc.gridx = 2;
        panelForm.add(new JLabel("Posición:"), gbc);
        gbc.gridx = 3;
        cmbPosicion.setPreferredSize(new Dimension(160, 24));
        panelForm.add(cmbPosicion, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panelForm.add(new JLabel("Equipo:"), gbc);
        gbc.gridx = 1;
        cmbEquipo.setPreferredSize(new Dimension(230, 24));
        panelForm.add(cmbEquipo, gbc);
        gbc.gridx = 2;
        panelForm.add(new JLabel("Estado:"), gbc);
        gbc.gridx = 3;
        cmbEstado.setPreferredSize(new Dimension(160, 24));
        panelForm.add(cmbEstado, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 5;
        lblInfo.setForeground(new Color(0x3A, 0x55, 0x68));
        panelForm.add(lblInfo, gbc);

        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton btnGuardar = new JButton("Registrar jugador");
        btnGuardar.addActionListener(e -> btnGuardarActionPerformed(e));
        JButton btnActualizar = new JButton("Actualizar listado");
        btnActualizar.addActionListener(e -> cargarJugadores());
        JButton btnCancelar = new JButton("Volver al menú");
        btnCancelar.addActionListener(e -> dispose());
        panelAcciones.add(btnGuardar);
        panelAcciones.add(btnActualizar);
        panelAcciones.add(btnCancelar);

        JScrollPane scroll = new JScrollPane(tablaJugadores);
        scroll.setPreferredSize(new Dimension(860, 210));

        JPanel panelCentro = new JPanel(new BorderLayout(8, 8));
        panelCentro.setBackground(Color.WHITE);
        panelCentro.add(panelForm, BorderLayout.NORTH);
        panelCentro.add(scroll, BorderLayout.CENTER);

        panel.add(panelCentro, BorderLayout.CENTER);
        panel.add(panelAcciones, BorderLayout.SOUTH);
        setContentPane(panel);
        pack();
        setMinimumSize(new Dimension(900, 560));
    }

    private void cargarPosiciones() {
        cmbPosicion.removeAllItems();
        for (String pos : new String[]{"Arquero", "Defensor", "Mediocampista", "Delantero", "Lateral"}) {
            cmbPosicion.addItem(pos);
        }
    }

    private void cargarEstado() {
        cmbEstado.removeAllItems();
        for (TiposDatos.EstadoJugador e : TiposDatos.EstadoJugador.values()) {
            cmbEstado.addItem(e.name());
        }
        cmbEstado.setSelectedItem(TiposDatos.EstadoJugador.ACTIVO.name());
        boolean esCapitan = operador != null && operador.getRol() == TiposDatos.Rol.CAPITAN;
        cmbEstado.setEnabled(!esCapitan);
    }

    /** CU-02: el equipo es obligatorio (precondicion de la especificacion). */
    private void cargarEquipos() {
        cmbEquipo.removeAllItems();
        equiposCargados = new ArrayList<>();
        try {
            if (operador != null && operador.getRol() == TiposDatos.Rol.CAPITAN) {
                Equipo eq = new GestorEquipos().buscarEquipoDeCapitan(operador.getId());
                if (eq != null) {
                    equiposCargados.add(eq);
                    cmbEquipo.addItem(eq.getNombre());
                }
                cmbEquipo.setEnabled(false);
            } else {
                equiposCargados.addAll(GestorEquipos.getEquiposRegistrados());
                for (Equipo e : equiposCargados) {
                    cmbEquipo.addItem(e.getNombre());
                }
            }
        } catch (Exception e) {
            lblInfo.setText("No se pudieron cargar los equipos registrados.");
        }
        if (equiposCargados.isEmpty()) {
            lblInfo.setText("No hay equipos registrados: registre primero un equipo para poder dar de alta jugadores.");
        }
    }

    private void cargarJugadores() {
        jugadoresCargados = new ArrayList<>();
        try {
            jugadoresCargados = new JugadorDAO().buscarTodos();
        } catch (Exception e) {
            lblInfo.setText("No se pudo cargar el listado de jugadores.");
        }
        modeloTabla.setRowCount(0);
        for (Jugador j : jugadoresCargados) {
            modeloTabla.addRow(new Object[]{
                    j.getDni(),
                    j.getNombre(),
                    j.getIdEquipo() == null ? "(sin equipo)" : nombreEquipo(j.getIdEquipo()),
                    j.getPosicion(),
                    j.getEstado()});
        }
    }

    private String nombreEquipo(Long idEquipo) {
        for (Equipo e : equiposCargados) {
            if (e.getIdEquipo() != null && e.getIdEquipo().equals(idEquipo)) {
                return e.getNombre();
            }
        }
        return "(equipo " + idEquipo + ")";
    }

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {
        String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();
        String dniTexto = txtDni.getText() == null ? "" : txtDni.getText().trim();
        String fechaTexto = txtFechaNac.getText() == null ? "" : txtFechaNac.getText().trim();
        String posicion = (String) cmbPosicion.getSelectedItem();

        if (nombre.isEmpty() || dniTexto.isEmpty() || fechaTexto.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Complete todos los campos por favor.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int idxEquipo = cmbEquipo.getSelectedIndex();
        if (idxEquipo < 0 || idxEquipo >= equiposCargados.size()) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar el equipo al que se incorpora el jugador.",
                    "Equipo requerido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int dni;
        Date fechaNacimiento;
        try {
            dni = Integer.parseInt(dniTexto);
            formatoFecha.setLenient(false);
            fechaNacimiento = formatoFecha.parse(fechaTexto);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "El DNI debe ser numérico y la fecha tener el formato dd/MM/yyyy.",
                    "Datos inválidos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long idEquipo = equiposCargados.get(idxEquipo).getIdEquipo();
        TiposDatos.EstadoJugador estado = TiposDatos.EstadoJugador.valueOf((String) cmbEstado.getSelectedItem());

        String respuesta = gestorJugadores.iniciarAltaJugador(nombre, dni, fechaNacimiento, posicion,
                idEquipo, estado, operador);

        if (respuesta.startsWith("ERROR")) {
            JOptionPane.showMessageDialog(this, respuesta, "No se pudo registrar el jugador", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, respuesta, "Operación Exitosa", JOptionPane.INFORMATION_MESSAGE);
            txtNombre.setText("");
            txtDni.setText("");
            txtFechaNac.setText("");
            cargarJugadores();
        }
    }
}
