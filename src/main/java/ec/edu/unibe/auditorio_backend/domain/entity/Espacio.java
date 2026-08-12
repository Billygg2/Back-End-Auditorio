package ec.edu.unibe.auditorio_backend.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "espacios")
public class Espacio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    @NotBlank
    private String codigo;

    @Column(nullable = false, length = 120)
    @NotBlank
    @Size(max = 120)
    private String nombre;

    @Column(nullable = false, length = 30)
    @NotBlank
    private String bloque;

    @Column(nullable = false, length = 30)
    @NotBlank
    private String piso;

    @Column(nullable = false)
    @Min(1)
    @Max(1000)
    private int aforo;

    @Column(nullable = false)
    private boolean activo = true;

    public Espacio() {}

    public Espacio(String codigo, String nombre, String bloque, String piso, int aforo) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.bloque = bloque;
        this.piso = piso;
        this.aforo = aforo;
        this.activo = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getBloque() { return bloque; }
    public void setBloque(String bloque) { this.bloque = bloque; }
    public String getPiso() { return piso; }
    public void setPiso(String piso) { this.piso = piso; }
    public int getAforo() { return aforo; }
    public void setAforo(int aforo) { this.aforo = aforo; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
