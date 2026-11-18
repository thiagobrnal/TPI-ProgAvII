package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.controlador.GestorUsuarios;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.seguridad.PoliticaPermisos;
import com.gestionamateur.tpi.progav.seguridad.PoliticaPermisos.ModuloMenu;
import com.gestionamateur.tpi.progav.servicio.AuthService;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;

public class FrmPrincipal extends javax.swing.JFrame {

    private Usuario usuarioActual;
    private final AuthService authService;

    // Sobrecarga de Constructor 1: Recibe el usuario logueado para manejar permisos
    public FrmPrincipal(Usuario usuario) {
        this(usuario, null);
    }

    public FrmPrincipal(Usuario usuario, AuthService authService) {
        this.usuarioActual = usuario;
        this.authService = authService;
        initComponents();
        this.setLocationRelativeTo(null);
        aplicarPermisosYEstilos();
    }

    // Sobrecarga de Constructor 2: Vacío por defecto (fail-closed como Invitado)
    public FrmPrincipal() {
        this(null, null);
    }

    private void aplicarPermisosYEstilos() {
        if (PoliticaPermisos.esSesionAutenticada(usuarioActual)) {
            EstilosUI.estilizarIndicadorAcento(lblBienvenida);
            lblBienvenida.setText("Usuario: " + usuarioActual.getNombre() + " | Rol: " + usuarioActual.getRol().toString());
        } else {
            EstilosUI.estilizarEstadoInfo(lblBienvenida);
            lblBienvenida.setText("Usuario: Invitado (Acceso Público de Solo Lectura)");
        }

        configurarBotonModulo(btnGestionarEquipos, ModuloMenu.GESTIONAR_EQUIPOS);
        configurarBotonModulo(btnAltaJugadores, ModuloMenu.ALTA_JUGADORES);
        configurarBotonModulo(btnGestionarCanchas, ModuloMenu.GESTIONAR_CANCHAS);
        configurarBotonModulo(btnCredenciales, ModuloMenu.IMPRIMIR_CREDENCIALES);
        configurarBotonModulo(btnTorneos, ModuloMenu.GESTIONAR_TORNEOS);
        configurarBotonModulo(btnProgramarPartidos, ModuloMenu.PROGRAMAR_PARTIDOS);
        configurarBotonModulo(btnCargarActas, ModuloMenu.CARGAR_ACTAS);
        configurarBotonModulo(btnSuspenderPartidos, ModuloMenu.POSTERGAR_SUSPENDER_PARTIDOS);
        configurarBotonModulo(btnTribunal, ModuloMenu.TRIBUNAL_DISCIPLINARIO);
        configurarBotonModulo(btnReclamos, ModuloMenu.RESOLVER_RECLAMOS);
        configurarBotonModulo(btnReportes, ModuloMenu.REPORTES);
        configurarBotonModulo(btnConsultaPublica, ModuloMenu.CONSULTA_PUBLICA);
        configurarBotonModulo(btnGestorUsuarios, ModuloMenu.GESTIONAR_USUARIOS);
        configurarBotonModulo(btnConfiguracion, ModuloMenu.CONFIGURACION_GENERAL);

        boolean puedeCerrar = PoliticaPermisos.puedeCerrarSesion(usuarioActual);
        btnCerrarSesion.setEnabled(puedeCerrar);
        if (puedeCerrar) {
            EstilosUI.estilizarBotonPeligro(btnCerrarSesion);
            btnCerrarSesion.setToolTipText("Finaliza la sesión activa y regresa al inicio de sesión.");
        } else {
            EstilosUI.estilizarBotonDenegado(btnCerrarSesion);
            btnCerrarSesion.setToolTipText("No hay una sesión autenticada activa.");
        }
    }

    private void configurarBotonModulo(JButton boton, ModuloMenu modulo) {
        boton.setText(modulo.getEtiquetaMenu());
        boolean permitido = PoliticaPermisos.tienePermiso(usuarioActual, modulo);
        boton.setEnabled(permitido);
        EstilosUI.estilizarBotonModulo(boton, permitido);
        boton.setToolTipText(PoliticaPermisos.obtenerTooltip(usuarioActual, modulo));
    }

