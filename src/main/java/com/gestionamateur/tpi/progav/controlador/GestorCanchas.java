package com.gestionamateur.tpi.progav.controlador;

import com.gestionamateur.tpi.progav.dao.CanchaDAO;
import com.gestionamateur.tpi.progav.dao.ConexionBD;
import com.gestionamateur.tpi.progav.modelo.Cancha;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.seguridad.PoliticaPermisos;
import java.sql.Connection;
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

    private void validarOperadorCanchas(Usuario operador, String accion) throws TorneoExcepcion {
        if (!PoliticaPermisos.tienePermisoOperacion(operador, PoliticaPermisos.ModuloMenu.GESTIONAR_CANCHAS)) {
            throw new TorneoExcepcion("Acceso denegado: se requiere un Administrador u Organizador autenticado para " + accion + ".");
        }
    }

    public String registrarCancha(String nombre, String direccion, String superficie) throws TorneoExcepcion {
        return registrarCancha(nombre, direccion, superficie, null);
    }

    public String registrarCancha(String nombre, String direccion, String superficie, Usuario operador) throws TorneoExcepcion {
        validarOperadorCanchas(operador, "registrar canchas");
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new TorneoExcepcion("El nombre de la cancha es obligatorio.");
        }
        if (direccion == null || direccion.trim().isEmpty()) {
            throw new TorneoExcepcion("Debe especificar la dirección de la cancha.");
        }

        try {
            Cancha nuevaCancha = new Cancha(nombre.trim(), direccion.trim(), superficie);
            if (canchaDAO.existePorNombre(nombre.trim())) {
                throw new TorneoExcepcion("Ya existe una cancha registrada con ese nombre.");
            }
            Long id = canchaDAO.insertar(nuevaCancha);
            nuevaCancha.setIdCancha(id);
            dbCanchas.add(nuevaCancha);
            LOGGER.info("Cancha registrada en base de datos: " + nombre + " (ID: " + id + ")");
            return "ÉXITO: Cancha '" + nombre.trim() + "' agregada al catálogo del torneo en base de datos (ID: " + id + ").";
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Fallo de persistencia JDBC al registrar cancha: " + e.getMessage());
            // Sin fallback engañoso a memoria
            throw new TorneoExcepcion("ERROR DE CONEXIÓN: La base de datos no está disponible (" + e.getMessage() + "). No se guardaron cambios.");
        }
    }

    /** CU-16: edición integral de nombre, dirección y superficie de una cancha existente. */
    public String actualizarCancha(Long idCancha, String nombre, String direccion, String superficie, Usuario operador) throws TorneoExcepcion {
        validarOperadorCanchas(operador, "editar canchas");
        if (idCancha == null || idCancha <= 0) {
            throw new TorneoExcepcion("Debe indicar la cancha a editar.");
        }
        String nombreLimpio = nombre == null ? "" : nombre.trim();
        String dirLimpia = direccion == null ? "" : direccion.trim();
        String supLimpia = superficie == null ? "" : superficie.trim();
        Cancha editada = new Cancha(nombreLimpio, dirLimpia, supLimpia);
        editada.setIdCancha(idCancha);

        try (Connection conn = ConexionBD.obtenerConexion()) {
            Cancha existente = canchaDAO.buscarPorId(conn, idCancha);
            if (existente == null) {
                throw new TorneoExcepcion("La cancha indicada no existe.");
            }
            if (canchaDAO.existePorNombreExcluyendoId(conn, nombreLimpio, idCancha)) {
                throw new TorneoExcepcion("Ya existe otra cancha registrada con ese nombre.");
            }
            canchaDAO.actualizar(conn, editada);
            LOGGER.info("Cancha actualizada en base de datos ID " + idCancha + " por " + operador.getNombreUsuario());
            return "ÉXITO: Cancha '" + nombreLimpio + "' actualizada correctamente (ID: " + idCancha + ").";
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Fallo de persistencia JDBC al actualizar cancha: " + e.getMessage());
            throw new TorneoExcepcion("ERROR DE CONEXIÓN: La base de datos no está disponible (" + e.getMessage() + "). No se guardaron cambios.");
        }
    }

    /** CU-16 (alternativa A1): habilita o da de baja lógica una cancha preservando historial. */
    public String cambiarEstado(Long idCancha, boolean activa, Usuario operador) {
        try {
            validarOperadorCanchas(operador, "modificar canchas");
        } catch (TorneoExcepcion e) {
            return "ERROR DE VALIDACIÓN: " + e.getMessage();
        }
        if (idCancha == null || idCancha <= 0) {
            return "ERROR: Debe seleccionar la cancha a modificar.";
        }
        try {
            Cancha cancha = canchaDAO.buscarPorId(idCancha);
            if (cancha == null) {
                return "ERROR: La cancha indicada no existe.";
            }
            try (Connection conn = ConexionBD.obtenerConexion()) {
                canchaDAO.actualizarActiva(conn, idCancha, activa);
            }
            return activa
                    ? "ÉXITO: La cancha '" + cancha.getNombre() + "' vuelve a estar habilitada para la programación de partidos."
                    : "ÉXITO: La cancha '" + cancha.getNombre() + "' quedó inhabilitada para la programación de partidos.";
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fallo al actualizar el estado de la cancha: " + e.getMessage());
            return "ERROR DE CONEXIÓN: La base de datos no está disponible (" + e.getMessage() + "). No se guardaron cambios.";
        }
    }

    /** CU-16: listado autorizado de canchas (incluye inhabilitadas) para gestión. */
    public List<Cancha> listarCanchasParaGestion(Usuario operador) throws TorneoExcepcion {
        validarOperadorCanchas(operador, "consultar el catálogo de canchas");
        try {
            return canchaDAO.buscarTodasIncluyendoInactivas();
        } catch (SQLException e) {
            throw new TorneoExcepcion("No se pudieron cargar las canchas desde la base de datos: " + e.getMessage(), e);
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
