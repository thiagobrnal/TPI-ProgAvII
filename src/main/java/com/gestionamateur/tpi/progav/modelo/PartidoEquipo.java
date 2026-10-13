package com.gestionamateur.tpi.progav.modelo;

import java.util.logging.Logger;

public class PartidoEquipo {
    
    // 1. Logger para auditar la asignación de equipos a los partidos
    private static final Logger LOGGER = Logger.getLogger(PartidoEquipo.class.getName());

    private Long id;
    private boolean esLocal;
    private Equipo equipo;
    private Partido partido;

    // Constructor vacío (Requisito estricto de JPA)
    public PartidoEquipo() {}

    // 2. Constructor completo delegando en setters blindados
    public PartidoEquipo(boolean esLocal, Equipo equipo, Partido partido) throws TorneoExcepcion {
        setEsLocal(esLocal);
        setEquipo(equipo);
        setPartido(partido);
        
        // Log descriptivo usando el operador ternario para mostrar "Local" o "Visitante"
        String condicion = esLocal ? "Local" : "Visitante";
        LOGGER.info("Asignación de partido creada: El equipo " + equipo.getNombre() + " jugará de " + condicion);
    }

    public Long getId() { return id; } 
    public void setId(Long id) { this.id = id; }
    
    public boolean isEsLocal() { return esLocal; } 
    
    public void setEsLocal(boolean esLocal) { 
        this.esLocal = esLocal; 
    }
    
    public Equipo getEquipo() { return equipo; } 
    
    // 3. Validación de Integridad Referencial
    public void setEquipo(Equipo equipo) throws TorneoExcepcion { 
        if (equipo == null) {
            LOGGER.severe("Validación Crítica Fallida: Intento de crear una asignación sin equipo.");
            throw new TorneoExcepcion("La asignación es inválida: debe especificar qué equipo participa en el partido.");
        }
        this.equipo = equipo; 
    }
    
    public Partido getPartido() { return partido; } 
    
    public void setPartido(Partido partido) throws TorneoExcepcion { 
        if (partido == null) {
            LOGGER.severe("Validación Crítica Fallida: Asignación de equipo a un partido fantasma (nulo).");
            throw new TorneoExcepcion("La asignación es inválida: debe especificar a qué partido pertenece el equipo.");
        }
        this.partido = partido; 
    }
}