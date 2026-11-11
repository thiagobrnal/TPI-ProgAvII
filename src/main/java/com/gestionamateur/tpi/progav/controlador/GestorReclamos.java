package com.gestionamateur.tpi.progav.controlador;

import com.gestionamateur.tpi.progav.modelo.Reclamo;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.servicio.ReclamoService;

import java.util.ArrayList;
import java.util.List;

/**
 * CU-14: bandeja de reclamos formales pendientes de resolucion por el organizador.
 * El reclamo en si se presenta desde el canal web (R2); aqui se resuelve.
 */
public class GestorReclamos {

    private final ReclamoService reclamoService = new ReclamoService();

    public List<Reclamo> listarPendientes() {
        try {
            return reclamoService.listarReclamosPendientes();
        } catch (TorneoExcepcion e) {
            return new ArrayList<>();
        }
    }

    public String resolver(Long idReclamo, String veredicto, String justificacion, Usuario operador) {
        try {
            reclamoService.resolverExpediente(idReclamo, veredicto, justificacion, operador);
            return "ÉXITO: Reclamo resuelto y notificado en el estado del sistema.";
        } catch (TorneoExcepcion e) {
            return "ERROR DE VALIDACIÓN: " + e.getMessage();
        } catch (Exception e) {
            return "ERROR DE CONEXIÓN: La base de datos no está disponible (" + e.getMessage() + "). No se guardaron cambios.";
        }
    }
}
