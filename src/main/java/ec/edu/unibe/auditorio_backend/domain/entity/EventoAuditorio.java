package ec.edu.unibe.auditorio_backend.domain.entity;

import ec.edu.unibe.auditorio_backend.domain.enums.EstadoEvento;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@Entity
@Table(name = "eventos_auditorio")
public class EventoAuditorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    @NotBlank(message = "El nombre del evento es obligatorio")
    @Size(min = 5, max = 200, message = "El nombre debe tener entre 5 y 200 caracteres")
    private String nombreEvento;          
    
    @Column(length = 1000)
    @Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
    private String descripcion;           
    
    @Column(nullable = false)
    @NotNull(message = "La fecha del evento es obligatoria")
    private LocalDate fechaEvento;        
    
    @Column(nullable = false)
    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;         
    
    @Column(nullable = false)
    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime horaFin;            
    
    @Column(nullable = false)
    @Min(value = 1, message = "Debe haber al menos 1 asistente")
    @Max(value = 1000, message = "No puede exceder 1000 asistentes")
    private int numeroAsistentes;         
    
    @Column(nullable = false)
    private boolean publicoExterno;      

    @Column(length = 150)
    @Size(max = 150, message = "El nombre de la empresa no puede exceder 150 caracteres")
    private String empresaPublicoExterno;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean publicoInterno;

    @Column(length = 150)
    @Size(max = 150, message = "La carrera no puede exceder 150 caracteres")
    private String carreraPublicoInterno;
    
    @Column(nullable = false)
    private boolean requiereRegistroPrevio; 

    @ManyToOne
    @JoinColumn(name = "espacio_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Espacio espacio;
    
    @Column(nullable = false, length = 100)
    @NotBlank(message = "El tipo de disposición es obligatorio")
    @Size(max = 100, message = "El tipo de disposición no puede exceder 100 caracteres")
    private String tipoDisposicion;      
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "El estado es obligatorio")
    private EstadoEvento estado = EstadoEvento.PENDIENTE;

    @ManyToOne
    @JoinColumn(name = "responsable_id", nullable = false)
    @NotNull(message = "El responsable es obligatorio")
    @JsonIgnoreProperties({"eventos"})
    private Responsable responsable;

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL)
    @Valid
    @JsonIgnoreProperties({"evento"})
    private List<Requerimiento> requerimientos;

    @ManyToOne
    @JoinColumn(name = "usuario_solicitante_id", nullable = false)
    @NotNull(message = "El usuario solicitante es obligatorio")
    @JsonIgnoreProperties({"password", "role", "eventosSolicitados"})
    private Usuario usuarioSolicitante;

    @Column(length = 500)
    @Size(max = 500, message = "El motivo de rechazo no puede exceder 500 caracteres")
    private String motivoRechazo;

    @Column(name = "documento_aprobacion_nombre", length = 255)
    private String documentoAprobacionNombre;

    @Column(name = "documento_aprobacion_tipo", length = 100)
    private String documentoAprobacionTipo;

    @Column(name = "documento_aprobacion_tamanio")
    private Long documentoAprobacionTamanio;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "documento_aprobacion_contenido", columnDefinition = "bytea")
    @JsonIgnore
    private byte[] documentoAprobacionContenido;

    // Constructores
    public EventoAuditorio() {
        this.estado = EstadoEvento.PENDIENTE;
    }

    // VALIDACIÓN PERSONALIZADA - horaInicio < horaFin
    @AssertTrue(message = "La hora de inicio debe ser antes de la hora de fin")
    public boolean isHorariosValidos() {
        return horaInicio != null && horaFin != null && horaInicio.isBefore(horaFin);
    }

    @AssertTrue(message = "Debe indicar la empresa cuando participa público externo")
    public boolean isEmpresaPublicoExternoValida() {
        return !publicoExterno
                || (empresaPublicoExterno != null && !empresaPublicoExterno.isBlank());
    }

    @AssertTrue(message = "Debe indicar la carrera cuando participa público interno")
    public boolean isCarreraPublicoInternoValida() {
        return !publicoInterno
                || (carreraPublicoInterno != null && !carreraPublicoInterno.isBlank());
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

    public String getEmpresaPublicoExterno() { return empresaPublicoExterno; }
    public void setEmpresaPublicoExterno(String empresaPublicoExterno) {
        this.empresaPublicoExterno = empresaPublicoExterno;
    }

    public boolean isPublicoInterno() { return publicoInterno; }
    public void setPublicoInterno(boolean publicoInterno) { this.publicoInterno = publicoInterno; }

    public String getCarreraPublicoInterno() { return carreraPublicoInterno; }
    public void setCarreraPublicoInterno(String carreraPublicoInterno) {
        this.carreraPublicoInterno = carreraPublicoInterno;
    }
    
    public boolean isRequiereRegistroPrevio() { return requiereRegistroPrevio; }
    public void setRequiereRegistroPrevio(boolean requiereRegistroPrevio) { this.requiereRegistroPrevio = requiereRegistroPrevio; }

    public Espacio getEspacio() { return espacio; }
    public void setEspacio(Espacio espacio) { this.espacio = espacio; }
    
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

    public String getDocumentoAprobacionNombre() { return documentoAprobacionNombre; }
    public void setDocumentoAprobacionNombre(String nombre) { this.documentoAprobacionNombre = nombre; }
    public String getDocumentoAprobacionTipo() { return documentoAprobacionTipo; }
    public void setDocumentoAprobacionTipo(String tipo) { this.documentoAprobacionTipo = tipo; }
    public Long getDocumentoAprobacionTamanio() { return documentoAprobacionTamanio; }
    public void setDocumentoAprobacionTamanio(Long tamanio) { this.documentoAprobacionTamanio = tamanio; }
    @JsonIgnore
    public byte[] getDocumentoAprobacionContenido() { return documentoAprobacionContenido; }
    @JsonIgnore
    public void setDocumentoAprobacionContenido(byte[] contenido) { this.documentoAprobacionContenido = contenido; }
    public boolean isTieneDocumentoAprobacion() {
        return documentoAprobacionContenido != null && documentoAprobacionContenido.length > 0;
    }
}
