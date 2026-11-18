package com.gestionamateur.tpi.progav.vista;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

/**
 * Sistema visual centralizado Dark Stadium Slate (Etapa 0).
 *
 * <p>Concentra los tokens cromáticos, jerarquía tipográfica, bordes y utilidades
 * de estilización compatibles con FlatLaf 3.2.1 y Java 17.
 */
public final class EstilosUI {

    // ------------------------------------------------------------------
    // Tokens de superficie y estructura
    // ------------------------------------------------------------------
    public static final Color BG_CANVAS = new Color(0x0D, 0x13, 0x1A);
    public static final Color BG_HEADER = new Color(0x0F, 0x29, 0x1E);
    public static final Color BG_SURFACE = new Color(0x16, 0x1F, 0x2C);
    public static final Color BG_CONTROL = new Color(0x1E, 0x29, 0x3B);
    public static final Color BG_TABLE = new Color(0x13, 0x1B, 0x26);
    public static final Color BG_TABLE_ALT = new Color(0x18, 0x22, 0x30);
    public static final Color BG_SELECTION = new Color(0x1E, 0x3A, 0x5F);
    public static final Color BORDER_DEFAULT = new Color(0x2A, 0x37, 0x4A);

    // ------------------------------------------------------------------
    // Tokens de texto
    // ------------------------------------------------------------------
    public static final Color TEXT_PRIMARY = new Color(0xF1, 0xF5, 0xF9);
    public static final Color TEXT_SECONDARY = new Color(0x94, 0xA3, 0xB8);
    public static final Color TEXT_DISABLED = new Color(0x64, 0x74, 0x8B);
    public static final Color TEXT_ON_LIGHT_ACCENT = new Color(0x09, 0x10, 0x17);

    // ------------------------------------------------------------------
    // Tokens semánticos de acción y estado (verificados WCAG AA)
    // ------------------------------------------------------------------
    public static final Color ACTION_PRIMARY = new Color(0x04, 0x78, 0x57);
    public static final Color ACTION_ACCENT = new Color(0x10, 0xB9, 0x81);
    public static final Color ACTION_SECONDARY = new Color(0x25, 0x63, 0xEB);
    public static final Color ACTION_WARNING = new Color(0xF5, 0x9E, 0x0B);
    public static final Color ACTION_DANGER = new Color(0xDC, 0x26, 0x26);

