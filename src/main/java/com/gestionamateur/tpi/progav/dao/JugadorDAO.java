package com.gestionamateur.tpi.progav.dao;

import com.gestionamateur.tpi.progav.modelo.Jugador;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class JugadorDAO {

    public Long insertar(Jugador jugador, Long idEquipo) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return insertar(conn, jugador, idEquipo);
        }
    }

    public Long insertar(Connection conn, Jugador jugador, Long idEquipo) throws SQLException {
        String sql = "INSERT INTO jugadores (id_equipo, nombre, dni, fecha_nacimiento, posicion, estado, historial_tarjetas) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id_jugador";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (idEquipo != null) {
                ps.setLong(1, idEquipo);
            } else if (jugador.getIdEquipo() != null) {
                ps.setLong(1, jugador.getIdEquipo());
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setString(2, jugador.getNombre());
            ps.setInt(3, jugador.getDni());
            ps.setDate(4, new Date(jugador.getFechaNacimiento().getTime()));
            ps.setString(5, jugador.getPosicion());
            ps.setString(6, jugador.getEstado() != null ? jugador.getEstado().name() : "ACTIVO");
            ps.setInt(7, jugador.getHistorialTarjetas());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    jugador.setId(id);
                    if (idEquipo != null) {
                        jugador.setIdEquipo(idEquipo);
                    }
                    return id;
                }
            }
        }
        throw new SQLException("Error al insertar jugador: no se generó ID.");
    }

    public Jugador buscarPorId(Long idJugador) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarPorId(conn, idJugador);
        }
    }

    public Jugador buscarPorId(Connection conn, Long idJugador) throws SQLException {
        String sql = "SELECT id_jugador, id_equipo, nombre, dni, fecha_nacimiento, posicion, estado, historial_tarjetas "
                   + "FROM jugadores WHERE id_jugador = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idJugador);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearJugador(rs);
                }
            }
        }
        return null;
    }

    public Jugador buscarPorDni(int dni) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarPorDni(conn, dni);
        }
    }

    public Jugador buscarPorDni(Connection conn, int dni) throws SQLException {
        String sql = "SELECT id_jugador, id_equipo, nombre, dni, fecha_nacimiento, posicion, estado, historial_tarjetas "
                   + "FROM jugadores WHERE dni = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dni);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearJugador(rs);
                }
            }
        }
        return null;
    }

    public List<Jugador> buscarPorEquipo(Long idEquipo) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarPorEquipo(conn, idEquipo);
        }
    }

    public List<Jugador> buscarPorEquipo(Connection conn, Long idEquipo) throws SQLException {
        String sql = "SELECT id_jugador, id_equipo, nombre, dni, fecha_nacimiento, posicion, estado, historial_tarjetas "
                   + "FROM jugadores WHERE id_equipo = ? ORDER BY nombre";
        List<Jugador> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idEquipo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearJugador(rs));
                }
            }
        }
        return lista;
    }

    public List<Jugador> buscarTodos() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarTodos(conn);
        }
    }

    public List<Jugador> buscarTodos(Connection conn) throws SQLException {
        String sql = "SELECT id_jugador, id_equipo, nombre, dni, fecha_nacimiento, posicion, estado, historial_tarjetas "
                   + "FROM jugadores ORDER BY nombre";
        List<Jugador> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearJugador(rs));
            }
        }
        return lista;
    }

    public void actualizarEstado(Long idJugador, String estado) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            actualizarEstado(conn, idJugador, estado);
        }
    }

    public void actualizarEstado(Connection conn, Long idJugador, String estado) throws SQLException {
        String sql = "UPDATE jugadores SET estado = ? WHERE id_jugador = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setLong(2, idJugador);
            ps.executeUpdate();
        }
    }

    public void actualizarHistorialTarjetas(Connection conn, Long idJugador, int nuevoHistorial) throws SQLException {
        String sql = "UPDATE jugadores SET historial_tarjetas = ? WHERE id_jugador = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nuevoHistorial);
            ps.setLong(2, idJugador);
            ps.executeUpdate();
        }
    }

    public void asignarEquipo(Connection conn, Long idJugador, Long idEquipo) throws SQLException {
        String sql = "UPDATE jugadores SET id_equipo = ? WHERE id_jugador = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (idEquipo != null) {
                ps.setLong(1, idEquipo);
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setLong(2, idJugador);
            ps.executeUpdate();
        }
    }

    private Jugador mapearJugador(ResultSet rs) throws SQLException {
        try {
            Long id = rs.getLong("id_jugador");
            Long idEquipo = rs.getLong("id_equipo");
            if (rs.wasNull()) {
                idEquipo = null;
            }
            String nombre = rs.getString("nombre");
            int dni = rs.getInt("dni");
            java.util.Date fechaNac = rs.getDate("fecha_nacimiento");
            String posicion = rs.getString("posicion");
            String estadoStr = rs.getString("estado");
            int tarjetas = rs.getInt("historial_tarjetas");

            TiposDatos.EstadoJugador estado = TiposDatos.EstadoJugador.ACTIVO;
            try {
                if (estadoStr != null) {
                    estado = TiposDatos.EstadoJugador.valueOf(estadoStr);
                }
            } catch (IllegalArgumentException ignored) {}

            Jugador j = new Jugador(nombre, dni, fechaNac, posicion, estado);
            j.setId(id);
            j.setIdEquipo(idEquipo);
            j.setHistorialTarjetas(tarjetas);
            return j;
        } catch (TorneoExcepcion e) {
            throw new SQLException("Error de validación al mapear Jugador: " + e.getMessage(), e);
        }
    }
}
