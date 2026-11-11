package com.gestionamateur.tpi.progav.modelo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class Torneo {
    
    // 1. Logger para la clase Torneo
    private static final Logger LOGGER = Logger.getLogger(Torneo.class.getName());

    private Long idTorneo;
    private String nombre;
    private TiposDatos.FormatoTorneo formato;
    private int cupoEquipos;
    private Date fechaInicio;
    private String estado = "CONFIGURACION"; // CONFIGURACION, EN_CURSO, FINALIZADO
    private List<Fase> esquemaFases;

    // Constructor vacío (Obligatorio para JPA)
    public Torneo() { 
        this.esquemaFases = new ArrayList<>(); 
    }

    // 2. Constructor de instanciación validada
    public Torneo(String nombre, int cupoEquipos, TiposDatos.FormatoTorneo formato, Date fechaInicio) throws TorneoExcepcion {
        setNombre(nombre);
        setCupoEquipos(cupoEquipos);
        setFormato(formato);
        setFechaInicio(fechaInicio);
        this.estado = "CONFIGURACION";
        this.esquemaFases = new ArrayList<>();
        
        LOGGER.info("Se ha creado exitosamente el torneo central: " + this.nombre + " (Cupo: " + this.cupoEquipos + " equipos)");
    }

    public Torneo(String nombre, String formatoStr, int cupoEquipos, Date fechaInicio) throws TorneoExcepcion {
        setNombre(nombre);
        setCupoEquipos(cupoEquipos);
        setFormato(formatoStr);
        setFechaInicio(fechaInicio);
        this.estado = "CONFIGURACION";
        this.esquemaFases = new ArrayList<>();
    }
    
    // --- COMPORTAMIENTO DE DOMINIO RICO ---
    
    public void agregarFase(Fase fase) throws TorneoExcepcion {
        if (fase == null) {
            LOGGER.warning("Validación fallida: Intento de agregar una fase nula al esquema del torneo.");
            throw new TorneoExcepcion("No se puede registrar una fase inválida o vacía.");
        }
        this.esquemaFases.add(fase);
        LOGGER.info("Fase '" + fase.getNombre() + "' agregada correctamente al torneo " + this.nombre);
    }

    // --- GETTERS Y SETTERS BLINDADOS ---

    public Long getIdTorneo() { return idTorneo; } 
    public void setIdTorneo(Long idTorneo) { this.idTorneo = idTorneo; }
    
    public String getNombre() { return nombre; }
    
    public void setNombre(String nombre) throws TorneoExcepcion { 
        if (nombre == null || nombre.trim().isEmpty()) {
            LOGGER.warning("Validación fallida: Intento de crear un torneo sin nombre.");
            throw new TorneoExcepcion("El nombre del torneo es un dato obligatorio.");
        }
        this.nombre = nombre; 
    }
    
    public TiposDatos.FormatoTorneo getFormato() { return formato; }
    
    public void setFormato(TiposDatos.FormatoTorneo formato) throws TorneoExcepcion { 
        if (formato == null) {
            LOGGER.warning("Validación fallida: Torneo sin formato de juego.");
            throw new TorneoExcepcion("Debe seleccionar un formato de competencia válido (Ej. Liga, Eliminatoria).");
        }
        this.formato = formato; 
    }

    public void setFormato(String formatoStr) throws TorneoExcepcion {
        if (formatoStr == null || formatoStr.trim().isEmpty()) {
            throw new TorneoExcepcion("El formato del torneo no puede estar vacío.");
        }
        try {
            this.formato = TiposDatos.FormatoTorneo.valueOf(formatoStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new TorneoExcepcion("Formato de torneo desconocido: " + formatoStr + ". Formatos válidos: LIGA, ELIMINATORIA, GRUPOS.");
        }
    }
    
    public int getCupoEquipos() { return cupoEquipos; }
    
    // 3. Validaciones Matemáticas de Negocio
    public void setCupoEquipos(int cupoEquipos) throws TorneoExcepcion { 
        if (cupoEquipos < 2) {
            LOGGER.warning("Validación fallida: Cupo de equipos insuficiente (" + cupoEquipos + ").");
            throw new TorneoExcepcion("Un torneo debe tener un cupo mínimo de 2 equipos inscriptos para poder disputarse.");
        }
        if (cupoEquipos % 2 != 0) {
            LOGGER.warning("Incoherencia detectada: Intento de configurar un cupo de equipos impar (" + cupoEquipos + ").");
            throw new TorneoExcepcion("Para garantizar el correcto armado del fixture y los emparejamientos, el cupo de equipos debe ser un número par.");
        }
        this.cupoEquipos = cupoEquipos; 
    }
    
    public Date getFechaInicio() { return fechaInicio; }
    
    public void setFechaInicio(Date fechaInicio) throws TorneoExcepcion { 
        if (fechaInicio == null) {
            LOGGER.warning("Validación fallida: Fecha de inicio de torneo no especificada.");
            throw new TorneoExcepcion("Es obligatorio definir la fecha de inicio en la que arrancará el torneo.");
        }
        this.fechaInicio = fechaInicio; 
    }

    public String getEstado() { return estado; }
    public void setEstado(String estado) throws TorneoExcepcion {
        if (estado == null || estado.trim().isEmpty()) {
            throw new TorneoExcepcion("El estado del torneo no puede ser nulo.");
        }
        String norm = estado.trim().toUpperCase();
        if (!norm.equals("CONFIGURACION") && !norm.equals("EN_CURSO") && !norm.equals("FINALIZADO")) {
            throw new TorneoExcepcion("Estado de torneo inválido: debe ser CONFIGURACION, EN_CURSO o FINALIZADO.");
        }
        this.estado = norm;
    }
    
    public List<Fase> getEsquemaFases() { return esquemaFases; }
    public void setEsquemaFases(List<Fase> esquemaFases) { this.esquemaFases = esquemaFases; }
}
