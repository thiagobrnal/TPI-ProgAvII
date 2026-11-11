package com.gestionamateur.tpi.progav.dao;

import com.gestionamateur.tpi.progav.modelo.Motivo;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MotivoDAO {

    public List<Motivo> buscarTodos() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarTodos(conn);
        }
    }

    public List<Motivo> buscarTodos(Connection conn) throws SQLException {
        String sql = "SELECT id_motivo, motivo FROM motivos ORDER BY id_motivo";
        List<Motivo> lista = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearMotivo(rs));
            }
        }
        return lista;
    }

    public Motivo buscarPorId(Long idMotivo) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return buscarPorId(conn, idMotivo);
        }
    }

    public Motivo buscarPorId(Connection conn, Long idMotivo) throws SQLException {
        String sql = "SELECT id_motivo, motivo FROM motivos WHERE id_motivo = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idMotivo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearMotivo(rs);
                }
            }
        }
        return null;
    }

    public Long insertar(Connection conn, Motivo motivo) throws SQLException {
        String sql = "INSERT INTO motivos (motivo) VALUES (?) RETURNING id_motivo";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, motivo.getMotivo());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    motivo.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Error al insertar motivo: no se obtuvo ID autogenerado.");
    }

    /**
     * Inserta un motivo de forma idempotente y segura ante concurrencia sin violar
     * la restricción UNIQUE (motivo). Si ya existe, recupera su ID dentro de la misma transacción.
     */
    public Long obtenerOInsertar(Connection conn, Motivo motivo) throws SQLException {
        if (motivo == null || motivo.getMotivo() == null || motivo.getMotivo().trim().length() < 5) {
            throw new SQLException("El motivo debe tener al menos 5 caracteres válidos.");
        }
        String texto = motivo.getMotivo().trim();
        String sqlInsert = "INSERT INTO motivos (motivo) VALUES (?) ON CONFLICT (motivo) DO NOTHING RETURNING id_motivo";
        try (PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
            ps.setString(1, texto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    motivo.setId(id);
                    return id;
                }
            }
        }

        String sqlSelect = "SELECT id_motivo FROM motivos WHERE motivo = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlSelect)) {
            ps.setString(1, texto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    motivo.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Error crítico: no se pudo obtener ni registrar el motivo de suspensión/postergación.");
    }

    private Motivo mapearMotivo(ResultSet rs) throws SQLException {
        try {
            Long id = rs.getLong("id_motivo");
            String texto = rs.getString("motivo");
            Motivo m = new Motivo(texto);
            m.setId(id);
            return m;
        } catch (TorneoExcepcion te) {
            throw new SQLException("Error al mapear motivo: " + te.getMessage(), te);
        }
    }
}
