package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.controlador.GestorReclamos;
import com.gestionamateur.tpi.progav.modelo.Reclamo;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.seguridad.PoliticaPermisos;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

/**
 * CU-14: bandeja de reclamos formales y resolucion por el organizador.
 */
public class FrmReclamos extends JFrame {

    private static final SimpleDateFormat FORMATO_FECHA = new SimpleDateFormat("dd/MM/yyyy");

    private final Usuario operador;
    private final GestorReclamos gestorReclamos = new GestorReclamos();

    private List<Reclamo> reclamos = new ArrayList<>();
    private final JComboBox<String> cmbVeredicto = new JComboBox<>();
    private final JTextArea txtJustificacion = new JTextArea(6, 42);
    private final JLabel lblInfo = new JLabel(" ");
    private final JButton btnResolver = new JButton("Resolver reclamo (seleccionado)");

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Fecha", "Motivo", "Partido", "Capitán", "Sanción apelada"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tablaReclamos = new JTable(modeloTabla);

    public FrmReclamos(Usuario operador) {
        this.operador = operador;
        setTitle("Reclamos Pendientes (CU-14)");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        cargarReclamos();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        EstilosUI.estilizarVentana(panel);

        JPanel panelEncabezado = EstilosUI.crearEncabezado(
                "Reclamos formales",
                "Bandeja de reclamos pendientes y dictamen fundamentado del organizador (CU-14)");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 10);
        gbc.anchor = GridBagConstraints.WEST;

        JPanel panelSup = new JPanel(new GridBagLayout());
        EstilosUI.estilizarTarjeta(panelSup);

        JLabel lblSeccion = new JLabel("Resolución del reclamo");
        EstilosUI.estilizarTituloSeccion(lblSeccion);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        panelSup.add(lblSeccion, gbc);

        JLabel lblVeredicto = new JLabel("Veredicto:");
        EstilosUI.estilizarEtiquetaCampo(lblVeredicto);
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panelSup.add(lblVeredicto, gbc);

        cmbVeredicto.addItem("A FAVOR");
        cmbVeredicto.addItem("EN CONTRA");
        EstilosUI.estilizarCombo(cmbVeredicto);
        gbc.gridx = 1;
        panelSup.add(cmbVeredicto, gbc);

        JLabel lblFundamento = new JLabel("Fundamentación (mínimo 15 caracteres):");
        EstilosUI.estilizarEtiquetaCampo(lblFundamento);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panelSup.add(lblFundamento, gbc);

        txtJustificacion.setLineWrap(true);
        txtJustificacion.setWrapStyleWord(true);
        EstilosUI.estilizarAreaTexto(txtJustificacion);
        txtJustificacion.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JScrollPane scrollJustificacion = new JScrollPane(
                txtJustificacion,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollJustificacion.setBorder(BorderFactory.createLineBorder(EstilosUI.BORDER_DEFAULT, 1));
        scrollJustificacion.getViewport().setBackground(EstilosUI.BG_CONTROL);
        scrollJustificacion.setPreferredSize(new Dimension(520, 105));
        scrollJustificacion.setMinimumSize(new Dimension(320, 90));
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        panelSup.add(scrollJustificacion, gbc);

        EstilosUI.estilizarEstadoInfo(lblInfo);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        panelSup.add(lblInfo, gbc);

        JScrollPane scroll = new JScrollPane(tablaReclamos);
        scroll.setPreferredSize(new Dimension(900, 220));
        EstilosUI.estilizarTabla(tablaReclamos, scroll);
        TableColumnModel columnas = tablaReclamos.getColumnModel();
        if (columnas.getColumnCount() >= 6) {
            columnas.getColumn(0).setPreferredWidth(55);
            columnas.getColumn(0).setMaxWidth(75);
            columnas.getColumn(0).setCellRenderer(EstilosUI.crearRendererCebra(SwingConstants.CENTER));
        }

        EstilosUI.estilizarBotonPrimario(btnResolver);
        btnResolver.addActionListener(e -> resolver());

        JButton btnVolver = new JButton("Volver al menú");
        EstilosUI.estilizarBotonNeutro(btnVolver);
        btnVolver.addActionListener(e -> dispose());

        JPanel panelAcciones = EstilosUI.crearBarraAcciones(
                new JComponent[]{btnResolver},
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
        setMinimumSize(new Dimension(960, 620));
    }

    private void cargarReclamos() {
        boolean autorizado = PoliticaPermisos.tienePermisoOperacion(
                operador, PoliticaPermisos.ModuloMenu.RESOLVER_RECLAMOS);
        btnResolver.setEnabled(autorizado);
        cmbVeredicto.setEnabled(autorizado);
        if (!autorizado) {
            reclamos = new ArrayList<>();
            modeloTabla.setRowCount(0);
            lblInfo.setText("Acceso denegado: requiere sesión de Organizador o Administrador.");
            return;
        }
        reclamos = gestorReclamos.listarPendientes(operador);
        modeloTabla.setRowCount(0);
        for (Reclamo r : reclamos) {
            modeloTabla.addRow(new Object[]{
                    r.getId(),
                    r.getFechaInicio() == null ? "" : FORMATO_FECHA.format(r.getFechaInicio()),
                    r.getMotivo(),
                    r.getPartido() == null ? "(sin partido)" : "#" + r.getPartido().getIdPartido(),
                    r.getCapitan() == null ? "(sin capitán)" : r.getCapitan().getNombreUsuario(),
                    r.getSancionApelada() == null ? "(sin sanción)" : "#" + r.getSancionApelada().getId()});
        }
        if (reclamos.isEmpty()) {
            lblInfo.setText("No hay reclamos pendientes de resolución en este momento.");
        } else {
            lblInfo.setText("Seleccione un reclamo de la tabla para resolverlo.");
        }
    }

    private void resolver() {
        int fila = tablaReclamos.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione el reclamo a resolver.", "Reclamos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long idReclamo = (Long) modeloTabla.getValueAt(fila, 0);
        String veredicto = (String) cmbVeredicto.getSelectedItem();
        String justificacion = txtJustificacion.getText() == null ? "" : txtJustificacion.getText().trim();
        if (justificacion.length() < 15) {
            JOptionPane.showMessageDialog(this, "La fundamentación del veredicto debe tener al menos 15 caracteres.",
                    "Reclamos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String respuesta = gestorReclamos.resolver(idReclamo, veredicto, justificacion, operador);
        if (respuesta.startsWith("ÉXITO")) {
            JOptionPane.showMessageDialog(this, respuesta, "Reclamos", JOptionPane.INFORMATION_MESSAGE);
            txtJustificacion.setText("");
            cargarReclamos();
        } else {
            JOptionPane.showMessageDialog(this, respuesta, "No se pudo resolver el reclamo", JOptionPane.ERROR_MESSAGE);
        }
    }
}
