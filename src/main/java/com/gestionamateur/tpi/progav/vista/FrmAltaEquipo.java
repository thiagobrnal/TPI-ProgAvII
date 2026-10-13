package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.controlador.GestorEquipos;
import com.gestionamateur.tpi.progav.modelo.Cancha;
import com.gestionamateur.tpi.progav.modelo.Categoria;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import javax.swing.JOptionPane;

public class FrmAltaEquipo extends javax.swing.JFrame {

    // Controlador
    private GestorEquipos gestorEquipos;

    public FrmAltaEquipo() {
        initComponents();
        this.setLocationRelativeTo(null); // Centra la ventana
        gestorEquipos = new GestorEquipos();
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
        btnGuardar = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Gestión de Equipos - Alta");

        lblTitulo.setFont(new java.awt.Font("Segoe UI", 1, 18)); 
        lblTitulo.setText("Registrar Nuevo Equipo");

        lblNombre.setText("Nombre del Equipo:");
        lblCategoria.setText("Categoría:");
        lblColor.setText("Color de Camiseta:");

        // Opciones de categorías para el prototipo
        cmbCategoria.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Libre", "Senior", "Femenino", "Sub-20" }));

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

        // --- Layout manual para que se vea ordenado ---
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
                                .addComponent(lblColor))
                            .addGap(18, 18, 18)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(txtNombre)
                                .addComponent(cmbCategoria, 0, 150, Short.MAX_VALUE)
                                .addComponent(txtColor)))))
                .addContainerGap(40, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(lblTitulo)
                .addGap(30, 30, 30)
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 40, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnGuardar)
                    .addComponent(btnCancelar))
                .addGap(30, 30, 30))
        );

        pack();
    }

    // --- LÓGICA DEL BOTÓN GUARDAR (Conexión MVC) ---
    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {
        
        String nombre = txtNombre.getText();
        String catSeleccionada = cmbCategoria.getSelectedItem().toString();
        String color = txtColor.getText();

        // Validaciones básicas de la vista
        if(nombre.trim().isEmpty() || color.trim().isEmpty()){
            JOptionPane.showMessageDialog(this, "Por favor, complete todos los campos.", "Campos vacíos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // 1. Creamos objetos simulados utilizando los constructores blindados que acabamos de programar.
            // Si algún dato simulado es inválido (ej. mail sin arroba), esto lanzará TorneoExcepcion.
            Categoria categoriaObj = new Categoria(catSeleccionada, "Mixto", 18, 99);
            Cancha sedeSimulada = new Cancha("Cancha La Nueva", "Av. San Martín 1500", "Sintético");
            Usuario capitanSimulado = new Usuario("capitan1", "123", "capitan@mail.com", "Juan", "Pérez", TiposDatos.Rol.CAPITAN);

            // 2. Llamamos al Controlador para que valide las reglas de negocio del alta
            String respuesta = gestorEquipos.registrarEquipo(nombre, categoriaObj, color, sedeSimulada, capitanSimulado);

            // 3. Evaluamos la respuesta del Controlador (GestorEquipos)
            if(respuesta.startsWith("ERROR")) {
                JOptionPane.showMessageDialog(this, respuesta, "Error de validación", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, respuesta, "Operación Exitosa", JOptionPane.INFORMATION_MESSAGE);
                // Limpiamos los campos para una nueva carga
                txtNombre.setText("");
                txtColor.setText("");
            }

        } catch (com.gestionamateur.tpi.progav.modelo.TorneoExcepcion ex) {
            // Si nuestros datos simulados rompieron las reglas del Modelo, lo atrapamos acá
            JOptionPane.showMessageDialog(this, "Error en los datos de simulación: " + ex.getMessage(), "Error Crítico", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose(); // Cierra esta ventana y vuelve a donde estaba
    }

    // Declaración de Variables
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JComboBox<String> cmbCategoria;
    private javax.swing.JLabel lblCategoria;
    private javax.swing.JLabel lblColor;
    private javax.swing.JLabel lblNombre;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JTextField txtColor;
    private javax.swing.JTextField txtNombre;
}