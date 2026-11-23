package cl.nubeplay.api.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;



@Entity
@Table(name = "detalle_boleta")
public class DetalleBoleta {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "boleta_id", nullable = false)
    private Boleta boleta;


    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;


    @Column(nullable = false, length = 100)
    private String nombreProducto;


    @Column(length = 60)
    private String categoria;


    @Column(length = 1000)
    private String imagen;


    @Column(nullable = false)
    private Integer precioUnitario;


    @Column(nullable = false)
    private Integer cantidad;


    public int calcularSubtotal() {
        return precioUnitario * cantidad;
    }


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Boleta getBoleta() { return boleta; }
    public void setBoleta(Boleta boleta) { this.boleta = boleta; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }
    public Integer getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(Integer precioUnitario) { this.precioUnitario = precioUnitario; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
}