    private void abrirModulo(ModuloMenu modulo, Supplier<JFrame> constructorVentana) {
        if (!PoliticaPermisos.tienePermiso(usuarioActual, modulo)) {
            JOptionPane.showMessageDialog(this,
                    "Acceso denegado: " + PoliticaPermisos.obtenerTooltip(usuarioActual, modulo),
                    "Permisos insuficientes", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFrame ventana = constructorVentana.get();
        ventana.setVisible(true);
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        lblTitulo = new javax.swing.JLabel();
        lblBienvenida = new javax.swing.JLabel();
        btnGestionarEquipos = new javax.swing.JButton();
        btnAltaJugadores = new javax.swing.JButton();
        btnGestionarCanchas = new javax.swing.JButton();
        btnProgramarPartidos = new javax.swing.JButton();
        btnCargarActas = new javax.swing.JButton();
        btnSuspenderPartidos = new javax.swing.JButton();
        btnTribunal = new javax.swing.JButton();
        btnReclamos = new javax.swing.JButton();
        btnCredenciales = new javax.swing.JButton();
        btnTorneos = new javax.swing.JButton();
        btnGestorUsuarios = new javax.swing.JButton();
        btnConfiguracion = new javax.swing.JButton();
        btnReportes = new javax.swing.JButton();
        btnConsultaPublica = new javax.swing.JButton();
        btnCerrarSesion = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Menú Principal - Torneo Fútbol");

        JPanel panel = new JPanel(new BorderLayout(12, 6));
        EstilosUI.estilizarVentana(panel);

        // --- Encabezado institucional y barra de sesión ---
        JPanel panelSuperior = new JPanel(new BorderLayout(0, 6));
        panelSuperior.setBackground(EstilosUI.BG_CANVAS);

        JPanel panelEncabezado = EstilosUI.crearEncabezado(
                "Menú Principal — Torneo Fútbol",
                "Centro de navegación deportiva y administrativa");
        panelSuperior.add(panelEncabezado, BorderLayout.NORTH);

        JPanel panelSesion = new JPanel(new GridBagLayout());
        EstilosUI.estilizarTarjeta(panelSesion);

        GridBagConstraints gbcSes = new GridBagConstraints();
        gbcSes.insets = new Insets(0, 2, 2, 8);
        gbcSes.anchor = GridBagConstraints.WEST;
        gbcSes.gridx = 0;
        gbcSes.gridy = 0;
        gbcSes.weightx = 1.0;
        gbcSes.fill = GridBagConstraints.HORIZONTAL;

        lblTitulo.setText("Panel de Control");
        EstilosUI.estilizarTituloSeccion(lblTitulo);
        panelSesion.add(lblTitulo, gbcSes);

        btnCerrarSesion.setText("Cerrar Sesión");
        EstilosUI.estilizarBotonPeligro(btnCerrarSesion);
        btnCerrarSesion.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCerrarSesionActionPerformed(evt);
            }
        });
        gbcSes.gridx = 1;
        gbcSes.gridy = 0;
        gbcSes.weightx = 0.0;
        gbcSes.fill = GridBagConstraints.NONE;
        gbcSes.anchor = GridBagConstraints.EAST;
        gbcSes.insets = new Insets(0, 8, 2, 2);
        panelSesion.add(btnCerrarSesion, gbcSes);

        lblBienvenida.setText("Usuario: Invitado");
        EstilosUI.estilizarEstadoInfo(lblBienvenida);
        gbcSes.gridx = 0;
        gbcSes.gridy = 1;
        gbcSes.gridwidth = 2;
        gbcSes.weightx = 1.0;
        gbcSes.fill = GridBagConstraints.HORIZONTAL;
        gbcSes.anchor = GridBagConstraints.WEST;
        gbcSes.insets = new Insets(0, 2, 0, 2);
        panelSesion.add(lblBienvenida, gbcSes);

        panelSuperior.add(panelSesion, BorderLayout.CENTER);

        // --- Configuración funcional y visual de botones ---
        btnGestionarEquipos.setText(ModuloMenu.GESTIONAR_EQUIPOS.getEtiquetaMenu());
        btnGestionarEquipos.addActionListener(evt -> abrirModulo(ModuloMenu.GESTIONAR_EQUIPOS, () -> new FrmAltaEquipo(usuarioActual)));

        btnAltaJugadores.setText(ModuloMenu.ALTA_JUGADORES.getEtiquetaMenu());
        btnAltaJugadores.addActionListener(evt -> abrirModulo(ModuloMenu.ALTA_JUGADORES, () -> new FrmAltaJugador(usuarioActual)));

        btnGestionarCanchas.setText(ModuloMenu.GESTIONAR_CANCHAS.getEtiquetaMenu());
        btnGestionarCanchas.addActionListener(evt -> abrirModulo(ModuloMenu.GESTIONAR_CANCHAS, () -> new FrmAltaCancha(usuarioActual)));

