package com.gestionamateur.tpi.progav.servicio;

import com.gestionamateur.tpi.progav.dao.RolDAO;
import com.gestionamateur.tpi.progav.dao.UsuarioDAO;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.seguridad.BCrypt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Seguridad BCrypt y Autenticación (H-01 / H-02)")
public class BCryptSecurityTest {

    private AuthService authService;
    private MockUsuarioDAO mockUsuarioDAO;
    private MockRolDAO mockRolDAO;

    // Mock liviano en memoria para aislar la lógica de autenticación
    static class MockUsuarioDAO extends UsuarioDAO {
        Map<String, Usuario> usuarios = new HashMap<>();

        @Override
        public Usuario buscarPorNombreUsuario(String nombreUsuario) {
            return usuarios.get(nombreUsuario.toLowerCase());
        }

        @Override
        public void actualizarClaveHash(Long idUsuario, String nuevoHash) {
            for (Usuario u : usuarios.values()) {
                if (u.getId().equals(idUsuario)) {
                    try {
                        u.setContraseña(nuevoHash);
                    } catch (TorneoExcepcion ignored) {}
                }
            }
        }

        @Override
        public int contarUsuarios() {
            return usuarios.size();
        }

        @Override
        public Long insertar(Usuario usuario, String contrasenaHash, int idRol) {
            long newId = (long) (usuarios.size() + 1);
            usuario.setId(newId);
            try {
                usuario.setContraseña(contrasenaHash);
            } catch (TorneoExcepcion ignored) {}
            usuarios.put(usuario.getUsuario().toLowerCase(), usuario);
            return newId;
        }

        @Override
        public Long insertarPrimerAdministradorAtomico(Usuario usuario, String contrasenaHash, int idRol) throws TorneoExcepcion {
            if (!usuarios.isEmpty()) {
                throw new TorneoExcepcion("Inicialización rechazada: el sistema ya posee usuarios registrados.");
            }
            return insertar(usuario, contrasenaHash, idRol);
        }
    }

    static class MockRolDAO extends RolDAO {
        @Override
        public RolRegistro buscarPorCodigo(String codigo) {
            return new RolRegistro(1, codigo, "Rol Mock", "Descripcion Mock");
        }
    }

    @BeforeEach
    void setUp() {
        mockUsuarioDAO = new MockUsuarioDAO();
        mockRolDAO = new MockRolDAO();
        authService = new AuthService(mockUsuarioDAO, mockRolDAO);
    }

    @Test
    @DisplayName("BCrypt: Generación de hash salado individual con formato $2a$ y verificación exitosa")
    void testBCryptHashingYVerificacion() {
        String clave = "MiPasswordSeguro123!";
        String hash1 = BCrypt.hashpw(clave, BCrypt.gensalt(12));
        String hash2 = BCrypt.hashpw(clave, BCrypt.gensalt(12));

        assertTrue(hash1.startsWith("$2a$12$"), "El hash debe tener formato BCrypt con 12 rounds");
        assertTrue(hash2.startsWith("$2a$12$"));
        assertNotEquals(hash1, hash2, "Dos hashes de la misma contraseña deben diferir debido a la sal individual");

        assertTrue(BCrypt.checkpw(clave, hash1));
        assertTrue(BCrypt.checkpw(clave, hash2));
        assertFalse(BCrypt.checkpw("ClaveIncorrecta", hash1));
    }

    @Test
    @DisplayName("AuthService: Autenticación exitosa con hash nativo BCrypt")
    void testAutenticacionConBCrypt() throws TorneoExcepcion {
        String clave = "AdminPass123";
        String hashBCrypt = BCrypt.hashpw(clave, BCrypt.gensalt(12));
        Usuario u = new Usuario("admin", hashBCrypt, "admin@torneo.com", "Admin", "General", TiposDatos.Rol.ADMINISTRADOR);
        u.setId(1L);
        mockUsuarioDAO.usuarios.put("admin", u);

        Usuario logueado = authService.autenticar("admin", clave);
        assertNotNull(logueado);
        assertEquals("admin", logueado.getUsuario());
        assertTrue(authService.estaAutenticado());
        assertTrue(authService.tieneRol("ADMINISTRADOR"));
    }

