package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.controlador.GestorUsuarios;
import com.gestionamateur.tpi.progav.modelo.Arbitro;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.seguridad.PoliticaPermisos;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

/**
 * CU-17: Gestión integral de usuarios (actor: Administrador).
 *
 * Delega todas las consultas y mutaciones en {@link GestorUsuarios} (capa de controlador/servicio),
 * sin invocar DAOs directamente desde la interfaz.
 * Incluye alta, edición de perfil/rol/datos arbitrales, restablecimiento seguro de contraseña
 * y activación/desactivación lógica con protecciones de último administrador y autobloqueo.
 */
public class FrmGestorUsuarios extends JFrame {

    private static final TiposDatos.Rol[] ROLES_ADMISIBLES = {
            TiposDatos.Rol.ADMINISTRADOR,
            TiposDatos.Rol.ORGANIZADOR,
            TiposDatos.Rol.ARBITRO,
            TiposDatos.Rol.CAPITAN};

    private final Usuario operador;
    private final GestorUsuarios gestorUsuarios = new GestorUsuarios();

    private final List<Usuario> usuarios = new ArrayList<>();

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Usuario", "Nombre y apellido", "Correo", "Rol", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tablaUsuarios = new JTable(modeloTabla);
    private final JLabel lblInfo = new JLabel(" ");
    private final JLabel lblSeleccion = new JLabel(" ");
    private final JButton btnActualizar = new JButton("Actualizar listado");
    private final JButton btnCrear = new JButton("Crear usuario");
    private final JButton btnEditar = new JButton("Editar usuario");
    private final JButton btnRestablecerPass = new JButton("Restablecer contraseña");
    private final JButton btnActivar = new JButton("Activar cuenta");
    private final JButton btnDesactivar = new JButton("Desactivar cuenta");

    public FrmGestorUsuarios(Usuario operador) {
        this.operador = operador;
        setTitle("Gestión de Usuarios (CU-17)");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        cargarUsuarios();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        EstilosUI.estilizarVentana(panel);

        JPanel panelEncabezado = EstilosUI.crearEncabezado(
                "Gestión de usuarios",
                describirOperador());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JPanel panelSup = new JPanel(new GridBagLayout());
        EstilosUI.estilizarTarjeta(panelSup);

        JLabel lblSeccion = new JLabel("Cuentas y roles del sistema");
        EstilosUI.estilizarTituloSeccion(lblSeccion);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelSup.add(lblSeccion, gbc);

        EstilosUI.estilizarTextoPrimario(lblSeleccion);
        gbc.gridy = 1;
        panelSup.add(lblSeleccion, gbc);

        EstilosUI.estilizarEstadoInfo(lblInfo);
        gbc.gridy = 2;
        panelSup.add(lblInfo, gbc);

        tablaUsuarios.setAutoCreateRowSorter(false);
        tablaUsuarios.getSelectionModel().addListSelectionListener(e -> actualizarSeleccion());

        JScrollPane scroll = new JScrollPane(tablaUsuarios);
        scroll.setPreferredSize(new Dimension(900, 300));
        EstilosUI.estilizarTabla(tablaUsuarios, scroll);
        TableColumnModel columnas = tablaUsuarios.getColumnModel();
        if (columnas.getColumnCount() >= 6) {
            columnas.getColumn(0).setPreferredWidth(55);
            columnas.getColumn(0).setMaxWidth(75);
            columnas.getColumn(0).setCellRenderer(EstilosUI.crearRendererCebra(SwingConstants.CENTER));
            columnas.getColumn(5).setPreferredWidth(110);
            columnas.getColumn(5).setCellRenderer(EstilosUI.crearRendererEstadoActivo("ACTIVO"));
        }

        EstilosUI.estilizarBotonNeutro(btnActualizar);
        btnActualizar.addActionListener(e -> cargarUsuarios());

        EstilosUI.estilizarBotonPrimario(btnCrear);
        btnCrear.addActionListener(e -> crearUsuario());

        EstilosUI.estilizarBotonSecundario(btnEditar);
        btnEditar.addActionListener(e -> editarUsuario());

        EstilosUI.estilizarBotonNeutro(btnRestablecerPass);
        btnRestablecerPass.addActionListener(e -> restablecerContrasena());

        EstilosUI.estilizarBotonSecundario(btnActivar);
        btnActivar.addActionListener(e -> cambiarEstadoCuenta(true));

        EstilosUI.estilizarBotonAdvertencia(btnDesactivar);
        btnDesactivar.addActionListener(e -> cambiarEstadoCuenta(false));

        JButton btnVolver = new JButton("Volver al menú");
        EstilosUI.estilizarBotonNeutro(btnVolver);
        btnVolver.addActionListener(e -> dispose());

        JPanel panelAcciones = EstilosUI.crearBarraAcciones(
                new JComponent[]{btnActualizar, btnCrear, btnEditar, btnRestablecerPass, btnActivar, btnDesactivar},
                new JComponent[]{btnVolver});

        JPanel panelCentro = new JPanel(new BorderLayout(12, 12));
        panelCentro.setBackground(EstilosUI.BG_CANVAS);
        panelCentro.add(panelSup, BorderLayout.NORTH);
        panelCentro.add(scroll, BorderLayout.CENTER);

        panel.add(panelEncabezado, BorderLayout.NORTH);
        panel.add(panelCentro, BorderLayout.CENTER);
        panel.add(panelAcciones, BorderLayout.SOUTH);
        setContentPane(panel);
        pack();
        setMinimumSize(new Dimension(1080, 600));
    }

