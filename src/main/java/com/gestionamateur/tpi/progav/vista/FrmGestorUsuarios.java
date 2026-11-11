package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.controlador.GestorUsuarios;
import com.gestionamateur.tpi.progav.dao.ArbitroDAO;
import com.gestionamateur.tpi.progav.dao.UsuarioDAO;
import com.gestionamateur.tpi.progav.modelo.Arbitro;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.servicio.AuthService;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * CU-17: Gestion de usuarios (actor: Administrador).
 *
 * Expone el listado de cuentas con su rol y estado, el alta de usuarios con los roles
 * ADMINISTRADOR, ORGANIZADOR, ARBITRO y CAPITAN, y la activacion/desactivacion de cuentas.
 *
 * Seguridad:
 *  - La contrasena nunca se muestra en pantalla ni se escribe en logs. Solo se hashea
 *    con BCrypt (AuthService.hashearBCrypt) antes de persistir.
 *  - Los roles privilegiados (ADMINISTRADOR, ORGANIZADOR, ARBITRO) solo pueden ser dados
 *    de alta por un operador con rol ADMINISTRADOR.
 *  - El alta de un arbitro inserta ademas su fila en la tabla 'arbitros' con la categoria
 *    arbitral y el telefono indicados.
 */
public class FrmGestorUsuarios extends JFrame {

    private static final TiposDatos.Rol[] ROLES_ADMISIBLES = {
            TiposDatos.Rol.ADMINISTRADOR,
            TiposDatos.Rol.ORGANIZADOR,
            TiposDatos.Rol.ARBITRO,
            TiposDatos.Rol.CAPITAN};

    private final Usuario operador;
    private final GestorUsuarios gestorUsuarios = new GestorUsuarios();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final ArbitroDAO arbitroDAO = new ArbitroDAO();

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

