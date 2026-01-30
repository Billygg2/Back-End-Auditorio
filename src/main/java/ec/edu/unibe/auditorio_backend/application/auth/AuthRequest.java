package ec.edu.unibe.auditorio_backend.application.auth;

import jakarta.validation.constraints.*;

public class AuthRequest {
    
    @NotBlank(message = "La cédula es obligatoria")
    @Size(min = 10, max = 10, message = "La cédula debe tener 10 dígitos")
    private String username; // Cédula
    
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
    
    private String role;
    
    // Nuevos campos para registro
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;
    
    @NotBlank(message = "El correo institucional es obligatorio")
    @Email(message = "Debe ser un correo electrónico válido")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@unibe\\.edu\\.ec$", 
             message = "El correo debe terminar en @unibe.edu.ec")
    private String correoInstitucional;
    
    @NotBlank(message = "El teléfono es obligatorio")
    @Size(min = 10, max = 10, message = "El teléfono debe tener 10 dígitos")
    private String telefono;

    // Getters y Setters para todos los campos
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    
    public String getCorreoInstitucional() { return correoInstitucional; }
    public void setCorreoInstitucional(String correoInstitucional) { 
        this.correoInstitucional = correoInstitucional; 
    }
    
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
}