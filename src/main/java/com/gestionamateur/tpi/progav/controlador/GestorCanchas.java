package com.gestionamateur.tpi.progav.controlador;

import com.gestionamateur.tpi.progav.dao.CanchaDAO;
import com.gestionamateur.tpi.progav.modelo.Cancha;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GestorCanchas {
    
    private static final Logger LOGGER = Logger.getLogger(GestorCanchas.class.getName());
    private static final List<Cancha> dbCanchas = new ArrayList<>();
    private final CanchaDAO canchaDAO = new CanchaDAO();

    public GestorCanchas() {
    }

    public String registrarCancha(String nombre, String direccion, String superficie) throws TorneoExcepcion {
        return registrarCancha(nombre, direccion, superficie, null);
    }

    public String registrarCancha(String nombre, String direccion, String superficie, Usuario operador) throws TorneoExcepcion {
        if (operador != null) {
            TiposDatos.Rol rol = operador.getRol();
            if (rol != TiposDatos.Rol.ADMINISTRADOR && rol != TiposDatos.Rol.ORGANIZADOR) {
                throw new TorneoExcepcion("Acceso denegado: El rol " + rol + " no tiene permisos para registrar sedes/canchas.");
            }
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new TorneoExcepcion("El nombre de la sede es obligatorio.");
        }
        if (direccion == null || direccion.trim().isEmpty()) {
            throw new TorneoExcepcion("Debe especificar la dirección de la cancha.");
        }

        try {
            Cancha nuevaCancha = new Cancha(nombre.trim(), direccion.trim(), superficie);
            Long id = canchaDAO.insertar(nuevaCancha);
            nuevaCancha.setIdCancha(id);
            dbCanchas.add(nuevaCancha);
            LOGGER.info("Sede registrada en base de datos: " + nombre + " (ID: " + id + ")");
            return "ÉXITO: Cancha '" + nombre + "' agregada al catálogo del torneo en base de datos (ID: " + id + ").";
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Fallo de persistencia JDBC al registrar cancha: " + e.getMessage());
            // Sin fallback engañoso a memoria
            throw new TorneoExcepcion("ERROR DE CONEXIÓN: La base de datos no está disponible (" + e.getMessage() + "). No se guardaron cambios.");
        }
    }

    public static List<Cancha> getCanchasRegistradas() {
        try {
            CanchaDAO dao = new CanchaDAO();
            List<Cancha> listaBD = dao.buscarTodas();
            if (listaBD != null && !listaBD.isEmpty()) {
                return listaBD;
            }
        } catch (Exception e) {
            LOGGER.warning("No se pudieron cargar canchas desde la base de datos: " + e.getMessage());
        }
        return dbCanchas;
    }
}
