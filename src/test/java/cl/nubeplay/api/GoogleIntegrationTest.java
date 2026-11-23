package cl.nubeplay.api;

import cl.nubeplay.api.exception.ApiException;
import cl.nubeplay.api.repository.UsuarioRepository;
import cl.nubeplay.api.security.GoogleTokenService;
import cl.nubeplay.api.security.GoogleTokenService.UsuarioGoogle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Login con Google. La verificación criptográfica del token contra Google se reemplaza por un
 * doble de prueba (no hay Internet ni tokens reales en las pruebas); se prueba la lógica del backend.
 */
@DisplayName("Inicio de sesión y registro con Google")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:nubeplay-google;MODE=MySQL;DB_CLOSE_DELAY=-1")
class GoogleIntegrationTest extends IntegracionBase {

    @MockitoBean
    private GoogleTokenService google;

    @Autowired
    private UsuarioRepository usuarios;

    private void tokenDe(String sub, String correo, boolean verificado) {
        when(google.verificar(anyString())).thenReturn(new UsuarioGoogle(sub, correo, verificado, "Sofía", "Muñoz"));
    }

    private org.springframework.test.web.servlet.ResultActions loginGoogle() throws Exception {
        return mvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON)
                .content(aJson(Map.of("credential", "token-de-google"))));
    }

    @Test
    @DisplayName("Un correo nuevo crea un Cliente vinculado a Google y entrega los JWT")
    void registroConGoogle() throws Exception {
        tokenDe("google-123", "sofia.munoz@gmail.com", true);

        String respuesta = loginGoogle()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyString())))
                .andExpect(jsonPath("$.usuario.rol").value("Cliente"))
                .andExpect(jsonPath("$.usuario.nombre").value("Sofía"))
                .andExpect(jsonPath("$.usuario.cuentaGoogle").value(true))
                .andReturn().getResponse().getContentAsString();

        assertThat(usuarios.findByGoogleId("google-123")).isPresent();
        // El token entregado sirve para la API
        String token = json.readTree(respuesta).get("accessToken").asText();
        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("sofia.munoz@gmail.com"));
    }

    @Test
    @DisplayName("Si el correo ya estaba registrado, vincula Google a esa cuenta (mismo rol)")
    void vinculaCuentaExistente() throws Exception {
        tokenDe("google-admin", "admin@duoc.cl", true);

        loginGoogle()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.rol").value("Administrador"))
                .andExpect(jsonPath("$.usuario.cuentaGoogle").value(true));

        assertThat(usuarios.findByCorreoIgnoreCase("admin@duoc.cl").orElseThrow().getGoogleId()).isEqualTo("google-admin");
        // Una segunda vez entra por el googleId ya vinculado
        loginGoogle().andExpect(status().isOk());
    }

    @Test
    @DisplayName("Rechaza dominios no permitidos (403) y correos no verificados (401)")
    void validaciones() throws Exception {
        tokenDe("google-x", "alguien@hotmail.com", true);
        loginGoogle().andExpect(status().isForbidden());

        tokenDe("google-y", "otro@gmail.com", false);
        loginGoogle().andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un token de Google inválido responde 401")
    void tokenInvalido() throws Exception {
        when(google.verificar(anyString())).thenThrow(ApiException.noAutorizado("No se pudo validar la cuenta de Google. Intenta nuevamente."));
        loginGoogle()
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("No se pudo validar la cuenta de Google. Intenta nuevamente."));
    }
}
