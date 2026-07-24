package ec.edu.unibe.auditorio_backend.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "requerimientos")
public class Requerimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // CAMBIADO: antes era @Enumerated con TipoRequerimiento
    @ManyToOne
    @JoinColumn(name = "tipo_requerimiento_id", nullable = false)
    @NotNull(message = "El tipo de requerimiento es obligatorio")
    private TipoRequerimientoEntity tipo;

    @Column(nullable = false)
    @Min(value = 1) @Max(value = 100)
    private int cantidad = 1;

    @Column(nullable = false)
    private boolean requerido = true;

    @ManyToOne
    @JoinColumn(name = "evento_id", nullable = false)
    @JsonIgnoreProperties({"requerimientos", "usuarioSolicitante"})
    @NotNull(message = "El evento es obligatorio")
    private EventoAuditorio evento;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TipoRequerimientoEntity getTipo() { return tipo; }
    public void setTipo(TipoRequerimientoEntity tipo) { this.tipo = tipo; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public boolean isRequerido() { return requerido; }
    public void setRequerido(boolean requerido) { this.requerido = requerido; }

    public EventoAuditorio getEvento() { return evento; }
    public void setEvento(EventoAuditorio evento) { this.evento = evento; }
}