package com.gestionamateur.tpi.progav.dao;

import com.gestionamateur.tpi.progav.modelo.Arbitro;
import com.gestionamateur.tpi.progav.modelo.Cancha;
import com.gestionamateur.tpi.progav.modelo.Categoria;
import com.gestionamateur.tpi.progav.modelo.Equipo;
import com.gestionamateur.tpi.progav.modelo.Fase;
import com.gestionamateur.tpi.progav.modelo.Jugador;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.Torneo;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.servicio.AuthService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de Integración - Persistencia JDBC en PostgreSQL (tpi_pa2_inf04_test)")
public class PostgreSqlPersistenceIT {

    private static RolDAO rolDAO;
    private static UsuarioDAO usuarioDAO;
    private static ArbitroDAO arbitroDAO;
    private static CategoriaDAO categoriaDAO;
    private static CanchaDAO canchaDAO;
    private static TorneoDAO torneoDAO;
    private static FaseDAO faseDAO;
    private static EquipoDAO equipoDAO;
    private static JugadorDAO jugadorDAO;
    private static ConfiguracionDAO configuracionDAO;

    @BeforeAll
    static void setUp() throws SQLException {
        // Verificar que la base de datos de test esté activa y respondiendo
        assertTrue(ConexionBD.comprobarConexion(), "El servidor PostgreSQL no responde en 127.0.0.1:55432");

        rolDAO = new RolDAO();
        usuarioDAO = new UsuarioDAO();
        arbitroDAO = new ArbitroDAO();
        categoriaDAO = new CategoriaDAO();
        canchaDAO = new CanchaDAO();
        torneoDAO = new TorneoDAO();
        faseDAO = new FaseDAO();
        equipoDAO = new EquipoDAO();
        jugadorDAO = new JugadorDAO();
        configuracionDAO = new ConfiguracionDAO();
    }

