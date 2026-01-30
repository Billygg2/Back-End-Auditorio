package ec.edu.unibe.auditorio_backend.application.auth;

public class AuthResponse {
    private String token;
    private String username;        // Cédula
    private String nombreCompleto;  
    private String role;            
    private String correoInstitucional;
    private String telefono;
    private String nombre;
    private String apellido;

    public AuthResponse(String token) {
        this.token = token;
    }

    // Constructor completo
    public AuthResponse(String token, String username, String nombre, String apellido, 
                       String role, String correoInstitucional, String telefono) {
        this.token = token;
        this.username = username;
        this.nombre = nombre;
        this.apellido = apellido;
        this.nombreCompleto = nombre + " " + apellido;
        this.role = role;
        this.correoInstitucional = correoInstitucional;
        this.telefono = telefono;
    }

    // Getters y Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getCorreoInstitucional() { return correoInstitucional; }
    public void setCorreoInstitucional(String correoInstitucional) { 
        this.correoInstitucional = correoInstitucional; 
    }
    
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
}