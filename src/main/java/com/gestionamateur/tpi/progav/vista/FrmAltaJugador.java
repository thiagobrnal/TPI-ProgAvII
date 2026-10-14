package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.controlador.GestorJugadores;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JOptionPane;

public class FrmAltaJugador extends javax.swing.JFrame {

    private GestorJugadores gestorJugadores;
    private SimpleDateFormat formatoFecha;

    public FrmAltaJugador() {
        initComponents();
        this.setLocationRelativeTo(null);
        gestorJugadores = new GestorJugadores();
        formatoFecha = new SimpleDateFormat("dd/MM/yyyy");
        formatoFecha.setLenient(false); // Para que no acepte fechas raras como 32/13/2020
        
        // Carga dinámica de posiciones
        String[] posiciones = {"Arquero", "Defensor", "Mediocampista", "Delantero"};
        for (String pos : posiciones) {
            cmbPosicion.addItem(pos);
        }
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        lblTitulo = new javax.swing.JLabel();
        lblNombre = new javax.swing.JLabel();
        txtNombre = new javax.swing.JTextField();
        lblDni = new javax.swing.JLabel();
        txtDni = new javax.swing.JTextField();
        lblFechaNac = new javax.swing.JLabel();
        txtFechaNac = new javax.swing.JTextField();
        lblPosicion = new javax.swing.JLabel();
        cmbPosicion = new javax.swing.JComboBox<>();
        btnGuardar = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Alta de Jugadores");

        lblTitulo.setFont(new java.awt.Font("Segoe UI", 1, 18)); 
        lblTitulo.setText("Registrar Nuevo Jugador");

        lblNombre.setText("Nombre Completo:");
        lblDni.setText("Documento (DNI):");
        lblFechaNac.setText("Fecha Nacimiento (DD/MM/AAAA):");
        lblPosicion.setText("Posición principal:");

        btnGuardar.setText("Guardar Jugador");
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
                        .addComponent(btnCancelar)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnGuardar))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblNombre)
                            .addComponent(lblDni)
                            .addComponent(lblFechaNac)
                            .addComponent(lblPosicion))
                        .addGap(20, 20, 20)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(txtNombre)
                            .addComponent(txtDni)
                            .addComponent(txtFechaNac)
                            .addComponent(cmbPosicion, 0, 160, Short.MAX_VALUE))))
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
                    .addComponent(lblDni)
                    .addComponent(txtDni, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblFechaNac)
                    .addComponent(txtFechaNac, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblPosicion)
                    .addComponent(cmbPosicion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 35, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnGuardar)
                    .addComponent(btnCancelar))
                .addGap(30, 30, 30))
        );
        pack();
    }

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {
        String nombre = txtNombre.getText();
        String dniTexto = txtDni.getText();
        String fechaTexto = txtFechaNac.getText();
        String posicion = cmbPosicion.getSelectedItem().toString();

        // 1. Validaciones básicas de la interfaz (evitar mandar vacíos al controlador)
        if (nombre.trim().isEmpty() || dniTexto.trim().isEmpty() || fechaTexto.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Complete todos los campos por favor.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // 2. Parseo de datos al formato que exige tu GestorJugadores
            int dni = Integer.parseInt(dniTexto);
            Date fechaNacimiento = formatoFecha.parse(fechaTexto);

            // 3. Llamada a tu controlador
            String respuesta = gestorJugadores.iniciarAltaJugador(nombre, dni, fechaNacimiento, posicion);

            // 4. Tu controlador devuelve un String que empieza con "ERROR" si algo falló
            if (respuesta.startsWith("ERROR")) {
                JOptionPane.showMessageDialog(this, respuesta, "Error en el registro", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, respuesta, "Operación Exitosa", JOptionPane.INFORMATION_MESSAGE);
                limpiarCampos();
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "El DNI debe contener únicamente números.", "Error de formato", JOptionPane.ERROR_MESSAGE);
        } catch (ParseException e) {
            JOptionPane.showMessageDialog(this, "La fecha de nacimiento debe tener el formato DD/MM/AAAA.", "Error de formato", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose();
    }

    private void limpiarCampos() {
        txtNombre.setText("");
        txtDni.setText("");
        txtFechaNac.setText("");
        cmbPosicion.setSelectedIndex(0);
    }

    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JComboBox<String> cmbPosicion;
    private javax.swing.JLabel lblDni;
    private javax.swing.JLabel lblFechaNac;
    private javax.swing.JLabel lblNombre;
    private javax.swing.JLabel lblPosicion;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JTextField txtDni;
    private javax.swing.JTextField txtFechaNac;
    private javax.swing.JTextField txtNombre;
}