package com.gestionamateur.tpi.progav.servicio;

import com.gestionamateur.tpi.progav.dao.ArbitroDAO;
import com.gestionamateur.tpi.progav.dao.ConexionBD;
import com.gestionamateur.tpi.progav.dao.RolDAO;
import com.gestionamateur.tpi.progav.dao.UsuarioDAO;
import com.gestionamateur.tpi.progav.modelo.Arbitro;
import com.gestionamateur.tpi.progav.modelo.SistemaLogs;
import com.gestionamateur.tpi.progav.modelo.TiposDatos;
import com.gestionamateur.tpi.progav.modelo.TorneoExcepcion;
import com.gestionamateur.tpi.progav.modelo.Usuario;
import com.gestionamateur.tpi.progav.seguridad.PoliticaPermisos;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Servicio administrativo de gestión integral de cuentas de usuario y perfiles arbitrales (CU-17).
 * Todas las operaciones exigen un operador ADMINISTRADOR autenticado y con ID válido.
 */
public class UsuarioService {

    private static final Logger LOGGER = Logger.getLogger(UsuarioService.class.getName());

    private final UsuarioDAO usuarioDAO;
    private final ArbitroDAO arbitroDAO;
    private final RolDAO rolDAO;

    public UsuarioService() {
        this(new UsuarioDAO(), new ArbitroDAO(), new RolDAO());
    }

    public UsuarioService(UsuarioDAO usuarioDAO, ArbitroDAO arbitroDAO, RolDAO rolDAO) {
        this.usuarioDAO = usuarioDAO;
        this.arbitroDAO = arbitroDAO;
        this.rolDAO = rolDAO;
    }

    private void validarAdministrador(Usuario operador, String accion) throws TorneoExcepcion {
        if (!PoliticaPermisos.esOperadorPersistidoValido(operador)) {
            throw new TorneoExcepcion("Acceso denegado: se requiere un Administrador autenticado con identificador válido para " + accion + ".");
        }
        if (!PoliticaPermisos.tienePermisoOperacion(operador, PoliticaPermisos.ModuloMenu.GESTIONAR_USUARIOS)) {
            throw new TorneoExcepcion("Acceso denegado: solo un Administrador autenticado puede " + accion + ".");
        }
    }

    public List<Usuario> listarUsuarios(Usuario operador) throws TorneoExcepcion {
        validarAdministrador(operador, "consultar usuarios");
        try {
            return usuarioDAO.buscarTodos();
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al consultar el listado de usuarios: " + e.getMessage(), e);
        }
    }

    public Arbitro obtenerDetalleArbitro(Long idUsuario, Usuario operador) throws TorneoExcepcion {
        validarAdministrador(operador, "consultar datos arbitrales");
        if (idUsuario == null || idUsuario <= 0) {
            return null;
        }
        try {
            return arbitroDAO.buscarPorId(idUsuario);
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error al consultar los datos del árbitro: " + e.getMessage(), e);
        }
    }

