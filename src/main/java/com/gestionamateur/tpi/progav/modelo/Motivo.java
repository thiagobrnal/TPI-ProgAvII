package com.gestionamateur.tpi.progav.modelo;

import java.util.logging.Logger;

public class Motivo {
    
    // 1. Logger de la clase
    private static final Logger LOGGER = Logger.getLogger(Motivo.class.getName());

    private Long id;
    private String motivo;

    // Constructor vacío (JPA)
    public Motivo() {}

    // 2. Constructor completo delegando al setter seguro
    public Motivo(String motivo) throws TorneoExcepcion { 
        setMotivo(motivo);
        
        LOGGER.info("Se ha registrado un nuevo motivo formal en el sistema.");
    }

    public Long getId() { return id; } 
    public void setId(Long id) { this.id = id; }
    
    public String getMotivo() { return motivo; }
    
    // 3. Validación de negocio
    public void setMotivo(String motivo) throws TorneoExcepcion { 
        if (motivo == null || motivo.trim().isEmpty()) {
            LOGGER.warning("Validación fallida: Se intentó registrar una justificación vacía.");
            throw new TorneoExcepcion("El texto del motivo es obligatorio. Debe especificar las razones de la acción.");
        }
        
        // Regla opcional: Si queremos evitar motivos demasiado cortos como "Ah" o "Sí"
        if (motivo.trim().length() < 5) {
            LOGGER.warning("Validación fallida: El motivo ingresado es demasiado corto.");
            throw new TorneoExcepcion("El motivo debe ser lo suficientemente descriptivo (mínimo 5 caracteres).");
        }
        
        this.motivo = motivo; 
    }
}