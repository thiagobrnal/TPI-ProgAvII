package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.seguridad.PoliticaPermisos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.HeadlessException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Seguridad UI, PoliticaPermisos y semántica cromática del menú principal")
public class SeguridadPermisosYMenuTest {

    private static final Color BG_CONTROL_DENIED = new Color(0x1B, 0x23, 0x30);
    private static final Color BG_NEUTRAL_ENABLED = new Color(0x25, 0x33, 0x47);

    private static Usuario crearUsuarioConId(Long id, TiposDatos.Rol rol, boolean activo) throws TorneoExcepcion {
        Usuario u = new Usuario("user_" + rol.name().toLowerCase(), "hash", "user@test.com", "Nombre", "Apellido", rol);
        u.setId(id);
        u.setActivo(activo);
        return u;
    }

    private static List<JButton> recolectarBotones(Container root) {
        List<JButton> res = new ArrayList<>();
        for (Component c : root.getComponents()) {
            if (c instanceof JButton) {
                res.add((JButton) c);
            }
            if (c instanceof Container) {
                res.addAll(recolectarBotones((Container) c));
            }
        }
        return res;
    }

    private static List<JTable> recolectarTablas(Container root) {
        List<JTable> res = new ArrayList<>();
        for (Component c : root.getComponents()) {
            if (c instanceof JTable) {
                res.add((JTable) c);
            }
            if (c instanceof Container) {
                res.addAll(recolectarTablas((Container) c));
            }
        }
        return res;
    }

    private static Map<String, JButton> indexarBotonesPorTexto(Container root) {
        Map<String, JButton> mapa = new LinkedHashMap<>();
        for (JButton b : recolectarBotones(root)) {
            if (b.getText() != null) {
                mapa.put(b.getText(), b);
            }
        }
        return mapa;
    }

    private static void liberar(JFrame frame) {
        if (frame == null) {
            return;
        }
        try {
            SwingUtilities.invokeAndWait(frame::dispose);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            frame.dispose();
        } catch (InvocationTargetException | IllegalStateException e) {
            frame.dispose();
        }
    }

    @Test
    @DisplayName("PoliticaPermisos distingue visibilidad visual de operación persistida (id obligatorio y activo)")
    void testPoliticaPermisosFailClosed() throws TorneoExcepcion {
        Usuario sinId = new Usuario("sinid", "hash123", "a@b.c", "A", "B", TiposDatos.Rol.ADMINISTRADOR);
        Usuario adminValido = crearUsuarioConId(10L, TiposDatos.Rol.ADMINISTRADOR, true);
        Usuario adminInactivo = crearUsuarioConId(11L, TiposDatos.Rol.ADMINISTRADOR, false);

        assertFalse(PoliticaPermisos.esOperadorPersistidoValido(null));
        assertFalse(PoliticaPermisos.esOperadorPersistidoValido(sinId));
        assertFalse(PoliticaPermisos.esOperadorPersistidoValido(adminInactivo));
        assertTrue(PoliticaPermisos.esOperadorPersistidoValido(adminValido));

        assertFalse(PoliticaPermisos.tienePermisoOperacion(sinId, PoliticaPermisos.ModuloMenu.GESTIONAR_USUARIOS));
        assertFalse(PoliticaPermisos.tienePermisoOperacion(adminInactivo, PoliticaPermisos.ModuloMenu.GESTIONAR_USUARIOS));
        assertTrue(PoliticaPermisos.tienePermisoOperacion(adminValido, PoliticaPermisos.ModuloMenu.GESTIONAR_USUARIOS));

        assertFalse(PoliticaPermisos.tienePermiso(crearUsuarioConId(20L, TiposDatos.Rol.ORGANIZADOR, true),
                PoliticaPermisos.ModuloMenu.GESTIONAR_USUARIOS));
        assertFalse(PoliticaPermisos.tienePermiso(crearUsuarioConId(21L, TiposDatos.Rol.ORGANIZADOR, true),
                PoliticaPermisos.ModuloMenu.CONFIGURACION_GENERAL));
        assertTrue(PoliticaPermisos.tienePermiso(crearUsuarioConId(22L, TiposDatos.Rol.ORGANIZADOR, true),
                PoliticaPermisos.ModuloMenu.GESTIONAR_TORNEOS));
        assertTrue(PoliticaPermisos.tienePermiso(null, PoliticaPermisos.ModuloMenu.CONSULTA_PUBLICA));
        assertFalse(PoliticaPermisos.puedeCerrarSesion(null));
        assertFalse(PoliticaPermisos.puedeCerrarSesion(crearUsuarioConId(30L, TiposDatos.Rol.PUBLICO, true)));
        assertTrue(PoliticaPermisos.puedeCerrarSesion(adminValido));
    }

    @Test
    @DisplayName("EstilosUI resuelve colisión cromática entre botones Neutro habilitados y módulos denegados")
    void testEstilosUISemanticaCromatica() {
        JButton btnNeutro = new JButton("Neutro");
        EstilosUI.estilizarBotonNeutro(btnNeutro);
        JButton btnDenegado = new JButton("Denegado");
        EstilosUI.estilizarBotonDenegado(btnDenegado);

        assertEquals(BG_NEUTRAL_ENABLED, btnNeutro.getBackground());
        assertEquals(BG_CONTROL_DENIED, btnDenegado.getBackground());
        assertNotEquals(btnNeutro.getBackground(), btnDenegado.getBackground(),
                "El fondo neutro habilitado y el fondo denegado deben ser visualmente distintos.");
        assertNotEquals(EstilosUI.ACTION_PRIMARY, btnDenegado.getBackground());
        assertEquals(new Color(0x04, 0x78, 0x57), EstilosUI.ACTION_PRIMARY);
    }

