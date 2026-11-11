package com.gestionamateur.tpi.progav.controlador;

import com.gestionamateur.tpi.progav.dao.JugadorDAO;
import com.gestionamateur.tpi.progav.modelo.Jugador;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.servicio.JugadorService;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GestorJugadores {

    private static final Logger LOGGER = Logger.getLogger(GestorJugadores.class.getName());
    private static final List<Jugador> dbJugadores = new ArrayList<>();
    private final JugadorService jugadorService = new JugadorService();
    private final JugadorDAO jugadorDAO = new JugadorDAO();

    public GestorJugadores() {
    }

    // CU 2: Dar de alta jugadores con persistencia obligatoria
    public String iniciarAltaJugador(String nombre, int dni, Date fechaNacimiento, String posicion) {
        return iniciarAltaJugador(nombre, dni, fechaNacimiento, posicion, null, null);
    }

    public String iniciarAltaJugador(String nombre, int dni, Date fechaNacimiento, String posicion, Long idEquipo, Usuario operador) {
        return iniciarAltaJugador(nombre, dni, fechaNacimiento, posicion, idEquipo,
                com.gestionamateur.tpi.progav.modelo.TiposDatos.EstadoJugador.ACTIVO, operador);
    }

    /** CU-02: alta de jugador con equipo y estado inicial. */
    public String iniciarAltaJugador(String nombre, int dni, Date fechaNacimiento, String posicion, Long idEquipo,
                                     com.gestionamateur.tpi.progav.modelo.TiposDatos.EstadoJugador estado,
                                     Usuario operador) {
        try {
            Jugador nuevo = jugadorService.registrarJugador(nombre, dni, fechaNacimiento, posicion, idEquipo, estado, operador);
            dbJugadores.add(nuevo);
            return "ÉXITO: Jugador habilitado e inscripto en la base de datos (ID: " + nuevo.getId() + "). Listo para imprimir credencial.";
        } catch (TorneoExcepcion te) {
            return "ERROR DE VALIDACIÓN: " + te.getMessage();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fallo de persistencia JDBC al registrar jugador: " + e.getMessage());
            // Sin fallback engañoso a memoria
            return "ERROR DE CONEXIÓN: La base de datos no está disponible (" + e.getMessage() + "). No se guardaron cambios.";
        }
    }

    public static List<Jugador> getJugadoresRegistrados() {
        try {
            JugadorDAO dao = new JugadorDAO();
            List<Jugador> listaBD = dao.buscarTodos();
            if (listaBD != null && !listaBD.isEmpty()) {
                return listaBD;
            }
        } catch (Exception e) {
            LOGGER.warning("No se pudieron cargar jugadores desde base de datos: " + e.getMessage());
        }
        return dbJugadores;
    }
}
