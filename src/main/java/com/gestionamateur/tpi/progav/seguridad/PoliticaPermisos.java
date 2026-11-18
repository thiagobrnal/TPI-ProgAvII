package com.gestionamateur.tpi.progav.seguridad;

import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.Usuario;

/**
 * Fuente única de verdad para autorización por rol de los módulos del menú
 * principal y operaciones protegidas asociadas.
 */
public final class PoliticaPermisos {

    public enum ModuloMenu {
        GESTIONAR_EQUIPOS("Gestionar Equipos", "Alta, consulta, edición y baja lógica de equipos."),
        ALTA_JUGADORES("Dar de Alta Jugadores", "Alta de jugadores habilitados y consulta según permisos del rol."),
        GESTIONAR_CANCHAS("Gestionar Canchas", "Alta, consulta, edición e inhabilitación de canchas."),
        IMPRIMIR_CREDENCIALES("Imprimir Credenciales de Jugador", "Emisión e impresión de credenciales con código de barras Code 39."),
        GESTIONAR_TORNEOS("Gestionar Torneos", "Alta, consulta, edición, fases y cancelación de torneos."),
        PROGRAMAR_PARTIDOS("Programar Partidos", "Programación de encuentros con validación de equipos y canchas activas."),
        CARGAR_ACTAS("Cargar Actas de Partido", "Carga de goles, tarjetas, incidencias y oficialización de actas."),
        POSTERGAR_SUSPENDER_PARTIDOS("Postergar / Suspender Partido", "Postergación o suspensión de encuentros programados con motivo obligatorio."),
        TRIBUNAL_DISCIPLINARIO("Tribunal Disciplinario", "Emisión de fallos, registro de sanciones y anulación disciplinaria."),
        RESOLVER_RECLAMOS("Resolver Reclamos Pendientes", "Evaluación y veredicto formal sobre reclamos y apelaciones pendientes."),
        REPORTES("Reportes", "Consulta de tablas, estadísticas, historiales y exportación a PDF/CSV."),
        CONSULTA_PUBLICA("Consulta pública", "Fixture, perfil público de equipos, estadísticas de jugadores y avance de fases (CU-08 a CU-11)."),
        GESTIONAR_USUARIOS("Gestionar Usuarios", "Alta, consulta, edición, activación y restablecimiento de credenciales de usuarios."),
        CONFIGURACION_GENERAL("Configuración General", "Consulta y actualización de parámetros reglamentarios del sistema.");

        private final String etiquetaMenu;
        private final String descripcionPermitida;

        ModuloMenu(String etiquetaMenu, String descripcionPermitida) {
            this.etiquetaMenu = etiquetaMenu;
            this.descripcionPermitida = descripcionPermitida;
        }

        public String getEtiquetaMenu() {
            return etiquetaMenu;
        }

        public String getDescripcionPermitida() {
            return descripcionPermitida;
        }
    }

    private PoliticaPermisos() {
    }

    /** Verifica si existe una identidad autenticada activa y no anónima (distinta de PUBLICO). */
    public static boolean esSesionAutenticada(Usuario usuario) {
        return usuario != null
                && usuario.getRol() != null
                && usuario.getRol() != TiposDatos.Rol.PUBLICO
                && usuario.isActivo();
    }

    /** Verifica si el usuario posee identidad autenticada con identificador persistido válido. */
    public static boolean esOperadorPersistidoValido(Usuario operador) {
        return operador != null
                && operador.getId() != null
                && operador.getId() > 0
                && operador.getRol() != null
                && operador.isActivo();
    }

    /**
     * Evalúa si el usuario (o sesión invitado cuando {@code usuario == null}) tiene permiso
     * para acceder al módulo indicado desde el menú principal.
     */
    public static boolean tienePermiso(Usuario usuario, ModuloMenu modulo) {
        if (modulo == null) {
            return false;
        }
        if (modulo == ModuloMenu.CONSULTA_PUBLICA) {
            return true;
        }
        if (usuario == null || usuario.getRol() == null || !usuario.isActivo()) {
            return false;
        }
        TiposDatos.Rol rol = usuario.getRol();
        boolean esAdmin = rol == TiposDatos.Rol.ADMINISTRADOR;
        boolean esOrganizador = esAdmin || rol == TiposDatos.Rol.ORGANIZADOR;
        boolean esArbitro = rol == TiposDatos.Rol.ARBITRO;
        boolean esCapitan = rol == TiposDatos.Rol.CAPITAN;

        switch (modulo) {
            case GESTIONAR_EQUIPOS:
            case ALTA_JUGADORES:
                return esOrganizador || esCapitan;
            case GESTIONAR_CANCHAS:
            case IMPRIMIR_CREDENCIALES:
            case GESTIONAR_TORNEOS:
            case PROGRAMAR_PARTIDOS:
            case POSTERGAR_SUSPENDER_PARTIDOS:
            case TRIBUNAL_DISCIPLINARIO:
            case RESOLVER_RECLAMOS:
            case REPORTES:
                return esOrganizador;
            case CARGAR_ACTAS:
                return esOrganizador || esArbitro;
            case GESTIONAR_USUARIOS:
            case CONFIGURACION_GENERAL:
                return esAdmin;
            default:
                return false;
        }
    }

    /**
     * Evalúa permiso para ejecutar operaciones administrativas/protegidas en capa de servicio,
     * exigiendo operador no nulo, activo, con identificador persistido válido y rol habilitado.
     */
    public static boolean tienePermisoOperacion(Usuario operador, ModuloMenu modulo) {
        if (modulo == ModuloMenu.CONSULTA_PUBLICA) {
            return true;
        }
        return esOperadorPersistidoValido(operador) && tienePermiso(operador, modulo);
    }

    /** Verifica si la acción transversal Cerrar Sesión debe habilitarse en pantalla. */
    public static boolean puedeCerrarSesion(Usuario usuario) {
        return esSesionAutenticada(usuario);
    }

    /** Devuelve un mensaje explicativo no sensible para tooltips del menú principal. */
    public static String obtenerTooltip(Usuario usuario, ModuloMenu modulo) {
        if (modulo == null) {
            return null;
        }
        if (tienePermiso(usuario, modulo)) {
            return modulo.getDescripcionPermitida();
        }
        if (usuario == null || usuario.getRol() == null) {
            return "No disponible en acceso invitado: requiere iniciar sesión con un rol autorizado.";
        }
        if (!usuario.isActivo()) {
            return "No disponible: la cuenta se encuentra inactiva.";
        }
        return "No disponible para tu rol de usuario (" + usuario.getRol().name() + ").";
    }
}