    @Test
    @DisplayName("FrmPrincipal muestra nombres reales, botones verdes habilitados y grises deshabilitados con tooltip para todos los roles")
    void testFrmPrincipalPorTodosLosRoles() throws TorneoExcepcion {
        try {
            Usuario[] perfiles = {
                    crearUsuarioConId(1L, TiposDatos.Rol.ADMINISTRADOR, true),
                    crearUsuarioConId(2L, TiposDatos.Rol.ORGANIZADOR, true),
                    crearUsuarioConId(3L, TiposDatos.Rol.ARBITRO, true),
                    crearUsuarioConId(4L, TiposDatos.Rol.CAPITAN, true),
                    crearUsuarioConId(5L, TiposDatos.Rol.PUBLICO, true),
                    null
            };

            for (Usuario perfil : perfiles) {
                FrmPrincipal frm = new FrmPrincipal(perfil, null);
                try {
                    Map<String, JButton> botones = indexarBotonesPorTexto(frm.getContentPane());
                    for (PoliticaPermisos.ModuloMenu modulo : PoliticaPermisos.ModuloMenu.values()) {
                        JButton btn = botones.get(modulo.getEtiquetaMenu());
                        assertNotNull(btn, "Debe existir botón con etiqueta exacta: " + modulo.getEtiquetaMenu());
                        boolean esperadoPermitido = PoliticaPermisos.tienePermiso(perfil, modulo);
                        assertEquals(esperadoPermitido, btn.isEnabled(),
                                "Estado enabled incorrecto en " + modulo.name() + " para " + (perfil == null ? "Invitado" : perfil.getRol()));
                        if (esperadoPermitido) {
                            assertEquals(EstilosUI.ACTION_PRIMARY, btn.getBackground(),
                                    "Módulo habilitado debe ser verde: " + modulo.getEtiquetaMenu());
                            assertEquals(Color.WHITE, btn.getForeground());
                        } else {
                            assertEquals(BG_CONTROL_DENIED, btn.getBackground(),
                                    "Módulo denegado debe ser gris neutro apagado: " + modulo.getEtiquetaMenu());
                            assertEquals(EstilosUI.TEXT_DISABLED, btn.getForeground());
                            assertNotNull(btn.getToolTipText());
                            assertTrue(btn.getToolTipText().contains("No disponible"),
                                    "Módulo denegado debe informar el motivo en tooltip: " + modulo.getEtiquetaMenu());
                        }
                    }

                    JButton btnCerrar = botones.get("Cerrar Sesión");
                    assertNotNull(btnCerrar);
                    boolean autenticado = PoliticaPermisos.puedeCerrarSesion(perfil);
                    assertEquals(autenticado, btnCerrar.isEnabled());
                    assertEquals(autenticado ? EstilosUI.ACTION_DANGER : BG_CONTROL_DENIED,
                            btnCerrar.getBackground());
                } finally {
                    liberar(frm);
                }
            }

            // Constructor sin argumentos equivale a sesión Invitado / no autenticada
            FrmPrincipal frmNoArg = new FrmPrincipal();
            try {
                Map<String, JButton> botones = indexarBotonesPorTexto(frmNoArg.getContentPane());
                assertTrue(botones.get(PoliticaPermisos.ModuloMenu.CONSULTA_PUBLICA.getEtiquetaMenu()).isEnabled());
                assertFalse(botones.get(PoliticaPermisos.ModuloMenu.GESTIONAR_USUARIOS.getEtiquetaMenu()).isEnabled());
                assertFalse(botones.get("Cerrar Sesión").isEnabled());
            } finally {
                liberar(frmNoArg);
            }
        } catch (HeadlessException ignored) {
            // Entorno CI headless: omitir renderizado gráfico AWT
        }
    }

    @Test
    @DisplayName("Formularios restringidos son fail-closed sin operador válido (tablas vacías y acciones deshabilitadas)")
    void testFormulariosFailClosedSinOperador() throws TorneoExcepcion {
        try {
            Usuario operadorFalsoSinId = new Usuario("forged", "hash123", "f@t.c", "F", "F", TiposDatos.Rol.ADMINISTRADOR);
            Usuario publicoConId = crearUsuarioConId(99L, TiposDatos.Rol.PUBLICO, true);

            for (Usuario operador : new Usuario[]{null, operadorFalsoSinId, publicoConId}) {
                JFrame[] frames = {
                        new FrmAltaEquipo(operador),
                        new FrmAltaCancha(operador),
                        new FrmAltaJugador(operador),
                        new FrmProgramarPartido(operador),
                        new FrmCargarResultado(operador),
                        new FrmTribunal(operador),
                        new FrmReclamos(operador),
                        new FrmConfiguracion(operador),
                        new FrmTorneos(operador),
                        new FrmGestorUsuarios(operador),
                        new FrmCredencialJugador(operador),
                        new FrmSuspenderPartido(operador),
                        new FrmReportes(operador)
                };
                for (JFrame f : frames) {
                    try {
                        for (JTable t : recolectarTablas(f.getContentPane())) {
                            assertEquals(0, t.getRowCount(),
                                    f.getClass().getSimpleName() + " no debe exponer filas sin operador autorizado.");
                        }
                        for (JButton b : recolectarBotones(f.getContentPane())) {
                            String txt = b.getText() == null ? "" : b.getText();
                            if (!txt.isEmpty() && !txt.equalsIgnoreCase("Volver al menú")) {
                                assertFalse(b.isEnabled(),
                                        f.getClass().getSimpleName() + " no debe habilitar acción '" + txt + "' sin autorización.");
                            }
                        }
                    } finally {
                        liberar(f);
                    }
                }
            }
        } catch (HeadlessException ignored) {
            // Entorno CI headless: omitir renderizado gráfico AWT
        }
    }
}
