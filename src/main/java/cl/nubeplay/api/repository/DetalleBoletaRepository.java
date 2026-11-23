package cl.nubeplay.api.repository;

import cl.nubeplay.api.model.DetalleBoleta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DetalleBoletaRepository extends JpaRepository<DetalleBoleta, Long> {

    /** Al eliminar un producto, las boletas antiguas conservan su nombre y precio, pero sin el vínculo. */
    @Modifying
    @Query("update DetalleBoleta d set d.producto = null where d.producto.id = :productoId")
    int desvincularProducto(@Param("productoId") Long productoId);
}
