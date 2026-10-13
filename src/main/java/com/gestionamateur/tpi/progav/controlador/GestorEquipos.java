package com.gestionamateur.tpi.progav.controlador;

import com.gestionamateur.tpi.progav.modelo.Cancha;
import com.gestionamateur.tpi.progav.modelo.Categoria;
import com.gestionamateur.tpi.progav.modelo.Equipo;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import java.util.ArrayList;
import java.util.List;

public class GestorEquipos {

    private static List<Equipo> dbEquipos = new ArrayList<>();

    // CU 1: Gestionar Equipos (Alta)
    public String registrarEquipo(String nombre, Categoria categoria, String colorCamiseta, Cancha sede, Usuario capitan) {
        
        // 1. Validaciones previas de la capa Controlador
        if (nombre == null || nombre.trim().isEmpty()) {
            return "ERROR: El nombre del equipo no puede estar vacío.";
        }

        // Regla de Negocio del Controlador: Verificar disponibilidad de nombre en la misma categoría
        for (Equipo e : dbEquipos) {
            if (e.getNombre().equalsIgnoreCase(nombre) && e.getCategoria().getNombre().equals(categoria.getNombre())) {
                return "ERROR: Ya existe un equipo llamado '" + nombre + "' en la categoría " + categoria.getNombre() + ".";
            }
        }

        // 2. Creación blindada usando el bloque Try-Catch
        try {
            // Si algún dato es inválido (ej. color nulo o sin capitán), el constructor lanzará TorneoExcepcion
            Equipo nuevoEquipo = new Equipo(nombre, categoria, colorCamiseta, sede, capitan);
            
            // Guardado (Simulando la persistencia en BD)
            dbEquipos.add(nuevoEquipo);
            
            return "ÉXITO: El equipo '" + nombre + "' fue registrado correctamente.";
            
        } catch (TorneoExcepcion ex) {
            // Si el Modelo rechaza los datos, atrapamos la excepción y devolvemos su mensaje
            return "ERROR DE VALIDACIÓN: " + ex.getMessage();
        } catch (Exception e) {
            // Capturamos cualquier otro error inesperado (NullPointer, etc.)
            return "ERROR CRÍTICO: Ocurrió un problema inesperado al registrar el equipo (" + e.getMessage() + ").";
        }
    }

    public static List<Equipo> getEquiposRegistrados() {
        return dbEquipos;
    }
}