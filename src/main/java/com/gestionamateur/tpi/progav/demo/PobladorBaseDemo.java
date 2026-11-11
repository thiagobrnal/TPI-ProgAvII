package com.gestionamateur.tpi.progav.demo;

import com.gestionamateur.tpi.progav.dao.CanchaDAO;
import com.gestionamateur.tpi.progav.dao.CategoriaDAO;
import com.gestionamateur.tpi.progav.dao.ConexionBD;
import com.gestionamateur.tpi.progav.dao.FaseDAO;
import com.gestionamateur.tpi.progav.dao.SolicitudInscripcionDAO;
import com.gestionamateur.tpi.progav.dao.UsuarioDAO;
import com.gestionamateur.tpi.progav.modelo.Arbitro;
import com.gestionamateur.tpi.progav.modelo.Cancha;
import com.gestionamateur.tpi.progav.modelo.Categoria;
import com.gestionamateur.tpi.progav.modelo.Equipo;
import com.gestionamateur.tpi.progav.modelo.Fase;
import com.gestionamateur.tpi.progav.modelo.Partido;
import com.gestionamateur.tpi.progav.modelo.SistemaLogs;
import com.gestionamateur.tpi.progav.modelo.SolicitudInscripcion;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.Torneo;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.servicio.AuthService;
import com.gestionamateur.tpi.progav.servicio.EquipoService;
import com.gestionamateur.tpi.progav.servicio.JugadorService;
import com.gestionamateur.tpi.progav.servicio.PartidoService;
import com.gestionamateur.tpi.progav.servicio.TorneoService;

import java.sql.Connection;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Utilidad de aprovisionamiento de datos sintéticos mínimos de demostración para R1.
 * Utiliza estrictamente la capa de servicios y DAOs de la aplicación, respetando
 * las validaciones de negocio, hashing BCrypt, consistencia relacional y logs.
 */
public class PobladorBaseDemo {

