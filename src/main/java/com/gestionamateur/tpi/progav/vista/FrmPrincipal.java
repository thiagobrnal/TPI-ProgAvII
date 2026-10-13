package com.gestionamateur.tpi.progav.vista;

// Agregamos las importaciones con la nueva ruta de paquetes
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;

public class FrmPrincipal extends javax.swing.JFrame {

    private Usuario usuarioActual;

    // Sobrecarga de Constructor 1: Recibe el usuario logueado para manejar permisos
    public FrmPrincipal(Usuario usuario) {
        this.usuarioActual = usuario;
        initComponents();
        this.setLocationRelativeTo(null);
        
        // Modificación dinámica de la GUI aprovechando el Dominio Rico
        lblBienvenida.setText("Usuario: " + usuario.getNombre() + " | Rol: " + usuario.getRol().toString());
        
        // Lógica de Permisos: Encapsular funcionalidad según rol
        if(usuario.getRol() == TiposDatos.Rol.ARBITRO) {
            btnGestionarEquipos.setEnabled(false); // Un árbitro no da de alta equipos
            btnAltaJugadores.setEnabled(false);    // Tampoco da de alta jugadores
        } else if (usuario.getRol() == TiposDatos.Rol.CAPITAN) {
            btnProgramarPartidos.setEnabled(false); // Un capitán no puede programar el fixture
        }
    }

    // Sobrecarga de Constructor 2: Vacío por defecto
    public FrmPrincipal() {
        initComponents();
        this.setLocationRelativeTo(null);
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        lblTitulo = new javax.swing.JLabel();
        lblBienvenida = new javax.swing.JLabel();
        btnGestionarEquipos = new javax.swing.JButton();
        btnAltaJugadores = new javax.swing.JButton();
        btnProgramarPartidos = new javax.swing.JButton();
        btnCerrarSesion = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Menú Principal - Torneo Fútbol");

        lblTitulo.setFont(new java.awt.Font("Segoe UI", 1, 24)); 
        lblTitulo.setText("Panel de Control");

        lblBienvenida.setText("Usuario: Invitado");

        btnGestionarEquipos.setText("Gestionar Equipos (Alta)");
        btnGestionarEquipos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGestionarEquiposActionPerformed(evt);
            }
        });

        btnAltaJugadores.setText("Dar de Alta Jugadores");
        btnAltaJugadores.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAltaJugadoresActionPerformed(evt);
            }
        });

        btnProgramarPartidos.setText("Programar Partidos");
        btnProgramarPartidos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnProgramarPartidosActionPerformed(evt);
            }
        });

        btnCerrarSesion.setText("Cerrar Sesión");
        btnCerrarSesion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCerrarSesionActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblBienvenida)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnCerrarSesion)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(142, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblTitulo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnGestionarEquipos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnAltaJugadores, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnProgramarPartidos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(133, 133, 133))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblBienvenida)
                    .addComponent(btnCerrarSesion))
                .addGap(30, 30, 30)
                .addComponent(lblTitulo)
                .addGap(42, 42, 42)
                .addComponent(btnGestionarEquipos, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnAltaJugadores, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnProgramarPartidos, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(78, Short.MAX_VALUE))
        );

        pack();
    }

    private void btnGestionarEquiposActionPerformed(java.awt.event.ActionEvent evt) {
        // Interacción entre ventanas: Llamada a la interfaz ya construida
        FrmAltaEquipo ventanaEquipos = new FrmAltaEquipo();
        ventanaEquipos.setVisible(true);
        this.dispose();
    }

    private void btnAltaJugadoresActionPerformed(java.awt.event.ActionEvent evt) {
        javax.swing.JOptionPane.showMessageDialog(this, "Módulo en construcción para el próximo sprint.");
    }

    private void btnProgramarPartidosActionPerformed(java.awt.event.ActionEvent evt) {
        javax.swing.JOptionPane.showMessageDialog(this, "Módulo en construcción para el próximo sprint.");
    }

    private void btnCerrarSesionActionPerformed(java.awt.event.ActionEvent evt) {
        FrmLogin login = new FrmLogin();
        login.setVisible(true);
        this.dispose(); // Libera los recursos asociados a la ventana actual (Dispose_On_Close)
    }

    private javax.swing.JButton btnAltaJugadores;
    private javax.swing.JButton btnCerrarSesion;
    private javax.swing.JButton btnGestionarEquipos;
    private javax.swing.JButton btnProgramarPartidos;
    private javax.swing.JLabel lblBienvenida;
    private javax.swing.JLabel lblTitulo;
}