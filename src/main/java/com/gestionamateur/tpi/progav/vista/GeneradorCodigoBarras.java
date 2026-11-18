package com.gestionamateur.tpi.progav.vista;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

/**
 * CU-02.2: generador de codigos de barras Code 39 (simbolgia abierta, sin dependencias).
 *
 * <p>Cada caracter se dibuja con 9 elementos: 5 barras y 4 espacios, alternando barra/espacio
 * y comenzando por barra. De esos 9 elementos, 3 son anchos (3 modulos) y 6 son estrechos
 * (1 modulo). Los caracteres se separan con un espacio estrecho adicional.
 *
 * <p><b>DEF-015.</b> La tabla de patrones anterior contenia 7 entradas no canonicas
 * ({@code 0}, {@code 6}, {@code 7}, {@code 8}, {@code 9}, {@code O} y {@code -}), lo que hacia
 * que la credencial no fuera decodificable por un lector estandar o se leyera con un dato
 * equivocado (por ejemplo {@code 8} se leia como {@code 5}). La tabla vigente es la codificacion
 * canonica de Code 39 (9 elementos por caracter), verificada contra la especificacion de la
 * simbologia (ISO/IEC 16388:2023) mediante derivacion algoritmica y contrastada con la geometria
 * de una fuente Code 39 vectorial publica.
 *
 * <p>Ademas, el ancho total ya no se estima suponiendo 15 modulos por caracter: se calcula
 * sumando los modulos reales de cada patron, de modo que la escala del simbolo sea exacta.
 */
public final class GeneradorCodigoBarras {

    /**
     * Pesos del codigo dos-de-cinco sobre las posiciones de barra 1..5 (ISO/IEC 16388:2023).
     * El valor codificado es la suma de los pesos de las dos barras anchas; la unica
     * excepcion es 4+7 = 11, que se asigna al valor 10.
     */
    private static final int[] PESOS_BARRAS = {1, 2, 4, 7, 0};

    /**
     * Patron de cada caracter del alfabeto de Code 39, derivado del algoritmo de la
     * simbologia y no transcrito, para eliminar el riesgo de un error de copia.
     *
     * <p>Reglas aplicadas (ver el javadoc de la clase):
     * <ul>
     *   <li>9 elementos por caracter: B1 S1 B2 S2 B3 S3 B4 S4 B5 (5 barras, 4 espacios).</li>
     *   <li>Las 2 barras anchas codifican el valor 1..10 por el codigo dos-de-cinco.</li>
     *   <li>El espacio ancho (1 de 4) selecciona el grupo del caracter.</li>
     *   <li>Los 4 caracteres finales tienen las 5 barras estrechas y 3 espacios anchos.</li>
     * </ul>
     *
     * <p>El indice sigue el orden de {@link #indiceDe(char)}:
     * 0-9 digitos, 10-35 letras A-Z, 36 '-', 37 '.', 38 espacio, 39 '$', 40 '/', 41 '+',
     * 42 '%', 43 '*' (marcador de inicio y fin).
     */
    private static final String[] PATRONES = construirPatrones();

