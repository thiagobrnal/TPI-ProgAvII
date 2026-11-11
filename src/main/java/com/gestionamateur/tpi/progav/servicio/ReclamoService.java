package com.gestionamateur.tpi.progav.servicio;

import com.gestionamateur.tpi.progav.dao.ConfiguracionDAO;
import com.gestionamateur.tpi.progav.dao.ConexionBD;
import com.gestionamateur.tpi.progav.dao.EquipoDAO;
import com.gestionamateur.tpi.progav.dao.JugadorDAO;
import com.gestionamateur.tpi.progav.dao.PartidoDAO;
import com.gestionamateur.tpi.progav.dao.PartidoEquipoDAO;
import com.gestionamateur.tpi.progav.dao.ReclamoDAO;
import com.gestionamateur.tpi.progav.dao.SancionDAO;
import com.gestionamateur.tpi.progav.dao.UsuarioDAO;
import com.gestionamateur.tpi.progav.modelo.Equipo;
import com.gestionamateur.tpi.progav.modelo.Jugador;
import com.gestionamateur.tpi.progav.modelo.Partido;
import com.gestionamateur.tpi.progav.modelo.PartidoEquipo;
import com.gestionamateur.tpi.progav.modelo.Reclamo;
import com.gestionamateur.tpi.progav.modelo.Sancion;
import com.gestionamateur.tpi.progav.modelo.SistemaLogs;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class ReclamoService {

    private static final Logger LOGGER = Logger.getLogger(ReclamoService.class.getName());
    private final ReclamoDAO reclamoDAO;
    private final PartidoDAO partidoDAO;
    private final SancionDAO sancionDAO;
    private final JugadorDAO jugadorDAO;
    private final UsuarioDAO usuarioDAO;
    private final ConfiguracionDAO configuracionDAO;
    private final DisciplinaService disciplinaService;
    private final PartidoEquipoDAO partidoEquipoDAO;
    private final EquipoDAO equipoDAO;

    public ReclamoService() {
        this.reclamoDAO = new ReclamoDAO();
        this.partidoDAO = new PartidoDAO();
        this.sancionDAO = new SancionDAO();
        this.jugadorDAO = new JugadorDAO();
        this.usuarioDAO = new UsuarioDAO();
        this.configuracionDAO = new ConfiguracionDAO();
        this.disciplinaService = new DisciplinaService();
        this.partidoEquipoDAO = new PartidoEquipoDAO();
        this.equipoDAO = new EquipoDAO();
    }

    public ReclamoService(ReclamoDAO reclamoDAO, PartidoDAO partidoDAO, SancionDAO sancionDAO, JugadorDAO jugadorDAO,
                          UsuarioDAO usuarioDAO, ConfiguracionDAO configuracionDAO, DisciplinaService disciplinaService) {
        this.reclamoDAO = reclamoDAO;
        this.partidoDAO = partidoDAO;
        this.sancionDAO = sancionDAO;
        this.jugadorDAO = jugadorDAO;
        this.usuarioDAO = usuarioDAO;
        this.configuracionDAO = configuracionDAO;
        this.disciplinaService = disciplinaService;
        this.partidoEquipoDAO = new PartidoEquipoDAO();
        this.equipoDAO = new EquipoDAO();
    }

    /**
     * CU-13: Presentación de reclamo sobre un partido por un capitán.
     * Plazo perentorio: 72 horas corridas desde la finalización real del encuentro.
     */
    public Reclamo presentarReclamoPartido(Long idPartido, Long idCapitan, String motivo) throws TorneoExcepcion {
        return presentarReclamoPartido(idPartido, idCapitan, motivo, null);
    }

    public Reclamo presentarReclamoPartido(Long idPartido, Long idCapitan, String motivo, Usuario operador) throws TorneoExcepcion {
        if (operador == null) {
            throw new TorneoExcepcion("Acceso denegado: se requiere un operador autenticado para presentar reclamos.");
        }
        TiposDatos.Rol rol = operador.getRol();
        if (rol != TiposDatos.Rol.CAPITAN && rol != TiposDatos.Rol.ADMINISTRADOR && rol != TiposDatos.Rol.ORGANIZADOR) {
            throw new TorneoExcepcion("Acceso denegado: El rol " + rol + " no tiene permisos para presentar reclamos.");
        }
        if (rol == TiposDatos.Rol.CAPITAN && operador.getId() != null && !operador.getId().equals(idCapitan)) {
            throw new TorneoExcepcion("Acceso denegado: El operador autenticado no coincide con el capitán que suscribe el reclamo.");
        }

        if (motivo == null || motivo.trim().length() < 15) {
            throw new TorneoExcepcion("El motivo del reclamo debe ser detallado (al menos 15 caracteres).");
        }

        try (Connection conn = ConexionBD.obtenerConexion()) {
            Partido p = partidoDAO.buscarPorId(conn, idPartido);
            if (p == null) {
                throw new TorneoExcepcion("El partido no existe.");
            }
            if (p.getFechaHoraFinalizacionReal() == null) {
                throw new TorneoExcepcion("No se puede presentar un reclamo sobre un partido que no ha finalizado.");
            }

            int horasLimite = configuracionDAO.obtenerValorInt(conn, "HORAS_LIMITE_RECLAMO", 72);
            boolean dentroDePlazo = reclamoDAO.estaDentroDelPlazoPartido(conn, idPartido, horasLimite);
            if (!dentroDePlazo) {
                throw new TorneoExcepcion("Plazo de reclamo vencido: han transcurrido más de " + horasLimite + " horas corridas desde la finalización del partido.");
            }

            Usuario cap = usuarioDAO.buscarPorId(conn, idCapitan);
            if (cap == null) {
                throw new TorneoExcepcion("El usuario capitán no existe.");
            }

            // Validación de pertenencia de recurso: el capitán debe ser capitán de alguno de los equipos participantes
            PartidoEquipo local = partidoEquipoDAO.buscarLocal(conn, idPartido);
            PartidoEquipo visitante = partidoEquipoDAO.buscarVisitante(conn, idPartido);
            boolean esCapitanLocal = local != null && local.getEquipo() != null && local.getEquipo().getCapitan() != null && idCapitan.equals(local.getEquipo().getCapitan().getId());
            boolean esCapitanVisitante = visitante != null && visitante.getEquipo() != null && visitante.getEquipo().getCapitan() != null && idCapitan.equals(visitante.getEquipo().getCapitan().getId());
            if (!esCapitanLocal && !esCapitanVisitante) {
                throw new TorneoExcepcion("Acceso denegado: El capitán con ID " + idCapitan + " no pertenece a ninguno de los equipos participantes de este encuentro.");
            }

            Reclamo reclamo = new Reclamo(new Date(), motivo, null, p, cap);
            Long idReclamo = reclamoDAO.insertar(conn, reclamo, null, idPartido, idCapitan);
            reclamo.setId(idReclamo);

            SistemaLogs.info(ReclamoService.class, "Nuevo reclamo de partido ingresado ID: " + idReclamo + " por capitán ID: " + idCapitan);
            return reclamo;
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al registrar el reclamo de partido: " + e.getMessage(), e);
        }
    }

    /**
     * CU-14: Apelación de sanción disciplinaria por un capitán.
     * Plazo perentorio: 5 días corridos desde la fecha de inicio de la sanción.
     */
    public Reclamo apelarSancion(Long idSancion, Long idCapitan, String motivo) throws TorneoExcepcion {
        return apelarSancion(idSancion, idCapitan, motivo, null);
    }

    public Reclamo apelarSancion(Long idSancion, Long idCapitan, String motivo, Usuario operador) throws TorneoExcepcion {
        if (operador == null) {
            throw new TorneoExcepcion("Acceso denegado: se requiere un operador autenticado para apelar sanciones.");
        }
        TiposDatos.Rol rol = operador.getRol();
        if (rol != TiposDatos.Rol.CAPITAN && rol != TiposDatos.Rol.ADMINISTRADOR && rol != TiposDatos.Rol.ORGANIZADOR) {
            throw new TorneoExcepcion("Acceso denegado: El rol " + rol + " no tiene permisos para apelar sanciones.");
        }
        if (rol == TiposDatos.Rol.CAPITAN && operador.getId() != null && !operador.getId().equals(idCapitan)) {
            throw new TorneoExcepcion("Acceso denegado: El operador autenticado no coincide con el capitán que suscribe la apelación.");
        }

        if (motivo == null || motivo.trim().length() < 15) {
            throw new TorneoExcepcion("La fundamentación de la apelación debe contener al menos 15 caracteres.");
        }

        try (Connection conn = ConexionBD.obtenerConexion()) {
            Sancion s = sancionDAO.buscarPorId(conn, idSancion);
            if (s == null) {
                throw new TorneoExcepcion("La sanción a apelar no existe.");
            }
            if (s.getFechaPublicacionFormal() == null) {
                throw new TorneoExcepcion("La sanción ID " + idSancion + " aún no ha sido formalmente publicada o notificada por el tribunal.");
            }

            int diasLimite = configuracionDAO.obtenerValorInt(conn, "DIAS_LIMITE_APELACION", 5);
            boolean dentroDePlazo = reclamoDAO.estaDentroDelPlazoApelacion(conn, idSancion, diasLimite);
            if (!dentroDePlazo) {
                throw new TorneoExcepcion("Plazo de apelación vencido: han transcurrido más de " + diasLimite + " días corridos desde la publicación formal de la sanción.");
            }

            Usuario cap = usuarioDAO.buscarPorId(conn, idCapitan);
            if (cap == null) {
                throw new TorneoExcepcion("El usuario capitán no existe.");
            }

            // Validación de pertenencia de recurso: el capitán debe ser el capitán del equipo del jugador sancionado
            if (s.getJugadorSancionado() != null) {
                Long idJug = s.getJugadorSancionado().getId();
                Jugador j = jugadorDAO.buscarPorId(conn, idJug);
                if (j == null || j.getIdEquipo() == null) {
                    throw new TorneoExcepcion("Acceso denegado: El jugador sancionado no pertenece a un equipo registrado.");
                }
                Equipo eq = equipoDAO.buscarPorId(conn, j.getIdEquipo());
                if (eq == null || eq.getCapitan() == null || eq.getCapitan().getId() == null || !idCapitan.equals(eq.getCapitan().getId())) {
                    throw new TorneoExcepcion("Acceso denegado: El capitán con ID " + idCapitan + " no es el capitán del equipo del jugador sancionado.");
                }
            }

            Partido partidoOrigen = s.getPartidoOrigen();
            Long idPartido = partidoOrigen != null ? partidoOrigen.getIdPartido() : null;

            Reclamo reclamo = new Reclamo(new Date(), motivo, s, partidoOrigen, cap);
            Long idReclamo = reclamoDAO.insertar(conn, reclamo, idSancion, idPartido, idCapitan);
            reclamo.setId(idReclamo);

            SistemaLogs.info(ReclamoService.class, "Nueva apelación de sanción ID: " + idSancion + " radicada por expediente ID: " + idReclamo);
            return reclamo;
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al registrar la apelación: " + e.getMessage(), e);
        }
    }

    /**
     * Resolución del reclamo o apelación por parte del tribunal de disciplina u organización.
     */
    public void resolverExpediente(Long idReclamo, String veredicto, String justificacion) throws TorneoExcepcion {
        resolverExpediente(idReclamo, veredicto, justificacion, null);
    }

    public void resolverExpediente(Long idReclamo, String veredicto, String justificacion, Usuario operador) throws TorneoExcepcion {
        if (operador == null) {
            throw new TorneoExcepcion("Acceso denegado: se requiere un operador autenticado para resolver expedientes.");
        }
        TiposDatos.Rol rol = operador.getRol();
        if (rol != TiposDatos.Rol.ADMINISTRADOR && rol != TiposDatos.Rol.ORGANIZADOR) {
            throw new TorneoExcepcion("Acceso denegado: Sólo usuarios con rol ADMINISTRADOR u ORGANIZADOR (Tribunal) pueden resolver expedientes.");
        }
        if (veredicto == null || veredicto.trim().isEmpty()) {
            throw new TorneoExcepcion("El veredicto formal es obligatorio.");
        }
        if (justificacion == null || justificacion.trim().length() < 15) {
            throw new TorneoExcepcion("La fundamentación del veredicto debe ser formal y detallada.");
        }

        try (Connection conn = ConexionBD.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                Reclamo r = reclamoDAO.buscarPorId(conn, idReclamo);
                if (r == null) {
                    throw new TorneoExcepcion("El expediente de reclamo no existe.");
                }
                if ("RESUELTO".equalsIgnoreCase(r.getEstado())) {
                    throw new TorneoExcepcion("Este expediente ya se encuentra formalmente resuelto y cerrado.");
                }

                reclamoDAO.resolver(conn, idReclamo, veredicto, justificacion);

                // Si fue apelación contra sanción y el veredicto es favorable ("A FAVOR"), anular la sanción y rehabilitar jugador
                if (r.getSancionApelada() != null && "A FAVOR".equalsIgnoreCase(veredicto.trim())) {
                    Long idSancion = r.getSancionApelada().getId();
                    sancionDAO.anularSancion(conn, idSancion, "Apelación resuelta A FAVOR en expediente #" + idReclamo);

                    Sancion san = sancionDAO.buscarPorId(conn, idSancion);
                    if (san != null && san.getJugadorSancionado() != null) {
                        Long idJug = san.getJugadorSancionado().getId();
                        Long idTorneo = san.getTorneo() != null ? san.getTorneo().getIdTorneo() : null;
                        List<Sancion> restantes = sancionDAO.buscarActivasPorJugador(conn, idJug, idTorneo);
                        if (restantes.isEmpty()) {
                            Jugador jug = jugadorDAO.buscarPorId(conn, idJug);
                            if (jug != null && jug.getEstado() == TiposDatos.EstadoJugador.SUSPENDIDO) {
                                jugadorDAO.actualizarEstado(conn, idJug, "ACTIVO");
                            }
                        }
                    }
                    SistemaLogs.info(ReclamoService.class, "Sanción ID " + idSancion + " anulada en mérito a resolución de apelación #" + idReclamo);
                }

                conn.commit();
                SistemaLogs.info(ReclamoService.class, "Expediente de reclamo ID " + idReclamo + " resuelto: " + veredicto);
            } catch (Exception e) {
                conn.rollback();
                LOGGER.severe("Error al resolver expediente de reclamo, rollback: " + e.getMessage());
                throw new TorneoExcepcion("Error al resolver el reclamo: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error de base de datos.", e);
        }
    }

    public List<Reclamo> listarReclamosPendientes() throws TorneoExcepcion {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return reclamoDAO.buscarPendientes(conn);
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al listar reclamos pendientes.", e);
        }
    }

    public Reclamo buscarPorId(Long idReclamo) throws TorneoExcepcion {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return reclamoDAO.buscarPorId(conn, idReclamo);
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al buscar reclamo por ID.", e);
        }
    }
}
