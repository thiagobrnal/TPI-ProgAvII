package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.controlador.GestorCanchas;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import javax.swing.JOptionPane;

public class FrmAltaCancha extends javax.swing.JFrame {

    private GestorCanchas gestorCanchas;

    public FrmAltaCancha() {
        initComponents();
        this.setLocationRelativeTo(null);
        gestorCanchas = new GestorCanchas();
        
        cmbSuperficie.addItem("Césped Natural");
        cmbSuperficie.addItem("Césped Sintético");
        cmbSuperficie.addItem("Tierra / Arena");
        cmbSuperficie.addItem("Cemento");
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        lblTitulo = new javax.swing.JLabel();
        lblNombre = new javax.swing.JLabel();
        txtNombre = new javax.swing.JTextField();
        lblDireccion = new javax.swing.JLabel();
        txtDireccion = new javax.swing.JTextField();
        lblSuperficie = new javax.swing.JLabel();
        cmbSuperficie = new javax.swing.JComboBox<>();
        btnGuardar = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Gestión de Sedes");

        lblTitulo.setFont(new java.awt.Font("Segoe UI", 1, 18)); 
        lblTitulo.setText("Registrar Nueva Cancha");

        lblNombre.setText("Nombre del Predio:");
        lblDireccion.setText("Dirección Física:");
        lblSuperficie.setText("Tipo de Superficie:");

        btnGuardar.setText("Guardar Sede");
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });

        btnCancelar.setText("Cancelar");
        btnCancelar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblTitulo)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblNombre)
                            .addComponent(lblDireccion)
                            .addComponent(lblSuperficie))
                        .addGap(25, 25, 25)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(txtNombre)
                            .addComponent(txtDireccion)
                            .addComponent(cmbSuperficie, 0, 160, Short.MAX_VALUE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnCancelar)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 100, Short.MAX_VALUE)
                        .addComponent(btnGuardar)))
                .addContainerGap(40, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(lblTitulo)
                .addGap(30, 30, 30)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblNombre)
                    .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblDireccion)
                    .addComponent(txtDireccion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblSuperficie)
                    .addComponent(cmbSuperficie, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 40, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnGuardar)
                    .addComponent(btnCancelar))
                .addGap(30, 30, 30))
        );
        pack();
    }

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {
        String nombre = txtNombre.getText();
        String direccion = txtDireccion.getText();
        String superficie = cmbSuperficie.getSelectedItem().toString();

        try {
            String respuesta = gestorCanchas.registrarCancha(nombre, direccion, superficie);
            JOptionPane.showMessageDialog(this, respuesta, "Registro Exitoso", JOptionPane.INFORMATION_MESSAGE);
            txtNombre.setText("");
            txtDireccion.setText("");
            cmbSuperficie.setSelectedIndex(0);
        } catch (TorneoExcepcion ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose();
    }

    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JComboBox<String> cmbSuperficie;
    private javax.swing.JLabel lblDireccion;
    private javax.swing.JLabel lblNombre;
    private javax.swing.JLabel lblSuperficie;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JTextField txtDireccion;
    private javax.swing.JTextField txtNombre;
}