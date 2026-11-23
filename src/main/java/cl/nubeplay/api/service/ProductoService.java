package cl.nubeplay.api.service;


import cl.nubeplay.api.dto.CatalogoDto.ProductoRequest;
import cl.nubeplay.api.dto.CatalogoDto.ProductoResponse;
import cl.nubeplay.api.exception.ApiException;
import cl.nubeplay.api.model.Categoria;
import cl.nubeplay.api.model.Producto;
import cl.nubeplay.api.repository.CategoriaRepository;
import cl.nubeplay.api.repository.DetalleBoletaRepository;
import cl.nubeplay.api.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;


@Service
public class ProductoService {


    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final DetalleBoletaRepository detalleBoletaRepository;


    public ProductoService(ProductoRepository productoRepository,
                           CategoriaRepository categoriaRepository,
                           DetalleBoletaRepository detalleBoletaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.detalleBoletaRepository = detalleBoletaRepository;
    }


    /**
     * @param categoria   
     * @param soloCriticos 
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(String categoria, boolean soloCriticos) {
        List<Producto> productos;
        if (soloCriticos) {
            productos = productoRepository.buscarStockCritico();
        } else if (categoria != null && !categoria.isBlank()) {
            productos = productoRepository.findByCategoriaValorOrderByIdAsc(categoria.trim());
        } else {
            productos = productoRepository.findAllByOrderByIdAsc();
        }
        return productos.stream().map(Mapper::producto).toList();
    }


    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return Mapper.producto(buscar(id));
    }


    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        if (productoRepository.existsByCodigoIgnoreCase(request.codigo().trim())) {
            throw ApiException.conflicto("Ya existe otro producto con ese código.", "codigo");
        }
        Producto producto = new Producto();
        aplicar(producto, request);
        return Mapper.producto(productoRepository.save(producto));
    }


    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = buscar(id);
        if (productoRepository.existsByCodigoIgnoreCaseAndIdNot(request.codigo().trim(), id)) {
            throw ApiException.conflicto("Ya existe otro producto con ese código.", "codigo");
        }
        aplicar(producto, request);
        return Mapper.producto(productoRepository.save(producto));
    }


    /** Las boletas antiguas conservan el nombre y el precio del producto eliminado. */
    @Transactional
    public void eliminar(Long id) {
        Producto producto = buscar(id);
        detalleBoletaRepository.desvincularProducto(id);
        productoRepository.delete(producto);
    }


    private void aplicar(Producto producto, ProductoRequest request) {
        Categoria categoria = categoriaRepository.findByValor(request.categoria().trim())
                .orElseThrow(() -> ApiException.solicitudInvalida("La categoría seleccionada no existe.", "categoria"));


        producto.setCodigo(request.codigo().trim().toUpperCase());
        producto.setNombre(request.nombre().trim());
        producto.setDescripcion(Mapper.limpiar(request.descripcion()));
        producto.setPrecio(request.precio());
        producto.setStock(request.stock());
        producto.setStockCritico(request.stockCritico() != null ? request.stockCritico() : 0);
        producto.setImagen(Mapper.limpiar(request.imagen()));
        producto.setCategoria(categoria);
    }


    private Producto buscar(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("El producto " + id + " no existe."));
    }
}



