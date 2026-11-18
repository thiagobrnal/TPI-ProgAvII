package com.gestionamateur.tpi.progav.controlador;

import com.gestionamateur.tpi.progav.dao.EquipoDAO;
import com.gestionamateur.tpi.progav.modelo.Cancha;
import com.gestionamateur.tpi.progav.modelo.Categoria;
import com.gestionamateur.tpi.progav.modelo.Equipo;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.servicio.EquipoService;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GestorEquipos {

    private static final Logger LOGGER = Logger.getLogger(GestorEquipos.class.getName());
    private static final List<Equipo> dbEquipos = new ArrayList<>();
    private final EquipoService equipoService = new EquipoService();
    private final EquipoDAO equipoDAO = new EquipoDAO();

    public GestorEquipos() {
    }

    // CU 1: Gestionar Equipos (Alta con persistencia estricta)
    public String registrarEquipo(String nombre, Categoria categoria, String colorCamiseta, Cancha sede, Usuario capitan) {
        return registrarEquipo(nombre, null, categoria, colorCamiseta, sede, capitan, null);
    }

    public String registrarEquipo(String nombre, Categoria categoria, String colorCamiseta, Cancha sede, Usuario capitan, Usuario operador) {
        return registrarEquipo(nombre, null, categoria, colorCamiseta, sede, capitan, operador);
    }

    public String registrarEquipo(String nombre, String alias, Categoria categoria, String colorCamiseta, Cancha sede, Usuario capitan, Usuario operador) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return "ERROR: El nombre del equipo no puede estar vacío.";
        }
        if (categoria == null || categoria.getId() == null) {
            return "ERROR: Debe seleccionar una categoría válida.";
        }
        if (capitan == null || capitan.getId() == null) {
            return "ERROR: Debe asignar un capitán con cuenta de usuario registrada.";
        }

        try {
            Equipo eq = equipoService.registrarEquipo(nombre.trim(), alias, categoria.getId(), colorCamiseta, sede, capitan.getId(), operador);
            dbEquipos.add(eq);
            return "ÉXITO: El equipo '" + nombre + "' fue registrado correctamente en la base de datos (ID: " + eq.getIdEquipo() + ").";
        } catch (TorneoExcepcion te) {
            return "ERROR DE VALIDACIÓN: " + te.getMessage();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fallo de persistencia JDBC al registrar equipo: " + e.getMessage());
            // Sin fallback engañoso a memoria
            return "ERROR DE CONEXIÓN: La base de datos no está disponible (" + e.getMessage() + "). No se guardaron cambios.";
        }
    }

    /** CU-01 (alternativa A1): edicion de un equipo registrado. */
    public String actualizarEquipo(Long idEquipo, String nombre, Categoria categoria, String colorCamiseta,
                                   Cancha sede, Usuario capitan, Usuario operador) {
        return actualizarEquipo(idEquipo, nombre, null, categoria, colorCamiseta, sede, capitan, operador);
    }

    /** CU-01 (alternativa A1): edicion integral de equipo incluyendo alias. */
    public String actualizarEquipo(Long idEquipo, String nombre, String alias, Categoria categoria, String colorCamiseta,
                                   Cancha sede, Usuario capitan, Usuario operador) {
        if (idEquipo == null) {
            return "ERROR: Debe indicar el equipo a editar.";
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            return "ERROR: El nombre del equipo no puede estar vacío.";
        }
        if (categoria == null || categoria.getId() == null) {
            return "ERROR: Debe seleccionar una categoría válida.";
        }
        if (capitan == null || capitan.getId() == null) {
            return "ERROR: Debe asignar un capitán con cuenta de usuario registrada.";
        }
        try {
            equipoService.actualizarEquipo(idEquipo, nombre.trim(), alias, categoria.getId(),
                    colorCamiseta, capitan.getId(), sede, operador);
            return "ÉXITO: El equipo '" + nombre + "' fue actualizado correctamente (ID: " + idEquipo + ").";
        } catch (TorneoExcepcion te) {
            return "ERROR DE VALIDACIÓN: " + te.getMessage();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fallo de persistencia JDBC al actualizar equipo: " + e.getMessage());
            return "ERROR DE CONEXIÓN: La base de datos no está disponible (" + e.getMessage() + "). No se guardaron cambios.";
        }
    }

    /** CU-01: baja lógica o reactivación de un equipo preservando su historial. */
    public String cambiarEstadoEquipo(Long idEquipo, boolean activo, Usuario operador) {
        try {
            Equipo eq = equipoService.cambiarEstadoEquipo(idEquipo, activo, operador);
            return "ÉXITO: El equipo '" + eq.getNombre() + "' quedó " + (activo ? "ACTIVO." : "INACTIVO (baja lógica).");
        } catch (TorneoExcepcion te) {
            return "ERROR DE VALIDACIÓN: " + te.getMessage();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fallo de persistencia JDBC al cambiar estado de equipo: " + e.getMessage());
            return "ERROR DE CONEXIÓN: La base de datos no está disponible (" + e.getMessage() + "). No se guardaron cambios.";
        }
    }

    public List<Equipo> listarEquiposParaGestion(Usuario operador) throws TorneoExcepcion {
        return equipoService.listarEquiposParaGestion(operador);
    }

    public List<Equipo> buscarEquiposActivosDeCapitan(Long idCapitan) {
        try {
            return equipoDAO.buscarListaPorCapitan(idCapitan, false);
        } catch (Exception e) {
            LOGGER.warning("No se pudieron consultar los equipos del capitán: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Equipo buscarEquipoDeCapitan(Long idCapitan) {
        try {
            return equipoDAO.buscarPorCapitan(idCapitan);
        } catch (Exception e) {
            LOGGER.warning("No se pudo buscar equipo del capitán: " + e.getMessage());
            return null;
        }
    }

    public static List<Equipo> getEquiposRegistrados() {
        try {
            EquipoDAO dao = new EquipoDAO();
            List<Equipo> listaBD = dao.buscarTodos();
            if (listaBD != null && !listaBD.isEmpty()) {
                return listaBD;
            }
        } catch (Exception e) {
            LOGGER.warning("No se pudieron cargar equipos desde base de datos: " + e.getMessage());
        }
        return dbEquipos;
    }
}
