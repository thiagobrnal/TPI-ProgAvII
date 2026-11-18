package com.gestionamateur.tpi.progav.modelo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Transporte de correo por SMTP simple, implementado sobre sockets sin dependencias externas.
 *
 * <p>La configuracion se obtiene de variables de entorno, de modo que ninguna credencial queda
 * escrita en el repositorio:
 * <ul>
 *   <li>{@code TPI_SMTP_HOST} (obligatorio para despachar)</li>
 *   <li>{@code TPI_SMTP_PORT} (por defecto 25)</li>
 *   <li>{@code TPI_SMTP_USER} / {@code TPI_SMTP_PASSWORD} (autenticacion AUTH LOGIN, opcional)</li>
 *   <li>{@code TPI_SMTP_FROM} (remitente; por defecto {@code noreply@torneo.local})</li>
 *   <li>{@code TPI_SMTP_STARTTLS} ({@code true} para cifrar la conexion)</li>
 * </ul>
 */
public class TransporteCorreoSmtp implements TransporteCorreo {

    private static final Logger LOGGER = Logger.getLogger(TransporteCorreoSmtp.class.getName());

    public static final String ENV_HOST = "TPI_SMTP_HOST";
    public static final String ENV_PORT = "TPI_SMTP_PORT";
    public static final String ENV_USER = "TPI_SMTP_USER";
    public static final String ENV_PASSWORD = "TPI_SMTP_PASSWORD";
    public static final String ENV_FROM = "TPI_SMTP_FROM";
    public static final String ENV_STARTTLS = "TPI_SMTP_STARTTLS";

    private static final int TIMEOUT_MILISEGUNDOS = 15000;

    private final String host;
    private final int puerto;
    private final String usuario;
    private final String password;
    private final String remitente;
    private final boolean startTls;

    public TransporteCorreoSmtp() {
        this(System.getenv(ENV_HOST),
                leerPuerto(System.getenv(ENV_PORT)),
                System.getenv(ENV_USER),
                System.getenv(ENV_PASSWORD),
                System.getenv(ENV_FROM) != null ? System.getenv(ENV_FROM) : "noreply@torneo.local",
                "true".equalsIgnoreCase(System.getenv(ENV_STARTTLS)));
    }

    public TransporteCorreoSmtp(String host, int puerto, String usuario, String password, String remitente,
                               boolean startTls) {
        this.host = host;
        this.puerto = puerto;
        this.usuario = usuario;
        this.password = password;
        this.remitente = remitente;
        this.startTls = startTls;
    }

    private static int leerPuerto(String valor) {
        try {
            return (valor == null || valor.trim().isEmpty()) ? 25 : Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return 25;
        }
    }

    @Override
    public boolean estaConfigurado() {
        return host != null && !host.trim().isEmpty();
    }

    @Override
    public String descripcion() {
        return "SMTP " + (host == null ? "(sin configurar)" : host + ":" + puerto)
                + " desde " + remitente;
    }

    @Override
    public boolean enviar(String destinatario, String asunto, String cuerpo) throws Exception {
        if (!estaConfigurado()) {
            throw new IOException("No hay servidor SMTP configurado (variable " + ENV_HOST + " ausente).");
        }
        if (destinatario == null || destinatario.trim().isEmpty()) {
            throw new IOException("El destinatario del mensaje no es valido.");
        }
        Socket base = new Socket();
        try {
            base.connect(new InetSocketAddress(host, puerto), TIMEOUT_MILISEGUNDOS);
            base.setSoTimeout(TIMEOUT_MILISEGUNDOS);
            if (startTls) {
                // STARTTLS: se superpone un socket TLS sobre el socket de texto plano.
                javax.net.ssl.SSLSocketFactory factory =
                        (javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault();
                try (Socket tls = factory.createSocket(base, host, puerto, true)) {
                    tls.setSoTimeout(TIMEOUT_MILISEGUNDOS);
                    ((javax.net.ssl.SSLSocket) tls).startHandshake();
                    return realizarIntercambio(tls, destinatario, asunto, cuerpo);
                }
            }
            return realizarIntercambio(base, destinatario, asunto, cuerpo);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Fallo el envio de correo por SMTP: " + e.getMessage());
            throw e;
        } finally {
            try {
                base.close();
            } catch (IOException ignored) {
                // El socket ya pudo cerrarse al superponer el socket TLS.
            }
        }
    }

    /** Realiza la conversacion SMTP sobre el socket dado. */
    private boolean realizarIntercambio(Socket socket, String destinatario, String asunto, String cuerpo)
            throws IOException {
        BufferedReader lector = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        Writer escritor = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);

        esperar(lector, 220);
        escribir(escritor, "EHLO " + remitente);
        esperar(lector, 250);

        if (usuario != null && !usuario.trim().isEmpty()) {
            escribir(escritor, "AUTH LOGIN");
            esperar(lector, 334);
            escribirBase64(escritor, usuario);
            esperar(lector, 334);
            escribirBase64(escritor, password == null ? "" : password);
            esperar(lector, 235);
        }

        escribir(escritor, "MAIL FROM:<" + remitente + ">");
        esperar(lector, 250);
        escribir(escritor, "RCPT TO:<" + destinatario.trim() + ">");
        esperar(lector, 250);
        escribir(escritor, "DATA");
        esperar(lector, 354);
        escribir(escritor, "Subject: " + asunto);
        escribir(escritor, "From: " + remitente);
        escribir(escritor, "To: " + destinatario.trim());
        escribir(escritor, "Date: " + new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z")
                .format(new java.util.Date()));
        escribir(escritor, "Content-Type: text/plain; charset=UTF-8");
        escribir(escritor, "");
        for (String linea : cuerpo.split("\\r?\\n")) {
            escribir(escritor, linea.startsWith(".") ? "." + linea : linea);
        }
        escribir(escritor, ".");
        esperar(lector, 250);
        escribir(escritor, "QUIT");
        return true;
    }

    private void escribir(Writer escritor, String linea) throws IOException {
        escritor.write(linea);
        escritor.write("\r\n");
        escritor.flush();
    }

    private void escribirBase64(Writer escritor, String valor) throws IOException {
        escribir(escritor, Base64.getEncoder().encodeToString(valor.getBytes(StandardCharsets.UTF_8)));
    }

    private void esperar(BufferedReader lector, int codigoEsperado) throws IOException {
        String respuesta;
        while ((respuesta = lector.readLine()) != null) {
            if (respuesta.length() >= 3) {
                int codigo = Integer.parseInt(respuesta.substring(0, 3));
                if (codigo == codigoEsperado) {
                    return;
                }
                if (codigo >= 400) {
                    throw new IOException("El servidor SMTP respondio con error: " + respuesta);
                }
            }
            if (respuesta.length() < 4 || respuesta.charAt(3) != '-') {
                break;
            }
        }
        throw new IOException("El servidor SMTP no respondio con el codigo esperado (" + codigoEsperado + ").");
    }
}
