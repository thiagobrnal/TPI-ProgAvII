package com.gestionamateur.tpi.progav.modelo;

import java.util.Date;
import java.util.logging.Logger;

public class Partido {
    
    // 1. Logger de la clase Partido
    private static final Logger LOGGER = Logger.getLogger(Partido.class.getName());

    private Long idPartido;
    private Date fechaHora;
    private TiposDatos.EstadoPartido estadoPartido;
    private Arbitro arbitroDesignado;
    private Cancha sede;
    private Motivo motivoSuspension;

    // Constructor vacío (JPA)
    public Partido() {}

    // 2. Constructor completo: Un partido nace en estado PROGRAMADO
    public Partido(Date fechaHora, Cancha sede) throws TorneoExcepcion {
        setFechaHora(fechaHora);
        setSede(sede);
        
        // Estado inicial por defecto
        this.estadoPartido = TiposDatos.EstadoPartido.PROGRAMADO;
        
        LOGGER.info("Se ha programado un nuevo partido para la fecha: " + this.fechaHora);
    }
    
    // --- MÉTODOS DE DOMINIO RICO (Comportamiento) ---

    public void finalizarPartido() throws TorneoExcepcion {
        if (this.estadoPartido == TiposDatos.EstadoPartido.FINALIZADO) {
            LOGGER.warning("Validación fallida: Intento de finalizar un partido que ya estaba cerrado.");
            throw new TorneoExcepcion("El partido ya se encuentra en estado FINALIZADO.");
        }
        this.estadoPartido = TiposDatos.EstadoPartido.FINALIZADO;
        LOGGER.info("El partido ha finalizado exitosamente.");
    }

    public void posponerPartido(Motivo motivo) throws TorneoExcepcion {
        if (motivo == null) {
            LOGGER.warning("Validación fallida: Intento de posponer un partido sin justificación formal.");
            throw new TorneoExcepcion("Para posponer un partido es obligatorio adjuntar un Motivo formal.");
        }
        
        this.estadoPartido = TiposDatos.EstadoPartido.POSTERGADO;
        this.motivoSuspension = motivo;
        LOGGER.info("El partido ha sido POSTERGADO. Motivo asignado.");
    }

    // --- GETTERS Y SETTERS BLINDADOS ---

    public Long getIdPartido() { return idPartido; }
    public void setIdPartido(Long idPartido) { this.idPartido = idPartido; }
    
    public Date getFechaHora() { return fechaHora; }
    
    public void setFechaHora(Date fechaHora) throws TorneoExcepcion { 
        if (fechaHora == null) {
            LOGGER.warning("Validación fallida: Partido programado sin fecha ni hora.");
            throw new TorneoExcepcion("Debe asignar una fecha y hora válida para programar el partido.");
        }
        this.fechaHora = fechaHora; 
    }
    
    public TiposDatos.EstadoPartido getEstadoPartido() { return estadoPartido; }
    
    // Este setter suele ser de uso interno, pero lo validamos igual
    public void setEstadoPartido(TiposDatos.EstadoPartido estadoPartido) throws TorneoExcepcion { 
        if (estadoPartido == null) {
            LOGGER.severe("Error Crítico: Intento de dejar un partido sin estado.");
            throw new TorneoExcepcion("El estado del partido no puede ser nulo.");
        }
        this.estadoPartido = estadoPartido; 
    }
    
    public Arbitro getArbitroDesignado() { return arbitroDesignado; } 
    
    public void setArbitroDesignado(Arbitro arbitroDesignado) throws TorneoExcepcion { 
        if (arbitroDesignado == null) {
            LOGGER.warning("Validación fallida: Intento de asignar un árbitro nulo.");
            throw new TorneoExcepcion("Debe seleccionar un árbitro válido para el partido.");
        }
        this.arbitroDesignado = arbitroDesignado; 
        LOGGER.info("Árbitro " + arbitroDesignado.getNombre() + " designado para el partido.");
    }
    
    public Cancha getSede() { return sede; } 
    
    public void setSede(Cancha sede) throws TorneoExcepcion { 
        if (sede == null) {
            LOGGER.warning("Validación fallida: Partido programado sin sede.");
            throw new TorneoExcepcion("Debe asignar una sede/cancha física donde se jugará el partido.");
        }
        this.sede = sede; 
    }
    
    public Motivo getMotivoSuspension() { return motivoSuspension; } 
    public void setMotivoSuspension(Motivo motivoSuspension) { this.motivoSuspension = motivoSuspension; }
}