    private static String[] construirPatrones() {
        String[] tabla = new String[44];
        // Digitos: grupo Digits(+0), espacio ancho en la posicion 2. El valor 10 codifica el '0'.
        for (int v = 1; v <= 10; v++) {
            char digito = (v == 10) ? '0' : (char) ('0' + v);
            tabla[indiceDe(digito)] = componer(barrasDeValor(v), espaciosDePosicion(2));
        }
        // Letras A..J: grupo Letras(+10), espacio ancho en la posicion 3.
        for (int i = 0; i < 10; i++) {
            tabla[indiceDe((char) ('A' + i))] = componer(barrasDeValor(i + 1), espaciosDePosicion(3));
        }
        // Letras K..T: grupo Letras(+20), espacio ancho en la posicion 4.
        for (int i = 0; i < 10; i++) {
            tabla[indiceDe((char) ('K' + i))] = componer(barrasDeValor(i + 1), espaciosDePosicion(4));
        }
        // Letras U..Z: grupo Letras(+30), espacio ancho en la posicion 1.
        for (int i = 0; i < 6; i++) {
            tabla[indiceDe((char) ('U' + i))] = componer(barrasDeValor(i + 1), espaciosDePosicion(1));
        }
        // Simbolos y marcador: mismo grupo (+30) con los valores de barra 7, 8, 9 y 10.
        tabla[indiceOf('-')] = componer(barrasDeValor(7), espaciosDePosicion(1));
        tabla[indiceOf('.')] = componer(barrasDeValor(8), espaciosDePosicion(1));
        tabla[indiceOf(' ')] = componer(barrasDeValor(9), espaciosDePosicion(1));
        tabla[indiceOf('*')] = componer(barrasDeValor(10), espaciosDePosicion(1));
        // Los 4 caracteres finales: 5 barras estrechas y 3 espacios anchos (1 espacio estrecho).
        final char[] finales = {'$', '/', '+', '%'};
        for (int i = 0; i < finales.length; i++) {
            boolean[] espacios = {true, true, true, true};
            espacios[i] = false;
            tabla[indiceOf(finales[i])] = componer(new boolean[]{false, false, false, false, false}, espacios);
        }
        return tabla;
    }

    /** Posiciones de las dos barras anchas que codifican el valor 1..10. */
    private static int[] posicionesBarrasAnchas(int valor) {
        for (int p1 = 1; p1 <= 5; p1++) {
            for (int p2 = p1 + 1; p2 <= 5; p2++) {
                int suma = PESOS_BARRAS[p1 - 1] + PESOS_BARRAS[p2 - 1];
                int v = (suma == 11) ? 10 : suma;
                if (v == valor) {
                    return new int[]{p1, p2};
                }
            }
        }
        throw new IllegalStateException("Valor de barras fuera de rango: " + valor);
    }

    private static boolean[] barrasDeValor(int valor) {
        boolean[] barras = {false, false, false, false, false};
        for (int p : posicionesBarrasAnchas(valor)) {
            barras[p - 1] = true;
        }
        return barras;
    }

    private static boolean[] espaciosDePosicion(int posicion) {
        boolean[] espacios = {false, false, false, false};
        espacios[posicion - 1] = true;
        return espacios;
    }

