package cl.nubeplay.api.service;


import cl.nubeplay.api.dto.CatalogoDto.CategoriaRequest;
import cl.nubeplay.api.dto.CatalogoDto.CategoriaResponse;
import cl.nubeplay.api.exception.ApiException;
import cl.nubeplay.api.model.Categoria;
import cl.nubeplay.api.repository.CategoriaRepository;
import cl.nubeplay.api.repository.ProductoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;


@Service
public class CategoriaService {


    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;


    public CategoriaService(CategoriaRepository categoriaRepository, ProductoRepository productoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
    }


    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAll(Sort.by("id")).stream()
                .map(c -> Mapper.categoria(c, productoRepository.countByCategoriaId(c.getId())))
                .toList();
    }


    @Transactional(readOnly = true)
    public CategoriaResponse obtener(Long id) {
        Categoria categoria = buscar(id);
        return Mapper.categoria(categoria, productoRepository.countByCategoriaId(id));
    }


    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        String nombre = request.nombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw ApiException.conflicto("Ya existe una categoría con ese nombre.", "nombre");
        }


        // El "valor" se genera desde el nombre y debe ser único: consolas, consolas-2, ...
        String base = Mapper.slug(nombre);
        String valor = base;
        for (int i = 2; categoriaRepository.existsByValor(valor); i++) {
            valor = base + "-" + i;
        }


        Categoria categoria = new Categoria();
        categoria.setValor(valor);
        aplicar(categoria, request);
        return Mapper.categoria(categoriaRepository.save(categoria), 0);
    }


    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscar(id);
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(request.nombre().trim(), id)) {
            throw ApiException.conflicto("Ya existe otra categoría con ese nombre.", "nombre");
        }
        aplicar(categoria, request); // el "valor" no cambia para no romper los productos ni las URLs
        return Mapper.categoria(categoriaRepository.save(categoria), productoRepository.countByCategoriaId(id));
    }


    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscar(id);
        long cantidad = productoRepository.countByCategoriaId(id);
        if (cantidad > 0) {
            throw ApiException.conflicto(
                    "La categoría tiene " + cantidad + " producto(s). Cámbialos de categoría primero.", null);
        }
        categoriaRepository.delete(categoria);
    }


    private void aplicar(Categoria categoria, CategoriaRequest request) {
        categoria.setNombre(request.nombre().trim());
        categoria.setDescripcion(Mapper.limpiar(request.descripcion()));
        categoria.setIcono(request.icono() == null || request.icono().isBlank() ? "bi-tag" : request.icono().trim());
    }


    private Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("La categoría " + id + " no existe."));
    }
}



