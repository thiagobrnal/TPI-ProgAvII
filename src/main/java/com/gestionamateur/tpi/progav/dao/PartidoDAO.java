package com.gestionamateur.tpi.progav.dao;

import com.gestionamateur.tpi.progav.modelo.Arbitro;
import com.gestionamateur.tpi.progav.modelo.Cancha;
import com.gestionamateur.tpi.progav.modelo.Motivo;
import com.gestionamateur.tpi.progav.modelo.Partido;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class PartidoDAO {

    private final CanchaDAO canchaDAO = new CanchaDAO();
    private final ArbitroDAO arbitroDAO = new ArbitroDAO();
    private final MotivoDAO motivoDAO = new MotivoDAO();

    public Long insertar(Partido partido, Long idFase, Long idCancha, Long idArbitro) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return insertar(conn, partido, idFase, idCancha, idArbitro);
        }
    }

    public Long insertar(Connection conn, Partido partido, Long idFase, Long idCancha, Long idArbitro) throws SQLException {
        String sql = "INSERT INTO partidos (id_fase, id_cancha, id_arbitro, fecha_hora, "
                   + "duracion_partido_minutos, estado) "
                   + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id_partido";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (idFase != null) {
                ps.setLong(1, idFase);
            } else if (partido.getIdFase() != null) {
                ps.setLong(1, partido.getIdFase());
            } else {
                ps.setNull(1, Types.BIGINT);
            }

            Long canchaId = idCancha != null ? idCancha : (partido.getSede() != null ? partido.getSede().getIdCancha() : null);
            if (canchaId == null) {
                throw new SQLException("El partido requiere una cancha (sede) obligatoria.");
            }
            ps.setLong(2, canchaId);

            Long arbId = idArbitro != null ? idArbitro : (partido.getArbitroDesignado() != null ? partido.getArbitroDesignado().getId() : null);
            if (arbId != null) {
                ps.setLong(3, arbId);
            } else {
                ps.setNull(3, Types.BIGINT);
            }

            ps.setTimestamp(4, new Timestamp(partido.getFechaHora().getTime()));
            ps.setInt(5, partido.getDuracionPartidoMinutos() > 0 ? partido.getDuracionPartidoMinutos() : 90);
            ps.setString(6, partido.getEstadoPartido() != null ? partido.getEstadoPartido().name() : "PROGRAMADO");

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    partido.setIdPartido(id);
                    partido.setIdFase(idFase);
                    return id;
                }
            }
        }
        throw new SQLException("Error al insertar partido: no se generó ID.");
    }

    public Partido buscarPorId(Long idPartido) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarPorId(conn, idPartido);
        }
    }

    public Partido buscarPorId(Connection conn, Long idPartido) throws SQLException {
        String sql = "SELECT id_partido, id_fase, id_cancha, id_arbitro, id_motivo_suspension, "
                   + "fecha_hora, fecha_hora_finalizacion_real, duracion_partido_minutos, estado "
                   + "FROM partidos WHERE id_partido = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idPartido);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearPartido(conn, rs);
                }
            }
        }
        return null;
    }

    public Partido bloquearParaFinalizacion(Connection conn, Long idPartido) throws SQLException {
        String sql = "SELECT id_partido, id_fase, id_cancha, id_arbitro, id_motivo_suspension, "
                   + "fecha_hora, fecha_hora_finalizacion_real, duracion_partido_minutos, estado "
                   + "FROM partidos WHERE id_partido = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idPartido);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearPartido(conn, rs);
                }
            }
        }
        return null;
    }

    public List<Partido> buscarPorFase(Long idFase) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarPorFase(conn, idFase);
        }
    }

    public List<Partido> buscarPorFase(Connection conn, Long idFase) throws SQLException {
        String sql = "SELECT id_partido, id_fase, id_cancha, id_arbitro, id_motivo_suspension, "
                   + "fecha_hora, fecha_hora_finalizacion_real, duracion_partido_minutos, estado "
                   + "FROM partidos WHERE id_fase = ? ORDER BY fecha_hora";
        List<Partido> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idFase);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearPartido(conn, rs));
                }
            }
        }
        return lista;
    }

    public List<Partido> buscarPorTorneo(Connection conn, Long idTorneo) throws SQLException {
        String sql = "SELECT p.id_partido, p.id_fase, p.id_cancha, p.id_arbitro, p.id_motivo_suspension, "
                   + "p.fecha_hora, p.fecha_hora_finalizacion_real, p.duracion_partido_minutos, p.estado "
                   + "FROM partidos p "
                   + "JOIN fases f ON p.id_fase = f.id_fase "
                   + "WHERE f.id_torneo = ? ORDER BY f.orden, p.fecha_hora";
        List<Partido> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idTorneo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearPartido(conn, rs));
                }
            }
        }
        return lista;
    }

    public List<Partido> buscarTodos() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarTodos(conn);
        }
    }

    public List<Partido> buscarTodos(Connection conn) throws SQLException {
        String sql = "SELECT id_partido, id_fase, id_cancha, id_arbitro, id_motivo_suspension, "
                   + "fecha_hora, fecha_hora_finalizacion_real, duracion_partido_minutos, estado "
                   + "FROM partidos ORDER BY fecha_hora";
        List<Partido> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearPartido(conn, rs));
            }
        }
        return lista;
    }

    public void actualizarEstado(Connection conn, Long idPartido, String nuevoEstado) throws SQLException {
        String sql = "UPDATE partidos SET estado = ? WHERE id_partido = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setLong(2, idPartido);
            ps.executeUpdate();
        }
    }

    public void actualizarFinalizacionReal(Connection conn, Long idPartido, java.util.Date finalizacionReal) throws SQLException {
        String sql = "UPDATE partidos SET estado = 'FINALIZADO', fecha_hora_finalizacion_real = ? WHERE id_partido = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, finalizacionReal != null ? new Timestamp(finalizacionReal.getTime()) : new Timestamp(System.currentTimeMillis()));
            ps.setLong(2, idPartido);
            ps.executeUpdate();
        }
    }

    public void suspenderOPostergar(Connection conn, Long idPartido, String nuevoEstado, Long idMotivo) throws SQLException {
        String sql = "UPDATE partidos SET estado = ?, id_motivo_suspension = ? WHERE id_partido = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setLong(2, idMotivo);
            ps.setLong(3, idPartido);
            int filas = ps.executeUpdate();
            if (filas != 1) {
                throw new SQLException("No se pudo actualizar el estado del partido ID " + idPartido + " (filas afectadas: " + filas + ").");
            }
        }
    }

    public void asignarArbitro(Connection conn, Long idPartido, Long idArbitro) throws SQLException {
        String sql = "UPDATE partidos SET id_arbitro = ? WHERE id_partido = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (idArbitro != null) {
                ps.setLong(1, idArbitro);
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setLong(2, idPartido);
            ps.executeUpdate();
        }
    }

    public Long obtenerIdTorneoPorPartido(Connection conn, Long idPartido) throws SQLException {
        String sql = "SELECT f.id_torneo FROM partidos p "
                   + "JOIN fases f ON p.id_fase = f.id_fase "
                   + "WHERE p.id_partido = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idPartido);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return null;
    }

    private Partido mapearPartido(Connection conn, ResultSet rs) throws SQLException {
        try {
            Long id = rs.getLong("id_partido");
            Long idFase = rs.getLong("id_fase");
            if (rs.wasNull()) idFase = null;

            Long idCancha = rs.getLong("id_cancha");
            Long idArbitro = rs.getLong("id_arbitro");
            if (rs.wasNull()) idArbitro = null;

            Long idMotivo = rs.getLong("id_motivo_suspension");
            if (rs.wasNull()) idMotivo = null;

            Timestamp fechaHoraTs = rs.getTimestamp("fecha_hora");
            Timestamp finalizacionRealTs = rs.getTimestamp("fecha_hora_finalizacion_real");
            int duracion = rs.getInt("duracion_partido_minutos");
            String estadoStr = rs.getString("estado");

            Cancha cancha = canchaDAO.buscarPorId(conn, idCancha);
            Arbitro arbitro = idArbitro != null ? arbitroDAO.buscarPorId(conn, idArbitro) : null;
            Motivo motivo = idMotivo != null ? motivoDAO.buscarPorId(conn, idMotivo) : null;

            Partido p = new Partido();
            p.setIdPartido(id);
            p.setIdFase(idFase);
            p.setFechaHora(new java.util.Date(fechaHoraTs.getTime()));
            if (finalizacionRealTs != null) {
                p.setFechaHoraFinalizacionReal(new java.util.Date(finalizacionRealTs.getTime()));
            }
            p.setDuracionPartidoMinutos(duracion);
            p.setSede(cancha);
            if (arbitro != null) {
                p.setArbitroDesignado(arbitro);
            }
            if (motivo != null) {
                p.setMotivoSuspension(motivo);
            }

            TiposDatos.EstadoPartido estado = TiposDatos.EstadoPartido.PROGRAMADO;
            try {
                if (estadoStr != null) {
                    estado = TiposDatos.EstadoPartido.valueOf(estadoStr);
                }
            } catch (IllegalArgumentException ignored) {}
            p.setEstadoPartido(estado);

            return p;
        } catch (TorneoExcepcion e) {
            throw new SQLException("Error de validación al mapear Partido: " + e.getMessage(), e);
        }
    }
}
