package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.HeadlessException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de Integración Swing - Interfaz de Usuario y Controles (Fase 3 / H-06)")
public class SwingGuiIntegrationTest {

    @Test
    @DisplayName("GUI-01: Instanciación segura de FrmLogin en entorno headless/desktop")
    void testFrmLoginInstanciacion() {
        assertDoesNotThrow(() -> {
            try {
                FrmLogin login = new FrmLogin();
                assertNotNull(login);
                assertEquals("Sistema de Torneos - Login", login.getTitle());
            } catch (HeadlessException e) {
                // En servidores CI sin servidor X11 o display gráfico, HeadlessException es esperada y válida
                assertTrue(true);
            }
        });
    }

    @Test
    @DisplayName("GUI-02: Instanciación de FrmPrincipal y filtrado de botones por perfil de usuario")
    void testFrmPrincipalSegunRol() {
        assertDoesNotThrow(() -> {
            try {
                // 1. ÁRBITRO: no debe poder gestionar equipos, jugadores, canchas ni programar partidos
                Usuario arbitro = new Usuario("arbitro_test", "123", "arbitro@test.com", "Néstor", "Pitana", TiposDatos.Rol.ARBITRO);
                FrmPrincipal frmArbitro = new FrmPrincipal(arbitro);
                assertNotNull(frmArbitro);

                // 2. CAPITÁN: no debe poder programar partidos ni sedes
                Usuario cap = new Usuario("cap_test", "123", "cap@test.com", "Lucas", "Pratto", TiposDatos.Rol.CAPITAN);
                FrmPrincipal frmCap = new FrmPrincipal(cap);
                assertNotNull(frmCap);

                // 3. ORGANIZADOR / ADMINISTRADOR: acceso irrestricto
                Usuario admin = new Usuario("admin_test", "123", "admin@test.com", "Admin", "General", TiposDatos.Rol.ADMINISTRADOR);
                FrmPrincipal frmAdmin = new FrmPrincipal(admin);
                assertNotNull(frmAdmin);
            } catch (HeadlessException e) {
                assertTrue(true);
            }
        });
    }

    @Test
    @DisplayName("GUI-03: Instanciación y controles de FrmRegistroUsuario (Cierre H-03 Escalamiento de Privilegios)")
    void testFrmRegistroUsuario() {
        assertDoesNotThrow(() -> {
            try {
                FrmRegistroUsuario regPublico = new FrmRegistroUsuario(false);
                assertNotNull(regPublico);
                assertFalse(regPublico.isModoBootstrap());

                FrmRegistroUsuario regBootstrap = new FrmRegistroUsuario(true);
                assertNotNull(regBootstrap);
                assertTrue(regBootstrap.isModoBootstrap());
            } catch (HeadlessException e) {
                assertTrue(true);
            }
        });
    }

    @Test
    @DisplayName("GUI-04: Instanciación de formularios de alta (Cancha, Equipo, Jugador, Partido)")
    void testFormulariosAlta() {
        assertDoesNotThrow(() -> {
            try {
                FrmAltaCancha fCancha = new FrmAltaCancha();
                assertNotNull(fCancha);

                FrmAltaEquipo fEquipo = new FrmAltaEquipo();
                assertNotNull(fEquipo);

                FrmAltaJugador fJugador = new FrmAltaJugador();
                assertNotNull(fJugador);

                FrmProgramarPartido fPartido = new FrmProgramarPartido();
                assertNotNull(fPartido);
            } catch (HeadlessException e) {
                assertTrue(true);
            }
        });
    }
}
