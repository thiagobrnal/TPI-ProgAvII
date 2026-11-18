package com.gestionamateur.tpi.progav.servicio;

import com.gestionamateur.tpi.progav.dao.ConexionBD;
import com.gestionamateur.tpi.progav.dao.FaseDAO;
import com.gestionamateur.tpi.progav.dao.TorneoDAO;
import com.gestionamateur.tpi.progav.modelo.Fase;
import com.gestionamateur.tpi.progav.modelo.SistemaLogs;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.Torneo;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.seguridad.PoliticaPermisos;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class TorneoService {

    private static final Logger LOGGER = Logger.getLogger(TorneoService.class.getName());
    private final TorneoDAO torneoDAO;
    private final FaseDAO faseDAO;

    public TorneoService() {
        this.torneoDAO = new TorneoDAO();
        this.faseDAO = new FaseDAO();
    }

    public TorneoService(TorneoDAO torneoDAO, FaseDAO faseDAO) {
        this.torneoDAO = torneoDAO;
        this.faseDAO = faseDAO;
    }

    private void validarOperadorGestion(Usuario operador, String accion) throws TorneoExcepcion {
        if (!PoliticaPermisos.esSesionAutenticada(operador)) {
            throw new TorneoExcepcion("Acceso denegado: Se requiere un operador autenticado para " + accion + ".");
        }
        if (!PoliticaPermisos.tienePermiso(operador, PoliticaPermisos.ModuloMenu.GESTIONAR_TORNEOS)) {
            throw new TorneoExcepcion("Acceso denegado: El rol " + operador.getRol() + " no tiene permisos para " + accion + ".");
        }
        if (!PoliticaPermisos.esOperadorPersistidoValido(operador)) {
            throw new TorneoExcepcion("Acceso denegado: Se requiere un operador autenticado con identificador válido para " + accion + ".");
        }
    }

    public Torneo crearTorneo(String nombre, String formato, int cupoEquipos, Date fechaInicio) throws TorneoExcepcion {
        return crearTorneo(nombre, formato, cupoEquipos, fechaInicio, null);
    }

    public Torneo crearTorneo(String nombre, String formato, int cupoEquipos, Date fechaInicio, Usuario operador) throws TorneoExcepcion {
        validarOperadorGestion(operador, "crear torneos");
        Torneo torneo = new Torneo(nombre, formato, cupoEquipos, fechaInicio);
        try (Connection conn = ConexionBD.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                if (torneoDAO.existePorNombreExcluyendoId(conn, nombre.trim(), null)) {
                    throw new TorneoExcepcion("Ya existe un torneo registrado con el nombre '" + nombre.trim() + "'.");
                }
                Long id = torneoDAO.insertar(conn, torneo);
                torneo.setIdTorneo(id);

                // Por defecto creamos la primera fase: "Fase Regular" (orden 1, sin limpiar amarillas)
                Fase faseRegular = new Fase("Fase Regular", 1, false);
                faseDAO.insertar(conn, faseRegular, id);

                conn.commit();
                SistemaLogs.info(TorneoService.class, "Torneo creado con éxito: " + nombre + " (ID: " + id + ")");
                return torneo;
            } catch (Exception e) {
                conn.rollback();
                LOGGER.severe("Error al crear torneo, rollback ejecutado: " + e.getMessage());
                throw new TorneoExcepcion("No se pudo registrar el torneo: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error de conexión a la base de datos.", e);
        }
    }

    public Torneo actualizarTorneo(Long idTorneo, String nombre, String formato, int cupoEquipos, Date fechaInicio, Usuario operador) throws TorneoExcepcion {
        validarOperadorGestion(operador, "editar torneos");
        if (idTorneo == null || idTorneo <= 0) {
            throw new TorneoExcepcion("Debe indicar el torneo a modificar.");
        }
        Torneo validado = new Torneo(nombre != null ? nombre.trim() : "", formato, cupoEquipos, fechaInicio);
        try (Connection conn = ConexionBD.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                Torneo existente = torneoDAO.buscarPorId(conn, idTorneo);
                if (existente == null) {
                    throw new TorneoExcepcion("El torneo con ID " + idTorneo + " no existe.");
                }
                if ("FINALIZADO".equalsIgnoreCase(existente.getEstado()) || "CANCELADO".equalsIgnoreCase(existente.getEstado())) {
                    throw new TorneoExcepcion("No se puede modificar un torneo en estado " + existente.getEstado() + ".");
                }
                int partidosVinculados = torneoDAO.contarPartidosDeTorneo(conn, idTorneo);
                boolean bloqueoEstructural = partidosVinculados > 0 || "EN_CURSO".equalsIgnoreCase(existente.getEstado());
                if (bloqueoEstructural) {
                    if (existente.getFormato() != validado.getFormato()) {
                        throw new TorneoExcepcion("No se puede cambiar el formato de un torneo en curso o con partidos ya programados.");
                    }
                    if (validado.getCupoEquipos() < existente.getCupoEquipos()) {
                        throw new TorneoExcepcion("No se puede reducir el cupo de un torneo en curso o con partidos ya programados.");
                    }
                }
                if (torneoDAO.existePorNombreExcluyendoId(conn, validado.getNombre(), idTorneo)) {
                    throw new TorneoExcepcion("Ya existe otro torneo con el nombre '" + validado.getNombre() + "'.");
                }
                validado.setIdTorneo(idTorneo);
                validado.setEstado(existente.getEstado());
                torneoDAO.actualizar(conn, validado);
                conn.commit();
                SistemaLogs.info(TorneoService.class, "Torneo ID " + idTorneo + " actualizado por " + operador.getNombreUsuario());
                return validado;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof TorneoExcepcion) {
                    throw (TorneoExcepcion) e;
                }
                throw new TorneoExcepcion("No se pudo actualizar el torneo: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error de conexión al actualizar el torneo.", e);
        }
    }

    public Fase agregarFase(Long idTorneo, String nombreFase, int orden, boolean limpiaAmarillas) throws TorneoExcepcion {
        return agregarFase(idTorneo, nombreFase, orden, limpiaAmarillas, null);
    }

    public Fase agregarFase(Long idTorneo, String nombreFase, int orden, boolean limpiaAmarillas, Usuario operador) throws TorneoExcepcion {
        validarOperadorGestion(operador, "agregar fases");
        try (Connection conn = ConexionBD.obtenerConexion()) {
            Torneo t = torneoDAO.buscarPorId(conn, idTorneo);
            if (t == null) {
                throw new TorneoExcepcion("El torneo con ID " + idTorneo + " no existe.");
            }
            if ("FINALIZADO".equalsIgnoreCase(t.getEstado()) || "CANCELADO".equalsIgnoreCase(t.getEstado())) {
                throw new TorneoExcepcion("No se pueden agregar fases a un torneo en estado " + t.getEstado() + ".");
            }

            Fase fase = new Fase(nombreFase != null ? nombreFase.trim() : "", orden, limpiaAmarillas);
            if (faseDAO.existeNombreEnTorneoExcluyendoId(conn, idTorneo, fase.getNombre(), null)) {
                throw new TorneoExcepcion("Ya existe una fase llamada '" + fase.getNombre() + "' en este torneo.");
            }
            if (faseDAO.existeOrdenEnTorneoExcluyendoId(conn, idTorneo, orden, null)) {
                throw new TorneoExcepcion("Ya existe una fase con el orden " + orden + " en este torneo.");
            }
            Long idFase = faseDAO.insertar(conn, fase, idTorneo);
            fase.setIdFase(idFase);
            SistemaLogs.info(TorneoService.class, "Fase agregada: " + fase.getNombre() + " (Torneo: " + t.getNombre() + ", limpiaAmarillas: " + limpiaAmarillas + ")");
            return fase;
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al persistir la fase: " + e.getMessage(), e);
        }
    }

    public Fase actualizarFase(Long idFase, String nombreFase, int orden, boolean limpiaAmarillas, Usuario operador) throws TorneoExcepcion {
        validarOperadorGestion(operador, "editar fases");
        if (idFase == null || idFase <= 0) {
            throw new TorneoExcepcion("Debe indicar la fase a editar.");
        }
        Fase validada = new Fase(nombreFase != null ? nombreFase.trim() : "", orden, limpiaAmarillas);
        validada.setIdFase(idFase);

        try (Connection conn = ConexionBD.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                Long idTorneo = faseDAO.obtenerIdTorneoDeFase(conn, idFase);
                if (idTorneo == null) {
                    throw new TorneoExcepcion("La fase indicada no existe.");
                }
                Torneo t = torneoDAO.buscarPorId(conn, idTorneo);
                if (t == null || "FINALIZADO".equalsIgnoreCase(t.getEstado()) || "CANCELADO".equalsIgnoreCase(t.getEstado())) {
                    throw new TorneoExcepcion("No se pueden editar fases de un torneo finalizado o cancelado.");
                }
                if (faseDAO.contarPartidosDeFase(conn, idFase) > 0) {
                    throw new TorneoExcepcion("No se puede modificar una fase que ya tiene partidos programados o disputados.");
                }
                if (faseDAO.existeNombreEnTorneoExcluyendoId(conn, idTorneo, validada.getNombre(), idFase)) {
                    throw new TorneoExcepcion("Ya existe otra fase con el nombre '" + validada.getNombre() + "' en este torneo.");
                }
                if (faseDAO.existeOrdenEnTorneoExcluyendoId(conn, idTorneo, orden, idFase)) {
                    throw new TorneoExcepcion("Ya existe otra fase con el orden " + orden + " en este torneo.");
                }
                faseDAO.actualizar(conn, validada);
                conn.commit();
                SistemaLogs.info(TorneoService.class, "Fase ID " + idFase + " actualizada por " + operador.getNombreUsuario());
                return validada;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof TorneoExcepcion) {
                    throw (TorneoExcepcion) e;
                }
                throw new TorneoExcepcion("Error al actualizar la fase: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error de conexión al actualizar la fase.", e);
        }
    }

    public void eliminarFase(Long idFase, Usuario operador) throws TorneoExcepcion {
        validarOperadorGestion(operador, "quitar fases");
        if (idFase == null || idFase <= 0) {
            throw new TorneoExcepcion("Debe indicar la fase a quitar.");
        }
        try (Connection conn = ConexionBD.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                Long idTorneo = faseDAO.obtenerIdTorneoDeFase(conn, idFase);
                if (idTorneo == null) {
                    throw new TorneoExcepcion("La fase indicada no existe.");
                }
                Torneo t = torneoDAO.buscarPorId(conn, idTorneo);
                if (t == null || !"CONFIGURACION".equalsIgnoreCase(t.getEstado())) {
                    throw new TorneoExcepcion("Solo se pueden quitar fases sin uso cuando el torneo está en CONFIGURACION.");
                }
                if (faseDAO.contarPartidosDeFase(conn, idFase) > 0) {
                    throw new TorneoExcepcion("No se puede quitar la fase porque ya posee partidos asociados.");
                }
                if (faseDAO.buscarPorTorneo(conn, idTorneo).size() <= 1) {
                    throw new TorneoExcepcion("El torneo debe conservar al menos una fase configurada.");
                }
                faseDAO.eliminarSiSinPartidos(conn, idFase);
                conn.commit();
                SistemaLogs.info(TorneoService.class, "Fase ID " + idFase + " removida (sin partidos) por " + operador.getNombreUsuario());
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof TorneoExcepcion) {
                    throw (TorneoExcepcion) e;
                }
                throw new TorneoExcepcion("No se pudo quitar la fase: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error de conexión al quitar la fase.", e);
        }
    }

    public List<Torneo> listarTodos() throws TorneoExcepcion {
        try {
            return torneoDAO.buscarTodos();
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al listar torneos.", e);
        }
    }

    public Torneo buscarPorId(Long idTorneo) throws TorneoExcepcion {
        try {
            return torneoDAO.buscarPorId(idTorneo);
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al buscar torneo por ID: " + idTorneo, e);
        }
    }

    public List<Fase> listarFasesDeTorneo(Long idTorneo) throws TorneoExcepcion {
        try {
            return faseDAO.buscarPorTorneo(idTorneo);
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al listar fases del torneo ID: " + idTorneo, e);
        }
    }

    public void actualizarEstadoTorneo(Long idTorneo, String nuevoEstado) throws TorneoExcepcion {
        actualizarEstadoTorneo(idTorneo, nuevoEstado, null);
    }

    public void actualizarEstadoTorneo(Long idTorneo, String nuevoEstado, Usuario operador) throws TorneoExcepcion {
        validarOperadorGestion(operador, "actualizar el estado de un torneo");
        if (idTorneo == null || idTorneo <= 0) {
            throw new TorneoExcepcion("Debe indicar el torneo a actualizar.");
        }
        Torneo temp = new Torneo();
        temp.setEstado(nuevoEstado);
        String estadoNormalizado = temp.getEstado();

        try (Connection conn = ConexionBD.obtenerConexion()) {
            Torneo existente = torneoDAO.buscarPorId(conn, idTorneo);
            if (existente == null) {
                throw new TorneoExcepcion("El torneo con ID " + idTorneo + " no existe.");
            }
            String estadoActual = existente.getEstado() == null ? "" : existente.getEstado().trim().toUpperCase();
            if ("FINALIZADO".equals(estadoActual) && !"FINALIZADO".equals(estadoNormalizado)) {
                throw new TorneoExcepcion("Un torneo FINALIZADO conserva su historial cerrado y no puede cambiar de estado.");
            }
            if ("CANCELADO".equals(estadoActual) && !"CANCELADO".equals(estadoNormalizado)) {
                throw new TorneoExcepcion("Un torneo CANCELADO no puede reactivarse.");
            }
            if ("CONFIGURACION".equals(estadoNormalizado) && "EN_CURSO".equals(estadoActual)) {
                throw new TorneoExcepcion("Un torneo EN_CURSO no puede retroceder a CONFIGURACION.");
            }
            torneoDAO.actualizarEstado(conn, idTorneo, estadoNormalizado);
            SistemaLogs.info(TorneoService.class, "Estado del torneo ID " + idTorneo + " actualizado a: " + estadoNormalizado);
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al actualizar estado del torneo: " + e.getMessage(), e);
        }
    }

    /** Baja lógica del torneo mediante estado CANCELADO, preservando partidos e historial. */
    public void cancelarTorneo(Long idTorneo, Usuario operador) throws TorneoExcepcion {
        validarOperadorGestion(operador, "cancelar torneos");
        if (idTorneo == null || idTorneo <= 0) {
            throw new TorneoExcepcion("Debe indicar el torneo a cancelar.");
        }
        try (Connection conn = ConexionBD.obtenerConexion()) {
            Torneo existente = torneoDAO.buscarPorId(conn, idTorneo);
            if (existente == null) {
                throw new TorneoExcepcion("El torneo con ID " + idTorneo + " no existe.");
            }
            if ("FINALIZADO".equalsIgnoreCase(existente.getEstado())) {
                throw new TorneoExcepcion("No se puede cancelar un torneo FINALIZADO; sus resultados e historial están protegidos.");
            }
            if ("CANCELADO".equalsIgnoreCase(existente.getEstado())) {
                throw new TorneoExcepcion("El torneo ya se encuentra CANCELADO.");
            }
            torneoDAO.actualizarEstado(conn, idTorneo, "CANCELADO");
            SistemaLogs.info(TorneoService.class, "Torneo ID " + idTorneo + " cancelado lógicamente por " + operador.getNombreUsuario());
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al cancelar el torneo: " + e.getMessage(), e);
        }
    }
}
