package com.gestionamateur.tpi.progav.servicio;

import com.gestionamateur.tpi.progav.dao.ReporteDAO;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.gestionamateur.tpi.progav.dao.ConexionBD;

/**
 * Servicio de reportes de Release 1 (CU-19 a CU-22).
 *
 * Orquesta el {@link ReporteDAO} y devuelve los datos ya formateados y
 * listos para cargar en una tabla de Swing (encabezados + filas).
 * Los errores de negocio y de base de datos se reportan como
 * {@link TorneoExcepcion}, igual que en el resto de los servicios del proyecto.
 */
public class ReporteService {

    private static final Logger LOGGER = Logger.getLogger(ReporteService.class.getName());

    private final ReporteDAO reporteDAO;

    public ReporteService() {
        this.reporteDAO = new ReporteDAO();
    }

    public ReporteService(ReporteDAO reporteDAO) {
        this.reporteDAO = reporteDAO;
    }

    /**
     * Estructura simple que transporta un reporte ya formateado: titulo del
     * reporte, encabezados de columna y filas de datos.
     */
    public static final class TablaReporte {

        private final String titulo;
        private final String[] columnas;
        private final List<Object[]> filas;

        public TablaReporte(String titulo, String[] columnas, List<Object[]> filas) {
            this.titulo = titulo;
            this.columnas = columnas;
            this.filas = filas == null ? Collections.emptyList() : filas;
        }

        public String getTitulo() {
            return titulo;
        }

        public String[] getColumnas() {
            return columnas;
        }

        public List<Object[]> getFilas() {
            return filas;
        }

        public boolean estaVacia() {
            return filas.isEmpty();
        }
    }

    // ------------------------------------------------------------------
    // CU-19: tabla de posiciones
    // ------------------------------------------------------------------

    private static final String[] COLUMNAS_POSICIONES = {
        "Equipo", "PJ", "PG", "PE", "PP", "GF", "GC", "DG", "Puntos"
    };

