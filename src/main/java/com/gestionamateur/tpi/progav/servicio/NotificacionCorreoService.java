package com.gestionamateur.tpi.progav.servicio;

import com.gestionamateur.tpi.progav.dao.ConexionBD;
import com.gestionamateur.tpi.progav.modelo.SistemaLogs;
import com.gestionamateur.tpi.progav.modelo.TransporteCorreo;
import com.gestionamateur.tpi.progav.modelo.TransporteCorreoSmtp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CU-03.2: envio de notificacion por correo a los involucrados de un partido programado.
 *
 * <p>Flujo oficial (especificacion de catedra, CU-03.2):
 * <ol>
 *   <li>El sistema detecta la confirmacion de un nuevo partido o de un cambio en el fixture.</li>
 *   <li>El sistema recopila las direcciones de correo de los capitanes involucrados y del
 *       arbitro asignado.</li>
 *   <li>El sistema redacta automaticamente el mensaje con los detalles del encuentro.</li>
 *   <li>El sistema invoca al servicio de correo electronico para despachar los mensajes.</li>
 *   <li>El sistema registra el envio exitoso en la bitacora interna.</li>
 * </ol>
 *
 * <p>Flujo alternativo A1 (fallo en el envio): si el servicio de correo externo no responde o
 * rechaza la conexion, el error se registra en la bitacora y la notificacion queda pendiente de
 * reintento. **El fallo del correo nunca invalida la operacion deportiva ya persistida.**
 */
public class NotificacionCorreoService {

    private static final Logger LOGGER = Logger.getLogger(NotificacionCorreoService.class.getName());

    private final TransporteCorreo transporte;

    public NotificacionCorreoService() {
        this(new TransporteCorreoSmtp());
    }

    public NotificacionCorreoService(TransporteCorreo transporte) {
        this.transporte = transporte;
    }

    /** Resultado del envio de las notificaciones de un partido. */
    public static final class Resultado {
        public int destinatarios;
        public int enviados;
        public int fallidos;
        public List<String> errores = new ArrayList<>();
        public boolean transporteConfigurado;

        @Override
        public String toString() {
            return "destinatarios=" + destinatarios + " enviados=" + enviados + " fallidos=" + fallidos
                    + " transporteConfigurado=" + transporteConfigurado;
        }
    }

    /**
     * Notifica a los capitanes de ambos equipos y al arbitro asignado la programacion de un
     * partido.
     *
     * @param idPartido identificador del partido ya persistido
     * @return detalle de los envios realizados
     */
    public Resultado notificarProgramacion(Long idPartido) {
        Resultado resultado = new Resultado();
        resultado.transporteConfigurado = transporte.estaConfigurado();
        if (!resultado.transporteConfigurado) {
            SistemaLogs.warning(NotificacionCorreoService.class,
                    "Notificacion de correo omitida para el partido " + idPartido
                    + ": no hay servicio de correo configurado.");
            return resultado;
        }
        List<String[]> involucrados = obtenerInvolucrados(idPartido);
        if (involucrados.isEmpty()) {
            return resultado;
        }
        String[] datos = obtenerDatosPartido(idPartido);
        String asunto = "Partido programado: " + datos[0] + " vs " + datos[1];
        String cuerpo = construirCuerpo(datos);
        for (String[] inv : involucrados) {
            String correo = inv[1];
            if (correo == null || correo.trim().isEmpty()) {
                resultado.fallidos++;
                resultado.errores.add("Sin direccion de correo registrada");
                continue;
            }
            resultado.destinatarios++;
            try {
                if (transporte.enviar(correo, asunto, cuerpo)) {
                    resultado.enviados++;
                } else {
                    resultado.fallidos++;
                    resultado.errores.add("El transporte no confirmo el envio");
                }
            } catch (Exception e) {
                // Flujo alternativo A1: el fallo del correo no invalida el partido ya persistido.
                resultado.fallidos++;
                resultado.errores.add(correo + ": " + e.getMessage());
                LOGGER.log(Level.WARNING, "No se pudo enviar la notificacion de correo a " + correo
                        + ": " + e.getMessage());
            }
        }
        SistemaLogs.info(NotificacionCorreoService.class,
                "Notificaciones del partido " + idPartido + ": " + resultado);
        return resultado;
    }

    /**
     * Obtiene los involucrados del partido con su correo: capitanes local y visitante y
     * arbitro asignado.
     */
    private List<String[]> obtenerInvolucrados(Long idPartido) {
        List<String[]> involucrados = new ArrayList<>();
        String sql = "SELECT e.nombre, u.mail, 'capitan' AS rol "
                   + "FROM partido_equipos pe "
                   + "JOIN equipos e ON e.id_equipo = pe.id_equipo "
                   + "JOIN usuarios u ON u.id_usuario = e.id_capitan "
                   + "WHERE pe.id_partido = ? "
                   + "UNION ALL "
                   + "SELECT 'Arbitro designado', u.mail, 'arbitro' "
                   + "FROM partidos p JOIN usuarios u ON u.id_usuario = p.id_arbitro "
                   + "WHERE p.id_partido = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idPartido);
            ps.setLong(2, idPartido);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    involucrados.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3)});
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "No se pudieron obtener los involucrados del partido " + idPartido, e);
        }
        return involucrados;
    }

    /**
     * Obtiene los datos del encuentro para el asunto y el cuerpo del mensaje:
     * equipo local, equipo visitante, fecha y hora, sede y arbitro.
     */
    private String[] obtenerDatosPartido(Long idPartido) {
        String sql = "SELECT COALESCE(el.nombre, 'Local'), COALESCE(ev.nombre, 'Visitante'), "
                   + "       to_char(p.fecha_hora, 'DD/MM/YYYY HH24:MI'), "
                   + "       COALESCE(c.nombre, 'Sede por confirmar'), "
                   + "       COALESCE(ua.nombre || ' ' || ua.apellido, 'Arbitro por designar') "
                   + "FROM partidos p "
                   + "LEFT JOIN partido_equipos pel ON pel.id_partido = p.id_partido AND pel.es_local = TRUE "
                   + "LEFT JOIN partido_equipos pev ON pev.id_partido = p.id_partido AND pev.es_local = FALSE "
                   + "LEFT JOIN equipos el ON el.id_equipo = pel.id_equipo "
                   + "LEFT JOIN equipos ev ON ev.id_equipo = pev.id_equipo "
                   + "LEFT JOIN canchas c ON c.id_cancha = p.id_cancha "
                   + "LEFT JOIN usuarios ua ON ua.id_usuario = p.id_arbitro "
                   + "WHERE p.id_partido = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idPartido);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.getString(5)};
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "No se pudieron obtener los datos del partido " + idPartido, e);
        }
        return new String[]{"Local", "Visitante", "(sin fecha)", "(sin sede)", "(arbitro por designar)"};
    }

    /** Redacta el mensaje con los detalles del encuentro. */
    private String construirCuerpo(String[] datos) {
        StringBuilder sb = new StringBuilder();
        sb.append("Se ha programado un nuevo partido en el torneo.\n\n");
        sb.append("Equipos: ").append(datos[0]).append(" vs ").append(datos[1]).append("\n");
        sb.append("Fecha y hora: ").append(datos[2]).append("\n");
        sb.append("Sede: ").append(datos[3]).append("\n");
        sb.append("Arbitro: ").append(datos[4]).append("\n\n");
        sb.append("Por favor, confirmen su disponibilidad.\n\n");
        sb.append("TPI - Torneo de Fútbol Amateur\n");
        sb.append("Generado el ").append(new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date())).append("\n");
        return sb.toString();
    }
}
