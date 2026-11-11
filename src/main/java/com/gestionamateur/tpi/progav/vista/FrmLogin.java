package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.controlador.GestorUsuarios;

public class FrmLogin extends javax.swing.JFrame {

    private GestorUsuarios gestorUsuarios;

    public FrmLogin() {
        initComponents();
        gestorUsuarios = new GestorUsuarios();
        this.setLocationRelativeTo(null); // Centrar en pantalla
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        txtUsuario = new javax.swing.JTextField();
        txtContrasenia = new javax.swing.JPasswordField();
        btnIngresar = new javax.swing.JButton();
        btnCrearCuenta = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Sistema de Torneos - Login");

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18));
        jLabel1.setText("Iniciar Sesión");

        jLabel2.setText("Usuario:");
        jLabel3.setText("Contraseña:");

        btnIngresar.setText("Ingresar");
        btnIngresar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIngresarActionPerformed(evt);
            }
        });

        btnCrearCuenta.setText("Crear Cuenta");
        btnCrearCuenta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCrearCuentaActionPerformed(evt);
            }
        });

        btnAccesoPublico = new javax.swing.JButton();
        btnAccesoPublico.setText("Acceso Público (Invitado)");
        btnAccesoPublico.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAccesoPublicoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(45, 45, 45)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnIngresar)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnCrearCuenta))
                    .addComponent(btnAccesoPublico, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3))
                        .addGap(24, 24, 24)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(txtUsuario)
                                .addComponent(txtContrasenia, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)))))
                .addContainerGap(45, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(jLabel1)
                .addGap(42, 42, 42)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtUsuario, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(27, 27, 27)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(txtContrasenia, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(25, 25, 25)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnIngresar)
                    .addComponent(btnCrearCuenta))
                .addGap(15, 15, 15)
                .addComponent(btnAccesoPublico)
                .addContainerGap(30, Short.MAX_VALUE))
        );

        pack();
    }

    private void btnIngresarActionPerformed(java.awt.event.ActionEvent evt) {
        String usr = txtUsuario.getText();
        String pass = new String(txtContrasenia.getPassword());

        com.gestionamateur.tpi.progav.modelo.Usuario usuarioLogueado = gestorUsuarios.autenticarUsuario(usr, pass);

        if (usuarioLogueado != null) {
            javax.swing.JOptionPane.showMessageDialog(this, "Bienvenido " + usuarioLogueado.getNombre());
            
            FrmPrincipal menu = new FrmPrincipal(usuarioLogueado);
            menu.setVisible(true);
            this.dispose(); 
        } else {
            if (!gestorUsuarios.esSistemaInicializado()) {
                javax.swing.JOptionPane.showMessageDialog(this, 
                    "El sistema no posee usuarios registrados.\nDebe inicializar el Primer Administrador usando 'Crear Cuenta'.", 
                    "Sistema No Inicializado", javax.swing.JOptionPane.INFORMATION_MESSAGE);
            } else {
                javax.swing.JOptionPane.showMessageDialog(this, "Credenciales incorrectas", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void btnCrearCuentaActionPerformed(java.awt.event.ActionEvent evt) {
        FrmRegistroUsuario ventanaRegistro = new FrmRegistroUsuario();
        ventanaRegistro.setVisible(true);
    }

    private void btnAccesoPublicoActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            com.gestionamateur.tpi.progav.modelo.Usuario publico = new com.gestionamateur.tpi.progav.modelo.Usuario(
                    "invitado", "invitado123", "publico@torneo.local", "Público", "Invitado", com.gestionamateur.tpi.progav.modelo.TiposDatos.Rol.PUBLICO);
            FrmPrincipal menu = new FrmPrincipal(publico);
            menu.setVisible(true);
            this.dispose();
        } catch (com.gestionamateur.tpi.progav.modelo.TorneoExcepcion te) {
            javax.swing.JOptionPane.showMessageDialog(this, "Error al iniciar sesión pública: " + te.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private javax.swing.JButton btnAccesoPublico;
    private javax.swing.JButton btnCrearCuenta;
    private javax.swing.JButton btnIngresar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPasswordField txtContrasenia;
    private javax.swing.JTextField txtUsuario;
}
