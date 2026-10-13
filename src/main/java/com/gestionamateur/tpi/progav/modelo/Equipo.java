package com.gestionamateur.tpi.progav.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Equipo {
    
    // 1. Logger para la clase Equipo
    private static final Logger LOGGER = Logger.getLogger(Equipo.class.getName());

    private Long idEquipo;
    private String nombre;
    private String alias;
    private Categoria categoria; // Relación con la clase Categoria
    private String colorCamiseta;
    private Cancha sedeHabitual;
    private Usuario capitan;
    private List<Jugador> plantel;

    // Constructor vacío (JPA)
    public Equipo() { 
        this.plantel = new ArrayList<>(); 
    }

    // 2. Constructor completo delegando en setters blindados
    public Equipo(String nombre, Categoria categoria, String colorCamiseta, Cancha sedeHabitual, Usuario capitan) throws TorneoExcepcion {
        setNombre(nombre);
        setCategoria(categoria);
        setColorCamiseta(colorCamiseta);
        setSedeHabitual(sedeHabitual);
        setCapitan(capitan);
        this.plantel = new ArrayList<>();
        
        LOGGER.info("Se ha instanciado y validado correctamente el equipo: " + this.nombre);
    }

    // 3. Comportamiento protegido (Cuidamos la lista del plantel)
    public void agregarJugador(Jugador j) throws TorneoExcepcion { 
        if (j == null) {
            LOGGER.warning("Validación fallida: Intento de agregar un jugador nulo al plantel del equipo " + this.nombre);
            throw new TorneoExcepcion("No se puede agregar un jugador inválido o vacío al plantel.");
        }
        this.plantel.add(j); 
        LOGGER.info("Jugador agregado al plantel del equipo: " + this.nombre);
    }

    public Long getIdEquipo() { return idEquipo; }
    public void setIdEquipo(Long idEquipo) { this.idEquipo = idEquipo; }
    
    public String getNombre() { return nombre; }
    
    public void setNombre(String nombre) throws TorneoExcepcion { 
        if (nombre == null || nombre.trim().isEmpty()) {
            LOGGER.warning("Validación fallida: Se intentó crear un equipo sin nombre.");
            throw new TorneoExcepcion("El nombre del equipo no puede estar vacío.");
        }
        this.nombre = nombre; 
    }
    
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    
    public Categoria getCategoria() { return categoria; }
    
    public void setCategoria(Categoria categoria) throws TorneoExcepcion { 
        if (categoria == null) {
            LOGGER.warning("Validación fallida: Equipo instanciado sin categoría asignada.");
            throw new TorneoExcepcion("El equipo debe pertenecer obligatoriamente a una categoría válida.");
        }
        this.categoria = categoria; 
    }
    
    public String getColorCamiseta() { return colorCamiseta; }
    
    public void setColorCamiseta(String colorCamiseta) throws TorneoExcepcion { 
        if (colorCamiseta == null || colorCamiseta.trim().isEmpty()) {
            LOGGER.warning("Validación fallida: Equipo sin color de camiseta.");
            throw new TorneoExcepcion("Debe especificar el color principal de la camiseta para evitar choques en los partidos.");
        }
        this.colorCamiseta = colorCamiseta; 
    }
    
    public Cancha getSedeHabitual() { return sedeHabitual; }
    
    public void setSedeHabitual(Cancha sedeHabitual) { 
        // La sede habitual puede no ser obligatoria al momento de la inscripción (se permite null)
        this.sedeHabitual = sedeHabitual; 
    }
    
    public Usuario getCapitan() { return capitan; }
    
    public void setCapitan(Usuario capitan) throws TorneoExcepcion { 
        if (capitan == null) {
            LOGGER.warning("Validación fallida: Equipo inscripto sin capitán.");
            throw new TorneoExcepcion("Todo equipo debe tener un usuario responsable o capitán asignado.");
        }
        this.capitan = capitan; 
    }
    
    public List<Jugador> getPlantel() { return plantel; } 
    public void setPlantel(List<Jugador> plantel) { this.plantel = plantel; }
}