package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.controlador.GestorCanchas;
import com.gestionamateur.tpi.progav.modelo.Cancha;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.seguridad.PoliticaPermisos;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
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
 * CU-16: gestion integral de canchas (alta, edicion, habilitacion e inhabilitacion).
 */
public class FrmAltaCancha extends JFrame {

    private final GestorCanchas gestorCanchas = new GestorCanchas();
    private final Usuario operador;
    private List<Cancha> canchasCargadas = new ArrayList<>();
    private Long canchaEnEdicion = null;

    private final JTextField txtNombre = new JTextField(22);
    private final JTextField txtDireccion = new JTextField(26);
    private final JComboBox<String> cmbSuperficie = new JComboBox<>();
    private final JLabel lblInfo = new JLabel(" ");

    private final JButton btnGuardar = new JButton("Guardar cancha");
    private final JButton btnLimpiar = new JButton("Nuevo / Limpiar");
    private final JButton btnEditar = new JButton("Editar seleccionada");
    private final JButton btnHabilitar = new JButton("Habilitar seleccionada");
    private final JButton btnInhabilitar = new JButton("Inhabilitar seleccionada");
    private final JButton btnActualizar = new JButton("Actualizar listado");

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Cancha", "Dirección", "Superficie", "Estado"}, 0) {
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
        setTitle("Gestión de Canchas (CU-16)");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        cargarCanchas();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        EstilosUI.estilizarVentana(panel);