    private String describirOperador() {
        if (!esOperadorAdministrador()) {
            return "Operador: sin autorización de Administrador.";
        }
        return "Operador: " + operador.getNombreUsuario() + " (" + operador.getRol().name() + ")";
    }

    private void cargarUsuarios() {
        boolean autorizado = esOperadorAdministrador();
        btnActualizar.setEnabled(autorizado);
        btnCrear.setEnabled(autorizado);
        btnEditar.setEnabled(autorizado);
        btnRestablecerPass.setEnabled(autorizado);
        btnActivar.setEnabled(autorizado);
        btnDesactivar.setEnabled(autorizado);
        if (!autorizado) {
            usuarios.clear();
            modeloTabla.setRowCount(0);
            lblInfo.setText("Acceso denegado: solo un Administrador autenticado puede gestionar usuarios.");
            lblSeleccion.setText(" ");
            return;
        }
        try {
            List<Usuario> cargados = gestorUsuarios.listarUsuarios(operador);
            usuarios.clear();
            if (cargados != null) {
                usuarios.addAll(cargados);
            }
            modeloTabla.setRowCount(0);
            for (Usuario u : usuarios) {
                modeloTabla.addRow(new Object[]{
                        u.getId(),
                        u.getNombreUsuario(),
                        (u.getNombre() == null ? "" : u.getNombre()) + " " + (u.getApellido() == null ? "" : u.getApellido()),
                        u.getMail(),
                        u.getRol() != null ? u.getRol().name() : "",
                        u.isActivo() ? "ACTIVO" : "INACTIVO"});
            }
            if (usuarios.isEmpty()) {
                lblInfo.setText("No hay cuentas de usuario registradas en el sistema.");
                lblSeleccion.setText(" ");
            } else {
                lblInfo.setText("Seleccione una cuenta de la tabla para editarla, cambiar su contraseña o su estado.");
                lblSeleccion.setText(" ");
            }
        } catch (TorneoExcepcion ex) {
            lblInfo.setText("No se pudo cargar el listado de usuarios: " + ex.getMessage());
        }
    }

    private void actualizarSeleccion() {
        Usuario u = obtenerUsuarioSeleccionado();
        if (u == null) {
            lblSeleccion.setText("No hay ninguna cuenta seleccionada.");
        } else {
            lblSeleccion.setText("Cuenta seleccionada: " + u.getNombreUsuario() + " - Rol: "
                    + (u.getRol() != null ? u.getRol().name() : "N/D") + " - Estado: "
                    + (u.isActivo() ? "ACTIVO" : "INACTIVO"));
        }
    }

    private Usuario obtenerUsuarioSeleccionado() {
        int fila = tablaUsuarios.getSelectedRow();
        if (fila < 0 || fila >= usuarios.size()) {
            return null;
        }
        return usuarios.get(fila);
    }

    private boolean esOperadorAdministrador() {
        return PoliticaPermisos.tienePermisoOperacion(operador, PoliticaPermisos.ModuloMenu.GESTIONAR_USUARIOS);
    }

