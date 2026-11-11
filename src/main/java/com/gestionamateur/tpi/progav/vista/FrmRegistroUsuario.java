package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.controlador.GestorUsuarios;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import javax.swing.JOptionPane;

public class FrmRegistroUsuario extends javax.swing.JFrame {

    private GestorUsuarios gestorUsuarios;
    private boolean esBootstrap;

    public FrmRegistroUsuario() {
        initComponents();
        this.setLocationRelativeTo(null);
        this.gestorUsuarios = new GestorUsuarios();
        this.esBootstrap = !gestorUsuarios.esSistemaInicializado();
        configurarVistaSegunModo();
    }

    public FrmRegistroUsuario(boolean forzarBootstrap) {
        initComponents();
        this.setLocationRelativeTo(null);
        this.gestorUsuarios = new GestorUsuarios();
        this.esBootstrap = forzarBootstrap;
        configurarVistaSegunModo();
    }

    private void configurarVistaSegunModo() {
        cmbRol.removeAllItems();
        if (esBootstrap) {
            setTitle("Inicialización del Sistema - Primer Administrador");
            lblTitulo.setText("Crear Primer Administrador");
            cmbRol.addItem(TiposDatos.Rol.ADMINISTRADOR.name());
            cmbRol.setEnabled(false);
            btnRegistrar.setText("Inicializar Administrador");
        } else {
            setTitle("Registro de Nuevo Usuario");
            // Cierre estricto: PUBLICO es anónimo sin cuenta; solo se permite autorregistro de CAPITAN
            cmbRol.addItem(TiposDatos.Rol.CAPITAN.name());
            cmbRol.setEnabled(false);
            btnRegistrar.setText("Registrar Cuenta");
        }
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        lblTitulo = new javax.swing.JLabel();
        lblUsuario = new javax.swing.JLabel();
        txtUsuario = new javax.swing.JTextField();
        lblContrasenia = new javax.swing.JLabel();
        txtContrasenia = new javax.swing.JPasswordField();
        lblMail = new javax.swing.JLabel();
        txtMail = new javax.swing.JTextField();
        lblNombre = new javax.swing.JLabel();
        txtNombre = new javax.swing.JTextField();
        lblApellido = new javax.swing.JLabel();
        txtApellido = new javax.swing.JTextField();
        lblRol = new javax.swing.JLabel();
        cmbRol = new javax.swing.JComboBox<>();
        btnRegistrar = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Registro de Nuevo Usuario");

        lblTitulo.setFont(new java.awt.Font("Segoe UI", 1, 18)); 
        lblTitulo.setText("Crear Nueva Cuenta");

        lblUsuario.setText("Alias de Usuario:");
        lblContrasenia.setText("Contraseña:");
        lblMail.setText("Correo Electrónico:");
        lblNombre.setText("Nombre:");
        lblApellido.setText("Apellido:");
        lblRol.setText("Rol en el Sistema:");

        btnRegistrar.setText("Registrar Cuenta");
        btnRegistrar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegistrarActionPerformed(evt);
            }
        });

        btnCancelar.setText("Volver al Login");
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
                            .addComponent(btnRegistrar))
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(lblUsuario)
                                .addComponent(lblContrasenia)
                                .addComponent(lblMail)
                                .addComponent(lblNombre)
                                .addComponent(lblApellido)
                                .addComponent(lblRol))
                            .addGap(25, 25, 25)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(txtUsuario)
                                .addComponent(txtContrasenia)
                                .addComponent(txtMail)
                                .addComponent(txtNombre)
                                .addComponent(txtApellido)
                                .addComponent(cmbRol, 0, 180, Short.MAX_VALUE)))))
                .addContainerGap(40, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(lblTitulo)
                .addGap(30, 30, 30)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblUsuario)
                    .addComponent(txtUsuario, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblContrasenia)
                    .addComponent(txtContrasenia, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblMail)
                    .addComponent(txtMail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblNombre)
                    .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblApellido)
                    .addComponent(txtApellido, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblRol)
                    .addComponent(cmbRol, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 35, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnRegistrar)
                    .addComponent(btnCancelar))
                .addGap(30, 30, 30))
        );

        pack();
    }

    private void btnRegistrarActionPerformed(java.awt.event.ActionEvent evt) {
        String usuario = txtUsuario.getText();
        String pass = new String(txtContrasenia.getPassword());
        String mail = txtMail.getText();
        String nombre = txtNombre.getText();
        String apellido = txtApellido.getText();

        if (usuario.trim().isEmpty() || pass.trim().isEmpty() || mail.trim().isEmpty()
                || nombre.trim().isEmpty() || apellido.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Complete todos los campos obligatorios.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String respuesta;
        if (esBootstrap) {
            respuesta = gestorUsuarios.inicializarPrimerAdministrador(usuario, pass, mail, nombre, apellido);
        } else {
            String rolStr = (String) cmbRol.getSelectedItem();
            TiposDatos.Rol rolSeleccionado;
            try {
                rolSeleccionado = TiposDatos.Rol.valueOf(rolStr);
            } catch (Exception e) {
                rolSeleccionado = TiposDatos.Rol.CAPITAN;
            }
            respuesta = gestorUsuarios.registrarUsuarioPublico(usuario, pass, mail, nombre, apellido, rolSeleccionado);
        }

        if (respuesta.startsWith("ERROR")) {
            JOptionPane.showMessageDialog(this, respuesta, "Error al registrar", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, respuesta, "Operación Exitosa", JOptionPane.INFORMATION_MESSAGE);
            this.dispose();
        }
    }

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose();
    }

    public boolean isModoBootstrap() {
        return esBootstrap;
    }

    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnRegistrar;
    private javax.swing.JComboBox<String> cmbRol;
    private javax.swing.JLabel lblApellido;
    private javax.swing.JLabel lblContrasenia;
    private javax.swing.JLabel lblMail;
    private javax.swing.JLabel lblNombre;
    private javax.swing.JLabel lblRol;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JLabel lblUsuario;
    private javax.swing.JTextField txtApellido;
    private javax.swing.JPasswordField txtContrasenia;
    private javax.swing.JTextField txtMail;
    private javax.swing.JTextField txtNombre;
    private javax.swing.JTextField txtUsuario;
}