    public static void main(String[] args) {
        System.out.println("=====================================================================");
        System.out.println(" TPI-PA2 — Aprovisionamiento de Datos Sintéticos Mínimos (R1 Demo)   ");
        System.out.println("=====================================================================");

        try (Connection conn = ConexionBD.obtenerConexion()) {
            System.out.println("Conexión exitosa a: " + conn.getMetaData().getURL());

            // 1. Verificar usuarios semilla existentes
            UsuarioDAO usuarioDAO = new UsuarioDAO();
            Usuario admin = usuarioDAO.buscarPorNombreUsuario(conn, "admin");
            Usuario cap1 = usuarioDAO.buscarPorNombreUsuario(conn, "capitan1");
            Usuario arb1 = usuarioDAO.buscarPorNombreUsuario(conn, "arbitro1");

            if (admin == null || cap1 == null || arb1 == null) {
                throw new IllegalStateException("Faltan usuarios semilla en la base demo. Verifique la ejecución de seed_demo_accounts.sql.");
            }

            // 2. Registrar segundo capitán de demostración (capitan2) si no existe
            Usuario cap2 = usuarioDAO.buscarPorNombreUsuario(conn, "capitan2");
            if (cap2 == null) {
                cap2 = new Usuario("capitan2", "Cap123!", "capitan2@torneos.ugd.edu.ar", "Lucas", "Capitán", TiposDatos.Rol.CAPITAN);
                String hashBcrypt = AuthService.hashearBCrypt("Cap123!");
                Long idCap2 = usuarioDAO.insertar(conn, cap2, hashBcrypt, 4);
                cap2.setId(idCap2);
                System.out.println("[OK] Segundo capitán creado: capitan2 (ID: " + idCap2 + ")");
            } else {
                System.out.println("[INFO] Segundo capitán ya existe: capitan2 (ID: " + cap2.getId() + ")");
            }

            // 3. Registrar Cancha de Demostración si no existe
            CanchaDAO canchaDAO = new CanchaDAO();
            Cancha cancha = null;
            List<Cancha> canchasExistentes = canchaDAO.buscarTodas(conn);
            for (Cancha c : canchasExistentes) {
                if ("Predio Central UGD (Demo)".equalsIgnoreCase(c.getNombre())) {
                    cancha = c;
                    break;
                }
            }
            if (cancha == null) {
                cancha = new Cancha("Predio Central UGD (Demo)", "Av. Rademacher 3985, Posadas", "Césped Natural");
                Long idCancha = canchaDAO.insertar(conn, cancha);
                cancha.setIdCancha(idCancha);
                System.out.println("[OK] Cancha registrada: Predio Central UGD (Demo) (ID: " + idCancha + ")");
            } else {
                System.out.println("[INFO] Cancha ya existe: Predio Central UGD (Demo) (ID: " + cancha.getIdCancha() + ")");
            }

            // 4. Registrar dos Equipos de Demostración
            EquipoService equipoService = new EquipoService();
            CategoriaDAO categoriaDAO = new CategoriaDAO();
            Categoria catLibre = categoriaDAO.buscarPorId(conn, 2L); // Libre Masculino

            Equipo eq1 = null;
            Equipo eq2 = null;
            com.gestionamateur.tpi.progav.dao.EquipoDAO eqDAO = new com.gestionamateur.tpi.progav.dao.EquipoDAO();
            List<Equipo> equiposExistentes = eqDAO.buscarTodos(conn);
            for (Equipo e : equiposExistentes) {
                if ("Deportivo Posadas (Demo)".equalsIgnoreCase(e.getNombre())) {
                    eq1 = e;
                } else if ("Atlético Guaraní (Demo)".equalsIgnoreCase(e.getNombre())) {
                    eq2 = e;
                }
            }

            if (eq1 == null) {
                eq1 = equipoService.registrarEquipo("Deportivo Posadas (Demo)", "DEP", catLibre.getId(), "Rojo y Blanco", cancha, cap1.getId(), admin);
                System.out.println("[OK] Equipo 1 registrado: Deportivo Posadas (Demo) (ID: " + eq1.getIdEquipo() + ")");
            } else {
                System.out.println("[INFO] Equipo 1 ya existe: Deportivo Posadas (Demo) (ID: " + eq1.getIdEquipo() + ")");
            }

            if (eq2 == null) {
                eq2 = equipoService.registrarEquipo("Atlético Guaraní (Demo)", "GUA", catLibre.getId(), "Franja Azul", cancha, cap2.getId(), admin);
                System.out.println("[OK] Equipo 2 registrado: Atlético Guaraní (Demo) (ID: " + eq2.getIdEquipo() + ")");
            } else {
                System.out.println("[INFO] Equipo 2 ya existe: Atlético Guaraní (Demo) (ID: " + eq2.getIdEquipo() + ")");
            }

            // Aprobar solicitudes de inscripción si están pendientes
            SolicitudInscripcionDAO solDAO = new SolicitudInscripcionDAO();
            List<SolicitudInscripcion> solicitudes = solDAO.buscarPendientes(conn);
            for (SolicitudInscripcion sol : solicitudes) {
                if ("PENDIENTE".equalsIgnoreCase(sol.getEstado())) {
                    equipoService.resolverSolicitudInscripcion(sol.getIdSolicitud(), true, "Inscripción aprobada por el comité organizador", admin);
                    System.out.println("[OK] Solicitud de inscripción #" + sol.getIdSolicitud() + " aprobada para equipo: " + (sol.getEquipoSolicitante() != null ? sol.getEquipoSolicitante().getNombre() : "ID " + sol.getIdSolicitud()));
                }
            }

            // 5. Registrar Jugadores para ambos equipos
            JugadorService jugadorService = new JugadorService();
            Calendar cal = Calendar.getInstance();

            // Jugadores Equipo 1 (Deportivo Posadas)
            registrarJugadorSiNoExiste(jugadorService, "Martín Benítez (Demo)", 38111222, fecha(cal, 1995, 4, 12), "Delantero", eq1.getIdEquipo(), admin);
            registrarJugadorSiNoExiste(jugadorService, "Lucas Rodríguez (Demo)", 39333444, fecha(cal, 1996, 8, 20), "Mediocampista", eq1.getIdEquipo(), admin);
            registrarJugadorSiNoExiste(jugadorService, "Esteban Gómez (Demo)", 37555666, fecha(cal, 1994, 11, 5), "Defensor", eq1.getIdEquipo(), admin);

            // Jugadores Equipo 2 (Atlético Guaraní)
            registrarJugadorSiNoExiste(jugadorService, "Gonzalo Silvero (Demo)", 38777888, fecha(cal, 1995, 2, 18), "Arquero", eq2.getIdEquipo(), admin);
            registrarJugadorSiNoExiste(jugadorService, "Facundo Romero (Demo)", 40111333, fecha(cal, 1997, 6, 25), "Delantero", eq2.getIdEquipo(), admin);
            registrarJugadorSiNoExiste(jugadorService, "Santiago Ruiz (Demo)", 39555777, fecha(cal, 1996, 9, 14), "Defensor", eq2.getIdEquipo(), admin);

            // 6. Registrar Torneo de Demostración si no existe
            TorneoService torneoService = new TorneoService();
            Torneo torneo = null;
            List<Torneo> torneosExistentes = torneoService.listarTodos();
            for (Torneo t : torneosExistentes) {
                if ("Torneo Apertura 2026 — R1 Demo".equalsIgnoreCase(t.getNombre())) {
                    torneo = t;
                    break;
                }
            }
            if (torneo == null) {
                torneo = torneoService.crearTorneo("Torneo Apertura 2026 — R1 Demo", "LIGA", 8, fecha(cal, 2026, 6, 1), admin);
                System.out.println("[OK] Torneo registrado: Torneo Apertura 2026 — R1 Demo (ID: " + torneo.getIdTorneo() + ")");
            } else {
                System.out.println("[INFO] Torneo ya existe: Torneo Apertura 2026 — R1 Demo (ID: " + torneo.getIdTorneo() + ")");
            }

            // Obtener primera fase del torneo
            FaseDAO faseDAO = new FaseDAO();
            List<Fase> fases = faseDAO.buscarPorTorneo(conn, torneo.getIdTorneo());
            if (fases.isEmpty()) {
                throw new IllegalStateException("El torneo no posee fase creada.");
            }
            Fase fase1 = fases.get(0);

            // 7. Registrar Partido Programado si no existe
            PartidoService partidoService = new PartidoService();
            com.gestionamateur.tpi.progav.dao.PartidoDAO partidoDAO = new com.gestionamateur.tpi.progav.dao.PartidoDAO();
            List<Partido> partidosExistentes = partidoDAO.buscarPorFase(conn, fase1.getIdFase());

            if (partidosExistentes.isEmpty()) {
                cal.set(2026, Calendar.NOVEMBER, 13, 18, 0, 0);
                Date fechaPartido = cal.getTime();
                Partido p = partidoService.programarPartido(fase1.getIdFase(), eq1.getIdEquipo(), eq2.getIdEquipo(),
                        cancha.getIdCancha(), arb1.getId(), fechaPartido, 90, admin);
                System.out.println("[OK] Partido programado exitosamente (ID: " + p.getIdPartido() + ")");
                System.out.println("     Local: " + eq1.getNombre() + " vs Visitante: " + eq2.getNombre());
                System.out.println("     Sede: " + cancha.getNombre() + " | Árbitro asignado: Roberto Pérez (arbitro1)");
            } else {
                System.out.println("[INFO] Partido ya programado previamente en fase 1 (ID: " + partidosExistentes.get(0).getIdPartido() + ")");
            }

            System.out.println("\n=====================================================================");
            System.out.println(" ¡APROVISIONAMIENTO DE DEMOSTRACIÓN COMPLETADO CON ÉXITO!            ");
            System.out.println("=====================================================================");

        } catch (Exception e) {
            System.err.println("ERROR al aprovisionar base de demostración: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Date fecha(Calendar cal, int anio, int mes, int dia) {
        cal.set(anio, mes - 1, dia, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private static void registrarJugadorSiNoExiste(JugadorService js, String nombre, int dni, Date fechaNac, String pos, Long idEq, Usuario admin) {
        try {
            js.registrarJugador(nombre, dni, fechaNac, pos, idEq, admin);
            System.out.println("[OK] Jugador registrado: " + nombre + " (DNI: " + dni + ")");
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Ya existe un jugador")) {
                System.out.println("[INFO] Jugador ya registrado: " + nombre + " (DNI: " + dni + ")");
            } else {
                System.err.println("Fallo al registrar jugador " + nombre + ": " + e.getMessage());
            }
        }
    }
}
