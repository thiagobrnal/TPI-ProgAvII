package com.gestionamateur.tpi.progav.modelo;

import java.util.logging.Logger;

public class Categoria {
    
    // 1. Logger para la clase Categoria
    private static final Logger LOGGER = Logger.getLogger(Categoria.class.getName());

    private Long id;
    private String nombre;
    private String genero;
    private int edadMinima;
    private int edadMaxima;

    // Constructor vacío (JPA)
    public Categoria() {}

    // 2. Constructor que delega en los setters y hace validación cruzada
    public Categoria(String nombre, String genero, int edadMinima, int edadMaxima) throws TorneoExcepcion {
        setNombre(nombre);
        setGenero(genero);
        setEdadMinima(edadMinima);
        setEdadMaxima(edadMaxima);
        
        // Validación cruzada de negocio
        if (this.edadMinima > this.edadMaxima) {
            LOGGER.warning("Incoherencia lógica: Rango de edades inválido al crear categoría " + this.nombre);
            throw new TorneoExcepcion("La edad mínima (" + this.edadMinima + ") no puede ser mayor que la edad máxima (" + this.edadMaxima + ").");
        }
        
        LOGGER.info("Se ha instanciado correctamente la categoría: " + this.nombre + " (" + this.genero + ")");
    }

    public Long getId() { return id; } 
    public void setId(Long id) { this.id = id; }
    
    public String getNombre() { return nombre; }
    
    public void setNombre(String nombre) throws TorneoExcepcion { 
        if (nombre == null || nombre.trim().isEmpty()) {
            LOGGER.warning("Validación fallida: Nombre de categoría en blanco.");
            throw new TorneoExcepcion("El nombre de la categoría no puede estar vacío.");
        }
        this.nombre = nombre; 
    }
    
    public String getGenero() { return genero; }
    
    public void setGenero(String genero) throws TorneoExcepcion { 
        if (genero == null || genero.trim().isEmpty()) {
            LOGGER.warning("Validación fallida: Género de categoría no especificado.");
            throw new TorneoExcepcion("Debe especificar el género de la categoría (ej. Masculino, Femenino, Mixto).");
        }
        this.genero = genero; 
    }
    
    public int getEdadMinima() { return edadMinima; }
    
    public void setEdadMinima(int edadMinima) throws TorneoExcepcion { 
        if (edadMinima < 0) {
            LOGGER.warning("Validación fallida: Se ingresó una edad mínima negativa.");
            throw new TorneoExcepcion("La edad mínima no puede ser menor a 0.");
        }
        this.edadMinima = edadMinima; 
    }
    
    public int getEdadMaxima() { return edadMaxima; }
    
    public void setEdadMaxima(int edadMaxima) throws TorneoExcepcion { 
        if (edadMaxima < 0) {
            LOGGER.warning("Validación fallida: Se ingresó una edad máxima negativa.");
            throw new TorneoExcepcion("La edad máxima no puede ser un número negativo.");
        }
        this.edadMaxima = edadMaxima; 
    }
}