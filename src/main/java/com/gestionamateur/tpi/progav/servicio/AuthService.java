package com.gestionamateur.tpi.progav.servicio;

import com.gestionamateur.tpi.progav.dao.RolDAO;
import com.gestionamateur.tpi.progav.dao.UsuarioDAO;
import com.gestionamateur.tpi.progav.modelo.SistemaLogs;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.seguridad.BCrypt;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.logging.Logger;

public class AuthService {

    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());
    private final UsuarioDAO usuarioDAO;
    private final RolDAO rolDAO;
    private Usuario usuarioAutenticado;

    public AuthService() {
        this.usuarioDAO = new UsuarioDAO();
        this.rolDAO = new RolDAO();
    }

    public AuthService(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
        this.rolDAO = new RolDAO();
    }

    public AuthService(UsuarioDAO usuarioDAO, RolDAO rolDAO) {
        this.usuarioDAO = usuarioDAO;
        this.rolDAO = rolDAO;
    }

    public Usuario autenticar(String nombreUsuario, String contrasenia) throws TorneoExcepcion {
        if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
            throw new TorneoExcepcion("El nombre de usuario es obligatorio.");
        }
        if (contrasenia == null || contrasenia.trim().isEmpty()) {
            throw new TorneoExcepcion("La contraseña es obligatoria.");
        }
        if (contrasenia.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            LOGGER.warning("Intento de autenticación con contraseña que excede el límite de 72 bytes de BCrypt.");
            throw new TorneoExcepcion("Credenciales inválidas: usuario o contraseña incorrectos.");
        }

        try {
            Usuario u = usuarioDAO.buscarPorNombreUsuario(nombreUsuario.trim());
            if (u == null) {
                LOGGER.warning("Intento de inicio de sesión con usuario inexistente.");
                throw new TorneoExcepcion("Credenciales inválidas: usuario o contraseña incorrectos.");
            }

            if (!u.isActivo()) {
                LOGGER.warning("Intento de inicio de sesión de usuario inactivo.");
                throw new TorneoExcepcion("La cuenta de usuario se encuentra inactiva. Contacte al administrador.");
            }

            String hashAlmacenado = u.getClaveHash();
            boolean claveValida = false;
            boolean requiereMigracion = false;

            if (hashAlmacenado != null && hashAlmacenado.startsWith("$2")) {
                claveValida = BCrypt.checkpw(contrasenia, hashAlmacenado);
            } else if (hashAlmacenado != null) {
                // Verificación y migración transparente de hash SHA-256 legado
                String hashSha256 = hashearSha256(contrasenia);
                if (hashSha256.equalsIgnoreCase(hashAlmacenado)) {
                    claveValida = true;
                    requiereMigracion = true;
                }
            }

            if (!claveValida) {
                LOGGER.warning("Credenciales incorrectas para el usuario consultado.");
                throw new TorneoExcepcion("Credenciales inválidas: usuario o contraseña incorrectos.");
            }

            if (requiereMigracion) {
                try {
                    String nuevoHashBCrypt = hashearBCrypt(contrasenia);
                    usuarioDAO.actualizarClaveHash(u.getId(), nuevoHashBCrypt);
                    u.setContraseña(nuevoHashBCrypt);
                    SistemaLogs.info(AuthService.class, "Hash de contraseña migrado exitosamente a BCrypt para el usuario ID " + u.getId());
                } catch (SQLException e) {
                    LOGGER.warning("No se pudo persistir la migración del hash BCrypt: " + e.getMessage());
                }
            }

            this.usuarioAutenticado = u;
            SistemaLogs.info(AuthService.class, "Inicio de sesión exitoso: " + u.getNombreUsuario() + " (Rol: " + u.getRol().name() + ")");
            return u;
        } catch (SQLException e) {
            LOGGER.severe("Error de base de datos durante la autenticación: " + e.getMessage());
            throw new TorneoExcepcion("Error del sistema de persistencia al verificar credenciales.", e);
        }
    }

    public Usuario inicializarPrimerAdministrador(String usuario, String contrasena, String mail, String nombre, String apellido) throws TorneoExcepcion {
        if (usuario == null || usuario.trim().length() < 3) {
            throw new TorneoExcepcion("El nombre de usuario debe tener al menos 3 caracteres.");
        }
        if (contrasena == null || contrasena.trim().length() < 6) {
            throw new TorneoExcepcion("La contraseña del administrador debe tener al menos 6 caracteres.");
        }
        if (mail == null || !mail.contains("@")) {
            throw new TorneoExcepcion("Debe ingresar un correo electrónico válido.");
        }

        try {
            RolDAO.RolRegistro regRol = rolDAO.buscarPorCodigo("ADMINISTRADOR");
            int idRol = regRol != null ? regRol.idRol : 1;

            String hashBCrypt = hashearBCrypt(contrasena);
            Usuario nuevoAdmin = new Usuario(usuario.trim(), hashBCrypt, mail.trim(), nombre.trim(), apellido.trim(), TiposDatos.Rol.ADMINISTRADOR);
            Long id = usuarioDAO.insertarPrimerAdministradorAtomico(nuevoAdmin, hashBCrypt, idRol);
            nuevoAdmin.setId(id);

            SistemaLogs.info(AuthService.class, "Primer Administrador del sistema inicializado con éxito (ID: " + id + ", Usuario: " + usuario + ")");
            return nuevoAdmin;
        } catch (SQLException e) {
            LOGGER.severe("Error al inicializar el primer administrador: " + e.getMessage());
            throw new TorneoExcepcion("Error de persistencia al inicializar cuenta administradora.", e);
        }
    }

    public void cerrarSesion() {
        if (this.usuarioAutenticado != null) {
            SistemaLogs.info(AuthService.class, "Cierre de sesión: " + usuarioAutenticado.getNombreUsuario());
            this.usuarioAutenticado = null;
        }
    }

    public Usuario getUsuarioAutenticado() {
        return usuarioAutenticado;
    }

    public boolean estaAutenticado() {
        return usuarioAutenticado != null;
    }

    public boolean tieneRol(String codigoRol) {
        if (usuarioAutenticado == null || usuarioAutenticado.getRol() == null) {
            return false;
        }
        return usuarioAutenticado.getRol().name().equalsIgnoreCase(codigoRol);
    }

    public static final int MAX_PASSWORD_BYTES = 72;
    public static final int MIN_PASSWORD_CHARS = 4;

    public static void validarPoliticaContrasena(String contrasena) throws TorneoExcepcion {
        if (contrasena == null || contrasena.trim().length() < MIN_PASSWORD_CHARS) {
            throw new TorneoExcepcion("La contraseña debe contener al menos " + MIN_PASSWORD_CHARS + " caracteres.");
        }
        byte[] bytesUtf8 = contrasena.getBytes(StandardCharsets.UTF_8);
        if (bytesUtf8.length > MAX_PASSWORD_BYTES) {
            throw new TorneoExcepcion("La contraseña supera el límite máximo permitido de 72 bytes en codificación UTF-8 "
                    + "(límite del algoritmo BCrypt). La clave ingresada posee " + bytesUtf8.length + " bytes.");
        }
    }

    public static String hashearBCrypt(String textoPlano) throws TorneoExcepcion {
        validarPoliticaContrasena(textoPlano);
        return BCrypt.hashpw(textoPlano, BCrypt.gensalt(12));
    }

    public static boolean verificarBCrypt(String textoPlano, String hash) {
        if (textoPlano == null || hash == null) {
            return false;
        }
        if (textoPlano.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            return false;
        }
        return BCrypt.checkpw(textoPlano, hash);
    }

    public static String hashearSha256(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo SHA-256 no disponible.", e);
        }
    }
}
