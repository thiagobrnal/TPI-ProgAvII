package com.gestionamateur.tpi.progav.modelo;

import java.util.logging.Logger;

public class Usuario {
    
    // 1. Logger para auditoría de seguridad y accesos
    protected static final Logger LOGGER = Logger.getLogger(Usuario.class.getName());

    protected Long id;
    protected String usuario;
    protected String contraseña;
    protected String mail;
    protected String nombre;
    protected String apellido;
    protected TiposDatos.Rol rol;

    // Constructor vacío (Obligatorio para JPA)
    public Usuario() {}

    // 2. Constructor completo: Nace delegando en los setters estrictos
    public Usuario(String usuario, String contraseña, String mail, String nombre, String apellido, TiposDatos.Rol rol) throws TorneoExcepcion {
        setUsuario(usuario);
        setContraseña(contraseña);
        setMail(mail);
        setNombre(nombre);
        setApellido(apellido);
        setRol(rol);
        
        LOGGER.info("Cuenta de usuario creada con éxito: " + this.usuario + " (Rol: " + this.rol.name() + ")");
    }

    // --- COMPORTAMIENTO DE DOMINIO RICO (Seguridad) ---
    
    public boolean verificarCredenciales(String usr, String clave) {
        if (usr == null || clave == null) {
            LOGGER.warning("Intento de acceso fallido: Se enviaron credenciales nulas para el usuario " + this.usuario);
            return false;
        }
        
        boolean accesoConcedido = this.usuario.equals(usr) && this.contraseña.equals(clave);
        
        if (accesoConcedido) {
            LOGGER.info("Acceso concedido al sistema para el usuario: " + this.usuario);
        } else {
            LOGGER.warning("Intento de acceso denegado: Credenciales incorrectas para el usuario " + this.usuario);
        }
        
        return accesoConcedido;
    }

    // --- GETTERS Y SETTERS BLINDADOS ---

    public Long getId() { return id; } 
    public void setId(Long id) { this.id = id; }
    
    public String getUsuario() { return usuario; }
    
    public void setUsuario(String usuario) throws TorneoExcepcion { 
        if (usuario == null || usuario.trim().isEmpty()) {
            LOGGER.warning("Validación fallida: Intento de crear cuenta sin nombre de usuario (alias).");
            throw new TorneoExcepcion("El nombre de usuario para el login no puede estar en blanco.");
        }
        // Regla: No permitir espacios en el nombre de usuario
        if (usuario.contains(" ")) {
            throw new TorneoExcepcion("El nombre de usuario para el login no puede contener espacios.");
        }
        this.usuario = usuario; 
    }
    
    public String getContraseña() { return contraseña; }
    
    public void setContraseña(String contraseña) throws TorneoExcepcion { 
        if (contraseña == null || contraseña.trim().isEmpty()) {
            LOGGER.severe("Validación de Seguridad fallida: Intento de guardar una contraseña en blanco.");
            throw new TorneoExcepcion("La contraseña es obligatoria y no puede estar vacía.");
        }
        if (contraseña.length() < 3) {
            throw new TorneoExcepcion("Por motivos de seguridad, la contraseña debe tener al menos 3 caracteres.");
        }
        this.contraseña = contraseña; 
    }
    
    public String getMail() { return mail; }
    
    // Validación de formato básico de correo electrónico
    public void setMail(String mail) throws TorneoExcepcion { 
        if (mail == null || mail.trim().isEmpty()) {
            throw new TorneoExcepcion("El correo electrónico es un dato obligatorio de contacto.");
        }
        if (!mail.contains("@") || !mail.contains(".")) {
            LOGGER.warning("Validación fallida: Formato de correo electrónico inválido (" + mail + ").");
            throw new TorneoExcepcion("El correo electrónico ingresado no tiene un formato válido (Ej: usuario@mail.com).");
        }
        this.mail = mail; 
    }
    
    public String getNombre() { return nombre; }
    
    public void setNombre(String nombre) throws TorneoExcepcion { 
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new TorneoExcepcion("El nombre real de la persona es obligatorio.");
        }
        this.nombre = nombre; 
    }
    
    public String getApellido() { return apellido; }
    
    public void setApellido(String apellido) throws TorneoExcepcion { 
        if (apellido == null || apellido.trim().isEmpty()) {
            throw new TorneoExcepcion("El apellido de la persona es obligatorio.");
        }
        this.apellido = apellido; 
    }
    
    public TiposDatos.Rol getRol() { return rol; }
    
    public void setRol(TiposDatos.Rol rol) throws TorneoExcepcion { 
        if (rol == null) {
            LOGGER.severe("Validación de Seguridad fallida: Creación de usuario sin rol definido.");
            throw new TorneoExcepcion("Es obligatorio asignar un Rol al usuario para definir sus permisos en el sistema.");
        }
        this.rol = rol; 
    }
}