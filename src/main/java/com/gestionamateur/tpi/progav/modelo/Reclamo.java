package com.gestionamateur.tpi.progav.modelo;

import java.util.Date;
import java.util.logging.Logger;

public class Reclamo {
    
    // 1. Logger de la clase Reclamo
    private static final Logger LOGGER = Logger.getLogger(Reclamo.class.getName());

    private Long id;
    private Date fechaInicio;
    private Date fechaFin;
    private String motivo;
    private String estado;
    private String veredicto;
    private String justificacion;
    private Sancion sancionApelada;

    // Constructor vacío (JPA)
    public Reclamo() {}

    // 2. Constructor de inicio: Cuando nace el reclamo
    public Reclamo(Date fechaInicio, String motivo, Sancion sancionApelada) throws TorneoExcepcion {
        setFechaInicio(fechaInicio);
        setMotivo(motivo);
        this.sancionApelada = sancionApelada; // Puede ser nulo si es un reclamo general y no contra una sanción
        
        // Regla de Negocio: Todo reclamo nuevo nace en este estado
        this.estado = "EN REVISIÓN"; 
        
        LOGGER.info("Se ha abierto un nuevo expediente de Reclamo (Estado: EN REVISIÓN).");
    }

    // --- COMPORTAMIENTO DE DOMINIO RICO ---
    
    // Método que controla el ciclo de vida del reclamo
    public void resolverReclamo(Date fechaFin, String veredicto, String justificacion) throws TorneoExcepcion {
        if (this.estado.equalsIgnoreCase("RESUELTO")) {
            LOGGER.warning("Validación fallida: Intento de volver a resolver un reclamo ya cerrado.");
            throw new TorneoExcepcion("Este reclamo ya cuenta con un veredicto y se encuentra cerrado.");
        }
        
        setFechaFin(fechaFin);
        setVeredicto(veredicto);
        setJustificacion(justificacion);
        
        this.estado = "RESUELTO"; // Transición de estado controlada
        LOGGER.info("El reclamo ha sido formalmente RESUELTO.");
    }

    // --- GETTERS Y SETTERS BLINDADOS ---

    public Long getId() { return id; } 
    public void setId(Long id) { this.id = id; }
    
    public Date getFechaInicio() { return fechaInicio; }
    
    public void setFechaInicio(Date fechaInicio) throws TorneoExcepcion { 
        if (fechaInicio == null) {
            LOGGER.warning("Validación fallida: Reclamo ingresado sin fecha de inicio.");
            throw new TorneoExcepcion("Todo reclamo formal debe tener una fecha de inicio (recepción).");
        }
        this.fechaInicio = fechaInicio; 
    }
    
    public Date getFechaFin() { return fechaFin; }
    
    // Validación cronológica: La fecha de fin no puede ser anterior a la de inicio
    public void setFechaFin(Date fechaFin) throws TorneoExcepcion { 
        if (fechaFin != null && this.fechaInicio != null && fechaFin.before(this.fechaInicio)) {
            LOGGER.severe("Incoherencia temporal: Fecha de resolución anterior a la fecha de inicio.");
            throw new TorneoExcepcion("La fecha de resolución no puede ser anterior a la fecha en que se inició el reclamo.");
        }
        this.fechaFin = fechaFin; 
    }
    
    public String getMotivo() { return motivo; }
    
    public void setMotivo(String motivo) throws TorneoExcepcion { 
        if (motivo == null || motivo.trim().isEmpty()) {
            LOGGER.warning("Validación fallida: Reclamo sin motivo.");
            throw new TorneoExcepcion("El motivo del reclamo es obligatorio para iniciar el expediente.");
        }
        if (motivo.length() < 15) {
            throw new TorneoExcepcion("El motivo debe estar redactado detalladamente (mínimo 15 caracteres).");
        }
        this.motivo = motivo; 
    }
    
    public String getEstado() { return estado; }
    
    // Uso interno, pero lo protegemos
    public void setEstado(String estado) throws TorneoExcepcion { 
        if (estado == null || estado.trim().isEmpty()) {
            throw new TorneoExcepcion("El estado del expediente no puede quedar en blanco.");
        }
        this.estado = estado; 
    }
    
    public String getVeredicto() { return veredicto; }
    
    public void setVeredicto(String veredicto) throws TorneoExcepcion { 
        if (veredicto == null || veredicto.trim().isEmpty()) {
            throw new TorneoExcepcion("El veredicto (Ej: 'A favor', 'En contra') es obligatorio para resolver el reclamo.");
        }
        this.veredicto = veredicto; 
    }
    
    public String getJustificacion() { return justificacion; }
    
    public void setJustificacion(String justificacion) throws TorneoExcepcion { 
        if (justificacion == null || justificacion.trim().isEmpty()) {
            throw new TorneoExcepcion("Debe escribir una justificación formal del tribunal para avalar el veredicto.");
        }
        this.justificacion = justificacion; 
    }
    
    public Sancion getSancionApelada() { return sancionApelada; } 
    public void setSancionApelada(Sancion sancionApelada) { this.sancionApelada = sancionApelada; }
}