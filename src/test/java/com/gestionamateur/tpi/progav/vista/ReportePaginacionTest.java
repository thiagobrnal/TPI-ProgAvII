package com.gestionamateur.tpi.progav.vista;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DEF-017 y A4 Portrait: paginacion correcta de los reportes imprimibles.
 *
 * <p>Las pruebas NO se limitan a contar paginas: capturan el texto exacto que el
 * {@link FrmReportes.ReportePrintable} dibuja en cada pagina (mediante el punto de extension
 * {@code dibujarTexto}) y comprueban que <b>todas las filas aparecen exactamente una vez y en
 * orden</b>.
 *
 * <p>El formato de pagina utilizado es el mismo que usa la aplicacion en produccion
 * ({@code FrmReportes.crearFormatoPortraitA4()}), de modo que la geometria verificada es
 * exactamente la que se usa al exportar a PDF.
 */
@DisplayName("DEF-017 y A4 Portrait: paginacion de los reportes imprimibles")
public class ReportePaginacionTest {

    /** Columnas de la tabla de posiciones (9 columnas, el caso mas exigente). */
    private static final String[] COLUMNAS_POSICIONES = {
            "Equipo", "PJ", "PG", "PE", "PP", "GF", "GC", "DG", "Puntos"
    };

    /** Columnas del reporte de jugadores (encabezado largo). */
    private static final String[] COLUMNAS_JUGADORES = {
            "Jugador", "Equipo", "Goles", "Tarjetas amarillas", "Tarjetas rojas", "Sanciones vigentes"
    };

    /**
     * ReportePrintable que registra cada texto dibujado. Permite saber con exactitud que
     * registros aparecen en cada pagina, en lugar de deducirlo del numero de paginas.
     */
    private static final class Grabador extends FrmReportes.ReportePrintable {

        final List<String[]> dibujos = new ArrayList<>();

        Grabador(String titulo, String[] columnas, List<Object[]> filas, String fecha) {
            super(titulo, columnas, filas, fecha);
        }

        @Override
        void dibujarTexto(Graphics2D g2, String texto, float x, float y) {
            dibujos.add(new String[]{texto, String.valueOf(x), String.valueOf(y)});
            super.dibujarTexto(g2, texto, x, y);
        }
    }

    /** Renderiza una pagina y devuelve los textos dibujados. */
    private static List<String[]> renderizar(FrmReportes.ReportePrintable printable, PageFormat pf, int pagina) {
        BufferedImage img = new BufferedImage(595, 842, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 595, 842);
        try {
            int res = printable.print(g, pf, pagina);
            assertEquals(java.awt.print.Printable.PAGE_EXISTS, res,
                    "La pagina " + pagina + " deberia existir.");
        } catch (java.awt.print.PrinterException e) {
            throw new IllegalStateException(e);
        } finally {
            g.dispose();
        }
        return printable instanceof Grabador ? ((Grabador) printable).dibujos : new ArrayList<>();
    }

