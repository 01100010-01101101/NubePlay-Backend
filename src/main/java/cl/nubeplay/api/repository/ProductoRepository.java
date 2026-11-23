package cl.nubeplay.api.repository;

import cl.nubeplay.api.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findAllByOrderByIdAsc();

    List<Producto> findByCategoriaValorOrderByIdAsc(String valor);

    long countByCategoriaId(Long categoriaId);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    /** Productos en stock crítico (stock <= stockCritico). */
    @Query("select p from Producto p where p.stock <= p.stockCritico order by p.id")
    List<Producto> buscarStockCritico();
}