        JPanel panelEncabezado = EstilosUI.crearEncabezado(
                "Gestión de canchas",
                "Alta, edición, disponibilidad y catálogo de predios deportivos (CU-16)");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 10);
        gbc.anchor = GridBagConstraints.WEST;

        JPanel panelForm = new JPanel(new GridBagLayout());
        EstilosUI.estilizarTarjeta(panelForm);

        JLabel lblSeccion = new JLabel("Datos de la cancha");
        EstilosUI.estilizarTituloSeccion(lblSeccion);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        panelForm.add(lblSeccion, gbc);

        JLabel lblNombre = new JLabel("Nombre del predio:");
        EstilosUI.estilizarEtiquetaCampo(lblNombre);
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panelForm.add(lblNombre, gbc);

        EstilosUI.estilizarCampoTexto(txtNombre, "Ej: Predio Polideportivo Norte");
        gbc.gridx = 1;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelForm.add(txtNombre, gbc);

        JLabel lblDireccion = new JLabel("Dirección física:");
        EstilosUI.estilizarEtiquetaCampo(lblDireccion);
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panelForm.add(lblDireccion, gbc);

        EstilosUI.estilizarCampoTexto(txtDireccion, "Ej: Av. Quaranta y Ruta 12");
        gbc.gridx = 3;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelForm.add(txtDireccion, gbc);

        JLabel lblSuperficie = new JLabel("Tipo de superficie:");
        EstilosUI.estilizarEtiquetaCampo(lblSuperficie);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panelForm.add(lblSuperficie, gbc);

        cmbSuperficie.addItem("Césped Natural");
        cmbSuperficie.addItem("Césped Sintético");
        cmbSuperficie.addItem("Tierra / Arena");
        cmbSuperficie.addItem("Cemento");
        EstilosUI.estilizarCombo(cmbSuperficie);
        gbc.gridx = 1;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelForm.add(cmbSuperficie, gbc);

        EstilosUI.estilizarEstadoInfo(lblInfo);
        gbc.gridx = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelForm.add(lblInfo, gbc);

        JScrollPane scroll = new JScrollPane(tablaCanchas);
        scroll.setPreferredSize(new Dimension(880, 230));
        EstilosUI.estilizarTabla(tablaCanchas, scroll);
        TableColumnModel columnas = tablaCanchas.getColumnModel();
        if (columnas.getColumnCount() >= 5) {
            columnas.getColumn(0).setPreferredWidth(60);
            columnas.getColumn(0).setMaxWidth(80);
            columnas.getColumn(0).setCellRenderer(EstilosUI.crearRendererCebra(SwingConstants.CENTER));
            columnas.getColumn(1).setPreferredWidth(220);
            columnas.getColumn(2).setPreferredWidth(280);
            columnas.getColumn(3).setPreferredWidth(160);
            columnas.getColumn(4).setPreferredWidth(130);
            columnas.getColumn(4).setCellRenderer(EstilosUI.crearRendererEstadoActivo("Activa"));
        }

        JPanel panelAccionesIzq = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        panelAccionesIzq.setBackground(EstilosUI.BG_CANVAS);

        EstilosUI.estilizarBotonPrimario(btnGuardar);
        btnGuardar.addActionListener(e -> guardar());

        EstilosUI.estilizarBotonNeutro(btnLimpiar);
        btnLimpiar.addActionListener(e -> limpiarFormulario());

        EstilosUI.estilizarBotonSecundario(btnEditar);
        btnEditar.addActionListener(e -> editarSeleccionada());

        EstilosUI.estilizarBotonSecundario(btnHabilitar);
        btnHabilitar.addActionListener(e -> cambiarEstado(true));

        EstilosUI.estilizarBotonAdvertencia(btnInhabilitar);
        btnInhabilitar.addActionListener(e -> cambiarEstado(false));

        EstilosUI.estilizarBotonNeutro(btnActualizar);
        btnActualizar.addActionListener(e -> cargarCanchas());

        panelAccionesIzq.add(btnGuardar);
        panelAccionesIzq.add(btnLimpiar);
        panelAccionesIzq.add(btnEditar);
        panelAccionesIzq.add(btnHabilitar);
        panelAccionesIzq.add(btnInhabilitar);
        panelAccionesIzq.add(btnActualizar);

        JPanel panelAccionesDer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        panelAccionesDer.setBackground(EstilosUI.BG_CANVAS);
        JButton btnCancelar = new JButton("Volver al menú");
        EstilosUI.estilizarBotonNeutro(btnCancelar);
        btnCancelar.addActionListener(e -> dispose());
        panelAccionesDer.add(btnCancelar);

        JPanel panelAcciones = new JPanel(new BorderLayout(8, 0));
        panelAcciones.setBackground(EstilosUI.BG_CANVAS);
        panelAcciones.add(panelAccionesIzq, BorderLayout.CENTER);
        panelAcciones.add(panelAccionesDer, BorderLayout.EAST);

        JPanel panelCentro = new JPanel(new BorderLayout(12, 12));
        panelCentro.setBackground(EstilosUI.BG_CANVAS);
        panelCentro.add(panelForm, BorderLayout.NORTH);
        panelCentro.add(scroll, BorderLayout.CENTER);

        panel.add(panelEncabezado, BorderLayout.NORTH);
        panel.add(panelCentro, BorderLayout.CENTER);
        panel.add(panelAcciones, BorderLayout.SOUTH);
        setContentPane(panel);
        pack();
        setMinimumSize(new Dimension(980, 580));
    }

    private boolean tieneAccesoAutorizado() {
        return PoliticaPermisos.tienePermisoOperacion(operador, PoliticaPermisos.ModuloMenu.GESTIONAR_CANCHAS);
    }

    private void cargarCanchas() {
        boolean autorizado = tieneAccesoAutorizado();
        btnGuardar.setEnabled(autorizado);
        btnLimpiar.setEnabled(autorizado);
        btnEditar.setEnabled(autorizado);
        btnHabilitar.setEnabled(autorizado);
        btnInhabilitar.setEnabled(autorizado);
        btnActualizar.setEnabled(autorizado);
        cmbSuperficie.setEnabled(autorizado);
        modeloTabla.setRowCount(0);
        canchasCargadas = new ArrayList<>();
        if (!autorizado) {
            lblInfo.setText("Acceso denegado: requiere sesión de Organizador o Administrador.");
            return;
        }
        try {
            canchasCargadas = gestorCanchas.listarCanchasParaGestion(operador);
        } catch (com.gestionamateur.tpi.progav.modelo.TorneoExcepcion ex) {
            canchasCargadas = new ArrayList<>();
            lblInfo.setText(ex.getMessage());
            return;
        }
        for (Cancha c : canchasCargadas) {
            modeloTabla.addRow(new Object[]{
                    c.getIdCancha(),
                    c.getNombre(),
                    c.getDireccion(),
                    c.getTipoSuperficie(),
                    c.isActiva() ? "Activa" : "Inhabilitada"});
        }
        if (canchasCargadas.isEmpty()) {
            lblInfo.setText("Todavía no hay canchas registradas.");
        } else if (canchaEnEdicion == null) {
            lblInfo.setText(canchasCargadas.size() + " cancha(s) registrada(s).");
        }
    }

    private void limpiarFormulario() {
        canchaEnEdicion = null;
        txtNombre.setText("");
        txtDireccion.setText("");
        if (cmbSuperficie.getItemCount() > 0) {
            cmbSuperficie.setSelectedIndex(0);
        }
        lblInfo.setText("Alta de cancha nueva.");
    }

    private void editarSeleccionada() {
        if (!tieneAccesoAutorizado()) {
            return;
        }
        int fila = tablaCanchas.getSelectedRow();
        if (fila < 0 || fila >= canchasCargadas.size()) {
            JOptionPane.showMessageDialog(this, "Seleccione la cancha a editar.", "Edición de cancha",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Cancha c = canchasCargadas.get(fila);
        canchaEnEdicion = c.getIdCancha();
        txtNombre.setText(c.getNombre());
        txtDireccion.setText(c.getDireccion());
        if (c.getTipoSuperficie() != null) {
            cmbSuperficie.setSelectedItem(c.getTipoSuperficie());
        }
        lblInfo.setText("Editando cancha #" + canchaEnEdicion + ". Pulse Guardar cancha para confirmar.");
    }

    private void guardar() {
        if (!tieneAccesoAutorizado()) {
            JOptionPane.showMessageDialog(this,
                    "Acceso denegado: requiere sesión de Organizador o Administrador.",
                    "Acceso denegado", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();
        String direccion = txtDireccion.getText() == null ? "" : txtDireccion.getText().trim();
        String superficie = (String) cmbSuperficie.getSelectedItem();

        if (nombre.isEmpty() || direccion.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Complete el nombre y la dirección de la cancha.",
                    "Campos vacíos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            String respuesta = canchaEnEdicion == null
                    ? gestorCanchas.registrarCancha(nombre, direccion, superficie, operador)
                    : gestorCanchas.actualizarCancha(canchaEnEdicion, nombre, direccion, superficie, operador);
            if (respuesta.startsWith("ERROR")) {
                JOptionPane.showMessageDialog(this, respuesta, "No se pudo guardar la cancha", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, respuesta, "Operación Exitosa", JOptionPane.INFORMATION_MESSAGE);
                limpiarFormulario();
                cargarCanchas();
            }
        } catch (com.gestionamateur.tpi.progav.modelo.TorneoExcepcion ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo guardar la cancha", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cambiarEstado(boolean activa) {
        if (!tieneAccesoAutorizado()) {
            JOptionPane.showMessageDialog(this,
                    "Acceso denegado: requiere sesión de Organizador o Administrador.",
                    "Acceso denegado", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int fila = tablaCanchas.getSelectedRow();
        if (fila < 0 || fila >= canchasCargadas.size()) {
            JOptionPane.showMessageDialog(this, "Seleccione la cancha de la tabla.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Cancha c = canchasCargadas.get(fila);
        if (c.isActiva() == activa) {
            JOptionPane.showMessageDialog(this,
                    "La cancha \"" + c.getNombre() + "\" ya se encuentra " + (activa ? "Activa." : "Inhabilitada."),
                    "Estado de cancha", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String pregunta = activa
                ? "¿Confirma habilitar la cancha \"" + c.getNombre() + "\"?"
                : "¿Confirma inhabilitar la cancha \"" + c.getNombre()
                + "\"?\nConservará su historial y no estará disponible para programar nuevos partidos.";
        int conf = JOptionPane.showConfirmDialog(this, pregunta,
                activa ? "Habilitar cancha" : "Inhabilitar cancha", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) {
            return;
        }
        String respuesta = gestorCanchas.cambiarEstado(c.getIdCancha(), activa, operador);
        if (respuesta.startsWith("ERROR")) {
            JOptionPane.showMessageDialog(this, respuesta, "No se pudo modificar la cancha", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, respuesta, "Operación Exitosa", JOptionPane.INFORMATION_MESSAGE);
            cargarCanchas();
        }
    }
}
