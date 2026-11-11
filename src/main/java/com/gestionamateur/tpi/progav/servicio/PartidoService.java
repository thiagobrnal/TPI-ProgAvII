package com.gestionamateur.tpi.progav.servicio;

import com.gestionamateur.tpi.progav.dao.CanchaDAO;
import com.gestionamateur.tpi.progav.dao.ConfiguracionDAO;
import com.gestionamateur.tpi.progav.dao.ConexionBD;
import com.gestionamateur.tpi.progav.dao.CronologiaDAO;
import com.gestionamateur.tpi.progav.dao.EquipoDAO;
import com.gestionamateur.tpi.progav.dao.FaseDAO;
import com.gestionamateur.tpi.progav.dao.JugadorDAO;
import com.gestionamateur.tpi.progav.dao.MotivoDAO;
import com.gestionamateur.tpi.progav.dao.PartidoDAO;
import com.gestionamateur.tpi.progav.dao.PartidoEquipoDAO;
import com.gestionamateur.tpi.progav.dao.SancionDAO;
import com.gestionamateur.tpi.progav.dao.TorneoDAO;
import com.gestionamateur.tpi.progav.modelo.Cancha;
import com.gestionamateur.tpi.progav.modelo.Cronologia;
import com.gestionamateur.tpi.progav.modelo.Fase;
import com.gestionamateur.tpi.progav.modelo.Jugador;
import com.gestionamateur.tpi.progav.modelo.Motivo;
import com.gestionamateur.tpi.progav.modelo.Partido;
import com.gestionamateur.tpi.progav.modelo.PartidoEquipo;
import com.gestionamateur.tpi.progav.modelo.Sancion;
import com.gestionamateur.tpi.progav.modelo.SistemaLogs;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.Torneo;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class PartidoService {

    private static final Logger LOGGER = Logger.getLogger(PartidoService.class.getName());

    private final PartidoDAO partidoDAO;
    private final PartidoEquipoDAO partidoEquipoDAO;
    private final CronologiaDAO cronologiaDAO;
    private final SancionDAO sancionDAO;
    private final FaseDAO faseDAO;
    private final JugadorDAO jugadorDAO;
    private final ConfiguracionDAO configuracionDAO;
    private final CanchaDAO canchaDAO;
    private final EquipoDAO equipoDAO;
    private final TorneoDAO torneoDAO;
    private final MotivoDAO motivoDAO;

    public PartidoService() {
        this.partidoDAO = new PartidoDAO();
        this.partidoEquipoDAO = new PartidoEquipoDAO();
        this.cronologiaDAO = new CronologiaDAO();
        this.sancionDAO = new SancionDAO();
        this.faseDAO = new FaseDAO();
        this.jugadorDAO = new JugadorDAO();
        this.configuracionDAO = new ConfiguracionDAO();
        this.canchaDAO = new CanchaDAO();
        this.equipoDAO = new EquipoDAO();
        this.torneoDAO = new TorneoDAO();
        this.motivoDAO = new MotivoDAO();
    }

    public Partido programarPartido(Long idFase, Long idLocal, Long idVisitante, Long idCancha,
                                     Long idArbitro, Date fechaHora, int duracionMinutos, Usuario operador) throws TorneoExcepcion {
        if (operador == null) {
            throw new TorneoExcepcion("Acceso denegado: se requiere un operador autenticado para programar encuentros.");
        }
        TiposDatos.Rol rol = operador.getRol();
        if (rol != TiposDatos.Rol.ADMINISTRADOR && rol != TiposDatos.Rol.ORGANIZADOR) {
            throw new TorneoExcepcion("Acceso denegado: El rol " + rol + " no tiene permisos para programar encuentros.");
        }
        return programarPartido(idFase, idLocal, idVisitante, idCancha, idArbitro, fechaHora, duracionMinutos);
    }

    private Partido programarPartido(Long idFase, Long idLocal, Long idVisitante, Long idCancha,
                                     Long idArbitro, Date fechaHora, int duracionMinutos) throws TorneoExcepcion {
        if (idLocal.equals(idVisitante)) {
            throw new TorneoExcepcion("Un partido debe disputarse entre dos equipos distintos.");
        }
        try (Connection conn = ConexionBD.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                Cancha cancha = canchaDAO.buscarPorId(conn, idCancha);
                if (cancha == null) {
                    throw new TorneoExcepcion("La sede especificada no existe.");
                }

                Partido p = new Partido(idFase, fechaHora, cancha, duracionMinutos > 0 ? duracionMinutos : 90);
                Long idPartido = partidoDAO.insertar(conn, p, idFase, idCancha, idArbitro);
                p.setIdPartido(idPartido);

                // Asignar condición local y visitante
                partidoEquipoDAO.asignarEquiposAPartido(conn, idPartido, idLocal, idVisitante);

                conn.commit();
                SistemaLogs.info(PartidoService.class, "Partido programado ID: " + idPartido + " (Local: " + idLocal + " vs Vis: " + idVisitante + ")");
                return p;
            } catch (Exception e) {
                conn.rollback();
                LOGGER.severe("Error al programar partido, rollback: " + e.getMessage());
                throw new TorneoExcepcion("No se pudo programar el encuentro: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error de base de datos al programar partido.", e);
        }
    }

    public void registrarIncidencia(Long idPartido, Long idJugador, int minuto, TiposDatos.TipoAccion accion,
                                   String tipoExpulsion, String observacion) throws TorneoExcepcion {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            Partido p = partidoDAO.buscarPorId(conn, idPartido);
            if (p == null) {
                throw new TorneoExcepcion("El partido no existe.");
            }
            if (p.getEstadoPartido() == TiposDatos.EstadoPartido.FINALIZADO) {
                throw new TorneoExcepcion("No se pueden agregar incidencias a un partido ya finalizado.");
            }

            Jugador j = jugadorDAO.buscarPorId(conn, idJugador);
            if (j == null) {
                throw new TorneoExcepcion("El jugador autor de la incidencia no existe.");
            }

            Cronologia c = new Cronologia(minuto, accion, tipoExpulsion, j, p, observacion);
            cronologiaDAO.insertar(conn, c, idPartido, idJugador);
            SistemaLogs.info(PartidoService.class, "Incidencia registrada: [" + accion + "] Jugador ID: " + idJugador + " en Partido ID: " + idPartido);
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al registrar incidencia: " + e.getMessage(), e);
        }
    }

    public void posponerPartido(Long idPartido, String textoMotivo, Usuario operador) throws TorneoExcepcion {
        validarOperadorAdministrativo(operador, "postergar");
        if (idPartido == null) {
            throw new TorneoExcepcion("Debe especificar un ID de partido válido.");
        }
        Motivo motivo = new Motivo(textoMotivo);

        try (Connection conn = ConexionBD.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                Partido p = partidoDAO.bloquearParaFinalizacion(conn, idPartido);
                if (p == null) {
                    throw new TorneoExcepcion("El partido con ID " + idPartido + " no existe.");
                }
                if (p.getEstadoPartido() == TiposDatos.EstadoPartido.FINALIZADO) {
                    throw new TorneoExcepcion("No se puede postergar un partido que ya ha sido finalizado.");
                }
                if (p.getEstadoPartido() == TiposDatos.EstadoPartido.POSTERGADO) {
                    throw new TorneoExcepcion("El partido ya se encuentra en estado POSTERGADO.");
                }
                if (p.getEstadoPartido() == TiposDatos.EstadoPartido.SUSPENDIDO) {
                    throw new TorneoExcepcion("No se puede postergar un partido que se encuentra en estado SUSPENDIDO.");
                }
                if (p.getEstadoPartido() != TiposDatos.EstadoPartido.PROGRAMADO) {
                    throw new TorneoExcepcion("Solo se pueden postergar partidos que se encuentren en estado PROGRAMADO.");
                }

                p.posponerPartido(motivo);
                Long idMotivo = motivoDAO.obtenerOInsertar(conn, motivo);
                partidoDAO.suspenderOPostergar(conn, idPartido, "POSTERGADO", idMotivo);
                conn.commit();
                SistemaLogs.info(PartidoService.class, "Partido ID " + idPartido + " formalmente POSTERGADO por operador: " + operador.getNombreUsuario());
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof TorneoExcepcion) {
                    throw (TorneoExcepcion) e;
                }
                throw new TorneoExcepcion("Error al postergar el partido ID " + idPartido + ": " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error de conexión a la base de datos: " + e.getMessage(), e);
        }
    }

    public void suspenderPartido(Long idPartido, String textoMotivo, Usuario operador) throws TorneoExcepcion {
        validarOperadorAdministrativo(operador, "suspender");
        if (idPartido == null) {
            throw new TorneoExcepcion("Debe especificar un ID de partido válido.");
        }
        Motivo motivo = new Motivo(textoMotivo);

        try (Connection conn = ConexionBD.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                Partido p = partidoDAO.bloquearParaFinalizacion(conn, idPartido);
                if (p == null) {
                    throw new TorneoExcepcion("El partido con ID " + idPartido + " no existe.");
                }
                if (p.getEstadoPartido() == TiposDatos.EstadoPartido.FINALIZADO) {
                    throw new TorneoExcepcion("No se puede suspender un partido que ya ha sido finalizado.");
                }
                if (p.getEstadoPartido() == TiposDatos.EstadoPartido.SUSPENDIDO) {
                    throw new TorneoExcepcion("El partido ya se encuentra en estado SUSPENDIDO.");
                }
                if (p.getEstadoPartido() == TiposDatos.EstadoPartido.POSTERGADO) {
                    throw new TorneoExcepcion("No se puede suspender un partido que se encuentra en estado POSTERGADO.");
                }

                p.suspenderPartido(motivo);
                Long idMotivo = motivoDAO.obtenerOInsertar(conn, motivo);
                partidoDAO.suspenderOPostergar(conn, idPartido, "SUSPENDIDO", idMotivo);
                conn.commit();
                SistemaLogs.info(PartidoService.class, "Partido ID " + idPartido + " formalmente SUSPENDIDO por operador: " + operador.getNombreUsuario());
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof TorneoExcepcion) {
                    throw (TorneoExcepcion) e;
                }
                throw new TorneoExcepcion("Error al suspender el partido ID " + idPartido + ": " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error de conexión a la base de datos: " + e.getMessage(), e);
        }
    }

    private void validarOperadorAdministrativo(Usuario operador, String accion) throws TorneoExcepcion {
        if (operador == null) {
            throw new TorneoExcepcion("Acceso denegado: Se requiere un operador autenticado para " + accion + " encuentros.");
        }
        TiposDatos.Rol rol = operador.getRol();
        if (rol != TiposDatos.Rol.ADMINISTRADOR && rol != TiposDatos.Rol.ORGANIZADOR) {
            throw new TorneoExcepcion("Acceso denegado: El rol " + rol + " no tiene permisos para " + accion + " encuentros.");
        }
    }

    public void cerrarActaPartido(Long idPartido, Date fechaHoraFinReal, List<Cronologia> incidenciasFinales, Usuario operador) throws TorneoExcepcion {
        if (operador == null) {
            throw new TorneoExcepcion("Acceso denegado: se requiere un operador autenticado para cerrar actas de partido.");
        }
        TiposDatos.Rol rol = operador.getRol();
        if (rol != TiposDatos.Rol.ADMINISTRADOR && rol != TiposDatos.Rol.ORGANIZADOR && rol != TiposDatos.Rol.ARBITRO) {
            throw new TorneoExcepcion("Acceso denegado: el rol " + rol + " no tiene permisos para cerrar actas de partido.");
        }
        cerrarActaPartidoInterno(idPartido, fechaHoraFinReal, incidenciasFinales, null, null, operador);
    }

    public void cerrarActaPartido(Long idPartido, Date fechaHoraFinReal, int golesLocalDeclarados, int golesVisitanteDeclarados, Usuario operador) throws TorneoExcepcion {
        if (golesLocalDeclarados < 0 || golesVisitanteDeclarados < 0) {
            throw new TorneoExcepcion("La cantidad de goles no puede ser negativa.");
        }
        if (operador == null) {
            throw new TorneoExcepcion("Acceso denegado: se requiere un operador autenticado para cerrar actas de partido.");
        }
        TiposDatos.Rol rol = operador.getRol();
        if (rol != TiposDatos.Rol.ADMINISTRADOR && rol != TiposDatos.Rol.ORGANIZADOR && rol != TiposDatos.Rol.ARBITRO) {
            throw new TorneoExcepcion("Acceso denegado: el rol " + rol + " no tiene permisos para cerrar actas de partido.");
        }
        cerrarActaPartidoInterno(idPartido, fechaHoraFinReal, null, golesLocalDeclarados, golesVisitanteDeclarados, operador);
    }

    /**
     * PROTOCOLO TRANSACCIONAL DE CIERRE DE ACTA DE PARTIDO EN 11 PASOS ATÓMICOS.
     * Garantiza atomicidad, consistencia, bloqueo pesimista, acumulación Variante A e idempotencia de sanciones.
     */
    private void cerrarActaPartido(Long idPartido, Date fechaHoraFinReal, List<Cronologia> incidenciasFinales) throws TorneoExcepcion {
        throw new TorneoExcepcion("Acceso denegado: se requiere un operador autenticado para cerrar actas de partido.");
    }

    private void cerrarActaPartidoInterno(Long idPartido, Date fechaHoraFinReal, List<Cronologia> incidenciasFinales,
                                          Integer golesLocalDeclarados, Integer golesVisitanteDeclarados,
                                          Usuario operador) throws TorneoExcepcion {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            // PASO 1: Iniciar transacción explícita
            conn.setAutoCommit(false);
            try {
                // PASO 2: Adquirir lock pesimista sobre el partido
                Partido partido = partidoDAO.bloquearParaFinalizacion(conn, idPartido);
                if (partido == null) {
                    throw new TorneoExcepcion("El partido con ID " + idPartido + " no existe.");
                }

                // Validación de titularidad arbitral si el operador posee rol ARBITRO
                if (operador != null && operador.getRol() == TiposDatos.Rol.ARBITRO) {
                    if (partido.getArbitroDesignado() != null && partido.getArbitroDesignado().getId() != null) {
                        if (operador.getId() == null || !partido.getArbitroDesignado().getId().equals(operador.getId())) {
                            throw new TorneoExcepcion("Acceso denegado: El árbitro " + operador.getNombreUsuario()
                                    + " no es el árbitro oficial designado para dirigir y cerrar el acta de este encuentro.");
                        }
                    }
                }

                // PASO 3: Validar estado pre-cierre
                if (partido.getEstadoPartido() == TiposDatos.EstadoPartido.FINALIZADO) {
                    throw new TorneoExcepcion("Conflicto: El partido ya ha sido finalizado con anterioridad.");
                }
                if (partido.getEstadoPartido() == TiposDatos.EstadoPartido.SUSPENDIDO ||
                    partido.getEstadoPartido() == TiposDatos.EstadoPartido.POSTERGADO) {
                    throw new TorneoExcepcion("No se puede cerrar el acta de un partido en estado " + partido.getEstadoPartido());
                }

                // PASO 4: Determinar y validar hora real de finalización (obligatoria, sin fallback silencioso)
                if (fechaHoraFinReal == null) {
                    throw new TorneoExcepcion("La fecha y hora real de finalización es obligatoria para el cierre oficial del acta.");
                }
                if (fechaHoraFinReal.after(new Date())) {
                    throw new TorneoExcepcion("La fecha y hora real de finalización no puede ser posterior al momento actual.");
                }
                Date finReal = fechaHoraFinReal;

                // PASO 5: Registrar incidencias finales si se suministraron en el acta
                if (incidenciasFinales != null && !incidenciasFinales.isEmpty()) {
                    for (Cronologia inc : incidenciasFinales) {
                        cronologiaDAO.insertar(conn, inc, idPartido, inc.getAutor().getId());
                    }
                }

                // PASO 6: Computar y consolidar marcador oficial en partido_equipos
                PartidoEquipo local = partidoEquipoDAO.buscarLocal(conn, idPartido);
                PartidoEquipo visitante = partidoEquipoDAO.buscarVisitante(conn, idPartido);
                if (local == null || visitante == null) {
                    throw new TorneoExcepcion("El partido no cuenta con los dos equipos participantes configurados.");
                }

                int golesLocal = cronologiaDAO.contarGolesPorEquipo(conn, idPartido, local.getEquipo().getIdEquipo());
                int golesVisitante = cronologiaDAO.contarGolesPorEquipo(conn, idPartido, visitante.getEquipo().getIdEquipo());

                if (golesLocalDeclarados != null && golesVisitanteDeclarados != null) {
                    if (golesLocal != golesLocalDeclarados.intValue() || golesVisitante != golesVisitanteDeclarados.intValue()) {
                        throw new TorneoExcepcion("Discrepancia en acta: El marcador declarado (" + golesLocalDeclarados + " - " + golesVisitanteDeclarados
                                + ") no coincide con los goles registrados en la cronología del encuentro (" + golesLocal + " - " + golesVisitante
                                + "). Debe registrar cada gol con su respectivo autor antes de oficializar el acta.");
                    }
                }

                partidoEquipoDAO.actualizarGoles(conn, idPartido, local.getEquipo().getIdEquipo(), golesLocal);
                partidoEquipoDAO.actualizarGoles(conn, idPartido, visitante.getEquipo().getIdEquipo(), golesVisitante);

                // Obtener contexto de torneo y fase
                Long idTorneo = partidoDAO.obtenerIdTorneoPorPartido(conn, idPartido);
                if (idTorneo == null) {
                    throw new TorneoExcepcion("No se pudo resolver el torneo al que pertenece el partido.");
                }
                Torneo torneo = torneoDAO.buscarPorId(conn, idTorneo);
                Fase faseActual = faseDAO.buscarPorId(conn, partido.getIdFase());

                // Obtener incidencias registradas de este partido para procesamiento disciplinario
                List<Cronologia> cronologias = cronologiaDAO.buscarPorPartido(conn, idPartido);
                Set<Long> jugadoresConAmarilla = new HashSet<>();
                Set<Long> jugadoresSancionadosEnEstePartido = new HashSet<>();

                // PASO 7: Evaluar expulsiones del partido (Roja directa y Doble amarilla) con idempotencia
                for (Cronologia c : cronologias) {
                    if (c.getAccion() == TiposDatos.TipoAccion.TARJETA_ROJA) {
                        Long idJugador = c.getAutor().getId();
                        String tipoExp = c.getTipoExpulsion();

                        if ("DOBLE_AMARILLA".equalsIgnoreCase(tipoExp)) {
                            // Sanción automática de 1 fecha por doble amarilla (Estado ACTIVA)
                            Sancion sDobleAmarilla = new Sancion(
                                finReal, null,
                                "Expulsión por doble amonestación en el partido ID " + idPartido,
                                "LEVE", c.getAutor(), partido, torneo,
                                "TORNEO", "DOBLE_AMARILLA", 1, 0, "ACTIVA", finReal
                            );
                            sancionDAO.insertarIdempotente(conn, sDobleAmarilla);
                            jugadoresSancionadosEnEstePartido.add(idJugador);
                        } else {
                            // Expulsión por Roja Directa: PENDIENTE de resolución de tribunal (mínimo 1 fecha cautelar, sin publicación formal aún)
                            Sancion sRojaDirecta = new Sancion(
                                finReal, null,
                                "Expulsión directa por infracción grave en el partido ID " + idPartido + (c.getObservacion() != null ? ": " + c.getObservacion() : ""),
                                "GRAVE", c.getAutor(), partido, torneo,
                                "TORNEO", "ROJA_DIRECTA", 1, 0, "PENDIENTE"
                            );
                            sancionDAO.insertarIdempotente(conn, sRojaDirecta);
                            jugadoresSancionadosEnEstePartido.add(idJugador);
                        }
                    } else if (c.getAccion() == TiposDatos.TipoAccion.TARJETA_AMARILLA) {
                        jugadoresConAmarilla.add(c.getAutor().getId());
                    }
                }

                // PASO 8: Acumulación de tarjetas amarillas según VARIANTE A
                int umbralAmarillas = configuracionDAO.obtenerValorInt(conn, "AMARILLAS_SUSPENSION", 5);
                int ordenInicioVentana = faseDAO.obtenerOrdenInicioVentana(conn, idTorneo, faseActual.getOrden());

                for (Long idJugador : jugadoresConAmarilla) {
                    // Si el jugador fue expulsado por doble amarilla en este partido, Variante A excluye esas amarillas
                    boolean fueExpulsadoPorDobleAmarilla = cronologiaDAO.huboDobleAmarillaEnPartido(conn, idPartido, idJugador);
                    if (!fueExpulsadoPorDobleAmarilla) {
                        int amarillasAcumuladas = cronologiaDAO.contarAmarillasVarianteA(conn, idJugador, idTorneo, ordenInicioVentana);
                        // Si alcanzó el umbral (o múltiplo de 5)
                        if (amarillasAcumuladas > 0 && amarillasAcumuladas % umbralAmarillas == 0) {
                            Jugador jug = jugadorDAO.buscarPorId(conn, idJugador);
                            Sancion sAcumulacion = new Sancion(
                                finReal, null,
                                "Suspensión de 1 fecha por acumulación de " + umbralAmarillas + " tarjetas amarillas reglamentarias (Variante A).",
                                "LEVE", jug, partido, torneo,
                                "TORNEO", "ACUMULACION_AMARILLAS", 1, 0, "ACTIVA", finReal
                            );
                            sancionDAO.insertarIdempotente(conn, sAcumulacion);
                            jugadoresSancionadosEnEstePartido.add(idJugador);
                        }
                    }
                }

                // PASO 9: Actualizar estado de los jugadores recién sancionados a SUSPENDIDO
                for (Long idJugador : jugadoresSancionadosEnEstePartido) {
                    Jugador jug = jugadorDAO.buscarPorId(conn, idJugador);
                    if (jug != null && jug.getEstado() == TiposDatos.EstadoJugador.ACTIVO) {
                        jugadorDAO.actualizarEstado(conn, idJugador, "SUSPENDIDO");
                    }
                }

                // PASO 10: Cumplimiento de sanciones de jugadores inhabilitados que purgaron sanción en este encuentro
                avanzarCumplimientoSanciones(conn, idTorneo, local.getEquipo().getIdEquipo(), jugadoresSancionadosEnEstePartido);
                avanzarCumplimientoSanciones(conn, idTorneo, visitante.getEquipo().getIdEquipo(), jugadoresSancionadosEnEstePartido);

                // PASO 11: Marcar partido como FINALIZADO y confirmar transacción
                partidoDAO.actualizarFinalizacionReal(conn, idPartido, finReal);
                conn.commit();

                SistemaLogs.info(PartidoService.class, "Acta de partido ID " + idPartido + " cerrada exitosamente. Marcador: "
                        + local.getEquipo().getNombre() + " " + golesLocal + " - " + golesVisitante + " " + visitante.getEquipo().getNombre());
            } catch (Exception e) {
                conn.rollback();
                LOGGER.severe("Rollback atómico en cierre de acta de partido ID " + idPartido + ": " + e.getMessage());
                throw new TorneoExcepcion("Fallo en la transacción atómica de cierre de partido: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error de conexión a la base de datos durante el cierre de partido.", e);
        }
    }

    private void avanzarCumplimientoSanciones(Connection conn, Long idTorneo, Long idEquipo, Set<Long> recienSancionados) throws SQLException, TorneoExcepcion {
        List<Jugador> plantel = jugadorDAO.buscarPorEquipo(conn, idEquipo);
        for (Jugador j : plantel) {
            if (recienSancionados.contains(j.getId())) {
                continue; // No purga una sanción originada en este mismo partido
            }
            List<Sancion> sancionesActivas = sancionDAO.buscarActivasPorJugador(conn, j.getId(), idTorneo);
            for (Sancion s : sancionesActivas) {
                sancionDAO.registrarFechaCumplida(conn, s.getId());
                // Verificar si con esta fecha cumplida el jugador queda limpio
                List<Sancion> restantes = sancionDAO.buscarActivasPorJugador(conn, j.getId(), idTorneo);
                if (restantes.isEmpty()) {
                    Jugador jug = jugadorDAO.buscarPorId(conn, j.getId());
                    if (jug != null && jug.getEstado() == TiposDatos.EstadoJugador.SUSPENDIDO) {
                        jugadorDAO.actualizarEstado(conn, j.getId(), "ACTIVO");
                        SistemaLogs.info(PartidoService.class, "Jugador " + j.getNombre() + " cumplió su suspensión y vuelve al estado ACTIVO.");
                    }
                }
            }
        }
    }

    public Partido buscarPorId(Long idPartido) throws TorneoExcepcion {
        try {
            return partidoDAO.buscarPorId(idPartido);
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al buscar partido por ID.", e);
        }
    }

    public List<Partido> listarPartidosPorTorneo(Long idTorneo) throws TorneoExcepcion {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return partidoDAO.buscarPorTorneo(conn, idTorneo);
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al listar partidos del torneo.", e);
        }
    }

    public List<PartidoEquipo> obtenerEquiposDelPartido(Long idPartido) throws TorneoExcepcion {
        try {
            return partidoEquipoDAO.buscarPorPartido(idPartido);
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al obtener equipos del partido.", e);
        }
    }

    public List<Cronologia> obtenerCronologiaDelPartido(Long idPartido) throws TorneoExcepcion {
        try {
            return cronologiaDAO.buscarPorPartido(idPartido);
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al obtener cronología del partido.", e);
        }
    }
}