    // ------------------------------------------------------------------
    // Jerarquía tipográfica
    // ------------------------------------------------------------------
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 12);

    private static volatile boolean temaRegistrado = false;

    private EstilosUI() {
    }

    /**
     * Registra la fuente de propiedades personalizadas del tema y activa FlatDarkLaf.
     * Es idempotente y segura para invocarse tanto en el arranque como en pruebas aisladas.
     */
    public static synchronized void inicializarTema() {
        if (!temaRegistrado) {
            FlatLaf.registerCustomDefaultsSource("tema");
            temaRegistrado = true;
        }
        if (!(UIManager.getLookAndFeel() instanceof FlatDarkLaf)) {
            FlatDarkLaf.setup();
        }
    }

    /** Aplica fondo base Dark Stadium Slate y márgenes exteriores uniformes al contenedor raíz. */
    public static void estilizarVentana(JPanel raiz) {
        raiz.setBackground(BG_CANVAS);
        raiz.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
    }

    /** Construye un encabezado institucional limpio con título, subtítulo y borde inferior sutil. */
    public static JPanel crearEncabezado(String titulo, String subtitulo) {
        JPanel header = new JPanel(new BorderLayout(4, 4));
        header.setBackground(BG_HEADER);
        Border borde = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, ACTION_ACCENT),
                BorderFactory.createEmptyBorder(12, 16, 12, 16));
        header.setBorder(borde);

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(FONT_TITLE);
        lblTitulo.setForeground(TEXT_PRIMARY);
        header.add(lblTitulo, BorderLayout.NORTH);

        if (subtitulo != null && !subtitulo.isBlank()) {
            JLabel lblSubtitulo = new JLabel(subtitulo);
            lblSubtitulo.setFont(FONT_SUBTITLE);
            lblSubtitulo.setForeground(TEXT_SECONDARY);
            header.add(lblSubtitulo, BorderLayout.SOUTH);
        }
        return header;
    }

    /** Aplica superficie de tarjeta elevada con borde discreto y relleno interno. */
    public static void estilizarTarjeta(JPanel panel) {
        panel.setBackground(BG_SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_DEFAULT, 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
    }

    /** Aplica superficie de tarjeta con título de borde legible en modo oscuro. */
    public static void estilizarTarjetaConTitulo(JPanel panel, String titulo) {
        panel.setBackground(BG_SURFACE);
        TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_DEFAULT, 1),
                titulo,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                FONT_SECTION,
                TEXT_PRIMARY);
        panel.setBorder(BorderFactory.createCompoundBorder(
                tb,
                BorderFactory.createEmptyBorder(8, 12, 10, 12)));
    }

    /** Estiliza paneles embebidos en diálogos JOptionPane. */
    public static void estilizarPanelModal(JPanel panel) {
        panel.setBackground(BG_SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_DEFAULT, 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
    }

    /** Estiliza títulos de sección dentro de tarjetas o formularios. */
    public static void estilizarTituloSeccion(JLabel label) {
        label.setFont(FONT_SECTION);
        label.setForeground(TEXT_PRIMARY);
    }

    /** Estiliza valores de texto primario en formularios o modales. */
    public static void estilizarTextoPrimario(JLabel label) {
        label.setFont(FONT_BODY);
        label.setForeground(TEXT_PRIMARY);
    }

    /** Estiliza etiquetas descriptivas de campos de formulario. */
    public static void estilizarEtiquetaCampo(JLabel label) {
        label.setFont(FONT_LABEL);
        label.setForeground(TEXT_SECONDARY);
    }

    /** Estiliza etiquetas de estado o resumen contextual. */
    public static void estilizarEstadoInfo(JLabel label) {
        label.setFont(FONT_SUBTITLE);
        label.setForeground(TEXT_SECONDARY);
    }

    /** Estiliza indicadores destacados de estado o marcador en vivo. */
    public static void estilizarIndicadorAcento(JLabel label) {
        label.setFont(FONT_SECTION);
        label.setForeground(ACTION_ACCENT);
    }

    /** Configura tipografía, altura mínima y placeholder en campos de texto. */
    public static void estilizarCampoTexto(JTextField campo, String placeholder) {
        campo.setFont(FONT_BODY);
        campo.setForeground(TEXT_PRIMARY);
        campo.setBackground(BG_CONTROL);
        campo.setCaretColor(TEXT_PRIMARY);
        Dimension pref = campo.getPreferredSize();
        campo.setPreferredSize(new Dimension(pref.width, Math.max(pref.height, 30)));
        if (placeholder != null && !placeholder.isBlank()) {
            campo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        }
        campo.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, Boolean.TRUE);
    }

    /** Configura campos de contraseña coherentes con el sistema oscuro. */
    public static void estilizarCampoPassword(JPasswordField campo) {
        campo.setFont(FONT_BODY);
        campo.setForeground(TEXT_PRIMARY);
        campo.setBackground(BG_CONTROL);
        campo.setCaretColor(TEXT_PRIMARY);
        Dimension pref = campo.getPreferredSize();
        campo.setPreferredSize(new Dimension(pref.width, Math.max(pref.height, 30)));
    }

    /** Configura áreas de texto multilínea con borde y relleno legibles. */
    public static void estilizarAreaTexto(JTextArea area) {
        area.setFont(FONT_BODY);
        area.setForeground(TEXT_PRIMARY);
        area.setBackground(BG_CONTROL);
        area.setCaretColor(TEXT_PRIMARY);
        area.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_DEFAULT, 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
    }

    /** Estiliza casillas de verificación sobre superficies oscuras. */
    public static void estilizarCheckBox(JCheckBox check) {
        check.setFont(FONT_BODY);
        check.setForeground(TEXT_PRIMARY);
        check.setOpaque(false);
    }

    /** Configura tipografía y altura cómoda en listas desplegables. */
    public static void estilizarCombo(JComboBox<?> combo) {
        combo.setFont(FONT_BODY);
        combo.setForeground(TEXT_PRIMARY);
        combo.setBackground(BG_CONTROL);
        Dimension pref = combo.getPreferredSize();
        combo.setPreferredSize(new Dimension(Math.max(pref.width, 180), Math.max(pref.height, 30)));
    }

    /** Estiliza contenedores de pestañas bajo la identidad Dark Stadium Slate. */
    public static void estilizarPestanas(JTabbedPane pestanas) {
        pestanas.setFont(FONT_LABEL);
        pestanas.setForeground(TEXT_PRIMARY);
        pestanas.setBackground(BG_CANVAS);
        pestanas.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_TYPE,
                FlatClientProperties.TABBED_PANE_TAB_TYPE_UNDERLINED);
    }

    /** Construye la barra inferior estándar separando acciones operativas (izq.) y navegación (der.). */
    public static JPanel crearBarraAcciones(JComponent[] izquierda, JComponent[] derecha) {
        JPanel barra = new JPanel(new BorderLayout(8, 0));
        barra.setBackground(BG_CANVAS);

        JPanel panelIzq = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 4));
        panelIzq.setBackground(BG_CANVAS);
        if (izquierda != null) {
            for (JComponent c : izquierda) {
                panelIzq.add(c);
            }
        }
        barra.add(panelIzq, BorderLayout.CENTER);

        if (derecha != null && derecha.length > 0) {
            JPanel panelDer = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 4));
            panelDer.setBackground(BG_CANVAS);
            for (JComponent c : derecha) {
                panelDer.add(c);
            }
            barra.add(panelDer, BorderLayout.EAST);
        }
        return barra;
    }

    /** Configura apariencia Dark Stadium Slate y renderers estándar para tablas de datos. */
    public static void estilizarTabla(JTable tabla, JScrollPane scroll) {
        tabla.setFont(FONT_BODY);
        tabla.setForeground(TEXT_PRIMARY);
        tabla.setBackground(BG_TABLE);
        tabla.setSelectionBackground(BG_SELECTION);
        tabla.setSelectionForeground(TEXT_PRIMARY);
        tabla.setGridColor(BORDER_DEFAULT);
        tabla.setShowHorizontalLines(true);
        tabla.setShowVerticalLines(false);
        tabla.setRowHeight(30);
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setDefaultRenderer(Object.class, crearRendererCebra(SwingConstants.LEFT));

        JTableHeader header = tabla.getTableHeader();
        if (header != null) {
            header.setFont(FONT_LABEL);
            header.setForeground(TEXT_PRIMARY);
            header.setBackground(BG_SURFACE);
            header.setReorderingAllowed(false);
            header.setPreferredSize(new Dimension(header.getPreferredSize().width, 32));
        }

        if (scroll != null) {
            scroll.getViewport().setBackground(BG_TABLE);
            scroll.setBorder(BorderFactory.createLineBorder(BORDER_DEFAULT, 1));
        }
    }

    /** Renderer estándar con relleno horizontal y filas alternadas de alto contraste. */
    public static DefaultTableCellRenderer crearRendererCebra(int alineacion) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(alineacion);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (isSelected) {
                    setBackground(BG_SELECTION);
                    setForeground(TEXT_PRIMARY);
                } else {
                    setBackground((row % 2 == 0) ? BG_TABLE : BG_TABLE_ALT);
                    setForeground(TEXT_PRIMARY);
                }
                return this;
            }
        };
    }

    /** Renderer semántico para columnas de estado binario (activa vs inhabilitada). */
    public static DefaultTableCellRenderer crearRendererEstadoActivo(String textoActivo) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                String texto = value == null ? "" : String.valueOf(value);
                boolean activa = textoActivo.equalsIgnoreCase(texto);
                setFont(FONT_LABEL);
                if (isSelected) {
                    setBackground(BG_SELECTION);
                    setForeground(TEXT_PRIMARY);
                } else {
                    setBackground((row % 2 == 0) ? BG_TABLE : BG_TABLE_ALT);
                    setForeground(activa ? ACTION_ACCENT : ACTION_WARNING);
                }
                return this;
            }
        };
    }

    // ------------------------------------------------------------------
    // Botones semánticos (FlatClientProperties.STYLE)
    // ------------------------------------------------------------------

    public static void estilizarBotonPrimario(JButton boton) {
        aplicarEstiloBoton(boton,
                "background: #047857; foreground: #FFFFFF; hoverBackground: #059669; "
                        + "pressedBackground: #065F46; borderColor: #10B981; "
                        + "hoverBorderColor: #34D399; focusedBorderColor: #10B981; "
                        + "disabledBackground: #1B2330; disabledText: #64748B; disabledBorderColor: #263244; "
                        + "arc: 6; margin: 6,14,6,14",
                ACTION_PRIMARY, Color.WHITE);
    }

    public static void estilizarBotonSecundario(JButton boton) {
        aplicarEstiloBoton(boton,
                "background: #2563EB; foreground: #FFFFFF; hoverBackground: #1D4ED8; "
                        + "pressedBackground: #1E40AF; borderColor: #3B82F6; "
                        + "hoverBorderColor: #60A5FA; focusedBorderColor: #93C5FD; "
                        + "disabledBackground: #1B2330; disabledText: #64748B; disabledBorderColor: #263244; "
                        + "arc: 6; margin: 6,14,6,14",
                ACTION_SECONDARY, Color.WHITE);
    }

    public static void estilizarBotonAdvertencia(JButton boton) {
        aplicarEstiloBoton(boton,
                "background: #F59E0B; foreground: #091017; hoverBackground: #D97706; "
                        + "pressedBackground: #B45309; borderColor: #FBBF24; "
                        + "hoverBorderColor: #FCD34D; focusedBorderColor: #FDE68A; "
                        + "disabledBackground: #1B2330; disabledText: #64748B; disabledBorderColor: #263244; "
                        + "arc: 6; margin: 6,14,6,14",
                ACTION_WARNING, TEXT_ON_LIGHT_ACCENT);
    }

    public static void estilizarBotonPeligro(JButton boton) {
        aplicarEstiloBoton(boton,
                "background: #DC2626; foreground: #FFFFFF; hoverBackground: #B91C1C; "
                        + "pressedBackground: #991B1B; borderColor: #EF4444; "
                        + "hoverBorderColor: #F87171; focusedBorderColor: #FCA5A5; "
                        + "disabledBackground: #1B2330; disabledText: #64748B; disabledBorderColor: #263244; "
                        + "arc: 6; margin: 6,14,6,14",
                ACTION_DANGER, Color.WHITE);
    }

    public static void estilizarBotonNeutro(JButton boton) {
        aplicarEstiloBoton(boton,
                "background: #253347; foreground: #F1F5F9; hoverBackground: #30425A; "
                        + "pressedBackground: #3B4F6B; borderColor: #3B4D66; "
                        + "hoverBorderColor: #526784; focusedBorderColor: #10B981; "
                        + "disabledBackground: #1B2330; disabledText: #64748B; disabledBorderColor: #263244; "
                        + "arc: 6; margin: 6,14,6,14",
                new Color(0x25, 0x33, 0x47), TEXT_PRIMARY);
    }

    /** Semántica binaria de módulos en menú principal: verde si está permitido, gris apagado si está denegado. */
    public static void estilizarBotonModulo(JButton boton, boolean permitido) {
        if (permitido) {
            estilizarBotonPrimario(boton);
        } else {
            estilizarBotonDenegado(boton);
        }
    }

    /** Variante gris apagada inequívocamente distinguible tanto de botones activos como del fondo de tarjeta. */
    public static void estilizarBotonDenegado(JButton boton) {
        boton.setFont(FONT_BUTTON);
        boton.setBackground(new Color(0x1B, 0x23, 0x30));
        boton.setForeground(TEXT_DISABLED);
        boton.setFocusPainted(false);
        boton.setCursor(Cursor.getDefaultCursor());
        boton.putClientProperty(FlatClientProperties.STYLE,
                "background: #1B2330; foreground: #64748B; borderColor: #263244; "
                        + "disabledBackground: #1B2330; disabledText: #64748B; disabledBorderColor: #263244; "
                        + "arc: 6; margin: 6,14,6,14");
    }

    private static void aplicarEstiloBoton(JButton boton, String style, Color fallbackBg, Color fallbackFg) {
        boton.setFont(FONT_BUTTON);
        boton.setBackground(fallbackBg);
        boton.setForeground(fallbackFg);
        boton.setFocusPainted(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.putClientProperty(FlatClientProperties.STYLE, style);
    }
}
