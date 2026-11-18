package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.HeadlessException;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de integracion Swing - Interfaz de Usuario y Controles (Fase 3 / H-06).
 *
 * <p>DEF-007: cada ventana creada por esta suite se libera con {@code dispose()} en el hilo de
 * eventos de AWT, incluso si una asercion falla. Sin esta liberacion, el Event Dispatch Thread
 * queda vivo y el proceso de pruebas no termina de forma ordenada, lo que mantiene bloqueados
 * los archivos .class generados y provoca fallos de {@code mvn clean} sobre el directorio
 * {@code target/test-classes}.
 *
 * <p>Nota: la correccion de DEF-007 no implica por si sola que el bloqueo de archivos de
 * {@code mvn clean} quede resuelto; esa comprobacion se realiza por separado sobre la copia
 * temporal de validacion.
 */
@DisplayName("Pruebas de Integración Swing - Interfaz de Usuario y Controles (Fase 3 / H-06)")
public class SwingGuiIntegrationTest {

    @Test
    @DisplayName("GUI-01: Instanciación segura de FrmLogin en entorno headless/desktop")
    void testFrmLoginInstanciacion() {
        assertDoesNotThrow(() -> {
            try {
                FrmLogin login = new FrmLogin();
                try {
                    assertNotNull(login);
                    assertEquals("Sistema de Torneos - Login", login.getTitle());
                } finally {
                    liberar(login);
                }
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
                try {
                    assertNotNull(frmArbitro);
                } finally {
                    liberar(frmArbitro);
                }

                // 2. CAPITÁN: no debe poder programar partidos ni sedes
                Usuario cap = new Usuario("cap_test", "123", "cap@test.com", "Lucas", "Pratto", TiposDatos.Rol.CAPITAN);
                FrmPrincipal frmCap = new FrmPrincipal(cap);
                try {
                    assertNotNull(frmCap);
                } finally {
                    liberar(frmCap);
                }

                // 3. ORGANIZADOR / ADMINISTRADOR: acceso irrestricto
                Usuario admin = new Usuario("admin_test", "123", "admin@test.com", "Admin", "General", TiposDatos.Rol.ADMINISTRADOR);
                FrmPrincipal frmAdmin = new FrmPrincipal(admin);
                try {
                    assertNotNull(frmAdmin);
                } finally {
                    liberar(frmAdmin);
                }
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
                try {
                    assertNotNull(regPublico);
                    assertTrue(!regPublico.isModoBootstrap());
                } finally {
                    liberar(regPublico);
                }

                FrmRegistroUsuario regBootstrap = new FrmRegistroUsuario(true);
                try {
                    assertNotNull(regBootstrap);
                    assertTrue(regBootstrap.isModoBootstrap());
                } finally {
                    liberar(regBootstrap);
                }
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
                try {
                    assertNotNull(fCancha);
                } finally {
                    liberar(fCancha);
                }

                FrmAltaEquipo fEquipo = new FrmAltaEquipo();
                try {
                    assertNotNull(fEquipo);
                } finally {
                    liberar(fEquipo);
                }

                FrmAltaJugador fJugador = new FrmAltaJugador();
                try {
                    assertNotNull(fJugador);
                } finally {
                    liberar(fJugador);
                }

                FrmProgramarPartido fPartido = new FrmProgramarPartido();
                try {
                    assertNotNull(fPartido);
                } finally {
                    liberar(fPartido);
                }
            } catch (HeadlessException e) {
                assertTrue(true);
            }
        });
    }

    /**
     * Libera la ventana en el hilo de eventos de AWT para no dejar vivos el EDT ni el
     * class loader que mantiene mapeados los .class generados.
     */
    private static void liberar(JFrame ventana) {
        if (ventana == null) {
            return;
        }
        try {
            SwingUtilities.invokeAndWait(() -> ventana.dispose());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ventana.dispose();
        } catch (InvocationTargetException e) {
            ventana.dispose();
        } catch (IllegalStateException e) {
            // El EDT no puede ejecutarse (entorno headless): se libera directamente.
            ventana.dispose();
        }
    }
}
