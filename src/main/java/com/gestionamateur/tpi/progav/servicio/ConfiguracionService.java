package com.gestionamateur.tpi.progav.servicio;

import com.gestionamateur.tpi.progav.dao.ConexionBD;
import com.gestionamateur.tpi.progav.dao.ConfiguracionDAO;
import com.gestionamateur.tpi.progav.modelo.SistemaLogs;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.seguridad.PoliticaPermisos;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Servicio de acceso y modificación de parámetros globales (CU-18).
 * Protege lectura y escritura administrativa evitando accesos directos al DAO desde la interfaz.
 */
public class ConfiguracionService {

    private final ConfiguracionDAO configuracionDAO;

    public ConfiguracionService() {
        this(new ConfiguracionDAO());
    }

    public ConfiguracionService(ConfiguracionDAO configuracionDAO) {
        this.configuracionDAO = configuracionDAO;
    }

    private void validarAdministrador(Usuario operador, String accion) throws TorneoExcepcion {
        if (!PoliticaPermisos.esOperadorPersistidoValido(operador)) {
            throw new TorneoExcepcion("Acceso denegado: se requiere un Administrador autenticado para " + accion + ".");
        }
        if (!PoliticaPermisos.tienePermisoOperacion(operador, PoliticaPermisos.ModuloMenu.CONFIGURACION_GENERAL)) {
            throw new TorneoExcepcion("Acceso denegado: solo un Administrador autenticado puede " + accion + ".");
        }
    }

    public Map<String, Integer> obtenerValores(Usuario operador) throws TorneoExcepcion {
        validarAdministrador(operador, "consultar la configuración general");
        try {
            return configuracionDAO.obtenerMapa();
        } catch (SQLException e) {
            throw new TorneoExcepcion("No se pudieron consultar los parámetros de configuración: " + e.getMessage(), e);
        }
    }

    public Map<String, String> obtenerDescripciones(Usuario operador) throws TorneoExcepcion {
        validarAdministrador(operador, "consultar las descripciones de configuración");
        try {
            return configuracionDAO.obtenerDescripciones();
        } catch (SQLException e) {
            throw new TorneoExcepcion("No se pudieron consultar las descripciones de configuración: " + e.getMessage(), e);
        }
    }

    public void actualizarParametro(String clave, int nuevoValor, Usuario operador) throws TorneoExcepcion {
        validarAdministrador(operador, "modificar la configuración general");
        if (clave == null || clave.trim().isEmpty()) {
            throw new TorneoExcepcion("La clave del parámetro es obligatoria.");
        }
        String claveNorm = clave.trim();
        if (nuevoValor < 0) {
            throw new TorneoExcepcion("El valor del parámetro no puede ser negativo.");
        }
        try (Connection conn = ConexionBD.obtenerConexion()) {
            configuracionDAO.actualizarValorInt(conn, claveNorm, nuevoValor);
            SistemaLogs.info(ConfiguracionService.class,
                    "Parámetro '" + claveNorm + "' actualizado a " + nuevoValor + " por " + operador.getNombreUsuario());
        } catch (SQLException e) {
            throw new TorneoExcepcion("No se pudo actualizar el parámetro '" + claveNorm + "': " + e.getMessage(), e);
        }
    }
}
