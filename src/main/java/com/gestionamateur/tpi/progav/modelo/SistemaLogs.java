package com.gestionamateur.tpi.progav.modelo;

import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.io.IOException;

public class SistemaLogs {
    public static void configurar() {
        try {
            FileHandler fileHandler = new FileHandler("registro_torneo.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            Logger loggerGlobal = Logger.getLogger("");
            loggerGlobal.addHandler(fileHandler);
        } catch (IOException e) {
            System.out.println("Error al iniciar bitácora.");
        }
    }
}