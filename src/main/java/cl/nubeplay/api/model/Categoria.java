package cl.nubeplay.api.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;



@Entity
@Table(name = "categoria")
public class Categoria {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    
    @Column(nullable = false, unique = true, length = 60)
    private String valor;


    @Column(nullable = false, length = 50)
    private String nombre;


    @Column(length = 200)
    private String descripcion;


    
    @Column(length = 40)
    private String icono;


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getIcono() { return icono; }
    public void setIcono(String icono) { this.icono = icono; }
}

