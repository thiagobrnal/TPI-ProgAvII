package com.gestionamateur.tpi.progav;

import com.gestionamateur.tpi.progav.modelo.SistemaLogs;
import com.gestionamateur.tpi.progav.vista.FrmLogin;

public class TPIProgAv {
    public static void main(String[] args) {
        // 1. Inicialización de la Bitácora de Eventos
        SistemaLogs.configurar();

        // 2. Configuración del diseño visual (Nimbus Look and Feel)
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            System.err.println("Error al cargar el diseño visual Nimbus.");
        }

        // 3. Invocación de la GUI
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new FrmLogin().setVisible(true);
            }
        });
    }
}