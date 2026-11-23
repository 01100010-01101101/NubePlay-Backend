package cl.nubeplay.api.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tabla BOLETA: la orden de compra. Un USUARIO tiene muchas boletas (1:N)
 * y cada boleta tiene muchos DETALLE_BOLETA (1:N).
 * Guarda una copia de los datos del comprador y de envío al momento de la compra.
 */
@Entity
@Table(name = "boleta")
public class Boleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Puede quedar en null si el usuario se elimina; la boleta se conserva como respaldo contable. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, length = 20)
    private String estado = "Pagada";

    // Datos del comprador
    @Column(nullable = false, length = 50)
    private String compradorNombre;
    @Column(nullable = false, length = 100)
    private String compradorApellidos;
    @Column(nullable = false, length = 100)
    private String compradorCorreo;

    // Datos de envío
    @Column(nullable = false, length = 300)
    private String calle;
    @Column(length = 50)
    private String departamento;
    @Column(nullable = false, length = 100)
    private String region;
    @Column(nullable = false, length = 100)
    private String comuna;
    @Column(length = 300)
    private String indicaciones;

    // Totales (pesos chilenos, sin decimales)
    @Column(nullable = false)
    private Integer subtotal;
    @Column(nullable = false)
    private Integer descuento = 0;
    @Column(length = 30)
    private String cupon;
    @Column(nullable = false)
    private Integer total;

    @OneToMany(mappedBy = "boleta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<DetalleBoleta> detalles = new ArrayList<>();

    public void agregarDetalle(DetalleBoleta detalle) {
        detalle.setBoleta(this);
        detalles.add(detalle);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getCompradorNombre() { return compradorNombre; }
    public void setCompradorNombre(String compradorNombre) { this.compradorNombre = compradorNombre; }
    public String getCompradorApellidos() { return compradorApellidos; }
    public void setCompradorApellidos(String compradorApellidos) { this.compradorApellidos = compradorApellidos; }
    public String getCompradorCorreo() { return compradorCorreo; }
    public void setCompradorCorreo(String compradorCorreo) { this.compradorCorreo = compradorCorreo; }
    public String getCalle() { return calle; }
    public void setCalle(String calle) { this.calle = calle; }
    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getComuna() { return comuna; }
    public void setComuna(String comuna) { this.comuna = comuna; }
    public String getIndicaciones() { return indicaciones; }
    public void setIndicaciones(String indicaciones) { this.indicaciones = indicaciones; }
    public Integer getSubtotal() { return subtotal; }
    public void setSubtotal(Integer subtotal) { this.subtotal = subtotal; }
    public Integer getDescuento() { return descuento; }
    public void setDescuento(Integer descuento) { this.descuento = descuento; }
    public String getCupon() { return cupon; }
    public void setCupon(String cupon) { this.cupon = cupon; }
    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }
    public List<DetalleBoleta> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleBoleta> detalles) { this.detalles = detalles; }
}