    /** Crea filas sinteticas con identificador unico en la primera columna. */
    private static List<Object[]> filas(int cantidad, String prefijo, String[] columnas) {
        List<Object[]> filas = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            Object[] fila = new Object[columnas.length];
            fila[0] = prefijo + String.format("%03d", i);
            for (int c = 1; c < columnas.length; c++) {
                fila[c] = (c == 1) ? ("Equipo " + (i % 7)) : String.valueOf((i * c) % 5);
            }
            filas.add(fila);
        }
        return filas;
    }

    /** Extrae los identificadores de fila dibujados en una pagina. */
    private static List<String> identificadores(List<String[]> dibujos, String prefijo) {
        List<String> ids = new ArrayList<>();
        for (String[] d : dibujos) {
            if (d[0].startsWith(prefijo)) {
                ids.add(d[0]);
            }
        }
        return ids;
    }

    /**
     * Renderiza todas las paginas y devuelve los identificadores de fila en el orden en que
     * fueron marcados.
     */
    private static List<String> identificadoresEnTodasLasPaginas(
            String[] columnas, int cantidad, String prefijo, PageFormat pf, List<Integer> porPagina) {
        Grabador grabador = new Grabador("Reporte de prueba", columnas, filas(cantidad, prefijo, columnas),
                "12/11/2026 12:00");
        List<String> todos = new ArrayList<>();
        int pagina = 0;
        while (true) {
            grabador.dibujos.clear();
            BufferedImage img = new BufferedImage(595, 842, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 595, 842);
            int res;
            try {
                res = grabador.print(g, pf, pagina);
            } catch (java.awt.print.PrinterException e) {
                throw new IllegalStateException(e);
            } finally {
                g.dispose();
            }
            if (res != java.awt.print.Printable.PAGE_EXISTS) {
                break;
            }
            List<String> ids = identificadores(grabador.dibujos, prefijo);
            porPagina.add(ids.size());
            todos.addAll(ids);
            pagina++;
            if (pagina > 200) {
                throw new IllegalStateException("Paginacion sin fin.");
            }
        }
        return todos;
    }

    // ==================================================================
    // 1) Formato de pagina
    // ==================================================================

    @Test
    @DisplayName("El formato de produccion es A4 vertical (portrait)")
    void testFormatoA4Portrait() {
        PageFormat pf = FrmReportes.crearFormatoPortraitA4();
        assertNotNull(pf);
        assertEquals(PageFormat.PORTRAIT, pf.getOrientation(), "La orientacion debe ser PORTRAIT.");
        assertTrue(pf.getImageableWidth() < pf.getImageableHeight(),
                "El area imprimible debe ser mas alta que ancha (vertical).");
        assertEquals(595.2755813598633, pf.getPaper().getWidth(), 0.01, "Papel A4: 595,27 pt de ancho.");
        assertEquals(841.8897399902344, pf.getPaper().getHeight(), 0.01, "Papel A4: 841,89 pt de alto.");
        assertTrue(pf.getImageableWidth() > 500 && pf.getImageableWidth() < 560,
                "Ancho imprimible esperado cerca de 539 pt, obtenido " + pf.getImageableWidth());
        assertTrue(pf.getImageableHeight() > 750 && pf.getImageableHeight() < 810,
                "Alto imprimible esperado cerca de 786 pt, obtenido " + pf.getImageableHeight());
    }

    @Test
    @DisplayName("El formato de produccion no es el apaisado anterior (regresion de orientacion)")
    void testFormatoNoEsLandscape() {
        PageFormat pf = FrmReportes.crearFormatoPortraitA4();
        assertTrue(pf.getImageableWidth() < pf.getImageableHeight(),
                "El area imprimible no puede ser apaisada: el informe anterior usaba 786 x 539 pt.");
    }

    // ==================================================================
    // 2) Integridad de la paginacion (DEF-017)
    // ==================================================================

    @Test
    @DisplayName("DEF-017: Ninguna fila se pierde ni se duplica en el historial por jugador")
    void testNingunaFilaSePierde() {
        PageFormat pf = FrmReportes.crearFormatoPortraitA4();
        List<Integer> porPagina = new ArrayList<>();
        List<String> ids = identificadoresEnTodasLasPaginas(
                COLUMNAS_JUGADORES, 60, "ROW-", pf, porPagina);

        List<String> esperados = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            esperados.add(String.format("ROW-%03d", i));
        }
        assertEquals(esperados, ids,
                "Las 60 filas deben aparecer exactamente una vez y en orden. Reparto por pagina: " + porPagina);
        assertEquals(60, ids.size());
        assertEquals(60, new LinkedHashSet<>(ids).size(), "No debe haber identificadores repetidos.");
    }

    @Test
    @DisplayName("DEF-017: 100 filas se imprimen completas y en orden")
    void testCienFilasCompletas() {
        PageFormat pf = FrmReportes.crearFormatoPortraitA4();
        List<Integer> porPagina = new ArrayList<>();
        List<String> ids = identificadoresEnTodasLasPaginas(
                COLUMNAS_JUGADORES, 100, "ROW-", pf, porPagina);
        assertEquals(100, ids.size());
        assertEquals(100, new LinkedHashSet<>(ids).size());
        for (int i = 0; i < 100; i++) {
            assertEquals(String.format("ROW-%03d", i), ids.get(i), "Orden de filas alterado en la posicion " + i);
        }
        assertTrue(porPagina.size() >= 2, "Con 100 filas debe haber varias paginas: " + porPagina);
    }

    @Test
    @DisplayName("DEF-017: La capacidad por pagina se determina de forma consistente (limites de pagina)")
    void testLimitesDePagina() {
        PageFormat pf = FrmReportes.crearFormatoPortraitA4();

        // Capacidad empirica de la primera pagina y de las siguientes.
        List<Integer> porPagina = new ArrayList<>();
        identificadoresEnTodasLasPaginas(COLUMNAS_JUGADORES, 200, "ROW-", pf, porPagina);
        assertTrue(porPagina.size() >= 4, "Se esperaban al menos 4 paginas: " + porPagina);
        int capacidadPrimera = porPagina.get(0);
        int capacidadSiguiente = porPagina.get(1);
        assertTrue(capacidadPrimera > 0 && capacidadSiguiente > 0);

        // Casos justo por debajo, en y por encima de los limites.
        int[] casos = {
                0, 1, 5,
                capacidadPrimera - 1, capacidadPrimera, capacidadPrimera + 1,
                capacidadPrimera + capacidadSiguiente - 1,
                capacidadPrimera + capacidadSiguiente,
                capacidadPrimera + capacidadSiguiente + 1
        };
        for (int n : casos) {
            if (n < 0) {
                continue;
            }
            List<Integer> reparto = new ArrayList<>();
            List<String> ids = identificadoresEnTodasLasPaginas(
                    COLUMNAS_JUGADORES, n, "ROW-", pf, reparto);
            assertEquals(n, ids.size(), "Con " + n + " filas deben imprimirse " + n + " (reparto: " + reparto + ")");
            assertEquals(n, new LinkedHashSet<>(ids).size(),
                    "Con " + n + " filas no debe haber repeticiones (reparto: " + reparto + ")");
            for (int i = 0; i < n; i++) {
                assertEquals(String.format("ROW-%03d", i), ids.get(i),
                        "Con " + n + " filas el orden se altero en la posicion " + i);
            }
            if (n > 0) {
                assertTrue(reparto.size() >= 1);
                for (int c : reparto) {
                    assertTrue(c > 0, "No debe haber paginas vacias de datos (reparto: " + reparto + ")");
                }
            }
        }
    }

    @Test
    @DisplayName("DEF-017: Un reporte vacio genera una sola pagina con el aviso")
    void testReporteVacio() {
        PageFormat pf = FrmReportes.crearFormatoPortraitA4();
        Grabador grabador = new Grabador("Reporte vacio", COLUMNAS_POSICIONES, new ArrayList<>(),
                "12/11/2026 12:00");
        List<String[]> dibujos = renderizar(grabador, pf, 0);
        boolean aviso = dibujos.stream().anyMatch(d -> d[0].contains("No hay datos disponibles"));
        assertTrue(aviso, "Debe mostrarse el aviso de reporte sin datos.");
        assertEquals(0, identificadores(dibujos, "ROW-").size());
    }

    @Test
    @DisplayName("DEF-017: Solicitar las paginas fuera de orden produce el mismo contenido")
    void testPaginasFueraDeOrden() {
        PageFormat pf = FrmReportes.crearFormatoPortraitA4();
        List<Object[]> datos = filas(100, "ROW-", COLUMNAS_JUGADORES);

        // Cantidad de paginas del reporte.
        List<Integer> porPagina = new ArrayList<>();
        identificadoresEnTodasLasPaginas(COLUMNAS_JUGADORES, 100, "ROW-", pf, porPagina);
        int totalPaginas = porPagina.size();
        assertTrue(totalPaginas >= 2, "Se esperaban varias paginas: " + porPagina);

        // Contenido de referencia: paginas en orden natural.
        List<List<String>> referencia = new ArrayList<>();
        for (int p = 0; p < totalPaginas; p++) {
            Grabador g = new Grabador("Reporte", COLUMNAS_JUGADORES, datos, "12/11/2026 12:00");
            referencia.add(identificadores(renderizar(g, pf, p), "ROW-"));
        }

        // Mismo reporte, paginas solicitadas en orden invertido: la ultima primero.
        for (int p = totalPaginas - 1; p >= 0; p--) {
            Grabador g = new Grabador("Reporte", COLUMNAS_JUGADORES, datos, "12/11/2026 12:00");
            List<String> ids = identificadores(renderizar(g, pf, p), "ROW-");
            assertEquals(referencia.get(p), ids,
                    "La pagina " + p + " debe ser identica con independencia del orden de solicitud.");
        }
    }

    @Test
    @DisplayName("DEF-017: La paginacion es reproducible entre ejecuciones")
    void testPaginacionReproducible() {
        PageFormat pf = FrmReportes.crearFormatoPortraitA4();
        List<Integer> primera = new ArrayList<>();
        List<Integer> segunda = new ArrayList<>();
        identificadoresEnTodasLasPaginas(COLUMNAS_JUGADORES, 100, "ROW-", pf, primera);
        identificadoresEnTodasLasPaginas(COLUMNAS_JUGADORES, 100, "ROW-", pf, segunda);
        assertEquals(primera, segunda, "Dos ejecuciones deben producir el mismo reparto de filas.");
    }

    // ==================================================================
    // 3) Integridad del contenido (nada se recorta)
    // ==================================================================

    @Test
    @DisplayName("A4 Portrait: Ningun valor se recorta: el texto largo se reparte en lineas completas")
    void testNingunValorSeRecorta() {
        PageFormat pf = FrmReportes.crearFormatoPortraitA4();
        String nombreLargo = "Club Atletico Dewey Wonders ninety characters nombre para forzar el ajuste de linea";
        List<Object[]> filas = new ArrayList<>();
        Object[] fila = new Object[COLUMNAS_POSICIONES.length];
        fila[0] = nombreLargo;
        for (int c = 1; c < COLUMNAS_POSICIONES.length; c++) {
            fila[c] = String.valueOf(c);
        }
        filas.add(fila);

        Grabador grabador = new Grabador("Reporte", COLUMNAS_POSICIONES, filas, "12/11/2026 12:00");
        List<String[]> dibujos = renderizar(grabador, pf, 0);

        StringBuilder reconstruido = new StringBuilder();
        boolean encontro = false;
        for (String[] d : dibujos) {
            if (d[0].contains("Club") || d[0].contains("ninety") || d[0].contains("linea")
                    || d[0].contains("Atletico") || d[0].contains("caracters") || d[0].contains("ajuste")) {
                reconstruido.append(d[0]);
                encontro = true;
            }
        }
        assertTrue(encontro, "El nombre largo debe dibujarse en la pagina.");
        String normalizado = reconstruido.toString().replaceAll("\\s+", "");
        String esperado = nombreLargo.replaceAll("\\s+", "");
        assertEquals(esperado, normalizado,
                "El texto debe aparecer completo: al unir las lineas no puede faltar ningun caracter.");
    }

    @Test
    @DisplayName("A4 Portrait: Los encabezados largos se reparten en varias lineas, no se truncan")
    void testEncabezadosNoSeTruncan() {
        PageFormat pf = FrmReportes.crearFormatoPortraitA4();
        Grabador grabador = new Grabador("Reporte", COLUMNAS_JUGADORES, new ArrayList<>(), "12/11/2026 12:00");
        List<String[]> dibujos = renderizar(grabador, pf, 0);
        StringBuilder encabezado = new StringBuilder();
        for (String[] d : dibujos) {
            if (d[0].contains("Tarjetas") || d[0].contains("Sanciones") || d[0].contains("Jugador")
                    || d[0].contains("amarillas") || d[0].contains("rojas") || d[0].contains("vigentes")
                    || d[0].contains("Goles") || d[0].contains("Equipo")) {
                encabezado.append(d[0]);
            }
        }
        String normalizado = encabezado.toString().replaceAll("\\s+", "");
        assertTrue(normalizado.contains("Tarjetasamarillas"), "Debe aparecer el encabezado completo: " + normalizado);
        assertTrue(normalizado.contains("Tarjetasrojas"), "Debe aparecer el encabezado completo: " + normalizado);
        assertTrue(normalizado.contains("Sancionesvigentes"), "Debe aparecer el encabezado completo: " + normalizado);
        assertTrue(!normalizado.contains("..."), "No debe usarse elipsis: " + normalizado);
    }

    @Test
    @DisplayName("A4 Portrait: Todas las columnas del reporte caben en el ancho imprimible")
    void testColumnasDentroDelAncho() {
        PageFormat pf = FrmReportes.crearFormatoPortraitA4();
        List<Object[]> filas = filas(5, "ROW-", COLUMNAS_POSICIONES);
        Grabador grabador = new Grabador("Reporte", COLUMNAS_POSICIONES, filas, "12/11/2026 12:00");
        List<String[]> dibujos = renderizar(grabador, pf, 0);
        double ancho = pf.getImageableWidth();
        double maxX = 0;
        for (String[] d : dibujos) {
            maxX = Math.max(maxX, Double.parseDouble(d[1]));
        }
        assertTrue(maxX <= pf.getImageableX() + ancho,
                "Ninguna columna debe comenzar fuera del area imprimible (maxX=" + maxX + ").");
    }

    @Test
    @DisplayName("A4 Portrait: El encabezado de la tabla se repite en cada pagina")
    void testEncabezadoSeRepite() {
        PageFormat pf = FrmReportes.crearFormatoPortraitA4();
        for (int pagina = 0; pagina < 3; pagina++) {
            Grabador g = new Grabador("Reporte", COLUMNAS_JUGADORES, filas(100, "ROW-", COLUMNAS_JUGADORES),
                    "12/11/2026 12:00");
            List<String[]> dibujos = renderizar(g, pf, pagina);
            boolean conColumna = dibujos.stream().anyMatch(d -> d[0].startsWith("ROW-"));
            boolean conEncabezado = dibujos.stream().anyMatch(d -> d[0].contains("Sanciones")
                    || d[0].contains("vigentes") || d[0].contains("Jugador") || d[0].contains("Goles"));
            assertTrue(conColumna, "La pagina " + pagina + " debe contener filas de datos.");
            assertTrue(conEncabezado, "La pagina " + pagina + " debe repetir el encabezado de la tabla.");
        }
    }

    @Test
    @DisplayName("A4 Portrait: El pie de pagina indica la pagina actual y el total")
    void testPieDePagina() {
        PageFormat pf = FrmReportes.crearFormatoPortraitA4();
        Grabador g = new Grabador("Reporte", COLUMNAS_JUGADORES, filas(100, "ROW-", COLUMNAS_JUGADORES),
                "12/11/2026 12:00");
        List<String[]> dibujos = renderizar(g, pf, 1);
        boolean conPie = dibujos.stream().anyMatch(d -> d[0].startsWith("Pagina ") && d[0].contains(" de "));
        assertTrue(conPie, "El pie debe indicar pagina actual y total: " + dibujos);
    }

    @Test
    @DisplayName("A4 Portrait: El titulo solo aparece en la primera pagina")
    void testTituloSoloEnPrimeraPagina() {
        PageFormat pf = FrmReportes.crearFormatoPortraitA4();
        List<Object[]> filas = filas(100, "ROW-", COLUMNAS_JUGADORES);
        Grabador primera = new Grabador("Reporte de jugadores (CU-20)", COLUMNAS_JUGADORES, filas,
                "12/11/2026 12:00");
        List<String[]> d0 = renderizar(primera, pf, 0);
        assertTrue(d0.stream().anyMatch(d -> d[0].contains("Reporte de jugadores")),
                "La primera pagina debe mostrar el titulo.");

        Grabador segunda = new Grabador("Reporte de jugadores (CU-20)", COLUMNAS_JUGADORES, filas,
                "12/11/2026 12:00");
        List<String[]> d1 = renderizar(segunda, pf, 1);
        assertTrue(d1.stream().noneMatch(d -> d[0].contains("Reporte de jugadores")),
                "El titulo no debe repetirse en la segunda pagina.");
    }

    @Test
    @DisplayName("DEF-017: La tabla de posiciones tambien pagina sin perder equipos")
    void testTablaDePosicionesSinPerderEquipos() {
        PageFormat pf = FrmReportes.crearFormatoPortraitA4();
        List<Integer> porPagina = new ArrayList<>();
        List<String> ids = identificadoresEnTodasLasPaginas(COLUMNAS_POSICIONES, 45, "ROW-", pf, porPagina);
        assertEquals(45, ids.size());
        assertEquals(45, new LinkedHashSet<>(ids).size());
        for (int i = 0; i < 45; i++) {
            assertEquals(String.format("ROW-%03d", i), ids.get(i));
        }
    }

    @Test
    @DisplayName("DEF-017: Ninguna pagina queda vacia de datos cuando el reporte tiene filas")
    void testNingunaPaginaVacia() {
        PageFormat pf = FrmReportes.crearFormatoPortraitA4();
        List<Integer> porPagina = new ArrayList<>();
        identificadoresEnTodasLasPaginas(COLUMNAS_JUGADORES, 100, "ROW-", pf, porPagina);
        for (int c : porPagina) {
            assertTrue(c > 0, "Se encontro una pagina sin filas: " + porPagina);
        }
    }

    @Test
    @DisplayName("A4 Portrait: Una fila con texto muy largo sigue apareciendo completa en su pagina")
    void testFilaMuyLarga() {
        PageFormat pf = FrmReportes.crearFormatoPortraitA4();
        StringBuilder largo = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            largo.append("PALABRA").append(i).append(' ');
        }
        List<Object[]> filas = new ArrayList<>();
        Object[] fila = new Object[COLUMNAS_POSICIONES.length];
        fila[0] = largo.toString().trim();
        for (int c = 1; c < COLUMNAS_POSICIONES.length; c++) {
            fila[c] = "1";
        }
        filas.add(fila);

        Grabador grabador = new Grabador("Reporte", COLUMNAS_POSICIONES, filas, "12/11/2026 12:00");
        List<String[]> dibujos = renderizar(grabador, pf, 0);
        StringBuilder reconstruido = new StringBuilder();
        for (String[] d : dibujos) {
            if (d[0].contains("PALABRA")) {
                reconstruido.append(d[0]);
            }
        }
        assertEquals(largo.toString().trim().replaceAll("\\s+", ""),
                reconstruido.toString().replaceAll("\\s+", ""),
                "Ningun caracter puede perderse en una celda muy larga.");
        assertTrue(Arrays.asList("PALABRA0").contains("PALABRA0"));
    }
}
