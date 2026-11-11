package com.gestionamateur.tpi.progav.controlador;

import com.gestionamateur.tpi.progav.modelo.Sancion;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.servicio.DisciplinaService;

import java.util.ArrayList;
import java.util.List;

/**
 * CU-12: gestion disciplinaria (tribunal). Coordina las sanciones pendientes de fallo,
 * la emision del fallo, la anulacion y el alta manual de sanciones administrativas.
 */
public class GestorDisciplina {

    private final DisciplinaService disciplinaService = new DisciplinaService();

    public List<Sancion> listarPendientes() {
        try {
            return disciplinaService.listarSancionesPendientesTribunal();
        } catch (TorneoExcepcion e) {
            return new ArrayList<>();
        }
    }

    public String emitirFallo(Long idSancion, int partidosSuspension, String motivo, String gravedad, Usuario operador) {
        try {
            disciplinaService.emitirFalloTribunal(idSancion, partidosSuspension, motivo, gravedad, operador);
            return "ÉXITO: Fallo disciplinario emitido y sanción oficializada.";
        } catch (TorneoExcepcion e) {
            return "ERROR DE VALIDACIÓN: " + e.getMessage();
        } catch (Exception e) {
            return "ERROR DE CONEXIÓN: La base de datos no está disponible (" + e.getMessage() + "). No se guardaron cambios.";
        }
    }

    public String anular(Long idSancion, String motivoAnulacion, Usuario operador) {
        try {
            disciplinaService.anularSancion(idSancion, motivoAnulacion, operador);
            return "ÉXITO: Sanción anulada y jugador rehabilitado.";
        } catch (TorneoExcepcion e) {
            return "ERROR DE VALIDACIÓN: " + e.getMessage();
        } catch (Exception e) {
            return "ERROR DE CONEXIÓN: La base de datos no está disponible (" + e.getMessage() + "). No se guardaron cambios.";
        }
    }

    public String registrarSancionAdministrativa(Long idJugador, int partidosSuspension, String motivo,
                                                 String gravedad, Usuario operador) {
        try {
            Sancion sancion = disciplinaService.registrarSancionAdministrativa(
                    idJugador, partidosSuspension, motivo, gravedad, operador);
            return "ÉXITO: Sanción administrativa registrada (ID " + sancion.getId()
                    + ") y derivada al tribunal para su fallo.";
        } catch (TorneoExcepcion e) {
            return "ERROR DE VALIDACIÓN: " + e.getMessage();
        } catch (Exception e) {
            return "ERROR DE CONEXIÓN: La base de datos no está disponible (" + e.getMessage() + "). No se guardaron cambios.";
        }
    }
}
