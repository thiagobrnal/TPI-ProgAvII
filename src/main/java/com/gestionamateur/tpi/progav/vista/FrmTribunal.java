package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.controlador.GestorDisciplina;
import com.gestionamateur.tpi.progav.modelo.Jugador;
import com.gestionamateur.tpi.progav.modelo.Sancion;
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

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
 * CU-12: tribunal disciplinario. Permite emitir el fallo sobre las sanciones pendientes,
 * anular sanciones y registrar sanciones administrativas manuales.
 */
public class FrmTribunal extends JFrame {

    private static final SimpleDateFormat FORMATO_FECHA = new SimpleDateFormat("dd/MM/yyyy");

    private final Usuario operador;
    private final GestorDisciplina gestorDisciplina = new GestorDisciplina();

    private List<Sancion> sanciones = new ArrayList<>();
    private List<Jugador> jugadores = new ArrayList<>();

    private final JComboBox<String> cmbJugador = new JComboBox<>();
    private final JTextField txtPartidos = new JTextField("1", 4);
    private final JTextField txtMotivo = new JTextField(34);
    private final JTextField txtGravedad = new JTextField(16);
    private final JLabel lblInfo = new JLabel(" ");
    private final JButton btnEmitir = new JButton("Emitir fallo (sancion seleccionada)");
    private final JButton btnAnular = new JButton("Anular sancion (seleccionada)");
    private final JButton btnRegistrar = new JButton("Registrar sancion administrativa");

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Jugador", "Origen", "Motivo", "Gravedad", "Fecha"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tablaSanciones = new JTable(modeloTabla);

    public FrmTribunal(Usuario operador) {
        this.operador = operador;
        setTitle("Tribunal Disciplinario (CU-12)");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        cargarDatos();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        EstilosUI.estilizarVentana(panel);

        JPanel panelEncabezado = EstilosUI.crearEncabezado(
                "Tribunal disciplinario",
                "Resolución de sanciones pendientes, anulación y sanciones administrativas (CU-12)");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 10);
        gbc.anchor = GridBagConstraints.WEST;

        JPanel panelSup = new JPanel(new GridBagLayout());
        EstilosUI.estilizarTarjeta(panelSup);

        JLabel lblSeccion = new JLabel("Fallo o sanción disciplinaria");
        EstilosUI.estilizarTituloSeccion(lblSeccion);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        panelSup.add(lblSeccion, gbc);

        JLabel lblJugador = new JLabel("Jugador:");
        EstilosUI.estilizarEtiquetaCampo(lblJugador);
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panelSup.add(lblJugador, gbc);

        EstilosUI.estilizarCombo(cmbJugador);
        cmbJugador.setPreferredSize(new Dimension(260, 30));
        gbc.gridx = 1;
        gbc.weightx = 0.6;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelSup.add(cmbJugador, gbc);

        JLabel lblFechas = new JLabel("Fechas de suspensión:");
        EstilosUI.estilizarEtiquetaCampo(lblFechas);
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panelSup.add(lblFechas, gbc);

        EstilosUI.estilizarCampoTexto(txtPartidos, "1");
        gbc.gridx = 3;
        gbc.weightx = 0.2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelSup.add(txtPartidos, gbc);

        JLabel lblMotivo = new JLabel("Motivo:");
        EstilosUI.estilizarEtiquetaCampo(lblMotivo);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panelSup.add(lblMotivo, gbc);

        EstilosUI.estilizarCampoTexto(txtMotivo, "Detalle o fundamento disciplinario");
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelSup.add(txtMotivo, gbc);

        JLabel lblGravedad = new JLabel("Gravedad:");
        EstilosUI.estilizarEtiquetaCampo(lblGravedad);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panelSup.add(lblGravedad, gbc);

        EstilosUI.estilizarCampoTexto(txtGravedad, "Ej: LEVE / GRAVE");
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelSup.add(txtGravedad, gbc);

        EstilosUI.estilizarEstadoInfo(lblInfo);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 4;
        panelSup.add(lblInfo, gbc);

        JScrollPane scroll = new JScrollPane(tablaSanciones);
        scroll.setPreferredSize(new Dimension(900, 240));
        EstilosUI.estilizarTabla(tablaSanciones, scroll);
        TableColumnModel columnas = tablaSanciones.getColumnModel();
        if (columnas.getColumnCount() >= 6) {
            columnas.getColumn(0).setPreferredWidth(55);
            columnas.getColumn(0).setMaxWidth(75);
            columnas.getColumn(0).setCellRenderer(EstilosUI.crearRendererCebra(SwingConstants.CENTER));
        }

        EstilosUI.estilizarBotonPrimario(btnEmitir);
        btnEmitir.addActionListener(e -> emitirFallo());

