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

    public Equipo(String nombre, Categoria categoria, String colorCamiseta, Usuario capitan) throws TorneoExcepcion {
        this(nombre, categoria, colorCamiseta, null, capitan);
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

    // --- GETTERS Y SETTERS BLINDADOS ---

    public Long getIdEquipo() { return idEquipo; } 
    public void setIdEquipo(Long idEquipo) { this.idEquipo = idEquipo; }
    
    public String getNombre() { return nombre; }
    
    // 4. Validaciones de negocio en atributos obligatorios
    public void setNombre(String nombre) throws TorneoExcepcion { 
        if (nombre == null || nombre.trim().isEmpty()) {
            LOGGER.warning("Validación fallida: Intento de registrar equipo sin nombre.");
            throw new TorneoExcepcion("El nombre del equipo es obligatorio.");
        }
        this.nombre = nombre; 
    }
    
    public String getAlias() { return alias; } 
    public void setAlias(String alias) { this.alias = alias; }
    
    public Categoria getCategoria() { return categoria; }
    
    public void setCategoria(Categoria categoria) throws TorneoExcepcion { 
        if (categoria == null) {
            LOGGER.severe("Validación Crítica: Intento de crear un equipo sin asociarlo a una categoría.");
            throw new TorneoExcepcion("Todo equipo debe pertenecer obligatoriamente a una Categoría deportiva.");
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
            LOGGER.severe("Validación Crítica: Equipo registrado sin un Capitán responsable.");
            throw new TorneoExcepcion("El equipo debe tener asignado un Capitán responsable para las comunicaciones.");
        }
        this.capitan = capitan; 
    }
    
    public List<Jugador> getPlantel() { 
        return new ArrayList<>(this.plantel); 
    }

    public void setPlantel(List<Jugador> plantel) {
        this.plantel = plantel != null ? new ArrayList<>(plantel) : new ArrayList<>();
    }
}
