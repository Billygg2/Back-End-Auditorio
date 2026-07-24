package ec.edu.unibe.auditorio_backend.domain.entity;

import ec.edu.unibe.auditorio_backend.domain.enums.RolUsuario;
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
    @Enumerated(EnumType.STRING)
    @NotNull(message = "El rol es obligatorio")
    private RolUsuario role;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[\\p{L}]+$", message = "El nombre debe ser una sola palabra y contener solo letras")
    private String nombre;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 100, message = "El apellido debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[\\p{L}]+$", message = "El apellido debe ser una sola palabra y contener solo letras")
    private String apellido;

    @Column(unique = true, nullable = false, length = 100)
    @NotBlank(message = "El correo institucional es obligatorio")
    @Email(message = "Debe ser un correo electrónico válido")
    private String correoInstitucional;

    @Column(unique = true, nullable = false, length = 10)
    @NotBlank(message = "El teléfono es obligatorio")
    @Size(min = 10, max = 10, message = "El teléfono debe tener 10 dígitos")
    @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe contener solo números")
    private String telefono;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean activo = true;

    @Column(name = "debe_cambiar_password", nullable = false, columnDefinition = "boolean default false")
    private boolean debeCambiarPassword = false;

    // Constructor vacío REQUERIDO por JPA
    public Usuario() {}

    // Factory method actualizado
    public static Usuario crearUsuario(String username, String passwordEncriptado, RolUsuario role,
                                       String nombre, String apellido, String correoInstitucional, String telefono) {
        Usuario usuario = new Usuario();
        usuario.username = username;
        usuario.password = passwordEncriptado;
        usuario.role = role;
        usuario.nombre = nombre;
        usuario.apellido = apellido;
        usuario.correoInstitucional = correoInstitucional;
        usuario.telefono = telefono;
        usuario.activo = true;
        usuario.debeCambiarPassword = false;
        return usuario;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public RolUsuario getRole() { return role; }
    public void setRole(RolUsuario role) { this.role = role; }
    
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

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public boolean isDebeCambiarPassword() { return debeCambiarPassword; }
    public void setDebeCambiarPassword(boolean debeCambiarPassword) {
        this.debeCambiarPassword = debeCambiarPassword;
    }
}
