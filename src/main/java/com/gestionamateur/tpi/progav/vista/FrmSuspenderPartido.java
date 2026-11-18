package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.controlador.GestorPartidos;
import com.gestionamateur.tpi.progav.dao.PartidoDAO;
import com.gestionamateur.tpi.progav.modelo.Partido;
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
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * CU-05: postergacion y suspension de partidos programados, con motivo obligatorio.
 */
public class FrmSuspenderPartido extends JFrame {

    private static final SimpleDateFormat FORMATO_FECHA = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private final Usuario operador;
    private final GestorPartidos gestorPartidos = new GestorPartidos();
    private final PartidoDAO partidoDAO = new PartidoDAO();

    private List<Partido> partidos = new ArrayList<>();
    private final JComboBox<String> cmbPartido = new JComboBox<>();
    private final JTextArea txtMotivo = new JTextArea(5, 40);
    private final JLabel lblInfo = new JLabel(" ");
    private final JButton btnPostergar = new JButton("Postergar partido");
    private final JButton btnSuspender = new JButton("Suspender partido");

    public FrmSuspenderPartido(Usuario operador) {
        this.operador = operador;
        setTitle("Postergar o Suspender Partido (CU-05)");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        cargarPartidos();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        EstilosUI.estilizarVentana(panel);

        JPanel panelEncabezado = EstilosUI.crearEncabezado(
                "Gestión de encuentros",
                "Postergación o suspensión de partidos programados con motivo obligatorio (CU-05)");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 10);
        gbc.anchor = GridBagConstraints.WEST;

        JPanel panelSup = new JPanel(new GridBagLayout());
        EstilosUI.estilizarTarjeta(panelSup);

        JLabel lblPartido = new JLabel("Partido programado:");
        EstilosUI.estilizarEtiquetaCampo(lblPartido);
        gbc.gridx = 0;
        gbc.gridy = 0;
        panelSup.add(lblPartido, gbc);

        EstilosUI.estilizarCombo(cmbPartido);
        cmbPartido.setPreferredSize(new Dimension(440, 30));
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelSup.add(cmbPartido, gbc);

        JLabel lblMotivo = new JLabel("Motivo (obligatorio):");
        EstilosUI.estilizarEtiquetaCampo(lblMotivo);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panelSup.add(lblMotivo, gbc);

        txtMotivo.setLineWrap(true);
        txtMotivo.setWrapStyleWord(true);
        EstilosUI.estilizarAreaTexto(txtMotivo);
        txtMotivo.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JScrollPane scrollMotivo = new JScrollPane(
                txtMotivo,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollMotivo.setBorder(BorderFactory.createLineBorder(EstilosUI.BORDER_DEFAULT, 1));
        scrollMotivo.getViewport().setBackground(EstilosUI.BG_CONTROL);
        scrollMotivo.setPreferredSize(new Dimension(440, 120));
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panelSup.add(scrollMotivo, gbc);

        EstilosUI.estilizarEstadoInfo(lblInfo);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        panelSup.add(lblInfo, gbc);

        EstilosUI.estilizarBotonAdvertencia(btnPostergar);
        btnPostergar.addActionListener(e -> ejecutar("POSTERGAR"));

        EstilosUI.estilizarBotonPeligro(btnSuspender);
        btnSuspender.addActionListener(e -> ejecutar("SUSPENDER"));

        JButton btnVolver = new JButton("Volver al menú");
        EstilosUI.estilizarBotonNeutro(btnVolver);
        btnVolver.addActionListener(e -> dispose());

        JPanel panelAcciones = EstilosUI.crearBarraAcciones(
                new JComponent[]{btnPostergar, btnSuspender},
                new JComponent[]{btnVolver});

        panel.add(panelEncabezado, BorderLayout.NORTH);
        panel.add(panelSup, BorderLayout.CENTER);
        panel.add(panelAcciones, BorderLayout.SOUTH);
        setContentPane(panel);
        pack();
        setMinimumSize(new Dimension(700, 460));
    }

    private void cargarPartidos() {
        partidos = new ArrayList<>();
        boolean autorizado = PoliticaPermisos.tienePermisoOperacion(
                operador, PoliticaPermisos.ModuloMenu.POSTERGAR_SUSPENDER_PARTIDOS);
        btnPostergar.setEnabled(autorizado);
        btnSuspender.setEnabled(autorizado);
        cmbPartido.setEnabled(autorizado);
        if (!autorizado) {
            cmbPartido.setModel(new DefaultComboBoxModel<>());
            lblInfo.setText("Acceso denegado: requiere sesión de Organizador o Administrador.");
            return;
        }
        try {
            for (Partido p : partidoDAO.buscarPendientesDeFinalizacion()) {
                if (p.getEstadoPartido() == com.gestionamateur.tpi.progav.modelo.TiposDatos.EstadoPartido.PROGRAMADO) {
                    partidos.add(p);
                }
            }
        } catch (Exception e) {
            lblInfo.setText("No se pudieron consultar los partidos programados.");
        }
        DefaultComboBoxModel<String> modelo = new DefaultComboBoxModel<>();
        for (Partido p : partidos) {
            modelo.addElement("#" + p.getIdPartido() + " - " + FORMATO_FECHA.format(p.getFechaHora())
                    + (p.getArbitroDesignado() == null ? " - sin arbitro" : " - arbitro: " + p.getArbitroDesignado().getNombre()));
        }
        cmbPartido.setModel(modelo);
        if (partidos.isEmpty()) {
            lblInfo.setText("No hay partidos programados disponibles para postergar o suspender.");
        }
    }

    private void ejecutar(String operacion) {
        int idx = cmbPartido.getSelectedIndex();
        if (idx < 0 || idx >= partidos.size()) {
            JOptionPane.showMessageDialog(this, "Seleccione el partido a modificar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String motivo = txtMotivo.getText() == null ? "" : txtMotivo.getText().trim();
        if (motivo.length() < 10) {
            JOptionPane.showMessageDialog(this, "Indique el motivo de la decisión (mínimo 10 caracteres).",
                    "Motivo requerido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long idPartido = partidos.get(idx).getIdPartido();
        String respuesta = "POSTERGAR".equals(operacion)
                ? gestorPartidos.procesarSuspension(idPartido, motivo, operador)
                : gestorPartidos.procesarSuspensionPorIncidencias(idPartido, motivo, operador);

        if (respuesta.startsWith("ÉXITO")) {
            JOptionPane.showMessageDialog(this, respuesta, operacion, JOptionPane.INFORMATION_MESSAGE);
            txtMotivo.setText("");
            cargarPartidos();
        } else {
            JOptionPane.showMessageDialog(this, respuesta, "No se pudo registrar la decisión", JOptionPane.ERROR_MESSAGE);
        }
    }
}
