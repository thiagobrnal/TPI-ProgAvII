package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.controlador.GestorEquipos;
import com.gestionamateur.tpi.progav.controlador.GestorJugadores;
import com.gestionamateur.tpi.progav.modelo.Equipo;
import com.gestionamateur.tpi.progav.modelo.Jugador;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.seguridad.PoliticaPermisos;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

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
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        EstilosUI.estilizarVentana(panel);

        JPanel panelEncabezado = EstilosUI.crearEncabezado(
                "Alta de jugador",
                "Registro de jugadores, posición en campo, equipo y control de elegibilidad (CU-02)");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 10);
        gbc.anchor = GridBagConstraints.WEST;

        JPanel panelForm = new JPanel(new GridBagLayout());
        EstilosUI.estilizarTarjeta(panelForm);

        JLabel lblSeccion = new JLabel("Datos del jugador");
        EstilosUI.estilizarTituloSeccion(lblSeccion);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        panelForm.add(lblSeccion, gbc);

        JLabel lblNombre = new JLabel("Nombre completo:");
        EstilosUI.estilizarEtiquetaCampo(lblNombre);
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panelForm.add(lblNombre, gbc);

        EstilosUI.estilizarCampoTexto(txtNombre, "Ej: Lionel Messi");
        gbc.gridx = 1;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelForm.add(txtNombre, gbc);

        JLabel lblDni = new JLabel("DNI:");
        EstilosUI.estilizarEtiquetaCampo(lblDni);
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panelForm.add(lblDni, gbc);

        EstilosUI.estilizarCampoTexto(txtDni, "Ej: 40111222");
        gbc.gridx = 3;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelForm.add(txtDni, gbc);

        JLabel lblFecha = new JLabel("Fecha de nacimiento:");
        EstilosUI.estilizarEtiquetaCampo(lblFecha);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panelForm.add(lblFecha, gbc);

        EstilosUI.estilizarCampoTexto(txtFechaNac, "dd/MM/yyyy");
        gbc.gridx = 1;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelForm.add(txtFechaNac, gbc);

        JLabel lblPosicion = new JLabel("Posición:");
        EstilosUI.estilizarEtiquetaCampo(lblPosicion);
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panelForm.add(lblPosicion, gbc);

        EstilosUI.estilizarCombo(cmbPosicion);
        gbc.gridx = 3;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelForm.add(cmbPosicion, gbc);

        JLabel lblEquipo = new JLabel("Equipo:");
        EstilosUI.estilizarEtiquetaCampo(lblEquipo);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panelForm.add(lblEquipo, gbc);

        EstilosUI.estilizarCombo(cmbEquipo);
        gbc.gridx = 1;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelForm.add(cmbEquipo, gbc);

        JLabel lblEstado = new JLabel("Estado:");
        EstilosUI.estilizarEtiquetaCampo(lblEstado);
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panelForm.add(lblEstado, gbc);

        EstilosUI.estilizarCombo(cmbEstado);
        gbc.gridx = 3;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelForm.add(cmbEstado, gbc);

        EstilosUI.estilizarEstadoInfo(lblInfo);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 4;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelForm.add(lblInfo, gbc);

        JButton btnGuardar = new JButton("Registrar jugador");
        EstilosUI.estilizarBotonPrimario(btnGuardar);
        btnGuardar.setEnabled(tieneAccesoAutorizado());
        btnGuardar.addActionListener(e -> btnGuardarActionPerformed(e));

        JButton btnActualizar = new JButton("Actualizar listado");
        EstilosUI.estilizarBotonNeutro(btnActualizar);
        btnActualizar.setEnabled(tieneAccesoAutorizado());
        btnActualizar.addActionListener(e -> cargarJugadores());

        JButton btnCancelar = new JButton("Volver al menú");
        EstilosUI.estilizarBotonNeutro(btnCancelar);
        btnCancelar.addActionListener(e -> dispose());

        JPanel panelAcciones = EstilosUI.crearBarraAcciones(
                new JComponent[]{btnGuardar, btnActualizar},
                new JComponent[]{btnCancelar});

        JScrollPane scroll = new JScrollPane(tablaJugadores);
        scroll.setPreferredSize(new Dimension(880, 220));
        EstilosUI.estilizarTabla(tablaJugadores, scroll);
        TableColumnModel columnas = tablaJugadores.getColumnModel();
        if (columnas.getColumnCount() >= 5) {
            columnas.getColumn(0).setPreferredWidth(100);
            columnas.getColumn(0).setMaxWidth(130);
            columnas.getColumn(0).setCellRenderer(EstilosUI.crearRendererCebra(SwingConstants.CENTER));
            columnas.getColumn(4).setPreferredWidth(120);
            columnas.getColumn(4).setCellRenderer(EstilosUI.crearRendererEstadoActivo("ACTIVO"));
        }

        JPanel panelCentro = new JPanel(new BorderLayout(12, 12));
        panelCentro.setBackground(EstilosUI.BG_CANVAS);
        panelCentro.add(panelForm, BorderLayout.NORTH);
        panelCentro.add(scroll, BorderLayout.CENTER);

        panel.add(panelEncabezado, BorderLayout.NORTH);
        panel.add(panelCentro, BorderLayout.CENTER);
        panel.add(panelAcciones, BorderLayout.SOUTH);
        setContentPane(panel);
        pack();
        setMinimumSize(new Dimension(920, 580));
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

    private boolean tieneAccesoAutorizado() {
        return PoliticaPermisos.tienePermisoOperacion(operador, PoliticaPermisos.ModuloMenu.ALTA_JUGADORES);
    }

    /** CU-02: el equipo es obligatorio (precondicion de la especificacion). */
    private void cargarEquipos() {
        cmbEquipo.removeAllItems();
        equiposCargados = new ArrayList<>();
        if (!tieneAccesoAutorizado()) {
            cmbEquipo.setEnabled(false);
            return;
        }
        try {
            if (operador.getRol() == TiposDatos.Rol.CAPITAN) {
                equiposCargados.addAll(new GestorEquipos().buscarEquiposActivosDeCapitan(operador.getId()));
                for (Equipo e : equiposCargados) {
                    cmbEquipo.addItem(e.getNombre());
                }
                cmbEquipo.setEnabled(equiposCargados.size() > 1);
            } else {
                cmbEquipo.setEnabled(true);
                equiposCargados.addAll(GestorEquipos.getEquiposRegistrados());
                for (Equipo e : equiposCargados) {
                    cmbEquipo.addItem(e.getNombre());
                }
            }
        } catch (Exception e) {
            lblInfo.setText("No se pudieron cargar los equipos registrados.");
        }
        if (equiposCargados.isEmpty()) {
            lblInfo.setText("No hay equipos activos registrados: registre o reactive un equipo para dar de alta jugadores.");
        }
    }

    private void cargarJugadores() {
        jugadoresCargados = new ArrayList<>();
        modeloTabla.setRowCount(0);
        if (!tieneAccesoAutorizado()) {
            lblInfo.setText("Acceso denegado: requiere sesión autorizada con identificador válido.");
            return;
        }
        try {
            jugadoresCargados = gestorJugadores.listarJugadoresAutorizados(operador);
        } catch (TorneoExcepcion ex) {
            lblInfo.setText(ex.getMessage());
            return;
        } catch (Exception e) {
            lblInfo.setText("No se pudo cargar el listado de jugadores.");
            return;
        }
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
        if (!tieneAccesoAutorizado()) {
            JOptionPane.showMessageDialog(this,
                    "Acceso denegado: requiere sesión autorizada con identificador válido.",
                    "Acceso denegado", JOptionPane.ERROR_MESSAGE);
            return;
        }
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
