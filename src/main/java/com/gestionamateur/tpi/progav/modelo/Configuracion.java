package com.gestionamateur.tpi.progav.modelo;

import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;

public class Configuracion {
    
    // 1. Logger para auditar cambios en las reglas del sistema
    private static final Logger LOGGER = Logger.getLogger(Configuracion.class.getName());

    private Map<String, Integer> valoresGlobales;

    public Configuracion() {
        this.valoresGlobales = new HashMap<>();
    }

    public Map<String, Integer> getValores() { 
        return valoresGlobales; 
    }

    // Setter general blindado
    public void setValores(Map<String, Integer> valoresGlobales) throws TorneoExcepcion { 
        if (valoresGlobales == null) {
            LOGGER.severe("Error Crítico: Intento de borrar el mapa de configuraciones globales (Null).");
            throw new TorneoExcepcion("El diccionario de configuraciones no puede ser nulo.");
        }
        this.valoresGlobales = valoresGlobales; 
        LOGGER.info("Se ha reemplazado el mapa completo de configuraciones globales.");
    }

    // 2. Método de Dominio Rico: Agregar o actualizar una configuración específica
    public void agregarValor(String clave, Integer valor) throws TorneoExcepcion {
        if (clave == null || clave.trim().isEmpty()) {
            LOGGER.warning("Validación fallida: Intento de guardar una configuración sin nombre (clave vacía).");
            throw new TorneoExcepcion("La clave de configuración no puede estar en blanco.");
        }
        
        if (valor == null) {
            LOGGER.warning("Validación fallida: Intento de asignar un valor nulo a la configuración '" + clave + "'.");
            throw new TorneoExcepcion("El valor para la configuración '" + clave + "' no puede ser nulo.");
        }
        
        // Si todo es correcto, se guarda o actualiza en el mapa
        this.valoresGlobales.put(clave, valor);
        LOGGER.info("Configuración global actualizada -> " + clave + ": " + valor);
    }
}