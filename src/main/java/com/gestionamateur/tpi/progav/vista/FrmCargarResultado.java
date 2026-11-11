package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.controlador.GestorPartidos;
import com.gestionamateur.tpi.progav.dao.JugadorDAO;
import com.gestionamateur.tpi.progav.dao.PartidoDAO;
import com.gestionamateur.tpi.progav.dao.PartidoEquipoDAO;
import com.gestionamateur.tpi.progav.modelo.Cronologia;
import com.gestionamateur.tpi.progav.modelo.Equipo;
import com.gestionamateur.tpi.progav.modelo.Jugador;
import com.gestionamateur.tpi.progav.modelo.Partido;
import com.gestionamateur.tpi.progav.modelo.PartidoEquipo;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * CU-04: Carga del acta de un partido por el arbitro designado.
 * Permite registrar goles por jugador, tarjetas e incidencias, y oficializa el
 * resultado mediante el protocolo transaccional de cierre dePartidoService.
 */
public class FrmCargarResultado extends JFrame {

    private static final SimpleDateFormat FORMATO_FECHA = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private final Usuario operador;
    private final GestorPartidos gestorPartidos = new GestorPartidos();
    private final PartidoDAO partidoDAO = new PartidoDAO();
    private final PartidoEquipoDAO partidoEquipoDAO = new PartidoEquipoDAO();
    private final JugadorDAO jugadorDAO = new JugadorDAO();

    private List<Partido> partidosAsignados = new ArrayList<>();
    private final List<Cronologia> incidencias = new ArrayList<>();
    private final List<Jugador> jugadoresLocal = new ArrayList<>();
    private final List<Jugador> jugadoresVisitante = new ArrayList<>();
    private Partido partidoSeleccionado;

    private final JComboBox<String> cmbPartido = new JComboBox<>();
    private final JComboBox<String> cmbJugador = new JComboBox<>();
    private final JComboBox<String> cmbIncidencia = new JComboBox<>();
    private final JComboBox<String> cmbTipoExpulsion = new JComboBox<>();
    private final JTextField txtMinuto = new JTextField("1", 5);
    private final JTextField txtObservacion = new JTextField(22);
    private final JTextField txtFinReal = new JTextField(FORMATO_FECHA.format(new Date()), 16);
    private final JLabel lblMarcador = new JLabel("Marcador: -");
    private final JLabel lblDetalle = new JLabel(" ");

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"Min.", "Incidencia", "Jugador", "Observación"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tablaIncidencias = new JTable(modeloTabla);

