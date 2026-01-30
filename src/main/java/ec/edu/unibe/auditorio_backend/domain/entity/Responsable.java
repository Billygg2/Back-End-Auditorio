package ec.edu.unibe.auditorio_backend.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@Entity
@Table(name = "responsables")
public class Responsable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Debe ser un correo electrónico válido")
    @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
    private String correo;

    @Column(nullable = false, length = 15)
    @NotBlank(message = "El teléfono es obligatorio")
    @Size(min = 10, max = 15, message = "El teléfono debe tener entre 10 y 15 dígitos")
    @Pattern(regexp = "^[0-9+]*$", message = "El teléfono debe contener solo números y el signo +")
    private String telefono;

    @OneToMany(mappedBy = "responsable")
    @JsonIgnoreProperties({"responsable", "usuarioSolicitante", "requerimientos"}) // En lugar de @JsonIgnore
    private List<EventoAuditorio> eventos;

    // ===== GETTERS Y SETTERS =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public List<EventoAuditorio> getEventos() { return eventos; }
    public void setEventos(List<EventoAuditorio> eventos) { this.eventos = eventos; }
}