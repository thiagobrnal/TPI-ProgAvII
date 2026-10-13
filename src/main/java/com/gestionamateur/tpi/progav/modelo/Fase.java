package com.gestionamateur.tpi.progav.modelo;

import java.util.logging.Logger;

public class Fase {
    
    // 1. Logger para la clase Fase
    private static final Logger LOGGER = Logger.getLogger(Fase.class.getName());

    private Long id;
    private String nombre;
    private int orden;

    // Constructor vacío (JPA)
    public Fase() {}
    
    // 2. Constructor completo que delega en los setters blindados
    public Fase(String nombre, int orden) throws TorneoExcepcion {
        setNombre(nombre);
        setOrden(orden);
        
        LOGGER.info("Se ha instanciado correctamente la fase: " + this.nombre + " (Orden de juego: " + this.orden + ")");
    }

    public Long getId() { return id; } 
    public void setId(Long id) { this.id = id; }
    
    public String getNombre() { return nombre; }
    
    // 3. Validación de texto
    public void setNombre(String nombre) throws TorneoExcepcion { 
        if (nombre == null || nombre.trim().isEmpty()) {
            LOGGER.warning("Validación fallida: Intento de crear una fase sin nombre.");
            throw new TorneoExcepcion("El nombre de la fase no puede estar vacío (Ej: 'Fase de Grupos', 'Cuartos de Final').");
        }
        this.nombre = nombre; 
    }
    
    public int getOrden() { return orden; }
    
    // 4. Validación matemática de negocio
    public void setOrden(int orden) throws TorneoExcepcion { 
        if (orden <= 0) {
            LOGGER.warning("Validación fallida: Intento de asignar un orden inválido (" + orden + ") a la fase '" + this.nombre + "'.");
            throw new TorneoExcepcion("El orden de ejecución de la fase debe ser un número mayor a 0.");
        }
        this.orden = orden; 
    }
}