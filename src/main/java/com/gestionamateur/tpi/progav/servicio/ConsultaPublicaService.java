package com.gestionamateur.tpi.progav.servicio;

import com.gestionamateur.tpi.progav.dao.ConexionBD;
import com.gestionamateur.tpi.progav.dao.ConsultaPublicaDAO;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Consultas publicas de solo lectura (CU-08 a CU-11).
 *
 * <p>El rol PUBLICO es un acceso anonimo de solo lectura (ADR-02 y ADR-18): no requiere cuenta de
 * usuario ni credenciales en la base de datos. Este servicio no expone datos personales (DNI,
 * correo electronico, hash de contrasena), ni informacion de salud del jugador, ni informacion
 * disciplinaria reservada (sanciones, reclamos y expedientes del tribunal).
 */
public class ConsultaPublicaService {

    private final ConsultaPublicaDAO consultaPublicaDAO;

    public ConsultaPublicaService() {
        this(new ConsultaPublicaDAO());
    }

    public ConsultaPublicaService(ConsultaPublicaDAO consultaPublicaDAO) {
        this.consultaPublicaDAO = consultaPublicaDAO;
    }

    /** CU-08: calendario de partidos, resultados y sedes del torneo. */
    public List<Object[]> fixture(Long idTorneo) throws TorneoExcepcion {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return consultaPublicaDAO.fixtureDeTorneo(conn, idTorneo);
        } catch (SQLException e) {
            throw new TorneoExcepcion("No se pudo consultar el fixture: " + e.getMessage(), e);
        }
    }

    /** CU-09: estadisticas publicas consolidadas de un equipo. */
    public List<Object[]> perfilPublicoDeEquipo(Long idEquipo) throws TorneoExcepcion {
        if (idEquipo == null) {
            throw new TorneoExcepcion("Debe seleccionar un equipo.");
        }
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return consultaPublicaDAO.estadisticasDeEquipo(conn, idEquipo);
        } catch (SQLException e) {
            throw new TorneoExcepcion("No se pudo consultar el equipo: " + e.getMessage(), e);
        }
    }

    /** CU-10: estadisticas publicas de los jugadores (goles, tarjetas y partidos). */
    public List<Object[]> estadisticasDeJugadores(Long idTorneo) throws TorneoExcepcion {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return consultaPublicaDAO.estadisticasDeJugadores(conn, idTorneo);
        } catch (SQLException e) {
            throw new TorneoExcepcion("No se pudieron consultar las estadisticas de jugadores: " + e.getMessage(), e);
        }
    }

    /** CU-11: progresion de fases del torneo. */
    public List<Object[]> avanceDeFases(Long idTorneo) throws TorneoExcepcion {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return consultaPublicaDAO.avanceDeFases(conn, idTorneo);
        } catch (SQLException e) {
            throw new TorneoExcepcion("No se pudo consultar el avance de fases: " + e.getMessage(), e);
        }
    }

    /** Torneos disponibles para el selector del acceso publico. */
    public List<Object[]> torneos() throws TorneoExcepcion {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return consultaPublicaDAO.torneos(conn);
        } catch (SQLException e) {
            throw new TorneoExcepcion("No se pudieron consultar los torneos: " + e.getMessage(), e);
        }
    }

    /** Equipos disponibles para el selector del acceso publico. */
    public List<Object[]> equipos() throws TorneoExcepcion {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return consultaPublicaDAO.equipos(conn);
        } catch (SQLException e) {
            throw new TorneoExcepcion("No se pudieron consultar los equipos: " + e.getMessage(), e);
        }
    }
}
