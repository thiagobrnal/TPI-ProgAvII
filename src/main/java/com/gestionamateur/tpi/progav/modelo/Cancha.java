package com.gestionamateur.tpi.progav.modelo;

import java.util.logging.Logger;

public class Cancha {
    
    // 1. Instanciamos el Logger para auditar la creación de Sedes
    private static final Logger LOGGER = Logger.getLogger(Cancha.class.getName());

    private Long idCancha;
    private String nombre;
    private String direccion;
    private String tipoSuperficie;

    // Constructor vacío (obligatorio para JPA en la Fase 2)
    public Cancha() {}
    
    // 2. Constructor completo que delega en los setters estrictos
    public Cancha(String nombre, String direccion, String tipoSuperficie) throws TorneoExcepcion {
        setNombre(nombre);
        setDireccion(direccion);
        setTipoSuperficie(tipoSuperficie);
        
        // Log de éxito en la bitácora
        LOGGER.info("Se ha registrado correctamente la sede/cancha: " + this.nombre + " (" + this.tipoSuperficie + ")");
    }

    public Long getIdCancha() { return idCancha; } 
    public void setIdCancha(Long idCancha) { this.idCancha = idCancha; }
    
    public String getNombre() { return nombre; } 
    
    // 3. Setters blindados que lanzan tu excepción personalizada
    public void setNombre(String nombre) throws TorneoExcepcion { 
        if (nombre == null || nombre.trim().isEmpty()) {
            LOGGER.warning("Validación fallida: Intento de registrar una cancha sin nombre.");
            throw new TorneoExcepcion("El nombre de la cancha es un dato obligatorio.");
        }
        this.nombre = nombre; 
    }
    
    public String getDireccion() { return direccion; } 
    
    public void setDireccion(String direccion) throws TorneoExcepcion { 
        if (direccion == null || direccion.trim().isEmpty()) {
            LOGGER.warning("Validación fallida: Intento de registrar la cancha '" + this.nombre + "' sin dirección.");
            throw new TorneoExcepcion("La dirección de la sede es obligatoria para la programación de los partidos.");
        }
        this.direccion = direccion; 
    }
    
    public String getTipoSuperficie() { return tipoSuperficie; } 
    
    public void setTipoSuperficie(String tipoSuperficie) throws TorneoExcepcion { 
        if (tipoSuperficie == null || tipoSuperficie.trim().isEmpty()) {
            LOGGER.warning("Validación fallida: No se especificó la superficie para la cancha '" + this.nombre + "'.");
            throw new TorneoExcepcion("Debe especificar el tipo de superficie de la cancha (Ej. Sintético, Parquet, Césped).");
        }
        this.tipoSuperficie = tipoSuperficie; 
    }
}