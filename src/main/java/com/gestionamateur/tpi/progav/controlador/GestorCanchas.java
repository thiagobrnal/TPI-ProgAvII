package com.gestionamateur.tpi.progav.controlador;

import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import java.util.logging.Logger;
import java.util.logging.Level;

public class GestorCanchas {
    
    private static final Logger LOGGER = Logger.getLogger(GestorCanchas.class.getName());

    public String registrarCancha(String nombre, String direccion, String superficie) throws TorneoExcepcion {
        try {
            if (nombre == null || nombre.trim().isEmpty()) {
                throw new TorneoExcepcion("El nombre de la sede es obligatorio.");
            }
            if (direccion == null || direccion.trim().isEmpty()) {
                throw new TorneoExcepcion("Debe especificar la dirección de la cancha.");
            }

            // Simulación de guardado en base de datos / lista
            // Cancha nuevaCancha = new Cancha(nombre, direccion, superficie);
            
            LOGGER.info("Sede registrada correctamente: " + nombre);
            return "Cancha '" + nombre + "' agregada al catálogo del torneo.";

        } catch (TorneoExcepcion te) {
            LOGGER.warning("Error de validación en Canchas: " + te.getMessage());
            throw te;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fallo general en GestorCanchas", e);
            throw new TorneoExcepcion("Error del sistema al guardar la sede.");
        }
    }
}