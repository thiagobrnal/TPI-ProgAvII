package com.gestionamateur.tpi.progav.controlador;

import com.gestionamateur.tpi.progav.dao.ArbitroDAO;
import com.gestionamateur.tpi.progav.dao.RolDAO;
import com.gestionamateur.tpi.progav.dao.UsuarioDAO;
import com.gestionamateur.tpi.progav.modelo.Arbitro;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.seguridad.PoliticaPermisos;
import com.gestionamateur.tpi.progav.servicio.AuthService;
import com.gestionamateur.tpi.progav.servicio.UsuarioService;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GestorUsuarios {

    private static final Logger LOGGER = Logger.getLogger(GestorUsuarios.class.getName());
    private static final List<Usuario> dbUsuarios = new ArrayList<>();

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final ArbitroDAO arbitroDAO = new ArbitroDAO();
    private final RolDAO rolDAO = new RolDAO();
    private final AuthService authService = new AuthService(usuarioDAO, rolDAO);
    private final UsuarioService usuarioService = new UsuarioService(usuarioDAO, arbitroDAO, rolDAO);

    public GestorUsuarios() {
    }

    // CU 0: Iniciar Sesión (Transversal) - Autenticación con persistencia JDBC
    public Usuario autenticarUsuario(String usuario, String contraseña) {
        try {
            return authService.autenticar(usuario, contraseña);
        } catch (TorneoExcepcion ex) {
            LOGGER.log(Level.WARNING, "Fallo de autenticación: " + ex.getMessage());
            return null;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error de infraestructura durante autenticación: " + ex.getMessage());
            return null;
        }
    }

    public boolean esSistemaInicializado() {
        try {
            return usuarioDAO.contarUsuarios() > 0;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo consultar conteo de usuarios: " + e.getMessage());
            return false;
        }
    }

    public String registrarUsuarioPublico(String usuario, String contraseña, String mail, String nombre, String apellido, TiposDatos.Rol rol) {
        if (rol == TiposDatos.Rol.PUBLICO) {
            return "ERROR: El rol PÚBLICO es de acceso libre y anónimo. No requiere ni admite la creación de una cuenta de usuario.";
        }
        if (rol != TiposDatos.Rol.CAPITAN) {
            return "ERROR DE SEGURIDAD: Registro público denegado. Solo se admite el autorregistro de Capitanes. Los roles privilegiados (" + rol + ") requieren autorización y alta por un Administrador.";
        }
        return registrarUsuarioInterno(usuario, contraseña, mail, nombre, apellido, rol);
    }

    // CU 17: Gestionar Usuarios con control estricto de operador ADMINISTRADOR para todos los roles
    public String registrarUsuario(String usuario, String contraseña, String mail, String nombre, String apellido, TiposDatos.Rol rol, Usuario operador) {
        return registrarUsuario(usuario, contraseña, mail, nombre, apellido, rol, "Oficial", "S/D", operador);
    }

    public String registrarUsuario(String usuario, String contraseña, String mail, String nombre, String apellido,
                                   TiposDatos.Rol rol, String categoriaArbitral, String telefono, Usuario operador) {
        if (!PoliticaPermisos.tienePermisoOperacion(operador, PoliticaPermisos.ModuloMenu.GESTIONAR_USUARIOS)) {
            return "ERROR DE SEGURIDAD: Acceso denegado. Solo un Administrador autenticado puede crear cuentas desde el gestor (" + rol + ").";
        }
        if (rol == TiposDatos.Rol.PUBLICO) {
            return "ERROR: El rol PÚBLICO es de consulta anónima y no admite cuentas de usuario.";
        }
        try {
            Usuario creado = usuarioService.registrarUsuario(usuario, contraseña, mail, nombre, apellido, rol, categoriaArbitral, telefono, operador);
            dbUsuarios.add(creado);
            return "ÉXITO: La cuenta de " + nombre + " (" + rol.name() + ") ha sido creada en la base de datos.";
        } catch (TorneoExcepcion te) {
            if (te.getMessage() != null && te.getMessage().startsWith("Acceso denegado")) {
                return "ERROR DE SEGURIDAD: " + te.getMessage();
            }
            return "ERROR DE VALIDACIÓN: " + te.getMessage();
        } catch (Exception e) {
            return "ERROR DE CONEXIÓN: " + e.getMessage();
        }
    }

    // CU 17: Gestionar Usuarios
    public String registrarUsuario(String usuario, String contraseña, String mail, String nombre, String apellido, TiposDatos.Rol rol) {
        return registrarUsuario(usuario, contraseña, mail, nombre, apellido, rol, null);
    }

    public List<Usuario> listarUsuarios(Usuario operador) throws TorneoExcepcion {
        return usuarioService.listarUsuarios(operador);
    }

    public Arbitro obtenerDetalleArbitro(Long idUsuario, Usuario operador) throws TorneoExcepcion {
        return usuarioService.obtenerDetalleArbitro(idUsuario, operador);
    }

    public String actualizarUsuario(Long idUsuario, String mail, String nombre, String apellido,
                                    TiposDatos.Rol nuevoRol, String categoriaArbitral, String telefono, Usuario operador) {
        try {
            usuarioService.actualizarUsuario(idUsuario, mail, nombre, apellido, nuevoRol, categoriaArbitral, telefono, operador);
            return "ÉXITO: Los datos del usuario ID " + idUsuario + " fueron actualizados correctamente.";
        } catch (TorneoExcepcion te) {
            if (te.getMessage() != null && te.getMessage().startsWith("Acceso denegado")) {
                return "ERROR DE SEGURIDAD: " + te.getMessage();
            }
            return "ERROR DE VALIDACIÓN: " + te.getMessage();
        } catch (Exception e) {
            return "ERROR DE CONEXIÓN: " + e.getMessage();
        }
    }

    public String cambiarEstadoCuenta(Long idUsuario, boolean activo, Usuario operador) {
        try {
            usuarioService.cambiarEstadoCuenta(idUsuario, activo, operador);
            return "ÉXITO: La cuenta ID " + idUsuario + " quedó " + (activo ? "ACTIVA." : "INACTIVA.");
        } catch (TorneoExcepcion te) {
            if (te.getMessage() != null && te.getMessage().startsWith("Acceso denegado")) {
                return "ERROR DE SEGURIDAD: " + te.getMessage();
            }
            return "ERROR DE VALIDACIÓN: " + te.getMessage();
        } catch (Exception e) {
            return "ERROR DE CONEXIÓN: " + e.getMessage();
        }
    }

    public String restablecerContrasena(Long idUsuario, String nuevaContrasena, Usuario operador) {
        try {
            usuarioService.restablecerContrasena(idUsuario, nuevaContrasena, operador);
            return "ÉXITO: Contraseña restablecida de forma segura.";
        } catch (TorneoExcepcion te) {
            if (te.getMessage() != null && te.getMessage().startsWith("Acceso denegado")) {
                return "ERROR DE SEGURIDAD: " + te.getMessage();
            }
            return "ERROR DE VALIDACIÓN: " + te.getMessage();
        } catch (Exception e) {
            return "ERROR DE CONEXIÓN: " + e.getMessage();
        }
    }

    public void cerrarSesion(Usuario usuario) {
        authService.cerrarSesion(usuario);
    }

    private String registrarUsuarioInterno(String usuario, String contraseña, String mail, String nombre, String apellido, TiposDatos.Rol rol) {
        try {
            AuthService.validarPoliticaContrasena(contraseña);
            if (rol == TiposDatos.Rol.PUBLICO) {
                return "ERROR: El rol PÚBLICO es de consulta anónima y no admite cuentas de usuario.";
            }
            if (rol == TiposDatos.Rol.ARBITRO) {
                Arbitro arbitro = new Arbitro(usuario, contraseña, mail, nombre, apellido, "Oficial", "S/D");
                arbitroDAO.insertar(arbitro, AuthService.hashearBCrypt(contraseña));
                dbUsuarios.add(arbitro);
            } else {
                RolDAO.RolRegistro reg = rolDAO.buscarPorCodigo(rol.name());
                int idRol = reg != null ? reg.idRol : 5;
                Usuario u = new Usuario(usuario, contraseña, mail, nombre, apellido, rol);
                usuarioDAO.insertar(u, AuthService.hashearBCrypt(contraseña), idRol);
                dbUsuarios.add(u);
            }
            return "ÉXITO: La cuenta de " + nombre + " (" + rol.name() + ") ha sido creada en la base de datos.";
        } catch (TorneoExcepcion te) {
            return "ERROR DE VALIDACIÓN: " + te.getMessage();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fallo crítico de persistencia al registrar usuario: " + e.getMessage());
            // Sin fallback silencioso: fallo explícito al usuario
            return "ERROR DE CONEXIÓN: La base de datos no está disponible (" + e.getMessage() + "). No se guardaron cambios.";
        }
    }

    public List<Usuario> getCapitanesRegistrados() {
        try {
            RolDAO.RolRegistro reg = rolDAO.buscarPorCodigo("CAPITAN");
            int idRol = reg != null ? reg.idRol : 4;
            return usuarioDAO.buscarPorRol(idRol);
        } catch (Exception e) {
            LOGGER.warning("No se pudieron cargar capitanes desde base de datos: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public String inicializarPrimerAdministrador(String usuario, String contrasena, String mail, String nombre, String apellido) {
        try {
            Usuario admin = authService.inicializarPrimerAdministrador(usuario, contrasena, mail, nombre, apellido);
            dbUsuarios.add(admin);
            return "ÉXITO: Primer Administrador inicializado con éxito (" + admin.getNombreUsuario() + "). Ya puede iniciar sesión.";
        } catch (TorneoExcepcion te) {
            return "ERROR: " + te.getMessage();
        } catch (Exception e) {
            return "ERROR DE CONEXIÓN: " + e.getMessage();
        }
    }

    public static List<Arbitro> getArbitrosRegistrados() {
        try {
            ArbitroDAO dao = new ArbitroDAO();
            List<Arbitro> arbitrosBD = dao.buscarTodos();
            if (arbitrosBD != null && !arbitrosBD.isEmpty()) {
                return arbitrosBD;
            }
        } catch (Exception ignored) {}
        List<Arbitro> arbitrosMemoria = new ArrayList<>();
        for (Usuario u : dbUsuarios) {
            if (u instanceof Arbitro) {
                arbitrosMemoria.add((Arbitro) u);
            }
        }
        return arbitrosMemoria;
    }

    public AuthService getAuthService() {
        return authService;
    }
}
