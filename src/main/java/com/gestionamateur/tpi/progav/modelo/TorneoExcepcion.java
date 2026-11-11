package com.gestionamateur.tpi.progav.modelo;

public class TorneoExcepcion extends Exception {
    
    public TorneoExcepcion(String mensaje) {
        super(mensaje);
    }

    public TorneoExcepcion(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
