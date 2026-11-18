package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.controlador.GestorUsuarios;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;

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
        txtUsuario = new javax.swing.JTextField(22);
        txtContrasenia = new javax.swing.JPasswordField(22);
        btnIngresar = new javax.swing.JButton();
        btnCrearCuenta = new javax.swing.JButton();
        btnAccesoPublico = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Sistema de Torneos - Login");

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        EstilosUI.estilizarVentana(panel);

        JPanel panelEncabezado = EstilosUI.crearEncabezado(
                "Sistema de Torneos",
                "Gestión deportiva amateur — Control de acceso");

        // --- Tarjeta de credenciales ---
        JPanel panelCredenciales = new JPanel(new GridBagLayout());
        EstilosUI.estilizarTarjeta(panelCredenciales);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 4, 5, 8);
        gbc.anchor = GridBagConstraints.WEST;

        jLabel1.setText("Iniciar Sesión");
        EstilosUI.estilizarTituloSeccion(jLabel1);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelCredenciales.add(jLabel1, gbc);

        JLabel lblSubtituloAcceso = new JLabel("Ingrese sus credenciales registradas para acceder al sistema.");
        EstilosUI.estilizarEstadoInfo(lblSubtituloAcceso);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 4, 10, 4);
        panelCredenciales.add(lblSubtituloAcceso, gbc);

        jLabel2.setText("Usuario:");
        jLabel2.setLabelFor(txtUsuario);
        EstilosUI.estilizarEtiquetaCampo(jLabel2);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(6, 4, 6, 10);
        panelCredenciales.add(jLabel2, gbc);

        EstilosUI.estilizarCampoTexto(txtUsuario, "Ingrese su alias de usuario");
        txtUsuario.setMinimumSize(new Dimension(140, txtUsuario.getPreferredSize().height));
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 4, 6, 4);
        panelCredenciales.add(txtUsuario, gbc);

        jLabel3.setText("Contraseña:");
        jLabel3.setLabelFor(txtContrasenia);
        EstilosUI.estilizarEtiquetaCampo(jLabel3);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(6, 4, 6, 10);
        panelCredenciales.add(jLabel3, gbc);

        EstilosUI.estilizarCampoPassword(txtContrasenia);
        txtContrasenia.setMinimumSize(new Dimension(140, txtContrasenia.getPreferredSize().height));
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 4, 6, 4);
        panelCredenciales.add(txtContrasenia, gbc);

        btnIngresar.setText("Ingresar");
        EstilosUI.estilizarBotonPrimario(btnIngresar);
        btnIngresar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIngresarActionPerformed(evt);
            }
        });
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(12, 4, 2, 4);
        panelCredenciales.add(btnIngresar, gbc);

        // --- Tarjeta de accesos alternativos ---
        JPanel panelOpciones = new JPanel(new GridBagLayout());
        EstilosUI.estilizarTarjetaConTitulo(panelOpciones, "Otras opciones de acceso");

        GridBagConstraints gbcOpc = new GridBagConstraints();
        gbcOpc.insets = new Insets(4, 4, 6, 4);
        gbcOpc.anchor = GridBagConstraints.WEST;
        gbcOpc.gridx = 0;
        gbcOpc.gridy = 0;
        gbcOpc.weightx = 1.0;
        gbcOpc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblAyudaOpciones = new JLabel("Registre una cuenta nueva o consulte torneos sin autenticarse.");
        EstilosUI.estilizarEstadoInfo(lblAyudaOpciones);
        panelOpciones.add(lblAyudaOpciones, gbcOpc);

        btnCrearCuenta.setText("Crear Cuenta");
        EstilosUI.estilizarBotonNeutro(btnCrearCuenta);
        btnCrearCuenta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCrearCuentaActionPerformed(evt);
            }
        });

        btnAccesoPublico.setText("Acceso Público (Invitado)");
        EstilosUI.estilizarBotonSecundario(btnAccesoPublico);
        btnAccesoPublico.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAccesoPublicoActionPerformed(evt);
            }
        });

        JPanel panelBotonesSec = new JPanel(new GridLayout(1, 2, 10, 0));
        panelBotonesSec.setBackground(EstilosUI.BG_SURFACE);
        panelBotonesSec.add(btnCrearCuenta);
        panelBotonesSec.add(btnAccesoPublico);

        gbcOpc.gridy = 1;
        gbcOpc.insets = new Insets(6, 4, 2, 4);
        panelOpciones.add(panelBotonesSec, gbcOpc);

        // --- Contenedor central adaptable ---
        JPanel panelColumna = new JPanel(new GridBagLayout());
        panelColumna.setBackground(EstilosUI.BG_CANVAS);

        GridBagConstraints gbcCol = new GridBagConstraints();
        gbcCol.gridx = 0;
        gbcCol.gridy = 0;
        gbcCol.weightx = 1.0;
        gbcCol.fill = GridBagConstraints.HORIZONTAL;
        gbcCol.insets = new Insets(0, 0, 12, 0);
        panelColumna.add(panelCredenciales, gbcCol);

        gbcCol.gridy = 1;
        gbcCol.insets = new Insets(0, 0, 0, 0);
        panelColumna.add(panelOpciones, gbcCol);

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
        panelWrapper.add(panelColumna, BorderLayout.CENTER);

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

        panel.add(panelEncabezado, BorderLayout.NORTH);
        panel.add(scrollCentro, BorderLayout.CENTER);
        setContentPane(panel);

        pack();
        setMinimumSize(new Dimension(Math.max(getWidth() + 18, 474), Math.min(getHeight(), 360)));
        setSize(Math.max(getWidth(), getMinimumSize().width), getHeight());
    }

    private void btnIngresarActionPerformed(java.awt.event.ActionEvent evt) {
        String usr = txtUsuario.getText();
        String pass = new String(txtContrasenia.getPassword());

        com.gestionamateur.tpi.progav.modelo.Usuario usuarioLogueado = gestorUsuarios.autenticarUsuario(usr, pass);

        if (usuarioLogueado != null) {
            javax.swing.JOptionPane.showMessageDialog(this, "Bienvenido " + usuarioLogueado.getNombre());
            
            FrmPrincipal menu = new FrmPrincipal(usuarioLogueado, gestorUsuarios.getAuthService());
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

    /**
     * Acceso publico anonimo (ADR-02 y ADR-18): el rol PUBLICO es un perfil de solo lectura para
     * consultar fixture, resultados, tablas de posiciones y estadisticas, sin cuenta de usuario ni
     * credenciales. La ventana principal administrativa no aplica a este rol: se abre
     * directamente la ventana de consulta publica.
     */
    private void btnAccesoPublicoActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            FrmConsultaPublica ventanaPublica = new FrmConsultaPublica(null);
            ventanaPublica.setVisible(true);
            this.dispose();
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Error al iniciar la consulta publica: " + e.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
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

    private javax.swing.JButton btnAccesoPublico;
    private javax.swing.JButton btnCrearCuenta;
    private javax.swing.JButton btnIngresar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPasswordField txtContrasenia;
    private javax.swing.JTextField txtUsuario;
}
