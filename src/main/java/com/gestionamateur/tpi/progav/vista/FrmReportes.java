package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.servicio.ReporteService;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * Ventana de reportes de Release 1 (CU-19 a CU-22).
 *
 * Contiene cuatro pestañas, cada una con su tabla, un boton "Actualizar" y un
 * boton "Exportar a PDF". La exportacion usa el paquete javax.print (incluido en
 * java.desktop, sin dependencias externas) dirigido al servicio de impresion
 * virtual "Microsoft Print to PDF" de Windows.
 *
 * La logica de impresion esta encapsulada en {@link #exportarReporteAPdf} para
 * poder reconectarla facilmente si el entorno de impresion cambia.
 */
public class FrmReportes extends JFrame {

    /** Nombre del servicio de impresion virtual de Windows usado para exportar. */
    private static final String SERVICIO_PDF_WINDOWS = "Microsoft Print to PDF";
    private static final Color COLOR_TITULO = new Color(0x12, 0x38, 0x5C);
    private static final Color COLOR_TEXTO_SECUNDARIO = new Color(0x3A, 0x55, 0x68);

    private final Usuario operador;
    private final ReporteService reporteService = new ReporteService();
    private final JTabbedPane pestanas = new JTabbedPane();

    private final PestanaReporte pestanaPosiciones;
    private final PestanaReporte pestanaJugadores;
    private final PestanaReporte pestanaHistorial;
    private final PestanaReporte pestanaArbitros;

    private final JComboBox<String> cmbTipoHistorial = new JComboBox<>();
    private final JLabel lblInformacion = new JLabel(" ");

    public FrmReportes(Usuario operador) {
        this.operador = operador;
        this.pestanaPosiciones = new PestanaReporte(
                "Tabla de posiciones",
                "Puntos por partido sobre partidos finalizados (victoria 3, empate 1, derrota 0).",
                new String[]{"Equipo", "PJ", "PG", "PE", "PP", "GF", "GC", "DG", "Puntos"},
                reporteService::obtenerTablaPosiciones);

        this.pestanaJugadores = new PestanaReporte(
                "Reporte de jugadores",
                "Goleadores, tarjetas amarillas, tarjetas rojas y sanciones vigentes por jugador.",
                new String[]{"Jugador", "Equipo", "Goles", "Tarjetas amarillas", "Tarjetas rojas", "Sanciones vigentes"},
                reporteService::obtenerReporteJugadores);

        this.pestanaHistorial = new PestanaReporte(
                "Historial deportivo",
                "Historial por equipo y por jugador. Use el selector para cambiar la vista.",
                new String[]{"Equipo", "Torneos", "PJ", "PG", "PE", "PP", "GF", "GC"},
                this::obtenerHistorialSeleccionado);

        this.pestanaArbitros = new PestanaReporte(
                "Reporte de árbitros",
                "Partidos dirigidos e incidencias registradas. Incluye a los árbitros sin partidos asignados.",
                new String[]{"Árbitro", "Categoría arbitral", "Partidos dirigidos", "Incidencias registradas"},
                reporteService::obtenerReporteArbitros);

        setTitle("Reportes del torneo (CU-19 a CU-22)");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        cargarTodos();
        setLocationRelativeTo(null);
    }

    /**
     * Los reportes del torneo son un modulo de la organizacion (consigna oficial:
     * "Reportes (Desktop - Organizador / Administrador)"). El control no se
     * apoya solo en el boton del panel: se verifica tambien aqui, en la ventana.
     */
    private boolean tienePermisoParaReportes() {
        if (operador == null || operador.getRol() == null) {
            return false;
        }
        return operador.getRol() == TiposDatos.Rol.ADMINISTRADOR
                || operador.getRol() == TiposDatos.Rol.ORGANIZADOR;
    }

    // ------------------------------------------------------------------
    // Construccion de la interfaz
    // ------------------------------------------------------------------

    private void initComponents() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.setBackground(Color.WHITE);

        JPanel panelSup = new JPanel(new BorderLayout());
        panelSup.setBackground(Color.WHITE);

        JLabel titulo = new JLabel("Reportes del torneo");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(COLOR_TITULO);
        panelSup.add(titulo, BorderLayout.NORTH);

        JPanel panelSelector = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        panelSelector.setBackground(Color.WHITE);
        panelSelector.add(new JLabel("Historial deportivo por:"));
        cmbTipoHistorial.setModel(new DefaultComboBoxModel<>(new String[]{"Equipo", "Jugador"}));
        cmbTipoHistorial.setPreferredSize(new Dimension(160, 24));
        cmbTipoHistorial.addActionListener(e -> pestanaHistorial.actualizar());
        panelSelector.add(cmbTipoHistorial);
        lblInformacion.setForeground(COLOR_TEXTO_SECUNDARIO);
        panelSelector.add(lblInformacion);
        panelSup.add(panelSelector, BorderLayout.CENTER);

        pestanas.addTab("Tabla de posiciones", pestanaPosiciones);
        pestanas.addTab("Jugadores", pestanaJugadores);
        pestanas.addTab("Historial deportivo", pestanaHistorial);
        pestanas.addTab("Árbitros", pestanaArbitros);

        panel.add(panelSup, BorderLayout.NORTH);
        panel.add(pestanas, BorderLayout.CENTER);

        setContentPane(panel);
        pack();
        setMinimumSize(new Dimension(980, 640));
    }

    private void cargarTodos() {
        pestanaPosiciones.actualizar();
        pestanaJugadores.actualizar();
        pestanaHistorial.actualizar();
        pestanaArbitros.actualizar();
    }

    /**
     * Devuelve el historial segun el tipo elegido en el selector de la pestana
     * CU-21, adaptando tambien los encabezados de columna.
     */
    private ReporteService.TablaReporte obtenerHistorialSeleccionado() throws TorneoExcepcion {
        boolean porJugador = "Jugador".equalsIgnoreCase((String) cmbTipoHistorial.getSelectedItem());
        return porJugador
                ? reporteService.obtenerHistorialJugadores()
                : reporteService.obtenerHistorialEquipos();
    }

    // ------------------------------------------------------------------
    // Exportacion a PDF
    // ------------------------------------------------------------------

    /**
     * Exporta el reporte visible a PDF usando el servicio de impresion virtual
     * "Microsoft Print to PDF" de Windows. Todo el flujo de impresion queda
     * encapsulado en este metodo para poder ajustarlo si el entorno de
     * impresion de la maquina cambia.
     *
     * @param pestana pestana cuyo contenido se desea exportar
     */
    private void exportarReporteAPdf(PestanaReporte pestana) {
        if (!tienePermisoParaReportes()) {
            JOptionPane.showMessageDialog(this,
                    "Su rol no tiene permisos para exportar los reportes del torneo.",
                    "Exportar a PDF", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            PrintService servicio = buscarServicioPdf();
            if (servicio == null) {
                JOptionPane.showMessageDialog(this,
                        "No se encontro el servicio de impresion \"" + SERVICIO_PDF_WINDOWS + "\".\n\n"
                                + "Para poder exportar a PDF debe estar habilitado el servicio \"Spooler\" de Windows "
                                + "y tener instalada la impresora virtual \"Microsoft Print to PDF\".\n\n"
                                + "Puede habilitarla desde Configuracion > Impresoras en Windows.",
                        "Exportar a PDF", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
            PageFormat formato = crearFormatoLandscapeA4();
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintService(servicio);
            job.setJobName(pestana.nombreArchivoSugerido());
            job.setPrintable(new ReportePrintable(pestana.tituloReporte, pestana.columnas,
                    pestana.ultimaCarga, fecha), formato);

            HashPrintRequestAttributeSet atributos = new HashPrintRequestAttributeSet();
            atributos.add(OrientationRequested.LANDSCAPE);
            atributos.add(new Copies(1));
            job.print(atributos);

            JOptionPane.showMessageDialog(this,
                    "El reporte \"" + pestana.tituloReporte + "\" se envio a la impresora virtual \""
                            + SERVICIO_PDF_WINDOWS + "\".\nElija la carpeta y el nombre del archivo en el dialogo de Windows.",
                    "Exportar a PDF", JOptionPane.INFORMATION_MESSAGE);
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo generar el PDF del reporte: " + e.getMessage(),
                    "Exportar a PDF", JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this,
                    "Ocurrio un error inesperado al exportar el reporte: " + e.getMessage(),
                    "Exportar a PDF", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Respaldo en CSV: permite obtener los datos del reporte aunque la maquina no tenga
     * ningun servicio de impresion disponible (la exportacion a PDF requiere el spooler
     * de Windows y la impresora virtual "Microsoft Print to PDF").
     */
    private void exportarReporteACsv(PestanaReporte pestana) {
        if (!tienePermisoParaReportes()) {
            JOptionPane.showMessageDialog(this,
                    "Su rol no tiene permisos para exportar los reportes del torneo.",
                    "Exportar a CSV", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (pestana.ultimaCarga.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay datos para exportar en este reporte.",
                    "Exportar a CSV", JOptionPane.WARNING_MESSAGE);
            return;
        }
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setSelectedFile(new java.io.File(pestana.nombreArchivoSugerido() + ".csv"));
        if (chooser.showSaveDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) {
            return;
        }
        java.io.File destino = chooser.getSelectedFile();
        try (java.io.PrintWriter salida = new java.io.PrintWriter(destino, "UTF-8")) {
            salida.println(String.join(";", pestana.columnas));
            for (Object[] fila : pestana.ultimaCarga) {
                StringBuilder linea = new StringBuilder();
                for (int i = 0; i < fila.length; i++) {
                    if (i > 0) {
                        linea.append(';');
                    }
                    String valor = fila[i] == null ? "" : String.valueOf(fila[i]);
                    linea.append('"').append(valor.replace("\"", "\"\"")).append('"');
                }
                salida.println(linea);
            }
            JOptionPane.showMessageDialog(this, "El reporte se exportó en: " + destino.getAbsolutePath(),
                    "Exportar a CSV", JOptionPane.INFORMATION_MESSAGE);
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo escribir el archivo CSV: " + ex.getMessage(),
                    "Exportar a CSV", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Crea el formato de pagina A4 horizontal con margenes de 1 centímetro,
     * para que la tabla del reporte ocupe el ancho completo de la hoja.
     */
    private PageFormat crearFormatoLandscapeA4() {
        PageFormat formato = new PageFormat();
        Paper papel = new Paper();
        MediaSize tamanoA4 = MediaSize.getMediaSizeForName(MediaSizeName.ISO_A4);
        float[] medidas = tamanoA4.getSize(Size2DSyntax.INCH);
        // El papel se declara con las medidas verticales del A4 (595 x 842 pt): es la
        // unica medida que el controlador de Windows reconoce como A4. Con la
        // orientacion LANDSCAPE, AWT dibuja la tabla a lo largo del lado largo de
        // la hoja; el controlador "Microsoft Print to PDF" la escribe girada 90
        // grados sobre la hoja vertical (comportamiento estandar de Windows).
        double ancho = medidas[0] * 72.0;
        double alto = medidas[1] * 72.0;
        papel.setSize(ancho, alto);
        double margen = 28.0;
        papel.setImageableArea(margen, margen, ancho - (margen * 2), alto - (margen * 2));
        formato.setPaper(papel);
        formato.setOrientation(PageFormat.LANDSCAPE);
        return formato;
    }

    /**
     * Busca el servicio de impresion virtual de Windows dentro de los servicios
     * de impresion disponibles en la maquina.
     *
     * @return el servicio encontrado o null si la maquina no lo expone
     */
    private PrintService buscarServicioPdf() {
        List<PrintService> servicios = Arrays.asList(PrintServiceLookup.lookupPrintServices(null, null));
        for (PrintService servicio : servicios) {
            if (servicio.getName().equalsIgnoreCase(SERVICIO_PDF_WINDOWS)) {
                return servicio;
            }
        }
        for (PrintService servicio : servicios) {
            if (servicio.getName().toLowerCase().contains("print to pdf")) {
                return servicio;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Componentes internos
    // ------------------------------------------------------------------

    /** Proveedor de datos de un reporte, con soporte de error de negocio. */
    private interface ProveedorReporte {
        ReporteService.TablaReporte obtener() throws TorneoExcepcion;
    }

    /**
     * Pestana de reporte: encabezado, tabla no editable y botones de accion.
     */
    private final class PestanaReporte extends JPanel {

        private final String tituloReporte;
        private final String descripcion;
        private String[] columnas;
        private final ProveedorReporte proveedor;
        private final DefaultTableModel modelo;
        private final JTable tabla;
        private List<Object[]> ultimaCarga = new ArrayList<>();

        PestanaReporte(String tituloReporte, String descripcion, String[] columnas, ProveedorReporte proveedor) {
            super(new BorderLayout(8, 8));
            this.tituloReporte = tituloReporte;
            this.descripcion = descripcion;
            this.columnas = columnas;
            this.proveedor = proveedor;
            this.modelo = new DefaultTableModel(columnas, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            this.tabla = new JTable(modelo);
            this.tabla.setRowHeight(22);
            this.tabla.setAutoCreateRowSorter(false);
            this.tabla.setFillsViewportHeight(true);

            setBackground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JPanel panelEncabezado = new JPanel(new BorderLayout());
            panelEncabezado.setBackground(Color.WHITE);
            JLabel lblTitulo = new JLabel(tituloReporte);
            lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 15));
            lblTitulo.setForeground(COLOR_TITULO);
            panelEncabezado.add(lblTitulo, BorderLayout.NORTH);
            JLabel lblDescripcion = new JLabel(descripcion);
            lblDescripcion.setForeground(COLOR_TEXTO_SECUNDARIO);
            panelEncabezado.add(lblDescripcion, BorderLayout.CENTER);

            JScrollPane scroll = new JScrollPane(tabla);
            scroll.setPreferredSize(new Dimension(880, 340));

            JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            panelBotones.setBackground(Color.WHITE);
            JButton btnActualizar = new JButton("Actualizar");
            btnActualizar.addActionListener(e -> actualizar());
            JButton btnExportar = new JButton("Exportar a PDF");
            btnExportar.addActionListener(e -> exportarReporteAPdf(this));
            JButton btnExportarCsv = new JButton("Exportar a CSV");
            btnExportarCsv.addActionListener(e -> exportarReporteACsv(this));
            panelBotones.add(btnActualizar);
            panelBotones.add(btnExportar);
            panelBotones.add(btnExportarCsv);

            add(panelEncabezado, BorderLayout.NORTH);
            add(scroll, BorderLayout.CENTER);
            add(panelBotones, BorderLayout.SOUTH);
        }

        /**
         * Nombre de archivo sugerido para el PDF, derivado del titulo del
         * reporte y sanitizado para que Windows lo acepte como nombre de archivo.
         */
        String nombreArchivoSugerido() {
            String base = tituloReporte.replaceAll("[\\\\/:*?\"<>|]", " ").trim();
            return base.isEmpty() ? "reporte" : base;
        }

        /** Vuelve a consultar los datos del reporte y refresca la tabla. */
        void actualizar() {
            if (!tienePermisoParaReportes()) {
                modelo.setRowCount(0);
                ultimaCarga = new ArrayList<>();
                lblInformacion.setText("Su rol no tiene permisos para consultar los reportes del torneo.");
                return;
            }
            lblInformacion.setText("Consultando datos...");
            // La consulta se resuelve fuera del hilo de interfaz (EDT) para no congelar la ventana.
            new SwingWorker<ReporteService.TablaReporte, Void>() {
                @Override
                protected ReporteService.TablaReporte doInBackground() {
                    try {
                        return proveedor.obtener();
                    } catch (TorneoExcepcion e) {
                        throw new IllegalStateException(e.getMessage(), e);
                    }
                }

                @Override
                protected void done() {
                    try {
                        aplicarReporte(get());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        Throwable causa = e.getCause();
                        String mensaje = causa == null ? e.getMessage() : causa.getMessage();
                        modelo.setRowCount(0);
                        ultimaCarga = new ArrayList<>();
                        lblInformacion.setText(mensaje);
                        JOptionPane.showMessageDialog(FrmReportes.this, mensaje,
                                "Reportes", JOptionPane.WARNING_MESSAGE);
                    }
                }
            }.execute();
        }

        /** Vuelca el resultado del reporte en la tabla. */
        private void aplicarReporte(ReporteService.TablaReporte reporte) {
            if (reporte != null) {
                if (reporte.getColumnas() != null && !Arrays.equals(reporte.getColumnas(), columnas)) {
                    columnas = reporte.getColumnas();
                    modelo.setColumnIdentifiers(columnas);
                }
                ultimaCarga = new ArrayList<>(reporte.getFilas());
            } else {
                ultimaCarga = new ArrayList<>();
            }
            modelo.setRowCount(0);
            for (Object[] fila : ultimaCarga) {
                modelo.addRow(fila);
            }
            if (ultimaCarga.isEmpty()) {
                lblInformacion.setText("No hay datos para mostrar en este reporte.");
            } else {
                lblInformacion.setText(" ");
            }
        }
    }

    // ------------------------------------------------------------------
    // Dibujo del reporte para impresion
    // ------------------------------------------------------------------

    /**
     * Printable que dibuja el reporte completo: titulo, fecha de generacion,
     * encabezados de columna y filas de datos, paginando si no entra en una
     * sola hoja.
     */
    public static class ReportePrintable implements Printable {

        private static final int ALTO_FILA = 16;
        private static final int ALTO_ENCABEZADO = 20;
        private static final int ALTO_TITULO = 26;
        private static final int ALTO_SUBTITULO = 16;
        private static final int MARGEN_INFERIOR = 8;

        private final String titulo;
        private final String[] columnas;
        private final List<Object[]> filas;
        private final String fechaGeneracion;

        public ReportePrintable(String titulo, String[] columnas, List<Object[]> filas, String fechaGeneracion) {
            this.titulo = titulo;
            this.columnas = columnas == null ? new String[0] : columnas;
            this.filas = filas == null ? new ArrayList<>() : filas;
            this.fechaGeneracion = fechaGeneracion;
        }

        @Override
        public int print(Graphics g, PageFormat pageFormat, int pageIndex) throws PrinterException {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                double x = pageFormat.getImageableX();
                double y = pageFormat.getImageableY();
                double ancho = pageFormat.getImageableWidth();
                double alto = pageFormat.getImageableHeight();

                int[] anchos = calcularAnchosColumnas(g2, ancho);
                int altoBloqueTitulo = (pageIndex == 0) ? ALTO_TITULO + ALTO_SUBTITULO + 6 : 0;
                int altoDisponible = (int) (alto - altoBloqueTitulo - ALTO_ENCABEZADO - MARGEN_INFERIOR);
                int filasPorPagina = Math.max(1, altoDisponible / ALTO_FILA);

                int desde = pageIndex * filasPorPagina;
                int hasta = Math.min(filas.size(), desde + filasPorPagina);
                if (desde >= filas.size() && !(filas.isEmpty() && pageIndex == 0)) {
                    return NO_SUCH_PAGE;
                }

                double cursorY = y + altoBloqueTitulo;

                if (pageIndex == 0) {
                    g2.setFont(new Font("SansSerif", Font.BOLD, 15));
                    g2.setColor(Color.BLACK);
                    g2.drawString(titulo, (float) x, (float) (y + ALTO_TITULO - 8));
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                    g2.setColor(Color.DARK_GRAY);
                    g2.drawString("Generado el " + fechaGeneracion, (float) x, (float) (y + ALTO_TITULO + 6));
                    cursorY = y + ALTO_TITULO + ALTO_SUBTITULO + 6;
                }

                // Encabezado de la tabla
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                FontMetrics metricsEncabezado = g2.getFontMetrics();
                g2.setColor(new Color(0xDC, 0xE4, 0xEC));
                g2.fillRect((int) x, (int) cursorY, (int) ancho, ALTO_ENCABEZADO);
                g2.setColor(Color.BLACK);
                double cursorX = x;
                for (int i = 0; i < columnas.length; i++) {
                    g2.drawString(recortar(columnas[i], anchos[i], metricsEncabezado),
                            (float) cursorX + 3, (float) cursorY + 14);
                    cursorX += anchos[i];
                }
                cursorY += ALTO_ENCABEZADO;

                // Filas de datos
                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                FontMetrics metricsFila = g2.getFontMetrics();
                for (int f = desde; f < hasta; f++) {
                    Object[] fila = filas.get(f);
                    cursorX = x;
                    for (int i = 0; i < columnas.length; i++) {
                        Object valor = i < fila.length ? fila[i] : "";
                        g2.setColor(Color.BLACK);
                        g2.drawString(recortar(valor == null ? "" : String.valueOf(valor), anchos[i], metricsFila),
                                (float) cursorX + 3, (float) cursorY + 12);
                        cursorX += anchos[i];
                    }
                    g2.setColor(new Color(0xE0, 0xE0, 0xE0));
                    g2.drawLine((int) x, (int) cursorY + ALTO_FILA - 2, (int) (x + ancho), (int) cursorY + ALTO_FILA - 2);
                    cursorY += ALTO_FILA;
                }

                if (filas.isEmpty()) {
                    g2.setFont(new Font("SansSerif", Font.ITALIC, 10));
                    g2.setColor(Color.GRAY);
                    g2.drawString("No hay datos disponibles para este reporte.", (float) x, (float) cursorY + 14);
                }

                // Pie de pagina
                g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
                g2.setColor(Color.GRAY);
                g2.drawString("Pagina " + (pageIndex + 1), (float) x, (float) (y + alto - 4));

                return PAGE_EXISTS;
            } finally {
                g2.dispose();
            }
        }

        /**
         * Calcula el ancho de cada columna a partir del contenido y reparte el
         * ancho disponible de la hoja de forma proporcional.
         */
        private int[] calcularAnchosColumnas(Graphics2D g2, double anchoDisponible) {
            int[] anchos = new int[columnas.length];
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            int total = 0;
            for (int i = 0; i < columnas.length; i++) {
                int max = g2.getFontMetrics().stringWidth(columnas[i]) + 8;
                for (Object[] fila : filas) {
                    Object valor = i < fila.length ? fila[i] : "";
                    int anchoTexto = g2.getFontMetrics().stringWidth(valor == null ? "" : String.valueOf(valor)) + 8;
                    if (anchoTexto > max) {
                        max = anchoTexto;
                    }
                }
                anchos[i] = Math.min(max, 220);
                total += anchos[i];
            }
            if (total <= 0) {
                return anchos;
            }
            if (total > anchoDisponible) {
                double factor = anchoDisponible / total;
                for (int i = 0; i < anchos.length; i++) {
                    anchos[i] = Math.max(20, (int) (anchos[i] * factor));
                }
            }
            return anchos;
        }

        /** Recorta el texto para que no desborde el ancho de la columna. */
        private String recortar(String texto, int ancho, FontMetrics metrics) {
            if (texto == null) {
                return "";
            }
            if (metrics.stringWidth(texto) <= ancho) {
                return texto;
            }
            String recortado = texto;
            while (recortado.length() > 1 && metrics.stringWidth(recortado + "...") > ancho) {
                recortado = recortado.substring(0, recortado.length() - 1);
            }
            return recortado + "...";
        }
    }
}
