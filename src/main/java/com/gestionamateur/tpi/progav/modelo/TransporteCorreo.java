package com.gestionamateur.tpi.progav.modelo;

/**
 * Transporte de correo electronico (CU-03.2).
 *
 * <p>La implementacion concreta se inyecta en el servicio de notificaciones para que las pruebas
 * puedan usar un doble en memoria y para que la aplicacion funcione sin un servidor SMTP
 * configurado (en ese caso el transporte informa que no esta configurado y la notificacion queda
 * registrada como no despachada, sin fallar la operacion deportiva).
 */
public interface TransporteCorreo {

    /**
     * Envia un mensaje.
     *
     * @param destinatario direccion de correo del destinatario
     * @param asunto      asunto del mensaje
     * @param cuerpo       cuerpo del mensaje
     * @return true si el mensaje fue aceptado por el servidor de correo
     * @throws Exception si el envio fallo de forma irrecuperable
     */
    boolean enviar(String destinatario, String asunto, String cuerpo) throws Exception;

    /** Indica si el transporte tiene configuracion suficiente para despachar mensajes. */
    boolean estaConfigurado();

    /** Descripcion del transporte para la bitacora (sin credenciales). */
    String descripcion();
}
