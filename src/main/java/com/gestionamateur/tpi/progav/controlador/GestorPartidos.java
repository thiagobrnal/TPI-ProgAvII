package com.gestionamateur.tpi.progav.controlador;

import com.gestionamateur.tpi.progav.modelo.Arbitro;
import com.gestionamateur.tpi.progav.modelo.Cancha;
import com.gestionamateur.tpi.progav.modelo.Motivo;
import com.gestionamateur.tpi.progav.modelo.Partido;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class GestorPartidos {

    private static List<Partido> dbPartidos = new ArrayList<>();

    // CU 3: Programar partidos
    public String programarPartido(Date fechaHora, Cancha sede) {
        try {
            Partido nuevoPartido = new Partido(fechaHora, sede);
            
            // CU 3.1: Asignar árbitros según disponibilidad
            Arbitro arbitroAsignado = buscarArbitroDisponible(fechaHora);
            
            if (arbitroAsignado != null) {
                nuevoPartido.setArbitroDesignado(arbitroAsignado);
                dbPartidos.add(nuevoPartido);
                return "ÉXITO: Partido programado. Árbitro designado: " + arbitroAsignado.getNombre();
            } else {
                // Flujo alternativo: No hay árbitros
                dbPartidos.add(nuevoPartido);
                return "ADVERTENCIA: Partido guardado, pero no hay árbitros disponibles. Queda pendiente de asignación.";
            }
        } catch (TorneoExcepcion ex) {
            return "ERROR DE VALIDACIÓN: " + ex.getMessage();
        } catch (Exception e) {
            return "ERROR CRÍTICO: Falló la programación del partido (" + e.getMessage() + ").";
        }
    }

    // Simula la búsqueda de un árbitro libre
    private Arbitro buscarArbitroDisponible(Date fechaHora) {
        // TODO: Filtrar de una lista de Árbitros. Retornamos null para simular que no hay libres.
        return null; 
    }

    // CU 4: Cargar resultados e incidencias
    public String procesarActa(Long idPartido, String golesLocales, String golesVisitantes) {
        try {
            for (Partido p : dbPartidos) {
                if (p.getIdPartido() != null && p.getIdPartido().equals(idPartido)) {
                    p.finalizarPartido();
                    return "ÉXITO: Acta guardada y resultado oficializado.";
                }
            }
            return "ERROR: Partido no encontrado.";
        } catch (TorneoExcepcion ex) {
            return "ERROR DE VALIDACIÓN: " + ex.getMessage();
        } catch (Exception e) {
            return "ERROR CRÍTICO: Falló el procesamiento del acta (" + e.getMessage() + ").";
        }
    }

    // CU 5: Registrar postergaciones o suspensiones
    public String procesarSuspension(Long idPartido, String textoMotivo) {
        try {
            for (Partido p : dbPartidos) {
                if (p.getIdPartido() != null && p.getIdPartido().equals(idPartido)) {
                    Motivo motivo = new Motivo(textoMotivo);
                    p.posponerPartido(motivo);
                    return "ÉXITO: Partido pospuesto formalmente.";
                }
            }
            return "ERROR: No se encontró el partido a suspender.";
        } catch (TorneoExcepcion ex) {
            return "ERROR DE VALIDACIÓN: " + ex.getMessage();
        } catch (Exception e) {
            return "ERROR CRÍTICO: Falló la suspensión del partido (" + e.getMessage() + ").";
        }
    }
}