    public FrmCargarResultado(Usuario operador) {
        this.operador = operador;
        setTitle("Carga de Acta de Partido (CU-04)");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        cargarPartidosAsignados();
        cargarTipos();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panelPrincipal.setBackground(Color.WHITE);

        // --- Encabezado ---
        JPanel panelEncabezado = new JPanel(new GridBagLayout());
        panelEncabezado.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel titulo = new JLabel("Acta de partido");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(new Color(0x12, 0x38, 0x5C));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        panelEncabezado.add(titulo, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panelEncabezado.add(new JLabel("Partido asignado:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        cmbPartido.setPreferredSize(new Dimension(320, 26));
        panelEncabezado.add(cmbPartido, gbc);
        gbc.gridx = 3;
        gbc.gridwidth = 1;
        lblDetalle.setForeground(new Color(0x3A, 0x55, 0x68));
        panelEncabezado.add(lblDetalle, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panelEncabezado.add(new JLabel("Hora real de fin:"), gbc);
        gbc.gridx = 1;
        panelEncabezado.add(txtFinReal, gbc);
        gbc.gridx = 2;
        lblMarcador.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblMarcador.setForeground(new Color(0x12, 0x38, 0x5C));
        panelEncabezado.add(lblMarcador, gbc);

        // --- Alta de incidencia ---
        JPanel panelAlta = new JPanel(new GridBagLayout());
        panelAlta.setBackground(new Color(0xF4, 0xF8, 0xFB));
        panelAlta.setBorder(BorderFactory.createTitledBorder("Registrar incidencia"));

        gbc.gridx = 0;
        gbc.gridy = 0;
        panelAlta.add(new JLabel("Jugador:"), gbc);
        gbc.gridx = 1;
        cmbJugador.setPreferredSize(new Dimension(230, 24));
        panelAlta.add(cmbJugador, gbc);
        gbc.gridx = 2;
        panelAlta.add(new JLabel("Incidencia:"), gbc);
        gbc.gridx = 3;
        cmbIncidencia.setPreferredSize(new Dimension(150, 24));
        panelAlta.add(cmbIncidencia, gbc);
        gbc.gridx = 4;
        panelAlta.add(new JLabel("Minuto:"), gbc);
        gbc.gridx = 5;
        panelAlta.add(txtMinuto, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panelAlta.add(new JLabel("Tipo de expulsión:"), gbc);
        gbc.gridx = 1;
        cmbTipoExpulsion.setPreferredSize(new Dimension(150, 24));
        panelAlta.add(cmbTipoExpulsion, gbc);
        gbc.gridx = 2;
        panelAlta.add(new JLabel("Observación:"), gbc);
        gbc.gridx = 3;
        gbc.gridwidth = 2;
        panelAlta.add(txtObservacion, gbc);
        gbc.gridx = 5;
        gbc.gridwidth = 1;

        JButton btnAgregar = new JButton("Agregar");
        btnAgregar.addActionListener(e -> agregarIncidencia());
        gbc.gridy = 2;
        panelAlta.add(btnAgregar, gbc);

        JButton btnQuitar = new JButton("Quitar seleccionada");
        btnQuitar.addActionListener(e -> quitarIncidencia());
        gbc.gridx = 4;
        panelAlta.add(btnQuitar, gbc);

        // --- Tabla de incidencias ---
        JScrollPane scroll = new JScrollPane(tablaIncidencias);
        scroll.setPreferredSize(new Dimension(760, 220));

        // --- Acciones ---
        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        JButton btnCerrarActa = new JButton("Oficializar acta");
        btnCerrarActa.addActionListener(e -> oficializarActa());
        JButton btnVolver = new JButton("Volver al menú");
        btnVolver.addActionListener(e -> dispose());
        panelAcciones.add(btnCerrarActa);
        panelAcciones.add(btnVolver);

        JPanel panelCentro = new JPanel(new BorderLayout(6, 6));
        panelCentro.setBackground(Color.WHITE);
        panelCentro.add(panelAlta, BorderLayout.NORTH);
        panelCentro.add(scroll, BorderLayout.CENTER);

        panelPrincipal.add(panelEncabezado, BorderLayout.NORTH);
        panelPrincipal.add(panelCentro, BorderLayout.CENTER);
        panelPrincipal.add(panelAcciones, BorderLayout.SOUTH);

        cmbPartido.addActionListener(e -> seleccionarPartido());
        cmbIncidencia.addActionListener(e -> actualizarVisibilidadExpulsion());

        setContentPane(panelPrincipal);
        pack();
        setMinimumSize(new Dimension(820, 560));
    }

    private void cargarTipos() {
        cmbIncidencia.addItem(TiposDatos.TipoAccion.GOL.name());
        cmbIncidencia.addItem(TiposDatos.TipoAccion.TARJETA_AMARILLA.name());
        cmbIncidencia.addItem(TiposDatos.TipoAccion.TARJETA_ROJA.name());
        cmbTipoExpulsion.addItem(TiposDatos.TipoExpulsion.DIRECTA.name());
        cmbTipoExpulsion.addItem(TiposDatos.TipoExpulsion.DOBLE_AMARILLA.name());
        cmbTipoExpulsion.setEnabled(false);
    }

    /** CU-04: solo se ofrecen los partidos asignados al arbitro autenticado. */
    private void cargarPartidosAsignados() {
        partidosAsignados = new ArrayList<>();
        try {
            for (Partido p : partidoDAO.buscarPendientesDeFinalizacion()) {
                if (p.getArbitroDesignado() == null || p.getArbitroDesignado().getId() == null) {
                    continue;
                }
                if (operador != null && operador.getId() != null
                        && !p.getArbitroDesignado().getId().equals(operador.getId())) {
                    continue;
                }
                partidosAsignados.add(p);
            }
        } catch (Exception e) {
            lblDetalle.setText("No se pudieron consultar los partidos asignados.");
        }

        DefaultComboBoxModel<String> modelo = new DefaultComboBoxModel<>();
        for (Partido p : partidosAsignados) {
            modelo.addElement("#" + p.getIdPartido() + " - " + FORMATO_FECHA.format(p.getFechaHora())
                    + " - " + p.getEstadoPartido());
        }
        cmbPartido.setModel(modelo);
        if (!partidosAsignados.isEmpty()) {
            seleccionarPartido();
        } else {
            lblDetalle.setText("No hay partidos pendientes asignados a este arbitro.");
        }
    }

    private void seleccionarPartido() {
        int idx = cmbPartido.getSelectedIndex();
        if (idx < 0 || idx >= partidosAsignados.size()) {
            partidoSeleccionado = null;
            return;
        }
        partidoSeleccionado = partidosAsignados.get(idx);
        incidencias.clear();
        modeloTabla.setRowCount(0);
        jugadoresLocal.clear();
        jugadoresVisitante.clear();

        try {
            for (PartidoEquipo pe : partidoEquipoDAO.buscarPorPartido(partidoSeleccionado.getIdPartido())) {
                if (pe.getEquipo() == null || pe.getEquipo().getIdEquipo() == null) {
                    continue;
                }
                List<Jugador> jugadores = jugadorDAO.buscarPorEquipo(pe.getEquipo().getIdEquipo());
                if (pe.isEsLocal()) {
                    jugadoresLocal.addAll(jugadores);
                } else {
                    jugadoresVisitante.addAll(jugadores);
                }
            }
        } catch (Exception e) {
            lblDetalle.setText("No se pudieron cargar los jugadores del encuentro.");
        }

        DefaultComboBoxModel<String> modelo = new DefaultComboBoxModel<>();
        for (Jugador j : jugadoresLocal) {
            modelo.addElement(j.getNombre() + " (" + j.getPosicion() + ") [LOCAL]");
        }
        for (Jugador j : jugadoresVisitante) {
            modelo.addElement(j.getNombre() + " (" + j.getPosicion() + ") [VISITANTE]");
        }
        cmbJugador.setModel(modelo);
        actualizarMarcador();
    }

    private void actualizarVisibilidadExpulsion() {
        boolean esRoja = TiposDatos.TipoAccion.TARJETA_ROJA.name().equals(cmbIncidencia.getSelectedItem());
        cmbTipoExpulsion.setEnabled(esRoja);
    }

    private void agregarIncidencia() {
        if (partidoSeleccionado == null) {
            JOptionPane.showMessageDialog(this, "Seleccione el partido a cargar.", "Acta", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int idxJugador = cmbJugador.getSelectedIndex();
        List<Jugador> todos = new ArrayList<>(jugadoresLocal);
        todos.addAll(jugadoresVisitante);
        if (idxJugador < 0 || idxJugador >= todos.size()) {
            JOptionPane.showMessageDialog(this, "Seleccione el jugador responsable de la incidencia.", "Acta", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Jugador jugador = todos.get(idxJugador);

        int minuto;
        try {
            minuto = Integer.parseInt(txtMinuto.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El minuto debe ser un número entero.", "Acta", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (minuto < 0) {
            JOptionPane.showMessageDialog(this, "El minuto no puede ser negativo.", "Acta", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String accionNombre = (String) cmbIncidencia.getSelectedItem();
        TiposDatos.TipoAccion accion = TiposDatos.TipoAccion.valueOf(accionNombre);
        String tipoExpulsion = null;
        if (accion == TiposDatos.TipoAccion.TARJETA_ROJA) {
            tipoExpulsion = (String) cmbTipoExpulsion.getSelectedItem();
        }

        try {
            Cronologia c = new Cronologia(minuto, accion, tipoExpulsion, jugador, partidoSeleccionado,
                    txtObservacion.getText() == null ? null : txtObservacion.getText().trim());
            incidencias.add(c);
            modeloTabla.addRow(new Object[]{minuto, accion.toString(), jugador.getNombre(),
                    c.getObservacion() == null ? "" : c.getObservacion()});
            txtObservacion.setText("");
            actualizarMarcador();
        } catch (com.gestionamateur.tpi.progav.modelo.TorneoExcepcion ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Incidencia inválida", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void quitarIncidencia() {
        int fila = tablaIncidencias.getSelectedRow();
        if (fila < 0) {
            return;
        }
        incidencias.remove(fila);
        modeloTabla.removeRow(fila);
        actualizarMarcador();
    }

    /** El marcador se calcula a partir de los goles registrados en la cronología del acta. */
    private void actualizarMarcador() {
        int local = 0;
        int visitante = 0;
        Long idLocal = null;
        Long idVisitante = null;
        try {
            for (PartidoEquipo pe : partidoEquipoDAO.buscarPorPartido(partidoSeleccionado.getIdPartido())) {
                if (pe.getEquipo() == null || pe.getEquipo().getIdEquipo() == null) {
                    continue;
                }
                if (pe.isEsLocal()) {
                    idLocal = pe.getEquipo().getIdEquipo();
                } else {
                    idVisitante = pe.getEquipo().getIdEquipo();
                }
            }
        } catch (Exception e) {
            // El marcador queda sin calcular si no se puede leer el encuentro.
        }
        for (Cronologia c : incidencias) {
            if (c.getAccion() != TiposDatos.TipoAccion.GOL || c.getAutor() == null) {
                continue;
            }
            Long idEquipo = c.getAutor().getIdEquipo();
            if (idEquipo != null && idEquipo.equals(idLocal)) {
                local++;
            } else if (idEquipo != null && idEquipo.equals(idVisitante)) {
                visitante++;
            }
        }
        lblMarcador.setText("Marcador: " + local + " - " + visitante);
    }

    private void oficializarActa() {
        if (partidoSeleccionado == null) {
            JOptionPane.showMessageDialog(this, "Seleccione el partido a oficializar.", "Acta", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Date finReal;
        try {
            FORMATO_FECHA.setLenient(false);
            finReal = FORMATO_FECHA.parse(txtFinReal.getText().trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "La hora real de finalización debe tener el formato dd/MM/yyyy HH:mm.",
                    "Acta", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String respuesta = gestorPartidos.cerrarActa(partidoSeleccionado.getIdPartido(), finReal, incidencias, operador);
        if (respuesta.startsWith("ÉXITO")) {
            JOptionPane.showMessageDialog(this, respuesta, "Acta", JOptionPane.INFORMATION_MESSAGE);
            cargarPartidosAsignados();
        } else {
            JOptionPane.showMessageDialog(this, respuesta, "No se pudo oficializar el acta", JOptionPane.ERROR_MESSAGE);
        }
    }
}
