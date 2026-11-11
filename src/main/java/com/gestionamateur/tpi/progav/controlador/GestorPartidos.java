package com.gestionamateur.tpi.progav.controlador;

import com.gestionamateur.tpi.progav.dao.PartidoDAO;
import com.gestionamateur.tpi.progav.dao.PartidoEquipoDAO;
import com.gestionamateur.tpi.progav.modelo.Arbitro;
import com.gestionamateur.tpi.progav.modelo.Cancha;
import com.gestionamateur.tpi.progav.modelo.Cronologia;
import com.gestionamateur.tpi.progav.modelo.Equipo;
import com.gestionamateur.tpi.progav.modelo.Motivo;
import com.gestionamateur.tpi.progav.modelo.Partido;
import com.gestionamateur.tpi.progav.modelo.PartidoEquipo;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.servicio.PartidoService;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GestorPartidos {

    private static final Logger LOGGER = Logger.getLogger(GestorPartidos.class.getName());
    private static final List<Partido> dbPartidos = new ArrayList<>();
    private static final List<PartidoEquipo> dbPartidoEquipos = new ArrayList<>();
    private static long secuenciaIdPartido = 1L;

    private final PartidoService partidoService = new PartidoService();
    private final PartidoDAO partidoDAO = new PartidoDAO();
    private final PartidoEquipoDAO partidoEquipoDAO = new PartidoEquipoDAO();

    // CU 3: Programar partidos completo con persistencia JDBC
    public String programarPartido(Equipo local, Equipo visitante, Date fechaHora, Cancha sede) {
        return programarPartido(local, visitante, fechaHora, sede, null);
    }

    public String programarPartido(Equipo local, Equipo visitante, Date fechaHora, Cancha sede, Usuario operador) {
        try {
            if (local == null || visitante == null) {
                return "ERROR: Debe seleccionar tanto el equipo local como el visitante.";
            }
            if (local.getNombre().equalsIgnoreCase(visitante.getNombre())) {
                return "ERROR: Un equipo no puede jugar un partido contra sí mismo.";
            }
            if (fechaHora == null) {
                return "ERROR: La fecha y hora del encuentro son obligatorias.";
            }
            if (sede == null) {
                return "ERROR: Debe seleccionar una cancha/sede válida.";
            }

            if (local.getIdEquipo() != null && visitante.getIdEquipo() != null && sede.getIdCancha() != null) {
                Long idFase = resolverIdFaseVigente();
                if (idFase == null) {
                    return "ERROR: No hay ninguna fase disponible para el encuentro. "
                            + "Cree un torneo con al menos una fase antes de programar partidos.";
                }
                Arbitro arbitroAsignado = buscarArbitroDisponible(fechaHora);
                if (arbitroAsignado == null) {
                    return "ERROR: No hay ningun arbitro disponible para el horario seleccionado. "
                            + "Registre un arbitro o elija otra franja horaria.";
                }
                Long idArbitro = arbitroAsignado.getId();
                Partido pBD = partidoService.programarPartido(idFase, local.getIdEquipo(), visitante.getIdEquipo(), sede.getIdCancha(), idArbitro, fechaHora, 90, operador);
                dbPartidos.add(pBD);
                String msg = "ÉXITO: Partido #" + pBD.getIdPartido() + " registrado en la base de datos (" + local.getNombre() + " vs " + visitante.getNombre() + ").";
                msg += "\nÁrbitro asignado: " + arbitroAsignado.getNombre() + " " + arbitroAsignado.getApellido();
                return msg;
            } else {
                return "ERROR: Los equipos y la sede deben estar registrados formalmente en la base de datos para programar el partido.";
            }
        } catch (TorneoExcepcion ex) {
            LOGGER.warning("Validación rechazada en GestorPartidos: " + ex.getMessage());
            return "ERROR DE VALIDACIÓN: " + ex.getMessage();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fallo crítico en GestorPartidos", e);
            return "ERROR DE CONEXIÓN: La base de datos no está disponible (" + e.getMessage() + "). No se guardaron cambios.";
        }
    }

    public String programarPartido(Date fechaHora, Cancha sede) {
        if (fechaHora == null || sede == null || sede.getIdCancha() == null) {
            return "ERROR: Parámetros inválidos para programar el partido.";
        }
        return "ADVERTENCIA: Para persistir en base de datos debe asignar los equipos participantes.";
    }

    /**
     * Resuelve la fase donde se encuadra el encuentro: la primera fase del torneo vigente
     * (el mas reciente que no este FINALIZADO y que tenga fases). Evita depender de un
     * identificador de torneo fijo.
     */
    private Long resolverIdFaseVigente() {
        try {
            com.gestionamateur.tpi.progav.servicio.TorneoService torneoService =
                    new com.gestionamateur.tpi.progav.servicio.TorneoService();
            List<com.gestionamateur.tpi.progav.modelo.Torneo> torneos = torneoService.listarTodos();
            for (com.gestionamateur.tpi.progav.modelo.Torneo t : torneos) {
                if ("FINALIZADO".equalsIgnoreCase(t.getEstado())) {
                    continue;
                }
                List<com.gestionamateur.tpi.progav.modelo.Fase> fases =
                        new com.gestionamateur.tpi.progav.dao.FaseDAO().buscarPorTorneo(t.getIdTorneo());
                if (fases != null && !fases.isEmpty()) {
                    return fases.get(0).getIdFase();
                }
            }
            for (com.gestionamateur.tpi.progav.modelo.Torneo t : torneos) {
                List<com.gestionamateur.tpi.progav.modelo.Fase> fases =
                        new com.gestionamateur.tpi.progav.dao.FaseDAO().buscarPorTorneo(t.getIdTorneo());
                if (fases != null && !fases.isEmpty()) {
                    return fases.get(0).getIdFase();
                }
            }
        } catch (Exception e) {
            LOGGER.warning("No se pudieron resolver las fases disponibles del torneo: " + e.getMessage());
        }
        return null;
    }

    // CU 3.1: Busca un arbitro registrado que no tenga otro encuentro asignado
    // en una franja horaria incompatible. La comprobacion se hace contra PostgreSQL.
    private Arbitro buscarArbitroDisponible(Date fechaHora) {
        List<Arbitro> arbitros = GestorUsuarios.getArbitrosRegistrados();
        if (arbitros.isEmpty()) {
            return null;
        }
        List<Partido> partidosPendientes = new ArrayList<>();
        try {
            partidosPendientes = partidoDAO.buscarPendientesDeFinalizacion();
        } catch (SQLException e) {
            LOGGER.warning("No se pudieron consultar los partidos pendientes para evaluar "
                    + "la disponibilidad de arbitros: " + e.getMessage());
        }
        for (Arbitro arb : arbitros) {
            boolean ocupado = false;
            for (Partido p : partidosPendientes) {
                if (p.getArbitroDesignado() == null || p.getFechaHora() == null) {
                    continue;
                }
                if (!p.getArbitroDesignado().getUsuario().equalsIgnoreCase(arb.getUsuario())) {
                    continue;
                }
                long minutos = Math.abs(p.getFechaHora().getTime() - fechaHora.getTime()) / 60000L;
                if (minutos < 90) {
                    ocupado = true;
                    break;
                }
            }
            if (!ocupado) {
                return arb;
            }
        }
        return null;
    }

    /**
     * CU 4: Cierre oficial del acta con las incidencias registradas por el arbitro.
     * El marcador se calcula a partir de la cronologia informada (PASO 6 del protocolo).
     */
    public String cerrarActa(Long idPartido, Date fechaHoraFinReal, List<Cronologia> incidencias, Usuario operador) {
        try {
            if (idPartido == null) {
                return "ERROR: Debe seleccionar el partido a oficializar.";
            }
            if (fechaHoraFinReal == null) {
                return "ERROR: Debe indicar la hora real de finalizacion del encuentro.";
            }
            if (operador == null) {
                return "ERROR: No hay un arbitro autenticado en la sesion actual.";
            }
            partidoService.cerrarActaPartido(idPartido, fechaHoraFinReal, incidencias, operador);
            sincronizarPartidoEnMemoria(idPartido);
            return "ÉXITO: Acta cerrada y resultado oficializado en la base de datos.";
        } catch (TorneoExcepcion ex) {
            LOGGER.warning("Cierre de acta rechazado: " + ex.getMessage());
            return "ERROR DE VALIDACIÓN: " + ex.getMessage();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fallo crítico al cerrar el acta", e);
            return "ERROR DE CONEXIÓN: La base de datos no está disponible (" + e.getMessage() + "). No se guardaron cambios.";
        }
    }

    /**
     * CU 4: Cargar resultados e incidencias (protocolo atómico de 11 pasos)
     */
    public String procesarActa(Long idPartido, String golesLocales, String golesVisitantes) {
        return procesarActa(idPartido, golesLocales, golesVisitantes, null);
    }

    public String procesarActa(Long idPartido, String golesLocales, String golesVisitantes, Usuario operador) {
        if (idPartido == null) {
            return "ERROR: Debe especificar el ID del partido a oficializar.";
        }
        if (operador == null) {
            return "ERROR DE VALIDACIÓN: Se requiere un operador autenticado para oficializar el acta.";
        }
        if (golesLocales == null || golesLocales.trim().isEmpty() ||
            golesVisitantes == null || golesVisitantes.trim().isEmpty()) {
            return "ERROR DE VALIDACIÓN: Debe indicar los goles tanto para el equipo local como para el visitante.";
        }
        int gLoc;
        int gVis;
        try {
            gLoc = Integer.parseInt(golesLocales.trim());
            gVis = Integer.parseInt(golesVisitantes.trim());
        } catch (NumberFormatException e) {
            return "ERROR DE VALIDACIÓN: La cantidad de goles debe ser un número entero válido.";
        }
        if (gLoc < 0 || gVis < 0) {
            return "ERROR DE VALIDACIÓN: La cantidad de goles no puede ser negativa.";
        }
        try {
            partidoService.cerrarActaPartido(idPartido, new Date(), gLoc, gVis, operador);
            sincronizarPartidoEnMemoria(idPartido);
            return "ÉXITO: Acta guardada y resultado oficializado en la base de datos (Protocolo 11 pasos completado).";
        } catch (TorneoExcepcion ex) {
            return "ERROR DE VALIDACIÓN: " + ex.getMessage();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fallo crítico al cerrar acta: " + e.getMessage(), e);
            return "ERROR CRÍTICO: Falló el procesamiento del acta (" + e.getMessage() + "). Transacción revertida.";
        }
    }

    // CU 5: Registrar postergaciones o suspensiones
    public String procesarSuspension(Long idPartido, String textoMotivo, Usuario operador) {
        if (idPartido == null) {
            return "ERROR: Debe especificar el ID del partido a postergar.";
        }
        if (operador == null) {
            return "ERROR DE VALIDACIÓN: Acceso denegado: Se requiere un operador autenticado para postergar encuentros.";
        }
        try {
            partidoService.posponerPartido(idPartido, textoMotivo, operador);
            sincronizarPartidoEnMemoria(idPartido);
            return "ÉXITO: Partido pospuesto formalmente.";
        } catch (TorneoExcepcion ex) {
            return "ERROR DE VALIDACIÓN: " + ex.getMessage();
        } catch (Exception e) {
            return "ERROR CRÍTICO: Falló la postergación del partido (" + e.getMessage() + ").";
        }
    }

    public String procesarSuspensionPorIncidencias(Long idPartido, String textoMotivo, Usuario operador) {
        if (idPartido == null) {
            return "ERROR: Debe especificar el ID del partido a suspender.";
        }
        if (operador == null) {
            return "ERROR DE VALIDACIÓN: Acceso denegado: Se requiere un operador autenticado para suspender encuentros.";
        }
        try {
            partidoService.suspenderPartido(idPartido, textoMotivo, operador);
            sincronizarPartidoEnMemoria(idPartido);
            return "ÉXITO: Partido suspendido formalmente.";
        } catch (TorneoExcepcion ex) {
            return "ERROR DE VALIDACIÓN: " + ex.getMessage();
        } catch (Exception e) {
            return "ERROR CRÍTICO: Falló la suspensión del partido (" + e.getMessage() + ").";
        }
    }

    private void sincronizarPartidoEnMemoria(Long idPartido) {
        try {
            Partido actualizado = partidoDAO.buscarPorId(idPartido);
            if (actualizado != null) {
                for (int i = 0; i < dbPartidos.size(); i++) {
                    if (idPartido.equals(dbPartidos.get(i).getIdPartido())) {
                        dbPartidos.set(i, actualizado);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("No se pudo sincronizar en memoria el partido ID " + idPartido + ": " + e.getMessage());
        }
    }

    public static List<Partido> getPartidosProgramados() {
        try {
            PartidoDAO dao = new PartidoDAO();
            List<Partido> listaBD = dao.buscarTodos();
            if (listaBD != null && !listaBD.isEmpty()) {
                return listaBD;
            }
        } catch (Exception e) {
            LOGGER.warning("No se pudieron cargar partidos desde base de datos: " + e.getMessage());
        }
        return dbPartidos;
    }

    public String obtenerResumenFixture() {
        List<Partido> lista = getPartidosProgramados();
        if (lista.isEmpty()) {
            return "Aún no hay partidos programados en el fixture.";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        StringBuilder sb = new StringBuilder();
        for (Partido p : lista) {
            String arb = (p.getArbitroDesignado() != null)
                    ? p.getArbitroDesignado().getNombre() + " " + p.getArbitroDesignado().getApellido()
                    : "Sin asignar";
            String sedeNom = (p.getSede() != null) ? p.getSede().getNombre() : "S/D";
            sb.append(String.format("Partido #%d | %s hs | Sede: %s | Árbitro: %s | [%s]\n",
                    p.getIdPartido(), sdf.format(p.getFechaHora()),
                    sedeNom, arb, p.getEstadoPartido().name()));
        }
        return sb.toString();
    }
}
