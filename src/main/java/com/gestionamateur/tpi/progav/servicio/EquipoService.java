package com.gestionamateur.tpi.progav.servicio;

import com.gestionamateur.tpi.progav.dao.CategoriaDAO;
import com.gestionamateur.tpi.progav.dao.TorneoDAO;
import com.gestionamateur.tpi.progav.dao.ConexionBD;
import com.gestionamateur.tpi.progav.dao.EquipoDAO;
import com.gestionamateur.tpi.progav.dao.TorneoDAO;
import com.gestionamateur.tpi.progav.dao.JugadorDAO;
import com.gestionamateur.tpi.progav.dao.TorneoDAO;
import com.gestionamateur.tpi.progav.dao.SolicitudInscripcionDAO;
import com.gestionamateur.tpi.progav.dao.TorneoDAO;
import com.gestionamateur.tpi.progav.dao.UsuarioDAO;
import com.gestionamateur.tpi.progav.dao.TorneoDAO;
import com.gestionamateur.tpi.progav.modelo.Cancha;
import com.gestionamateur.tpi.progav.modelo.Categoria;
import com.gestionamateur.tpi.progav.modelo.Equipo;
import com.gestionamateur.tpi.progav.modelo.Jugador;
import com.gestionamateur.tpi.progav.modelo.SistemaLogs;
import com.gestionamateur.tpi.progav.modelo.SolicitudInscripcion;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.seguridad.PoliticaPermisos;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

public class EquipoService {

    private static final Logger LOGGER = Logger.getLogger(EquipoService.class.getName());
    private final EquipoDAO equipoDAO;
    private final JugadorDAO jugadorDAO;
    private final SolicitudInscripcionDAO solicitudDAO;
    private final CategoriaDAO categoriaDAO;
    private final UsuarioDAO usuarioDAO;

    public EquipoService() {
        this.equipoDAO = new EquipoDAO();
        this.jugadorDAO = new JugadorDAO();
        this.solicitudDAO = new SolicitudInscripcionDAO();
        this.categoriaDAO = new CategoriaDAO();
        this.usuarioDAO = new UsuarioDAO();
    }

    public EquipoService(EquipoDAO equipoDAO, JugadorDAO jugadorDAO, SolicitudInscripcionDAO solicitudDAO,
                         CategoriaDAO categoriaDAO, UsuarioDAO usuarioDAO) {
        this.equipoDAO = equipoDAO;
        this.jugadorDAO = jugadorDAO;
        this.solicitudDAO = solicitudDAO;
        this.categoriaDAO = categoriaDAO;
        this.usuarioDAO = usuarioDAO;
    }

    public Equipo registrarEquipo(String nombre, String alias, Long idCategoria, String colorCamiseta,
                                  Cancha sedeHabitual, Long idCapitan) throws TorneoExcepcion {
        return registrarEquipo(nombre, alias, idCategoria, colorCamiseta, sedeHabitual, idCapitan, null);
    }

    public Equipo registrarEquipo(String nombre, String alias, Long idCategoria, String colorCamiseta,
                                  Cancha sedeHabitual, Long idCapitan, Usuario operador) throws TorneoExcepcion {
        if (!PoliticaPermisos.esOperadorPersistidoValido(operador)) {
            throw new TorneoExcepcion("Acceso denegado: se requiere un operador autenticado para registrar equipos.");
        }
        TiposDatos.Rol rol = operador.getRol();
        if (!PoliticaPermisos.tienePermisoOperacion(operador, PoliticaPermisos.ModuloMenu.GESTIONAR_EQUIPOS)) {
            throw new TorneoExcepcion("Acceso denegado: El rol " + rol + " no tiene permisos para registrar equipos.");
        }
        if (rol == TiposDatos.Rol.CAPITAN && !operador.getId().equals(idCapitan)) {
            throw new TorneoExcepcion("Acceso denegado: Un capitán sólo puede registrar un equipo designándose a sí mismo como capitán.");
        }
        try (Connection conn = ConexionBD.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                Categoria cat = categoriaDAO.buscarPorId(conn, idCategoria);
                if (cat == null) {
                    throw new TorneoExcepcion("La categoría seleccionada no existe.");
                }

                if (equipoDAO.existePorNombreYCategoria(conn, nombre.trim(), idCategoria, null)) {
                    throw new TorneoExcepcion("Ya existe un equipo con ese nombre en la categoría seleccionada.");
                }

                Usuario cap = usuarioDAO.buscarPorId(conn, idCapitan);
                if (cap == null) {
                    throw new TorneoExcepcion("El usuario capitán especificado no existe.");
                }

                Equipo equipo = new Equipo(nombre, cat, colorCamiseta, cap);
                equipo.setAlias(alias != null && !alias.trim().isEmpty() ? alias.trim() : null);
                equipo.setSedeHabitual(sedeHabitual);
                equipo.setActivo(true);

                Long idEquipo = equipoDAO.insertar(conn, equipo, idCapitan);
                equipo.setIdEquipo(idEquipo);

                // Crear automáticamente solicitud de inscripción pendiente
                SolicitudInscripcion sol = new SolicitudInscripcion(equipo);
                solicitudDAO.insertar(conn, sol, idEquipo);

                conn.commit();
                SistemaLogs.info(EquipoService.class, "Equipo registrado con éxito: " + nombre + " (ID: " + idEquipo + ")");
                return equipo;
            } catch (Exception e) {
                conn.rollback();
                LOGGER.severe("Error al registrar equipo, rollback: " + e.getMessage());
                throw new TorneoExcepcion("No se pudo completar el registro del equipo: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error de conexión a la base de datos.", e);
        }
    }

