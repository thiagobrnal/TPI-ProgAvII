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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
 * CU-01: alta y edicion de equipos (con categoria, color, sede habitual y capitan responsable).
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
    private final JTextField txtColor = new JTextField(14);
    private final JComboBox<String> cmbCategoria = new JComboBox<>();
    private final JComboBox<String> cmbCapitan = new JComboBox<>();
    private final JComboBox<String> cmbSede = new JComboBox<>();
    private final JLabel lblEstado = new JLabel(" ");

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Equipo", "Categoría", "Color", "Sede habitual", "Capitán"}, 0) {
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
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBackground(Color.WHITE);

        JLabel titulo = new JLabel("Gestión de equipos");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(new Color(0x12, 0x38, 0x5C));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        panelForm.add(titulo, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panelForm.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panelForm.add(txtNombre, gbc);
        gbc.gridx = 3;
        gbc.gridwidth = 1;
        panelForm.add(new JLabel("Color de camiseta:"), gbc);
        gbc.gridx = 4;
        panelForm.add(txtColor, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panelForm.add(new JLabel("Categoría:"), gbc);
        gbc.gridx = 1;
        cmbCategoria.setPreferredSize(new Dimension(220, 24));
        panelForm.add(cmbCategoria, gbc);
        gbc.gridx = 2;
        panelForm.add(new JLabel("Capitán responsable:"), gbc);
        gbc.gridx = 3;
        cmbCapitan.setPreferredSize(new Dimension(230, 24));
        panelForm.add(cmbCapitan, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panelForm.add(new JLabel("Sede habitual:"), gbc);
        gbc.gridx = 1;
        cmbSede.setPreferredSize(new Dimension(220, 24));
        panelForm.add(cmbSede, gbc);
        gbc.gridx = 2;
        gbc.gridwidth = 3;
        lblEstado.setForeground(new Color(0x3A, 0x55, 0x68));
        panelForm.add(lblEstado, gbc);

        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton btnGuardar = new JButton("Guardar equipo");
        btnGuardar.addActionListener(e -> guardar());
        JButton btnNuevo = new JButton("Nuevo (limpiar formulario)");
        btnNuevo.addActionListener(e -> limpiarFormulario());
        JButton btnEditar = new JButton("Editar seleccionado");
        btnEditar.addActionListener(e -> editarSeleccionado());
        JButton btnActualizar = new JButton("Actualizar listado");
        btnActualizar.addActionListener(e -> cargarEquipos());
        JButton btnCancelar = new JButton("Volver al menú");
        btnCancelar.addActionListener(e -> dispose());
        panelAcciones.add(btnGuardar);
        panelAcciones.add(btnNuevo);
        panelAcciones.add(btnEditar);
        panelAcciones.add(btnActualizar);
        panelAcciones.add(btnCancelar);

        JScrollPane scroll = new JScrollPane(tablaEquipos);
        scroll.setPreferredSize(new Dimension(880, 210));

        JPanel panelCentro = new JPanel(new BorderLayout(8, 8));
        panelCentro.setBackground(Color.WHITE);
        panelCentro.add(panelForm, BorderLayout.NORTH);
        panelCentro.add(scroll, BorderLayout.CENTER);

        panel.add(panelCentro, BorderLayout.CENTER);
        panel.add(panelAcciones, BorderLayout.SOUTH);
        setContentPane(panel);
        pack();
        setMinimumSize(new Dimension(920, 560));
    }

    private void cargarCategorias() {
        cmbCategoria.removeAllItems();
        categoriasCargadas.clear();
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
        if (operador != null && operador.getRol() == TiposDatos.Rol.CAPITAN) {
            cmbCapitan.addItem(operador.getNombre() + " " + operador.getApellido()
                    + " (" + operador.getNombreUsuario() + ")");
            capitanesCargados.add(operador);
            cmbCapitan.setEnabled(false);
        } else {
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
        try {
            canchasCargadas = new CanchaDAO().buscarTodas();
            for (Cancha c : canchasCargadas) {
                cmbSede.addItem(c.getNombre() + " (" + c.getDireccion() + ")");
            }
            if (canchasCargadas.isEmpty()) {
                cmbSede.addItem("(sin sedes registradas)");
            }
        } catch (Exception e) {
            cmbSede.addItem("(sin sedes registradas)");
        }
    }

    private void cargarEquipos() {
        equiposCargados = GestorEquipos.getEquiposRegistrados();
        modeloTabla.setRowCount(0);
        for (Equipo e : equiposCargados) {
            modeloTabla.addRow(new Object[]{
                    e.getIdEquipo(),
                    e.getNombre(),
                    e.getCategoria() == null ? "" : e.getCategoria().getNombre(),
                    e.getColorCamiseta(),
                    e.getSedeHabitual() == null ? "(sin sede)" : e.getSedeHabitual().getNombre(),
                    e.getCapitan() == null ? "(sin capitán)" : e.getCapitan().getNombreUsuario()});
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
        txtColor.setText("");
        cmbCategoria.setSelectedIndex(0);
        cmbSede.setSelectedIndex(0);
        if (cmbCapitan.isEnabled()) {
            cmbCapitan.setSelectedIndex(0);
        }
        lblEstado.setText("Alta de equipo nuevo.");
    }

    private void editarSeleccionado() {
        int fila = tablaEquipos.getSelectedRow();
        if (fila < 0 || fila >= equiposCargados.size()) {
            JOptionPane.showMessageDialog(this, "Seleccione el equipo a editar.", "Edición", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Equipo e = equiposCargados.get(fila);
        equipoEnEdicion = e.getIdEquipo();
        txtNombre.setText(e.getNombre());
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
        for (int i = 0; i < canchasCargadas.size(); i++) {
            if (e.getSedeHabitual() != null && e.getSedeHabitual().getIdCancha() != null
                    && e.getSedeHabitual().getIdCancha().equals(canchasCargadas.get(i).getIdCancha())) {
                cmbSede.setSelectedIndex(i);
                break;
            }
        }
        lblEstado.setText("Editando el equipo #" + equipoEnEdicion + ". Pulse Guardar para confirmar los cambios.");
    }

    private void guardar() {
        String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();
        String color = txtColor.getText() == null ? "" : txtColor.getText().trim();

        if (nombre.isEmpty() || color.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, complete todos los campos obligatorios.",
                    "Campos vacíos", JOptionPane.WARNING_MESSAGE);
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
        if (idxSede >= 0 && idxSede < canchasCargadas.size()) {
            sede = canchasCargadas.get(idxSede);
        }

        Categoria categoriaObj = categoriasCargadas.get(idxCat);
        Usuario capitanObj = capitanesCargados.get(idxCap);

        String respuesta;
        if (equipoEnEdicion == null) {
            respuesta = gestorEquipos.registrarEquipo(nombre, categoriaObj, color, sede, capitanObj, operador);
        } else {
            respuesta = gestorEquipos.actualizarEquipo(equipoEnEdicion, nombre, categoriaObj, color, sede,
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