        EstilosUI.estilizarBotonPeligro(btnAnular);
        btnAnular.addActionListener(e -> anular());

        EstilosUI.estilizarBotonAdvertencia(btnRegistrar);
        btnRegistrar.addActionListener(e -> registrarAdministrativa());

        JButton btnVolver = new JButton("Volver al menú");
        EstilosUI.estilizarBotonNeutro(btnVolver);
        btnVolver.addActionListener(e -> dispose());

        JPanel panelAcciones = EstilosUI.crearBarraAcciones(
                new JComponent[]{btnEmitir, btnAnular, btnRegistrar},
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

    private void cargarDatos() {
        boolean autorizado = PoliticaPermisos.tienePermisoOperacion(
                operador, PoliticaPermisos.ModuloMenu.TRIBUNAL_DISCIPLINARIO);
        btnEmitir.setEnabled(autorizado);
        btnAnular.setEnabled(autorizado);
        btnRegistrar.setEnabled(autorizado);
        cmbJugador.setEnabled(autorizado);
        if (!autorizado) {
            sanciones = new ArrayList<>();
            jugadores = new ArrayList<>();
            modeloTabla.setRowCount(0);
            cmbJugador.setModel(new DefaultComboBoxModel<>());
            lblInfo.setText("Acceso denegado: requiere sesión de Organizador o Administrador.");
            return;
        }
        sanciones = gestorDisciplina.listarPendientes(operador);
        modeloTabla.setRowCount(0);
        for (Sancion s : sanciones) {
            modeloTabla.addRow(new Object[]{
                    s.getId(),
                    s.getJugadorSancionado() == null ? "(desconocido)" : s.getJugadorSancionado().getNombre(),
                    s.getTipoOrigen(),
                    s.getMotivo(),
                    s.getGravedad(),
                    s.getFechaInicio() == null ? "" : FORMATO_FECHA.format(s.getFechaInicio())});
        }
        if (sanciones.isEmpty()) {
            lblInfo.setText("No hay sanciones pendientes de fallo en este momento.");
        } else {
            lblInfo.setText("Seleccione una fila de la tabla para emitir el fallo o anularla.");
        }

        jugadores = gestorDisciplina.listarJugadores(operador);
        DefaultComboBoxModel<String> modelo = new DefaultComboBoxModel<>();
        for (Jugador j : jugadores) {
            modelo.addElement(j.getNombre() + " (DNI " + j.getDni() + ")");
        }
        cmbJugador.setModel(modelo);
    }

    private int leerPartidos() {
        try {
            return Integer.parseInt(txtPartidos.getText().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void emitirFallo() {
        int fila = tablaSanciones.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione la sanción a resolver.", "Tribunal", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long idSancion = (Long) modeloTabla.getValueAt(fila, 0);
        int partidos = leerPartidos();
        String motivo = txtMotivo.getText() == null ? "" : txtMotivo.getText().trim();
        String gravedad = txtGravedad.getText() == null ? "" : txtGravedad.getText().trim();

        String respuesta = gestorDisciplina.emitirFallo(idSancion, partidos, motivo, gravedad, operador);
        mostrar(respuesta);
    }

    private void anular() {
        int fila = tablaSanciones.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione la sanción a anular.", "Tribunal", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long idSancion = (Long) modeloTabla.getValueAt(fila, 0);
        String motivo = txtMotivo.getText() == null ? "" : txtMotivo.getText().trim();
        if (motivo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Indique el motivo de la anulación.", "Tribunal", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String respuesta = gestorDisciplina.anular(idSancion, motivo, operador);
        mostrar(respuesta);
    }

    private void registrarAdministrativa() {
        int idx = cmbJugador.getSelectedIndex();
        if (idx < 0 || idx >= jugadores.size()) {
            JOptionPane.showMessageDialog(this, "Seleccione el jugador a sancionar.", "Tribunal", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int partidos = leerPartidos();
        String motivo = txtMotivo.getText() == null ? "" : txtMotivo.getText().trim();
        String gravedad = txtGravedad.getText() == null ? "" : txtGravedad.getText().trim();

        String respuesta = gestorDisciplina.registrarSancionAdministrativa(
                jugadores.get(idx).getId(), partidos, motivo, gravedad, operador);
        mostrar(respuesta);
    }

    private void mostrar(String respuesta) {
        if (respuesta.startsWith("ÉXITO")) {
            JOptionPane.showMessageDialog(this, respuesta, "Tribunal", JOptionPane.INFORMATION_MESSAGE);
            cargarDatos();
        } else {
            JOptionPane.showMessageDialog(this, respuesta, "No se pudo completar la operación", JOptionPane.ERROR_MESSAGE);
        }
    }
}