    /** Torneo vigente (el mas reciente que no este FINALIZADO ni CANCELADO). */
    private Long resolverTorneoVigente(Connection conn) throws SQLException {
        List<com.gestionamateur.tpi.progav.modelo.Torneo> torneos = new TorneoDAO().buscarTodos(conn);
        for (com.gestionamateur.tpi.progav.modelo.Torneo t : torneos) {
            if (!"FINALIZADO".equalsIgnoreCase(t.getEstado()) && !"CANCELADO".equalsIgnoreCase(t.getEstado())) {
                return t.getIdTorneo();
            }
        }
        return torneos.isEmpty() ? null : torneos.get(0).getIdTorneo();
    }

    /**
     * CU-01 (alternativa A1): edicion de un equipo ya registrado.
     */
    public Equipo actualizarEquipo(Long idEquipo, String nombre, String alias, Long idCategoria, String colorCamiseta,
                                   Long idCapitan, Cancha sedeHabitual, Usuario operador) throws TorneoExcepcion {
        if (!PoliticaPermisos.esOperadorPersistidoValido(operador)) {
            throw new TorneoExcepcion("Acceso denegado: se requiere un operador autenticado para editar equipos.");
        }
        TiposDatos.Rol rol = operador.getRol();
        if (!PoliticaPermisos.tienePermisoOperacion(operador, PoliticaPermisos.ModuloMenu.GESTIONAR_EQUIPOS)) {
            throw new TorneoExcepcion("Acceso denegado: El rol " + rol + " no tiene permisos para editar equipos.");
        }
        if (rol == TiposDatos.Rol.CAPITAN && !operador.getId().equals(idCapitan)) {
            throw new TorneoExcepcion("Acceso denegado: Un capitán sólo puede editar el equipo que captainea.");
        }
        if (idEquipo == null) {
            throw new TorneoExcepcion("Debe indicar el equipo a editar.");
        }

        try (Connection conn = ConexionBD.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                Equipo existente = equipoDAO.buscarPorId(conn, idEquipo);
                if (existente == null) {
                    throw new TorneoExcepcion("El equipo indicado no existe.");
                }
                // DEF-001 (control de pertenencia de recursos, ADR-19):
                // la pertenencia se valida contra la titularidad PERSISTIDA del equipo,
                // nunca contra el captain que la interfaz propone asignar.
                if (rol == TiposDatos.Rol.CAPITAN) {
                    Usuario capitanActual = existente.getCapitan();
                    if (capitanActual == null || capitanActual.getId() == null
                            || !capitanActual.getId().equals(operador.getId())) {
                        throw new TorneoExcepcion("Acceso denegado: Un capitán sólo puede editar el equipo que captainea. "
                                + "El equipo indicado pertenece a otro capitán.");
                    }
                }
                Categoria cat = categoriaDAO.buscarPorId(conn, idCategoria);
                if (cat == null) {
                    throw new TorneoExcepcion("La categoría seleccionada no existe.");
                }
                if (equipoDAO.existePorNombreYCategoria(conn, nombre.trim(), idCategoria, idEquipo)) {
                    throw new TorneoExcepcion("Ya existe otro equipo con ese nombre en la categoría seleccionada.");
                }
                Usuario cap = usuarioDAO.buscarPorId(conn, idCapitan);
                if (cap == null) {
                    throw new TorneoExcepcion("El usuario capitán especificado no existe.");
                }

                // DEF-005: si el llamante pasa null, se preserva el alias persistido;
                // si pasa un texto explícito, se actualiza o limpia si está vacío.
                String aliasEfectivo;
                if (alias == null) {
                    aliasEfectivo = existente.getAlias();
                } else {
                    String limpio = alias.trim();
                    aliasEfectivo = limpio.isEmpty() ? null : limpio;
                }

                Equipo equipo = new Equipo(nombre.trim(), cat, colorCamiseta, cap);
                equipo.setIdEquipo(idEquipo);
                equipo.setAlias(aliasEfectivo);
                equipo.setSedeHabitual(sedeHabitual);
                equipo.setActivo(existente.isActivo());
                equipoDAO.actualizar(conn, equipo, idCapitan);

                conn.commit();
                SistemaLogs.info(EquipoService.class, "Equipo actualizado: " + nombre + " (ID: " + idEquipo + ")");
                return equipo;
            } catch (Exception e) {
                conn.rollback();
                LOGGER.severe("Error al actualizar equipo, rollback: " + e.getMessage());
                throw new TorneoExcepcion("No se pudo actualizar el equipo: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error de conexión a la base de datos.", e);
        }
    }

