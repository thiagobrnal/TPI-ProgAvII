package com.gestionamateur.tpi.progav.servicio;

import com.gestionamateur.tpi.progav.dao.CategoriaDAO;
import com.gestionamateur.tpi.progav.dao.TorneoDAO;
import com.gestionamateur.tpi.progav.dao.ConfiguracionDAO;
import com.gestionamateur.tpi.progav.dao.TorneoDAO;
import com.gestionamateur.tpi.progav.dao.ConexionBD;
import com.gestionamateur.tpi.progav.dao.EquipoDAO;
import com.gestionamateur.tpi.progav.dao.TorneoDAO;
import com.gestionamateur.tpi.progav.dao.JugadorDAO;
import com.gestionamateur.tpi.progav.dao.TorneoDAO;
import com.gestionamateur.tpi.progav.modelo.Categoria;
import com.gestionamateur.tpi.progav.modelo.Equipo;
import com.gestionamateur.tpi.progav.modelo.Jugador;
import com.gestionamateur.tpi.progav.modelo.SistemaLogs;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class JugadorService {

    private static final Logger LOGGER = Logger.getLogger(JugadorService.class.getName());
    private final JugadorDAO jugadorDAO;
    private final CategoriaDAO categoriaDAO;
    private final ConfiguracionDAO configuracionDAO;
    private final EquipoDAO equipoDAO;

    public JugadorService() {
        this.jugadorDAO = new JugadorDAO();
        this.categoriaDAO = new CategoriaDAO();
        this.configuracionDAO = new ConfiguracionDAO();
        this.equipoDAO = new EquipoDAO();
    }

    public JugadorService(JugadorDAO jugadorDAO, CategoriaDAO categoriaDAO, ConfiguracionDAO configuracionDAO) {
        this.jugadorDAO = jugadorDAO;
        this.categoriaDAO = categoriaDAO;
        this.configuracionDAO = configuracionDAO;
        this.equipoDAO = new EquipoDAO();
    }

    public Jugador registrarJugador(String nombre, int dni, Date fechaNacimiento, String posicion, Long idEquipo) throws TorneoExcepcion {
        return registrarJugador(nombre, dni, fechaNacimiento, posicion, idEquipo, TiposDatos.EstadoJugador.ACTIVO, null);
    }

    public Jugador registrarJugador(String nombre, int dni, Date fechaNacimiento, String posicion, Long idEquipo, Usuario operador) throws TorneoExcepcion {
        return registrarJugador(nombre, dni, fechaNacimiento, posicion, idEquipo, TiposDatos.EstadoJugador.ACTIVO, operador);
    }

    /**
     * CU-02: alta de jugador. El estado inicial (activo, suspendido, lesionado) es parte
     * de los datos que exige la especificacion.
     */
    public Jugador registrarJugador(String nombre, int dni, Date fechaNacimiento, String posicion, Long idEquipo,
                                     TiposDatos.EstadoJugador estado, Usuario operador) throws TorneoExcepcion {
        if (operador == null) {
            throw new TorneoExcepcion("Acceso denegado: se requiere un operador autenticado para registrar jugadores.");
        }
        TiposDatos.Rol rol = operador.getRol();
        if (rol != TiposDatos.Rol.ADMINISTRADOR && rol != TiposDatos.Rol.ORGANIZADOR && rol != TiposDatos.Rol.CAPITAN) {
            throw new TorneoExcepcion("Acceso denegado: El rol " + rol + " no tiene permisos para registrar jugadores.");
        }
        try (Connection conn = ConexionBD.obtenerConexion()) {
            if (operador != null && operador.getRol() == TiposDatos.Rol.CAPITAN) {
                if (idEquipo == null) {
                    throw new TorneoExcepcion("Acceso denegado: Un capitán debe especificar su propio equipo para registrar jugadores.");
                }
                try {
                    Equipo eq = equipoDAO.buscarPorId(conn, idEquipo);
                    if (eq == null || eq.getCapitan() == null || eq.getCapitan().getId() == null || !operador.getId().equals(eq.getCapitan().getId())) {
                        throw new TorneoExcepcion("Acceso denegado: Un capitán sólo puede registrar jugadores en el plantel de su propio equipo.");
                    }
                } catch (SQLException sqle) {
                    throw new TorneoExcepcion("Error al verificar pertenencia de equipo: " + sqle.getMessage(), sqle);
                }
            }

            Jugador existente = jugadorDAO.buscarPorDni(conn, dni);
            if (existente != null) {
                throw new TorneoExcepcion("Ya existe un jugador registrado con el DNI " + dni + " (" + existente.getNombre() + ").");
            }

            int edadMinimaGeneral = configuracionDAO.obtenerValorInt(conn, "EDAD_MINIMA_GENERAL", 16);
            int edad = calcularEdad(fechaNacimiento, new Date());
            if (edad < edadMinimaGeneral) {
                throw new TorneoExcepcion("El jugador tiene " + edad + " años. La edad mínima reglamentaria general es de " + edadMinimaGeneral + " años.");
            }

            Jugador nuevo = new Jugador(idEquipo, nombre, dni, fechaNacimiento, posicion,
                    estado == null ? TiposDatos.EstadoJugador.ACTIVO : estado);

            // CU-02.1: control de inscripcion. Si el jugador se incorpora a un equipo, se
            // valida la elegibilidad por edad frente a la categoria de ese equipo.
            if (idEquipo != null) {
                Equipo equipo = equipoDAO.buscarPorId(conn, idEquipo);
                if (equipo == null) {
                    throw new TorneoExcepcion("El equipo seleccionado no existe.");
                }
                if (equipo.getCategoria() != null) {
                    Date inicioTorneo = resolverFechaInicioTorneo(conn);
                    if (inicioTorneo != null
                            && !verificarElegibilidadCategoria(nuevo, equipo.getCategoria(), inicioTorneo)) {
                        throw new TorneoExcepcion("El jugador no cumple la edad mínima de la categoría "
                                + equipo.getCategoria().getNombre() + " al inicio del torneo.");
                    }
                }
            }

            Long id = jugadorDAO.insertar(conn, nuevo, idEquipo);
            nuevo.setId(id);
            SistemaLogs.info(JugadorService.class, "Jugador registrado exitosamente: " + nombre + " (DNI: " + dni + ", ID: " + id + ")");
            return nuevo;
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al persistir el jugador: " + e.getMessage(), e);
        }
    }

    /** Fecha de inicio del torneo vigente, usada como referencia para la elegibilidad. */
    private Date resolverFechaInicioTorneo(Connection conn) throws SQLException {
        List<com.gestionamateur.tpi.progav.modelo.Torneo> torneos = new TorneoDAO().buscarTodos(conn);
        for (com.gestionamateur.tpi.progav.modelo.Torneo t : torneos) {
            if (!"FINALIZADO".equalsIgnoreCase(t.getEstado())) {
                return t.getFechaInicio();
            }
        }
        return torneos.isEmpty() ? null : torneos.get(0).getFechaInicio();
    }

    public boolean verificarElegibilidadCategoria(Jugador jugador, Categoria categoria, Date fechaInicioTorneo) throws TorneoExcepcion {
        if (jugador == null || categoria == null) {
            return false;
        }
        if (fechaInicioTorneo == null) {
            throw new TorneoExcepcion("La fecha de inicio del torneo es obligatoria para calcular la edad reglamentaria del jugador.");
        }
        int edad = calcularEdad(jugador.getFechaNacimiento(), fechaInicioTorneo);

        if (edad < categoria.getEdadMinima()) {
            LOGGER.warning("Jugador " + jugador.getNombre() + " no apto para " + categoria.getNombre() + ": tiene " + edad + " años (mínimo " + categoria.getEdadMinima() + ").");
            return false;
        }

        if (categoria.getEdadMaxima() != null && edad > categoria.getEdadMaxima()) {
            LOGGER.warning("Jugador " + jugador.getNombre() + " no apto para " + categoria.getNombre() + ": tiene " + edad + " años (máximo " + categoria.getEdadMaxima() + ").");
            return false;
        }

        return true;
    }

    public int calcularEdad(Date fechaNacimiento, Date fechaReferencia) {
        if (fechaNacimiento == null || fechaReferencia == null) {
            return 0;
        }
        LocalDate nac = fechaNacimiento.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate ref = fechaReferencia.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return Period.between(nac, ref).getYears();
    }

    public Jugador buscarPorId(Long idJugador) throws TorneoExcepcion {
        try {
            return jugadorDAO.buscarPorId(idJugador);
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al buscar jugador por ID.", e);
        }
    }

    public Jugador buscarPorDni(int dni) throws TorneoExcepcion {
        try {
            return jugadorDAO.buscarPorDni(dni);
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al buscar jugador por DNI.", e);
        }
    }

    public List<Jugador> listarTodos() throws TorneoExcepcion {
        try {
            return jugadorDAO.buscarTodos();
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al listar jugadores.", e);
        }
    }
}
