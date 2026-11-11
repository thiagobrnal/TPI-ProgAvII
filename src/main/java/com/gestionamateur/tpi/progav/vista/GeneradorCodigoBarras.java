package com.gestionamateur.tpi.progav.vista;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

/**
 * CU-02.2: generador de codigos de barras Code 39 (simbologia abierta, sin dependencias).
 * Cada caracter se dibuja con 9 elementos (5 barras y 4 espacios) y se separa del
 * siguiente con un espacio angosto.
 */
public final class GeneradorCodigoBarras {

    /** Patrones Code 39: 'n' angosto, 'w' ancho. 44 patrones (0-9, A-Z y 7 simbolos). */
    private static final String[] PATRONES = {
            "nnwwnwnn", "wnnwnnnnw", "nnwwnnnnw", "wnwwnnnnn", "nnnwwnnnw",
            "wnnwwnnnn", "nnwwnwnnn", "nnnwwnnnw", "wnnwwnnnn", "nnnwwnnnn",
            "wnnnnwnnw", "nnwnnwnnw", "wnwnnwnnn", "nnnnwwnnw", "wnnnwwnnn",
            "nnwnwwnnn", "nnnnnwwnw", "wnnnnwwnn", "nnwnnwwnn", "nnnnwwwnn",
            "wnnnnnnww", "nnwnnnnww", "wnwnnnnwn", "nnnnwnnww", "wnnnwnwn",
            "nnwnwnnwn", "nnnnnnwww", "wnnnnnwwn", "nnwnnnwwn", "nnnnwnwwn",
            "wwnnnnnnw", "nwwnnnnnw", "wwwnnnnnn", "nwnnwnnnw", "wwnnwnnnn",
            "nwwnwnnnn", "nwnnnnwnn", "wwnnnnwnn", "nwwnnnwnn", "nwnwnwnnn",
            "nwnwnnnwn", "nwnnnwnwn", "nnnwnwnwn", "nwnnwnwnn"
    };

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

        // Ancho total en modulos: cada caracter ocupa 15 modulos (3 anchos x 3 + 6 angostos x 1)
        // mas 1 modulo de separacion entre caracteres.
        int modulos = contenido.length() * 15 + (contenido.length() - 1);
        double unidad = ancho / (double) modulos;
        double anchoAngosto = unidad;
        double anchoAncho = unidad * 3;

        AffineTransform original = g2.getTransform();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color colorOriginal = g2.getColor();
        g2.setColor(Color.BLACK);

        double cursor = x;
        for (int i = 0; i < contenido.length(); i++) {
            String patron = PATRONES[indiceDe(contenido.charAt(i))];
            boolean barra = true;
            for (int j = 0; j < patron.length(); j++) {
                double anchoElemento = (patron.charAt(j) == 'w') ? anchoAncho : anchoAngosto;
                if (barra) {
                    g2.fill(new java.awt.Rectangle((int) Math.round(cursor), y, (int) Math.round(anchoElemento), alto));
                }
                cursor += anchoElemento;
                barra = !barra;
            }
            // Separacion entre caracteres (espacio angosto).
            cursor += anchoAngosto;
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