        btnCredenciales.setText(ModuloMenu.IMPRIMIR_CREDENCIALES.getEtiquetaMenu());
        btnCredenciales.addActionListener(evt -> abrirModulo(ModuloMenu.IMPRIMIR_CREDENCIALES, () -> new FrmCredencialJugador(usuarioActual)));

        btnTorneos.setText(ModuloMenu.GESTIONAR_TORNEOS.getEtiquetaMenu());
        btnTorneos.addActionListener(evt -> abrirModulo(ModuloMenu.GESTIONAR_TORNEOS, () -> new FrmTorneos(usuarioActual)));

        btnProgramarPartidos.setText(ModuloMenu.PROGRAMAR_PARTIDOS.getEtiquetaMenu());
        btnProgramarPartidos.addActionListener(evt -> abrirModulo(ModuloMenu.PROGRAMAR_PARTIDOS, () -> new FrmProgramarPartido(usuarioActual)));

        btnCargarActas.setText(ModuloMenu.CARGAR_ACTAS.getEtiquetaMenu());
        btnCargarActas.addActionListener(evt -> abrirModulo(ModuloMenu.CARGAR_ACTAS, () -> new FrmCargarResultado(usuarioActual)));

        btnSuspenderPartidos.setText(ModuloMenu.POSTERGAR_SUSPENDER_PARTIDOS.getEtiquetaMenu());
        btnSuspenderPartidos.addActionListener(evt -> abrirModulo(ModuloMenu.POSTERGAR_SUSPENDER_PARTIDOS, () -> new FrmSuspenderPartido(usuarioActual)));

        btnTribunal.setText(ModuloMenu.TRIBUNAL_DISCIPLINARIO.getEtiquetaMenu());
        btnTribunal.addActionListener(evt -> abrirModulo(ModuloMenu.TRIBUNAL_DISCIPLINARIO, () -> new FrmTribunal(usuarioActual)));

        btnReclamos.setText(ModuloMenu.RESOLVER_RECLAMOS.getEtiquetaMenu());
        btnReclamos.addActionListener(evt -> abrirModulo(ModuloMenu.RESOLVER_RECLAMOS, () -> new FrmReclamos(usuarioActual)));

        btnReportes.setText(ModuloMenu.REPORTES.getEtiquetaMenu());
        btnReportes.addActionListener(evt -> abrirModulo(ModuloMenu.REPORTES, () -> new FrmReportes(usuarioActual)));

        btnConsultaPublica.setText(ModuloMenu.CONSULTA_PUBLICA.getEtiquetaMenu());
        btnConsultaPublica.addActionListener(evt -> abrirModulo(ModuloMenu.CONSULTA_PUBLICA, () -> new FrmConsultaPublica(usuarioActual)));

        btnGestorUsuarios.setText(ModuloMenu.GESTIONAR_USUARIOS.getEtiquetaMenu());
        btnGestorUsuarios.addActionListener(evt -> abrirModulo(ModuloMenu.GESTIONAR_USUARIOS, () -> new FrmGestorUsuarios(usuarioActual)));

        btnConfiguracion.setText(ModuloMenu.CONFIGURACION_GENERAL.getEtiquetaMenu());
        btnConfiguracion.addActionListener(evt -> abrirModulo(ModuloMenu.CONFIGURACION_GENERAL, () -> new FrmConfiguracion(usuarioActual)));

        // --- Tarjetas de navegación agrupadas por afinidad funcional ---
        JPanel tarjetaEquiposYSedes = crearTarjetaModulo(
                "Equipos, Jugadores y Canchas",
                "Gestión de equipos, altas, canchas y carnets",
                new JButton[]{btnGestionarEquipos, btnAltaJugadores, btnGestionarCanchas, btnCredenciales});

        JPanel tarjetaCompetencia = crearTarjetaModulo(
                "Competencia y Disciplina",
                "Torneos, fixture, actas y tribunal",
                new JButton[]{
                    btnTorneos, btnProgramarPartidos, btnCargarActas,
                    btnSuspenderPartidos, btnTribunal, btnReclamos
                });

        JPanel tarjetaConsultas = crearTarjetaModulo(
                "Reportes y Consulta Pública",
                "Informes oficiales y vista pública",
                new JButton[]{btnReportes, btnConsultaPublica});

        JPanel tarjetaAdministracion = crearTarjetaModulo(
                "Administración del Sistema",
                "Cuentas, roles y parámetros globales",
                new JButton[]{btnGestorUsuarios, btnConfiguracion});

