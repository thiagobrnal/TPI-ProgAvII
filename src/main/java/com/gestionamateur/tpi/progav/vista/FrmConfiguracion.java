package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.dao.ConfiguracionDAO;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.Usuario;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * CU-18: Gestion de la configuracion general del sistema (actor: Administrador).
 *
 * Permite consultar los parametros globales almacenados en la tabla 'configuraciones'
 * (clave, valor y descripcion) y modificar el valor de un parametro existente.
 *
 * La persistencia se delega en {@link ConfiguracionDAO#actualizarValorInt(String, int)}.
 * Los valores admitidos son enteros no negativos: un valor negativo o no numerico
 * se rechaza con un mensaje claro y no se escribe nada en la base de datos.
 */
public class FrmConfiguracion extends JFrame {

    private final Usuario operador;
    private final ConfiguracionDAO configuracionDAO = new ConfiguracionDAO();

    private final Map<String, Integer> valores = new LinkedHashMap<>();
    private final Map<String, String> descripciones = new LinkedHashMap<>();
    private final List<String> claves = new ArrayList<>();

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"Parámetro", "Valor", "Descripción"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tablaConfiguracion = new JTable(modeloTabla);
    private final JLabel lblInfo = new JLabel(" ");
    private final JLabel lblSeleccion = new JLabel(" ");

    public FrmConfiguracion(Usuario operador) {
        this.operador = operador;
        setTitle("Configuración General (CU-18)");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        cargarConfiguracion();
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

        JLabel titulo = new JLabel("Configuración general del sistema");
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

        tablaConfiguracion.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaConfiguracion.setAutoCreateRowSorter(false);
        tablaConfiguracion.getSelectionModel().addListSelectionListener(e -> actualizarSeleccion());

        JScrollPane scroll = new JScrollPane(tablaConfiguracion);
        scroll.setPreferredSize(new Dimension(900, 260));

        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton btnActualizar = new JButton("Actualizar listado");
        btnActualizar.addActionListener(e -> cargarConfiguracion());
        JButton btnEditar = new JButton("Editar valor (seleccionado)");
        btnEditar.addActionListener(e -> editarValor());
        JButton btnVolver = new JButton("Volver al menú");
        btnVolver.addActionListener(e -> dispose());
        panelAcciones.add(btnActualizar);
        panelAcciones.add(btnEditar);
        panelAcciones.add(btnVolver);

        JPanel panelCentro = new JPanel(new BorderLayout(6, 6));
        panelCentro.setBackground(Color.WHITE);
        panelCentro.add(panelSup, BorderLayout.NORTH);
        panelCentro.add(scroll, BorderLayout.CENTER);

        panel.add(panelCentro, BorderLayout.CENTER);
        panel.add(panelAcciones, BorderLayout.SOUTH);
        setContentPane(panel);
        pack();
        setMinimumSize(new Dimension(940, 520));
    }

    private String describirOperador() {
        if (operador == null) {
            return "Operador: no identificado. Solo se habilita la consulta; la modificación fue rechazada.";
        }
        return "Operador: " + operador.getNombreUsuario() + " (" + operador.getRol().name() + ")";
    }

    private void cargarConfiguracion() {
        try {
            Map<String, Integer> mapaValores = configuracionDAO.obtenerMapa();
            Map<String, String> mapaDescripciones = configuracionDAO.obtenerDescripciones();

            valores.clear();
            descripciones.clear();
            claves.clear();

            if (mapaValores != null) {
                valores.putAll(mapaValores);
                claves.addAll(mapaValores.keySet());
            }
            if (mapaDescripciones != null) {
                descripciones.putAll(mapaDescripciones);
            }

            modeloTabla.setRowCount(0);
            for (String clave : claves) {
                modeloTabla.addRow(new Object[]{
                        clave,
                        valores.get(clave),
                        descripciones.get(clave) != null ? descripciones.get(clave) : ""});
            }

            if (claves.isEmpty()) {
                lblInfo.setText("No hay parámetros de configuración general registrados en el sistema.");
                lblSeleccion.setText(" ");
            } else {
                lblInfo.setText("Seleccione un parámetro de la tabla para modificar su valor.");
                lblSeleccion.setText(" ");
            }
        } catch (SQLException ex) {
            lblInfo.setText("No se pudo cargar la configuración general: " + ex.getMessage());
        }
    }

    private void actualizarSeleccion() {
        String clave = obtenerClaveSeleccionada();
        if (clave == null) {
            lblSeleccion.setText("No hay ningún parámetro seleccionado.");
        } else {
            Integer valor = valores.get(clave);
            lblSeleccion.setText("Parámetro seleccionado: " + clave + " - Valor actual: " + (valor != null ? valor : "N/D"));
        }
    }

    private String obtenerClaveSeleccionada() {
        int fila = tablaConfiguracion.getSelectedRow();
        if (fila < 0 || fila >= claves.size()) {
            return null;
        }
        return claves.get(fila);
    }

    private void editarValor() {
        String clave = obtenerClaveSeleccionada();
        if (clave == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un parámetro de la tabla.", "Configuración general",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (operador == null || operador.getRol() != TiposDatos.Rol.ADMINISTRADOR) {
            JOptionPane.showMessageDialog(this,
                    "ERROR DE SEGURIDAD: Solo un Administrador autenticado puede modificar la configuración general.",
                    "Configuración general", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Integer valorActual = valores.get(clave);
        JTextField txtValor = new JTextField(valorActual != null ? String.valueOf(valorActual) : "0", 8);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Parámetro:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        form.add(new JLabel(clave), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        form.add(new JLabel("Nuevo valor:"), gbc);
        gbc.gridx = 1;
        form.add(txtValor, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        form.add(new JLabel("El valor debe ser un número entero mayor o igual a 0."), gbc);

        int opcion = JOptionPane.showConfirmDialog(this, form, "Modificar parámetro \"" + clave + "\"",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }

        String texto = txtValor.getText() == null ? "" : txtValor.getText().trim();
        if (texto.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar un valor para el parámetro.", "Configuración general",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int nuevoValor;
        try {
            nuevoValor = Integer.parseInt(texto);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "El valor \"" + texto + "\" no es un número entero válido. Ingrese un número entero mayor o igual a 0.",
                    "Configuración general", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (nuevoValor < 0) {
            JOptionPane.showMessageDialog(this,
                    "El valor no puede ser negativo. Los plazos y umbrales del sistema deben ser enteros mayores o iguales a 0.",
                    "Configuración general", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Confirma cambiar el valor de \"" + clave + "\" de " + valorActual + " a " + nuevoValor + "?",
                "Confirmar modificación", JOptionPane.YES_NO_OPTION);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            configuracionDAO.actualizarValorInt(clave, nuevoValor);
            JOptionPane.showMessageDialog(this,
                    "ÉXITO: El parámetro \"" + clave + "\" quedó con valor " + nuevoValor + ".",
                    "Configuración general", JOptionPane.INFORMATION_MESSAGE);
            cargarConfiguracion();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "ERROR DE CONEXIÓN: No se pudo persistir el cambio del parámetro \"" + clave + "\". "
                            + ex.getMessage(),
                    "Configuración general", JOptionPane.ERROR_MESSAGE);
        }
    }
}