    /** CU-01: baja lógica o reactivación de un equipo preservando historial deportivo. */
    public Equipo cambiarEstadoEquipo(Long idEquipo, boolean activo, Usuario operador) throws TorneoExcepcion {
        if (!PoliticaPermisos.esOperadorPersistidoValido(operador)) {
            throw new TorneoExcepcion("Acceso denegado: se requiere un operador autenticado para cambiar el estado de un equipo.");
        }
        TiposDatos.Rol rol = operador.getRol();
        if (!PoliticaPermisos.tienePermisoOperacion(operador, PoliticaPermisos.ModuloMenu.GESTIONAR_EQUIPOS)) {
            throw new TorneoExcepcion("Acceso denegado: El rol " + rol + " no tiene permisos para modificar equipos.");
        }
        if (idEquipo == null || idEquipo <= 0) {
            throw new TorneoExcepcion("Debe indicar el equipo a modificar.");
        }
        try (Connection conn = ConexionBD.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                Equipo existente = equipoDAO.buscarPorId(conn, idEquipo);
                if (existente == null) {
                    throw new TorneoExcepcion("El equipo indicado no existe.");
                }
                if (rol == TiposDatos.Rol.CAPITAN) {
                    Usuario capitanActual = existente.getCapitan();
                    if (capitanActual == null || capitanActual.getId() == null
                            || !capitanActual.getId().equals(operador.getId())) {
                        throw new TorneoExcepcion("Acceso denegado: Un capitán sólo puede dar de baja o reactivar su propio equipo.");
                    }
                }
                if (!activo && equipoDAO.contarPartidosEnCursoPorEquipo(conn, idEquipo) > 0) {
                    throw new TorneoExcepcion("No se puede dar de baja el equipo porque posee partidos PROGRAMADO o EN_CURSO pendientes.");
                }
                equipoDAO.actualizarActivo(conn, idEquipo, activo);
                existente.setActivo(activo);
                conn.commit();
                SistemaLogs.info(EquipoService.class, "Estado de equipo ID " + idEquipo + " -> activo=" + activo + " por " + operador.getNombreUsuario());
                return existente;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof TorneoExcepcion) {
                    throw (TorneoExcepcion) e;
                }
                throw new TorneoExcepcion("No se pudo cambiar el estado del equipo: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error de conexión al modificar estado del equipo.", e);
        }
    }

    /** Consulta autorizada de equipos según alcance del rol (incluye inactivos para gestión). */
    public List<Equipo> listarEquiposParaGestion(Usuario operador) throws TorneoExcepcion {
        if (!PoliticaPermisos.tienePermisoOperacion(operador, PoliticaPermisos.ModuloMenu.GESTIONAR_EQUIPOS)) {
            throw new TorneoExcepcion("Acceso denegado: se requiere sesión autorizada para gestionar equipos.");
        }
        try {
            if (operador.getRol() == TiposDatos.Rol.CAPITAN) {
                return equipoDAO.buscarListaPorCapitan(operador.getId(), true);
            }
            return equipoDAO.buscarTodosIncluyendoInactivos();
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al consultar los equipos registrados.", e);
        }
    }

    public void agregarJugadorAPlantel(Long idEquipo, Long idJugador) throws TorneoExcepcion {
        agregarJugadorAPlantel(idEquipo, idJugador, null);
    }

    public void agregarJugadorAPlantel(Long idEquipo, Long idJugador, Usuario operador) throws TorneoExcepcion {
        if (!PoliticaPermisos.esOperadorPersistidoValido(operador)) {
            throw new TorneoExcepcion("Acceso denegado: se requiere un operador autenticado para agregar jugadores a un plantel.");
        }
        TiposDatos.Rol rol = operador.getRol();
        if (!PoliticaPermisos.tienePermisoOperacion(operador, PoliticaPermisos.ModuloMenu.GESTIONAR_EQUIPOS)) {
            throw new TorneoExcepcion("Acceso denegado: El rol " + rol + " no tiene permisos para agregar jugadores a un plantel.");
        }
        try (Connection conn = ConexionBD.obtenerConexion()) {
            // CU-02.1: control de suspensiones vigentes antes de incorporar al plantel.
            Long idTorneo = resolverTorneoVigente(conn);
            if (idTorneo != null && !new DisciplinaService().esJugadorHabilitado(idJugador, idTorneo)) {
                throw new TorneoExcepcion("El jugador no se encuentra habilitado: tiene una sanción vigente "
                        + "o no está en estado activo.");
            }
            Equipo eq = equipoDAO.buscarPorId(conn, idEquipo);
            if (eq == null) {
                throw new TorneoExcepcion("El equipo no existe.");
            }
            if (!eq.isActivo()) {
                throw new TorneoExcepcion("El equipo '" + eq.getNombre() + "' está dado de baja y no admite altas de plantel.");
            }
            if (rol == TiposDatos.Rol.CAPITAN) {
                if (eq.getCapitan() == null || eq.getCapitan().getId() == null || !operador.getId().equals(eq.getCapitan().getId())) {
                    throw new TorneoExcepcion("Acceso denegado: Un capitán sólo puede incorporar jugadores al plantel de su propio equipo.");
                }
            }
            Jugador j = jugadorDAO.buscarPorId(conn, idJugador);
            if (j == null) {
                throw new TorneoExcepcion("El jugador no existe.");
            }
            jugadorDAO.asignarEquipo(conn, idJugador, idEquipo);
            SistemaLogs.info(EquipoService.class, "Jugador " + j.getNombre() + " asignado al plantel de " + eq.getNombre());
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al asignar jugador al plantel: " + e.getMessage(), e);
        }
    }

    public List<Jugador> obtenerPlantel(Long idEquipo) throws TorneoExcepcion {
        try {
            return jugadorDAO.buscarPorEquipo(idEquipo);
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al obtener plantel del equipo.", e);
        }
    }

    public List<Equipo> listarTodos() throws TorneoExcepcion {
        try {
            return equipoDAO.buscarTodos();
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al listar equipos.", e);
        }
    }

    public Equipo buscarPorId(Long idEquipo) throws TorneoExcepcion {
        try {
            return equipoDAO.buscarPorId(idEquipo);
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al buscar equipo por ID.", e);
        }
    }

    public void resolverSolicitudInscripcion(Long idSolicitud, boolean aprobada, String motivo) throws TorneoExcepcion {
        resolverSolicitudInscripcion(idSolicitud, aprobada, motivo, null);
    }

    public void resolverSolicitudInscripcion(Long idSolicitud, boolean aprobada, String motivo, Usuario operador) throws TorneoExcepcion {
        if (!PoliticaPermisos.esOperadorPersistidoValido(operador)) {
            throw new TorneoExcepcion("Acceso denegado: se requiere un operador autenticado para resolver solicitudes de inscripción.");
        }
        TiposDatos.Rol rol = operador.getRol();
        if (rol != TiposDatos.Rol.ADMINISTRADOR && rol != TiposDatos.Rol.ORGANIZADOR) {
            throw new TorneoExcepcion("Acceso denegado: Sólo ADMINISTRADOR u ORGANIZADOR pueden resolver solicitudes de inscripción.");
        }
        try (Connection conn = ConexionBD.obtenerConexion()) {
            SolicitudInscripcion sol = solicitudDAO.buscarPorId(conn, idSolicitud);
            if (sol == null) {
                throw new TorneoExcepcion("La solicitud de inscripción no existe.");
            }
            String nuevoEstado = aprobada ? "APROBADA" : "RECHAZADA";
            solicitudDAO.actualizarEstado(conn, idSolicitud, nuevoEstado, motivo);
            SistemaLogs.info(EquipoService.class, "Solicitud de inscripción " + idSolicitud + " resuelta: " + nuevoEstado);
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al resolver solicitud de inscripción: " + e.getMessage(), e);
        }
    }
}
