package com.gestionamateur.tpi.progav.vista;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DEF-015: regresion de la simbologia Code 39 de las credenciales.
 *
 * <p>La verificacion NO utiliza el generador auditado como referencia. El patron canonico
 * esperado se CALCULA en esta clase a partir del algoritmo de la simbologia
 * (ISO/IEC 16388:2023), de modo que no existe ninguna tabla transcrita susceptible de error:
 *
 * <ul>
 *   <li>9 elementos por caracter: B1 S1 B2 S2 B3 S3 B4 S4 B5 (5 barras y 4 espacios).</li>
 *   <li>3 elementos anchos (3 modulos) y 6 estrechos (1 modulo).</li>
 *   <li>Las 2 barras anchas codifican el valor 1..10 por el codigo dos-de-cinco con los
 *       pesos [1,2,4,7,0] sobre las posiciones 1..5: valor = peso(p1) + peso(p2), con la
 *       excepcion 4+7 = 11, que se asigna a 10.</li>
 *   <li>El espacio ancho (1 de 4) selecciona el grupo: posicion 1 -&gt; Letras(+30),
 *       2 -&gt; Digitos(+0), 3 -&gt; Letras(+10), 4 -&gt; Letras(+20).</li>
 *   <li>Valor de checksum: digitos = valor de barras; letras = valor de barras + offset - 1.</li>
 *   <li>Los 4 caracteres finales ($, /, +, %) tienen las 5 barras estrechas y 3 espacios anchos,
 *       con el espacio estrecho en una posicion distinta cada uno.</li>
 * </ul>
 *
 * <p>El render se decodifica midiendo la anchura de cada barra y espacio, en un decodificador
 * independiente del generador auditado.
 */
@DisplayName("DEF-015: Simbologia Code 39 de las credenciales")
public class CodigoBarrasCode39Test {

    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%*";
    private static final int[] PESOS = {1, 2, 4, 7, 0};

    private static final int MARGEN = 20;
    private static final int ANCHO = 1200;
    private static final int ALTO = 60;
    private static final int ALTO_BARRAS = 40;

    /** Segmento contiguo de un mismo color sobre la fila muestreada. */
    private static final class Run {
        final int largo;
        final boolean negro;

        Run(int largo, boolean negro) {
            this.largo = largo;
            this.negro = negro;
        }
    }

    // ------------------------------------------------------------------
    // 1) Referencia canonica calculada por el algoritmo de la simbologia
    // ------------------------------------------------------------------

    /** Patron canonico de un caracter, calculado a partir del algoritmo de Code 39. */
    private static String patronCanonico(char c) {
        if (c >= '0' && c <= '9') {
            int valorBarras = (c == '0') ? 10 : (c - '0');
            return patron(barrasDeValor(valorBarras), espaciosDePosicion(2));
        }
        if (c >= 'A' && c <= 'Z') {
            int i = c - 'A';
            int espPos;
            if (i < 10) {
                espPos = 3;
            } else if (i < 20) {
                espPos = 4;
            } else {
                espPos = 1;
            }
            int valorBarras = (i % 10) + 1;
            return patron(barrasDeValor(valorBarras), espaciosDePosicion(espPos));
        }
        switch (c) {
            case '-': return patron(barrasDeValor(7), espaciosDePosicion(1));
            case '.': return patron(barrasDeValor(8), espaciosDePosicion(1));
            case ' ': return patron(barrasDeValor(9), espaciosDePosicion(1));
            case '*': return patron(barrasDeValor(10), espaciosDePosicion(1));
            default: break;
        }
        // Los 4 finales: 5 barras estrechas y 3 espacios anchos (1 espacio estrecho).
        int posEstrecho;
        switch (c) {
            case '$': posEstrecho = 1; break;
            case '/': posEstrecho = 2; break;
            case '+': posEstrecho = 3; break;
            case '%': posEstrecho = 4; break;
            default: throw new IllegalArgumentException("Caracter no codificable: " + c);
        }
        boolean[] espacios = {true, true, true, true};
        espacios[posEstrecho - 1] = false;
        return patron(new boolean[]{false, false, false, false, false}, espacios);
    }

