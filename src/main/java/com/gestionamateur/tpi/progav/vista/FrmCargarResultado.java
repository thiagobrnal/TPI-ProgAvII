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
import com.gestionamateur.tpi.progav.seguridad.PoliticaPermisos;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
    private final JButton btnAgregar = new JButton("Agregar");
    private final JButton btnQuitar = new JButton("Quitar seleccionada");
    private final JButton btnCerrarActa = new JButton("Oficializar acta");

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
        JPanel panelPrincipal = new JPanel(new BorderLayout(12, 12));
        EstilosUI.estilizarVentana(panelPrincipal);

        JPanel panelTitulo = EstilosUI.crearEncabezado(
                "Acta de partido",
                "Carga de goles, tarjetas, incidencias y cierre oficial del encuentro (CU-04)");

        // --- Datos del partido ---
        JPanel panelEncabezado = new JPanel(new GridBagLayout());
        EstilosUI.estilizarTarjeta(panelEncabezado);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel lblPartido = new JLabel("Partido asignado:");
        EstilosUI.estilizarEtiquetaCampo(lblPartido);
        gbc.gridx = 0;
        gbc.gridy = 0;
        panelEncabezado.add(lblPartido, gbc);

        EstilosUI.estilizarCombo(cmbPartido);
        cmbPartido.setPreferredSize(new Dimension(340, 30));
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 0.6;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelEncabezado.add(cmbPartido, gbc);

        EstilosUI.estilizarEstadoInfo(lblDetalle);
        gbc.gridx = 3;
        gbc.gridwidth = 1;
        gbc.weightx = 0.4;
        panelEncabezado.add(lblDetalle, gbc);

        JLabel lblFinReal = new JLabel("Hora real de fin:");
        EstilosUI.estilizarEtiquetaCampo(lblFinReal);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panelEncabezado.add(lblFinReal, gbc);

        EstilosUI.estilizarCampoTexto(txtFinReal, "dd/MM/yyyy HH:mm");
        gbc.gridx = 1;
        gbc.weightx = 0.4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelEncabezado.add(txtFinReal, gbc);

        EstilosUI.estilizarIndicadorAcento(lblMarcador);
        gbc.gridx = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 0.6;
        panelEncabezado.add(lblMarcador, gbc);

        // --- Alta de incidencia ---
        JPanel panelAlta = new JPanel(new GridBagLayout());
        EstilosUI.estilizarTarjetaConTitulo(panelAlta, "Registrar incidencia");
        GridBagConstraints gbcAlta = new GridBagConstraints();
        gbcAlta.insets = new Insets(5, 6, 5, 8);
        gbcAlta.anchor = GridBagConstraints.WEST;

        JLabel lblJugador = new JLabel("Jugador:");
        EstilosUI.estilizarEtiquetaCampo(lblJugador);
        gbcAlta.gridx = 0;
        gbcAlta.gridy = 0;
        panelAlta.add(lblJugador, gbcAlta);

        EstilosUI.estilizarCombo(cmbJugador);
        cmbJugador.setPreferredSize(new Dimension(230, 30));
        gbcAlta.gridx = 1;
        gbcAlta.weightx = 0.45;
        gbcAlta.fill = GridBagConstraints.HORIZONTAL;
        panelAlta.add(cmbJugador, gbcAlta);

        JLabel lblIncidencia = new JLabel("Incidencia:");
        EstilosUI.estilizarEtiquetaCampo(lblIncidencia);
        gbcAlta.gridx = 2;
        gbcAlta.weightx = 0;
        gbcAlta.fill = GridBagConstraints.NONE;
        panelAlta.add(lblIncidencia, gbcAlta);

        EstilosUI.estilizarCombo(cmbIncidencia);
        cmbIncidencia.setPreferredSize(new Dimension(170, 30));
        gbcAlta.gridx = 3;
        gbcAlta.weightx = 0.35;
        gbcAlta.fill = GridBagConstraints.HORIZONTAL;
        panelAlta.add(cmbIncidencia, gbcAlta);

        JLabel lblMinuto = new JLabel("Minuto:");
        EstilosUI.estilizarEtiquetaCampo(lblMinuto);
        gbcAlta.gridx = 4;
        gbcAlta.weightx = 0;
        gbcAlta.fill = GridBagConstraints.NONE;
        panelAlta.add(lblMinuto, gbcAlta);

        EstilosUI.estilizarCampoTexto(txtMinuto, "1");
        gbcAlta.gridx = 5;
        gbcAlta.weightx = 0.2;
        gbcAlta.fill = GridBagConstraints.HORIZONTAL;
        panelAlta.add(txtMinuto, gbcAlta);

        JLabel lblExpulsion = new JLabel("Tipo de expulsión:");
        EstilosUI.estilizarEtiquetaCampo(lblExpulsion);
        gbcAlta.gridx = 0;
        gbcAlta.gridy = 1;
        gbcAlta.weightx = 0;
        gbcAlta.fill = GridBagConstraints.NONE;
        panelAlta.add(lblExpulsion, gbcAlta);

        EstilosUI.estilizarCombo(cmbTipoExpulsion);
        cmbTipoExpulsion.setPreferredSize(new Dimension(170, 30));
        gbcAlta.gridx = 1;
        gbcAlta.weightx = 0.45;
        gbcAlta.fill = GridBagConstraints.HORIZONTAL;
        panelAlta.add(cmbTipoExpulsion, gbcAlta);

        JLabel lblObs = new JLabel("Observación:");
        EstilosUI.estilizarEtiquetaCampo(lblObs);
        gbcAlta.gridx = 2;
        gbcAlta.weightx = 0;
        gbcAlta.fill = GridBagConstraints.NONE;
        panelAlta.add(lblObs, gbcAlta);

        EstilosUI.estilizarCampoTexto(txtObservacion, "Detalle opcional de la jugada");
        gbcAlta.gridx = 3;
        gbcAlta.gridwidth = 3;
        gbcAlta.weightx = 0.55;
        gbcAlta.fill = GridBagConstraints.HORIZONTAL;
        panelAlta.add(txtObservacion, gbcAlta);

        EstilosUI.estilizarBotonSecundario(btnAgregar);
        btnAgregar.addActionListener(e -> agregarIncidencia());

        EstilosUI.estilizarBotonAdvertencia(btnQuitar);
        btnQuitar.addActionListener(e -> quitarIncidencia());

        JPanel panelBotonesAlta = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        panelBotonesAlta.setBackground(EstilosUI.BG_SURFACE);
        panelBotonesAlta.add(btnAgregar);
        panelBotonesAlta.add(btnQuitar);
        gbcAlta.gridx = 0;
        gbcAlta.gridy = 2;
        gbcAlta.gridwidth = 6;
        gbcAlta.weightx = 1.0;
        gbcAlta.fill = GridBagConstraints.HORIZONTAL;
        panelAlta.add(panelBotonesAlta, gbcAlta);

        // --- Tabla de incidencias ---
        JScrollPane scroll = new JScrollPane(tablaIncidencias);
        scroll.setPreferredSize(new Dimension(820, 200));
        EstilosUI.estilizarTabla(tablaIncidencias, scroll);
        TableColumnModel columnas = tablaIncidencias.getColumnModel();
        if (columnas.getColumnCount() >= 4) {
            columnas.getColumn(0).setPreferredWidth(65);
            columnas.getColumn(0).setMaxWidth(90);
            columnas.getColumn(0).setCellRenderer(EstilosUI.crearRendererCebra(SwingConstants.CENTER));
        }

        // --- Acciones ---
        EstilosUI.estilizarBotonPrimario(btnCerrarActa);
        btnCerrarActa.addActionListener(e -> oficializarActa());

        JButton btnVolver = new JButton("Volver al menú");
        EstilosUI.estilizarBotonNeutro(btnVolver);
        btnVolver.addActionListener(e -> dispose());

        JPanel panelAcciones = EstilosUI.crearBarraAcciones(
                new JComponent[]{btnCerrarActa},
                new JComponent[]{btnVolver});

        JPanel panelCentro = new JPanel(new BorderLayout(12, 12));
        panelCentro.setBackground(EstilosUI.BG_CANVAS);
        panelCentro.add(panelEncabezado, BorderLayout.NORTH);

        JPanel panelCuerpo = new JPanel(new BorderLayout(12, 12));
        panelCuerpo.setBackground(EstilosUI.BG_CANVAS);
        panelCuerpo.add(panelAlta, BorderLayout.NORTH);
        panelCuerpo.add(scroll, BorderLayout.CENTER);
        panelCentro.add(panelCuerpo, BorderLayout.CENTER);

        panelPrincipal.add(panelTitulo, BorderLayout.NORTH);
        panelPrincipal.add(panelCentro, BorderLayout.CENTER);
        panelPrincipal.add(panelAcciones, BorderLayout.SOUTH);

        cmbPartido.addActionListener(e -> seleccionarPartido());
        cmbIncidencia.addActionListener(e -> actualizarVisibilidadExpulsion());

        setContentPane(panelPrincipal);
        pack();
        setMinimumSize(new Dimension(920, 600));
    }

    private void cargarTipos() {
        cmbIncidencia.addItem(TiposDatos.TipoAccion.GOL.name());
        cmbIncidencia.addItem(TiposDatos.TipoAccion.TARJETA_AMARILLA.name());
        cmbIncidencia.addItem(TiposDatos.TipoAccion.TARJETA_ROJA.name());
        cmbTipoExpulsion.addItem(TiposDatos.TipoExpulsion.DIRECTA.name());
        cmbTipoExpulsion.addItem(TiposDatos.TipoExpulsion.DOBLE_AMARILLA.name());
        cmbTipoExpulsion.setEnabled(false);
    }

    /** CU-04: para ARBITRO filtra por designacion; para ADMIN/ORG ofrece todos los pendientes. */
    private void cargarPartidosAsignados() {
        partidosAsignados = new ArrayList<>();
        boolean autorizado = PoliticaPermisos.tienePermisoOperacion(operador, PoliticaPermisos.ModuloMenu.CARGAR_ACTAS);
        btnAgregar.setEnabled(autorizado);
        btnQuitar.setEnabled(autorizado);
        btnCerrarActa.setEnabled(autorizado);
        if (!autorizado) {
            cmbPartido.setModel(new DefaultComboBoxModel<>());
            cmbPartido.setEnabled(false);
            partidoSeleccionado = null;
            lblDetalle.setText("Acceso denegado: requiere sesión de Árbitro, Organizador o Administrador.");
            return;
        }
        cmbPartido.setEnabled(true);
        boolean esOrganizadorOAdmin = operador.getRol() == TiposDatos.Rol.ADMINISTRADOR
                || operador.getRol() == TiposDatos.Rol.ORGANIZADOR;
        try {
            for (Partido p : partidoDAO.buscarPendientesDeFinalizacion()) {
                if (p.getEstadoPartido() == TiposDatos.EstadoPartido.SUSPENDIDO
                        || p.getEstadoPartido() == TiposDatos.EstadoPartido.POSTERGADO) {
                    continue;
                }
                if (!esOrganizadorOAdmin) {
                    if (p.getArbitroDesignado() == null || p.getArbitroDesignado().getId() == null) {
                        continue;
                    }
                    if (!p.getArbitroDesignado().getId().equals(operador.getId())) {
                        continue;
                    }
                }
                partidosAsignados.add(p);
            }
        } catch (Exception e) {
            lblDetalle.setText("No se pudieron consultar los partidos pendientes.");
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
            partidoSeleccionado = null;
            lblDetalle.setText(esOrganizadorOAdmin
                    ? "No hay partidos pendientes disponibles para oficializar."
                    : "No hay partidos pendientes asignados a este árbitro.");
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
        if (!PoliticaPermisos.tienePermisoOperacion(operador, PoliticaPermisos.ModuloMenu.CARGAR_ACTAS)) {
            JOptionPane.showMessageDialog(this,
                    "Acceso denegado: requiere sesión autorizada de Árbitro, Organizador o Administrador.",
                    "Acceso denegado", JOptionPane.ERROR_MESSAGE);
            return;
        }
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
