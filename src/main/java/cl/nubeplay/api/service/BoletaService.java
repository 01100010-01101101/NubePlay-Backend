package cl.nubeplay.api.service;


import cl.nubeplay.api.dto.BoletaDto.BoletaRequest;
import cl.nubeplay.api.dto.BoletaDto.BoletaResponse;
import cl.nubeplay.api.dto.BoletaDto.ItemRequest;
import cl.nubeplay.api.exception.ApiException;
import cl.nubeplay.api.model.Boleta;
import cl.nubeplay.api.model.DetalleBoleta;
import cl.nubeplay.api.model.Producto;
import cl.nubeplay.api.model.Rol;
import cl.nubeplay.api.model.Usuario;
import cl.nubeplay.api.repository.BoletaRepository;
import cl.nubeplay.api.repository.ProductoRepository;
import cl.nubeplay.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;



@Service
public class BoletaService {


    public static final String CUPON_VALIDO = "NUBEPLAY123";
    public static final double PORCENTAJE_DESCUENTO = 0.10;
    public static final Set<String> ESTADOS = Set.of("Pagada", "Enviada", "Entregada", "Anulada");
    private static final ZoneId ZONA = ZoneId.of("America/Santiago");


    private final BoletaRepository boletaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;


    public BoletaService(BoletaRepository boletaRepository, ProductoRepository productoRepository, UsuarioRepository usuarioRepository) {
        this.boletaRepository = boletaRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
    }


    @Transactional
    public BoletaResponse crear(BoletaRequest request, String correoUsuario) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correoUsuario)
                .orElseThrow(() -> ApiException.noAutorizado("La sesión ya no es válida."));


        // Si el mismo producto viene dos veces, se suman las cantidades.
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (ItemRequest item : request.items()) {
            cantidades.merge(item.productoId(), item.cantidad(), Integer::sum);
        }


        Boleta boleta = new Boleta();
        int subtotal = 0;
        for (Map.Entry<Long, Integer> entrada : cantidades.entrySet()) {
            Producto producto = productoRepository.findById(entrada.getKey())
                    .orElseThrow(() -> ApiException.solicitudInvalida(
                            "Uno de los productos del carrito ya no existe (id " + entrada.getKey() + ").", "items"));
            int cantidad = entrada.getValue();
            if (cantidad > producto.getStock()) {
                throw ApiException.conflicto("No hay stock suficiente de \"" + producto.getNombre() + "\".", "items");
            }


            producto.setStock(producto.getStock() - cantidad);


            DetalleBoleta detalle = new DetalleBoleta();
            detalle.setProducto(producto);
            detalle.setNombreProducto(producto.getNombre());
            detalle.setCategoria(producto.getCategoria().getValor());
            detalle.setImagen(producto.getImagen());
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setCantidad(cantidad);
            boleta.agregarDetalle(detalle);
            subtotal += detalle.calcularSubtotal();
        }


        String cupon = request.cupon() == null ? null : request.cupon().trim().toUpperCase();
        int descuento = 0;
        if (cupon != null && !cupon.isEmpty()) {
            if (!CUPON_VALIDO.equals(cupon)) {
                throw ApiException.solicitudInvalida("El cupón ingresado no es válido.", "cupon");
            }
            descuento = (int) Math.round(subtotal * PORCENTAJE_DESCUENTO);
        } else {
            cupon = null;
        }


        boleta.setUsuario(usuario);
        boleta.setFecha(LocalDateTime.now(ZONA));
        boleta.setEstado("Pagada");
        boleta.setCompradorNombre(request.comprador().nombre().trim());
        boleta.setCompradorApellidos(request.comprador().apellidos().trim());
        boleta.setCompradorCorreo(request.comprador().correo().trim().toLowerCase());
        boleta.setCalle(request.envio().calle().trim());
        boleta.setDepartamento(Mapper.limpiar(request.envio().departamento()));
        boleta.setRegion(request.envio().region());
        boleta.setComuna(request.envio().comuna());
        boleta.setIndicaciones(Mapper.limpiar(request.envio().indicaciones()));
        boleta.setSubtotal(subtotal);
        boleta.setDescuento(descuento);
        boleta.setCupon(cupon);
        boleta.setTotal(subtotal - descuento);


        return Mapper.boleta(boletaRepository.save(boleta));
    }


    @Transactional(readOnly = true)
    public List<BoletaResponse> listar() {
        return boletaRepository.findAllByOrderByIdDesc().stream().map(Mapper::boleta).toList();
    }


    @Transactional(readOnly = true)
    public List<BoletaResponse> misBoletas(String correo) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> ApiException.noAutorizado("La sesión ya no es válida."));
        return boletaRepository.findByUsuarioIdOrderByIdDesc(usuario.getId()).stream().map(Mapper::boleta).toList();
    }



    @Transactional(readOnly = true)
    public BoletaResponse obtener(Long id, String correo, Rol rol) {
        Boleta boleta = buscar(id);
        if (rol == Rol.CLIENTE) {
            boolean esDueno = boleta.getUsuario() != null && boleta.getUsuario().getCorreo().equalsIgnoreCase(correo);
            if (!esDueno) {
                throw ApiException.prohibido("No tienes permisos para ver esta boleta.");
            }
        }
        return Mapper.boleta(boleta);
    }



    @Transactional
    public BoletaResponse cambiarEstado(Long id, String estado) {
        Boleta boleta = buscar(id);
        String nuevo = ESTADOS.stream().filter(e -> e.equalsIgnoreCase(estado.trim())).findFirst()
                .orElseThrow(() -> ApiException.solicitudInvalida("Estado inválido. Usa: " + String.join(", ", ESTADOS) + ".", "estado"));


        if ("Anulada".equals(boleta.getEstado())) {
            throw ApiException.conflicto("La boleta ya está anulada y no puede cambiar de estado.", "estado");
        }
        if ("Anulada".equals(nuevo)) {
            for (DetalleBoleta detalle : boleta.getDetalles()) {
                Producto producto = detalle.getProducto();
                if (producto != null) {
                    producto.setStock(producto.getStock() + detalle.getCantidad());
                }
            }
        }
        boleta.setEstado(nuevo);
        return Mapper.boleta(boleta);
    }


    @Transactional
    public void eliminar(Long id) {
        boletaRepository.delete(buscar(id));
    }


    private Boleta buscar(Long id) {
        return boletaRepository.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("La boleta " + id + " no existe."));
    }
}



