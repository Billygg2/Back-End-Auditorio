package ec.edu.unibe.auditorio_backend.application.auth;

import jakarta.validation.constraints.*;

public class AuthRequest {
    
    @NotBlank(message = "La cédula es obligatoria")
    @Size(min = 10, max = 10, message = "La cédula debe tener 10 dígitos")
    @Pattern(regexp = "^\\d{10}$", message = "La cédula debe contener solo números")
    private String username; // Cédula
    
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{8,64}$",
        message = "La contraseña debe incluir mayúscula, minúscula, número y símbolo"
    )
    private String password;
    
    private String role;
    
    // Nuevos campos para registro
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[\\p{L}]+$", message = "El nombre debe ser una sola palabra y contener solo letras")
    private String nombre;
    
    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 100, message = "El apellido debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[\\p{L}]+$", message = "El apellido debe ser una sola palabra y contener solo letras")
    private String apellido;
    
    @NotBlank(message = "El correo institucional es obligatorio")
    @Email(message = "Debe ser un correo electrónico válido")
    private String correoInstitucional;
    
    @NotBlank(message = "El teléfono es obligatorio")
    @Size(min = 10, max = 10, message = "El teléfono debe tener 10 dígitos")
    @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe contener solo números")
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