    /**
     * Tabla de posiciones del torneo: solo partidos finalizados, con victoria de
     * 3 puntos, empate de 1 y derrota de 0. Orden: puntos, diferencia de goles,
     * goles a favor y nombre del equipo.
     */
    public TablaReporte obtenerTablaPosiciones() throws TorneoExcepcion {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            List<Object[]> filas = reporteDAO.tablaPosiciones(conn);
            return new TablaReporte("Tabla de posiciones (CU-19)", COLUMNAS_POSICIONES, filas);
        } catch (SQLException e) {
            LOGGER.warning("Error al generar la tabla de posiciones: " + e.getMessage());
            throw new TorneoExcepcion("No se pudo generar la tabla de posiciones: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
    // CU-20: reporte de jugadores
    // ------------------------------------------------------------------

    private static final String[] COLUMNAS_JUGADORES = {
        "Jugador", "Equipo", "Goles", "Tarjetas amarillas", "Tarjetas rojas", "Sanciones vigentes"
    };

    /**
     * Reporte de jugadores: goleadores, tarjetas amarillas, tarjetas rojas y
     * sanciones vigentes por jugador.
     */
    public TablaReporte obtenerReporteJugadores() throws TorneoExcepcion {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            List<Object[]> filas = reporteDAO.reporteJugadores(conn);
            return new TablaReporte("Reporte de jugadores (CU-20)", COLUMNAS_JUGADORES, filas);
        } catch (SQLException e) {
            LOGGER.warning("Error al generar el reporte de jugadores: " + e.getMessage());
            throw new TorneoExcepcion("No se pudo generar el reporte de jugadores: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
    // CU-21: historial deportivo
    // ------------------------------------------------------------------

    private static final String[] COLUMNAS_HISTORIAL_EQUIPOS = {
        "Equipo", "Torneos", "PJ", "PG", "PE", "PP", "GF", "GC"
    };

    private static final String[] COLUMNAS_HISTORIAL_JUGADORES = {
        "Jugador", "Equipo", "Partidos", "Goles", "Tarjetas amarillas", "Tarjetas rojas", "Sanciones vigentes"
    };

    /**
     * Historial deportivo por equipo: torneos en los que participó, partidos
     * jugados, ganados, empatados, perdidos y goles a favor y en contra.
     */
    public TablaReporte obtenerHistorialEquipos() throws TorneoExcepcion {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            List<Object[]> filas = reporteDAO.historialEquipos(conn);
            return new TablaReporte("Historial deportivo por equipo (CU-21)", COLUMNAS_HISTORIAL_EQUIPOS, filas);
        } catch (SQLException e) {
            LOGGER.warning("Error al generar el historial por equipo: " + e.getMessage());
            throw new TorneoExcepcion("No se pudo generar el historial por equipo: " + e.getMessage(), e);
        }
    }

    /**
     * Historial deportivo por jugador: equipo, partidos, goles, tarjetas y
     * sanciones vigentes.
     */
    public TablaReporte obtenerHistorialJugadores() throws TorneoExcepcion {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            List<Object[]> filas = reporteDAO.historialJugadores(conn);
            return new TablaReporte("Historial deportivo por jugador (CU-21)", COLUMNAS_HISTORIAL_JUGADORES, filas);
        } catch (SQLException e) {
            LOGGER.warning("Error al generar el historial por jugador: " + e.getMessage());
            throw new TorneoExcepcion("No se pudo generar el historial por jugador: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
    // CU-22: reporte de arbitros
    // ------------------------------------------------------------------

    private static final String[] COLUMNAS_ARBITROS = {
        "Árbitro", "Categoría arbitral", "Partidos dirigidos", "Incidencias registradas"
    };

    /**
     * Reporte de árbitros: partidos dirigidos e incidencias registradas.
     * Incluye también a los árbitros que no tienen partidos asignados.
     */
    public TablaReporte obtenerReporteArbitros() throws TorneoExcepcion {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            List<Object[]> filas = reporteDAO.reporteArbitros(conn);
            return new TablaReporte("Reporte de árbitros (CU-22)", COLUMNAS_ARBITROS, filas);
        } catch (SQLException e) {
            LOGGER.warning("Error al generar el reporte de árbitros: " + e.getMessage());
            throw new TorneoExcepcion("No se pudo generar el reporte de árbitros: " + e.getMessage(), e);
        }
    }

    /**
     * Devuelve los cuatro reportes en un solo lote, reutilizando una misma
     * conexion. Se usa desde la vista para refrescar todas las pestañas.
     *
     * @return lista de reportes en el orden: posiciones, jugadores,
     *         historial por equipo, historial por jugador y árbitros.
     */
    public List<TablaReporte> obtenerTodosLosReportes() throws TorneoExcepcion {
        List<TablaReporte> reportes = new ArrayList<>();
        try (Connection conn = ConexionBD.obtenerConexion()) {
            reportes.add(new TablaReporte("Tabla de posiciones (CU-19)", COLUMNAS_POSICIONES,
                    reporteDAO.tablaPosiciones(conn)));
            reportes.add(new TablaReporte("Reporte de jugadores (CU-20)", COLUMNAS_JUGADORES,
                    reporteDAO.reporteJugadores(conn)));
            reportes.add(new TablaReporte("Historial deportivo por equipo (CU-21)", COLUMNAS_HISTORIAL_EQUIPOS,
                    reporteDAO.historialEquipos(conn)));
            reportes.add(new TablaReporte("Historial deportivo por jugador (CU-21)", COLUMNAS_HISTORIAL_JUGADORES,
                    reporteDAO.historialJugadores(conn)));
            reportes.add(new TablaReporte("Reporte de árbitros (CU-22)", COLUMNAS_ARBITROS,
                    reporteDAO.reporteArbitros(conn)));
        } catch (SQLException e) {
            LOGGER.warning("Error al generar los reportes: " + e.getMessage());
            throw new TorneoExcepcion("No se pudieron generar los reportes: " + e.getMessage(), e);
        }
        return reportes;
    }
}