    public FrmGestorUsuarios(Usuario operador) {
        this.operador = operador;
        setTitle("Gestión de Usuarios (CU-17)");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        cargarUsuarios();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JPanel panelSup = new JPanel(new GridBagLayout());
        panelSup.setBackground(Color.WHITE);

        JLabel titulo = new JLabel("Gestión de usuarios");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(new Color(0x12, 0x38, 0x5C));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        panelSup.add(titulo, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        JLabel lblOperador = new JLabel(describirOperador());
        lblOperador.setForeground(new Color(0x3A, 0x55, 0x68));
        panelSup.add(lblOperador, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        lblSeleccion.setForeground(new Color(0x3A, 0x55, 0x68));
        panelSup.add(lblSeleccion, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        lblInfo.setForeground(new Color(0x3A, 0x55, 0x68));
        panelSup.add(lblInfo, gbc);

        tablaUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaUsuarios.setAutoCreateRowSorter(false);
        tablaUsuarios.getSelectionModel().addListSelectionListener(e -> actualizarSeleccion());

        JScrollPane scroll = new JScrollPane(tablaUsuarios);
        scroll.setPreferredSize(new Dimension(900, 300));

        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton btnActualizar = new JButton("Actualizar listado");
        btnActualizar.addActionListener(e -> cargarUsuarios());
        JButton btnCrear = new JButton("Crear usuario");
        btnCrear.addActionListener(e -> crearUsuario());
        JButton btnActivar = new JButton("Activar cuenta");
        btnActivar.addActionListener(e -> cambiarEstadoCuenta(true));
        JButton btnDesactivar = new JButton("Desactivar cuenta");
        btnDesactivar.addActionListener(e -> cambiarEstadoCuenta(false));
        JButton btnVolver = new JButton("Volver al menú");
        btnVolver.addActionListener(e -> dispose());
        panelAcciones.add(btnActualizar);
        panelAcciones.add(btnCrear);
        panelAcciones.add(btnActivar);
        panelAcciones.add(btnDesactivar);
        panelAcciones.add(btnVolver);

        JPanel panelCentro = new JPanel(new BorderLayout(6, 6));
        panelCentro.setBackground(Color.WHITE);
        panelCentro.add(panelSup, BorderLayout.NORTH);
        panelCentro.add(scroll, BorderLayout.CENTER);

        panel.add(panelCentro, BorderLayout.CENTER);
        panel.add(panelAcciones, BorderLayout.SOUTH);
        setContentPane(panel);
        pack();
        setMinimumSize(new Dimension(940, 560));
    }

    private String describirOperador() {
        if (operador == null) {
            return "Operador: no identificado. Las operaciones de escritura fueron rechazadas.";
        }
        return "Operador: " + operador.getNombreUsuario() + " (" + operador.getRol().name() + ")";
    }

    private void cargarUsuarios() {
        try {
            List<Usuario> cargados = usuarioDAO.buscarTodos();
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
                lblInfo.setText("Seleccione una cuenta de la tabla para activarla o desactivarla.");
                lblSeleccion.setText(" ");
            }
        } catch (SQLException ex) {
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
        return operador != null && operador.getRol() == TiposDatos.Rol.ADMINISTRADOR;
    }

    private void crearUsuario() {
        if (operador == null) {
            JOptionPane.showMessageDialog(this,
                    "Debe iniciar sesión para dar de alta una cuenta de usuario.", "Alta de usuario",
                    JOptionPane.WARNING_MESSAGE);
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

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        int fila = 0;
        gbc.gridx = 0;
        gbc.gridy = fila;
        form.add(new JLabel("Alias de usuario:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        form.add(txtUsuario, gbc);

        fila++;
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 1;
        form.add(new JLabel("Correo electrónico:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        form.add(txtMail, gbc);

        fila++;
        gbc.gridx = 0;
        gbc.gridy = fila;
        form.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        form.add(txtNombre, gbc);
        gbc.gridx = 2;
        form.add(new JLabel("Apellido:"), gbc);
        gbc.gridx = 3;
        form.add(txtApellido, gbc);

        fila++;
        gbc.gridx = 0;
        gbc.gridy = fila;
        form.add(new JLabel("Rol:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        form.add(cmbRol, gbc);

        fila++;
        gbc.gridx = 0;
        gbc.gridy = fila;
        form.add(new JLabel("Contraseña:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        form.add(txtContrasena, gbc);

        fila++;
        gbc.gridx = 0;
        gbc.gridy = fila;
        form.add(new JLabel("Repetir contraseña:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        form.add(txtRepeticion, gbc);

        fila++;
        gbc.gridx = 0;
        gbc.gridy = fila;
        form.add(new JLabel("Categoría arbitral (solo árbitros):"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        form.add(txtCategoria, gbc);

        fila++;
        gbc.gridx = 0;
        gbc.gridy = fila;
        form.add(new JLabel("Teléfono (solo árbitros):"), gbc);
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

        // El alias debe ser unico en la base de datos.
        try {
            if (usuarioDAO.buscarPorUsuario(alias) != null) {
                JOptionPane.showMessageDialog(this, "El alias \"" + alias + "\" ya está registrado.",
                        "Alta de usuario", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "ERROR: No se pudo verificar la unicidad del alias. "
                    + ex.getMessage(), "Alta de usuario", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (rol == TiposDatos.Rol.ARBITRO) {
                crearArbitro(alias, contrasena, mail, nombre, apellido, categoria, telefono);
            } else {
                String respuesta = gestorUsuarios.registrarUsuario(alias, contrasena, mail, nombre, apellido, rol, operador);
                if (respuesta != null && respuesta.startsWith("ERROR")) {
                    JOptionPane.showMessageDialog(this, respuesta, "Alta de usuario", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                JOptionPane.showMessageDialog(this,
                        respuesta != null ? respuesta : "ÉXITO: La cuenta fue creada.",
                        "Alta de usuario", JOptionPane.INFORMATION_MESSAGE);
            }
            cargarUsuarios();
        } catch (TorneoExcepcion ex) {
            JOptionPane.showMessageDialog(this, "ERROR DE VALIDACIÓN: " + ex.getMessage(), "Alta de usuario",
                    JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "ERROR DE CONEXIÓN: " + ex.getMessage(), "Alta de usuario",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Da de alta un arbitro: usuario en la tabla 'usuarios' mas su fila en 'arbitros'.
     * La contrasena se hashea con BCrypt; nunca se persiste ni se muestra en claro.
     */
    private void crearArbitro(String alias, String contrasena, String mail, String nombre,
                               String apellido, String categoria, String telefono)
            throws TorneoExcepcion, SQLException {
        AuthService.validarPoliticaContrasena(contrasena);
        String hash = AuthService.hashearBCrypt(contrasena);
        // Se construye el modelo con el hash: la contrasena en claro no queda en memoria.
        Arbitro arbitro = new Arbitro(alias, hash, mail, nombre, apellido, categoria, telefono);
        arbitroDAO.insertar(arbitro, hash);
        JOptionPane.showMessageDialog(this,
                "ÉXITO: La cuenta de árbitro \"" + alias + "\" (" + nombre + ") ha sido creada en la base de datos.",
                "Alta de usuario", JOptionPane.INFORMATION_MESSAGE);
    }

    private void cambiarEstadoCuenta(boolean activo) {
        Usuario u = obtenerUsuarioSeleccionado();
        if (u == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una cuenta de la tabla.", "Estado de la cuenta",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!esOperadorAdministrador()) {
            JOptionPane.showMessageDialog(this,
                    "ERROR DE SEGURIDAD: Solo un Administrador autenticado puede activar o desactivar cuentas.",
                    "Estado de la cuenta", JOptionPane.ERROR_MESSAGE);
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
        if (!activo && operador != null && u.getId() != null && u.getId().equals(operador.getId())) {
            mensaje.append("\nAtención: está a punto de desactivar su propia cuenta de sesión.");
        }
        int confirmacion = JOptionPane.showConfirmDialog(this, mensaje.toString(),
                "Confirmar cambio de estado", JOptionPane.YES_NO_OPTION);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            usuarioDAO.actualizarActivo(u.getId(), activo);
            JOptionPane.showMessageDialog(this,
                    "ÉXITO: La cuenta \"" + u.getNombreUsuario() + "\" quedó "
                            + (activo ? "ACTIVA." : "INACTIVA."),
                    "Estado de la cuenta", JOptionPane.INFORMATION_MESSAGE);
            cargarUsuarios();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "ERROR DE CONEXIÓN: " + ex.getMessage(), "Estado de la cuenta",
                    JOptionPane.ERROR_MESSAGE);
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