    @Test
    @DisplayName("IT-01: Verificación de conectividad JDBC y conexión válida")
    void testConectividad() throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexionTest()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
            assertTrue(conn.isValid(2));
        }
    }

    @Test
    @DisplayName("IT-02: Catálogo de roles cargado desde seed_data (5 roles canónicos)")
    void testCatalogoRoles() throws SQLException {
        List<RolDAO.RolRegistro> roles = rolDAO.buscarTodos();
        assertNotNull(roles);
        assertTrue(roles.size() >= 5);

        RolDAO.RolRegistro admin = rolDAO.buscarPorCodigo("ADMINISTRADOR");
        assertNotNull(admin);
        assertEquals("ADMINISTRADOR", admin.codigo);

        RolDAO.RolRegistro arb = rolDAO.buscarPorCodigo("ARBITRO");
        assertNotNull(arb);
    }

    @Test
    @DisplayName("IT-03: Configuraciones globales rectificadas (72h reclamo, 5 días apelación, 5 amarillas)")
    void testConfiguracionesGlobales() throws SQLException {
        int amarillas = configuracionDAO.obtenerValorInt("AMARILLAS_SUSPENSION", 0);
        assertEquals(5, amarillas, "El umbral de amarillas debe ser 5");

        int horasReclamo = configuracionDAO.obtenerValorInt("HORAS_LIMITE_RECLAMO", 0);
        assertEquals(72, horasReclamo, "El plazo límite de reclamo debe ser 72 horas corridas");

        int diasApelacion = configuracionDAO.obtenerValorInt("DIAS_LIMITE_APELACION", 0);
        assertEquals(5, diasApelacion, "El plazo límite de apelación debe ser 5 días corridos");
    }

    @Test
    @DisplayName("IT-04: Categorías base con edad_maxima nullable (Libre y Senior)")
    void testCategoriasBase() throws SQLException {
        List<Categoria> cats = categoriaDAO.buscarTodas();
        assertNotNull(cats);
        assertTrue(cats.size() >= 3);

        Categoria libre = null;
        Categoria senior = null;
        for (Categoria c : cats) {
            if ("Libre".equalsIgnoreCase(c.getNombre())) libre = c;
            if ("Senior".equalsIgnoreCase(c.getNombre())) senior = c;
        }

        assertNotNull(libre, "Categoría Libre debe existir en seed");
        assertEquals(16, libre.getEdadMinima());
        assertNull(libre.getEdadMaxima(), "Categoría Libre debe tener edad_maxima NULL");

        assertNotNull(senior, "Categoría Senior debe existir en seed");
        assertEquals(35, senior.getEdadMinima());
        assertNull(senior.getEdadMaxima(), "Categoría Senior debe tener edad_maxima NULL");
    }

    @Test
    @DisplayName("IT-05: Alta y persistencia de Usuario y Arbitro (Herencia Class Table)")
    void testPersistenciaUsuarioYArbitro() throws SQLException, TorneoExcepcion {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        String username = "arb_" + sufijo;

        Arbitro arbitro = new Arbitro(username, "pass123", username + "@torneo.com", "Roberto", "Pérez", "Nacional", "3764000000");
        Long idArbitro = arbitroDAO.insertar(arbitro, AuthService.hashearSha256("pass123"));
        assertNotNull(idArbitro);

        Arbitro recuperado = arbitroDAO.buscarPorId(idArbitro);
        assertNotNull(recuperado);
        assertEquals("Roberto", recuperado.getNombre());
        assertEquals("Nacional", recuperado.getCategoriaArbitral());

        // Comprobar que también es recuperable mediante UsuarioDAO
        Usuario comoUsuario = usuarioDAO.buscarPorId(idArbitro);
        assertNotNull(comoUsuario);
        assertEquals(username, comoUsuario.getUsuario());
        assertEquals(TiposDatos.Rol.ARBITRO, comoUsuario.getRol());
    }

    @Test
    @DisplayName("IT-06: Persistencia de Torneo, Fases y cálculo de ventana de acumulación")
    void testPersistenciaTorneoYFases() throws SQLException, TorneoExcepcion {
        String nombreTorneo = "Torneo IT " + UUID.randomUUID().toString().substring(0, 6);
        Torneo t = new Torneo(nombreTorneo, "LIGA", 8, new Date());
        Long idTorneo = torneoDAO.insertar(t);
        assertNotNull(idTorneo);

        // Fase 1: Clasificación (orden 1, limpia_amarillas = false)
        Fase f1 = new Fase("Clasificación", 1, false);
        Long idF1 = faseDAO.insertar(f1, idTorneo);
        assertNotNull(idF1);

        // Fase 2: Semifinales (orden 2, limpia_amarillas = true -> resetea conteo en playoffs)
        Fase f2 = new Fase("Semifinales", 2, true);
        Long idF2 = faseDAO.insertar(f2, idTorneo);
        assertNotNull(idF2);

        // Fase 3: Final (orden 3, limpia_amarillas = false)
        Fase f3 = new Fase("Final", 3, false);
        Long idF3 = faseDAO.insertar(f3, idTorneo);
        assertNotNull(idF3);

        try (Connection conn = ConexionBD.obtenerConexionTest()) {
            // En fase 1: el inicio de ventana es 1
            int ventanaF1 = faseDAO.obtenerOrdenInicioVentana(conn, idTorneo, 1);
            assertEquals(1, ventanaF1);

            // En fase 2: limpia_amarillas = true en orden 2, ventana inicia en 2
            int ventanaF2 = faseDAO.obtenerOrdenInicioVentana(conn, idTorneo, 2);
            assertEquals(2, ventanaF2);

            // En fase 3: última limpieza fue en orden 2, ventana inicia en 2
            int ventanaF3 = faseDAO.obtenerOrdenInicioVentana(conn, idTorneo, 3);
            assertEquals(2, ventanaF3);
        }
    }

    @Test
    @DisplayName("IT-07: Persistencia de Cancha, Equipo y Jugador con integridad referencial")
    void testPersistenciaEquipoYJugador() throws SQLException, TorneoExcepcion {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);

        // 1. Cancha
        Cancha cancha = new Cancha("Cancha IT " + sufijo, "Calle IT 123", "Césped Sintético");
        Long idCancha = canchaDAO.insertar(cancha);

        // 2. Capitán
        Usuario capitan = new Usuario("cap_" + sufijo, "1234", "cap_" + sufijo + "@club.com", "Carlos", "Tevez", TiposDatos.Rol.CAPITAN);
        Long idCap = usuarioDAO.insertar(capitan, AuthService.hashearSha256("1234"), 4); // rol 4 = CAPITAN

        // 3. Categoría Libre
        Categoria libre = categoriaDAO.buscarPorId(2L); // ID 2 = Libre

        // 4. Equipo
        Equipo equipo = new Equipo("Club Atlético " + sufijo, libre, "Azul y Oro", cancha, capitan);
        Long idEquipo = equipoDAO.insertar(equipo, idCap);
        assertNotNull(idEquipo);

        // 5. Jugador
        int dniUnico = 40000000 + (int)(Math.random() * 9000000);
        Jugador jugador = new Jugador(idEquipo, "Goleador " + sufijo, dniUnico, new Date(95, 0, 1), "Delantero", TiposDatos.EstadoJugador.ACTIVO);
        Long idJugador = jugadorDAO.insertar(jugador, idEquipo);
        assertNotNull(idJugador);

        // Recuperar y verificar
        Jugador recuperado = jugadorDAO.buscarPorId(idJugador);
        assertNotNull(recuperado);
        assertEquals(dniUnico, recuperado.getDni());
        assertEquals(idEquipo, recuperado.getIdEquipo());
        assertEquals(TiposDatos.EstadoJugador.ACTIVO, recuperado.getEstado());
    }
}
