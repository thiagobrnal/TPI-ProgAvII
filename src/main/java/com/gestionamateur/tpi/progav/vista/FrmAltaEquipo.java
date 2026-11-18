package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.controlador.GestorEquipos;
import com.gestionamateur.tpi.progav.controlador.GestorUsuarios;
import com.gestionamateur.tpi.progav.dao.CanchaDAO;
import com.gestionamateur.tpi.progav.dao.CategoriaDAO;
import com.gestionamateur.tpi.progav.modelo.Cancha;
import com.gestionamateur.tpi.progav.modelo.Categoria;
import com.gestionamateur.tpi.progav.modelo.Equipo;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.seguridad.PoliticaPermisos;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
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
 * CU-01: gestion integral de equipos (alta, edicion, baja logica / reactivacion).
 */
public class FrmAltaEquipo extends JFrame {

    private final GestorEquipos gestorEquipos = new GestorEquipos();
    private final GestorUsuarios gestorUsuarios = new GestorUsuarios();
    private final Usuario operador;

    private List<Categoria> categoriasCargadas = new ArrayList<>();
    private List<Usuario> capitanesCargados = new ArrayList<>();
    private List<Cancha> canchasCargadas = new ArrayList<>();
    private List<Equipo> equiposCargados = new ArrayList<>();
    private Long equipoEnEdicion = null;

    private final JTextField txtNombre = new JTextField(20);
    private final JTextField txtAlias = new JTextField(12);
    private final JTextField txtColor = new JTextField(14);
    private final JComboBox<String> cmbCategoria = new JComboBox<>();
    private final JComboBox<String> cmbCapitan = new JComboBox<>();
    private final JComboBox<String> cmbSede = new JComboBox<>();
    private final JLabel lblEstado = new JLabel(" ");

    private final JButton btnGuardar = new JButton("Guardar equipo");
    private final JButton btnNuevo = new JButton("Nuevo / Limpiar");
    private final JButton btnEditar = new JButton("Editar seleccionado");
    private final JButton btnBaja = new JButton("Dar de baja");
    private final JButton btnReactivar = new JButton("Reactivar");
    private final JButton btnActualizar = new JButton("Actualizar listado");

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Equipo", "Alias", "Categoría", "Color", "Sede habitual", "Capitán", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tablaEquipos = new JTable(modeloTabla);

    public FrmAltaEquipo() {
        this(null);
    }

    public FrmAltaEquipo(Usuario operador) {
        this.operador = operador;
        setTitle("Gestión de Equipos (CU-01)");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        cargarCategorias();
        cargarCapitanes();
        cargarSedes();
        cargarEquipos();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        EstilosUI.estilizarVentana(panel);

        JPanel panelEncabezado = EstilosUI.crearEncabezado(
                "Gestión de equipos",
                "Alta, edición, baja lógica y configuración deportiva de equipos (CU-01)");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 10);
        gbc.anchor = GridBagConstraints.WEST;

        JPanel panelForm = new JPanel(new GridBagLayout());
        EstilosUI.estilizarTarjeta(panelForm);

        JLabel lblSeccion = new JLabel("Datos del equipo");
        EstilosUI.estilizarTituloSeccion(lblSeccion);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        panelForm.add(lblSeccion, gbc);

        JLabel lblNombre = new JLabel("Nombre:");
        EstilosUI.estilizarEtiquetaCampo(lblNombre);
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panelForm.add(lblNombre, gbc);

        EstilosUI.estilizarCampoTexto(txtNombre, "Ej: La Scaloneta FC");
        gbc.gridx = 1;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelForm.add(txtNombre, gbc);

        JLabel lblColor = new JLabel("Color de camiseta:");
        EstilosUI.estilizarEtiquetaCampo(lblColor);
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panelForm.add(lblColor, gbc);

        EstilosUI.estilizarCampoTexto(txtColor, "Ej: Celeste y Blanca");
        gbc.gridx = 3;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelForm.add(txtColor, gbc);

        JLabel lblCategoria = new JLabel("Categoría:");
        EstilosUI.estilizarEtiquetaCampo(lblCategoria);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panelForm.add(lblCategoria, gbc);

        EstilosUI.estilizarCombo(cmbCategoria);
        gbc.gridx = 1;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelForm.add(cmbCategoria, gbc);

        JLabel lblCapitan = new JLabel("Capitán responsable:");
        EstilosUI.estilizarEtiquetaCampo(lblCapitan);
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panelForm.add(lblCapitan, gbc);

        EstilosUI.estilizarCombo(cmbCapitan);
        gbc.gridx = 3;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelForm.add(cmbCapitan, gbc);

