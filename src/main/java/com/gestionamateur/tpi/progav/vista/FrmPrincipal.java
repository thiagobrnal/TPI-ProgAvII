package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.Usuario;

public class FrmPrincipal extends javax.swing.JFrame {

    private Usuario usuarioActual;

    // Sobrecarga de Constructor 1: Recibe el usuario logueado para manejar permisos
    public FrmPrincipal(Usuario usuario) {
        this.usuarioActual = usuario;
        initComponents();
        this.setLocationRelativeTo(null);

        // Modificación dinámica de la GUI según el Rol
        if (usuario != null && usuario.getRol() != null) {
            lblBienvenida.setText("Usuario: " + usuario.getNombre() + " | Rol: " + usuario.getRol().toString());

            // Lógica de Permisos: Encapsular funcionalidad según rol
            if (usuario.getRol() == TiposDatos.Rol.PUBLICO) {
                btnGestionarEquipos.setEnabled(false);
                btnAltaJugadores.setEnabled(false);
                btnGestionarCanchas.setEnabled(false);
                btnProgramarPartidos.setEnabled(false);
            } else if (usuario.getRol() == TiposDatos.Rol.ARBITRO) {
                btnGestionarEquipos.setEnabled(false);
                btnAltaJugadores.setEnabled(false);
                btnGestionarCanchas.setEnabled(false);
                btnProgramarPartidos.setEnabled(false);
            } else if (usuario.getRol() == TiposDatos.Rol.CAPITAN) {
                btnProgramarPartidos.setEnabled(false);
                btnGestionarCanchas.setEnabled(false);
            }
        } else {
            lblBienvenida.setText("Usuario: Invitado (Acceso Público de Solo Lectura)");
            btnGestionarEquipos.setEnabled(false);
            btnAltaJugadores.setEnabled(false);
            btnGestionarCanchas.setEnabled(false);
            btnProgramarPartidos.setEnabled(false);
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
        btnGestionarCanchas = new javax.swing.JButton();
        btnProgramarPartidos = new javax.swing.JButton();
        btnCerrarSesion = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Menú Principal - Torneo Fútbol");

        lblTitulo.setFont(new java.awt.Font("Segoe UI", 1, 24));
        lblTitulo.setText("Panel de Control");

        lblBienvenida.setText("Usuario: Invitado");

        btnGestionarEquipos.setText("Gestionar Equipos (Alta)");
        btnGestionarEquipos.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGestionarEquiposActionPerformed(evt);
            }
        });

        btnAltaJugadores.setText("Dar de Alta Jugadores");
        btnAltaJugadores.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAltaJugadoresActionPerformed(evt);
            }
        });

        btnGestionarCanchas.setText("Gestionar Sedes / Canchas");
        btnGestionarCanchas.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGestionarCanchasActionPerformed(evt);
            }
        });

        btnProgramarPartidos.setText("Programar Partidos");
        btnProgramarPartidos.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnProgramarPartidosActionPerformed(evt);
            }
        });

        btnCerrarSesion.setText("Cerrar Sesión");
        btnCerrarSesion.addActionListener(new java.awt.event.ActionListener() {
            @Override
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
                    .addComponent(btnGestionarCanchas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
                .addComponent(btnGestionarCanchas, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnProgramarPartidos, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(40, Short.MAX_VALUE))
        );

        pack();
    }

    private void btnGestionarEquiposActionPerformed(java.awt.event.ActionEvent evt) {
        FrmAltaEquipo ventanaEquipos = new FrmAltaEquipo(usuarioActual);
        ventanaEquipos.setVisible(true);
    }

    private void btnAltaJugadoresActionPerformed(java.awt.event.ActionEvent evt) {
        FrmAltaJugador ventanaJugadores = new FrmAltaJugador(usuarioActual);
        ventanaJugadores.setVisible(true);
    }

    private void btnGestionarCanchasActionPerformed(java.awt.event.ActionEvent evt) {
        FrmAltaCancha ventanaCanchas = new FrmAltaCancha(usuarioActual);
        ventanaCanchas.setVisible(true);
    }

    private void btnProgramarPartidosActionPerformed(java.awt.event.ActionEvent evt) {
        FrmProgramarPartido ventanaPartidos = new FrmProgramarPartido(usuarioActual);
        ventanaPartidos.setVisible(true);
    }

    private void btnCerrarSesionActionPerformed(java.awt.event.ActionEvent evt) {
        FrmLogin login = new FrmLogin();
        login.setVisible(true);
        this.dispose();
    }

    private javax.swing.JButton btnAltaJugadores;
    private javax.swing.JButton btnCerrarSesion;
    private javax.swing.JButton btnGestionarEquipos;
    private javax.swing.JButton btnGestionarCanchas;
    private javax.swing.JButton btnProgramarPartidos;
    private javax.swing.JLabel lblBienvenida;
    private javax.swing.JLabel lblTitulo;
}