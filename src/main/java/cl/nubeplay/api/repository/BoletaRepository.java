package cl.nubeplay.api.repository;

import cl.nubeplay.api.model.Boleta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoletaRepository extends JpaRepository<Boleta, Long> {

    List<Boleta> findAllByOrderByIdDesc();

    List<Boleta> findByUsuarioIdOrderByIdDesc(Long usuarioId);

    /** Al eliminar un usuario se conservan sus boletas, pero sin el vínculo. */
    @Modifying
    @Query("update Boleta b set b.usuario = null where b.usuario.id = :usuarioId")
    int desvincularUsuario(@Param("usuarioId") Long usuarioId);
}