        JLabel lblSede = new JLabel("Sede habitual:");
        EstilosUI.estilizarEtiquetaCampo(lblSede);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panelForm.add(lblSede, gbc);

        EstilosUI.estilizarCombo(cmbSede);
        gbc.gridx = 1;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelForm.add(cmbSede, gbc);

        JLabel lblAlias = new JLabel("Alias / Sigla:");
        EstilosUI.estilizarEtiquetaCampo(lblAlias);
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panelForm.add(lblAlias, gbc);

        EstilosUI.estilizarCampoTexto(txtAlias, "Ej: LSC (opcional)");
        gbc.gridx = 3;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelForm.add(txtAlias, gbc);

        EstilosUI.estilizarEstadoInfo(lblEstado);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 4;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelForm.add(lblEstado, gbc);

        EstilosUI.estilizarBotonPrimario(btnGuardar);
        btnGuardar.addActionListener(e -> guardar());

        EstilosUI.estilizarBotonNeutro(btnNuevo);
        btnNuevo.addActionListener(e -> limpiarFormulario());

        EstilosUI.estilizarBotonSecundario(btnEditar);
        btnEditar.addActionListener(e -> editarSeleccionado());

        EstilosUI.estilizarBotonPeligro(btnBaja);
        btnBaja.addActionListener(e -> cambiarEstadoEquipo(false));

        EstilosUI.estilizarBotonSecundario(btnReactivar);
        btnReactivar.addActionListener(e -> cambiarEstadoEquipo(true));

        EstilosUI.estilizarBotonNeutro(btnActualizar);
        btnActualizar.addActionListener(e -> cargarEquipos());

        JButton btnCancelar = new JButton("Volver al menú");
        EstilosUI.estilizarBotonNeutro(btnCancelar);
        btnCancelar.addActionListener(e -> dispose());

        JPanel panelAcciones = EstilosUI.crearBarraAcciones(
                new JComponent[]{btnGuardar, btnNuevo, btnEditar, btnBaja, btnReactivar, btnActualizar},
                new JComponent[]{btnCancelar});

        JScrollPane scroll = new JScrollPane(tablaEquipos);
        scroll.setPreferredSize(new Dimension(920, 220));
        EstilosUI.estilizarTabla(tablaEquipos, scroll);
        TableColumnModel columnas = tablaEquipos.getColumnModel();
        if (columnas.getColumnCount() >= 8) {
            columnas.getColumn(0).setPreferredWidth(50);
            columnas.getColumn(0).setMaxWidth(70);
            columnas.getColumn(0).setCellRenderer(EstilosUI.crearRendererCebra(SwingConstants.CENTER));
            columnas.getColumn(2).setPreferredWidth(85);
            columnas.getColumn(7).setPreferredWidth(100);
            columnas.getColumn(7).setCellRenderer(EstilosUI.crearRendererEstadoActivo("ACTIVO"));
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
        setMinimumSize(new Dimension(980, 600));
    }

    private boolean tieneAccesoAutorizado() {
        return PoliticaPermisos.tienePermisoOperacion(operador, PoliticaPermisos.ModuloMenu.GESTIONAR_EQUIPOS);
    }

