package com.gestionamateur.tpi.progav.vista;

// Importamos el gestor desde la nueva ruta de Thiago
import com.gestionamateur.tpi.progav.controlador.GestorUsuarios;

public class FrmLogin extends javax.swing.JFrame {

    // Referencia al controlador (Aplicación de MVC)
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
        btnCrearCuenta = new javax.swing.JButton(); // Instanciamos el nuevo botón

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

        // --- Layout actualizado para incluir ambos botones ---
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(60, 60, 60)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnCrearCuenta)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 40, Short.MAX_VALUE)
                        .addComponent(btnIngresar))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3))
                        .addGap(25, 25, 25)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(txtUsuario)
                            .addComponent(txtContrasenia, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addComponent(jLabel1)))))
                .addContainerGap(60, Short.MAX_VALUE))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 51, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnIngresar)
                    .addComponent(btnCrearCuenta))
                .addGap(45, 45, 45))
        );

        pack();
    }

    // Tratamiento del Evento: Iniciar Sesión
    private void btnIngresarActionPerformed(java.awt.event.ActionEvent evt) {
        String usr = txtUsuario.getText();
        String pass = new String(txtContrasenia.getPassword());

        // Aca también cambiamos el paquete viejo de modelo por la ruta nueva
        com.gestionamateur.tpi.progav.modelo.Usuario usuarioLogueado = gestorUsuarios.autenticarUsuario(usr, pass);

        if (usuarioLogueado != null) {
            javax.swing.JOptionPane.showMessageDialog(this, "Bienvenido " + usuarioLogueado.getNombre());
            
            FrmPrincipal menu = new FrmPrincipal(usuarioLogueado);
            menu.setVisible(true);
            this.dispose(); 
        } else {
            javax.swing.JOptionPane.showMessageDialog(this, "Credenciales incorrectas", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    // Tratamiento del Evento: Abrir ventana de registro
    private void btnCrearCuentaActionPerformed(java.awt.event.ActionEvent evt) {
        FrmRegistroUsuario ventanaRegistro = new FrmRegistroUsuario();
        ventanaRegistro.setVisible(true);
        // Dejamos el Login abierto de fondo por si el usuario cancela el registro
    }

    // Declaración de componentes
    private javax.swing.JButton btnCrearCuenta;
    private javax.swing.JButton btnIngresar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPasswordField txtContrasenia;
    private javax.swing.JTextField txtUsuario;
}