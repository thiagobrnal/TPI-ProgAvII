package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.dao.ConsultaPublicaDAO;
import com.gestionamateur.tpi.progav.servicio.ConsultaPublicaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Window;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import javax.swing.JComboBox;
import org.junit.jupiter.api.Assumptions;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias de contrato para CU-08 (Fixture del torneo en Consulta Publica).
 *
 * <p>Valida sin base de datos externa la correspondencia exacta entre la proyeccion SQL de
 * {@link ConsultaPublicaDAO#fixtureDeTorneo(Connection, Long)}, el contrato de
 * {@link ConsultaPublicaService#fixture(Long)} y el modelo/vista de la tabla CU-08 en
 * {@link FrmConsultaPublica}.
 */
@DisplayName("CU-08: Contrato de columnas DAO -> Service -> FrmConsultaPublica")
public class ConsultaPublicaCu08ContratoTest {

    private static final String[] ENCABEZADOS_ESPERADOS_CU08 = {
            "Fecha y hora", "Estado", "Local", "Visitante", "Resultado", "Sede", "Fase", "Torneo"
    };

    private static final String[] COLUMNAS_SQL_ESPERADAS_CU08 = {
            "fecha_hora", "estado", "equipo_local", "equipo_visitante", "resultado", "sede", "fase", "torneo"
    };

    @Test
    @DisplayName("CU-08 DAO: fixtureDeTorneo proyecta 8 columnas en el orden exacto del contrato (sin id_partido)")
    void testDaoFixtureProyectaOchoColumnasSinIdPartido() throws SQLException {
        ConsultaPublicaDAO dao = new ConsultaPublicaDAO();
        CapturaSql captura = new CapturaSql(filasSinteticasPorAlias());

        List<Object[]> filas = dao.fixtureDeTorneo(captura.conexion, 42L);

        assertNotNull(captura.ultimoSql, "Debe ejecutarse una consulta SQL");
        List<String> proyecciones = extraerNombresColumnasSelect(captura.ultimoSql);

        assertEquals(8, proyecciones.size(),
                "fixtureDeTorneo debe proyectar exactamente 8 columnas; actual=" + proyecciones);
        assertFalse(proyecciones.contains("id_partido"),
                "p.id_partido no debe formar parte de la proyeccion de presentacion CU-08");
        assertEquals(List.of(COLUMNAS_SQL_ESPERADAS_CU08), proyecciones,
                "Las columnas proyectadas deben respetar exactamente el orden del contrato CU-08");

        String sqlUpper = captura.ultimoSql.toUpperCase(Locale.ROOT);
        assertTrue(sqlUpper.contains("ORDER BY P.FECHA_HORA ASC, P.ID_PARTIDO ASC"),
                "Debe conservarse el ordenamiento determinista por fecha_hora e id_partido");

        assertEquals(2, captura.parametros.size(), "Debe enlazar dos parametros BIGINT para el filtro de torneo");
        assertEquals(42L, captura.parametros.get(1));
        assertEquals(42L, captura.parametros.get(2));
        assertEquals(Types.BIGINT, captura.tiposParametros.get(1));
        assertEquals(Types.BIGINT, captura.tiposParametros.get(2));

        assertEquals(3, filas.size());
        assertEquals(8, filas.get(0).length);
        assertEquals("18/11/2026 20:30", filas.get(0)[0]);
        assertEquals("PROGRAMADO", filas.get(0)[1]);
        assertEquals("Atletico Guarani", filas.get(0)[2]);
        assertEquals("Deportivo Posadas", filas.get(0)[3]);
        assertEquals("0 - 0", filas.get(0)[4]);
        assertEquals("Cancha Norte", filas.get(0)[5]);
        assertEquals("Fecha 1", filas.get(0)[6]);
        assertEquals("Apertura 2026", filas.get(0)[7]);

        // Caso borde: equipo/sede nulos sin desplazamiento de columnas posteriores.
        assertEquals(8, filas.get(2).length);
        assertEquals("01/12/2026 21:00", filas.get(2)[0]);
        assertEquals("SUSPENDIDO", filas.get(2)[1]);
        assertNull(filas.get(2)[2]);
        assertNull(filas.get(2)[3]);
        assertEquals("0 - 0", filas.get(2)[4]);
        assertNull(filas.get(2)[5]);
        assertEquals("Semifinal", filas.get(2)[6]);
        assertEquals("Copa Verano", filas.get(2)[7]);
    }

    @Test
    @DisplayName("CU-08 DAO: parametro idTorneo nulo enlaza NULL como BIGINT y soporta resultado vacio")
    void testDaoFixtureConTorneoNuloYResultadoVacio() throws SQLException {
        ConsultaPublicaDAO dao = new ConsultaPublicaDAO();
        CapturaSql captura = new CapturaSql(List.of());

        List<Object[]> filas = dao.fixtureDeTorneo(captura.conexion, null);

        assertNotNull(filas);
        assertTrue(filas.isEmpty());
        assertNull(captura.parametros.get(1));
        assertNull(captura.parametros.get(2));
        assertEquals(Types.BIGINT, captura.tiposParametros.get(1));
        assertEquals(Types.BIGINT, captura.tiposParametros.get(2));
    }

    @Test
    @DisplayName("CU-08 Flujo completo DAO -> Service -> FrmConsultaPublica: cada encabezado coincide con su valor")
    void testFlujoCompletoTablaCu08CorrespondenciaExacta() {
        runEdt(() -> {
            Timer timer = new Timer(40, e -> cerrarDialogosModales());
            timer.setRepeats(true);
            FrmConsultaPublica frm = null;
            try {
                timer.start();
                frm = new FrmConsultaPublica();
                ConsultaPublicaDAO dao = new ConsultaPublicaDAO();
                CapturaSql capturaTorneosVacia = new CapturaSql(List.of());
                CapturaSql captura = new CapturaSql(filasSinteticasPorAlias());
                ConsultaPublicaService servicioStub = new ConsultaPublicaService(dao) {
                    @Override
                    public List<Object[]> torneos() {
                        try {
                            return dao.torneos(capturaTorneosVacia.conexion);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public List<Object[]> fixture(Long idTorneo) {
                        try {
                            return dao.fixtureDeTorneo(captura.conexion, idTorneo);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };

                Field campoServicio = FrmConsultaPublica.class.getDeclaredField("servicio");
                campoServicio.setAccessible(true);
                campoServicio.set(frm, servicioStub);

                JTabbedPane tabs = buscarComponente(frm.getContentPane(), JTabbedPane.class);
                assertNotNull(tabs);
                Component tabCu08 = tabs.getComponentAt(0);
                JTable tabla = buscarTabla(tabCu08);
                assertNotNull(tabla, "La pestaña Fixture (CU-08) debe contener una JTable");

                AbstractButton btnConsultar = buscarBoton(tabCu08, "Consultar");
                assertNotNull(btnConsultar, "La pestaña Fixture (CU-08) debe contener el boton Consultar");
                btnConsultar.doClick();

                TableModel model = tabla.getModel();
                assertEquals(8, model.getColumnCount(), "El modelo de tabla CU-08 debe tener 8 columnas");
                assertEquals(8, tabla.getColumnCount(), "La JTable CU-08 debe mostrar exactamente 8 columnas");
                for (int c = 0; c < ENCABEZADOS_ESPERADOS_CU08.length; c++) {
                    assertEquals(ENCABEZADOS_ESPERADOS_CU08[c], model.getColumnName(c),
                            "Encabezado incorrecto en modelo columna " + c);
                    assertEquals(ENCABEZADOS_ESPERADOS_CU08[c], tabla.getColumnName(c),
                            "Encabezado incorrecto en vista columna " + c);
                }

                assertEquals(3, tabla.getRowCount(), "Deben cargarse las 3 filas sinteticas del fixture");

                // Fila 0: partido programado con valores distinguibles
                assertEquals("18/11/2026 20:30", valorColumna(tabla, 0, "Fecha y hora"));
                assertEquals("PROGRAMADO", valorColumna(tabla, 0, "Estado"));
                assertEquals("Atletico Guarani", valorColumna(tabla, 0, "Local"));
                assertEquals("Deportivo Posadas", valorColumna(tabla, 0, "Visitante"));
                assertEquals("0 - 0", valorColumna(tabla, 0, "Resultado"));
                assertEquals("Cancha Norte", valorColumna(tabla, 0, "Sede"));
                assertEquals("Fecha 1", valorColumna(tabla, 0, "Fase"));
                assertEquals("Apertura 2026", valorColumna(tabla, 0, "Torneo"));

                // Fila 1: partido finalizado con marcador no nulo
                assertEquals("25/11/2026 18:00", valorColumna(tabla, 1, "Fecha y hora"));
                assertEquals("FINALIZADO", valorColumna(tabla, 1, "Estado"));
                assertEquals("Club Misiones", valorColumna(tabla, 1, "Local"));
                assertEquals("Real Obera", valorColumna(tabla, 1, "Visitante"));
                assertEquals("3 - 1", valorColumna(tabla, 1, "Resultado"));
                assertEquals("Estadio Central", valorColumna(tabla, 1, "Sede"));
                assertEquals("Cuartos", valorColumna(tabla, 1, "Fase"));
                assertEquals("Clausura 2026", valorColumna(tabla, 1, "Torneo"));

                // Fila 2: nulos intermedios no desplazan Resultado, Fase ni Torneo
                assertEquals("01/12/2026 21:00", valorColumna(tabla, 2, "Fecha y hora"));
                assertEquals("SUSPENDIDO", valorColumna(tabla, 2, "Estado"));
                assertNull(valorColumna(tabla, 2, "Local"));
                assertNull(valorColumna(tabla, 2, "Visitante"));
                assertEquals("0 - 0", valorColumna(tabla, 2, "Resultado"));
                assertNull(valorColumna(tabla, 2, "Sede"));
                assertEquals("Semifinal", valorColumna(tabla, 2, "Fase"));
                assertEquals("Copa Verano", valorColumna(tabla, 2, "Torneo"));

                // Verificar regresion: las otras pestañas conservan sus columnas y encabezados intactos
                JTable tablaCu09 = buscarTabla(tabs.getComponentAt(1));
                JTable tablaCu10 = buscarTabla(tabs.getComponentAt(2));
                JTable tablaCu11 = buscarTabla(tabs.getComponentAt(3));
                assertNotNull(tablaCu09);
                assertNotNull(tablaCu10);
                assertNotNull(tablaCu11);
                assertEquals(9, tablaCu09.getColumnCount(), "CU-09 debe conservar 9 columnas");
                assertEquals(List.of("Equipo", "Categoría", "PJ", "PG", "PE", "PP", "GF", "GC", "Jugadores del plantel"),
                        encabezadosTabla(tablaCu09));
                assertEquals(6, tablaCu10.getColumnCount(), "CU-10 debe conservar 6 columnas");
                assertEquals(List.of("Jugador", "Equipo", "Goles", "Tarjetas amarillas", "Tarjetas rojas", "Partidos con incidencia"),
                        encabezadosTabla(tablaCu10));
                assertEquals(7, tablaCu11.getColumnCount(), "CU-11 debe conservar 7 columnas");
                assertEquals(List.of("Torneo", "Estado del torneo", "Fase", "Orden", "Limpia amarillas", "Partidos programados", "Partidos finalizados"),
                        encabezadosTabla(tablaCu11));

                // Flujo con selector de torneo cargado (id_torneo no nulo) -> fixture(42L) -> JTable CU-08
                CapturaSql capturaTorneos = new CapturaSql(torneosSinteticos());
                CapturaSql capturaEquipos = new CapturaSql(equiposSinteticos());
                CapturaSql capturaFiltrada = new CapturaSql(filasSinteticasPorAlias().subList(0, 1));
                campoServicio.set(frm, new ConsultaPublicaService(dao) {
                    @Override
                    public List<Object[]> torneos() {
                        try {
                            return dao.torneos(capturaTorneos.conexion);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public List<Object[]> equipos() {
                        try {
                            return dao.equipos(capturaEquipos.conexion);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public List<Object[]> fixture(Long idTorneo) {
                        try {
                            return dao.fixtureDeTorneo(capturaFiltrada.conexion, idTorneo);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                Method cargarSelectores = FrmConsultaPublica.class.getDeclaredMethod("cargarSelectores");
                cargarSelectores.setAccessible(true);
                cargarSelectores.invoke(frm);

                @SuppressWarnings("unchecked")
                JComboBox<String> cmbTorneo = (JComboBox<String>) buscarComponente(tabCu08, JComboBox.class);
                assertNotNull(cmbTorneo);
                assertEquals(2, cmbTorneo.getItemCount());
                cmbTorneo.setSelectedIndex(1);
                btnConsultar.doClick();

                assertEquals(42L, capturaFiltrada.parametros.get(1),
                        "Debe propagarse el id_torneo seleccionado (42L) al primer parametro de fixtureDeTorneo");
                assertEquals(42L, capturaFiltrada.parametros.get(2),
                        "Debe propagarse el id_torneo seleccionado (42L) al segundo parametro de fixtureDeTorneo");
                assertEquals(1, tabla.getRowCount());
                assertEquals("18/11/2026 20:30", valorColumna(tabla, 0, "Fecha y hora"));
                assertEquals("Apertura 2026", valorColumna(tabla, 0, "Torneo"));

                // Recarga con resultado vacio conserva los 8 encabezados sin filas residuales
                CapturaSql capturaVacia = new CapturaSql(List.of());
                campoServicio.set(frm, new ConsultaPublicaService(dao) {
                    @Override
                    public List<Object[]> fixture(Long idTorneo) {
                        try {
                            return dao.fixtureDeTorneo(capturaVacia.conexion, idTorneo);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                btnConsultar.doClick();
                assertEquals(0, tabla.getRowCount(), "Una consulta vacia debe limpiar las filas previas");
                assertEquals(8, tabla.getColumnCount(), "Una consulta vacia debe conservar los 8 encabezados");
            } finally {
                timer.stop();
                if (frm != null) {
                    frm.dispose();
                }
            }
        });
    }

    @Test
    @DisplayName("Regresion DAO: las consultas CU-09..CU-11 y selectores mantienen sus columnas e IDs internos")
    void testRegresionOtrasConsultasPublicas() throws SQLException {
        ConsultaPublicaDAO dao = new ConsultaPublicaDAO();

        CapturaSql capEqStats = new CapturaSql(List.of());
        dao.estadisticasDeEquipo(capEqStats.conexion, 1L);
        assertEquals(9, extraerNombresColumnasSelect(capEqStats.ultimoSql).size());

        CapturaSql capJugStats = new CapturaSql(List.of());
        dao.estadisticasDeJugadores(capJugStats.conexion, 1L);
        assertEquals(6, extraerNombresColumnasSelect(capJugStats.ultimoSql).size());

        CapturaSql capFases = new CapturaSql(List.of());
        dao.avanceDeFases(capFases.conexion, 1L);
        assertEquals(7, extraerNombresColumnasSelect(capFases.ultimoSql).size());

        CapturaSql capTorneos = new CapturaSql(List.of());
        dao.torneos(capTorneos.conexion);
        List<String> colsTorneos = extraerNombresColumnasSelect(capTorneos.ultimoSql);
        assertEquals(5, colsTorneos.size());
        assertEquals("id_torneo", colsTorneos.get(0));

        CapturaSql capEquipos = new CapturaSql(List.of());
        dao.equipos(capEquipos.conexion);
        List<String> colsEquipos = extraerNombresColumnasSelect(capEquipos.ultimoSql);
        assertEquals(3, colsEquipos.size());
        assertEquals("id_equipo", colsEquipos.get(0));
    }

    private static Object valorColumna(JTable tabla, int fila, String nombreColumna) {
        int col = tabla.getColumnModel().getColumnIndex(nombreColumna);
        return tabla.getValueAt(fila, col);
    }

    private static List<String> encabezadosTabla(JTable tabla) {
        List<String> out = new ArrayList<>();
        for (int c = 0; c < tabla.getColumnCount(); c++) {
            out.add(tabla.getColumnName(c));
        }
        return out;
    }

    private static List<Map<String, Object>> torneosSinteticos() {
        List<Map<String, Object>> filas = new ArrayList<>();
        Map<String, Object> t1 = new LinkedHashMap<>();
        t1.put("id_torneo", 10L);
        t1.put("nombre", "Torneo Inicial");
        t1.put("estado", "FINALIZADO");
        t1.put("formato", "LIGA");
        t1.put("cupo_equipos", 8);
        filas.add(t1);

        Map<String, Object> t2 = new LinkedHashMap<>();
        t2.put("id_torneo", 42L);
        t2.put("nombre", "Apertura 2026");
        t2.put("estado", "EN_CURSO");
        t2.put("formato", "LIGA");
        t2.put("cupo_equipos", 10);
        filas.add(t2);
        return filas;
    }

    private static List<Map<String, Object>> equiposSinteticos() {
        List<Map<String, Object>> filas = new ArrayList<>();
        Map<String, Object> e1 = new LinkedHashMap<>();
        e1.put("id_equipo", 7L);
        e1.put("nombre", "Atletico Guarani");
        e1.put("categoria", "Libre");
        filas.add(e1);
        return filas;
    }

    private static List<Map<String, Object>> filasSinteticasPorAlias() {
        List<Map<String, Object>> filas = new ArrayList<>();

        Map<String, Object> f1 = new LinkedHashMap<>();
        f1.put("id_partido", 901L);
        f1.put("fecha_hora", "18/11/2026 20:30");
        f1.put("estado", "PROGRAMADO");
        f1.put("equipo_local", "Atletico Guarani");
        f1.put("equipo_visitante", "Deportivo Posadas");
        f1.put("resultado", "0 - 0");
        f1.put("sede", "Cancha Norte");
        f1.put("fase", "Fecha 1");
        f1.put("torneo", "Apertura 2026");
        filas.add(f1);

        Map<String, Object> f2 = new LinkedHashMap<>();
        f2.put("id_partido", 902L);
        f2.put("fecha_hora", "25/11/2026 18:00");
        f2.put("estado", "FINALIZADO");
        f2.put("equipo_local", "Club Misiones");
        f2.put("equipo_visitante", "Real Obera");
        f2.put("resultado", "3 - 1");
        f2.put("sede", "Estadio Central");
        f2.put("fase", "Cuartos");
        f2.put("torneo", "Clausura 2026");
        filas.add(f2);

        Map<String, Object> f3 = new LinkedHashMap<>();
        f3.put("id_partido", 903L);
        f3.put("fecha_hora", "01/12/2026 21:00");
        f3.put("estado", "SUSPENDIDO");
        f3.put("equipo_local", null);
        f3.put("equipo_visitante", null);
        f3.put("resultado", "0 - 0");
        f3.put("sede", null);
        f3.put("fase", "Semifinal");
        f3.put("torneo", "Copa Verano");
        filas.add(f3);

        return filas;
    }

    /**
     * Extrae los alias o nombres de columnas de la clausula SELECT principal (hasta el FROM de nivel 0).
     */
    private static List<String> extraerNombresColumnasSelect(String sql) {
        String upper = sql.toUpperCase(Locale.ROOT);
        int depth = 0;
        int selectIdx = -1;
        int fromIdx = -1;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                depth--;
            } else if (depth == 0) {
                if (selectIdx < 0 && upper.startsWith("SELECT ", i)) {
                    selectIdx = i + "SELECT ".length();
                } else if (selectIdx >= 0 && upper.startsWith(" FROM ", i)) {
                    fromIdx = i;
                    break;
                }
            }
        }
        if (selectIdx < 0 || fromIdx < 0) {
            throw new IllegalArgumentException("SQL sin SELECT/FROM de nivel superior: " + sql);
        }
        String lista = sql.substring(selectIdx, fromIdx);
        List<String> expresiones = new ArrayList<>();
        int inicio = 0;
        depth = 0;
        boolean enComilla = false;
        for (int i = 0; i < lista.length(); i++) {
            char ch = lista.charAt(i);
            if (ch == '\'') {
                enComilla = !enComilla;
            } else if (!enComilla) {
                if (ch == '(') {
                    depth++;
                } else if (ch == ')') {
                    depth--;
                } else if (ch == ',' && depth == 0) {
                    expresiones.add(lista.substring(inicio, i).trim());
                    inicio = i + 1;
                }
            }
        }
        expresiones.add(lista.substring(inicio).trim());

        List<String> nombres = new ArrayList<>();
        for (String expr : expresiones) {
            String exprUpper = expr.toUpperCase(Locale.ROOT);
            int asPos = exprUpper.lastIndexOf(" AS ");
            if (asPos >= 0) {
                nombres.add(expr.substring(asPos + 4).trim());
            } else {
                int dotPos = expr.lastIndexOf('.');
                nombres.add(dotPos >= 0 ? expr.substring(dotPos + 1).trim() : expr.trim());
            }
        }
        return nombres;
    }

    /** Proxy JDBC minimo en memoria que captura SQL, parametros y mapea filas por nombre de columna. */
    private static final class CapturaSql {
        String ultimoSql;
        final Map<Integer, Object> parametros = new LinkedHashMap<>();
        final Map<Integer, Integer> tiposParametros = new LinkedHashMap<>();
        final Connection conexion;

        CapturaSql(List<Map<String, Object>> filasPorAlias) {
            this.conexion = (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if ("prepareStatement".equals(method.getName()) && args != null && args.length == 1) {
                            this.ultimoSql = (String) args[0];
                            List<String> columnas = extraerNombresColumnasSelect(this.ultimoSql);
                            return crearPreparedStatement(columnas, filasPorAlias);
                        }
                        if ("close".equals(method.getName())) {
                            return null;
                        }
                        throw new UnsupportedOperationException("Metodo Connection no esperado: " + method.getName());
                    });
        }

        private PreparedStatement crearPreparedStatement(List<String> columnas, List<Map<String, Object>> filas) {
            return (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "setObject":
                                if (args != null && args.length == 3) {
                                    parametros.put((Integer) args[0], args[1]);
                                    tiposParametros.put((Integer) args[0], (Integer) args[2]);
                                }
                                return null;
                            case "executeQuery":
                                return crearResultSet(columnas, filas);
                            case "close":
                                return null;
                            default:
                                throw new UnsupportedOperationException("Metodo PreparedStatement no esperado: " + method.getName());
                        }
                    });
        }

        private ResultSet crearResultSet(List<String> columnas, List<Map<String, Object>> filas) {
            ResultSetMetaData metaData = (ResultSetMetaData) Proxy.newProxyInstance(
                    ResultSetMetaData.class.getClassLoader(),
                    new Class<?>[]{ResultSetMetaData.class},
                    (proxy, method, args) -> {
                        if ("getColumnCount".equals(method.getName())) {
                            return columnas.size();
                        }
                        if ("getColumnLabel".equals(method.getName()) || "getColumnName".equals(method.getName())) {
                            return columnas.get(((Integer) args[0]) - 1);
                        }
                        throw new UnsupportedOperationException("Metodo ResultSetMetaData no esperado: " + method.getName());
                    });
            final int[] cursor = {-1};
            return (ResultSet) Proxy.newProxyInstance(
                    ResultSet.class.getClassLoader(),
                    new Class<?>[]{ResultSet.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "getMetaData":
                                return metaData;
                            case "next":
                                cursor[0]++;
                                return cursor[0] < filas.size();
                            case "getObject":
                                int idx = (Integer) args[0];
                                String alias = columnas.get(idx - 1);
                                return filas.get(cursor[0]).get(alias);
                            case "close":
                                return null;
                            default:
                                throw new UnsupportedOperationException("Metodo ResultSet no esperado: " + method.getName());
                        }
                    });
        }
    }

    @FunctionalInterface
    private interface EdtAction {
        void run() throws Exception;
    }

    private static void runEdt(EdtAction action) {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Entorno headless sin display grafico");
        AtomicReference<Throwable> error = new AtomicReference<>();
        assertDoesNotThrow(() -> {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        EstilosUI.inicializarTema();
                        action.run();
                    } catch (Throwable t) {
                        error.set(t);
                    }
                });
            } catch (InvocationTargetException e) {
                if (contieneHeadlessException(e.getCause())) {
                    error.set(e.getCause());
                    return;
                }
                throw e;
            }
        });
        Throwable t = error.get();
        if (t != null) {
            if (contieneHeadlessException(t)) {
                Assumptions.assumeTrue(false, "Entorno headless sin display grafico");
            }
            if (t instanceof Error err) {
                throw err;
            }
            if (t instanceof RuntimeException re) {
                throw re;
            }
            throw new AssertionError("Excepcion inesperada en EDT", t);
        }
    }

    private static boolean contieneHeadlessException(Throwable t) {
        Throwable cur = t;
        int depth = 0;
        while (cur != null && depth < 16) {
            if (cur instanceof HeadlessException) {
                return true;
            }
            cur = cur.getCause();
            depth++;
        }
        return false;
    }

    private static JTable buscarTabla(Component root) {
        JScrollPane scroll = buscarComponente(root, JScrollPane.class);
        if (scroll != null) {
            JViewport vp = scroll.getViewport();
            if (vp != null && vp.getView() instanceof JTable tabla) {
                return tabla;
            }
        }
        return buscarComponente(root, JTable.class);
    }

    private static <T extends Component> T buscarComponente(Component root, Class<T> type) {
        if (type.isInstance(root)) {
            return type.cast(root);
        }
        if (root instanceof Container c) {
            for (Component child : c.getComponents()) {
                T found = buscarComponente(child, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void cerrarDialogosModales() {
        for (Window w : Window.getWindows()) {
            if (w instanceof JDialog d && d.isShowing() && d.getModalityType() != Dialog.ModalityType.MODELESS) {
                AbstractButton btnOk = buscarBoton(d, "Aceptar", "OK", "Cerrar");
                if (btnOk != null) {
                    btnOk.doClick();
                } else {
                    d.dispose();
                }
            }
        }
    }

    private static AbstractButton buscarBoton(Component root, String... textos) {
        if (root instanceof AbstractButton btn && btn.getText() != null) {
            for (String t : textos) {
                if (btn.getText().equalsIgnoreCase(t)) {
                    return btn;
                }
            }
        }
        if (root instanceof Container c) {
            for (Component child : c.getComponents()) {
                AbstractButton found = buscarBoton(child, textos);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
