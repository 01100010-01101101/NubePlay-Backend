package cl.nubeplay.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lee el header "Authorization: Bearer <token>", valida el JWT y, si es correcto,
 * registra al usuario (con su rol actual de la base de datos) en el SecurityContext.
 * Si el token falta o es inválido la petición sigue como anónima y Spring Security
 * responde 401 en las rutas protegidas.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    /** Atributo que lee {@link RestAuthEntryPoint} para explicar por qué se rechazó el token. */
    public static final String ATRIBUTO_ERROR = "jwt_error";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();
        try {
            Claims claims = jwtService.validar(token, JwtService.TIPO_ACCESS);
            // Se vuelve a leer el usuario: si fue desactivado o cambió de rol, aplica de inmediato.
            UserDetails usuario = userDetailsService.loadUserByUsername(claims.getSubject());
            if (usuario.isEnabled()) {
                var autenticacion = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            } else {
                request.setAttribute(ATRIBUTO_ERROR, "Tu cuenta se encuentra inactiva.");
            }
        } catch (ExpiredJwtException ex) {
            request.setAttribute(ATRIBUTO_ERROR, "El token expiró. Renueva la sesión.");
        } catch (JwtException | IllegalArgumentException ex) {
            request.setAttribute(ATRIBUTO_ERROR, "Token inválido.");
        } catch (UsernameNotFoundException ex) {
            request.setAttribute(ATRIBUTO_ERROR, "El usuario del token ya no existe.");
        }

        chain.doFilter(request, response);
    }
}
