package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.controlador.GestorCanchas;
import com.gestionamateur.tpi.progav.controlador.GestorEquipos;
import com.gestionamateur.tpi.progav.controlador.GestorPartidos;
import com.gestionamateur.tpi.progav.modelo.Cancha;
import com.gestionamateur.tpi.progav.modelo.Categoria;
import com.gestionamateur.tpi.progav.modelo.Equipo;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.seguridad.PoliticaPermisos;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;

public class FrmProgramarPartido extends javax.swing.JFrame {

    private GestorPartidos gestorPartidos;
    private GestorCanchas gestorCanchas;
    private GestorEquipos gestorEquipos;
    private SimpleDateFormat formatoFechaHora;
    private Usuario operador;
    private List<Equipo> equiposCargados = new ArrayList<>();
    private List<Cancha> canchasCargadas = new ArrayList<>();

    public FrmProgramarPartido() {
        this(null);
    }

    public FrmProgramarPartido(Usuario operador) {
        this.operador = operador;
        initComponents();
        this.setLocationRelativeTo(null);
        
        gestorPartidos = new GestorPartidos();
        gestorCanchas = new GestorCanchas();
        gestorEquipos = new GestorEquipos();
        
        formatoFechaHora = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        formatoFechaHora.setLenient(false);

        // Fecha por defecto: mañana a las 18:00 (un encuentro no se programa en el pasado).
        Calendar fechaSugerida = Calendar.getInstance();
        fechaSugerida.add(Calendar.DAY_OF_MONTH, 1);
        fechaSugerida.set(Calendar.HOUR_OF_DAY, 18);
        fechaSugerida.set(Calendar.MINUTE, 0);
        fechaSugerida.set(Calendar.SECOND, 0);
        fechaSugerida.set(Calendar.MILLISECOND, 0);
        txtFechaHora.setText(formatoFechaHora.format(fechaSugerida.getTime()));

        cargarDatosCombos();
        actualizarVisorFixture();
    }

    private void cargarDatosCombos() {
        cmbEquipoLocal.removeAllItems();
        cmbEquipoVisitante.removeAllItems();
        cmbCancha.removeAllItems();
        equiposCargados = new ArrayList<>();
        canchasCargadas = new ArrayList<>();

        boolean autorizado = PoliticaPermisos.tienePermisoOperacion(operador, PoliticaPermisos.ModuloMenu.PROGRAMAR_PARTIDOS);
        if (!autorizado) {
            btnProgramar.setEnabled(false);
            cmbEquipoLocal.setEnabled(false);
            cmbEquipoVisitante.setEnabled(false);
            cmbCancha.setEnabled(false);
            lblSubtitulo.setText("Acceso denegado: requiere sesión de Administrador u Organizador.");
            return;
        }
        cmbEquipoLocal.setEnabled(true);
        cmbEquipoVisitante.setEnabled(true);
        cmbCancha.setEnabled(true);

        // Solo se cargan equipos activos y canchas habilitadas desde BD.
        equiposCargados.addAll(GestorEquipos.getEquiposRegistrados());
        for (Equipo e : equiposCargados) {
            cmbEquipoLocal.addItem(e.getNombre());
            cmbEquipoVisitante.addItem(e.getNombre());
        }
        if (cmbEquipoVisitante.getItemCount() > 1) {
            cmbEquipoVisitante.setSelectedIndex(1);
        }

        canchasCargadas.addAll(GestorCanchas.getCanchasRegistradas());
        for (Cancha c : canchasCargadas) {
            cmbCancha.addItem(c.getNombre());
        }

        if (equiposCargados.size() < 2 || canchasCargadas.isEmpty()) {
            btnProgramar.setEnabled(false);
            lblSubtitulo.setText("Para programar un partido se necesitan al menos 2 equipos activos y 1 cancha habilitada.");
        } else {
            btnProgramar.setEnabled(true);
        }
    }

    private void actualizarVisorFixture() {
        if (!PoliticaPermisos.tienePermisoOperacion(operador, PoliticaPermisos.ModuloMenu.PROGRAMAR_PARTIDOS)) {
            txtAreaFixture.setText("Acceso denegado: requiere sesión autorizada de Organizador o Administrador.");
            return;
        }
        txtAreaFixture.setText(gestorPartidos.obtenerResumenFixture());
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        lblTitulo = new javax.swing.JLabel();
        lblLocal = new javax.swing.JLabel();
        cmbEquipoLocal = new javax.swing.JComboBox<>();
        lblVisitante = new javax.swing.JLabel();
        cmbEquipoVisitante = new javax.swing.JComboBox<>();
        lblCancha = new javax.swing.JLabel();
        cmbCancha = new javax.swing.JComboBox<>();
        lblFechaHora = new javax.swing.JLabel();
        txtFechaHora = new javax.swing.JTextField();
        btnProgramar = new javax.swing.JButton();
        btnCerrar = new javax.swing.JButton();
        lblSubtitulo = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtAreaFixture = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Programación de Partidos y Asignación Arbitral (CU-03)");

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        EstilosUI.estilizarVentana(panel);

        JPanel panelEncabezado = EstilosUI.crearEncabezado(
                "Programación de partidos",
                "Asignación de contendientes, sede deportiva, horario y árbitro disponible (CU-03)");

        // --- Tarjeta de datos del encuentro ---
        JPanel panelFormulario = new JPanel(new GridBagLayout());
        EstilosUI.estilizarTarjeta(panelFormulario);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 12);
        gbc.anchor = GridBagConstraints.WEST;

