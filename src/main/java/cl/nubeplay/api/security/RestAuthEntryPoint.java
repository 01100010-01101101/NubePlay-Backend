package cl.nubeplay.api.security;

import cl.nubeplay.api.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Respuestas JSON para 401 (sin token / token inválido) y 403 (rol sin permiso). */
@Component
public class RestAuthEntryPoint implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAuthEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException {
        Object motivo = request.getAttribute(JwtAuthFilter.ATRIBUTO_ERROR);
        String mensaje = motivo != null ? motivo.toString() : "Debes iniciar sesión para acceder a este recurso.";
        escribir(response, HttpStatus.UNAUTHORIZED, mensaje, request.getRequestURI());
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) throws IOException {
        escribir(response, HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta acción.", request.getRequestURI());
    }

    private void escribir(HttpServletResponse response, HttpStatus status, String mensaje, String ruta) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), GlobalExceptionHandler.cuerpo(status, mensaje, null, ruta));
    }
}
