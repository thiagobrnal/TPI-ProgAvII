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

    public Torneo crearTorneo(String nombre, String formato, int cupoEquipos, Date fechaInicio) throws TorneoExcepcion {
        return crearTorneo(nombre, formato, cupoEquipos, fechaInicio, null);
    }

    public Torneo crearTorneo(String nombre, String formato, int cupoEquipos, Date fechaInicio, Usuario operador) throws TorneoExcepcion {
        if (operador != null) {
            TiposDatos.Rol rol = operador.getRol();
            if (rol != TiposDatos.Rol.ADMINISTRADOR && rol != TiposDatos.Rol.ORGANIZADOR) {
                throw new TorneoExcepcion("Acceso denegado: El rol " + rol + " no tiene permisos para crear torneos.");
            }
        }
        Torneo torneo = new Torneo(nombre, formato, cupoEquipos, fechaInicio);
        try (Connection conn = ConexionBD.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
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

    public Fase agregarFase(Long idTorneo, String nombreFase, int orden, boolean limpiaAmarillas) throws TorneoExcepcion {
        return agregarFase(idTorneo, nombreFase, orden, limpiaAmarillas, null);
    }

    public Fase agregarFase(Long idTorneo, String nombreFase, int orden, boolean limpiaAmarillas, Usuario operador) throws TorneoExcepcion {
        if (operador != null) {
            TiposDatos.Rol rol = operador.getRol();
            if (rol != TiposDatos.Rol.ADMINISTRADOR && rol != TiposDatos.Rol.ORGANIZADOR) {
                throw new TorneoExcepcion("Acceso denegado: El rol " + rol + " no tiene permisos para agregar fases.");
            }
        }
        try (Connection conn = ConexionBD.obtenerConexion()) {
            Torneo t = torneoDAO.buscarPorId(conn, idTorneo);
            if (t == null) {
                throw new TorneoExcepcion("El torneo con ID " + idTorneo + " no existe.");
            }
            if ("FINALIZADO".equalsIgnoreCase(t.getEstado())) {
                throw new TorneoExcepcion("No se pueden agregar fases a un torneo finalizado.");
            }

            Fase fase = new Fase(nombreFase, orden, limpiaAmarillas);
            Long idFase = faseDAO.insertar(conn, fase, idTorneo);
            fase.setIdFase(idFase);
            SistemaLogs.info(TorneoService.class, "Fase agregada: " + nombreFase + " (Torneo: " + t.getNombre() + ", limpiaAmarillas: " + limpiaAmarillas + ")");
            return fase;
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al persistir la fase: " + e.getMessage(), e);
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
        if (operador != null) {
            TiposDatos.Rol rol = operador.getRol();
            if (rol != TiposDatos.Rol.ADMINISTRADOR && rol != TiposDatos.Rol.ORGANIZADOR) {
                throw new TorneoExcepcion("Acceso denegado: El rol " + rol + " no tiene permisos para actualizar estado de torneos.");
            }
        }
        try (Connection conn = ConexionBD.obtenerConexion()) {
            torneoDAO.actualizarEstado(conn, idTorneo, nuevoEstado);
            SistemaLogs.info(TorneoService.class, "Estado del torneo ID " + idTorneo + " actualizado a: " + nuevoEstado);
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al actualizar estado del torneo: " + e.getMessage(), e);
        }
    }
}
