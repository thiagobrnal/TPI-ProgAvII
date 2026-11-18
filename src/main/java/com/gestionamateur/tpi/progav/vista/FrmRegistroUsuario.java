package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.controlador.GestorUsuarios;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;

public class FrmRegistroUsuario extends javax.swing.JFrame {

    private GestorUsuarios gestorUsuarios;
    private boolean esBootstrap;
    private JPanel panelEncabezadoHost;
    private JLabel lblModoEstado;
    private JLabel lblModoDetalle;

    public FrmRegistroUsuario() {
        initComponents();
        this.gestorUsuarios = new GestorUsuarios();
        this.esBootstrap = !gestorUsuarios.esSistemaInicializado();
        configurarVistaSegunModo();
        this.setLocationRelativeTo(null);
    }

    public FrmRegistroUsuario(boolean forzarBootstrap) {
        initComponents();
        this.gestorUsuarios = new GestorUsuarios();
        this.esBootstrap = forzarBootstrap;
        configurarVistaSegunModo();
        this.setLocationRelativeTo(null);
    }

    private void configurarVistaSegunModo() {
        cmbRol.removeAllItems();
        panelEncabezadoHost.removeAll();
        if (esBootstrap) {
            setTitle("Inicialización del Sistema - Primer Administrador");
            panelEncabezadoHost.add(
                    EstilosUI.crearEncabezado(
                            "Inicialización del sistema",
                            "Primer acceso: registre la cuenta administradora principal del torneo"),
                    BorderLayout.CENTER);
            lblTitulo.setText("Crear Primer Administrador");
            EstilosUI.estilizarIndicadorAcento(lblModoEstado);
            lblModoEstado.setText("Cuenta inicial única — Rol asignado: ADMINISTRADOR");
            lblModoDetalle.setText("No hay usuarios registrados. Complete todos los campos.");
            cmbRol.addItem(TiposDatos.Rol.ADMINISTRADOR.name());
            cmbRol.setEnabled(false);
            btnRegistrar.setText("Inicializar Administrador");
        } else {
            setTitle("Registro de Nuevo Usuario");
            panelEncabezadoHost.add(
                    EstilosUI.crearEncabezado(
                            "Registro de usuario",
                            "Autorregistro público habilitado para capitanes de equipo"),
                    BorderLayout.CENTER);
            lblTitulo.setText("Crear Nueva Cuenta");
            EstilosUI.estilizarTextoPrimario(lblModoEstado);
            lblModoEstado.setText("Perfil asignado: CAPITÁN (autorregistro público)");
            // Cierre estricto: PUBLICO es anónimo sin cuenta; solo se permite autorregistro de CAPITAN
            lblModoDetalle.setText("Otros roles los asigna un Administrador. Campos obligatorios.");
            cmbRol.addItem(TiposDatos.Rol.CAPITAN.name());
            cmbRol.setEnabled(false);
            btnRegistrar.setText("Registrar Cuenta");
        }
        panelEncabezadoHost.revalidate();
        panelEncabezadoHost.repaint();
        pack();
        setMinimumSize(new Dimension(Math.max(getWidth() + 18, 518), Math.min(getHeight(), 380)));
        setSize(Math.max(getWidth(), getMinimumSize().width), getHeight());
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        lblTitulo = new javax.swing.JLabel();
        lblUsuario = new javax.swing.JLabel();
        txtUsuario = new javax.swing.JTextField(24);
        lblContrasenia = new javax.swing.JLabel();
        txtContrasenia = new javax.swing.JPasswordField(24);
        lblMail = new javax.swing.JLabel();
        txtMail = new javax.swing.JTextField(24);
        lblNombre = new javax.swing.JLabel();
        txtNombre = new javax.swing.JTextField(24);
        lblApellido = new javax.swing.JLabel();
        txtApellido = new javax.swing.JTextField(24);
        lblRol = new javax.swing.JLabel();
        cmbRol = new javax.swing.JComboBox<>();
        btnRegistrar = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();
        lblModoEstado = new JLabel(" ");
        lblModoDetalle = new JLabel(" ");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Registro de Nuevo Usuario");

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        EstilosUI.estilizarVentana(panel);

        panelEncabezadoHost = new JPanel(new BorderLayout());
        panelEncabezadoHost.setBackground(EstilosUI.BG_CANVAS);

        // --- Tarjeta de formulario ---
        JPanel panelForm = new JPanel(new GridBagLayout());
        EstilosUI.estilizarTarjeta(panelForm);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;

        lblTitulo.setText("Crear Nueva Cuenta");
        EstilosUI.estilizarTituloSeccion(lblTitulo);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 6, 4, 6);
        panelForm.add(lblTitulo, gbc);