        JPanel panelCentro = new PanelCentroScrollable();
        panelCentro.setBackground(EstilosUI.BG_CANVAS);
        panelCentro.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));

        GridBagConstraints gbcCentro = new GridBagConstraints();
        gbcCentro.weightx = 0.5;
        gbcCentro.fill = GridBagConstraints.BOTH;
        gbcCentro.anchor = GridBagConstraints.NORTH;

        gbcCentro.gridx = 0;
        gbcCentro.gridy = 0;
        gbcCentro.weighty = 0.0;
        gbcCentro.insets = new Insets(0, 0, 6, 6);
        panelCentro.add(tarjetaEquiposYSedes, gbcCentro);

        gbcCentro.gridx = 1;
        gbcCentro.insets = new Insets(0, 6, 6, 0);
        panelCentro.add(tarjetaCompetencia, gbcCentro);

        gbcCentro.gridx = 0;
        gbcCentro.gridy = 1;
        gbcCentro.weighty = 0.0;
        gbcCentro.insets = new Insets(0, 0, 0, 6);
        panelCentro.add(tarjetaConsultas, gbcCentro);

        gbcCentro.gridx = 1;
        gbcCentro.insets = new Insets(0, 6, 0, 0);
        panelCentro.add(tarjetaAdministracion, gbcCentro);

        JScrollPane scrollCentro = new JScrollPane(
                panelCentro,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollCentro.setBorder(BorderFactory.createEmptyBorder());
        scrollCentro.setViewportBorder(null);
        scrollCentro.setBackground(EstilosUI.BG_CANVAS);
        scrollCentro.getViewport().setBackground(EstilosUI.BG_CANVAS);
        scrollCentro.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(panelSuperior, BorderLayout.NORTH);
        panel.add(scrollCentro, BorderLayout.CENTER);
        setContentPane(panel);

        pack();
        setMinimumSize(new Dimension(640, 480));
        setSize(new Dimension(Math.max(getWidth(), 960), Math.max(getHeight(), 650)));
    }

    private JPanel crearTarjetaModulo(String titulo, String descripcion, JButton[] botones) {
        JPanel tarjeta = new JPanel(new GridBagLayout());
        EstilosUI.estilizarTarjetaConTitulo(tarjeta, titulo);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 2, 4, 2);

        JLabel lblDesc = new JLabel(descripcion);
        EstilosUI.estilizarEstadoInfo(lblDesc);
        tarjeta.add(lblDesc, gbc);

        for (int i = 0; i < botones.length; i++) {
            JButton btn = botones[i];
            Dimension pref = btn.getPreferredSize();
            btn.setPreferredSize(new Dimension(pref.width, Math.max(pref.height, 30)));
            gbc.gridy = i + 1;
            gbc.insets = new Insets(0, 2, (i == botones.length - 1) ? 0 : 3, 2);
            tarjeta.add(btn, gbc);
        }

        // Espaciador flexible para alinear tarjetas de distinta altura dentro de la misma fila
        gbc.gridy = botones.length + 1;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);
        JPanel relleno = new JPanel();
        relleno.setOpaque(false);
        tarjeta.add(relleno, gbc);

        return tarjeta;
    }

    private void btnCerrarSesionActionPerformed(java.awt.event.ActionEvent evt) {
        if (!PoliticaPermisos.puedeCerrarSesion(usuarioActual)) {
            return;
        }
        if (authService != null) {
            authService.cerrarSesion(usuarioActual);
        } else {
            new GestorUsuarios().cerrarSesion(usuarioActual);
        }
        this.usuarioActual = null;
        aplicarPermisosYEstilos();
        FrmLogin login = new FrmLogin();
        login.setVisible(true);
        this.dispose();
    }

    private static final class PanelCentroScrollable extends JPanel implements Scrollable {
        PanelCentroScrollable() {
            super(new GridBagLayout());
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 48;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport vp && vp.getHeight() > getPreferredSize().height;
        }
    }

    private javax.swing.JButton btnAltaJugadores;
    private javax.swing.JButton btnCargarActas;
    private javax.swing.JButton btnCerrarSesion;
    private javax.swing.JButton btnConfiguracion;
    private javax.swing.JButton btnConsultaPublica;
    private javax.swing.JButton btnCredenciales;
    private javax.swing.JButton btnGestorUsuarios;
    private javax.swing.JButton btnReclamos;
    private javax.swing.JButton btnReportes;
    private javax.swing.JButton btnSuspenderPartidos;
    private javax.swing.JButton btnTribunal;
    private javax.swing.JButton btnTorneos;
    private javax.swing.JButton btnGestionarEquipos;
    private javax.swing.JButton btnGestionarCanchas;
    private javax.swing.JButton btnProgramarPartidos;
    private javax.swing.JLabel lblBienvenida;
    private javax.swing.JLabel lblTitulo;
}
