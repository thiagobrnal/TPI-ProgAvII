package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.modelo.Fase;
import com.gestionamateur.tpi.progav.modelo.Torneo;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.seguridad.PoliticaPermisos;
import com.gestionamateur.tpi.progav.servicio.TorneoService;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
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
 * CU-15: Gestion de torneos (actor: Administrador / Organizador).
 * Expone la capa de presentación sobre {@link TorneoService}: alta, edición,
 * esquema de fases (alta, edición, quita sin uso), cambio de estado y baja lógica (CANCELADO).
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
    private final JButton btnActualizar = new JButton("Actualizar listado");
    private final JButton btnCrear = new JButton("Crear torneo");
    private final JButton btnEditar = new JButton("Editar torneo");
    private final JButton btnVerFases = new JButton("Fases del torneo");
    private final JButton btnAgregarFase = new JButton("Agregar fase");
    private final JButton btnCambiarEstado = new JButton("Cambiar estado");
    private final JButton btnCancelarTorneo = new JButton("Cancelar torneo");

    public FrmTorneos(Usuario operador) {
        this.operador = operador;
        setTitle("Gestión de Torneos (CU-15)");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        cargarTorneos();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        EstilosUI.estilizarVentana(panel);

        JPanel panelEncabezado = EstilosUI.crearEncabezado(
                "Gestión de torneos",
                describirOperador());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JPanel panelSup = new JPanel(new GridBagLayout());
        EstilosUI.estilizarTarjeta(panelSup);

        JLabel lblSeccion = new JLabel("Torneos registrados");
        EstilosUI.estilizarTituloSeccion(lblSeccion);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelSup.add(lblSeccion, gbc);

        EstilosUI.estilizarTextoPrimario(lblSeleccion);
        gbc.gridy = 1;
        panelSup.add(lblSeleccion, gbc);

        EstilosUI.estilizarEstadoInfo(lblInfo);
        gbc.gridy = 2;
        panelSup.add(lblInfo, gbc);

        tablaTorneos.setAutoCreateRowSorter(false);
        tablaTorneos.getSelectionModel().addListSelectionListener(e -> actualizarSeleccion());

        JScrollPane scroll = new JScrollPane(tablaTorneos);
        scroll.setPreferredSize(new Dimension(900, 300));
        EstilosUI.estilizarTabla(tablaTorneos, scroll);
        TableColumnModel columnas = tablaTorneos.getColumnModel();
        if (columnas.getColumnCount() >= 6) {
            columnas.getColumn(0).setPreferredWidth(55);
            columnas.getColumn(0).setMaxWidth(75);
            columnas.getColumn(0).setCellRenderer(EstilosUI.crearRendererCebra(SwingConstants.CENTER));
            columnas.getColumn(3).setPreferredWidth(70);
            columnas.getColumn(3).setCellRenderer(EstilosUI.crearRendererCebra(SwingConstants.CENTER));
        }

        EstilosUI.estilizarBotonNeutro(btnActualizar);
        btnActualizar.addActionListener(e -> cargarTorneos());

        EstilosUI.estilizarBotonPrimario(btnCrear);
        btnCrear.addActionListener(e -> crearTorneo());

        EstilosUI.estilizarBotonSecundario(btnEditar);
        btnEditar.addActionListener(e -> editarTorneo());

        EstilosUI.estilizarBotonNeutro(btnVerFases);
        btnVerFases.addActionListener(e -> verFases());

        EstilosUI.estilizarBotonSecundario(btnAgregarFase);
        btnAgregarFase.addActionListener(e -> agregarFase());

        EstilosUI.estilizarBotonAdvertencia(btnCambiarEstado);
        btnCambiarEstado.addActionListener(e -> cambiarEstado());

        EstilosUI.estilizarBotonPeligro(btnCancelarTorneo);
        btnCancelarTorneo.addActionListener(e -> cancelarTorneo());

        JButton btnVolver = new JButton("Volver al menú");
        EstilosUI.estilizarBotonNeutro(btnVolver);
        btnVolver.addActionListener(e -> dispose());

        JPanel panelAcciones = EstilosUI.crearBarraAcciones(
                new JComponent[]{btnActualizar, btnCrear, btnEditar, btnVerFases, btnAgregarFase, btnCambiarEstado, btnCancelarTorneo},
                new JComponent[]{btnVolver});

        JPanel panelCentro = new JPanel(new BorderLayout(12, 12));
        panelCentro.setBackground(EstilosUI.BG_CANVAS);
        panelCentro.add(panelSup, BorderLayout.NORTH);
        panelCentro.add(scroll, BorderLayout.CENTER);

        panel.add(panelEncabezado, BorderLayout.NORTH);
        panel.add(panelCentro, BorderLayout.CENTER);
        panel.add(panelAcciones, BorderLayout.SOUTH);
        setContentPane(panel);
        pack();
        setMinimumSize(new Dimension(1120, 600));
    }

    private boolean tieneAccesoAutorizado() {
        return PoliticaPermisos.tienePermisoOperacion(operador, PoliticaPermisos.ModuloMenu.GESTIONAR_TORNEOS);
    }

    private String describirOperador() {
        if (!tieneAccesoAutorizado()) {
            return "Operador: sin autorización de Organizador o Administrador.";
        }
        return "Operador: " + operador.getNombreUsuario() + " (" + operador.getRol().name() + ")";
    }

    private void cargarTorneos() {
        boolean autorizado = tieneAccesoAutorizado();
        btnActualizar.setEnabled(autorizado);
        btnCrear.setEnabled(autorizado);
        btnEditar.setEnabled(autorizado);
        btnVerFases.setEnabled(autorizado);
        btnAgregarFase.setEnabled(autorizado);
        btnCambiarEstado.setEnabled(autorizado);
        btnCancelarTorneo.setEnabled(autorizado);
        if (!autorizado) {
            torneos.clear();
            modeloTabla.setRowCount(0);
            lblInfo.setText("Acceso denegado: requiere sesión de Organizador o Administrador.");
            lblSeleccion.setText(" ");
            return;
        }
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
                lblInfo.setText("Seleccione un torneo de la tabla para editarlo, gestionar sus fases, cambiar su estado o cancelarlo.");
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
        EstilosUI.estilizarCampoTexto(txtNombre, "Ej: Torneo Clausura 2026");
        EstilosUI.estilizarCombo(cmbFormato);
        EstilosUI.estilizarCampoTexto(txtCupo, "Ej: 8");
        EstilosUI.estilizarCampoTexto(txtFecha, "dd/MM/yyyy");

        JPanel form = new JPanel(new GridBagLayout());
        EstilosUI.estilizarPanelModal(form);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel lblNombre = new JLabel("Nombre del torneo:");
        EstilosUI.estilizarEtiquetaCampo(lblNombre);
        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(lblNombre, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        form.add(txtNombre, gbc);

        JLabel lblFormato = new JLabel("Formato:");
        EstilosUI.estilizarEtiquetaCampo(lblFormato);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        form.add(lblFormato, gbc);
        gbc.gridx = 1;
        form.add(cmbFormato, gbc);

        JLabel lblCupo = new JLabel("Cupo de equipos:");
        EstilosUI.estilizarEtiquetaCampo(lblCupo);
        gbc.gridx = 0;
        gbc.gridy = 2;
        form.add(lblCupo, gbc);
        gbc.gridx = 1;
        form.add(txtCupo, gbc);

        JLabel lblFecha = new JLabel("Fecha de inicio (dd/MM/yyyy):");
        EstilosUI.estilizarEtiquetaCampo(lblFecha);
        gbc.gridx = 0;
        gbc.gridy = 3;
        form.add(lblFecha, gbc);
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
        if (!tieneAccesoAutorizado()) {
            return;
        }
        Torneo t = obtenerTorneoSeleccionado();
        if (t == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un torneo de la tabla.", "Fases del torneo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        DefaultTableModel modeloFases = new DefaultTableModel(
                new Object[]{"ID", "Orden", "Nombre", "Limpia amarillas"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        List<Fase> fasesActuales = new ArrayList<>();
        JTable tabla = new JTable(modeloFases);
        tabla.setAutoCreateRowSorter(false);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setPreferredSize(new Dimension(640, 240));
        EstilosUI.estilizarTabla(tabla, scroll);

        JLabel lblEstadoFases = new JLabel(" ");
        EstilosUI.estilizarEstadoInfo(lblEstadoFases);

        Runnable recargarFases = () -> {
            fasesActuales.clear();
            modeloFases.setRowCount(0);
            try {
                List<Fase> cargadas = torneoService.listarFasesDeTorneo(t.getIdTorneo());
                if (cargadas != null) {
                    fasesActuales.addAll(cargadas);
                    for (Fase f : cargadas) {
                        modeloFases.addRow(new Object[]{
                                f.getIdFase(),
                                f.getOrden(),
                                f.getNombre(),
                                f.isLimpiaAmarillas() ? "Sí" : "No"});
                    }
                }
                lblEstadoFases.setText(fasesActuales.isEmpty()
                        ? "El torneo todavía no tiene fases configuradas."
                        : fasesActuales.size() + " fase(s) configurada(s).");
            } catch (TorneoExcepcion ex) {
                lblEstadoFases.setText("Error al consultar fases: " + ex.getMessage());
            }
        };
        recargarFases.run();

        JDialog dialog = new JDialog(this, "Fases de \"" + t.getNombre() + "\"", true);
        JPanel root = new JPanel(new BorderLayout(10, 10));
        EstilosUI.estilizarVentana(root);
        root.add(lblEstadoFases, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        botones.setBackground(EstilosUI.BG_CANVAS);

        JButton btnNuevaFase = new JButton("Agregar fase");
        EstilosUI.estilizarBotonPrimario(btnNuevaFase);
        btnNuevaFase.addActionListener(e -> {
            agregarFase();
            recargarFases.run();
        });

        JButton btnEditarFase = new JButton("Editar fase");
        EstilosUI.estilizarBotonSecundario(btnEditarFase);
        btnEditarFase.addActionListener(e -> {
            int row = tabla.getSelectedRow();
            if (row < 0 || row >= fasesActuales.size()) {
                JOptionPane.showMessageDialog(dialog, "Seleccione una fase de la tabla.", "Editar fase",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            editarFase(dialog, t, fasesActuales.get(row));
            recargarFases.run();
        });

        JButton btnQuitarFase = new JButton("Quitar fase sin partidos");
        EstilosUI.estilizarBotonPeligro(btnQuitarFase);
        btnQuitarFase.addActionListener(e -> {
            int row = tabla.getSelectedRow();
            if (row < 0 || row >= fasesActuales.size()) {
                JOptionPane.showMessageDialog(dialog, "Seleccione la fase a quitar.", "Quitar fase",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            Fase f = fasesActuales.get(row);
            int conf = JOptionPane.showConfirmDialog(dialog,
                    "¿Confirma quitar la fase \"" + f.getNombre() + "\" (orden " + f.getOrden() + ")?\nSolo es posible si el torneo está en CONFIGURACION y la fase no tiene partidos.",
                    "Confirmar quita de fase", JOptionPane.YES_NO_OPTION);
            if (conf != JOptionPane.YES_OPTION) {
                return;
            }
            try {
                torneoService.eliminarFase(f.getIdFase(), operador);
                JOptionPane.showMessageDialog(dialog, "ÉXITO: Fase quitada del esquema.", "Fases del torneo",
                        JOptionPane.INFORMATION_MESSAGE);
                recargarFases.run();
            } catch (TorneoExcepcion ex) {
                JOptionPane.showMessageDialog(dialog, "ERROR: " + ex.getMessage(), "No se pudo quitar la fase",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnCerrar = new JButton("Cerrar");
        EstilosUI.estilizarBotonNeutro(btnCerrar);
        btnCerrar.addActionListener(e -> dialog.dispose());

        botones.add(btnNuevaFase);
        botones.add(btnEditarFase);
        botones.add(btnQuitarFase);
        botones.add(btnCerrar);
        root.add(botones, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(700, 380));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void editarFase(JDialog parent, Torneo t, Fase f) {
        JTextField txtNombre = new JTextField(f.getNombre(), 20);
        JTextField txtOrden = new JTextField(String.valueOf(f.getOrden()), 4);
        JCheckBox chkLimpia = new JCheckBox("La fase reinicia la acumulación de amarillas", f.isLimpiaAmarillas());
        EstilosUI.estilizarCampoTexto(txtNombre, "Nombre de la fase");
        EstilosUI.estilizarCampoTexto(txtOrden, null);
        EstilosUI.estilizarCheckBox(chkLimpia);

        JPanel form = new JPanel(new GridBagLayout());
        EstilosUI.estilizarPanelModal(form);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel lblNombreFase = new JLabel("Nombre de la fase:");
        EstilosUI.estilizarEtiquetaCampo(lblNombreFase);
        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(lblNombreFase, gbc);
        gbc.gridx = 1;
        form.add(txtNombre, gbc);

        JLabel lblOrden = new JLabel("Orden de ejecución:");
        EstilosUI.estilizarEtiquetaCampo(lblOrden);
        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(lblOrden, gbc);
        gbc.gridx = 1;
        form.add(txtOrden, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        form.add(chkLimpia, gbc);

        int opcion = JOptionPane.showConfirmDialog(parent, form,
                "Editar fase #" + f.getIdFase() + " (" + t.getNombre() + ")",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }
        String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "El nombre de la fase es obligatorio.", "Editar fase",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int orden;
        try {
            orden = Integer.parseInt(txtOrden.getText() == null ? "" : txtOrden.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(parent, "El orden debe ser un número entero mayor a 0.", "Editar fase",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            torneoService.actualizarFase(f.getIdFase(), nombre, orden, chkLimpia.isSelected(), operador);
            JOptionPane.showMessageDialog(parent, "ÉXITO: Fase actualizada correctamente.", "Editar fase",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (TorneoExcepcion ex) {
            JOptionPane.showMessageDialog(parent, "ERROR: " + ex.getMessage(), "Editar fase",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editarTorneo() {
        if (!tieneAccesoAutorizado()) {
            return;
        }
        Torneo t = obtenerTorneoSeleccionado();
        if (t == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un torneo de la tabla.", "Editar torneo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        JTextField txtNombre = new JTextField(t.getNombre(), 24);
        JComboBox<String> cmbFormato = new JComboBox<>(FORMATOS_TORNEO);
        if (t.getFormato() != null) {
            cmbFormato.setSelectedItem(t.getFormato().name());
        }
        JTextField txtCupo = new JTextField(String.valueOf(t.getCupoEquipos()), 6);
        JTextField txtFecha = new JTextField(t.getFechaInicio() == null ? "" : FORMATO_FECHA.format(t.getFechaInicio()), 10);
        EstilosUI.estilizarCampoTexto(txtNombre, "Nombre único del torneo");
        EstilosUI.estilizarCombo(cmbFormato);
        EstilosUI.estilizarCampoTexto(txtCupo, "Cupo par");
        EstilosUI.estilizarCampoTexto(txtFecha, "dd/MM/yyyy");

        JPanel form = new JPanel(new GridBagLayout());
        EstilosUI.estilizarPanelModal(form);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel lblNombre = new JLabel("Nombre del torneo:");
        EstilosUI.estilizarEtiquetaCampo(lblNombre);
        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(lblNombre, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        form.add(txtNombre, gbc);

        JLabel lblFormato = new JLabel("Formato:");
        EstilosUI.estilizarEtiquetaCampo(lblFormato);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        form.add(lblFormato, gbc);
        gbc.gridx = 1;
        form.add(cmbFormato, gbc);

        JLabel lblCupo = new JLabel("Cupo de equipos:");
        EstilosUI.estilizarEtiquetaCampo(lblCupo);
        gbc.gridx = 0;
        gbc.gridy = 2;
        form.add(lblCupo, gbc);
        gbc.gridx = 1;
        form.add(txtCupo, gbc);

        JLabel lblFecha = new JLabel("Fecha de inicio (dd/MM/yyyy):");
        EstilosUI.estilizarEtiquetaCampo(lblFecha);
        gbc.gridx = 0;
        gbc.gridy = 3;
        form.add(lblFecha, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        form.add(txtFecha, gbc);

        int opcion = JOptionPane.showConfirmDialog(this, form, "Editar torneo #" + t.getIdTorneo(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }
        String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();
        String formato = (String) cmbFormato.getSelectedItem();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre del torneo es obligatorio.", "Editar torneo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int cupo;
        try {
            cupo = Integer.parseInt(txtCupo.getText() == null ? "" : txtCupo.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El cupo debe ser un número entero par mayor o igual a 2.",
                    "Editar torneo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Date fechaInicio;
        try {
            fechaInicio = parsearFecha(txtFecha.getText());
        } catch (ParseException ex) {
            JOptionPane.showMessageDialog(this, "Fecha inválida. Use el formato dd/MM/yyyy.",
                    "Editar torneo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int conf = JOptionPane.showConfirmDialog(this,
                "¿Confirma guardar los cambios del torneo \"" + nombre + "\"?",
                "Confirmar edición", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            torneoService.actualizarTorneo(t.getIdTorneo(), nombre, formato, cupo, fechaInicio, operador);
            JOptionPane.showMessageDialog(this, "ÉXITO: Torneo actualizado correctamente.", "Editar torneo",
                    JOptionPane.INFORMATION_MESSAGE);
            cargarTorneos();
        } catch (TorneoExcepcion ex) {
            JOptionPane.showMessageDialog(this, "ERROR: " + ex.getMessage(), "Editar torneo",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelarTorneo() {
        if (!tieneAccesoAutorizado()) {
            return;
        }
        Torneo t = obtenerTorneoSeleccionado();
        if (t == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un torneo de la tabla.", "Cancelar torneo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int conf = JOptionPane.showConfirmDialog(this,
                "¿Confirma la baja lógica (CANCELADO) del torneo \"" + t.getNombre()
                        + "\"?\nConservará su historial y partidos ya registrados, y no podrá reactivarse.",
                "Confirmar cancelación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conf != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            torneoService.cancelarTorneo(t.getIdTorneo(), operador);
            JOptionPane.showMessageDialog(this, "ÉXITO: El torneo \"" + t.getNombre() + "\" pasó a estado CANCELADO.",
                    "Cancelar torneo", JOptionPane.INFORMATION_MESSAGE);
            cargarTorneos();
        } catch (TorneoExcepcion ex) {
            JOptionPane.showMessageDialog(this, "ERROR: " + ex.getMessage(), "Cancelar torneo",
                    JOptionPane.ERROR_MESSAGE);
        }
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
        EstilosUI.estilizarCampoTexto(txtNombre, "Ej: Semifinal");
        EstilosUI.estilizarCampoTexto(txtOrden, null);
        EstilosUI.estilizarCheckBox(chkLimpia);

        JPanel form = new JPanel(new GridBagLayout());
        EstilosUI.estilizarPanelModal(form);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel lblTorneo = new JLabel("Torneo:");
        EstilosUI.estilizarEtiquetaCampo(lblTorneo);
        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(lblTorneo, gbc);
        JLabel lblTorneoVal = new JLabel(t.getNombre() + " (ID " + t.getIdTorneo() + ")");
        EstilosUI.estilizarTextoPrimario(lblTorneoVal);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        form.add(lblTorneoVal, gbc);

        JLabel lblNombreFase = new JLabel("Nombre de la fase:");
        EstilosUI.estilizarEtiquetaCampo(lblNombreFase);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        form.add(lblNombreFase, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        form.add(txtNombre, gbc);

        JLabel lblOrden = new JLabel("Orden de ejecución:");
        EstilosUI.estilizarEtiquetaCampo(lblOrden);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        form.add(lblOrden, gbc);
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
        EstilosUI.estilizarCombo(cmbEstado);

        JPanel form = new JPanel(new GridBagLayout());
        EstilosUI.estilizarPanelModal(form);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel lblTorneo = new JLabel("Torneo:");
        EstilosUI.estilizarEtiquetaCampo(lblTorneo);
        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(lblTorneo, gbc);
        JLabel lblTorneoVal = new JLabel(t.getNombre() + " (estado actual: " + t.getEstado() + ")");
        EstilosUI.estilizarTextoPrimario(lblTorneoVal);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        form.add(lblTorneoVal, gbc);

        JLabel lblNuevoEstado = new JLabel("Nuevo estado:");
        EstilosUI.estilizarEtiquetaCampo(lblNuevoEstado);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        form.add(lblNuevoEstado, gbc);
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
