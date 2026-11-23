package cl.nubeplay.api.security;

import cl.nubeplay.api.model.Rol;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Utilidades para leer el rol del usuario autenticado. */
public final class UsuarioActual {

    private UsuarioActual() {
    }

    public static Rol rol(UserDetails usuario) {
        return usuario.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> Rol.valueOf(a.substring(5)))
                .findFirst()
                .orElse(Rol.CLIENTE);
    }
}
