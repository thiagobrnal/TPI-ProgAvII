package com.gestionamateur.tpi.progav.controlador;

import com.gestionamateur.tpi.progav.dao.CanchaDAO;
import com.gestionamateur.tpi.progav.modelo.Cancha;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GestorCanchas {
    
    private static final Logger LOGGER = Logger.getLogger(GestorCanchas.class.getName());
    private static final List<Cancha> dbCanchas = new ArrayList<>();
    private final CanchaDAO canchaDAO = new CanchaDAO();

    public GestorCanchas() {
    }

    public String registrarCancha(String nombre, String direccion, String superficie) throws TorneoExcepcion {
        return registrarCancha(nombre, direccion, superficie, null);
    }

    public String registrarCancha(String nombre, String direccion, String superficie, Usuario operador) throws TorneoExcepcion {
        if (operador != null) {
            TiposDatos.Rol rol = operador.getRol();
            if (rol != TiposDatos.Rol.ADMINISTRADOR && rol != TiposDatos.Rol.ORGANIZADOR) {
                throw new TorneoExcepcion("Acceso denegado: El rol " + rol + " no tiene permisos para registrar sedes/canchas.");
            }
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new TorneoExcepcion("El nombre de la sede es obligatorio.");
        }
        if (direccion == null || direccion.trim().isEmpty()) {
            throw new TorneoExcepcion("Debe especificar la dirección de la cancha.");
        }

        try {
            Cancha nuevaCancha = new Cancha(nombre.trim(), direccion.trim(), superficie);
            if (canchaDAO.existePorNombre(nombre.trim())) {
                throw new TorneoExcepcion("Ya existe una sede registrada con ese nombre.");
            }
            Long id = canchaDAO.insertar(nuevaCancha);
            nuevaCancha.setIdCancha(id);
            dbCanchas.add(nuevaCancha);
            LOGGER.info("Sede registrada en base de datos: " + nombre + " (ID: " + id + ")");
            return "ÉXITO: Cancha '" + nombre + "' agregada al catálogo del torneo en base de datos (ID: " + id + ").";
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Fallo de persistencia JDBC al registrar cancha: " + e.getMessage());
            // Sin fallback engañoso a memoria
            throw new TorneoExcepcion("ERROR DE CONEXIÓN: La base de datos no está disponible (" + e.getMessage() + "). No se guardaron cambios.");
        }
    }

    /** CU-16 (alternativa A1): habilita o inhabilita una sede. */
    public String cambiarEstado(Long idCancha, boolean activa, Usuario operador) {
        if (operador == null) {
            return "ERROR DE VALIDACIÓN: Acceso denegado: Se requiere un operador autenticado para modificar sedes.";
        }
        TiposDatos.Rol rol = operador.getRol();
        if (rol != TiposDatos.Rol.ADMINISTRADOR && rol != TiposDatos.Rol.ORGANIZADOR) {
            return "ERROR DE VALIDACIÓN: Acceso denegado: El rol " + rol + " no tiene permisos para modificar sedes/canchas.";
        }
        try {
            Cancha cancha = canchaDAO.buscarPorId(idCancha);
            if (cancha == null) {
                return "ERROR: La sede indicada no existe.";
            }
            try (java.sql.Connection conn = com.gestionamateur.tpi.progav.dao.ConexionBD.obtenerConexion()) {
                canchaDAO.actualizarActiva(conn, idCancha, activa);
            }
            return activa
                    ? "ÉXITO: La sede '" + cancha.getNombre() + "' vuelve a estar disponible para la programación de partidos."
                    : "ÉXITO: La sede '" + cancha.getNombre() + "' quedó inhabilitada para la programación de partidos.";
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Fallo al actualizar el estado de la cancha: " + e.getMessage());
            return "ERROR DE CONEXIÓN: La base de datos no está disponible (" + e.getMessage() + "). No se guardaron cambios.";
        }
    }

    /** CU-16: listado de sedes, incluidas las inhabilitadas. */
    public static List<Cancha> getCanchasRegistradasConInactivas() {
        try {
            List<Cancha> lista = new CanchaDAO().buscarTodasIncluyendoInactivas();
            if (lista != null && !lista.isEmpty()) {
                return lista;
            }
        } catch (Exception e) {
            LOGGER.warning("No se pudieron cargar las canchas desde la base de datos: " + e.getMessage());
        }
        return dbCanchas;
    }

    public static List<Cancha> getCanchasRegistradas() {
        try {
            CanchaDAO dao = new CanchaDAO();
            List<Cancha> listaBD = dao.buscarTodas();
            if (listaBD != null && !listaBD.isEmpty()) {
                return listaBD;
            }
        } catch (Exception e) {
            LOGGER.warning("No se pudieron cargar canchas desde la base de datos: " + e.getMessage());
        }
        return dbCanchas;
    }
}
