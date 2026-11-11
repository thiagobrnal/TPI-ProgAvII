package com.gestionamateur.tpi.progav.servicio;

import com.gestionamateur.tpi.progav.dao.ConexionBD;
import com.gestionamateur.tpi.progav.dao.UsuarioDAO;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de Integración - Concurrencia de Bootstrap y Exclusión Mutua PostgreSQL (ETAPA D)")
public class BootstrapConcurrenteIT {

    private static final String TABLA_TEST = "test_bootstrap_usuarios";
    private static UsuarioDAO usuarioDAO;

    @BeforeAll
    static void setUp() throws SQLException {
        assertTrue(ConexionBD.comprobarConexion(), "PostgreSQL debe estar activo en puerto 55432");
        usuarioDAO = new UsuarioDAO();

        try (Connection conn = ConexionBD.obtenerConexion();
             Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS " + TABLA_TEST);
            st.execute("CREATE TABLE " + TABLA_TEST + " ("
                     + "id_usuario BIGSERIAL PRIMARY KEY, "
                     + "usuario VARCHAR(50) UNIQUE NOT NULL, "
                     + "contrasena VARCHAR(255) NOT NULL, "
                     + "mail VARCHAR(100) NOT NULL, "
                     + "nombre VARCHAR(50) NOT NULL, "
                     + "apellido VARCHAR(50) NOT NULL, "
                     + "id_rol INT NOT NULL, "
                     + "activo BOOLEAN NOT NULL DEFAULT TRUE"
                     + ")");
        }
    }

    @AfterAll
    static void tearDown() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion();
             Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS " + TABLA_TEST);
        }
    }

    @Test
    @DisplayName("BOOTSTRAP-01: 10 hilos concurrentes compiten por bootstrap; sólo uno triunfa y la tabla queda con un único registro")
    void testBootstrapConcurrenteUnicoGanador() throws Exception {
        int numHilos = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numHilos);
        CountDownLatch latchSalida = new CountDownLatch(1);
        CountDownLatch latchLlegada = new CountDownLatch(numHilos);

        AtomicInteger exitos = new AtomicInteger(0);
        AtomicInteger rechazosEsperados = new AtomicInteger(0);
        List<Throwable> erroresInesperados = new CopyOnWriteArrayList<>();

        String claveHash = AuthService.hashearBCrypt("AdminPassword123!");

        for (int i = 0; i < numHilos; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    Usuario u = new Usuario("superadmin_" + index, "AdminPassword123!",
                            "admin" + index + "@torneo.local", "Super", "Admin", TiposDatos.Rol.ADMINISTRADOR);
                    latchSalida.await(); // Espera a que todos los hilos arranquen al unísono

                    usuarioDAO.insertarPrimerAdministradorAtomico(TABLA_TEST, u, claveHash, 1);
                    exitos.incrementAndGet();
                } catch (TorneoExcepcion te) {
                    if (te.getMessage().contains("el sistema ya posee usuarios registrados")) {
                        rechazosEsperados.incrementAndGet();
                    } else {
                        erroresInesperados.add(te);
                    }
                } catch (Throwable t) {
                    erroresInesperados.add(t);
                } finally {
                    latchLlegada.countDown();
                }
            });
        }

        // Disparar todos los hilos simultáneamente
        latchSalida.countDown();

        boolean terminaron = latchLlegada.await(15, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(terminaron, "Todos los hilos concurrentes debieron haber concluido antes del timeout.");

        assertTrue(erroresInesperados.isEmpty(), "No deben producirse errores inesperados: " + erroresInesperados);
        assertEquals(1, exitos.get(), "Exactamente UN hilo debió lograr insertar el primer administrador.");
        assertEquals(numHilos - 1, rechazosEsperados.get(), "Los restantes 9 hilos debieron ser rechazados por exclusión mutua.");

        // Verificación de consistencia final en la base de datos
        try (Connection conn = ConexionBD.obtenerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + TABLA_TEST)) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1), "La tabla de usuarios debe contener exacta y únicamente 1 registro.");
        }
    }

    @Test
    @DisplayName("BOOTSTRAP-02: Rechazo inmediato de inicialización atómica cuando ya existe al menos un usuario")
    void testBootstrapRechazoPosterior() throws Exception {
        Usuario nuevoAdmin = new Usuario("intruso_admin", "AdminPassword123!",
                "intruso@torneo.local", "Intruso", "Admin", TiposDatos.Rol.ADMINISTRADOR);
        String claveHash = AuthService.hashearBCrypt("AdminPassword123!");

        TorneoExcepcion ex = assertThrows(TorneoExcepcion.class, () ->
            usuarioDAO.insertarPrimerAdministradorAtomico(TABLA_TEST, nuevoAdmin, claveHash, 1)
        );
        assertTrue(ex.getMessage().contains("el sistema ya posee usuarios registrados"));
    }
}
