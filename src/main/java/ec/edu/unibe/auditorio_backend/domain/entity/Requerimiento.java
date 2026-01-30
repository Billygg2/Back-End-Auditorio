package ec.edu.unibe.auditorio_backend.domain.entity;

import ec.edu.unibe.auditorio_backend.domain.enums.TipoRequerimiento;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "requerimientos")
public class Requerimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @NotNull(message = "El tipo de requerimiento es obligatorio")
    private TipoRequerimiento tipo;

    @Column(nullable = false)
    @Min(value = 1, message = "La cantidad mínima es 1")
    @Max(value = 100, message = "La cantidad máxima es 100")
    private int cantidad = 1;

    @Column(nullable = false)
    private boolean requerido = true;

    @ManyToOne
    @JoinColumn(name = "evento_id", nullable = false)
    @JsonIgnoreProperties({"requerimientos", "usuarioSolicitante"}) // En lugar de @JsonIgnore
    @NotNull(message = "El evento es obligatorio")
    private EventoAuditorio evento;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public TipoRequerimiento getTipo() { return tipo; }
    public void setTipo(TipoRequerimiento tipo) { this.tipo = tipo; }
    
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    
    public boolean isRequerido() { return requerido; }
    public void setRequerido(boolean requerido) { this.requerido = requerido; }
    
    public EventoAuditorio getEvento() { return evento; }
    public void setEvento(EventoAuditorio evento) { this.evento = evento; }
}