    @Test
    @DisplayName("AuthService: Migración transparente de hash SHA-256 legado a BCrypt en login exitoso")
    void testMigracionTransparenteSha256ABCrypt() throws TorneoExcepcion {
        String claveLegada = "ClaveVieja2026";
        String hashSha256 = AuthService.hashearSha256(claveLegada);

        Usuario u = new Usuario("usuario_legado", hashSha256, "legado@torneo.com", "Carlos", "Pérez", TiposDatos.Rol.ORGANIZADOR);
        u.setId(2L);
        mockUsuarioDAO.usuarios.put("usuario_legado", u);

        // Al autenticar con la clave correcta, debe tener éxito y actualizar su hash a BCrypt
        Usuario logueado = authService.autenticar("usuario_legado", claveLegada);
        assertNotNull(logueado);
        assertTrue(u.getClaveHash().startsWith("$2a$"), "El hash almacenado debe haber migrado a BCrypt");
        assertTrue(BCrypt.checkpw(claveLegada, u.getClaveHash()));
    }

    @Test
    @DisplayName("AuthService: Rechaza credenciales incorrectas y cuentas inactivas")
    void testRechazoCredencialesInvalidas() throws TorneoExcepcion {
        String hash = BCrypt.hashpw("Valida123", BCrypt.gensalt(12));
        Usuario u = new Usuario("juan", hash, "juan@torneo.com", "Juan", "Gómez", TiposDatos.Rol.CAPITAN);
        u.setId(3L);
        mockUsuarioDAO.usuarios.put("juan", u);

        // Contraseña incorrecta
        TorneoExcepcion ex1 = assertThrows(TorneoExcepcion.class, () -> authService.autenticar("juan", "Invalida999"));
        assertTrue(ex1.getMessage().contains("Credenciales inválidas"));

        // Usuario inexistente
        TorneoExcepcion ex2 = assertThrows(TorneoExcepcion.class, () -> authService.autenticar("fantasma", "Cualquiera123"));
        assertTrue(ex2.getMessage().contains("Credenciales inválidas"));

        // Cuenta inactiva
        u.setActivo(false);
        TorneoExcepcion ex3 = assertThrows(TorneoExcepcion.class, () -> authService.autenticar("juan", "Valida123"));
        assertTrue(ex3.getMessage().contains("inactiva"));
    }

    @Test
    @DisplayName("AuthService: Bootstrap seguro del primer administrador cuando la BD está vacía")
    void testBootstrapPrimerAdministrador() throws TorneoExcepcion {
        assertEquals(0, mockUsuarioDAO.contarUsuarios());

        Usuario admin = authService.inicializarPrimerAdministrador("superadmin", "ClaveMaestra2026", "admin@torneos.org", "Super", "Admin");
        assertNotNull(admin);
        assertEquals("superadmin", admin.getUsuario());
        assertEquals(TiposDatos.Rol.ADMINISTRADOR, admin.getRol());
        assertTrue(admin.getClaveHash().startsWith("$2a$"));
        assertEquals(1, mockUsuarioDAO.contarUsuarios());

        // Segundo intento de inicialización debe ser rechazado
        TorneoExcepcion ex = assertThrows(TorneoExcepcion.class, () -> {
            authService.inicializarPrimerAdministrador("otro_admin", "Password456", "otro@torneos.org", "Otro", "Admin");
        });
        assertTrue(ex.getMessage().contains("ya posee usuarios registrados"));
    }

