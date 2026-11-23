package cl.nubeplay.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de las pruebas de integración: levanta la aplicación completa (seguridad, JPA, controladores)
 * contra una base H2 en memoria. Cada prueba se revierte al terminar (@Transactional).
 * Usuarios de prueba creados por DataInitializer:
 *   admin@duoc.cl / admin, vendedor@duoc.cl / vendedor, cliente@gmail.com / cliente
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class IntegracionBase {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    protected String aJson(Object objeto) throws Exception {
        return json.writeValueAsString(objeto);
    }

    /** Hace login y devuelve la respuesta completa (accessToken, refreshToken, usuario). */
    protected JsonNode login(String correo, String password) throws Exception {
        String cuerpo = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aJson(Map.of("correo", correo, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(cuerpo);
    }

    protected String tokenAdmin() throws Exception {
        return "Bearer " + login("admin@duoc.cl", "admin").get("accessToken").asText();
    }

    protected String tokenVendedor() throws Exception {
        return "Bearer " + login("vendedor@duoc.cl", "vendedor").get("accessToken").asText();
    }

    protected String tokenCliente() throws Exception {
        return "Bearer " + login("cliente@gmail.com", "cliente").get("accessToken").asText();
    }
}
