package com.gestionamateur.tpi.progav.modelo;

import java.util.logging.Logger;

public class Arbitro extends Usuario {
    
    // 1. Instanciamos el Logger específico para la clase Arbitro
    private static final Logger LOGGER = Logger.getLogger(Arbitro.class.getName());

    private String categoriaArbitral;
    private String telefono;

    // Constructor vacío requerido por JPA
    public Arbitro() {
        super();
    }

    // 2. El constructor completo ahora avisa que puede lanzar excepciones
    public Arbitro(String usuario, String contraseña, String mail, String nombre, String apellido, String categoriaArbitral, String telefono) throws TorneoExcepcion {
        // Llamada a la clase padre (Usuario)
        super(usuario, contraseña, mail, nombre, apellido, TiposDatos.Rol.ARBITRO);
        
        // 3. Usamos los setters estrictos en lugar de asignar directamente
        setCategoriaArbitral(categoriaArbitral);
        setTelefono(telefono);
        
        // Log de éxito
        LOGGER.info("Se ha instanciado correctamente el árbitro: " + nombre + " " + apellido + " (Cat: " + categoriaArbitral + ")");
    }

    public String getDatos() { 
        return "Árbitro: " + nombre + " " + apellido + " | Cat: " + categoriaArbitral; 
    }

    public String getCategoriaArbitral() { 
        return categoriaArbitral; 
    }

    // 4. Setters con validación y lanzamiento de excepciones
    public void setCategoriaArbitral(String categoriaArbitral) throws TorneoExcepcion { 
        if (categoriaArbitral == null || categoriaArbitral.trim().isEmpty()) {
            LOGGER.warning("Validación fallida: Intento de asignar una categoría arbitral vacía.");
            throw new TorneoExcepcion("La categoría arbitral no puede estar en blanco.");
        }
        this.categoriaArbitral = categoriaArbitral; 
    }
    
    public String getTelefono() { 
        return telefono; 
    }

    public void setTelefono(String telefono) throws TorneoExcepcion { 
        if (telefono == null || telefono.trim().isEmpty()) {
            LOGGER.warning("Validación fallida: Intento de instanciar un árbitro sin teléfono.");
            throw new TorneoExcepcion("El teléfono del árbitro es un campo obligatorio para futuras designaciones.");
        }
        this.telefono = telefono; 
    }
}