    @Test
    @DisplayName("BCrypt: Validación contra 10 vectores oficiales canónicos (Openwall / Bouncy Castle)")
    void testVectoresOficialesBCrypt() {
        String[][] canonicalVectors = {
            {"U*U", "$2a$05$CCCCCCCCCCCCCCCCCCCCC.", 
             "$2a$05$CCCCCCCCCCCCCCCCCCCCC.E5YPO9kmyuRGyh0XouQYb4YMJKvyOeW"},
            {"U*U*", "$2a$05$CCCCCCCCCCCCCCCCCCCCC.", 
             "$2a$05$CCCCCCCCCCCCCCCCCCCCC.VGOzA784oUp/Z0DY336zx7pLYAy0lwK"},
            {"U*U*U", "$2a$05$XXXXXXXXXXXXXXXXXXXXXO", 
             "$2a$05$XXXXXXXXXXXXXXXXXXXXXOAcXxm9kjPGEMsLznoKqmqw7tc8WCx4a"},
            {"", "$2a$05$CCCCCCCCCCCCCCCCCCCCC.", 
             "$2a$05$CCCCCCCCCCCCCCCCCCCCC.7uG0VCzI2bS7j6ymqJi9CdcdxiRTWNy"},
            {"0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789chars after 72 are ignored",
             "$2a$05$abcdefghijklmnopqrstuu", 
             "$2a$05$abcdefghijklmnopqrstuu5s2v8.iXieOjg/.AySBTTZIIVFJeBui"},
            {"password", "$2a$05$bvIG6Nmid91Mu9RcmmWZfO", 
             "$2a$05$bvIG6Nmid91Mu9RcmmWZfO5HJIMCT8riNW0hEp8f6/FuA2/mHZFpe"},
            {"", "$2a$06$DCq7YPn5Rq63x1Lad4cll.", 
             "$2a$06$DCq7YPn5Rq63x1Lad4cll.TV4S6ytwfsfvkgY8jIucDrjc8deX1s."},
            {"", "$2a$08$HqWuK6/Ng6sg9gQzbLrgb.", 
             "$2a$08$HqWuK6/Ng6sg9gQzbLrgb.Tl.ZHfXLhvt/SgVyWhQqgqcZ7ZuUtye"},
            {"", "$2a$10$k1wbIrmNyFAPwPVPSVa/ze", 
             "$2a$10$k1wbIrmNyFAPwPVPSVa/zecw2BCEnBwVS2GbrmgzxFUOqW9dk4TCW"},
            {"", "$2a$12$k42ZFHFWqBp3vWli.nIn8u", 
             "$2a$12$k42ZFHFWqBp3vWli.nIn8uYyIkbvYRvodzbfbK18SSsY.CsIQPlxO"}
        };

        for (int i = 0; i < canonicalVectors.length; i++) {
            String plain = canonicalVectors[i][0];
            String salt = canonicalVectors[i][1];
            String expected = canonicalVectors[i][2];

            String computed = BCrypt.hashpw(plain, salt);
            assertEquals(expected, computed, "Vector canónico " + i + " falló en hashpw");
            assertTrue(BCrypt.checkpw(plain, expected), "Vector canónico " + i + " falló en checkpw");
        }
    }

    @Test
    @DisplayName("BCrypt: Manejo robusto de contraseñas y hashes nulos o vacíos")
    void testManejoContraseñasNulasYVacias() {
        String salt = BCrypt.gensalt(10);

        // Clave nula debe lanzar IllegalArgumentException en hashpw
        assertThrows(IllegalArgumentException.class, () -> BCrypt.hashpw(null, salt));

        // Clave vacía es válida en BCrypt y genera hash verificable
        String hashVacio = BCrypt.hashpw("", salt);
        assertNotNull(hashVacio);
        assertTrue(BCrypt.checkpw("", hashVacio));
        assertFalse(BCrypt.checkpw("algo", hashVacio));

        // checkpw con argumentos nulos o hashes inválidos debe retornar false sin lanzar excepción
        assertFalse(BCrypt.checkpw(null, hashVacio));
        assertFalse(BCrypt.checkpw("password", null));
        assertFalse(BCrypt.checkpw("password", "hash_invalido_sin_formato"));
        assertFalse(BCrypt.checkpw("password", "$2a$xx$formato_invalido_de_rounds_1234567890"));
    }

    @Test
    @DisplayName("BCrypt: Manejo de contraseñas de longitud > 72 bytes (truncamiento canónico Blowfish)")
    void testTruncamientoYContraseñasLargas() {
        String base72 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        assertEquals(72, base72.length());

        String salt = BCrypt.gensalt(10);
        String pass72 = base72;
        String pass80 = base72 + "EXTRA_CHARS";
        String pass100 = base72 + "MUCHOS_MAS_CARACTERES_DESPUES_DE_LOS_72_BYTES_CANONICOS";

        String hash72 = BCrypt.hashpw(pass72, salt);
        String hash80 = BCrypt.hashpw(pass80, salt);
        String hash100 = BCrypt.hashpw(pass100, salt);

        // En la especificación OpenBSD BCrypt, los caracteres posteriores al byte 72 se truncan
        assertEquals(hash72, hash80, "Los caracteres posteriores a 72 bytes deben ser ignorados según la especificación");
        assertEquals(hash72, hash100);

        assertTrue(BCrypt.checkpw(pass72, hash72));
        assertTrue(BCrypt.checkpw(pass80, hash72));
        assertTrue(BCrypt.checkpw(pass100, hash72));
    }

