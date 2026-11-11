package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.modelo.Fase;
import com.gestionamateur.tpi.progav.modelo.Torneo;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.servicio.TorneoService;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * CU-15: Gestion de torneos (actor: Administrador).
 * Expone la capa de presentación sobre {@link TorneoService}: alta de torneo,
 * esquema de fases, cambio de estado y consulta del listado.
 *
 * Todas las operaciones que mutan la base de datos se delegan al servicio de dominio
 * pasando siempre el operador de la sesión, de modo que la autorización del
 * servicio (fail-closed ante operador nulo o sin permisos) se mantiene intacta.
 */
public class FrmTorneos extends JFrame {

    private static final SimpleDateFormat FORMATO_FECHA = new SimpleDateFormat("dd/MM/yyyy");

    private static final String[] FORMATOS_TORNEO = {"LIGA", "ELIMINATORIA", "GRUPOS"};
    private static final String[] ESTADOS_TORNEO = {"CONFIGURACION", "EN_CURSO", "FINALIZADO"};

    private final Usuario operador;
    private final TorneoService torneoService = new TorneoService();

    private final List<Torneo> torneos = new ArrayList<>();

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Nombre", "Formato", "Cupo", "Fecha inicio", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tablaTorneos = new JTable(modeloTabla);
    private final JLabel lblInfo = new JLabel(" ");
    private final JLabel lblSeleccion = new JLabel(" ");

