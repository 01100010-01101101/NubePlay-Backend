package cl.nubeplay.api.repository;

import cl.nubeplay.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    Optional<Usuario> findByGoogleId(String googleId);

    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);

    boolean existsByRunIgnoreCase(String run);

    boolean existsByRunIgnoreCaseAndIdNot(String run, Long id);
}
