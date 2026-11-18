package com.gestionamateur.tpi.progav.vista;

import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.servicio.ConsultaPublicaService;

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
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.UnresolvedAddressException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Acceso publico anonimo de solo lectura (ADR-02 y ADR-18).
 *
 * <p>Implementa los casos de uso CU-08 (consultar fixture), CU-09 (consultar perfil publico de
 * equipos), CU-10 (consultar estadisticas de jugadores) y CU-11 (consultar avance de fases del
 * torneo).
 *
 * <p>La ventana no expone datos personales (DNI, correo electronico, hash de contrasena), ni
 * informacion de salud del jugador, ni informacion disciplinaria reservada (sanciones, reclamos y
 * expedientes del tribunal). Todas las operaciones son de lectura.
 */
public class FrmConsultaPublica extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(FrmConsultaPublica.class.getName());
    private static final int LIMITE_PROFUNDIDAD_CAUSAS = 16;

    private static final String TOOLTIP_NO_DISPONIBLE_CONEXION =
            "Datos no disponibles: sin conexión con PostgreSQL.";
    private static final String TOOLTIP_ERROR_CONSULTA =
            "No se pudieron cargar los datos por un error de consulta.";
    private static final String TOOLTIP_VACIO_VALIDO =
            "Sin registros disponibles para la consulta actual.";
    private static final String TOOLTIP_SIN_EQUIPOS_DISPONIBLES =
            "Sin registros disponibles: no hay equipos disponibles para la consulta actual.";

    enum EstadoSeccion {
        SIN_INICIAR,
        CON_DATOS,
        VACIO_VALIDO,
        NO_DISPONIBLE_CONEXION,
        ERROR_CONSULTA
    }

    enum SeccionPublica {
        TORNEOS("Torneos"),
        EQUIPOS("Equipos"),
        FIXTURE("Fixture (CU-08)"),
        PERFIL_EQUIPO("Perfil público de equipos (CU-09)"),
        ESTADISTICAS("Estadísticas de jugadores (CU-10)"),
        FASES("Avance de fases (CU-11)");

        private final String nombreVisible;

        SeccionPublica(String nombreVisible) {
            this.nombreVisible = nombreVisible;
        }

        String getNombreVisible() {
            return nombreVisible;
        }
    }

    enum TipoErrorCarga {
        CONECTIVIDAD,
        CONSULTA
    }

    private final ConsultaPublicaService servicio;
    private final Usuario usuarioActual;

    private final DefaultTableModel modeloFixture = new DefaultTableModel();
    private final DefaultTableModel modeloPerfilEquipo = new DefaultTableModel();
    private final DefaultTableModel modeloEstadisticas = new DefaultTableModel();
    private final DefaultTableModel modeloFases = new DefaultTableModel();

    private final DefaultComboBoxModel<String> modeloTorneos = new DefaultComboBoxModel<>();
    private final JComboBox<String> cmbTorneoFixture = new JComboBox<>(modeloTorneos);
    private final JComboBox<String> cmbTorneoEstadisticas = new JComboBox<>(modeloTorneos);
    private final JComboBox<String> cmbTorneoFases = new JComboBox<>(modeloTorneos);
    private final JComboBox<String> cmbEquipo = new JComboBox<>();
    private final Map<SeccionPublica, JTable> tablasPorSeccion = new EnumMap<>(SeccionPublica.class);
    private final Map<SeccionPublica, EstadoSeccion> estadoSecciones = new EnumMap<>(SeccionPublica.class);
    private List<Object[]> torneos = null;
    private List<Object[]> equipos = null;

    public FrmConsultaPublica() {
        this(null);
    }

    public FrmConsultaPublica(Usuario usuario) {
        this(usuario, new ConsultaPublicaService());
    }

    FrmConsultaPublica(Usuario usuario, ConsultaPublicaService servicio) {
        super("Consulta pública del torneo");
        this.servicio = Objects.requireNonNull(servicio, "El servicio de consulta pública es obligatorio.");
        this.usuarioActual = usuario;
        for (SeccionPublica seccion : SeccionPublica.values()) {
            estadoSecciones.put(seccion, EstadoSeccion.SIN_INICIAR);
        }
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        construirInterfaz();
        setSize(1080, 640);
        setLocationRelativeTo(null);
        inicializarDatos();
    }

    private void construirInterfaz() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        EstilosUI.estilizarVentana(panel);

        JPanel panelEncabezado = EstilosUI.crearEncabezado(
                "Consulta pública del torneo",
                "Acceso público (invitado) — consultas de solo lectura (CU-08 a CU-11)");
        panel.add(panelEncabezado, BorderLayout.NORTH);

        JTabbedPane pestanas = new JTabbedPane();
        EstilosUI.estilizarPestanas(pestanas);

        // CU-08
        pestanas.addTab("Fixture (CU-08)", panelConsulta(
                SeccionPublica.FIXTURE,
                "Torneo:", cmbTorneoFixture, "Consultar", modeloFixture,
                new String[]{"Fecha y hora", "Estado", "Local", "Visitante", "Resultado", "Sede", "Fase", "Torneo"},
                this::cargarFixture));

        // CU-09
        pestanas.addTab("Perfil público de equipos (CU-09)", panelConsulta(
                SeccionPublica.PERFIL_EQUIPO,
                "Equipo:", cmbEquipo, "Consultar", modeloPerfilEquipo,
                new String[]{"Equipo", "Categoría", "PJ", "PG", "PE", "PP", "GF", "GC", "Jugadores del plantel"},
                this::cargarPerfilEquipo));

        // CU-10
        pestanas.addTab("Estadísticas de jugadores (CU-10)", panelConsulta(
                SeccionPublica.ESTADISTICAS,
                "Torneo:", cmbTorneoEstadisticas, "Consultar", modeloEstadisticas,
                new String[]{"Jugador", "Equipo", "Goles", "Tarjetas amarillas", "Tarjetas rojas", "Partidos con incidencia"},
                this::cargarEstadisticas));

        // CU-11
        pestanas.addTab("Avance de fases (CU-11)", panelConsulta(
                SeccionPublica.FASES,
                "Torneo:", cmbTorneoFases, "Consultar", modeloFases,
                new String[]{"Torneo", "Estado del torneo", "Fase", "Orden", "Limpia amarillas", "Partidos programados", "Partidos finalizados"},
                this::cargarFases));

        panel.add(pestanas, BorderLayout.CENTER);

        JButton btnCerrar = new JButton("Cerrar");
        EstilosUI.estilizarBotonNeutro(btnCerrar);
        btnCerrar.addActionListener(e -> dispose());
        JPanel panelSur = EstilosUI.crearBarraAcciones(null, new JComponent[]{btnCerrar});
        panel.add(panelSur, BorderLayout.SOUTH);

        setContentPane(panel);
        pack();
        setMinimumSize(new Dimension(720, 480));
    }

    /** Construye un panel de consulta con selector y tabla de solo lectura. */
    private JPanel panelConsulta(SeccionPublica seccion, String etiqueta, JComboBox<String> selector, String textoBoton,
                                 DefaultTableModel modelo, String[] columnas, Runnable accion) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(EstilosUI.BG_CANVAS);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 2, 2, 2));

        JPanel panelFiltros = new JPanel(new GridBagLayout());
        EstilosUI.estilizarTarjeta(panelFiltros);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel lblSelector = new JLabel(etiqueta);
        lblSelector.setLabelFor(selector);
        EstilosUI.estilizarEtiquetaCampo(lblSelector);
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 10);
        panelFiltros.add(lblSelector, gbc);

        EstilosUI.estilizarCombo(selector);
        selector.setPreferredSize(new Dimension(340, 30));
        selector.setMinimumSize(new Dimension(180, 30));
        gbc.gridx = 1;
        gbc.weightx = 0.55;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 10);
        panelFiltros.add(selector, gbc);

        JButton btn = new JButton(textoBoton);
        EstilosUI.estilizarBotonPrimario(btn);
        btn.addActionListener(e -> accion.run());
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 0);
        panelFiltros.add(btn, gbc);

        JPanel espaciador = new JPanel();
        espaciador.setOpaque(false);
        gbc.gridx = 3;
        gbc.weightx = 0.45;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelFiltros.add(espaciador, gbc);

        panel.add(panelFiltros, BorderLayout.NORTH);

        modelo.setColumnIdentifiers(columnas);
        JTable tabla = new JTable(modelo) {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return getParent() != null && getPreferredSize().width < getParent().getWidth();
            }
        };
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setEnabled(false);
        tabla.setAutoCreateRowSorter(true);
        tablasPorSeccion.put(seccion, tabla);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setPreferredSize(new Dimension(920, 360));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JTableHeader header = tabla.getTableHeader();
        boolean reordenamientoOriginal = header != null && header.getReorderingAllowed();
        EstilosUI.estilizarTabla(tabla, scroll);
        if (header != null) {
            header.setReorderingAllowed(reordenamientoOriginal);
        }
        configurarPresentacionColumnas(tabla, columnas);

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private static void configurarPresentacionColumnas(JTable tabla, String[] columnas) {
        JTableHeader header = tabla.getTableHeader();
        Font fuenteHeader = (header != null && header.getFont() != null) ? header.getFont() : EstilosUI.FONT_LABEL;
        FontMetrics fmHeader = tabla.getFontMetrics(fuenteHeader);
        TableColumnModel columnModel = tabla.getColumnModel();
        DefaultTableCellRenderer rendererCentrado = EstilosUI.crearRendererCebra(SwingConstants.CENTER);

        for (int i = 0; i < columnas.length && i < columnModel.getColumnCount(); i++) {
            String nombre = columnas[i];
            int anchoHeader = fmHeader.stringWidth(nombre) + 34;
            int minAncho;
            int prefAncho;
            boolean centrar = false;

            switch (nombre) {
                case "PJ":
                case "PG":
                case "PE":
                case "PP":
                case "GF":
                case "GC":
                    minAncho = Math.max(anchoHeader, 48);
                    prefAncho = Math.max(minAncho + 6, 54);
                    centrar = true;
                    break;
                case "Orden":
                case "Goles":
                    minAncho = Math.max(anchoHeader, 64);
                    prefAncho = Math.max(minAncho + 10, 76);
                    centrar = true;
                    break;
                case "Resultado":
                    minAncho = Math.max(anchoHeader, 78);
                    prefAncho = Math.max(minAncho + 10, 88);
                    centrar = true;
                    break;
                case "Jugadores del plantel":
                case "Tarjetas amarillas":
                case "Tarjetas rojas":
                case "Partidos con incidencia":
                case "Limpia amarillas":
                case "Partidos programados":
                case "Partidos finalizados":
                    minAncho = Math.max(anchoHeader, 110);
                    prefAncho = Math.max(minAncho + 14, 136);
                    centrar = true;
                    break;
                case "Fecha y hora":
                    minAncho = Math.max(anchoHeader, 118);
                    prefAncho = Math.max(minAncho + 10, 128);
                    break;
                case "Estado":
                    minAncho = Math.max(anchoHeader, 98);
                    prefAncho = Math.max(minAncho + 14, 112);
                    break;
                case "Estado del torneo":
                    minAncho = Math.max(anchoHeader, 128);
                    prefAncho = Math.max(minAncho + 12, 145);
                    break;
                case "Sede":
                case "Fase":
                    minAncho = Math.max(anchoHeader, 100);
                    prefAncho = Math.max(minAncho + 20, 120);
                    break;
                case "Local":
                case "Visitante":
                case "Torneo":
                case "Categoría":
                    minAncho = Math.max(anchoHeader, 118);
                    prefAncho = Math.max(minAncho + 24, 145);
                    break;
                case "Equipo":
                case "Jugador":
                default:
                    minAncho = Math.max(anchoHeader, 130);
                    prefAncho = Math.max(minAncho + 35, 175);
                    break;
            }

            TableColumn col = columnModel.getColumn(i);
            col.setMinWidth(minAncho);
            col.setPreferredWidth(prefAncho);
            if (centrar) {
                col.setCellRenderer(rendererCentrado);
            }
        }
    }

    private void inicializarDatos() {
        for (SeccionPublica seccion : SeccionPublica.values()) {
            estadoSecciones.put(seccion, EstadoSeccion.SIN_INICIAR);
        }
        RegistroCargaInicial registro = new RegistroCargaInicial();

        try {
            cargarTorneosSelector();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al cargar torneos durante la inicialización de consulta pública.", e);
            if (clasificarError(e) == TipoErrorCarga.CONECTIVIDAD) {
                marcarNoDisponiblePorCorte(registro,
                        SeccionPublica.TORNEOS, SeccionPublica.EQUIPOS, SeccionPublica.FIXTURE,
                        SeccionPublica.PERFIL_EQUIPO, SeccionPublica.ESTADISTICAS, SeccionPublica.FASES);
                emitirNotificacionInicialConsolidada(registro);
                return;
            }
            registro.registrarErrorTecnico(SeccionPublica.TORNEOS, e);
        }

        try {
            cargarEquiposSelector();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al cargar equipos durante la inicialización de consulta pública.", e);
            if (clasificarError(e) == TipoErrorCarga.CONECTIVIDAD) {
                marcarNoDisponiblePorCorte(registro,
                        SeccionPublica.EQUIPOS, SeccionPublica.FIXTURE,
                        SeccionPublica.PERFIL_EQUIPO, SeccionPublica.ESTADISTICAS, SeccionPublica.FASES);
                emitirNotificacionInicialConsolidada(registro);
                return;
            }
            registro.registrarErrorTecnico(SeccionPublica.EQUIPOS, e);
        }

        try {
            List<Object[]> filasFixture = servicio.fixture(idTorneoSeleccionado());
            rellenar(modeloFixture, filasFixture);
            actualizarEstadoYTooltip(SeccionPublica.FIXTURE,
                    (filasFixture == null || filasFixture.isEmpty())
                            ? EstadoSeccion.VACIO_VALIDO : EstadoSeccion.CON_DATOS);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al cargar fixture (CU-08) durante la inicialización.", e);
            modeloFixture.setRowCount(0);
            if (clasificarError(e) == TipoErrorCarga.CONECTIVIDAD) {
                marcarNoDisponiblePorCorte(registro,
                        SeccionPublica.FIXTURE, SeccionPublica.PERFIL_EQUIPO,
                        SeccionPublica.ESTADISTICAS, SeccionPublica.FASES);
                emitirNotificacionInicialConsolidada(registro);
                return;
            }
            actualizarEstadoYTooltip(SeccionPublica.FIXTURE, EstadoSeccion.ERROR_CONSULTA);
            registro.registrarErrorTecnico(SeccionPublica.FIXTURE, e);
        }

        if (equipos == null) {
            modeloPerfilEquipo.setRowCount(0);
            EstadoSeccion estadoEquipos = getEstadoSeccion(SeccionPublica.EQUIPOS);
            actualizarEstadoYTooltip(SeccionPublica.PERFIL_EQUIPO,
                    estadoEquipos == EstadoSeccion.NO_DISPONIBLE_CONEXION
                            ? EstadoSeccion.NO_DISPONIBLE_CONEXION : EstadoSeccion.ERROR_CONSULTA);
        } else if (equipos.isEmpty()) {
            modeloPerfilEquipo.setRowCount(0);
            estadoSecciones.put(SeccionPublica.PERFIL_EQUIPO, EstadoSeccion.VACIO_VALIDO);
            actualizarTooltipTabla(SeccionPublica.PERFIL_EQUIPO, TOOLTIP_SIN_EQUIPOS_DISPONIBLES);
        } else {
            try {
                List<Object[]> filasPerfil = servicio.perfilPublicoDeEquipo(idEquipoSeleccionado());
                rellenar(modeloPerfilEquipo, filasPerfil);
                actualizarEstadoYTooltip(SeccionPublica.PERFIL_EQUIPO,
                        (filasPerfil == null || filasPerfil.isEmpty())
                                ? EstadoSeccion.VACIO_VALIDO : EstadoSeccion.CON_DATOS);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al cargar perfil público de equipo (CU-09) durante la inicialización.", e);
                modeloPerfilEquipo.setRowCount(0);
                if (clasificarError(e) == TipoErrorCarga.CONECTIVIDAD) {
                    marcarNoDisponiblePorCorte(registro,
                            SeccionPublica.PERFIL_EQUIPO, SeccionPublica.ESTADISTICAS, SeccionPublica.FASES);
                    emitirNotificacionInicialConsolidada(registro);
                    return;
                }
                actualizarEstadoYTooltip(SeccionPublica.PERFIL_EQUIPO, EstadoSeccion.ERROR_CONSULTA);
                registro.registrarErrorTecnico(SeccionPublica.PERFIL_EQUIPO, e);
            }
        }

        try {
            List<Object[]> filasEstadisticas = servicio.estadisticasDeJugadores(idTorneoSeleccionado());
            rellenar(modeloEstadisticas, filasEstadisticas);
            actualizarEstadoYTooltip(SeccionPublica.ESTADISTICAS,
                    (filasEstadisticas == null || filasEstadisticas.isEmpty())
                            ? EstadoSeccion.VACIO_VALIDO : EstadoSeccion.CON_DATOS);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al cargar estadísticas (CU-10) durante la inicialización.", e);
            modeloEstadisticas.setRowCount(0);
            if (clasificarError(e) == TipoErrorCarga.CONECTIVIDAD) {
                marcarNoDisponiblePorCorte(registro, SeccionPublica.ESTADISTICAS, SeccionPublica.FASES);
                emitirNotificacionInicialConsolidada(registro);
                return;
            }
            actualizarEstadoYTooltip(SeccionPublica.ESTADISTICAS, EstadoSeccion.ERROR_CONSULTA);
            registro.registrarErrorTecnico(SeccionPublica.ESTADISTICAS, e);
        }

        try {
            List<Object[]> filasFases = servicio.avanceDeFases(idTorneoSeleccionado());
            rellenar(modeloFases, filasFases);
            actualizarEstadoYTooltip(SeccionPublica.FASES,
                    (filasFases == null || filasFases.isEmpty())
                            ? EstadoSeccion.VACIO_VALIDO : EstadoSeccion.CON_DATOS);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al cargar avance de fases (CU-11) durante la inicialización.", e);
            modeloFases.setRowCount(0);
            if (clasificarError(e) == TipoErrorCarga.CONECTIVIDAD) {
                marcarNoDisponiblePorCorte(registro, SeccionPublica.FASES);
                emitirNotificacionInicialConsolidada(registro);
                return;
            }
            actualizarEstadoYTooltip(SeccionPublica.FASES, EstadoSeccion.ERROR_CONSULTA);
            registro.registrarErrorTecnico(SeccionPublica.FASES, e);
        }

        emitirNotificacionInicialConsolidada(registro);
    }

    private void marcarNoDisponiblePorCorte(RegistroCargaInicial registro, SeccionPublica... secciones) {
        registro.conexionNoDisponible = true;
        for (SeccionPublica seccion : secciones) {
            limpiarDatosSeccion(seccion);
            actualizarEstadoYTooltip(seccion, EstadoSeccion.NO_DISPONIBLE_CONEXION);
            registro.seccionesNoDisponibles.add(seccion.getNombreVisible());
        }
    }

    private void limpiarDatosSeccion(SeccionPublica seccion) {
        switch (seccion) {
            case TORNEOS:
                torneos = null;
                modeloTorneos.removeAllElements();
                break;
            case EQUIPOS:
                equipos = null;
                cmbEquipo.removeAllItems();
                break;
            case FIXTURE:
                modeloFixture.setRowCount(0);
                break;
            case PERFIL_EQUIPO:
                modeloPerfilEquipo.setRowCount(0);
                break;
            case ESTADISTICAS:
                modeloEstadisticas.setRowCount(0);
                break;
            case FASES:
                modeloFases.setRowCount(0);
                break;
        }
    }

    private void emitirNotificacionInicialConsolidada(RegistroCargaInicial registro) {
        if (!registro.hayFallos()) {
            return;
        }
        if (registro.conexionNoDisponible) {
            StringBuilder sb = new StringBuilder();
            sb.append("No hay conexión con la base de datos PostgreSQL.\n");
            sb.append("No se pudieron cargar las siguientes secciones: ")
                    .append(String.join(", ", registro.seccionesNoDisponibles))
                    .append(".\n");
            sb.append("Cuando el servidor vuelva a estar disponible, utilice el botón Consultar para reintentar.");
            if (!registro.erroresTecnicos.isEmpty()) {
                sb.append("\n\nErrores adicionales detectados:\n- ")
                        .append(String.join("\n- ", registro.erroresTecnicos));
            }
            notificarInicializacion("Consulta pública", sb.toString(), JOptionPane.WARNING_MESSAGE);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Se produjeron errores de consulta al cargar la ventana:\n- ")
                .append(String.join("\n- ", registro.erroresTecnicos));
        notificarInicializacion("Consulta pública", sb.toString(), JOptionPane.ERROR_MESSAGE);
    }

    private void cargarTorneosSelector() throws Exception {
        try {
            List<Object[]> resultado = servicio.torneos();
            torneos = resultado != null ? resultado : Collections.emptyList();
            modeloTorneos.removeAllElements();
            for (Object[] fila : torneos) {
                modeloTorneos.addElement(String.valueOf(fila[1]) + " (" + fila[2] + ")");
            }
            estadoSecciones.put(SeccionPublica.TORNEOS,
                    torneos.isEmpty() ? EstadoSeccion.VACIO_VALIDO : EstadoSeccion.CON_DATOS);
        } catch (Exception e) {
            torneos = null;
            modeloTorneos.removeAllElements();
            estadoSecciones.put(SeccionPublica.TORNEOS,
                    esErrorDeConectividad(e) ? EstadoSeccion.NO_DISPONIBLE_CONEXION : EstadoSeccion.ERROR_CONSULTA);
            throw e;
        }
    }

    private void cargarEquiposSelector() throws Exception {
        try {
            List<Object[]> resultado = servicio.equipos();
            equipos = resultado != null ? resultado : Collections.emptyList();
            cmbEquipo.removeAllItems();
            for (Object[] fila : equipos) {
                cmbEquipo.addItem(String.valueOf(fila[1]));
            }
            estadoSecciones.put(SeccionPublica.EQUIPOS,
                    equipos.isEmpty() ? EstadoSeccion.VACIO_VALIDO : EstadoSeccion.CON_DATOS);
        } catch (Exception e) {
            equipos = null;
            cmbEquipo.removeAllItems();
            estadoSecciones.put(SeccionPublica.EQUIPOS,
                    esErrorDeConectividad(e) ? EstadoSeccion.NO_DISPONIBLE_CONEXION : EstadoSeccion.ERROR_CONSULTA);
            throw e;
        }
    }

    @SuppressWarnings("unused")
    private void cargarSelectores() {
        try {
            cargarTorneosSelector();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al cargar el selector de torneos.", e);
            if (esErrorDeConectividad(e)) {
                mostrarDialogo("Consulta pública",
                        "No hay conexión con la base de datos PostgreSQL. No se pudieron cargar los torneos.",
                        JOptionPane.WARNING_MESSAGE);
            } else {
                mostrarDialogo("Consulta pública",
                        "No se pudieron cargar los torneos: " + sanitizarMensajeParaUsuario(e.getMessage()),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        try {
            cargarEquiposSelector();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al cargar el selector de equipos.", e);
            if (esErrorDeConectividad(e)) {
                mostrarDialogo("Consulta pública",
                        "No hay conexión con la base de datos PostgreSQL. No se pudieron cargar los equipos.",
                        JOptionPane.WARNING_MESSAGE);
            } else {
                mostrarDialogo("Consulta pública",
                        "No se pudieron cargar los equipos: " + sanitizarMensajeParaUsuario(e.getMessage()),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private Long idTorneoSeleccionado() {
        int idx = cmbTorneoFixture.getSelectedIndex();
        if (idx < 0 || torneos == null || idx >= torneos.size()) {
            return null;
        }
        Object id = torneos.get(idx)[0];
        return id == null ? null : ((Number) id).longValue();
    }

    private Long idEquipoSeleccionado() {
        int idx = cmbEquipo.getSelectedIndex();
        if (idx < 0 || equipos == null || idx >= equipos.size()) {
            return null;
        }
        Object id = equipos.get(idx)[0];
        return id == null ? null : ((Number) id).longValue();
    }

    private void cargarFixture() {
        ejecutarConsultaPorTorneo(
                SeccionPublica.FIXTURE,
                "el fixture",
                modeloFixture,
                () -> servicio.fixture(idTorneoSeleccionado()));
    }

    private void cargarPerfilEquipo() {
        if (equipos == null) {
            try {
                cargarEquiposSelector();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al recargar el selector de equipos antes de consultar el equipo.", e);
                modeloPerfilEquipo.setRowCount(0);
                manejarErrorConsultaInteractiva(SeccionPublica.PERFIL_EQUIPO, "el equipo", e);
                return;
            }
        }
        try {
            List<Object[]> filas = servicio.perfilPublicoDeEquipo(idEquipoSeleccionado());
            rellenar(modeloPerfilEquipo, filas);
            actualizarEstadoYTooltip(SeccionPublica.PERFIL_EQUIPO,
                    (filas == null || filas.isEmpty()) ? EstadoSeccion.VACIO_VALIDO : EstadoSeccion.CON_DATOS);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al consultar el perfil público de equipo.", e);
            modeloPerfilEquipo.setRowCount(0);
            manejarErrorConsultaInteractiva(SeccionPublica.PERFIL_EQUIPO, "el equipo", e);
        }
    }

    private void cargarEstadisticas() {
        ejecutarConsultaPorTorneo(
                SeccionPublica.ESTADISTICAS,
                "las estadísticas",
                modeloEstadisticas,
                () -> servicio.estadisticasDeJugadores(idTorneoSeleccionado()));
    }

    private void cargarFases() {
        ejecutarConsultaPorTorneo(
                SeccionPublica.FASES,
                "el avance de fases",
                modeloFases,
                () -> servicio.avanceDeFases(idTorneoSeleccionado()));
    }

    private void ejecutarConsultaPorTorneo(SeccionPublica seccion, String descripcionConsulta,
                                           DefaultTableModel modelo, ProveedorFilas proveedor) {
        if (torneos == null) {
            try {
                cargarTorneosSelector();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING,
                        "Error al recargar el selector de torneos antes de consultar " + descripcionConsulta + ".", e);
                if (esErrorDeConectividad(e)) {
                    modelo.setRowCount(0);
                    manejarErrorConsultaInteractiva(seccion, descripcionConsulta, e);
                    return;
                }
            }
        }
        try {
            List<Object[]> filas = proveedor.obtener();
            rellenar(modelo, filas);
            actualizarEstadoYTooltip(seccion,
                    (filas == null || filas.isEmpty()) ? EstadoSeccion.VACIO_VALIDO : EstadoSeccion.CON_DATOS);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al consultar " + descripcionConsulta + ".", e);
            modelo.setRowCount(0);
            manejarErrorConsultaInteractiva(seccion, descripcionConsulta, e);
        }
    }

    private void manejarErrorConsultaInteractiva(SeccionPublica seccion, String descripcionConsulta, Exception error) {
        if (esErrorDeConectividad(error)) {
            actualizarEstadoYTooltip(seccion, EstadoSeccion.NO_DISPONIBLE_CONEXION);
            mostrarDialogo("Consulta pública",
                    "No hay conexión con la base de datos PostgreSQL. No se pudo consultar " + descripcionConsulta
                            + ".\nCuando el servidor vuelva a estar disponible, intente nuevamente.",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        actualizarEstadoYTooltip(seccion, EstadoSeccion.ERROR_CONSULTA);
        String detalle = sanitizarMensajeParaUsuario(error.getMessage());
        String mensaje;
        if (detalle.isEmpty()) {
            mensaje = "No se pudo consultar " + descripcionConsulta + ".";
        } else if (detalle.startsWith("No se pudo ") || detalle.startsWith("No se pudieron ")) {
            mensaje = detalle;
        } else {
            mensaje = "No se pudo consultar " + descripcionConsulta + ": " + detalle;
        }
        int tipo = contieneSqlException(error) ? JOptionPane.ERROR_MESSAGE : JOptionPane.WARNING_MESSAGE;
        mostrarDialogo("Consulta pública", mensaje, tipo);
    }

    private void actualizarEstadoYTooltip(SeccionPublica seccion, EstadoSeccion estado) {
        estadoSecciones.put(seccion, estado);
        String tooltip;
        switch (estado) {
            case NO_DISPONIBLE_CONEXION:
                tooltip = TOOLTIP_NO_DISPONIBLE_CONEXION;
                break;
            case ERROR_CONSULTA:
                tooltip = TOOLTIP_ERROR_CONSULTA;
                break;
            case VACIO_VALIDO:
                tooltip = TOOLTIP_VACIO_VALIDO;
                break;
            case CON_DATOS:
            case SIN_INICIAR:
            default:
                tooltip = null;
                break;
        }
        actualizarTooltipTabla(seccion, tooltip);
    }

    private void actualizarTooltipTabla(SeccionPublica seccion, String tooltip) {
        JTable tabla = tablasPorSeccion.get(seccion);
        if (tabla != null) {
            tabla.setToolTipText(tooltip);
        }
    }

    private void rellenar(DefaultTableModel modelo, List<Object[]> filas) {
        modelo.setRowCount(0);
        if (filas == null) {
            return;
        }
        for (Object[] fila : filas) {
            modelo.addRow(fila);
        }
    }

    void notificarInicializacion(String titulo, String mensaje, int tipoMensaje) {
        SwingUtilities.invokeLater(() -> {
            if (isDisplayable()) {
                mostrarDialogo(titulo, mensaje, tipoMensaje);
            }
        });
    }

    void mostrarDialogo(String titulo, String mensaje, int tipoMensaje) {
        JOptionPane.showMessageDialog(this, mensaje, titulo, tipoMensaje);
    }

    EstadoSeccion getEstadoSeccion(SeccionPublica seccion) {
        return estadoSecciones.getOrDefault(seccion, EstadoSeccion.SIN_INICIAR);
    }

    static TipoErrorCarga clasificarError(Throwable error) {
        return esErrorDeConectividad(error) ? TipoErrorCarga.CONECTIVIDAD : TipoErrorCarga.CONSULTA;
    }

    static boolean esErrorDeConectividad(Throwable error) {
        if (error == null) {
            return false;
        }
        Set<Throwable> visitados = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Throwable> pendientes = new ArrayList<>();
        pendientes.add(error);
        int procesados = 0;

        while (!pendientes.isEmpty() && procesados < LIMITE_PROFUNDIDAD_CAUSAS) {
            Throwable actual = pendientes.remove(0);
            if (actual == null || !visitados.add(actual)) {
                continue;
            }
            procesados++;

            if (actual instanceof SQLException sqlEx) {
                String sqlState = sqlEx.getSQLState();
                if (sqlState != null && sqlState.startsWith("08")) {
                    return true;
                }
                if (sqlEx.getNextException() != null) {
                    pendientes.add(sqlEx.getNextException());
                }
            }
            if (actual instanceof ConnectException
                    || actual instanceof SocketException
                    || actual instanceof SocketTimeoutException
                    || actual instanceof UnknownHostException
                    || actual instanceof UnresolvedAddressException) {
                return true;
            }
            if (actual.getCause() != null) {
                pendientes.add(actual.getCause());
            }
        }
        return false;
    }

    private static boolean contieneSqlException(Throwable error) {
        Set<Throwable> visitados = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable actual = error;
        int profundidad = 0;
        while (actual != null && profundidad < LIMITE_PROFUNDIDAD_CAUSAS && visitados.add(actual)) {
            if (actual instanceof SQLException) {
                return true;
            }
            actual = actual.getCause();
            profundidad++;
        }
        return false;
    }

    static String sanitizarMensajeParaUsuario(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            return "";
        }
        String limpio = mensaje.replaceAll("(?s)(?:^|\\r?\\n)\\s*at\\s+.*$", "");
        limpio = limpio.replaceAll("(?i)jdbc:[a-z0-9_:-]+://[^\\s'\"\\)]+", "[URL JDBC omitida]");
        limpio = limpio.replaceAll("(?i)jdbc:[^\\s'\"\\)]+", "[URL JDBC omitida]");
        limpio = limpio.replaceAll("(?i)(password|passwd|pwd)\\s*=\\s*(?:\"[^\"]*\"|'[^']*'|[^&;\\s,'\")]+)", "$1=***");
        return limpio.trim();
    }

    @FunctionalInterface
    private interface ProveedorFilas {
        List<Object[]> obtener() throws Exception;
    }

    private static final class RegistroCargaInicial {
        boolean conexionNoDisponible = false;
        final List<String> seccionesNoDisponibles = new ArrayList<>();
        final List<String> erroresTecnicos = new ArrayList<>();

        void registrarErrorTecnico(SeccionPublica seccion, Exception error) {
            String detalle = sanitizarMensajeParaUsuario(error.getMessage());
            if (detalle.isEmpty()) {
                erroresTecnicos.add(seccion.getNombreVisible());
            } else {
                erroresTecnicos.add(seccion.getNombreVisible() + ": " + detalle);
            }
        }

        boolean hayFallos() {
            return conexionNoDisponible || !erroresTecnicos.isEmpty();
        }
    }

    /** Rol con el que se accedio a la consulta publica (PUBLICO en el acceso anonimo). */
    public Usuario getUsuarioActual() {
        return usuarioActual;
    }

    /** Indica si el acceso actual es el anonimo previsto por ADR-18. */
    public boolean esAccesoAnonimo() {
        return usuarioActual == null || usuarioActual.getRol() == TiposDatos.Rol.PUBLICO;
    }
}
