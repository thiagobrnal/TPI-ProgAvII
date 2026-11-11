package com.gestionamateur.tpi.progav.servicio;

import com.gestionamateur.tpi.progav.dao.ConexionBD;
import com.gestionamateur.tpi.progav.dao.JugadorDAO;
import com.gestionamateur.tpi.progav.dao.SancionDAO;
import com.gestionamateur.tpi.progav.modelo.Jugador;
import com.gestionamateur.tpi.progav.modelo.Sancion;
import com.gestionamateur.tpi.progav.modelo.SistemaLogs;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

public class DisciplinaService {

    private static final Logger LOGGER = Logger.getLogger(DisciplinaService.class.getName());
    private final SancionDAO sancionDAO;
    private final JugadorDAO jugadorDAO;

    public DisciplinaService() {
        this.sancionDAO = new SancionDAO();
        this.jugadorDAO = new JugadorDAO();
    }

    public DisciplinaService(SancionDAO sancionDAO, JugadorDAO jugadorDAO) {
        this.sancionDAO = sancionDAO;
        this.jugadorDAO = jugadorDAO;
    }

    public List<Sancion> listarSancionesPendientesTribunal() throws TorneoExcepcion {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return sancionDAO.buscarPendientesTribunal(conn);
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al listar sanciones pendientes de resolución del tribunal.", e);
        }
    }

    public void emitirFalloTribunal(Long idSancion, int partidosSuspension, String motivo, String gravedad) throws TorneoExcepcion {
        emitirFalloTribunal(idSancion, partidosSuspension, motivo, gravedad, null);
    }

    public void emitirFalloTribunal(Long idSancion, int partidosSuspension, String motivo, String gravedad, Usuario operador) throws TorneoExcepcion {
        if (operador != null) {
            TiposDatos.Rol rol = operador.getRol();
            if (rol != TiposDatos.Rol.ADMINISTRADOR && rol != TiposDatos.Rol.ORGANIZADOR) {
                throw new TorneoExcepcion("Acceso denegado: El rol " + rol + " no tiene permisos para emitir fallos disciplinarios.");
            }
        }
        if (partidosSuspension < 1) {
            throw new TorneoExcepcion("Una sanción disciplinaria debe tener al menos 1 fecha de suspensión.");
        }
        if (motivo == null || motivo.trim().length() < 10) {
            throw new TorneoExcepcion("El motivo del fallo debe ser detallado (al menos 10 caracteres).");
        }

        try (Connection conn = ConexionBD.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                Sancion s = sancionDAO.buscarPorId(conn, idSancion);
                if (s == null) {
                    throw new TorneoExcepcion("La sanción con ID " + idSancion + " no existe.");
                }

                sancionDAO.oficializarYPublicarSancion(conn, idSancion, partidosSuspension, motivo, gravedad, new java.util.Date());
                Jugador jug = jugadorDAO.buscarPorId(conn, s.getJugadorSancionado().getId());
                if (jug != null && jug.getEstado() == TiposDatos.EstadoJugador.ACTIVO) {
                    jugadorDAO.actualizarEstado(conn, s.getJugadorSancionado().getId(), "SUSPENDIDO");
                }

                conn.commit();
                SistemaLogs.info(DisciplinaService.class, "Fallo disciplinario emitido y formalizado para sanción ID " + idSancion + " (" + partidosSuspension + " fechas)");
            } catch (Exception e) {
                conn.rollback();
                LOGGER.severe("Error al emitir fallo de tribunal, rollback: " + e.getMessage());
                throw new TorneoExcepcion("Error al procesar el fallo disciplinario: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error de conexión a la base de datos.", e);
        }
    }

    public void anularSancion(Long idSancion, String motivoAnulacion) throws TorneoExcepcion {
        anularSancion(idSancion, motivoAnulacion, null);
    }

    public void anularSancion(Long idSancion, String motivoAnulacion, Usuario operador) throws TorneoExcepcion {
        if (operador != null) {
            TiposDatos.Rol rol = operador.getRol();
            if (rol != TiposDatos.Rol.ADMINISTRADOR && rol != TiposDatos.Rol.ORGANIZADOR) {
                throw new TorneoExcepcion("Acceso denegado: El rol " + rol + " no tiene permisos para anular sanciones.");
            }
        }
        if (motivoAnulacion == null || motivoAnulacion.trim().isEmpty()) {
            throw new TorneoExcepcion("Debe justificar el motivo de la anulación de la sanción.");
        }

        try (Connection conn = ConexionBD.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                Sancion s = sancionDAO.buscarPorId(conn, idSancion);
                if (s == null) {
                    throw new TorneoExcepcion("La sanción no existe.");
                }

                sancionDAO.anularSancion(conn, idSancion, motivoAnulacion);

                // Si ya no le quedan otras sanciones activas, rehabilitar al jugador únicamente si su estado era SUSPENDIDO
                Long idTorneo = s.getTorneo() != null ? s.getTorneo().getIdTorneo() : null;
                List<Sancion> restantes = sancionDAO.buscarActivasPorJugador(conn, s.getJugadorSancionado().getId(), idTorneo);
                if (restantes.isEmpty()) {
                    Jugador jug = jugadorDAO.buscarPorId(conn, s.getJugadorSancionado().getId());
                    if (jug != null && jug.getEstado() == TiposDatos.EstadoJugador.SUSPENDIDO) {
                        jugadorDAO.actualizarEstado(conn, s.getJugadorSancionado().getId(), "ACTIVO");
                    }
                }

                conn.commit();
                SistemaLogs.info(DisciplinaService.class, "Sanción ID " + idSancion + " anulada. Motivo: " + motivoAnulacion);
            } catch (Exception e) {
                conn.rollback();
                LOGGER.severe("Error al anular sanción, rollback: " + e.getMessage());
                throw new TorneoExcepcion("No se pudo anular la sanción: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error de conexión a la base de datos.", e);
        }
    }

    public boolean esJugadorHabilitado(Long idJugador, Long idTorneo) throws TorneoExcepcion {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            Jugador j = jugadorDAO.buscarPorId(conn, idJugador);
            if (j == null) {
                return false;
            }
            if (j.getEstado() != TiposDatos.EstadoJugador.ACTIVO) {
                return false;
            }

            List<Sancion> activas = sancionDAO.buscarActivasPorJugador(conn, idJugador, idTorneo);
            return activas.isEmpty();
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al verificar habilitación del jugador.", e);
        }
    }

    public List<Sancion> listarSancionesPorJugador(Long idJugador) throws TorneoExcepcion {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return sancionDAO.buscarPorJugador(conn, idJugador);
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al listar sanciones del jugador.", e);
        }
    }
}
