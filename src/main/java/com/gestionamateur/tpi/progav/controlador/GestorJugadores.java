package com.gestionamateur.tpi.progav.controlador;

import com.gestionamateur.tpi.progav.modelo.Jugador;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class GestorJugadores {

    private static List<Jugador> dbJugadores = new ArrayList<>();

    // CU 2: Dar de alta jugadores
    public String iniciarAltaJugador(String nombre, int dni, Date fechaNacimiento, String posicion) {
        
        // Ejecución de Sub-Caso CU 2.1: Controlar inscripción (Validaciones del sistema)
        if (!validarEdadMinima(fechaNacimiento)) {
            return "ERROR: El jugador no cumple con la edad mínima reglamentaria.";
        }
        
        if (verificarSuspensionesVigentes(dni)) {
            return "ERROR: El jugador tiene una suspensión activa en el tribunal de disciplina.";
        }
        
        // Verificar que el DNI no esté duplicado en la base de datos
        for (Jugador j : dbJugadores) {
            if (j.getDni() == dni) {
                return "ERROR: El DNI " + dni + " ya se encuentra registrado en el sistema.";
            }
        }

        try {
            // Si pasa los controles del sistema, delegamos la creación estricta al Modelo.
            // Si hay un dato inválido (ej. DNI negativo), el constructor lanzará la TorneoExcepcion.
            Jugador nuevoJugador = new Jugador(nombre, dni, fechaNacimiento, posicion, TiposDatos.EstadoJugador.ACTIVO);
            
            // Persistencia (Simulada en la lista estática)
            dbJugadores.add(nuevoJugador);
            
            return "ÉXITO: Jugador habilitado e inscripto. Listo para imprimir credencial.";
            
        } catch (TorneoExcepcion ex) {
            // Atrapamos la regla de negocio rota desde la clase Jugador y la mostramos a la Vista
            return "ERROR DE VALIDACIÓN: " + ex.getMessage();
        } catch (Exception e) {
            // Falla de seguridad para caídas inesperadas
            return "ERROR CRÍTICO: Falló la creación del jugador (" + e.getMessage() + ").";
        }
    }

    // Métodos privados para uso interno del controlador (Encapsulamiento de la lógica)
    private boolean validarEdadMinima(Date fechaNacimiento) {
        // TODO: Acá en el futuro calcularíamos si (AñoActual - AñoNacimiento) >= 16
        return fechaNacimiento != null; 
    }

    private boolean verificarSuspensionesVigentes(int dni) {
        // TODO: Acá leeríamos la base de datos de Sanciones para ver si está inhabilitado
        return false; // Simulado: por ahora nadie está suspendido
    }
}