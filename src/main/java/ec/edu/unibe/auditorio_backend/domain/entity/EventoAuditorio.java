package ec.edu.unibe.auditorio_backend.domain.entity;

import ec.edu.unibe.auditorio_backend.domain.enums.EstadoEvento;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "eventos_auditorio")
public class EventoAuditorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombreEvento;          
    private String descripcion;           
    
    private LocalDate fechaEvento;        
    private LocalTime horaInicio;         
    private LocalTime horaFin;            
    
    private int numeroAsistentes;         
    
    private boolean publicoExterno;      
    private boolean requiereRegistroPrevio; 
    
    private String tipoDisposicion;      
    
    @Enumerated(EnumType.STRING)
    private EstadoEvento estado = EstadoEvento.PENDIENTE;

    @ManyToOne
    @JoinColumn(name = "responsable_id")  // Responsable DEL EVENTO (organizador)
    private Responsable responsable;

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL)
    private List<Requerimiento> requerimientos; // Requerimientos técnicos

    @ManyToOne
    @JoinColumn(name = "usuario_solicitante_id")  // Usuario que hizo la solicitud
    private Usuario usuarioSolicitante;

    @Column(length = 500)
    private String motivoRechazo;

    // Constructores
    public EventoAuditorio() {
        this.estado = EstadoEvento.PENDIENTE;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getNombreEvento() { return nombreEvento; }
    public void setNombreEvento(String nombreEvento) { this.nombreEvento = nombreEvento; }
    
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    
    public LocalDate getFechaEvento() { return fechaEvento; }
    public void setFechaEvento(LocalDate fechaEvento) { this.fechaEvento = fechaEvento; }
    
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    
    public int getNumeroAsistentes() { return numeroAsistentes; }
    public void setNumeroAsistentes(int numeroAsistentes) { this.numeroAsistentes = numeroAsistentes; }
    
    public boolean isPublicoExterno() { return publicoExterno; }
    public void setPublicoExterno(boolean publicoExterno) { this.publicoExterno = publicoExterno; }
    
    public boolean isRequiereRegistroPrevio() { return requiereRegistroPrevio; }
    public void setRequiereRegistroPrevio(boolean requiereRegistroPrevio) { this.requiereRegistroPrevio = requiereRegistroPrevio; }
    
    public String getTipoDisposicion() { return tipoDisposicion; }
    public void setTipoDisposicion(String tipoDisposicion) { this.tipoDisposicion = tipoDisposicion; }
    
    public EstadoEvento getEstado() { return estado; }
    public void setEstado(EstadoEvento estado) { this.estado = estado; }
    
    public Responsable getResponsable() { return responsable; }
    public void setResponsable(Responsable responsable) { this.responsable = responsable; }
    
    public List<Requerimiento> getRequerimientos() { return requerimientos; }
    public void setRequerimientos(List<Requerimiento> requerimientos) { this.requerimientos = requerimientos; }
    
    public Usuario getUsuarioSolicitante() { return usuarioSolicitante; }
    public void setUsuarioSolicitante(Usuario usuarioSolicitante) { this.usuarioSolicitante = usuarioSolicitante; }
    
    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String motivoRechazo) { this.motivoRechazo = motivoRechazo; }
}