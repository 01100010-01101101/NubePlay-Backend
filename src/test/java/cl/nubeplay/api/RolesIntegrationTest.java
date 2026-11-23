package cl.nubeplay.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Autorización por roles (Administrador, Vendedor, Cliente, anónimo)")
class RolesIntegrationTest extends IntegracionBase {

    private static final Map<String, Object> PRODUCTO = Map.of(
            "codigo", "TST-001", "nombre", "Producto de prueba", "precio", 1000,
            "stock", 5, "stockCritico", 1, "categoria", "consolas");

    @Test
    @DisplayName("Anónimo: puede ver el catálogo pero no modificarlo ni ver órdenes")
    void anonimo() throws Exception {
        mvc.perform(get("/api/v1/productos")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThan(0)));
        mvc.perform(get("/api/v1/categorias")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON).content(aJson(PRODUCTO)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/boletas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Vendedor: ve productos y órdenes, pero no crea/elimina productos ni ve usuarios")
    void vendedor() throws Exception {
        String token = tokenVendedor();
        mvc.perform(get("/api/v1/productos/1").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/boletas").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/productos").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(aJson(PRODUCTO)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/productos/1").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/usuarios").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/categorias").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(aJson(Map.of("nombre", "X"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cliente: solo tienda; no ve la lista de órdenes ni usuarios")
    void cliente() throws Exception {
        String token = tokenCliente();
        mvc.perform(get("/api/v1/productos").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/boletas/mis-boletas").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/boletas").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No tienes permisos para realizar esta acción."));
        mvc.perform(get("/api/v1/usuarios").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Administrador: acceso total")
    void administrador() throws Exception {
        String token = tokenAdmin();
        mvc.perform(get("/api/v1/usuarios").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThan(2)));
        mvc.perform(get("/api/v1/boletas").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/productos").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(aJson(PRODUCTO)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Un usuario desactivado pierde el acceso aunque tenga un token vigente")
    void usuarioDesactivado() throws Exception {
        String tokenDelVendedor = tokenVendedor();
        String admin = tokenAdmin();

        // El admin busca el id del vendedor y lo desactiva
        String lista = mvc.perform(get("/api/v1/usuarios").header(HttpHeaders.AUTHORIZATION, admin))
                .andReturn().getResponse().getContentAsString();
        long idVendedor = 0;
        for (var u : json.readTree(lista)) {
            if ("vendedor@duoc.cl".equals(u.get("correo").asText())) idVendedor = u.get("id").asLong();
        }
        Map<String, Object> cambios = new java.util.HashMap<>();
        cambios.put("run", "222222222");
        cambios.put("nombre", "Valentina");
        cambios.put("apellidos", "Vendedora");
        cambios.put("correo", "vendedor@duoc.cl");
        cambios.put("rol", "Vendedor");
        cambios.put("estado", "Inactivo");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/usuarios/" + idVendedor)
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(aJson(cambios)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("Inactivo"));

        mvc.perform(get("/api/v1/boletas").header(HttpHeaders.AUTHORIZATION, tokenDelVendedor))
                .andExpect(status().isUnauthorized());
    }
}