    private void crearUsuario() {
        if (!esOperadorAdministrador()) {
            JOptionPane.showMessageDialog(this,
                    "Acceso denegado: solo un Administrador autenticado puede crear cuentas de usuario.",
                    "Alta de usuario", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JTextField txtUsuario = new JTextField(16);
        JTextField txtMail = new JTextField(18);
        JTextField txtNombre = new JTextField(16);
        JTextField txtApellido = new JTextField(16);
        JPasswordField txtContrasena = new JPasswordField(16);
        JPasswordField txtRepeticion = new JPasswordField(16);
        JComboBox<String> cmbRol = new JComboBox<>();
        for (TiposDatos.Rol rol : ROLES_ADMISIBLES) {
            cmbRol.addItem(rol.name());
        }
        JTextField txtCategoria = new JTextField(16);
        JTextField txtTelefono = new JTextField(16);

        EstilosUI.estilizarCampoTexto(txtUsuario, "Ej: juan.perez");
        EstilosUI.estilizarCampoTexto(txtMail, "Ej: usuario@ugd.edu.ar");
        EstilosUI.estilizarCampoTexto(txtNombre, null);
        EstilosUI.estilizarCampoTexto(txtApellido, null);
        EstilosUI.estilizarCampoPassword(txtContrasena);
        EstilosUI.estilizarCampoPassword(txtRepeticion);
        EstilosUI.estilizarCombo(cmbRol);
        EstilosUI.estilizarCampoTexto(txtCategoria, "Solo árbitros");
        EstilosUI.estilizarCampoTexto(txtTelefono, "Solo árbitros");
        Runnable actualizarCamposArbitro = () -> {
            boolean esArbitro = TiposDatos.Rol.ARBITRO.name().equals(cmbRol.getSelectedItem());
            txtCategoria.setEnabled(esArbitro);
            txtTelefono.setEnabled(esArbitro);
        };
        cmbRol.addActionListener(e -> actualizarCamposArbitro.run());
        actualizarCamposArbitro.run();

        JPanel form = new JPanel(new GridBagLayout());
        EstilosUI.estilizarPanelModal(form);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.anchor = GridBagConstraints.WEST;

        int fila = 0;
        JLabel lblAlias = new JLabel("Alias de usuario:");
        EstilosUI.estilizarEtiquetaCampo(lblAlias);
        gbc.gridx = 0;
        gbc.gridy = fila;
        form.add(lblAlias, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        form.add(txtUsuario, gbc);

        fila++;
        JLabel lblMail = new JLabel("Correo electrónico:");
        EstilosUI.estilizarEtiquetaCampo(lblMail);
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        form.add(lblMail, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        form.add(txtMail, gbc);

        fila++;
        JLabel lblNombre = new JLabel("Nombre:");
        EstilosUI.estilizarEtiquetaCampo(lblNombre);
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        form.add(lblNombre, gbc);
        gbc.gridx = 1;
        form.add(txtNombre, gbc);
        JLabel lblApellido = new JLabel("Apellido:");
        EstilosUI.estilizarEtiquetaCampo(lblApellido);
        gbc.gridx = 2;
        form.add(lblApellido, gbc);
        gbc.gridx = 3;
        form.add(txtApellido, gbc);

        fila++;
        JLabel lblRol = new JLabel("Rol:");
        EstilosUI.estilizarEtiquetaCampo(lblRol);
        gbc.gridx = 0;
        gbc.gridy = fila;
        form.add(lblRol, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        form.add(cmbRol, gbc);

        fila++;
        JLabel lblPass = new JLabel("Contraseña:");
        EstilosUI.estilizarEtiquetaCampo(lblPass);
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        form.add(lblPass, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        form.add(txtContrasena, gbc);

        fila++;
        JLabel lblRep = new JLabel("Repetir contraseña:");
        EstilosUI.estilizarEtiquetaCampo(lblRep);
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        form.add(lblRep, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        form.add(txtRepeticion, gbc);

        fila++;
        JLabel lblCat = new JLabel("Categoría arbitral (solo árbitros):");
        EstilosUI.estilizarEtiquetaCampo(lblCat);
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        form.add(lblCat, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        form.add(txtCategoria, gbc);

        fila++;
        JLabel lblTel = new JLabel("Teléfono (solo árbitros):");
        EstilosUI.estilizarEtiquetaCampo(lblTel);
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        form.add(lblTel, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        form.add(txtTelefono, gbc);

        int opcion = JOptionPane.showConfirmDialog(this, form, "Nueva cuenta de usuario",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }

        String alias = txtUsuario.getText() == null ? "" : txtUsuario.getText().trim();
        String mail = txtMail.getText() == null ? "" : txtMail.getText().trim();
        String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();
        String apellido = txtApellido.getText() == null ? "" : txtApellido.getText().trim();
        String categoria = txtCategoria.getText() == null ? "" : txtCategoria.getText().trim();
        String telefono = txtTelefono.getText() == null ? "" : txtTelefono.getText().trim();
        String rolTexto = (String) cmbRol.getSelectedItem();
        char[] pass = txtContrasena.getPassword();
        char[] repeticion = txtRepeticion.getPassword();
        String contrasena = new String(pass);
        String repetir = new String(repeticion);
        limpiarArreglo(pass);
        limpiarArreglo(repeticion);

        if (alias.isEmpty() || mail.isEmpty() || nombre.isEmpty() || apellido.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Complete todos los campos obligatorios de la cuenta.",
                    "Alta de usuario", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (contrasena.isEmpty()) {
            JOptionPane.showMessageDialog(this, "La contraseña es obligatoria.", "Alta de usuario",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!contrasena.equals(repetir)) {
            JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden.", "Alta de usuario",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        TiposDatos.Rol rol;
        try {
            rol = TiposDatos.Rol.valueOf(rolTexto);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un rol válido.", "Alta de usuario",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Los roles privilegiados requieren un operador Administrador (fail-closed).
        boolean rolPrivilegiado = rol == TiposDatos.Rol.ADMINISTRADOR
                || rol == TiposDatos.Rol.ORGANIZADOR
                || rol == TiposDatos.Rol.ARBITRO;
        if (rolPrivilegiado && !esOperadorAdministrador()) {
            JOptionPane.showMessageDialog(this,
                    "ERROR DE SEGURIDAD: Acceso denegado. Solo un Administrador autenticado puede crear usuarios con roles privilegiados ("
                            + rol.name() + ").",
                    "Alta de usuario", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (rol == TiposDatos.Rol.ARBITRO && (categoria.isEmpty() || telefono.isEmpty())) {
            JOptionPane.showMessageDialog(this,
                    "Para un árbitro debe indicar la categoría arbitral y el teléfono.",
                    "Alta de usuario", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Confirma crear la cuenta \"" + alias + "\" con rol " + rol.name() + "?",
                "Confirmar alta de usuario", JOptionPane.YES_NO_OPTION);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        String respuesta = gestorUsuarios.registrarUsuario(
                alias, contrasena, mail, nombre, apellido, rol, categoria, telefono, operador);
        if (respuesta != null && respuesta.startsWith("ERROR")) {
            JOptionPane.showMessageDialog(this, respuesta, "Alta de usuario", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(this,
                respuesta != null ? respuesta : "ÉXITO: La cuenta fue creada.",
                "Alta de usuario", JOptionPane.INFORMATION_MESSAGE);
        cargarUsuarios();
    }

    private void editarUsuario() {
        if (!esOperadorAdministrador()) {
            return;
        }
        Usuario u = obtenerUsuarioSeleccionado();
        if (u == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una cuenta de la tabla.", "Editar usuario",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField txtMail = new JTextField(u.getMail() == null ? "" : u.getMail(), 18);
        JTextField txtNombre = new JTextField(u.getNombre() == null ? "" : u.getNombre(), 16);
        JTextField txtApellido = new JTextField(u.getApellido() == null ? "" : u.getApellido(), 16);
        JComboBox<String> cmbRol = new JComboBox<>();
        for (TiposDatos.Rol r : ROLES_ADMISIBLES) {
            cmbRol.addItem(r.name());
        }
        if (u.getRol() != null) {
            cmbRol.setSelectedItem(u.getRol().name());
        }
        JTextField txtCategoria = new JTextField(16);
        JTextField txtTelefono = new JTextField(16);
        if (u.getRol() == TiposDatos.Rol.ARBITRO) {
            try {
                Arbitro arb = gestorUsuarios.obtenerDetalleArbitro(u.getId(), operador);
                if (arb != null) {
                    txtCategoria.setText(arb.getCategoriaArbitral() == null ? "" : arb.getCategoriaArbitral());
                    txtTelefono.setText(arb.getTelefono() == null ? "" : arb.getTelefono());
                }
            } catch (TorneoExcepcion ignored) {
                // El administrador podrá completar o corregir categoría y teléfono si no estaban cargados.
            }
        }

        EstilosUI.estilizarCampoTexto(txtMail, null);
        EstilosUI.estilizarCampoTexto(txtNombre, null);
        EstilosUI.estilizarCampoTexto(txtApellido, null);
        EstilosUI.estilizarCombo(cmbRol);
        EstilosUI.estilizarCampoTexto(txtCategoria, "Obligatorio para árbitros");
        EstilosUI.estilizarCampoTexto(txtTelefono, "Obligatorio para árbitros");

        Runnable toggleArbitro = () -> {
            boolean esArbitro = TiposDatos.Rol.ARBITRO.name().equals(cmbRol.getSelectedItem());
            txtCategoria.setEnabled(esArbitro);
            txtTelefono.setEnabled(esArbitro);
        };
        cmbRol.addActionListener(e -> toggleArbitro.run());
        toggleArbitro.run();

        JPanel form = new JPanel(new GridBagLayout());
        EstilosUI.estilizarPanelModal(form);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.anchor = GridBagConstraints.WEST;

        int fila = 0;
        JLabel lblAlias = new JLabel("Alias (inmutable):");
        EstilosUI.estilizarEtiquetaCampo(lblAlias);
        gbc.gridx = 0;
        gbc.gridy = fila;
        form.add(lblAlias, gbc);
        JLabel lblAliasVal = new JLabel(u.getNombreUsuario() + " (ID " + u.getId() + ")");
        EstilosUI.estilizarTextoPrimario(lblAliasVal);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        form.add(lblAliasVal, gbc);

        fila++;
        JLabel lblMail = new JLabel("Correo electrónico:");
        EstilosUI.estilizarEtiquetaCampo(lblMail);
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        form.add(lblMail, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        form.add(txtMail, gbc);

        fila++;
        JLabel lblNombre = new JLabel("Nombre:");
        EstilosUI.estilizarEtiquetaCampo(lblNombre);
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        form.add(lblNombre, gbc);
        gbc.gridx = 1;
        form.add(txtNombre, gbc);
        JLabel lblApellido = new JLabel("Apellido:");
        EstilosUI.estilizarEtiquetaCampo(lblApellido);
        gbc.gridx = 2;
        form.add(lblApellido, gbc);
        gbc.gridx = 3;
        form.add(txtApellido, gbc);

        fila++;
        JLabel lblRol = new JLabel("Rol:");
        EstilosUI.estilizarEtiquetaCampo(lblRol);
        gbc.gridx = 0;
        gbc.gridy = fila;
        form.add(lblRol, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        form.add(cmbRol, gbc);

        fila++;
        JLabel lblCat = new JLabel("Categoría arbitral:");
        EstilosUI.estilizarEtiquetaCampo(lblCat);
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        form.add(lblCat, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        form.add(txtCategoria, gbc);

        fila++;
        JLabel lblTel = new JLabel("Teléfono:");
        EstilosUI.estilizarEtiquetaCampo(lblTel);
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        form.add(lblTel, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        form.add(txtTelefono, gbc);

        int opcion = JOptionPane.showConfirmDialog(this, form, "Editar usuario \"" + u.getNombreUsuario() + "\"",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }
        String mail = txtMail.getText() == null ? "" : txtMail.getText().trim();
        String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();
        String apellido = txtApellido.getText() == null ? "" : txtApellido.getText().trim();
        String categoria = txtCategoria.getText() == null ? "" : txtCategoria.getText().trim();
        String telefono = txtTelefono.getText() == null ? "" : txtTelefono.getText().trim();
        TiposDatos.Rol nuevoRol = TiposDatos.Rol.valueOf((String) cmbRol.getSelectedItem());

        if (mail.isEmpty() || nombre.isEmpty() || apellido.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Correo, nombre y apellido son obligatorios.",
                    "Editar usuario", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int conf = JOptionPane.showConfirmDialog(this,
                "¿Confirma guardar los cambios en la cuenta \"" + u.getNombreUsuario() + "\"?",
                "Confirmar edición", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) {
            return;
        }
        String respuesta = gestorUsuarios.actualizarUsuario(
                u.getId(), mail, nombre, apellido, nuevoRol, categoria, telefono, operador);
        if (respuesta.startsWith("ERROR")) {
            JOptionPane.showMessageDialog(this, respuesta, "Editar usuario", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, respuesta, "Editar usuario", JOptionPane.INFORMATION_MESSAGE);
            cargarUsuarios();
        }
    }

    private void restablecerContrasena() {
        if (!esOperadorAdministrador()) {
            return;
        }
        Usuario u = obtenerUsuarioSeleccionado();
        if (u == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una cuenta de la tabla.", "Restablecer contraseña",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        JPasswordField txtNueva = new JPasswordField(16);
        JPasswordField txtRepetir = new JPasswordField(16);
        EstilosUI.estilizarCampoPassword(txtNueva);
        EstilosUI.estilizarCampoPassword(txtRepetir);

        JPanel form = new JPanel(new GridBagLayout());
        EstilosUI.estilizarPanelModal(form);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel lblCuenta = new JLabel("Cuenta: " + u.getNombreUsuario());
        EstilosUI.estilizarTextoPrimario(lblCuenta);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        form.add(lblCuenta, gbc);

        JLabel lblN = new JLabel("Nueva contraseña:");
        EstilosUI.estilizarEtiquetaCampo(lblN);
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        form.add(lblN, gbc);
        gbc.gridx = 1;
        form.add(txtNueva, gbc);

        JLabel lblR = new JLabel("Repetir contraseña:");
        EstilosUI.estilizarEtiquetaCampo(lblR);
        gbc.gridx = 0;
        gbc.gridy = 2;
        form.add(lblR, gbc);
        gbc.gridx = 1;
        form.add(txtRepetir, gbc);

        int opcion = JOptionPane.showConfirmDialog(this, form,
                "Restablecer contraseña de \"" + u.getNombreUsuario() + "\"",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }
        char[] p1 = txtNueva.getPassword();
        char[] p2 = txtRepetir.getPassword();
        String nueva;
        String repetir;
        try {
            nueva = new String(p1);
            repetir = new String(p2);
        } finally {
            limpiarArreglo(p1);
            limpiarArreglo(p2);
        }
        if (nueva.isEmpty() || !nueva.equals(repetir)) {
            JOptionPane.showMessageDialog(this, "Las contraseñas deben coincidir y no estar vacías.",
                    "Restablecer contraseña", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int conf = JOptionPane.showConfirmDialog(this,
                "¿Confirma restablecer la contraseña de la cuenta \"" + u.getNombreUsuario() + "\"?",
                "Confirmar restablecimiento", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) {
            return;
        }
        String respuesta = gestorUsuarios.restablecerContrasena(u.getId(), nueva, operador);
        if (respuesta.startsWith("ERROR")) {
            JOptionPane.showMessageDialog(this, respuesta, "Restablecer contraseña", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, respuesta, "Restablecer contraseña", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void cambiarEstadoCuenta(boolean activo) {
        if (!esOperadorAdministrador()) {
            JOptionPane.showMessageDialog(this,
                    "ERROR DE SEGURIDAD: Solo un Administrador autenticado puede activar o desactivar cuentas.",
                    "Estado de la cuenta", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Usuario u = obtenerUsuarioSeleccionado();
        if (u == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una cuenta de la tabla.", "Estado de la cuenta",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (u.isActivo() == activo) {
            JOptionPane.showMessageDialog(this,
                    "La cuenta \"" + u.getNombreUsuario() + "\" ya se encuentra en ese estado.",
                    "Estado de la cuenta", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder mensaje = new StringBuilder();
        mensaje.append(activo ? "¿Confirma activar la cuenta " : "¿Confirma desactivar la cuenta ");
        mensaje.append("\"").append(u.getNombreUsuario()).append("\"?");
        int confirmacion = JOptionPane.showConfirmDialog(this, mensaje.toString(),
                "Confirmar cambio de estado", JOptionPane.YES_NO_OPTION);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        String respuesta = gestorUsuarios.cambiarEstadoCuenta(u.getId(), activo, operador);
        if (respuesta.startsWith("ERROR")) {
            JOptionPane.showMessageDialog(this, respuesta, "Estado de la cuenta", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, respuesta, "Estado de la cuenta", JOptionPane.INFORMATION_MESSAGE);
            cargarUsuarios();
        }
    }

    /**
     * Sobrescribe el arreglo de caracteres de la contrasena para no dejarla en memoria.
     */
    private static void limpiarArreglo(char[] contenido) {
        if (contenido != null) {
            java.util.Arrays.fill(contenido, '\0');
        }
    }
}