    public FrmTorneos(Usuario operador) {
        this.operador = operador;
        setTitle("Gestión de Torneos (CU-15)");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        cargarTorneos();
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

        JLabel titulo = new JLabel("Gestión de torneos");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(new Color(0x12, 0x38, 0x5C));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        panelSup.add(titulo, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        JLabel lblOperador = new JLabel(describirOperador());
        lblOperador.setForeground(new Color(0x3A, 0x55, 0x68));
        panelSup.add(lblOperador, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        lblSeleccion.setForeground(new Color(0x3A, 0x55, 0x68));
        panelSup.add(lblSeleccion, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        lblInfo.setForeground(new Color(0x3A, 0x55, 0x68));
        panelSup.add(lblInfo, gbc);

        tablaTorneos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaTorneos.setAutoCreateRowSorter(false);
        tablaTorneos.getSelectionModel().addListSelectionListener(e -> actualizarSeleccion());

        JScrollPane scroll = new JScrollPane(tablaTorneos);
        scroll.setPreferredSize(new Dimension(900, 300));

        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton btnActualizar = new JButton("Actualizar listado");
        btnActualizar.addActionListener(e -> cargarTorneos());
        JButton btnCrear = new JButton("Crear torneo");
        btnCrear.addActionListener(e -> crearTorneo());
        JButton btnVerFases = new JButton("Ver fases del torneo");
        btnVerFases.addActionListener(e -> verFases());
        JButton btnAgregarFase = new JButton("Agregar fase");
        btnAgregarFase.addActionListener(e -> agregarFase());
        JButton btnCambiarEstado = new JButton("Cambiar estado");
        btnCambiarEstado.addActionListener(e -> cambiarEstado());
        JButton btnVolver = new JButton("Volver al menú");
        btnVolver.addActionListener(e -> dispose());
        panelAcciones.add(btnActualizar);
        panelAcciones.add(btnCrear);
        panelAcciones.add(btnVerFases);
        panelAcciones.add(btnAgregarFase);
        panelAcciones.add(btnCambiarEstado);
        panelAcciones.add(btnVolver);

        JPanel panelCentro = new JPanel(new BorderLayout(6, 6));
        panelCentro.setBackground(Color.WHITE);
        panelCentro.add(panelSup, BorderLayout.NORTH);
        panelCentro.add(scroll, BorderLayout.CENTER);

        panel.add(panelCentro, BorderLayout.CENTER);
        panel.add(panelAcciones, BorderLayout.SOUTH);
        setContentPane(panel);
        pack();
        setMinimumSize(new Dimension(940, 560));
    }

    private String describirOperador() {
        if (operador == null) {
            return "Operador: no identificado. Solo se habilita la consulta; las operaciones de escritura fueron rechazadas.";
        }
        return "Operador: " + operador.getNombreUsuario() + " (" + operador.getRol().name() + ")";
    }

    private void cargarTorneos() {
        try {
            List<Torneo> cargados = torneoService.listarTodos();
            torneos.clear();
            if (cargados != null) {
                torneos.addAll(cargados);
            }
            modeloTabla.setRowCount(0);
            for (Torneo t : torneos) {
                modeloTabla.addRow(new Object[]{
                        t.getIdTorneo(),
                        t.getNombre(),
                        t.getFormato() != null ? t.getFormato().name() : "",
                        t.getCupoEquipos(),
                        t.getFechaInicio() == null ? "" : FORMATO_FECHA.format(t.getFechaInicio()),
                        t.getEstado()});
            }
            if (torneos.isEmpty()) {
                lblInfo.setText("Todavía no hay torneos registrados en el sistema.");
                lblSeleccion.setText(" ");
            } else {
                lblInfo.setText("Seleccione un torneo de la tabla para operar sobre sus fases y su estado.");
                lblSeleccion.setText(" ");

            }
        } catch (TorneoExcepcion ex) {
            lblInfo.setText("No se pudo cargar el listado de torneos: " + ex.getMessage());
        }
    }

    private void actualizarSeleccion() {
        Torneo t = obtenerTorneoSeleccionado();
        if (t == null) {
            lblSeleccion.setText("No hay ningún torneo seleccionado.");
        } else {
            lblSeleccion.setText("Torneo seleccionado: " + t.getNombre() + " (ID " + t.getIdTorneo() + ") - Estado: " + t.getEstado());
        }
    }

    private Torneo obtenerTorneoSeleccionado() {
        int fila = tablaTorneos.getSelectedRow();
        if (fila < 0 || fila >= torneos.size()) {
            return null;
        }
        return torneos.get(fila);
    }

    private void crearTorneo() {
        JTextField txtNombre = new JTextField(24);
        JComboBox<String> cmbFormato = new JComboBox<>(FORMATOS_TORNEO);
        JTextField txtCupo = new JTextField(6);
        JTextField txtFecha = new JTextField(10);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Nombre del torneo:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        form.add(txtNombre, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        form.add(new JLabel("Formato:"), gbc);
        gbc.gridx = 1;
        form.add(cmbFormato, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        form.add(new JLabel("Cupo de equipos:"), gbc);
        gbc.gridx = 1;
        form.add(txtCupo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        form.add(new JLabel("Fecha de inicio (dd/MM/yyyy):"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        form.add(txtFecha, gbc);

        int opcion = JOptionPane.showConfirmDialog(this, form, "Nuevo torneo",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }

        String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();
        String formato = (String) cmbFormato.getSelectedItem();
        String cupoTexto = txtCupo.getText() == null ? "" : txtCupo.getText().trim();
        String fechaTexto = txtFecha.getText() == null ? "" : txtFecha.getText().trim();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre del torneo es obligatorio.", "Alta de torneo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int cupo;
        try {
            cupo = Integer.parseInt(cupoTexto);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El cupo de equipos debe ser un número entero.",
                    "Alta de torneo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (cupo < 2) {
            JOptionPane.showMessageDialog(this, "El cupo debe ser de al menos 2 equipos.", "Alta de torneo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (cupo % 2 != 0) {
            JOptionPane.showMessageDialog(this, "El cupo de equipos debe ser un número par para garantizar los emparejamientos.",
                    "Alta de torneo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Date fechaInicio;
        try {
            fechaInicio = parsearFecha(fechaTexto);
        } catch (ParseException ex) {
            JOptionPane.showMessageDialog(this, "La fecha de inicio no es válida. Use el formato dd/MM/yyyy.",
                    "Alta de torneo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Confirma el alta del torneo \"" + nombre + "\" con " + cupo + " equipos?",
                "Confirmar alta", JOptionPane.YES_NO_OPTION);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        // La base de datos exige unicidad del nombre del torneo.
        for (Torneo existente : torneos) {
            if (existente.getNombre() != null && existente.getNombre().equalsIgnoreCase(nombre)) {
                JOptionPane.showMessageDialog(this,
                        "Ya existe un torneo con el nombre \"" + nombre + "\". El nombre del torneo debe ser único.",
                        "Alta de torneo", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        try {
            Torneo creado = torneoService.crearTorneo(nombre, formato, cupo, fechaInicio, operador);
            JOptionPane.showMessageDialog(this,
                    "ÉXITO: Torneo \"" + nombre + "\" registrado con ID " + (creado != null ? creado.getIdTorneo() : "N/D") + ".",
                    "Alta de torneo", JOptionPane.INFORMATION_MESSAGE);
            cargarTorneos();
        } catch (TorneoExcepcion ex) {
            JOptionPane.showMessageDialog(this, "ERROR: " + ex.getMessage(), "Alta de torneo",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private Date parsearFecha(String texto) throws ParseException {
        if (texto == null || texto.trim().isEmpty()) {
            throw new ParseException("Fecha vacía", 0);
        }
        SimpleDateFormat parser = new SimpleDateFormat("dd/MM/yyyy");
        parser.setLenient(false);
        return parser.parse(texto.trim());
    }

    private void verFases() {
        Torneo t = obtenerTorneoSeleccionado();
        if (t == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un torneo de la tabla.", "Fases del torneo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Fase> fases;
        try {
            fases = torneoService.listarFasesDeTorneo(t.getIdTorneo());
        } catch (TorneoExcepcion ex) {
            JOptionPane.showMessageDialog(this, "ERROR: " + ex.getMessage(), "Fases del torneo",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        DefaultTableModel modeloFases = new DefaultTableModel(
                new Object[]{"ID", "Orden", "Nombre", "Limpia amarillas"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        if (fases != null) {
            for (Fase f : fases) {
                modeloFases.addRow(new Object[]{
                        f.getIdFase(),
                        f.getOrden(),
                        f.getNombre(),
                        f.isLimpiaAmarillas() ? "Sí" : "No"});
            }
        }

        JTable tabla = new JTable(modeloFases);
        tabla.setAutoCreateRowSorter(false);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setPreferredSize(new Dimension(560, 220));

        String mensaje;
        if (fases == null || fases.isEmpty()) {
            mensaje = "El torneo \"" + t.getNombre() + "\" todavía no tiene fases configuradas.";
        } else {
            mensaje = "Esquema de fases del torneo \"" + t.getNombre() + "\":";
        }
        JOptionPane.showMessageDialog(this, scroll, mensaje, JOptionPane.INFORMATION_MESSAGE);
    }

    private void agregarFase() {
        Torneo t = obtenerTorneoSeleccionado();
        if (t == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un torneo de la tabla.", "Alta de fase",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int ordenSugerido = 1;
        List<Fase> fasesExistentes;
        try {
            fasesExistentes = torneoService.listarFasesDeTorneo(t.getIdTorneo());
            if (fasesExistentes != null) {
                for (Fase f : fasesExistentes) {
                    if (f.getOrden() >= ordenSugerido) {
                        ordenSugerido = f.getOrden() + 1;
                    }
                }
            }
        } catch (TorneoExcepcion ex) {
            JOptionPane.showMessageDialog(this, "ERROR: " + ex.getMessage(), "Alta de fase",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        JTextField txtNombre = new JTextField(20);
        JTextField txtOrden = new JTextField(String.valueOf(ordenSugerido), 4);
        JCheckBox chkLimpia = new JCheckBox("La fase reinicia la acumulación de amarillas");

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Torneo:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        form.add(new JLabel(t.getNombre() + " (ID " + t.getIdTorneo() + ")"), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        form.add(new JLabel("Nombre de la fase:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        form.add(txtNombre, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        form.add(new JLabel("Orden de ejecución:"), gbc);
        gbc.gridx = 1;
        form.add(txtOrden, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        form.add(chkLimpia, gbc);

        int opcion = JOptionPane.showConfirmDialog(this, form, "Nueva fase en \"" + t.getNombre() + "\"",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }

        String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre de la fase es obligatorio.", "Alta de fase",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int orden;
        try {
            orden = Integer.parseInt(txtOrden.getText() == null ? "" : txtOrden.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El orden de la fase debe ser un número entero.",
                    "Alta de fase", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (orden <= 0) {
            JOptionPane.showMessageDialog(this, "El orden de ejecución de la fase debe ser mayor a 0.",
                    "Alta de fase", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // La base de datos exige unicidad de (torneo, orden) y de (torneo, nombre).
        if (fasesExistentes != null) {
            for (Fase f : fasesExistentes) {
                if (f.getOrden() == orden) {
                    JOptionPane.showMessageDialog(this,
                            "Ya existe una fase con el orden " + orden + " en este torneo (\""
                                    + f.getNombre() + "\"). Elija otro orden de ejecución.",
                            "Alta de fase", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (f.getNombre() != null && f.getNombre().equalsIgnoreCase(nombre)) {
                    JOptionPane.showMessageDialog(this,
                            "Ya existe una fase con el nombre \"" + nombre + "\" en este torneo. "
                                    + "El nombre de la fase debe ser único por torneo.",
                            "Alta de fase", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
        }

        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Confirma agregar la fase \"" + nombre + "\" (orden " + orden + ") al torneo \"" + t.getNombre() + "\"?",
                "Confirmar alta de fase", JOptionPane.YES_NO_OPTION);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            Fase fase = torneoService.agregarFase(t.getIdTorneo(), nombre, orden, chkLimpia.isSelected(), operador);
            JOptionPane.showMessageDialog(this,
                    "ÉXITO: Fase \"" + nombre + "\" agregada al torneo \"" + t.getNombre() + "\" (ID "
                            + (fase != null ? fase.getIdFase() : "N/D") + ").",
                    "Alta de fase", JOptionPane.INFORMATION_MESSAGE);
            cargarTorneos();
        } catch (TorneoExcepcion ex) {
            JOptionPane.showMessageDialog(this, "ERROR: " + ex.getMessage(), "Alta de fase",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cambiarEstado() {
        Torneo t = obtenerTorneoSeleccionado();
        if (t == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un torneo de la tabla.", "Cambio de estado",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<String> cmbEstado = new JComboBox<>(ESTADOS_TORNEO);
        cmbEstado.setSelectedItem(t.getEstado());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Torneo:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        form.add(new JLabel(t.getNombre() + " (estado actual: " + t.getEstado() + ")"), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        form.add(new JLabel("Nuevo estado:"), gbc);
        gbc.gridx = 1;
        form.add(cmbEstado, gbc);

        int opcion = JOptionPane.showConfirmDialog(this, form, "Cambiar estado del torneo",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }

        String nuevoEstado = (String) cmbEstado.getSelectedItem();
        if (nuevoEstado == null || nuevoEstado.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un estado válido.", "Cambio de estado",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Confirma pasar el torneo \"" + t.getNombre() + "\" al estado " + nuevoEstado + "?",
                "Confirmar cambio de estado", JOptionPane.YES_NO_OPTION);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            torneoService.actualizarEstadoTorneo(t.getIdTorneo(), nuevoEstado, operador);
            JOptionPane.showMessageDialog(this,
                    "ÉXITO: El torneo \"" + t.getNombre() + "\" quedó en estado " + nuevoEstado + ".",
                    "Cambio de estado", JOptionPane.INFORMATION_MESSAGE);
            cargarTorneos();
        } catch (TorneoExcepcion ex) {
            JOptionPane.showMessageDialog(this, "ERROR: " + ex.getMessage(), "Cambio de estado",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