    @Test
    @DisplayName("BCrypt: Validación estricta y rechazo de sales malformadas y factores de costo inválidos")
    void testSalesMalformadasYCostosInvalidos() {
        // Sal nula
        assertThrows(IllegalArgumentException.class, () -> BCrypt.hashpw("test", null));

        // Longitud menor a 28 caracteres
        assertThrows(IllegalArgumentException.class, () -> BCrypt.hashpw("test", "$2a$10$salcorta"));

        // No inicia con $2
        assertThrows(IllegalArgumentException.class, () -> BCrypt.hashpw("test", "$1a$10$DCq7YPn5Rq63x1Lad4cll."));

        // Rounds no numéricos
        assertThrows(IllegalArgumentException.class, () -> BCrypt.hashpw("test", "$2a$XX$DCq7YPn5Rq63x1Lad4cll."));

        // Rounds fuera de rango en hashpw (< 4 o > 30)
        assertThrows(IllegalArgumentException.class, () -> BCrypt.hashpw("test", "$2a$03$DCq7YPn5Rq63x1Lad4cll."));
        assertThrows(IllegalArgumentException.class, () -> BCrypt.hashpw("test", "$2a$31$DCq7YPn5Rq63x1Lad4cll."));

        // Rounds fuera de rango en gensalt (< 4 o > 30)
        assertThrows(IllegalArgumentException.class, () -> BCrypt.gensalt(3));
        assertThrows(IllegalArgumentException.class, () -> BCrypt.gensalt(31));
    }

    @Test
    @DisplayName("AuthService: Política de seguridad de contraseñas de máximo 72 bytes en UTF-8 y caracteres multibyte")
    void testPoliticaContrasena72BytesYMultibyteUTF8() throws Exception {
        // 1. Exactamente 72 bytes ASCII -> aceptado
        String pass72Ascii = "A".repeat(72);
        assertEquals(72, pass72Ascii.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
        assertDoesNotThrow(() -> authService.validarPoliticaContrasena(pass72Ascii));
        String hash72 = authService.hashearBCrypt(pass72Ascii);
        assertTrue(authService.verificarBCrypt(pass72Ascii, hash72));

        // 2. 73 bytes ASCII -> rechazado por exceder 72 bytes UTF-8
        String pass73Ascii = "A".repeat(73);
        assertEquals(73, pass73Ascii.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
        TorneoExcepcion ex73 = assertThrows(TorneoExcepcion.class, () -> authService.validarPoliticaContrasena(pass73Ascii));
        assertTrue(ex73.getMessage().contains("72 bytes"));
        assertThrows(TorneoExcepcion.class, () -> authService.hashearBCrypt(pass73Ascii));
        assertFalse(authService.verificarBCrypt(pass73Ascii, hash72));

        // 3. Caracteres multibyte UTF-8: cada caracter 'ñ' ocupa 2 bytes en UTF-8
        // 36 caracteres 'ñ' = 72 bytes UTF-8 -> aceptado
        String pass36Enye = "ñ".repeat(36);
        assertEquals(36, pass36Enye.length());
        assertEquals(72, pass36Enye.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
        assertDoesNotThrow(() -> authService.validarPoliticaContrasena(pass36Enye));
        String hash36Enye = authService.hashearBCrypt(pass36Enye);
        assertTrue(authService.verificarBCrypt(pass36Enye, hash36Enye));

        // 37 caracteres 'ñ' = 74 bytes UTF-8 -> rechazado aunque length() sea 37 (< 72)
        String pass37Enye = "ñ".repeat(37);
        assertEquals(37, pass37Enye.length());
        assertEquals(74, pass37Enye.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
        TorneoExcepcion exMultibyte = assertThrows(TorneoExcepcion.class, () -> authService.validarPoliticaContrasena(pass37Enye));
        assertTrue(exMultibyte.getMessage().contains("72 bytes"));
        assertThrows(TorneoExcepcion.class, () -> authService.hashearBCrypt(pass37Enye));
        assertFalse(authService.verificarBCrypt(pass37Enye, hash36Enye));
    }
}
