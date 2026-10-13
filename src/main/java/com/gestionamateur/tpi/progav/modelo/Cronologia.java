package com.gestionamateur.tpi.progav.modelo;

import java.util.Date;
import java.util.logging.Logger;

public class Cronologia {
    
    // 1. Instanciamos el Logger para auditar todas las incidencias de los partidos
    private static final Logger LOGGER = Logger.getLogger(Cronologia.class.getName());

    private Long id;
    private Date minuto;
    private TiposDatos.TipoAccion accion;
    private Jugador autor;
    private Partido partido;

    // Constructor vacío requerido por JPA
    public Cronologia() {}

    // 2. Constructor completo para crear la incidencia de forma segura
    public Cronologia(Date minuto, TiposDatos.TipoAccion accion, Jugador autor, Partido partido) throws TorneoExcepcion {
        setMinuto(minuto);
        setAccion(accion);
        setAutor(autor);
        setPartido(partido);
        
        // Registro exitoso en la bitácora
        LOGGER.info("Nueva incidencia registrada: [" + this.accion.name() + "] por el jugador ID " + this.autor.getId() + " en el partido ID " + this.partido.getIdPartido());
    }

    public Long getId() { return id; } 
    public void setId(Long id) { this.id = id; }
    
    public Date getMinuto() { return minuto; }
    
    // 3. Validaciones de negocio: Nada puede ser nulo en un evento de cronología
    public void setMinuto(Date minuto) throws TorneoExcepcion { 
        if (minuto == null) {
            LOGGER.warning("Validación fallida: Intento de registrar incidencia sin especificar el minuto.");
            throw new TorneoExcepcion("Debe indicar el tiempo exacto en el que ocurrió la incidencia.");
        }
        this.minuto = minuto; 
    }
    
    public TiposDatos.TipoAccion getAccion() { return accion; }
    
    public void setAccion(TiposDatos.TipoAccion accion) throws TorneoExcepcion { 
        if (accion == null) {
            LOGGER.warning("Validación fallida: Se intentó registrar una incidencia sin tipo de acción.");
            throw new TorneoExcepcion("Debe clasificar el tipo de acción (Gol, Amarilla, Roja, Cambio).");
        }
        this.accion = accion; 
    }
    
    public Jugador getAutor() { return autor; }
    
    public void setAutor(Jugador autor) throws TorneoExcepcion { 
        if (autor == null) {
            LOGGER.warning("Validación fallida: Incidencia sin jugador autor.");
            throw new TorneoExcepcion("Toda acción en el partido debe estar vinculada al jugador que la realizó.");
        }
        this.autor = autor; 
    }
    
    public Partido getPartido() { return partido; }
    
    public void setPartido(Partido partido) throws TorneoExcepcion { 
        if (partido == null) {
            LOGGER.warning("Validación fallida: Incidencia flotante (sin partido asociado).");
            throw new TorneoExcepcion("La cronología debe estar asociada a un partido válido.");
        }
        this.partido = partido; 
    }
}