package cl.nubeplay.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Autenticación JWT: login, registro, /me y renovación de tokens")
class AuthIntegrationTest extends IntegracionBase {

    @Test
    @DisplayName("Login correcto entrega accessToken, refreshToken y el usuario sin contraseña")
    void loginCorrecto() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aJson(Map.of("correo", "admin@duoc.cl", "password", "admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyString())))
                .andExpect(jsonPath("$.refreshToken", not(emptyString())))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.usuario.rol").value("Administrador"))
                .andExpect(jsonPath("$.usuario.password").doesNotExist());
    }

    @Test
    @DisplayName("Login con contraseña incorrecta responde 401")
    void loginIncorrecto() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aJson(Map.of("correo", "admin@duoc.cl", "password", "otra"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos."));
    }

    @Test
    @DisplayName("Registro público crea un Cliente y no permite correos duplicados")
    void registro() throws Exception {
        Map<String, Object> datos = new HashMap<>();
        datos.put("run", "19011022K");
        datos.put("nombre", "Juan");
        datos.put("apellidos", "Pérez");
        datos.put("correo", "juan@gmail.com");
        datos.put("password", "clave123");
        datos.put("region", "Metropolitana de Santiago");
        datos.put("comuna", "Santiago");
        datos.put("direccion", "Calle 123");

        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(aJson(datos)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("Cliente"));

        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(aJson(datos)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campo").value("correo"));

        // El nuevo usuario puede iniciar sesión
        assertThat(login("juan@gmail.com", "clave123").get("usuario").get("rol").asText()).isEqualTo("Cliente");
    }

    @Test
    @DisplayName("Registro con correo de dominio no permitido responde 400")
    void registroInvalido() throws Exception {
        Map<String, Object> datos = new HashMap<>();
        datos.put("run", "12345678");
        datos.put("nombre", "Ana");
        datos.put("apellidos", "Soto");
        datos.put("correo", "ana@hotmail.com");
        datos.put("password", "1234");
        datos.put("region", "Valparaíso");
        datos.put("comuna", "Viña del Mar");
        datos.put("direccion", "Av. Libertad 1");

        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(aJson(datos)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("correo"));
    }

    @Test
    @DisplayName("/auth/me requiere token y devuelve el usuario de la sesión")
    void me() throws Exception {
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, tokenVendedor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("vendedor@duoc.cl"))
                .andExpect(jsonPath("$.rol").value("Vendedor"));
    }

    @Test
    @DisplayName("Un token alterado es rechazado con 401")
    void tokenAlterado() throws Exception {
        String token = tokenCliente();
        String alterado = token.substring(0, token.length() - 3) + "abc";
        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, alterado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Token inválido."));
    }

    @Test
    @DisplayName("El refreshToken entrega un par nuevo; un accessToken no sirve como refreshToken")
    void renovarTokens() throws Exception {
        JsonNode sesion = login("cliente@gmail.com", "cliente");
        String refresh = sesion.get("refreshToken").asText();
        String access = sesion.get("accessToken").asText();

        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(aJson(Map.of("refreshToken", refresh))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyString())))
                .andExpect(jsonPath("$.usuario.correo").value("cliente@gmail.com"));

        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(aJson(Map.of("refreshToken", access))))
                .andExpect(status().isUnauthorized());

        // Y el refreshToken no sirve para llamar a la API
        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + refresh))
                .andExpect(status().isUnauthorized());
    }
}
