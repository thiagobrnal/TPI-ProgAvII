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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.swing.JOptionPane;

public class FrmProgramarPartido extends javax.swing.JFrame {

    private GestorPartidos gestorPartidos;
    private GestorCanchas gestorCanchas;
    private GestorEquipos gestorEquipos;
    private SimpleDateFormat formatoFechaHora;
    private Usuario operador;

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

        // Solo se cargan equipos y sedes realmente registrados en la base de datos.
        // No se crean datos ficticios desde la interfaz.
        List<Equipo> equipos = GestorEquipos.getEquiposRegistrados();
        for (Equipo e : equipos) {
            cmbEquipoLocal.addItem(e.getNombre());
            cmbEquipoVisitante.addItem(e.getNombre());
        }
        if (cmbEquipoVisitante.getItemCount() > 1) {
            cmbEquipoVisitante.setSelectedIndex(1);
        }

        for (Cancha c : GestorCanchas.getCanchasRegistradas()) {
            cmbCancha.addItem(c.getNombre());
        }

        if (equipos.size() < 2 || cmbCancha.getItemCount() == 0) {
            btnProgramar.setEnabled(false);
            lblSubtitulo.setText("Para programar un partido se necesitan al menos 2 equipos y 1 sede registrados.");
        }
    }

    private void actualizarVisorFixture() {
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

        lblTitulo.setFont(new java.awt.Font("Segoe UI", 1, 18));
        lblTitulo.setText("Programar Encuentro del Torneo");

        lblLocal.setText("Equipo Local:");
        lblVisitante.setText("Equipo Visitante:");
        lblCancha.setText("Sede / Cancha:");
        lblFechaHora.setText("Fecha y Hora (DD/MM/AAAA HH:MM):");
        txtFechaHora.setText("25/10/2026 16:00");

        btnProgramar.setText("Programar y Asignar Árbitro");
        btnProgramar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnProgramarActionPerformed(evt);
            }
        });

        btnCerrar.setText("Volver al Menú");
        btnCerrar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCerrarActionPerformed(evt);
            }
        });

        lblSubtitulo.setFont(new java.awt.Font("Segoe UI", 1, 13));
        lblSubtitulo.setText("Partidos Programados en el Fixture:");

        txtAreaFixture.setEditable(false);
        txtAreaFixture.setColumns(20);
        txtAreaFixture.setRows(6);
        jScrollPane1.setViewportView(txtAreaFixture);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(35, 35, 35)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblTitulo)
                    .addComponent(lblSubtitulo)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 520, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblLocal)
                            .addComponent(lblVisitante)
                            .addComponent(lblCancha)
                            .addComponent(lblFechaHora)
                            .addComponent(btnCerrar))
                        .addGap(25, 25, 25)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cmbEquipoLocal, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cmbEquipoVisitante, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cmbCancha, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtFechaHora)
                            .addComponent(btnProgramar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap(35, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(lblTitulo)
                .addGap(25, 25, 25)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblLocal)
                    .addComponent(cmbEquipoLocal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblVisitante)
                    .addComponent(cmbEquipoVisitante, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblCancha)
                    .addComponent(cmbCancha, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblFechaHora)
                    .addComponent(txtFechaHora, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(25, 25, 25)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnProgramar, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCerrar, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(25, 25, 25)
                .addComponent(lblSubtitulo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(25, Short.MAX_VALUE))
        );

        pack();
    }

    private void btnProgramarActionPerformed(java.awt.event.ActionEvent evt) {
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

            List<Equipo> equipos = GestorEquipos.getEquiposRegistrados();
            List<Cancha> canchas = GestorCanchas.getCanchasRegistradas();

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