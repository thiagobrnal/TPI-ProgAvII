package com.gestionamateur.tpi.progav;

import com.gestionamateur.tpi.progav.modelo.SistemaLogs;
import com.gestionamateur.tpi.progav.vista.FrmLogin;

public class TPIProgAv {
    public static void main(String[] args) {
        // 1. Inicialización de la Bitácora de Eventos
        SistemaLogs.configurar();

        // 2. Configuración del diseño visual moderno (FlatLaf Modo Oscuro)
        com.formdev.flatlaf.FlatDarkLaf.setup();

        // 3. Invocación de la GUI
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new FrmLogin().setVisible(true);
            }
        });
    }
}