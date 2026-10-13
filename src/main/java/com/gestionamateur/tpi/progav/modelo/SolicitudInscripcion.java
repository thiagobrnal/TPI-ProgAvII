package com.gestionamateur.tpi.progav.modelo;

import java.util.logging.Logger;

public class SolicitudInscripcion {
    
    // 1. Logger para auditar el ingreso de los equipos al torneo
    private static final Logger LOGGER = Logger.getLogger(SolicitudInscripcion.class.getName());

    private Long idSolicitud;
    private String estado;
    private String motivo;
    private Equipo equipoSolicitante;

    // Constructor vacío (obligatorio para JPA)
    public SolicitudInscripcion() {}

    // 2. Constructor de inicio: La solicitud nace PENDIENTE
    public SolicitudInscripcion(Equipo equipoSolicitante) throws TorneoExcepcion {
        setEquipoSolicitante(equipoSolicitante);
        
        // Estado inicial inmutable al momento de crear
        this.estado = "PENDIENTE";
        this.motivo = "Evaluación de requisitos en proceso";
        
        LOGGER.info("Nueva solicitud de inscripción registrada para el equipo: " + this.equipoSolicitante.getNombre());
    }

    // --- COMPORTAMIENTO DE DOMINIO RICO (Ciclo de Vida) ---

    public void aprobar() throws TorneoExcepcion {
        if (!this.estado.equalsIgnoreCase("PENDIENTE")) {
            LOGGER.warning("Validación fallida: Intento de aprobar una solicitud que ya estaba " + this.estado);
            throw new TorneoExcepcion("Solo se pueden aprobar solicitudes que se encuentren en estado PENDIENTE.");
        }
        
        this.estado = "APROBADA";
        this.motivo = "Cumple con todos los requisitos reglamentarios.";
        LOGGER.info("Solicitud APROBADA para el equipo: " + equipoSolicitante.getNombre());
    }

    public void rechazar(String motivoRechazo) throws TorneoExcepcion {
        if (!this.estado.equalsIgnoreCase("PENDIENTE")) {
            LOGGER.warning("Validación fallida: Intento de rechazar una solicitud que ya estaba " + this.estado);
            throw new TorneoExcepcion("Solo se pueden rechazar solicitudes que se encuentren en estado PENDIENTE.");
        }
        
        if (motivoRechazo == null || motivoRechazo.trim().isEmpty()) {
            throw new TorneoExcepcion("Para rechazar una solicitud, es obligatorio explicar el motivo al equipo.");
        }
        
        this.estado = "RECHAZADA";
        this.motivo = motivoRechazo;
        LOGGER.info("Solicitud RECHAZADA para el equipo: " + equipoSolicitante.getNombre() + " | Motivo: " + motivoRechazo);
    }

    // --- GETTERS Y SETTERS BLINDADOS ---

    public Long getIdSolicitud() { return idSolicitud; } 
    public void setIdSolicitud(Long idSolicitud) { this.idSolicitud = idSolicitud; }
    
    public String getEstado() { return estado; }
    
    // Lo validamos por si el administrador lo usa manualmente
    public void setEstado(String estado) throws TorneoExcepcion { 
        if (estado == null || estado.trim().isEmpty()) {
            throw new TorneoExcepcion("El estado de la solicitud no puede estar vacío.");
        }
        this.estado = estado; 
    }
    
    public String getMotivo() { return motivo; }
    
    public void setMotivo(String motivo) { 
        this.motivo = motivo; 
    }
    
    public Equipo getEquipoSolicitante() { return equipoSolicitante; }
    
    public void setEquipoSolicitante(Equipo equipoSolicitante) throws TorneoExcepcion { 
        if (equipoSolicitante == null) {
            LOGGER.severe("Validación fallida: Solicitud de inscripción sin equipo asociado.");
            throw new TorneoExcepcion("La solicitud es inválida: Debe indicar qué equipo es el solicitante.");
        }
        this.equipoSolicitante = equipoSolicitante; 
    }
}