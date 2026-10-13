package com.gestionamateur.tpi.progav.controlador;

import com.gestionamateur.tpi.progav.modelo.Equipo;
import com.gestionamateur.tpi.progav.modelo.Jugador;
import com.gestionamateur.tpi.progav.modelo.SolicitudInscripcion;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import java.util.ArrayList;
import java.util.List;

public class GestorInscripciones {

    private static List<SolicitudInscripcion> dbSolicitudes = new ArrayList<>();

    // CU 6: Registrar equipo y plantel
    public String procesarInscripcion(Equipo equipo, List<Jugador> plantelCargado) {
        
        // 1. Validaciones previas de la capa Controlador (Reglas del torneo)
        if (plantelCargado == null || plantelCargado.size() < 7) {
            return "ERROR: El plantel no cumple con el mínimo de jugadores requeridos (7) para iniciar el trámite.";
        }

        try {
            // 2. Vinculamos el plantel validado al equipo
            equipo.setPlantel(plantelCargado);

            // 3. Creación blindada: El constructor ya exige el equipo y setea el estado inicial en "PENDIENTE"
            SolicitudInscripcion solicitud = new SolicitudInscripcion(equipo);
            
            // Guardado (Simulando la persistencia en base de datos)
            dbSolicitudes.add(solicitud);
            
            return "ÉXITO: Solicitud de inscripción enviada a la organización. Te notificaremos el resultado de la evaluación.";
            
        } catch (TorneoExcepcion ex) {
            // Si el Modelo rechaza la creación (ej. equipo nulo), atrapamos el error
            return "ERROR DE VALIDACIÓN: " + ex.getMessage();
        } catch (Exception e) {
            return "ERROR CRÍTICO: Ocurrió un problema inesperado en el sistema (" + e.getMessage() + ").";
        }
    }

    // CU 7: Consultar estado de inscripción
    public String obtenerEstadoInscripcion(Equipo equipo) {
        if (equipo == null) {
            return "ERROR: El equipo especificado no es válido.";
        }
        
        for (SolicitudInscripcion sol : dbSolicitudes) {
            // Comparamos ignorando mayúsculas/minúsculas para evitar falsos negativos
            if (sol.getEquipoSolicitante().getNombre().equalsIgnoreCase(equipo.getNombre())) {
                // Aprovechamos para devolver también el motivo (útil si fue rechazada o aprobada)
                return "ESTADO ACTUAL: " + sol.getEstado() + " | Detalle: " + sol.getMotivo();
            }
        }
        
        return "ERROR: No se encontró ninguna solicitud de inscripción registrada para este equipo.";
    }
}