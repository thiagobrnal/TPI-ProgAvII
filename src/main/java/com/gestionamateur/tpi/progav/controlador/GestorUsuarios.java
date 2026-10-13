package com.gestionamateur.tpi.progav.controlador;

import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import java.util.ArrayList;
import java.util.List;

public class GestorUsuarios {
    
    // Base de datos simulada en memoria
    private static List<Usuario> dbUsuarios = new ArrayList<>();

    public GestorUsuarios() {
        // Al instanciarse, si la lista está vacía, creamos un Administrador por defecto
        if (dbUsuarios.isEmpty()) {
            try {
                // Ahora esto es seguro. Si por algún motivo escribimos mal el mail del admin en el código, el sistema nos avisará.
                dbUsuarios.add(new Usuario("admin", "1234", "admin@torneo.com", "Admin", "General", TiposDatos.Rol.ADMINISTRADOR));
            } catch (TorneoExcepcion ex) {
                System.err.println("Error crítico al inicializar el Administrador por defecto: " + ex.getMessage());
            }
        }
    }

    // CU 0: Iniciar Sesión (Transversal)
    public Usuario autenticarUsuario(String usuario, String contraseña) {
        for (Usuario u : dbUsuarios) {
            // Delega la validación real al objeto Usuario (Dominio Rico)
            if (u.verificarCredenciales(usuario, contraseña)) {
                return u; // Retorna el objeto Usuario completo para que la Vista sepa qué permisos darle
            }
        }
        return null; // Credenciales inválidas
    }

    // CU 17: Gestionar Usuarios (Alta)
    public String registrarUsuario(String usuario, String contraseña, String mail, String nombre, String apellido, TiposDatos.Rol rol) {
        // Regla del Controlador: No pueden haber dos usuarios con el mismo nombre de cuenta
        for (Usuario u : dbUsuarios) {
            if (u.getUsuario().equalsIgnoreCase(usuario)) {
                return "ERROR: El nombre de usuario '" + usuario + "' ya se encuentra registrado.";
            }
        }
        
        try {
            // Delegamos las validaciones de formato e integridad a la clase Usuario
            Usuario nuevoUsuario = new Usuario(usuario, contraseña, mail, nombre, apellido, rol);
            dbUsuarios.add(nuevoUsuario);
            
            // Usamos rol.toString() para que muestre el texto amigable que configuramos en el Enum
            return "ÉXITO: La cuenta de " + nombre + " (" + rol.toString() + ") ha sido creada.";
            
        } catch (TorneoExcepcion ex) {
            return "ERROR DE VALIDACIÓN: " + ex.getMessage();
        } catch (Exception e) {
            return "ERROR CRÍTICO: Falló la creación de la cuenta (" + e.getMessage() + ").";
        }
    }
}