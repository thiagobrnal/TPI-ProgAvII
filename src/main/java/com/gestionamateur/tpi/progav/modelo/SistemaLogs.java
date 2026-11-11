package com.gestionamateur.tpi.progav.modelo;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class SistemaLogs {

    private static boolean configurado = false;

    public static synchronized void configurar() {
        if (configurado) return;
        try {
            FileHandler fileHandler = new FileHandler("registro_torneo.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            Logger loggerGlobal = Logger.getLogger("");
            loggerGlobal.addHandler(fileHandler);
            configurado = true;
        } catch (IOException e) {
            System.out.println("Error al iniciar bitácora: " + e.getMessage());
        }
    }

    public static void info(Class<?> clazz, String mensaje) {
        Logger.getLogger(clazz.getName()).info(mensaje);
    }

    public static void warning(Class<?> clazz, String mensaje) {
        Logger.getLogger(clazz.getName()).warning(mensaje);
    }

    public static void severe(Class<?> clazz, String mensaje) {
        Logger.getLogger(clazz.getName()).severe(mensaje);
    }
}