    public Usuario registrarUsuario(String alias, String contrasena, String mail, String nombre, String apellido,
                                    TiposDatos.Rol rol, String categoriaArbitral, String telefono,
                                    Usuario operador) throws TorneoExcepcion {
        validarAdministrador(operador, "registrar usuarios");
        if (rol == null || rol == TiposDatos.Rol.PUBLICO) {
            throw new TorneoExcepcion("Debe especificar un rol válido con cuenta de sistema.");
        }
        AuthService.validarPoliticaContrasena(contrasena);

        String aliasLimpio = alias == null ? "" : alias.trim();
        String mailLimpio = mail == null ? "" : mail.trim();
        String nombreLimpio = nombre == null ? "" : nombre.trim();
        String apellidoLimpio = apellido == null ? "" : apellido.trim();

        try (Connection conn = ConexionBD.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                if (usuarioDAO.buscarPorUsuario(conn, aliasLimpio) != null) {
                    throw new TorneoExcepcion("El alias \"" + aliasLimpio + "\" ya se encuentra registrado.");
                }
                if (usuarioDAO.existeMailExcluyendoId(conn, mailLimpio, null)) {
                    throw new TorneoExcepcion("Ya existe una cuenta registrada con ese correo electrónico.");
                }

                String hash = AuthService.hashearBCrypt(contrasena);
                Usuario creado;
                if (rol == TiposDatos.Rol.ARBITRO) {
                    String cat = (categoriaArbitral == null || categoriaArbitral.trim().isEmpty())
                            ? "Oficial" : categoriaArbitral.trim();
                    String tel = (telefono == null || telefono.trim().isEmpty())
                            ? "S/D" : telefono.trim();
                    Arbitro arbitro = new Arbitro(aliasLimpio, hash, mailLimpio, nombreLimpio, apellidoLimpio, cat, tel);
                    arbitroDAO.insertar(conn, arbitro, hash);
                    creado = arbitro;
                } else {
                    RolDAO.RolRegistro reg = rolDAO.buscarPorCodigo(conn, rol.name());
                    int idRol = reg != null ? reg.idRol : 4;
                    Usuario u = new Usuario(aliasLimpio, hash, mailLimpio, nombreLimpio, apellidoLimpio, rol);
                    usuarioDAO.insertar(conn, u, hash, idRol);
                    creado = u;
                }
                conn.commit();
                SistemaLogs.info(UsuarioService.class, "Usuario creado: " + aliasLimpio + " (Rol: " + rol.name() + ") por " + operador.getNombreUsuario());
                return creado;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof TorneoExcepcion) {
                    throw (TorneoExcepcion) e;
                }
                LOGGER.severe("Error al registrar usuario: " + e.getMessage());
                throw new TorneoExcepcion("No se pudo registrar el usuario: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error de conexión al registrar el usuario: " + e.getMessage(), e);
        }
    }

    public Usuario actualizarUsuario(Long idUsuario, String mail, String nombre, String apellido,
                                     TiposDatos.Rol nuevoRol, String categoriaArbitral, String telefono,
                                     Usuario operador) throws TorneoExcepcion {
        validarAdministrador(operador, "editar usuarios");
        if (idUsuario == null || idUsuario <= 0) {
            throw new TorneoExcepcion("Debe indicar un usuario válido a modificar.");
        }
        if (nuevoRol == null || nuevoRol == TiposDatos.Rol.PUBLICO) {
            throw new TorneoExcepcion("El rol PÚBLICO es anónimo y no puede asignarse a una cuenta.");
        }

        // Reutilizar validaciones de dominio sobre mail, nombre y apellido sin alterar credenciales
        Usuario validador = new Usuario();
        validador.setMail(mail != null ? mail.trim() : "");
        validador.setNombre(nombre != null ? nombre.trim() : "");
        validador.setApellido(apellido != null ? apellido.trim() : "");
        validador.setRol(nuevoRol);

        if (nuevoRol == TiposDatos.Rol.ARBITRO) {
            if (categoriaArbitral == null || categoriaArbitral.trim().isEmpty()
                    || telefono == null || telefono.trim().isEmpty()) {
                throw new TorneoExcepcion("Para un árbitro debe indicar la categoría arbitral y el teléfono.");
            }
        }

        try (Connection conn = ConexionBD.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                Usuario existente = usuarioDAO.buscarPorId(conn, idUsuario);
                if (existente == null) {
                    throw new TorneoExcepcion("El usuario indicado no existe.");
                }

                if (usuarioDAO.existeMailExcluyendoId(conn, validador.getMail(), idUsuario)) {
                    throw new TorneoExcepcion("Ya existe otra cuenta registrada con ese correo electrónico.");
                }

                if (existente.getRol() == TiposDatos.Rol.ADMINISTRADOR && nuevoRol != TiposDatos.Rol.ADMINISTRADOR) {
                    if (idUsuario.equals(operador.getId())) {
                        throw new TorneoExcepcion("Operación rechazada: no puede degradar su propio rol de Administrador en sesión activa.");
                    }
                    if (existente.isActivo() && usuarioDAO.contarAdministradoresActivos(conn) <= 1) {
                        throw new TorneoExcepcion("Operación rechazada: debe permanecer al menos un Administrador activo en el sistema.");
                    }
                }

                if (existente.getRol() == TiposDatos.Rol.ARBITRO && nuevoRol != TiposDatos.Rol.ARBITRO) {
                    if (arbitroDAO.contarPartidosAsignados(conn, idUsuario) > 0) {
                        throw new TorneoExcepcion("No se puede cambiar el rol de este árbitro porque registra partidos designados en el historial.");
                    }
                }

                RolDAO.RolRegistro reg = rolDAO.buscarPorCodigo(conn, nuevoRol.name());
                if (reg == null) {
                    throw new TorneoExcepcion("No se encontró el catálogo para el rol " + nuevoRol.name());
                }
                usuarioDAO.actualizarDatosBasicos(conn, idUsuario, validador.getMail(), validador.getNombre(), validador.getApellido(), reg.idRol);

                if (nuevoRol == TiposDatos.Rol.ARBITRO) {
                    arbitroDAO.insertarOActualizarPerfil(conn, idUsuario, categoriaArbitral.trim(), telefono.trim());
                }

                conn.commit();
                SistemaLogs.info(UsuarioService.class, "Usuario actualizado ID " + idUsuario + " por " + operador.getNombreUsuario());
                return usuarioDAO.buscarPorId(idUsuario);
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof TorneoExcepcion) {
                    throw (TorneoExcepcion) e;
                }
                LOGGER.severe("Error al actualizar usuario: " + e.getMessage());
                throw new TorneoExcepcion("No se pudo actualizar el usuario: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error de conexión al actualizar el usuario: " + e.getMessage(), e);
        }
    }

    public void cambiarEstadoCuenta(Long idUsuario, boolean activo, Usuario operador) throws TorneoExcepcion {
        validarAdministrador(operador, "cambiar el estado de cuentas");
        if (idUsuario == null || idUsuario <= 0) {
            throw new TorneoExcepcion("Debe indicar una cuenta de usuario válida.");
        }

        try (Connection conn = ConexionBD.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                Usuario objetivo = usuarioDAO.buscarPorId(conn, idUsuario);
                if (objetivo == null) {
                    throw new TorneoExcepcion("La cuenta de usuario indicada no existe.");
                }
                if (!activo) {
                    if (idUsuario.equals(operador.getId())) {
                        throw new TorneoExcepcion("Operación rechazada: no puede desactivar su propia cuenta mientras la sesión está activa.");
                    }
                    if (objetivo.getRol() == TiposDatos.Rol.ADMINISTRADOR
                            && objetivo.isActivo()
                            && usuarioDAO.contarAdministradoresActivos(conn) <= 1) {
                        throw new TorneoExcepcion("Operación rechazada: no se puede desactivar al último Administrador activo del sistema.");
                    }
                }
                usuarioDAO.actualizarActivo(conn, idUsuario, activo);
                conn.commit();
                SistemaLogs.info(UsuarioService.class, "Cuenta ID " + idUsuario + " -> activo=" + activo + " por " + operador.getNombreUsuario());
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof TorneoExcepcion) {
                    throw (TorneoExcepcion) e;
                }
                throw new TorneoExcepcion("No se pudo cambiar el estado de la cuenta: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error de conexión al actualizar estado de cuenta: " + e.getMessage(), e);
        }
    }

    public void restablecerContrasena(Long idUsuario, String nuevaContrasena, Usuario operador) throws TorneoExcepcion {
        validarAdministrador(operador, "restablecer contraseñas");
        if (idUsuario == null || idUsuario <= 0) {
            throw new TorneoExcepcion("Debe indicar una cuenta de usuario válida.");
        }
        AuthService.validarPoliticaContrasena(nuevaContrasena);

        try (Connection conn = ConexionBD.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                Usuario objetivo = usuarioDAO.buscarPorId(conn, idUsuario);
                if (objetivo == null) {
                    throw new TorneoExcepcion("La cuenta de usuario indicada no existe.");
                }
                String nuevoHash = AuthService.hashearBCrypt(nuevaContrasena);
                usuarioDAO.actualizarClaveHash(conn, idUsuario, nuevoHash);
                conn.commit();
                SistemaLogs.info(UsuarioService.class, "Restablecimiento seguro de contraseña para cuenta ID " + idUsuario + " por " + operador.getNombreUsuario());
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof TorneoExcepcion) {
                    throw (TorneoExcepcion) e;
                }
                throw new TorneoExcepcion("No se pudo restablecer la contraseña: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new TorneoExcepcion("Error de conexión al restablecer la contraseña: " + e.getMessage(), e);
        }
    }
}
