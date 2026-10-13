package com.gestionamateur.tpi.progav.modelo;

import java.util.Date;
import java.util.logging.Logger;

public class Jugador {
    
    // 1. Logger de la clase Jugador
    private static final Logger LOGGER = Logger.getLogger(Jugador.class.getName());

    private Long id;
    private String nombre;
    private int dni;
    private Date fechaNacimiento;
    private String posicion;
    private TiposDatos.EstadoJugador estado;
    private int historialTarjetas;

    // Constructor vacío (JPA)
    public Jugador() {}

    // 2. Constructor completo delegando en los setters estrictos
    public Jugador(String nombre, int dni, Date fechaNacimiento, String posicion, TiposDatos.EstadoJugador estado) throws TorneoExcepcion {
        setNombre(nombre);
        setDni(dni);
        setFechaNacimiento(fechaNacimiento);
        setPosicion(posicion);
        setEstado(estado);
        this.historialTarjetas = 0; // Inicia en 0 limpiecito
        
        LOGGER.info("Se ha inscripto correctamente el jugador: " + this.nombre + " (DNI: " + this.dni + ")");
    }

    // Comportamiento de Dominio
    public void sumarTarjetaAlConteo() { 
        this.historialTarjetas++; 
        LOGGER.info("Se ha sumado una tarjeta al historial del jugador " + this.nombre + " (Total: " + this.historialTarjetas + ")");
    }

    public Long getId() { return id; } 
    public void setId(Long id) { this.id = id; }
    
    public String getNombre() { return nombre; }
    
    // 3. Validaciones de negocio
    public void setNombre(String nombre) throws TorneoExcepcion { 
        if (nombre == null || nombre.trim().isEmpty()) {
            LOGGER.warning("Validación fallida: Intento de inscribir un jugador sin nombre.");
            throw new TorneoExcepcion("El nombre del jugador es un dato obligatorio.");
        }
        this.nombre = nombre; 
    }
    
    public int getDni() { return dni; }
    
    public void setDni(int dni) throws TorneoExcepcion { 
        if (dni <= 1000000) { // Validación lógica de un DNI real
            LOGGER.warning("Validación fallida: DNI inválido ingresado (" + dni + ").");
            throw new TorneoExcepcion("El DNI ingresado no es válido para la inscripción.");
        }
        this.dni = dni; 
    }
    
    public Date getFechaNacimiento() { return fechaNacimiento; }
    
    public void setFechaNacimiento(Date fechaNacimiento) throws TorneoExcepcion { 
        if (fechaNacimiento == null) {
            LOGGER.warning("Validación fallida: Jugador sin fecha de nacimiento.");
            throw new TorneoExcepcion("La fecha de nacimiento es obligatoria para calcular la categoría del jugador.");
        }
        this.fechaNacimiento = fechaNacimiento; 
    }
    
    public String getPosicion() { return posicion; }
    
    public void setPosicion(String posicion) throws TorneoExcepcion { 
        if (posicion == null || posicion.trim().isEmpty()) {
            LOGGER.warning("Validación fallida: Jugador sin posición definida.");
            throw new TorneoExcepcion("Debe indicar la posición del jugador en la cancha (ej. Arquero, Defensor, etc.).");
        }
        this.posicion = posicion; 
    }
    
    public TiposDatos.EstadoJugador getEstado() { return estado; }
    
    public void setEstado(TiposDatos.EstadoJugador estado) throws TorneoExcepcion { 
        if (estado == null) {
            LOGGER.warning("Validación fallida: Jugador sin estado disciplinario.");
            throw new TorneoExcepcion("Todo jugador debe tener un estado asignado (Activo, Suspendido, etc.).");
        }
        this.estado = estado; 
    }
    
    public int getHistorialTarjetas() { return historialTarjetas; }
    
    // En caso de que se necesite setear manualmente, también validamos que no sea negativo
    public void setHistorialTarjetas(int historialTarjetas) throws TorneoExcepcion { 
        if (historialTarjetas < 0) {
            LOGGER.warning("Validación fallida: Intento de asignar tarjetas negativas.");
            throw new TorneoExcepcion("El historial de tarjetas no puede ser un número negativo.");
        }
        this.historialTarjetas = historialTarjetas; 
    }
}