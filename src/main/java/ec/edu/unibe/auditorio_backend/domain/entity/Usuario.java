package ec.edu.unibe.auditorio_backend.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 10)
    @NotBlank(message = "La cédula es obligatoria")
    @Size(min = 10, max = 10, message = "La cédula debe tener 10 dígitos")
    @Pattern(regexp = "^[0-9]*$", message = "La cédula debe contener solo números")
    private String username; // Cédula (ej: 1722680335)

    @Column(nullable = false)
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    @Column(nullable = false, length = 20)
    @NotBlank(message = "El rol es obligatorio")
    @Pattern(regexp = "^(USER|ADMIN)$", message = "El rol debe ser USER o ADMIN")
    private String role;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 100, message = "El apellido debe tener entre 2 y 100 caracteres")
    private String apellido;

    @Column(unique = true, nullable = false, length = 100)
    @NotBlank(message = "El correo institucional es obligatorio")
    @Email(message = "Debe ser un correo electrónico válido")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@unibe\\.edu\\.ec$", 
             message = "El correo debe terminar en @unibe.edu.ec")
    private String correoInstitucional;

    @Column(nullable = false, length = 15)
    @NotBlank(message = "El teléfono es obligatorio")
    @Size(min = 10, max = 15, message = "El teléfono debe tener entre 10 y 15 dígitos")
    @Pattern(regexp = "^[0-9+]*$", message = "El teléfono debe contener solo números y el signo +")
    private String telefono;

    // Constructor vacío REQUERIDO por JPA
    public Usuario() {}

    // Factory method actualizado
    public static Usuario crearUsuario(String username, String passwordEncriptado, String role,
                                       String nombre, String apellido, String correoInstitucional, String telefono) {
        Usuario usuario = new Usuario();
        usuario.username = username;
        usuario.password = passwordEncriptado;
        usuario.role = role;
        usuario.nombre = nombre;
        usuario.apellido = apellido;
        usuario.correoInstitucional = correoInstitucional;
        usuario.telefono = telefono;
        return usuario;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
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