    /** Par de posiciones de barra anchas que codifican el valor 1..10. */
    private static int[] parDeBarras(int valor) {
        for (int p1 = 1; p1 <= 5; p1++) {
            for (int p2 = p1 + 1; p2 <= 5; p2++) {
                int suma = PESOS[p1 - 1] + PESOS[p2 - 1];
                int v = (suma == 11) ? 10 : suma;
                if (v == valor) {
                    return new int[]{p1, p2};
                }
            }
        }
        throw new IllegalArgumentException("Valor de barras fuera de rango: " + valor);
    }

    private static boolean[] barrasDeValor(int valor) {
        boolean[] barras = {false, false, false, false, false};
        for (int p : parDeBarras(valor)) {
            barras[p - 1] = true;
        }
        return barras;
    }

    private static boolean[] espaciosDePosicion(int posicion) {
        boolean[] espacios = {false, false, false, false};
        espacios[posicion - 1] = true;
        return espacios;
    }

    private static String patron(boolean[] barras, boolean[] espacios) {
        StringBuilder e = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            e.append(barras[i] ? 'w' : 'n');
            if (i < 4) {
                e.append(espacios[i] ? 'w' : 'n');
            }
        }
        return e.toString();
    }

    // ------------------------------------------------------------------
    // 2) Verificaciones de la referencia canonica
    // ------------------------------------------------------------------

    @Test
    @DisplayName("La referencia canonica calculada tiene 44 patrones distintos y 3 elementos anchos cada uno")
    void testInvariantesDeLaReferenciaCanonica() {
        Set<String> distintos = new LinkedHashSet<>();
        for (int i = 0; i < CHARS.length(); i++) {
            char c = CHARS.charAt(i);
            String p = patronCanonico(c);
            assertEquals(9, p.length(), "El patron de '" + c + "' debe tener 9 elementos.");
            int barras = 0;
            int espacios = 0;
            int anchos = 0;
            for (int k = 0; k < 9; k++) {
                if (k % 2 == 0) {
                    barras++;
                } else {
                    espacios++;
                }
                if (p.charAt(k) == 'w') {
                    anchos++;
                }
            }
            assertEquals(5, barras, "El patron de '" + c + "' debe tener 5 barras.");
            assertEquals(4, espacios, "El patron de '" + c + "' debe tener 4 espacios.");
            assertEquals(3, anchos, "El patron de '" + c + "' debe tener 3 elementos anchos.");
            distintos.add(p);
        }
        assertEquals(44, distintos.size(), "Los 44 patrones canonicos deben ser distintos entre si.");
    }

    /**
     * Valida el algoritmo contra los valores de checksum publicados por la especificacion
     * (0..42 para los 43 caracteres codificables; el marcador '*' no tiene valor).
     */
    @Test
    @DisplayName("Los 43 valores de checksum publicados se reconstruyen desde el patron canonico")
    void testValoresDeChecksumPublicados() {
        java.util.Map<Character, Integer> publicados = new java.util.HashMap<>();
        for (int i = 0; i <= 9; i++) {
            publicados.put((char) ('0' + i), i);
        }
        for (int i = 0; i < 26; i++) {
            publicados.put((char) ('A' + i), 10 + i);
        }
        publicados.put('-', 36);
        publicados.put('.', 37);
        publicados.put(' ', 38);
        publicados.put('$', 39);
        publicados.put('/', 40);
        publicados.put('+', 41);
        publicados.put('%', 42);

        for (java.util.Map.Entry<Character, Integer> e : publicados.entrySet()) {
            char c = e.getKey();
            String p = patronCanonico(c);
            boolean[] barras = new boolean[5];
            boolean[] espacios = new boolean[4];
            for (int k = 0; k < 9; k++) {
                if (k % 2 == 0) {
                    barras[k / 2] = (p.charAt(k) == 'w');
                } else {
                    espacios[k / 2] = (p.charAt(k) == 'w');
                }
            }
            int suma = 0;
            int barrasAnchas = 0;
            for (int k = 0; k < 5; k++) {
                if (barras[k]) {
                    suma += PESOS[k];
                    barrasAnchas++;
                }
            }
            int valorBarras = (suma == 11) ? 10 : suma;
            int espaciosAnchos = 0;
            int posicionEspacio = 0;
            for (int k = 0; k < 4; k++) {
                if (espacios[k]) {
                    espaciosAnchos++;
                    posicionEspacio = k + 1;
                }
            }
            int valor;
            if (c >= '0' && c <= '9') {
                assertEquals(1, espaciosAnchos, "El digito '" + c + "' debe tener un unico espacio ancho.");
                valor = (valorBarras == 10) ? 0 : valorBarras;
                assertEquals(2, posicionEspacio, "El digito '" + c + "' debe tener el espacio ancho en la posicion 2.");
            } else if (c == '$' || c == '/' || c == '+' || c == '%') {
                valor = e.getValue();
                assertEquals(0, barrasAnchas, "El caracter final '" + c + "' debe tener las 5 barras estrechas.");
                assertEquals(3, espaciosAnchos, "El caracter final '" + c + "' debe tener 3 espacios anchos.");
            } else {
                assertEquals(1, espaciosAnchos, "El caracter '" + c + "' debe tener un unico espacio ancho.");
                int offset = (c >= 'A' && c <= 'J') ? 10 : ((c >= 'K' && c <= 'T') ? 20 : 30);
                valor = valorBarras + offset - 1;
                int esperada = (offset == 10) ? 3 : ((offset == 20) ? 4 : 1);
                assertEquals(esperada, posicionEspacio,
                        "El caracter '" + c + "' debe tener el espacio ancho en la posicion " + esperada + ".");
            }
            assertEquals(e.getValue().intValue(), valor,
                    "El valor de checksum reconstruido de '" + c + "' no coincide con el publicado.");
        }
    }

    @Test
    @DisplayName("La referencia reproduce los ejemplos textuales de la especificacion")
    void testEjemplosDeLaEspecificacion() {
        // "the letter A ... is WNNNW": barra 1 y barra 5 anchas.
        String a = patronCanonico('A');
        String barrasA = "" + a.charAt(0) + a.charAt(2) + a.charAt(4) + a.charAt(6) + a.charAt(8);
        assertEquals("wnnnw", barrasA, "Las barras de 'A' deben ser WNNNW.");

        // "the number 6 is encoded NWWNN": barras 2 y 3 anchas.
        String seis = patronCanonico('6');
        String barras6 = "" + seis.charAt(0) + seis.charAt(2) + seis.charAt(4) + seis.charAt(6) + seis.charAt(8);
        assertEquals("nwwnn", barras6, "Las barras del valor 6 deben ser NWWNN.");

        // "P ... bars aligned to represent the number 6, and the space in the far right position".
        String p = patronCanonico('P');
        String barrasP = "" + p.charAt(0) + p.charAt(2) + p.charAt(4) + p.charAt(6) + p.charAt(8);
        assertEquals("nwwnn", barrasP, "Las barras de 'P' deben representar el numero 6.");
        assertEquals('w', p.charAt(7), "El espacio ancho de 'P' debe estar en la posicion mas a la derecha.");
    }

    // ------------------------------------------------------------------
    // 3) El render del generador coincide con la referencia canonica
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Cada caracter se renderiza con el patron canonico de Code 39")
    void testCadaCaracterUsaElPatronCanonico() {
        for (int i = 0; i < CHARS.length(); i++) {
            char c = CHARS.charAt(i);
            String medido = medirPatronDe(c);
            assertEquals(patronCanonico(c), medido,
                    "El patron renderizado del caracter '" + c + "' no coincide con el canonico de Code 39.");
        }
    }

    @Test
    @DisplayName("El render se decodifica exactamente con el contenido codificado")
    void testDecodificacionIdempotenteDelRender() {
        String[] contenidos = {
            "DNI47811685", "DNI12345678", "DNI10000000", "DNI99999999", "DNI67000000",
            "DNI87654321", "DNI00000000", "DNI1234-6789", "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
            "0123456789", "DNI11111111", "DNI78901234"
        };
        for (String contenido : contenidos) {
            assertEquals(contenido, decodificar(contenido),
                    "El codigo de barras de '" + contenido + "' no se decodifica como tal.");
        }
    }

    @Test
    @DisplayName("Regresion del caso historico: el DNI 47811685 ya no se lee como 44511655")
    void testRegresionCasoHistorico() {
        String decodificado = decodificar("DNI47811685");
        assertEquals("DNI47811685", decodificado);
        assertTrue(!"DNI44511655".equals(decodificado),
                "El codigo sigue leyendose con un DNI equivocado.");
    }

    @Test
    @DisplayName("La relacion de anchuras es de 3 modulos (ancho) contra 1 modulo (estrecho)")
    void testRelacionDeAnchuras() {
        for (int i = 0; i < CHARS.length(); i++) {
            char c = CHARS.charAt(i);
            String esperado = patronCanonico(c);
            List<Integer> anchos = medirAnchosDe(c);
            int unidad = Integer.MAX_VALUE;
            for (int k = 0; k < 9; k++) {
                if (esperado.charAt(k) == 'n') {
                    unidad = Math.min(unidad, anchos.get(k));
                }
            }
            for (int k = 0; k < 9; k++) {
                int objetivo = (esperado.charAt(k) == 'w') ? unidad * 3 : unidad;
                assertTrue(Math.abs(anchos.get(k) - objetivo) <= 1,
                        "Elemento " + k + " del caracter '" + c + "' mide " + anchos.get(k)
                                + " px y deberia medir " + objetivo + " px (unidad " + unidad + ").");
            }
        }
    }

    @Test
    @DisplayName("El total de modulos se calcula con los modulos reales de cada patron")
    void testCalculoDeModulos() {
        String contenido = "DNI47811685";
        String conMarcadores = "*" + contenido + "*";
        int esperado = 0;
        for (char c : conMarcadores.toCharArray()) {
            String p = patronCanonico(c);
            for (int k = 0; k < p.length(); k++) {
                esperado += (p.charAt(k) == 'w') ? 3 : 1;
            }
        }
        esperado += conMarcadores.length() - 1;
        assertEquals(esperado, GeneradorCodigoBarras.modulosTotales(contenido),
                "El total de modulos debe sumar los modulos reales de cada patron mas los separadores.");
    }

    @Test
    @DisplayName("Solo se aceptan caracteres codificables en Code 39")
    void testAdmisibilidad() {
        assertTrue(GeneradorCodigoBarras.esAdmisible("DNI12345678"));
        assertTrue(GeneradorCodigoBarras.esAdmisible("ABC-.$/+%"));
        assertTrue(!GeneradorCodigoBarras.esAdmisible("ñ"));
        assertTrue(!GeneradorCodigoBarras.esAdmisible(""));
        assertTrue(!GeneradorCodigoBarras.esAdmisible(null));
        assertNotNull(GeneradorCodigoBarras.patronDe('A'));
        assertEquals(null, GeneradorCodigoBarras.patronDe('ñ'));
    }

    // ------------------------------------------------------------------
    // Utilidades de medicion sobre el render
    // ------------------------------------------------------------------

    /** Renderiza el contenido y devuelve el patron medido de cada caracter (marcadores excluidos). */
    private static List<String> medirPatrones(String contenido) {
        List<Run> runs = runsDelRender(contenido);
        int inicio = 0;
        while (inicio < runs.size() && !runs.get(inicio).negro) {
            inicio++;
        }
        if (inicio >= runs.size()) {
            return new ArrayList<>();
        }
        int unidad = Integer.MAX_VALUE;
        for (int k = inicio; k < runs.size(); k++) {
            unidad = Math.min(unidad, runs.get(k).largo);
        }
        if (unidad <= 0) {
            return new ArrayList<>();
        }
        List<String> patrones = new ArrayList<>();
        int i = inicio;
        while (i + 9 <= runs.size()) {
            StringBuilder p = new StringBuilder();
            for (int k = 0; k < 9; k++) {
                p.append(runs.get(i + k).largo >= unidad * 2 ? 'w' : 'n');
            }
            String patron = p.toString();
            if (indiceDePatron(patron) < 0) {
                break;
            }
            i += 10; // 9 elementos + 1 espacio separador entre caracteres
            if (esMarcador(patron)) {
                // Los marcadores de inicio y fin no forman parte del contenido.
                continue;
            }
            patrones.add(patron);
        }
        return patrones;
    }

    /** El patron '*' (marcador) tiene la forma canonica del caracter '*'. */
    private static boolean esMarcador(String patron) {
        return patron.equals(patronCanonico('*'));
    }

    /** Patron medido del caracter aislado (el segundo del simbolo, entre los dos marcadores). */
    private static String medirPatronDe(char c) {
        List<String> patrones = medirPatrones(String.valueOf(c));
        // El simbolo es: marcador + caracter + marcador.
        List<String> todos = medirTodosLosPatrones(String.valueOf(c));
        assertEquals(3, todos.size(),
                "Se esperaban el marcador, el caracter y el marcador final para '" + c + "' y se obtuvieron "
                        + todos.size());
        return todos.get(1);
    }

    /** Todos los patrones medidos del render, incluidos los marcadores. */
    private static List<String> medirTodosLosPatrones(String contenido) {
        List<Run> runs = runsDelRender(contenido);
        int inicio = 0;
        while (inicio < runs.size() && !runs.get(inicio).negro) {
            inicio++;
        }
        List<String> patrones = new ArrayList<>();
        if (inicio >= runs.size()) {
            return patrones;
        }
        int unidad = Integer.MAX_VALUE;
        for (int k = inicio; k < runs.size(); k++) {
            unidad = Math.min(unidad, runs.get(k).largo);
        }
        int i = inicio;
        while (i + 9 <= runs.size()) {
            StringBuilder p = new StringBuilder();
            for (int k = 0; k < 9; k++) {
                p.append(runs.get(i + k).largo >= unidad * 2 ? 'w' : 'n');
            }
            String patron = p.toString();
            if (indiceDePatron(patron) < 0) {
                break;
            }
            patrones.add(patron);
            i += 10;
        }
        return patrones;
    }

    /** Anchos en pixeles de los 9 elementos del caracter medido. */
    private static List<Integer> medirAnchosDe(char c) {
        List<String> todos = medirTodosLosPatrones(String.valueOf(c));
        List<Run> runs = runsDelRender(String.valueOf(c));
        int inicio = 0;
        while (inicio < runs.size() && !runs.get(inicio).negro) {
            inicio++;
        }
        List<Integer> elementos = new ArrayList<>();
        // El caracter es el segundo simbolo: 10 runs del primero + 9 del segundo.
        int base = inicio + 10;
        for (int k = 0; k < 9 && base + k < runs.size(); k++) {
            elementos.add(runs.get(base + k).largo);
        }
        assertEquals(9, elementos.size(), "No se pudieron medir los 9 elementos del caracter '" + c + "'.");
        return elementos;
    }

    /** Decodifica el render completo de un contenido midiendo barras y espacios. */
    private static String decodificar(String contenido) {
        StringBuilder salida = new StringBuilder();
        for (String patron : medirPatrones(contenido)) {
            char c = caracterDePatron(patron);
            if (c != 0) {
                salida.append(c);
            }
        }
        return salida.toString();
    }

    /** Caracter canonico correspondiente a un patron medido, o 0 si no existe. */
    private static char caracterDePatron(String patron) {
        for (int i = 0; i < CHARS.length(); i++) {
            if (patronCanonico(CHARS.charAt(i)).equals(patron)) {
                return CHARS.charAt(i);
            }
        }
        return 0;
    }

    private static int indiceDePatron(String patron) {
        for (int i = 0; i < CHARS.length(); i++) {
            if (patronCanonico(CHARS.charAt(i)).equals(patron)) {
                return i;
            }
        }
        return -1;
    }

    /** Segmentos de color alternante sobre la fila media del simbolo. */
    private static List<Run> runsDelRender(String contenido) {
        BufferedImage img = new BufferedImage(ANCHO, ALTO, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, ANCHO, ALTO);
        GeneradorCodigoBarras.dibujar(g, contenido, MARGEN, 5, ANCHO - 2 * MARGEN, ALTO_BARRAS);
        g.dispose();

        int y = 5 + ALTO_BARRAS / 2;
        int x0 = MARGEN;
        int ancho = ANCHO - 2 * MARGEN;
        List<Run> runs = new ArrayList<>();
        Boolean anterior = null;
        int inicio = 0;
        for (int i = 0; i < ancho; i++) {
            int rgb = img.getRGB(x0 + i, y);
            int r = (rgb >> 16) & 0xFF, gg = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
            boolean negro = (r + gg + b) / 3 < 128;
            if (anterior == null) {
                anterior = negro;
                inicio = i;
                continue;
            }
            if (negro != anterior) {
                runs.add(new Run(i - inicio, anterior));
                anterior = negro;
                inicio = i;
            }
        }
        runs.add(new Run(ancho - inicio, anterior != null && anterior));
        return runs;
    }
}