        lblTitulo.setText("Programar Encuentro del Torneo");
        EstilosUI.estilizarTituloSeccion(lblTitulo);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 6, 8, 6);
        panelFormulario.add(lblTitulo, gbc);

        lblLocal.setText("Equipo Local:");
        lblLocal.setLabelFor(cmbEquipoLocal);
        EstilosUI.estilizarEtiquetaCampo(lblLocal);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 6, 5, 12);
        panelFormulario.add(lblLocal, gbc);

        EstilosUI.estilizarCombo(cmbEquipoLocal);
        cmbEquipoLocal.setMinimumSize(new Dimension(180, 30));
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 6, 5, 6);
        panelFormulario.add(cmbEquipoLocal, gbc);

        lblVisitante.setText("Equipo Visitante:");
        lblVisitante.setLabelFor(cmbEquipoVisitante);
        EstilosUI.estilizarEtiquetaCampo(lblVisitante);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 6, 5, 12);
        panelFormulario.add(lblVisitante, gbc);

        EstilosUI.estilizarCombo(cmbEquipoVisitante);
        cmbEquipoVisitante.setMinimumSize(new Dimension(180, 30));
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 6, 5, 6);
        panelFormulario.add(cmbEquipoVisitante, gbc);

        lblCancha.setText("Sede / Cancha:");
        lblCancha.setLabelFor(cmbCancha);
        EstilosUI.estilizarEtiquetaCampo(lblCancha);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 6, 5, 12);
        panelFormulario.add(lblCancha, gbc);

        EstilosUI.estilizarCombo(cmbCancha);
        cmbCancha.setMinimumSize(new Dimension(180, 30));
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 6, 5, 6);
        panelFormulario.add(cmbCancha, gbc);

        lblFechaHora.setText("Fecha y Hora (DD/MM/AAAA HH:MM):");
        lblFechaHora.setLabelFor(txtFechaHora);
        EstilosUI.estilizarEtiquetaCampo(lblFechaHora);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 6, 5, 12);
        panelFormulario.add(lblFechaHora, gbc);

        txtFechaHora.setText("25/10/2026 16:00");
        EstilosUI.estilizarCampoTexto(txtFechaHora, "dd/MM/yyyy HH:mm");
        txtFechaHora.setMinimumSize(new Dimension(180, txtFechaHora.getPreferredSize().height));
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 6, 5, 6);
        panelFormulario.add(txtFechaHora, gbc);

        // --- Tarjeta de resumen del fixture ---
        JPanel panelFixture = new JPanel(new BorderLayout(0, 10));
        EstilosUI.estilizarTarjeta(panelFixture);

        lblSubtitulo.setText("Partidos Programados en el Fixture:");
        EstilosUI.estilizarTituloSeccion(lblSubtitulo);
        panelFixture.add(lblSubtitulo, BorderLayout.NORTH);

        txtAreaFixture.setEditable(false);
        txtAreaFixture.setColumns(20);
        txtAreaFixture.setRows(6);
        txtAreaFixture.setLineWrap(true);
        txtAreaFixture.setWrapStyleWord(true);
        EstilosUI.estilizarAreaTexto(txtAreaFixture);
        txtAreaFixture.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        jScrollPane1.setViewportView(txtAreaFixture);
        jScrollPane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        jScrollPane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane1.setBorder(BorderFactory.createLineBorder(EstilosUI.BORDER_DEFAULT, 1));
        jScrollPane1.getViewport().setBackground(EstilosUI.BG_CONTROL);
        jScrollPane1.setPreferredSize(new Dimension(560, 150));
        jScrollPane1.setMinimumSize(new Dimension(320, 110));
        panelFixture.add(jScrollPane1, BorderLayout.CENTER);

        // --- Barra inferior de acciones ---
        btnProgramar.setText("Programar y Asignar Árbitro");
        EstilosUI.estilizarBotonPrimario(btnProgramar);
        btnProgramar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnProgramarActionPerformed(evt);
            }
        });

        btnCerrar.setText("Volver al Menú");
        EstilosUI.estilizarBotonNeutro(btnCerrar);
        btnCerrar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCerrarActionPerformed(evt);
            }
        });

        JPanel panelAcciones = EstilosUI.crearBarraAcciones(
                new JComponent[]{btnProgramar},
                new JComponent[]{btnCerrar});

        // --- Contenedor central adaptable ---
        JPanel panelCentro = new PanelCentroScrollable();
        panelCentro.setBackground(EstilosUI.BG_CANVAS);

        GridBagConstraints gbcCentro = new GridBagConstraints();
        gbcCentro.gridx = 0;
        gbcCentro.gridy = 0;
        gbcCentro.weightx = 1.0;
        gbcCentro.weighty = 0.0;
        gbcCentro.fill = GridBagConstraints.HORIZONTAL;
        gbcCentro.insets = new Insets(0, 0, 12, 0);
        panelCentro.add(panelFormulario, gbcCentro);

        gbcCentro.gridy = 1;
        gbcCentro.weighty = 1.0;
        gbcCentro.fill = GridBagConstraints.BOTH;
        gbcCentro.insets = new Insets(0, 0, 0, 0);
        panelCentro.add(panelFixture, gbcCentro);

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
        panel.add(panelAcciones, BorderLayout.SOUTH);
        setContentPane(panel);

        pack();
        setMinimumSize(new Dimension(660, 460));
        setSize(new Dimension(Math.max(getWidth(), 900), Math.max(getHeight(), 650)));
    }

    private void btnProgramarActionPerformed(java.awt.event.ActionEvent evt) {
        if (!PoliticaPermisos.tienePermisoOperacion(operador, PoliticaPermisos.ModuloMenu.PROGRAMAR_PARTIDOS)) {
            JOptionPane.showMessageDialog(this,
                    "Acceso denegado: requiere sesión de Organizador o Administrador.",
                    "Acceso denegado", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String fechaTexto = txtFechaHora.getText().trim();
        if (fechaTexto.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese la fecha y hora del encuentro.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Date fechaHora = formatoFechaHora.parse(fechaTexto);

            int idxLocal = cmbEquipoLocal.getSelectedIndex();
            int idxVisitante = cmbEquipoVisitante.getSelectedIndex();
            int idxCancha = cmbCancha.getSelectedIndex();

            List<Equipo> equipos = this.equiposCargados;
            List<Cancha> canchas = this.canchasCargadas;

            if (idxLocal < 0 || idxVisitante < 0 || idxLocal >= equipos.size() || idxVisitante >= equipos.size()) {
                JOptionPane.showMessageDialog(this, "Debe seleccionar ambos equipos contendientes.", "Datos incompletos", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (idxCancha < 0 || idxCancha >= canchas.size()) {
                JOptionPane.showMessageDialog(this, "Debe seleccionar una sede/cancha para el encuentro.", "Datos incompletos", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Equipo equipoLocal = equipos.get(idxLocal);
            Equipo equipoVisitante = equipos.get(idxVisitante);
            Cancha canchaSeleccionada = canchas.get(idxCancha);

            String respuesta = gestorPartidos.programarPartido(equipoLocal, equipoVisitante, fechaHora, canchaSeleccionada, operador);

            if (respuesta.startsWith("ERROR")) {
                JOptionPane.showMessageDialog(this, respuesta, "Error de Programación", JOptionPane.ERROR_MESSAGE);
            } else if (respuesta.startsWith("ADVERTENCIA")) {
                JOptionPane.showMessageDialog(this, respuesta, "Aviso de Árbitros", JOptionPane.WARNING_MESSAGE);
                actualizarVisorFixture();
            } else {
                JOptionPane.showMessageDialog(this, respuesta, "Partido Programado", JOptionPane.INFORMATION_MESSAGE);
                actualizarVisorFixture();
            }

        } catch (ParseException ex) {
            JOptionPane.showMessageDialog(this, "Formato de fecha inválido. Use DD/MM/AAAA HH:MM (Ej: 25/10/2026 16:00).", "Error de Formato", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void btnCerrarActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose();
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
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
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

    private javax.swing.JButton btnCerrar;
    private javax.swing.JButton btnProgramar;
    private javax.swing.JComboBox<String> cmbCancha;
    private javax.swing.JComboBox<String> cmbEquipoLocal;
    private javax.swing.JComboBox<String> cmbEquipoVisitante;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblCancha;
    private javax.swing.JLabel lblFechaHora;
    private javax.swing.JLabel lblLocal;
    private javax.swing.JLabel lblSubtitulo;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JLabel lblVisitante;
    private javax.swing.JTextArea txtAreaFixture;
    private javax.swing.JTextField txtFechaHora;
}