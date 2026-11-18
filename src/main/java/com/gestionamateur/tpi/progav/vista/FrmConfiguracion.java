package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.seguridad.PoliticaPermisos;
import com.gestionamateur.tpi.progav.servicio.ConfiguracionService;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

/**
 * CU-18: Gestion de la configuracion general del sistema (actor: Administrador).
 *
 * Permite consultar los parametros globales almacenados en la tabla 'configuraciones'
 * (clave, valor y descripcion) y modificar el valor de un parametro existente.
 *
 * La consulta y persistencia se delegan en {@link ConfiguracionService}.
 * Los valores admitidos son enteros no negativos: un valor negativo o no numerico
 * se rechaza con un mensaje claro y no se escribe nada en la base de datos.
 */
public class FrmConfiguracion extends JFrame {

    private final Usuario operador;
    private final ConfiguracionService configuracionService = new ConfiguracionService();

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
    private final JButton btnActualizar = new JButton("Actualizar listado");
    private final JButton btnEditar = new JButton("Editar valor (seleccionado)");

    public FrmConfiguracion(Usuario operador) {
        this.operador = operador;
        setTitle("Configuración General (CU-18)");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        cargarConfiguracion();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        EstilosUI.estilizarVentana(panel);

        JPanel panelEncabezado = EstilosUI.crearEncabezado(
                "Configuración general del sistema",
                describirOperador());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JPanel panelSup = new JPanel(new GridBagLayout());
        EstilosUI.estilizarTarjeta(panelSup);

        JLabel lblSeccion = new JLabel("Parámetros globales");
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

        tablaConfiguracion.setAutoCreateRowSorter(false);
        tablaConfiguracion.getSelectionModel().addListSelectionListener(e -> actualizarSeleccion());

        JScrollPane scroll = new JScrollPane(tablaConfiguracion);
        scroll.setPreferredSize(new Dimension(900, 270));
        EstilosUI.estilizarTabla(tablaConfiguracion, scroll);
        TableColumnModel columnas = tablaConfiguracion.getColumnModel();
        if (columnas.getColumnCount() >= 3) {
            columnas.getColumn(0).setPreferredWidth(230);
            columnas.getColumn(1).setPreferredWidth(100);
            columnas.getColumn(1).setMaxWidth(130);
            columnas.getColumn(1).setCellRenderer(EstilosUI.crearRendererCebra(SwingConstants.CENTER));
            columnas.getColumn(2).setPreferredWidth(540);
        }

        EstilosUI.estilizarBotonNeutro(btnActualizar);
        btnActualizar.addActionListener(e -> cargarConfiguracion());

        EstilosUI.estilizarBotonSecundario(btnEditar);
        btnEditar.addActionListener(e -> editarValor());

        JButton btnVolver = new JButton("Volver al menú");
        EstilosUI.estilizarBotonNeutro(btnVolver);
        btnVolver.addActionListener(e -> dispose());

        JPanel panelAcciones = EstilosUI.crearBarraAcciones(
                new JComponent[]{btnActualizar, btnEditar},
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
        setMinimumSize(new Dimension(940, 540));
    }

    private String describirOperador() {
        if (!PoliticaPermisos.tienePermisoOperacion(operador, PoliticaPermisos.ModuloMenu.CONFIGURACION_GENERAL)) {
            return "Operador: sin autorización de Administrador.";
        }
        return "Operador: " + operador.getNombreUsuario() + " (" + operador.getRol().name() + ")";
    }

    private void cargarConfiguracion() {
        boolean autorizado = PoliticaPermisos.tienePermisoOperacion(
                operador, PoliticaPermisos.ModuloMenu.CONFIGURACION_GENERAL);
        btnActualizar.setEnabled(autorizado);
        btnEditar.setEnabled(autorizado);
        if (!autorizado) {
            valores.clear();
            descripciones.clear();
            claves.clear();
            modeloTabla.setRowCount(0);
            lblInfo.setText("Acceso denegado: requiere sesión de Administrador autenticada.");
            lblSeleccion.setText(" ");
            return;
        }
        try {
            Map<String, Integer> mapaValores = configuracionService.obtenerValores(operador);
            Map<String, String> mapaDescripciones = configuracionService.obtenerDescripciones(operador);

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
        } catch (TorneoExcepcion ex) {
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
        if (!PoliticaPermisos.tienePermisoOperacion(operador, PoliticaPermisos.ModuloMenu.CONFIGURACION_GENERAL)) {
            JOptionPane.showMessageDialog(this,
                    "ERROR DE SEGURIDAD: Solo un Administrador autenticado puede modificar la configuración general.",
                    "Configuración general", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String clave = obtenerClaveSeleccionada();
        if (clave == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un parámetro de la tabla.", "Configuración general",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Integer valorActual = valores.get(clave);
        JTextField txtValor = new JTextField(valorActual != null ? String.valueOf(valorActual) : "0", 8);
        EstilosUI.estilizarCampoTexto(txtValor, null);

        JPanel form = new JPanel(new GridBagLayout());
        EstilosUI.estilizarPanelModal(form);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel lblParam = new JLabel("Parámetro:");
        EstilosUI.estilizarEtiquetaCampo(lblParam);
        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(lblParam, gbc);

        JLabel lblClave = new JLabel(clave);
        EstilosUI.estilizarTextoPrimario(lblClave);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        form.add(lblClave, gbc);

        JLabel lblNuevo = new JLabel("Nuevo valor:");
        EstilosUI.estilizarEtiquetaCampo(lblNuevo);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        form.add(lblNuevo, gbc);
        gbc.gridx = 1;
        form.add(txtValor, gbc);

        JLabel lblAyuda = new JLabel("El valor debe ser un número entero mayor o igual a 0.");
        EstilosUI.estilizarEstadoInfo(lblAyuda);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        form.add(lblAyuda, gbc);

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
            configuracionService.actualizarParametro(clave, nuevoValor, operador);
            JOptionPane.showMessageDialog(this,
                    "ÉXITO: El parámetro \"" + clave + "\" quedó con valor " + nuevoValor + ".",
                    "Configuración general", JOptionPane.INFORMATION_MESSAGE);
            cargarConfiguracion();
        } catch (TorneoExcepcion ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo persistir el cambio del parámetro \"" + clave + "\". "
                            + ex.getMessage(),
                    "Configuración general", JOptionPane.ERROR_MESSAGE);
        }
    }
}
