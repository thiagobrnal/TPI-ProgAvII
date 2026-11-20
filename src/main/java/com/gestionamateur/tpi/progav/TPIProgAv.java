package com.gestionamateur.tpi.progav;

import com.gestionamateur.tpi.progav.dao.ConexionBD;
import com.gestionamateur.tpi.progav.modelo.SistemaLogs;
import com.gestionamateur.tpi.progav.vista.EstilosUI;
import com.gestionamateur.tpi.progav.vista.FrmLogin;

public class TPIProgAv {
    public static void main(String[] args) {
        // 0. Activacion explicita de modo APLICACION antes de cualquier acceso potencial a BD
        ConexionBD.activarModoAplicacion();

        // 1. Inicialización de la Bitácora de Eventos
        SistemaLogs.configurar();

        // 2. Configuración del sistema visual Dark Stadium Slate (FlatLaf Modo Oscuro)
        EstilosUI.inicializarTema();

        // 3. Invocación de la GUI
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new FrmLogin().setVisible(true);
            }
        });
    }
}