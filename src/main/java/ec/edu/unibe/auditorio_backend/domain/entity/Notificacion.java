package ec.edu.unibe.auditorio_backend.domain.entity;

import ec.edu.unibe.auditorio_backend.domain.enums.TipoNotificacion;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones", indexes = {
        @Index(name = "idx_notificacion_destinatario_leida", columnList = "destinatario_id, leida"),
        @Index(name = "idx_notificacion_creada", columnList = "creada_en")
})
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinatario_id", nullable = false)
    private Usuario destinatario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id")
    private EventoAuditorio evento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoNotificacion tipo;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 600)
    private String mensaje;

    @Column(nullable = false)
    private boolean leida = false;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private LocalDateTime creadaEn;

    @PrePersist
    public void antesDeGuardar() {
        if (creadaEn == null) {
            creadaEn = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getDestinatario() { return destinatario; }
    public void setDestinatario(Usuario destinatario) { this.destinatario = destinatario; }
    public EventoAuditorio getEvento() { return evento; }
    public void setEvento(EventoAuditorio evento) { this.evento = evento; }
    public TipoNotificacion getTipo() { return tipo; }
    public void setTipo(TipoNotificacion tipo) { this.tipo = tipo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
    public void setCreadaEn(LocalDateTime creadaEn) { this.creadaEn = creadaEn; }
}