    private void cargarCategorias() {
        cmbCategoria.removeAllItems();
        categoriasCargadas.clear();
        if (!tieneAccesoAutorizado()) {
            cmbCategoria.setEnabled(false);
            return;
        }
        cmbCategoria.setEnabled(true);
        try {
            categoriasCargadas = new CategoriaDAO().buscarTodas();
            for (Categoria cat : categoriasCargadas) {
                cmbCategoria.addItem(cat.getNombre() + " (" + cat.getGenero() + ")");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar categorías desde BD: " + e.getMessage(),
                    "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void cargarCapitanes() {
        cmbCapitan.removeAllItems();
        capitanesCargados.clear();
        if (!tieneAccesoAutorizado()) {
            cmbCapitan.setEnabled(false);
            return;
        }
        if (operador.getRol() == TiposDatos.Rol.CAPITAN) {
            cmbCapitan.addItem(operador.getNombre() + " " + operador.getApellido()
                    + " (" + operador.getNombreUsuario() + ")");
            capitanesCargados.add(operador);
            cmbCapitan.setEnabled(false);
        } else {
            cmbCapitan.setEnabled(true);
            List<Usuario> caps = gestorUsuarios.getCapitanesRegistrados();
            capitanesCargados.addAll(caps);
            for (Usuario u : caps) {
                cmbCapitan.addItem(u.getNombre() + " " + u.getApellido() + " (" + u.getNombreUsuario() + ")");
            }
        }
    }

    private void cargarSedes() {
        cmbSede.removeAllItems();
        canchasCargadas.clear();
        cmbSede.addItem("(sin sede habitual)");
        if (!tieneAccesoAutorizado()) {
            cmbSede.setEnabled(false);
            return;
        }
        cmbSede.setEnabled(true);
        try {
            canchasCargadas = new CanchaDAO().buscarTodas();
            for (Cancha c : canchasCargadas) {
                cmbSede.addItem(c.getNombre() + " (" + c.getDireccion() + ")");
            }
        } catch (Exception e) {
            // Se conserva la opción "(sin sede habitual)"
        }
    }

    private void cargarEquipos() {
        boolean autorizado = tieneAccesoAutorizado();
        btnGuardar.setEnabled(autorizado);
        btnNuevo.setEnabled(autorizado);
        btnEditar.setEnabled(autorizado);
        btnBaja.setEnabled(autorizado);
        btnReactivar.setEnabled(autorizado);
        btnActualizar.setEnabled(autorizado);
        modeloTabla.setRowCount(0);
        equiposCargados = new ArrayList<>();
        if (!autorizado) {
            lblEstado.setText("Acceso denegado: requiere sesión de Capitán, Organizador o Administrador.");
            return;
        }
        try {
            equiposCargados = gestorEquipos.listarEquiposParaGestion(operador);
        } catch (com.gestionamateur.tpi.progav.modelo.TorneoExcepcion ex) {
            equiposCargados = new ArrayList<>();
            lblEstado.setText(ex.getMessage());
            return;
        }
        for (Equipo e : equiposCargados) {
            modeloTabla.addRow(new Object[]{
                    e.getIdEquipo(),
                    e.getNombre(),
                    e.getAlias() == null ? "" : e.getAlias(),
                    e.getCategoria() == null ? "" : e.getCategoria().getNombre(),
                    e.getColorCamiseta(),
                    e.getSedeHabitual() == null ? "(sin sede)" : e.getSedeHabitual().getNombre(),
                    e.getCapitan() == null ? "(sin capitán)" : e.getCapitan().getNombreUsuario(),
                    e.isActivo() ? "ACTIVO" : "INACTIVO"});
        }
        if (equiposCargados.isEmpty()) {
            lblEstado.setText("Todavía no hay equipos registrados.");
        } else if (equipoEnEdicion == null) {
            lblEstado.setText(equiposCargados.size() + " equipo(s) registrado(s).");
        }
    }

    private void limpiarFormulario() {
        equipoEnEdicion = null;
        txtNombre.setText("");
        txtAlias.setText("");
        txtColor.setText("");
        if (cmbCategoria.getItemCount() > 0) {
            cmbCategoria.setSelectedIndex(0);
        }
        if (cmbSede.getItemCount() > 0) {
            cmbSede.setSelectedIndex(0);
        }
        if (cmbCapitan.isEnabled() && cmbCapitan.getItemCount() > 0) {
            cmbCapitan.setSelectedIndex(0);
        }
        lblEstado.setText("Alta de equipo nuevo.");
    }

    private void editarSeleccionado() {
        if (!tieneAccesoAutorizado()) {
            return;
        }
        int fila = tablaEquipos.getSelectedRow();
        if (fila < 0 || fila >= equiposCargados.size()) {
            JOptionPane.showMessageDialog(this, "Seleccione el equipo a editar.", "Edición", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Equipo e = equiposCargados.get(fila);
        equipoEnEdicion = e.getIdEquipo();
        txtNombre.setText(e.getNombre());
        txtAlias.setText(e.getAlias() == null ? "" : e.getAlias());
        txtColor.setText(e.getColorCamiseta());
        for (int i = 0; i < categoriasCargadas.size(); i++) {
            if (e.getCategoria() != null && categoriasCargadas.get(i).getId() != null
                    && categoriasCargadas.get(i).getId().equals(e.getCategoria().getId())) {
                cmbCategoria.setSelectedIndex(i);
                break;
            }
        }
        for (int i = 0; i < capitanesCargados.size(); i++) {
            if (e.getCapitan() != null && capitanesCargados.get(i).getId() != null
                    && capitanesCargados.get(i).getId().equals(e.getCapitan().getId())) {
                cmbCapitan.setSelectedIndex(i);
                break;
            }
        }
        if (e.getSedeHabitual() == null || e.getSedeHabitual().getIdCancha() == null) {
            cmbSede.setSelectedIndex(0);
        } else {
            int sedeIdx = -1;
            for (int i = 0; i < canchasCargadas.size(); i++) {
                if (e.getSedeHabitual().getIdCancha().equals(canchasCargadas.get(i).getIdCancha())) {
                    sedeIdx = i + 1;
                    break;
                }
            }
            if (sedeIdx < 0) {
                canchasCargadas.add(e.getSedeHabitual());
                cmbSede.addItem(e.getSedeHabitual().getNombre() + " (inactiva)");
                sedeIdx = canchasCargadas.size();
            }
            cmbSede.setSelectedIndex(sedeIdx);
        }
        lblEstado.setText("Editando el equipo #" + equipoEnEdicion + ". Pulse Guardar equipo para confirmar.");
    }

    private void cambiarEstadoEquipo(boolean activo) {
        if (!tieneAccesoAutorizado()) {
            JOptionPane.showMessageDialog(this,
                    "Acceso denegado: requiere sesión autorizada.",
                    "Acceso denegado", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int fila = tablaEquipos.getSelectedRow();
        if (fila < 0 || fila >= equiposCargados.size()) {
            JOptionPane.showMessageDialog(this, "Seleccione un equipo de la tabla.", "Equipo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Equipo e = equiposCargados.get(fila);
        if (e.isActivo() == activo) {
            JOptionPane.showMessageDialog(this,
                    "El equipo \"" + e.getNombre() + "\" ya se encuentra " + (activo ? "ACTIVO." : "INACTIVO."),
                    "Estado del equipo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String pregunta = activo
                ? "¿Confirma reactivar el equipo \"" + e.getNombre() + "\"?"
                : "¿Confirma dar de baja lógica el equipo \"" + e.getNombre()
                + "\"?\nConservará su historial y no podrá programarse en nuevos partidos mientras esté inactivo.";
        int conf = JOptionPane.showConfirmDialog(this, pregunta,
                activo ? "Reactivar equipo" : "Dar de baja equipo", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) {
            return;
        }
        String respuesta = gestorEquipos.cambiarEstadoEquipo(e.getIdEquipo(), activo, operador);
        if (respuesta.startsWith("ERROR")) {
            JOptionPane.showMessageDialog(this, respuesta, "No se pudo cambiar el estado", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, respuesta, "Operación Exitosa", JOptionPane.INFORMATION_MESSAGE);
            if (equipoEnEdicion != null && equipoEnEdicion.equals(e.getIdEquipo())) {
                limpiarFormulario();
            }
            cargarEquipos();
        }
    }

    private void guardar() {
        if (!tieneAccesoAutorizado()) {
            JOptionPane.showMessageDialog(this,
                    "Acceso denegado: requiere sesión autorizada.",
                    "Acceso denegado", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();
        String alias = txtAlias.getText() == null ? "" : txtAlias.getText().trim();
        String color = txtColor.getText() == null ? "" : txtColor.getText().trim();

        if (nombre.isEmpty() || color.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, complete los campos obligatorios (Nombre y Color).",
                    "Campos vacíos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (alias.length() > 12) {
            JOptionPane.showMessageDialog(this, "El alias / sigla no puede superar los 12 caracteres.",
                    "Alias inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int idxCat = cmbCategoria.getSelectedIndex();
        if (idxCat < 0 || idxCat >= categoriasCargadas.size()) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar una categoría deportiva válida.",
                    "Categoría requerida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int idxCap = cmbCapitan.getSelectedIndex();
        if (idxCap < 0 || idxCap >= capitanesCargados.size()) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un capitán con cuenta de usuario registrada.",
                    "Capitán requerido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Cancha sede = null;
        int idxSede = cmbSede.getSelectedIndex();
        if (idxSede > 0 && idxSede - 1 < canchasCargadas.size()) {
            sede = canchasCargadas.get(idxSede - 1);
        }

        Categoria categoriaObj = categoriasCargadas.get(idxCat);
        Usuario capitanObj = capitanesCargados.get(idxCap);

        String respuesta;
        if (equipoEnEdicion == null) {
            respuesta = gestorEquipos.registrarEquipo(nombre, alias, categoriaObj, color, sede, capitanObj, operador);
        } else {
            respuesta = gestorEquipos.actualizarEquipo(equipoEnEdicion, nombre, alias, categoriaObj, color, sede,
                    capitanObj, operador);
        }

        if (respuesta.startsWith("ERROR")) {
            JOptionPane.showMessageDialog(this, respuesta, "No se pudo guardar el equipo", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, respuesta, "Operación Exitosa", JOptionPane.INFORMATION_MESSAGE);
            limpiarFormulario();
            cargarEquipos();
        }
    }
}