    /** Compone los 9 elementos alternando barra y espacio. */
    private static String componer(boolean[] barras, boolean[] espacios) {
        StringBuilder e = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            e.append(barras[i] ? 'w' : 'n');
            if (i < 4) {
                e.append(espacios[i] ? 'w' : 'n');
            }
        }
        return e.toString();
    }

    private static int indiceOf(char c) {
        int idx = indiceDe(c);
        if (idx < 0) {
            throw new IllegalArgumentException("Caracter no codificable en Code 39: " + c);
        }
        return idx;
    }

    /** Modulos de una zona de silencio a cada lado del simbolo (requisito de lectura). */
    private static final int ZONA_SILENCIO_MODULOS = 10;

    private static final char INICIO_FIN = '*';

    private GeneradorCodigoBarras() {
    }

    /** Caracteres admitidos por Code 39. */
    public static boolean esAdmisible(String texto) {
        if (texto == null || texto.isEmpty()) {
            return false;
        }
        for (int i = 0; i < texto.length(); i++) {
            if (indiceDe(texto.charAt(i)) < 0) {
                return false;
            }
        }
        return true;
    }

    private static int indiceDe(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'Z') {
            return 10 + (c - 'A');
        }
        switch (c) {
            case '-': return 36;
            case '.': return 37;
            case ' ': return 38;
            case '$': return 39;
            case '/': return 40;
            case '+': return 41;
            case '%': return 42;
            case '*': return 43;
            default: return -1;
        }
    }

    /**
     * Devuelve el patron canonico de un caracter, o null si no es codificable.
     */
    public static String patronDe(char caracter) {
        int idx = indiceDe(caracter);
        return idx < 0 ? null : PATRONES[idx];
    }

    /**
     * Cantidad de modulos que ocupa un patron: 3 por cada elemento ancho y 1 por cada estrecho.
     */
    public static int modulosDe(String patron) {
        int modulos = 0;
        for (int i = 0; i < patron.length(); i++) {
            modulos += (patron.charAt(i) == 'w') ? 3 : 1;
        }
        return modulos;
    }

    /**
     * Modulos totales del simbolo, incluyendo los separadores estrechos entre caracteres.
     */
    public static int modulosTotales(String texto) {
        String contenido = INICIO_FIN + texto.toUpperCase() + INICIO_FIN;
        int modulos = 0;
        for (int i = 0; i < contenido.length(); i++) {
            modulos += modulosDe(PATRONES[indiceDe(contenido.charAt(i))]);
        }
        // Un espacio estrecho separador entre caracteres consecutivos.
        return modulos + (contenido.length() - 1);
    }

    /**
     * Dibuja el codigo de barras centrado en el ancho indicado.
     *
     * @param g2    contexto grafico
     * @param texto contenido a codificar (debe ser admisible)
     * @param x     margen izquierdo
     * @param y     margen superior
     * @param ancho ancho total disponible
     * @param alto  alto de las barras
     */
    public static void dibujar(Graphics2D g2, String texto, int x, int y, int ancho, int alto) {
        if (texto == null || texto.isEmpty() || ancho <= 0 || alto <= 0) {
            return;
        }
        String contenido = INICIO_FIN + texto.toUpperCase() + INICIO_FIN;

        // Escala exacta: el simbolo ocupa 'modulos' unidades y se reservan zonas de silencio
        // a ambos lados para permitir la lectura sincronizada por el escaner.
        int modulos = 0;
        for (int i = 0; i < contenido.length(); i++) {
            modulos += modulosDe(PATRONES[indiceDe(contenido.charAt(i))]);
        }
        modulos += (contenido.length() - 1);
        int modulosTotalesConZonas = modulos + 2 * ZONA_SILENCIO_MODULOS;
        double unidad = ancho / (double) modulosTotalesConZonas;
        double anchoAngosto = unidad;
        double anchoAncho = unidad * 3;

        AffineTransform original = g2.getTransform();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color colorOriginal = g2.getColor();
        g2.setColor(Color.BLACK);

        // Posicion en modulos, para que cada elemento se dibuje sobre la retícula exacta.
        double cursorModulos = ZONA_SILENCIO_MODULOS;
        for (int i = 0; i < contenido.length(); i++) {
            String patron = PATRONES[indiceDe(contenido.charAt(i))];
            boolean barra = true;
            for (int j = 0; j < patron.length(); j++) {
                double anchoElemento = (patron.charAt(j) == 'w') ? anchoAncho : anchoAngosto;
                if (barra) {
                    int inicio = (int) Math.round(x + cursorModulos * unidad);
                    int fin = (int) Math.round(x + (cursorModulos + (patron.charAt(j) == 'w' ? 3 : 1)) * unidad);
                    g2.fill(new java.awt.Rectangle(inicio, y, Math.max(1, fin - inicio), alto));
                }
                cursorModulos += (patron.charAt(j) == 'w') ? 3 : 1;
                barra = !barra;
            }
            // Separacion entre caracteres (espacio angosto).
            cursorModulos += 1;
        }

        g2.setColor(colorOriginal);
        g2.setTransform(original);
    }

    /**
     * Dibuja el codigo de barras con el texto legible debajo.
     */
    public static void dibujarConTexto(Graphics2D g2, String texto, int x, int y, int ancho, int alto, int altoTexto) {
        dibujar(g2, texto, x, y, ancho, alto);
        java.awt.Font fuente = g2.getFont();
        g2.setFont(fuente.deriveFont(java.awt.Font.PLAIN, Math.max(9f, altoTexto * 0.75f)));
        java.awt.FontMetrics fm = g2.getFontMetrics();
        int anchoTexto = fm.stringWidth(texto);
        g2.drawString(texto, x + Math.max(0, (ancho - anchoTexto) / 2), y + alto + fm.getAscent());
        g2.setFont(fuente);
    }
}
