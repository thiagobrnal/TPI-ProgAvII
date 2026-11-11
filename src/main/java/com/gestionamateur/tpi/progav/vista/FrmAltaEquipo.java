package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.controlador.GestorEquipos;
import com.gestionamateur.tpi.progav.controlador.GestorUsuarios;
import com.gestionamateur.tpi.progav.dao.CategoriaDAO;
import com.gestionamateur.tpi.progav.modelo.Categoria;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

public class FrmAltaEquipo extends javax.swing.JFrame {

    private GestorEquipos gestorEquipos;
    private GestorUsuarios gestorUsuarios;
    private Usuario operador;
    private List<Categoria> categoriasCargadas = new ArrayList<>();
    private List<Usuario> capitanesCargados = new ArrayList<>();

    public FrmAltaEquipo() {
        this(null);
    }

    public FrmAltaEquipo(Usuario operador) {
        this.operador = operador;
        initComponents();
        this.setLocationRelativeTo(null);
        gestorEquipos = new GestorEquipos();
        gestorUsuarios = new GestorUsuarios();
        cargarCategorias();
        cargarCapitanes();
    }

    private void cargarCategorias() {
        cmbCategoria.removeAllItems();
        categoriasCargadas.clear();
        try {
            CategoriaDAO catDAO = new CategoriaDAO();
            categoriasCargadas = catDAO.buscarTodas();
            for (Categoria cat : categoriasCargadas) {
                cmbCategoria.addItem(cat.getNombre() + " (" + cat.getGenero() + ")");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar categorías desde BD: " + e.getMessage(), "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void cargarCapitanes() {
        cmbCapitan.removeAllItems();
        capitanesCargados.clear();
        if (operador != null && operador.getRol() == TiposDatos.Rol.CAPITAN) {
            cmbCapitan.addItem(operador.getNombre() + " " + operador.getApellido() + " (" + operador.getNombreUsuario() + ")");
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

    @SuppressWarnings("unchecked")
    private void initComponents() {

        lblTitulo = new javax.swing.JLabel();
        lblNombre = new javax.swing.JLabel();
        txtNombre = new javax.swing.JTextField();
        lblCategoria = new javax.swing.JLabel();
        cmbCategoria = new javax.swing.JComboBox<>();
        lblColor = new javax.swing.JLabel();
        txtColor = new javax.swing.JTextField();
        lblCapitan = new javax.swing.JLabel();
        cmbCapitan = new javax.swing.JComboBox<>();
        btnGuardar = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Gestión de Equipos - Alta");

        lblTitulo.setFont(new java.awt.Font("Segoe UI", 1, 18)); 
        lblTitulo.setText("Registrar Nuevo Equipo");

        lblNombre.setText("Nombre del Equipo:");
        lblCategoria.setText("Categoría:");
        lblColor.setText("Color de Camiseta:");
        lblCapitan.setText("Capitán Responsable:");

        btnGuardar.setText("Guardar Equipo");
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });

        btnCancelar.setText("Cancelar");
        btnCancelar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblTitulo)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(btnCancelar)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnGuardar))
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(lblNombre)
                                .addComponent(lblCategoria)
                                .addComponent(lblColor)
                                .addComponent(lblCapitan))
                            .addGap(18, 18, 18)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(txtNombre)
                                .addComponent(cmbCategoria, 0, 180, Short.MAX_VALUE)
                                .addComponent(txtColor)
                                .addComponent(cmbCapitan, 0, 180, Short.MAX_VALUE)))))
                .addContainerGap(40, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(lblTitulo)
                .addGap(25, 25, 25)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblNombre)
                    .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblCategoria)
                    .addComponent(cmbCategoria, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblColor)
                    .addComponent(txtColor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblCapitan)
                    .addComponent(cmbCapitan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 35, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnGuardar)
                    .addComponent(btnCancelar))
                .addGap(30, 30, 30))
        );

        pack();
    }

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {
        String nombre = txtNombre.getText().trim();
        String color = txtColor.getText().trim();

        if (nombre.isEmpty() || color.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, complete todos los campos obligatorios.", "Campos vacíos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int idxCat = cmbCategoria.getSelectedIndex();
        if (idxCat < 0 || idxCat >= categoriasCargadas.size()) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar una categoría deportiva válida.", "Categoría requerida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int idxCap = cmbCapitan.getSelectedIndex();
        if (idxCap < 0 || idxCap >= capitanesCargados.size()) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un capitán con cuenta de usuario registrada.", "Capitán requerido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Categoria categoriaObj = categoriasCargadas.get(idxCat);
        Usuario capitanObj = capitanesCargados.get(idxCap);

        String respuesta = gestorEquipos.registrarEquipo(nombre, categoriaObj, color, null, capitanObj, operador);

        if (respuesta.startsWith("ERROR")) {
            JOptionPane.showMessageDialog(this, respuesta, "Error al registrar", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, respuesta, "Operación Exitosa", JOptionPane.INFORMATION_MESSAGE);
            txtNombre.setText("");
            txtColor.setText("");
        }
    }

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose();
    }

    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JComboBox<String> cmbCapitan;
    private javax.swing.JComboBox<String> cmbCategoria;
    private javax.swing.JLabel lblCapitan;
    private javax.swing.JLabel lblCategoria;
    private javax.swing.JLabel lblColor;
    private javax.swing.JLabel lblNombre;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JTextField txtColor;
    private javax.swing.JTextField txtNombre;
}