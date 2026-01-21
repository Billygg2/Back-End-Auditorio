package ec.edu.unibe.auditorio_backend.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password; 

    @Column(nullable = false)
    private String role;

    // Constructor vacío REQUERIDO por JPA
    public Usuario() {}

    // Constructor para pruebas manuales
    private Usuario(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // Factory method para crear usuarios (usa PasswordEncoder externo)
    public static Usuario crearUsuario(String username, String passwordEncriptado, String role) {
        return new Usuario(username, passwordEncriptado, role);
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { 
        // ASIGNAR DIRECTAMENTE - NO encriptar aquí
        this.password = password; 
    }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}