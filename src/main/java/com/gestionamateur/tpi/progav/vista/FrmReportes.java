package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.seguridad.PoliticaPermisos;
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
import javax.swing.JComponent;
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
                () -> reporteService.obtenerTablaPosiciones(operador));

        this.pestanaJugadores = new PestanaReporte(
                "Reporte de jugadores",
                "Goleadores, tarjetas amarillas, tarjetas rojas y sanciones vigentes por jugador.",
                new String[]{"Jugador", "Equipo", "Goles", "Tarjetas amarillas", "Tarjetas rojas", "Sanciones vigentes"},
                () -> reporteService.obtenerReporteJugadores(operador));

        this.pestanaHistorial = new PestanaReporte(
                "Historial deportivo",
                "Historial por equipo y por jugador. Use el selector para cambiar la vista.",
                new String[]{"Equipo", "Torneos", "PJ", "PG", "PE", "PP", "GF", "GC"},
                this::obtenerHistorialSeleccionado);

        this.pestanaArbitros = new PestanaReporte(
                "Reporte de árbitros",
                "Partidos dirigidos e incidencias registradas. Incluye a los árbitros sin partidos asignados.",
                new String[]{"Árbitro", "Categoría arbitral", "Partidos dirigidos", "Incidencias registradas"},
                () -> reporteService.obtenerReporteArbitros(operador));

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
        return PoliticaPermisos.tienePermisoOperacion(operador, PoliticaPermisos.ModuloMenu.REPORTES);
    }

    // ------------------------------------------------------------------
    // Construccion de la interfaz
    // ------------------------------------------------------------------

    private void initComponents() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        EstilosUI.estilizarVentana(panel);

        JPanel panelSup = new JPanel(new BorderLayout(0, 10));
        panelSup.setBackground(EstilosUI.BG_CANVAS);

        JPanel panelEncabezado = EstilosUI.crearEncabezado(
                "Reportes del torneo",
                "Tablas oficiales, estadísticas, historiales y exportación a PDF / CSV (CU-19 a CU-22)");
        panelSup.add(panelEncabezado, BorderLayout.NORTH);

        JPanel panelSelector = new JPanel(new BorderLayout(12, 0));
        EstilosUI.estilizarTarjeta(panelSelector);
        JPanel panelCombo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panelCombo.setBackground(EstilosUI.BG_SURFACE);
        JLabel lblHistorial = new JLabel("Historial deportivo por:");
        EstilosUI.estilizarEtiquetaCampo(lblHistorial);
        panelCombo.add(lblHistorial);
        cmbTipoHistorial.setModel(new DefaultComboBoxModel<>(new String[]{"Equipo", "Jugador"}));
        EstilosUI.estilizarCombo(cmbTipoHistorial);
        cmbTipoHistorial.addActionListener(e -> pestanaHistorial.actualizar());
        panelCombo.add(cmbTipoHistorial);
        panelSelector.add(panelCombo, BorderLayout.WEST);
        EstilosUI.estilizarEstadoInfo(lblInformacion);
        panelSelector.add(lblInformacion, BorderLayout.CENTER);
        panelSup.add(panelSelector, BorderLayout.CENTER);

        EstilosUI.estilizarPestanas(pestanas);
        pestanas.addTab("Tabla de posiciones", pestanaPosiciones);
        pestanas.addTab("Jugadores", pestanaJugadores);
        pestanas.addTab("Historial deportivo", pestanaHistorial);
        pestanas.addTab("Árbitros", pestanaArbitros);

        panel.add(panelSup, BorderLayout.NORTH);
        panel.add(pestanas, BorderLayout.CENTER);

        setContentPane(panel);
        pack();
        setMinimumSize(new Dimension(980, 660));
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
                ? reporteService.obtenerHistorialJugadores(operador)
                : reporteService.obtenerHistorialEquipos(operador);
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
            PageFormat formato = crearFormatoPortraitA4();
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintService(servicio);
            job.setJobName(pestana.nombreArchivoSugerido());
            job.setPrintable(new ReportePrintable(pestana.tituloReporte, pestana.columnas,
                    pestana.ultimaCarga, fecha), formato);

            HashPrintRequestAttributeSet atributos = new HashPrintRequestAttributeSet();
            // Release 1: los reportes se generan en A4 vertical.
            atributos.add(OrientationRequested.PORTRAIT);
            atributos.add(MediaSizeName.ISO_A4);
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
     * Crea el formato de pagina A4 vertical (portrait) con margenes de 1 centimetro.
     *
     * <p>Requisito de Release 1: todos los reportes PDF (CU-19 a CU-22) se generan en A4
     * vertical. El papel se declara con las medidas verticales del A4 (595 x 842 pt) porque
     * es la unica medida que el controlador de Windows reconoce como A4; la orientacion
     * PORTRAIT se fija explicitamente en el {@link PageFormat} y en los atributos de
     * impresion, de modo que el area imprimible efectiva sea de 539 x 786 pt.
     *
     * <p>Nota tecnica: en el JDK, {@code PageFormat.setOrientation} no modifica el {@link Paper}
     * sino una bandera; la conmutacion se aplica en los getters (por eso un papel vertical con
     * orientacion LANDSCAPE entrega un area imprimible de 786 x 539 pt). Fijando PORTRAIT sobre
     * el papel vertical A4 el area imprimible queda correctamente en 539 x 786 pt.
     *
     * @return formato de pagina A4 portrait
     */
    static PageFormat crearFormatoPortraitA4() {
        PageFormat formato = new PageFormat();
        Paper papel = new Paper();
        MediaSize tamanoA4 = MediaSize.getMediaSizeForName(MediaSizeName.ISO_A4);
        float[] medidas = tamanoA4.getSize(Size2DSyntax.INCH);
        double ancho = medidas[0] * 72.0;
        double alto = medidas[1] * 72.0;
        papel.setSize(ancho, alto);
        double margen = 28.0;
        papel.setImageableArea(margen, margen, ancho - (margen * 2), alto - (margen * 2));
        formato.setPaper(papel);
        formato.setOrientation(PageFormat.PORTRAIT);
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
            super(new BorderLayout(10, 10));
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
            this.tabla.setAutoCreateRowSorter(false);

            setBackground(EstilosUI.BG_CANVAS);
            setBorder(BorderFactory.createEmptyBorder(10, 6, 6, 6));

            JPanel panelEncabezado = new JPanel(new BorderLayout(0, 4));
            EstilosUI.estilizarTarjeta(panelEncabezado);
            JLabel lblTitulo = new JLabel(tituloReporte);
            EstilosUI.estilizarTituloSeccion(lblTitulo);
            panelEncabezado.add(lblTitulo, BorderLayout.NORTH);
            JLabel lblDescripcion = new JLabel(descripcion);
            EstilosUI.estilizarEstadoInfo(lblDescripcion);
            panelEncabezado.add(lblDescripcion, BorderLayout.CENTER);

            JScrollPane scroll = new JScrollPane(tabla);
            scroll.setPreferredSize(new Dimension(880, 340));
            EstilosUI.estilizarTabla(tabla, scroll);

            boolean autorizado = tienePermisoParaReportes();

            JButton btnActualizar = new JButton("Actualizar");
            EstilosUI.estilizarBotonNeutro(btnActualizar);
            btnActualizar.setEnabled(autorizado);
            btnActualizar.addActionListener(e -> actualizar());

            JButton btnExportar = new JButton("Exportar a PDF");
            EstilosUI.estilizarBotonPrimario(btnExportar);
            btnExportar.setEnabled(autorizado);
            btnExportar.addActionListener(e -> exportarReporteAPdf(this));

            JButton btnExportarCsv = new JButton("Exportar a CSV");
            EstilosUI.estilizarBotonSecundario(btnExportarCsv);
            btnExportarCsv.setEnabled(autorizado);
            btnExportarCsv.addActionListener(e -> exportarReporteACsv(this));

            JPanel panelBotones = EstilosUI.crearBarraAcciones(
                    new JComponent[]{btnActualizar, btnExportar, btnExportarCsv},
                    null);

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
    /**
     * Printable que dibuja el reporte completo: titulo, fecha de generacion,
     * encabezados de columna y filas de datos, paginando si no entra en una
     * sola hoja.
     *
     * <p><b>DEF-017 (perdia de filas en la paginacion).</b> La paginacion se resuelve con un
     * <i>plan explicito</i>: se acumulan los altos reales de cada fila -que varian porque una
     * celda puede ocupar varias lineas- y se obtiene el rango de filas de cada pagina. El rango
     * ya no se calcula multiplicando el indice de pagina por un numero de filas por pagina
     * (que diferia entre la primera pagina y las siguientes y dejaba filas sin imprimir), de
     * modo que <b>todas las filas se imprimen exactamente una vez y en orden</b>.
     *
     * <p>El diseno se recalcula en cada invocacion a {@link #print}: es una funcion pura de
     * (filas, columnas, {@link PageFormat} y metricas de fuente), por lo que el resultado no
     * depende del orden en que el motor de impresion solicite las paginas.
     *
     * <p>En A4 vertical las celdas se reparten en varias lineas cuando el texto no cabe en la
     * columna. Ningun dato se recorta: el texto se divide en lineas y, si una palabra suelta
     * excede el ancho de la columna, se parte por caracteres.
     */
    public static class ReportePrintable implements Printable {

        /** Fuente del cuerpo del reporte. */
        private static final Font FUENTE_DATOS = new Font("SansSerif", Font.PLAIN, 10);
        /** Fuente de los encabezados de columna. */
        private static final Font FUENTE_ENCABEZADO = new Font("SansSerif", Font.BOLD, 10);
        /** Fuente del titulo del reporte. */
        private static final Font FUENTE_TITULO = new Font("SansSerif", Font.BOLD, 15);
        /** Fuente del pie de pagina. */
        private static final Font FUENTE_PIE = new Font("SansSerif", Font.PLAIN, 9);

        /** Alto de una linea de texto. */
        private static final int ALTO_LINEA = 12;
        /** Alto minimo de una fila de datos. */
        private static final int ALTO_FILA = 16;
        /** Alto del bloque de titulo (solo en la primera pagina). */
        private static final int ALTO_TITULO = 26;
        /** Alto del bloque de fecha de generacion (solo en la primera pagina). */
        private static final int ALTO_SUBTITULO = 16;
        /** Alto reservado al pie de pagina. */
        private static final int MARGEN_INFERIOR = 8;
        /** Relleno horizontal de cada celda. */
        private static final int RELLENO_HORIZONTAL = 6;
        /** Relleno vertical de cada fila y del encabezado. */
        private static final int RELLENO_VERTICAL = 4;
        /** Ancho maximo reservado a una columna. */
        private static final int ANCHO_MAXIMO_COLUMNA = 220;
        /** Alto del bloque de titulo mas la fecha de generacion. */
        private static final int ALTO_BLOQUE_TITULO = ALTO_TITULO + ALTO_SUBTITULO + 6;

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

        /**
         * Diseno maquetado de un reporte para un {@link PageFormat} concreto.
         * Es inmutable y se recalcula en cada impresion (no hay estado compartido).
         */
        private static final class Diseno {

            /** Ancho de cada columna, en puntos. */
            final int[] anchos;
            /** Lineas de cada encabezado de columna. */
            final String[][] lineasEncabezado;
            /** Alto del bloque de encabezado de la tabla. */
            final int altoEncabezado;
            /** Lineas de cada celda: [fila][columna][linea]. */
            final String[][][] celdas;
            /** Alto de cada fila de datos. */
            final int[] altoFila;
            /** Primera fila de cada pagina. */
            final int[] inicioFila;
            /** Ultima fila (exclusiva) de cada pagina. */
            final int[] finFila;

            Diseno(int[] anchos, String[][] lineasEncabezado, int altoEncabezado,
                   String[][][] celdas, int[] altoFila, int[] inicioFila, int[] finFila) {
                this.anchos = anchos;
                this.lineasEncabezado = lineasEncabezado;
                this.altoEncabezado = altoEncabezado;
                this.celdas = celdas;
                this.altoFila = altoFila;
                this.inicioFila = inicioFila;
                this.finFila = finFila;
            }

            /** Cantidad total de paginas del reporte. */
            int totalPaginas() {
                return inicioFila.length;
            }
        }

        @Override
        public int print(Graphics g, PageFormat pageFormat, int pageIndex) throws PrinterException {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // El diseno se recalcula en cada pagina: es una funcion pura de los datos y
                // del PageFormat, por lo que el resultado no depende del orden de llamada.
                Diseno diseno = calcularDiseno(g2, pageFormat);
                if (pageIndex < 0 || pageIndex >= diseno.totalPaginas()) {
                    return NO_SUCH_PAGE;
                }

                double x = pageFormat.getImageableX();
                double y = pageFormat.getImageableY();
                double ancho = pageFormat.getImageableWidth();
                double alto = pageFormat.getImageableHeight();

                int desde = diseno.inicioFila[pageIndex];
                int hasta = diseno.finFila[pageIndex];

                double cursorY = y;

                if (pageIndex == 0) {
                    g2.setFont(FUENTE_TITULO);
                    g2.setColor(Color.BLACK);
                    dibujarTexto(g2, titulo, (float) x, (float) (y + ALTO_TITULO - 8));
                    g2.setFont(FUENTE_DATOS);
                    g2.setColor(Color.DARK_GRAY);
                    dibujarTexto(g2, "Generado el " + fechaGeneracion, (float) x, (float) (y + ALTO_TITULO + 6));
                    cursorY = y + ALTO_BLOQUE_TITULO;
                }

                // Encabezado de la tabla: se repite en todas las paginas.
                g2.setFont(FUENTE_ENCABEZADO);
                g2.setColor(new Color(0xDC, 0xE4, 0xEC));
                g2.fillRect((int) x, (int) cursorY, (int) ancho, diseno.altoEncabezado);
                g2.setColor(Color.BLACK);
                double cursorX = x;
                for (int i = 0; i < columnas.length; i++) {
                    String[] lineas = diseno.lineasEncabezado[i];
                    for (int l = 0; l < lineas.length; l++) {
                        dibujarTexto(g2, lineas[l], (float) (cursorX + RELLENO_HORIZONTAL),
                                (float) (cursorY + RELLENO_VERTICAL + (ALTO_LINEA * (l + 1)) - 3));
                    }
                    cursorX += diseno.anchos[i];
                }
                cursorY += diseno.altoEncabezado;

                // Filas de datos
                g2.setFont(FUENTE_DATOS);
                for (int f = desde; f < hasta; f++) {
                    double inicioFila = cursorY;
                    cursorX = x;
                    for (int c = 0; c < columnas.length; c++) {
                        String[] lineas = diseno.celdas[f][c];
                        for (int l = 0; l < lineas.length; l++) {
                            g2.setColor(Color.BLACK);
                            dibujarTexto(g2, lineas[l], (float) (cursorX + RELLENO_HORIZONTAL),
                                    (float) (inicioFila + RELLENO_VERTICAL + (ALTO_LINEA * (l + 1)) - 3));
                        }
                        cursorX += diseno.anchos[c];
                    }
                    g2.setColor(new Color(0xE0, 0xE0, 0xE0));
                    g2.drawLine((int) x, (int) (inicioFila + diseno.altoFila[f] - 2),
                            (int) (x + ancho), (int) (inicioFila + diseno.altoFila[f] - 2));
                    cursorY += diseno.altoFila[f];
                }

                if (filas.isEmpty()) {
                    g2.setFont(FUENTE_DATOS.deriveFont(Font.ITALIC));
                    g2.setColor(Color.GRAY);
                    dibujarTexto(g2, "No hay datos disponibles para este reporte.", (float) x, (float) (cursorY + 14));
                }

                // Pie de pagina
                g2.setFont(FUENTE_PIE);
                g2.setColor(Color.GRAY);
                dibujarTexto(g2, "Pagina " + (pageIndex + 1) + " de " + diseno.totalPaginas(),
                        (float) x, (float) (y + alto - 4));

                return PAGE_EXISTS;
            } finally {
                g2.dispose();
            }
        }

        /**
         * Calcula el diseno completo del reporte: anchos de columna, celdas multilinea,
         * altura de cada fila y <b>plan de paginacion explicito</b>.
         *
         * <p>El plan se obtiene acumulando los altos reales de las filas en cada pagina. Como
         * la primera pagina reserva ademas el bloque de titulo, su capacidad es distinta de la
         * de las paginas siguientes; al acumular los alturas reales, los rangos de filas de
         * cada pagina quedan siempre contiguos y sin huecos, que es exactamente lo que fallaba
         * antes (DEF-017).
         */
        private Diseno calcularDiseno(Graphics2D g2, PageFormat pageFormat) {
            double anchoDisponible = pageFormat.getImageableWidth();
            g2.setFont(FUENTE_ENCABEZADO);
            FontMetrics metricsEncabezado = g2.getFontMetrics();
            g2.setFont(FUENTE_DATOS);
            FontMetrics metricsFila = g2.getFontMetrics();
            int[] anchos = calcularAnchosColumnas(metricsEncabezado, metricsFila, anchoDisponible);

            String[][] lineasEncabezado = new String[columnas.length][];
            int maxLineasEncabezado = 1;
            for (int i = 0; i < columnas.length; i++) {
                lineasEncabezado[i] = envolver(columnas[i], anchos[i] - (2 * RELLENO_HORIZONTAL), metricsEncabezado);
                maxLineasEncabezado = Math.max(maxLineasEncabezado, lineasEncabezado[i].length);
            }
            int altoEncabezado = (maxLineasEncabezado * ALTO_LINEA) + (2 * RELLENO_VERTICAL);

            g2.setFont(FUENTE_DATOS);
            String[][][] celdas = new String[filas.size()][][];
            int[] altoFila = new int[filas.size()];
            for (int f = 0; f < filas.size(); f++) {
                Object[] fila = filas.get(f);
                celdas[f] = new String[columnas.length][];
                int maxLineas = 1;
                for (int c = 0; c < columnas.length; c++) {
                    Object valor = c < fila.length ? fila[c] : null;
                    celdas[f][c] = envolver(valor == null ? "" : String.valueOf(valor),
                            anchos[c] - (2 * RELLENO_HORIZONTAL), metricsFila);
                    maxLineas = Math.max(maxLineas, celdas[f][c].length);
                }
                altoFila[f] = Math.max(ALTO_FILA, (maxLineas * ALTO_LINEA) + (2 * RELLENO_VERTICAL));
            }

            // Plan de paginacion explicito.
            int altoPagina = (int) pageFormat.getImageableHeight();
            List<int[]> rangos = new ArrayList<>();
            int f = 0;
            while (f < filas.size()) {
                int bloqueTitulo = rangos.isEmpty() ? ALTO_BLOQUE_TITULO : 0;
                int disponible = altoPagina - bloqueTitulo - altoEncabezado - MARGEN_INFERIOR;
                int inicio = f;
                int usado = 0;
                while (f < filas.size() && (f == inicio || (usado + altoFila[f]) <= disponible)) {
                    usado += altoFila[f];
                    f++;
                }
                rangos.add(new int[]{inicio, f});
            }
            if (rangos.isEmpty()) {
                rangos.add(new int[]{0, 0});
            }

            int[] inicioFila = new int[rangos.size()];
            int[] finFila = new int[rangos.size()];
            for (int p = 0; p < rangos.size(); p++) {
                inicioFila[p] = rangos.get(p)[0];
                finFila[p] = rangos.get(p)[1];
            }
            return new Diseno(anchos, lineasEncabezado, altoEncabezado, celdas, altoFila, inicioFila, finFila);
        }

        /**
         * Punto de extension usado por las pruebas para capturar exactamente el texto que se
         * dibuja en cada pagina. El comportamiento por defecto es la escritura directa.
         *
         * @param g2    contexto grafico
         * @param texto texto a dibujar
         * @param x     coordenada horizontal
         * @param y     coordenada vertical
         */
        void dibujarTexto(Graphics2D g2, String texto, float x, float y) {
            g2.drawString(texto, x, y);
        }

        /**
         * Calcula el ancho de cada columna. Se parte del ancho natural (el del texto mas
         * largo de la columna, con su relleno) y se reparte proporcionalmente el ancho
         * disponible de la hoja, de modo que la tabla ocupe todo el ancho imprimible.
         *
         * <p>Cada columna respeta ademas un ancho minimo propio: el de su palabra mas larga
         * (del encabezado y de los datos). Con ese minimo el texto nunca se parte a mitad de
         * palabra, sino que se reparte en lineas por los espacios.
         */
        /**
         * Calcula el ancho de cada columna.
         *
         * <p>Cada columna recibe como minimo el ancho de su <b>palabra mas larga</b> (del
         * encabezado y de los datos), de modo que ninguna palabra se parte a mitad: el texto se
         * reparte en lineas por los espacios. El excedente del ancho disponible se reparte
         * proporcionalmente entre las columnas segun cuanto necesitan por encima de su minimo.
         */
        private int[] calcularAnchosColumnas(FontMetrics metricsEncabezado, FontMetrics metricsFila,
                                             double anchoDisponible) {
            int[] anchos = new int[columnas.length];
            if (columnas.length == 0 || anchoDisponible <= 0) {
                return anchos;
            }
            int[] minimos = new int[columnas.length];
            int[] naturales = new int[columnas.length];
            int sumaMinimos = 0;
            int sumaNaturales = 0;
            for (int i = 0; i < columnas.length; i++) {
                // Ancho natural: el del contenido mas largo de la columna (con relleno).
                int maximo = metricsEncabezado.stringWidth(columnas[i]) + (2 * RELLENO_HORIZONTAL);
                // Ancho minimo: el de la palabra mas larga, para no partir ninguna palabra.
                int minimo = 0;
                for (String palabra : columnas[i].split("\\s+")) {
                    minimo = Math.max(minimo, metricsEncabezado.stringWidth(palabra) + (2 * RELLENO_HORIZONTAL));
                }
                for (Object[] fila : filas) {
                    Object valor = i < fila.length ? fila[i] : null;
                    String texto = valor == null ? "" : String.valueOf(valor);
                    int anchoTexto = metricsFila.stringWidth(texto) + (2 * RELLENO_HORIZONTAL);
                    if (anchoTexto > maximo) {
                        maximo = anchoTexto;
                    }
                    for (String palabra : texto.split("\\s+")) {
                        minimo = Math.max(minimo, metricsFila.stringWidth(palabra) + (2 * RELLENO_HORIZONTAL));
                    }
                }
                naturales[i] = Math.min(maximo, ANCHO_MAXIMO_COLUMNA);
                minimos[i] = Math.min(minimo + 2, ANCHO_MAXIMO_COLUMNA);
                sumaMinimos += minimos[i];
                sumaNaturales += naturales[i];
            }
            if (sumaMinimos >= anchoDisponible) {
                // Los minimos no caben: se reparten de forma proporcional.
                double factor = anchoDisponible / sumaMinimos;
                for (int i = 0; i < anchos.length; i++) {
                    anchos[i] = Math.max(1, (int) Math.round(minimos[i] * factor));
                }
                return anchos;
            }
            // Se reparten los minimos y el excedente se reparte segun la demanda de cada columna.
            int excedente = (int) Math.round(anchoDisponible) - sumaMinimos;
            int[] demanda = new int[columnas.length];
            int sumaDemanda = 0;
            for (int i = 0; i < columnas.length; i++) {
                demanda[i] = Math.max(0, naturales[i] - minimos[i]);
                sumaDemanda += demanda[i];
            }
            int asignado = 0;
            for (int i = 0; i < anchos.length; i++) {
                int extra = sumaDemanda > 0 ? (int) Math.round((excedente * demanda[i]) / (double) sumaDemanda) : 0;
                anchos[i] = minimos[i] + extra;
                asignado += anchos[i];
            }
            // El redondeo puede dejar algun punto sin asignar: se reparte entre las columnas.
            int resto = (int) Math.round(anchoDisponible) - asignado;
            for (int i = 0; i < anchos.length && resto > 0; i++) {
                anchos[i]++;
                resto--;
            }
            return anchos;
        }

        /**
         * Divide un texto en las lineas que caben en el ancho indicado.
         *
         * <p>No se descarta ningun caracter: si una palabra suelta excede el ancho de la
         * columna se parte por caracteres, de modo que ningun dato del reporte se pierde.
         *
         * @param texto            texto a dividir
         * @param anchoDisponible  ancho util en puntos
         * @param metrics          metricas de la fuente en uso
         * @return lineas del texto
         */
        private static String[] envolver(String texto, int anchoDisponible, FontMetrics metrics) {
            if (texto == null) {
                return new String[]{""};
            }
            String contenido = texto.trim();
            if (contenido.isEmpty()) {
                return new String[]{""};
            }
            if (anchoDisponible <= 0 || metrics.stringWidth(contenido) <= anchoDisponible) {
                return new String[]{contenido};
            }
            List<String> lineas = new ArrayList<>();
            StringBuilder actual = new StringBuilder();
            for (String palabra : contenido.split("\\s+")) {
                if (palabra.isEmpty()) {
                    continue;
                }
                String candidato = actual.length() == 0 ? palabra : actual + " " + palabra;
                if (metrics.stringWidth(candidato) <= anchoDisponible) {
                    actual.setLength(0);
                    actual.append(candidato);
                    continue;
                }
                if (actual.length() > 0) {
                    lineas.add(actual.toString());
                    actual.setLength(0);
                }
                // Palabra mas larga que la columna: se parte por caracteres.
                StringBuilder trozo = new StringBuilder();
                for (char ch : palabra.toCharArray()) {
                    String prueba = trozo.length() == 0 ? String.valueOf(ch) : trozo.toString() + ch;
                    if (metrics.stringWidth(prueba) > anchoDisponible && trozo.length() > 0) {
                        lineas.add(trozo.toString());
                        trozo.setLength(0);
                        trozo.append(ch);
                    } else {
                        trozo.append(ch);
                    }
                }
                actual.append(trozo);
            }
            if (actual.length() > 0) {
                lineas.add(actual.toString());
            }
            if (lineas.isEmpty()) {
                lineas.add("");
            }
            return lineas.toArray(new String[0]);
        }
    }
}