        EstilosUI.estilizarTextoPrimario(lblModoEstado);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 6, 2, 6);
        panelForm.add(lblModoEstado, gbc);

        EstilosUI.estilizarEstadoInfo(lblModoDetalle);
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 6, 8, 6);
        panelForm.add(lblModoDetalle, gbc);

        lblUsuario.setText("Alias de Usuario:");
        lblUsuario.setLabelFor(txtUsuario);
        EstilosUI.estilizarEtiquetaCampo(lblUsuario);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 6, 5, 12);
        panelForm.add(lblUsuario, gbc);

        EstilosUI.estilizarCampoTexto(txtUsuario, "Ej: juan.perez");
        txtUsuario.setMinimumSize(new Dimension(140, txtUsuario.getPreferredSize().height));
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 6, 5, 6);
        panelForm.add(txtUsuario, gbc);

        lblContrasenia.setText("Contraseña:");
        lblContrasenia.setLabelFor(txtContrasenia);
        EstilosUI.estilizarEtiquetaCampo(lblContrasenia);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 6, 5, 12);
        panelForm.add(lblContrasenia, gbc);

        EstilosUI.estilizarCampoPassword(txtContrasenia);
        txtContrasenia.setMinimumSize(new Dimension(140, txtContrasenia.getPreferredSize().height));
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 6, 5, 6);
        panelForm.add(txtContrasenia, gbc);

        lblMail.setText("Correo Electrónico:");
        lblMail.setLabelFor(txtMail);
        EstilosUI.estilizarEtiquetaCampo(lblMail);
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 6, 5, 12);
        panelForm.add(lblMail, gbc);

        EstilosUI.estilizarCampoTexto(txtMail, "Ej: usuario@correo.com");
        txtMail.setMinimumSize(new Dimension(140, txtMail.getPreferredSize().height));
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 6, 5, 6);
        panelForm.add(txtMail, gbc);

        lblNombre.setText("Nombre:");
        lblNombre.setLabelFor(txtNombre);
        EstilosUI.estilizarEtiquetaCampo(lblNombre);
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 6, 5, 12);
        panelForm.add(lblNombre, gbc);

        EstilosUI.estilizarCampoTexto(txtNombre, "Ingrese su nombre");
        txtNombre.setMinimumSize(new Dimension(140, txtNombre.getPreferredSize().height));
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 6, 5, 6);
        panelForm.add(txtNombre, gbc);

        lblApellido.setText("Apellido:");
        lblApellido.setLabelFor(txtApellido);
        EstilosUI.estilizarEtiquetaCampo(lblApellido);
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 6, 5, 12);
        panelForm.add(lblApellido, gbc);

        EstilosUI.estilizarCampoTexto(txtApellido, "Ingrese su apellido");
        txtApellido.setMinimumSize(new Dimension(140, txtApellido.getPreferredSize().height));
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 6, 5, 6);
        panelForm.add(txtApellido, gbc);

        lblRol.setText("Rol en el Sistema:");
        lblRol.setLabelFor(cmbRol);
        EstilosUI.estilizarEtiquetaCampo(lblRol);
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 6, 5, 12);
        panelForm.add(lblRol, gbc);

        EstilosUI.estilizarCombo(cmbRol);
        cmbRol.setMinimumSize(new Dimension(140, cmbRol.getPreferredSize().height));
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 6, 5, 6);
        panelForm.add(cmbRol, gbc);

        // --- Barra inferior de acciones ---
        btnRegistrar.setText("Registrar Cuenta");
        EstilosUI.estilizarBotonPrimario(btnRegistrar);
        btnRegistrar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegistrarActionPerformed(evt);
            }
        });

        btnCancelar.setText("Volver al Login");
        EstilosUI.estilizarBotonNeutro(btnCancelar);
        btnCancelar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarActionPerformed(evt);
            }
        });

        JPanel panelAcciones = EstilosUI.crearBarraAcciones(
                new JComponent[]{btnRegistrar},
                new JComponent[]{btnCancelar});

        JPanel panelCentro = new PanelCentroScrollable();
        panelCentro.setBackground(EstilosUI.BG_CANVAS);

        JPanel panelWrapper = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension base = super.getPreferredSize();
                Container p = getParent();
                if (p != null && p.getWidth() > 0 && p.getWidth() < base.width) {
                    return new Dimension(p.getWidth(), base.height);
                }
                return base;
            }
        };
        panelWrapper.setBackground(EstilosUI.BG_CANVAS);
        panelWrapper.add(panelForm, BorderLayout.CENTER);

        GridBagConstraints gbcCentro = new GridBagConstraints();
        gbcCentro.gridx = 0;
        gbcCentro.gridy = 0;
        gbcCentro.weightx = 1.0;
        gbcCentro.weighty = 1.0;
        gbcCentro.fill = GridBagConstraints.NONE;
        gbcCentro.anchor = GridBagConstraints.CENTER;
        panelCentro.add(panelWrapper, gbcCentro);

        JScrollPane scrollCentro = new JScrollPane(
                panelCentro,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollCentro.setBorder(BorderFactory.createEmptyBorder());
        scrollCentro.setViewportBorder(null);
        scrollCentro.setBackground(EstilosUI.BG_CANVAS);
        scrollCentro.getViewport().setBackground(EstilosUI.BG_CANVAS);
        scrollCentro.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(panelEncabezadoHost, BorderLayout.NORTH);
        panel.add(scrollCentro, BorderLayout.CENTER);
        panel.add(panelAcciones, BorderLayout.SOUTH);
        setContentPane(panel);
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

    private static final class PanelCentroScrollable extends JPanel implements Scrollable {
        PanelCentroScrollable() {
            super(new GridBagLayout());
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
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
