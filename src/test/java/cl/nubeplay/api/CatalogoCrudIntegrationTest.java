package cl.nubeplay.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CRUD de productos y categorías contra la base de datos")
class CatalogoCrudIntegrationTest extends IntegracionBase {

    private Map<String, Object> producto(String codigo, int precio) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("codigo", codigo);
        datos.put("nombre", "Control Pro");
        datos.put("descripcion", "Control de prueba");
        datos.put("precio", precio);
        datos.put("stock", 4);
        datos.put("stockCritico", 5);
        datos.put("imagen", "https://placehold.co/600x400");
        datos.put("categoria", "accesorios");
        return datos;
    }

    @Test
    @DisplayName("Crear, leer, actualizar y eliminar un producto")
    void crudProducto() throws Exception {
        String admin = tokenAdmin();

        String creado = mvc.perform(post("/api/v1/productos").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(aJson(producto("ctl-900", 39990))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("CTL-900"))
                .andExpect(jsonPath("$.categoriaNombre").value("Accesorios"))
                .andExpect(jsonPath("$.stockBajo").value(true))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(creado).get("id").asLong();

        mvc.perform(get("/api/v1/productos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Control Pro"));

        Map<String, Object> cambios = producto("CTL-900", 29990);
        cambios.put("stock", 20);
        mvc.perform(put("/api/v1/productos/" + id).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(aJson(cambios)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precio").value(29990))
                .andExpect(jsonPath("$.stockBajo").value(false));

        mvc.perform(delete("/api/v1/productos/" + id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/productos/" + id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Validaciones: precio negativo (400), código duplicado (409), categoría inexistente (400)")
    void validacionesProducto() throws Exception {
        String admin = tokenAdmin();

        mvc.perform(post("/api/v1/productos").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(aJson(producto("NEG-001", -5))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("precio"));

        mvc.perform(post("/api/v1/productos").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(aJson(producto("PS5-001", 1000))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campo").value("codigo"));

        Map<String, Object> sinCategoria = producto("NEW-001", 1000);
        sinCategoria.put("categoria", "no-existe");
        mvc.perform(post("/api/v1/productos").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(aJson(sinCategoria)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("categoria"));
    }

    @Test
    @DisplayName("Filtro de stock crítico y por categoría")
    void filtros() throws Exception {
        mvc.perform(get("/api/v1/productos").param("categoria", "consolas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoria").value("consolas"));
        mvc.perform(get("/api/v1/productos").param("criticos", "true")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Categorías: se crean con valor generado y no se eliminan si tienen productos")
    void crudCategoria() throws Exception {
        String admin = tokenAdmin();

        String creada = mvc.perform(post("/api/v1/categorias").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aJson(Map.of("nombre", "Tarjetas Gráficas", "descripcion", "GPU", "icono", "bi-pc-display"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valor").value("tarjetas-graficas"))
                .andReturn().getResponse().getContentAsString();
        JsonNode categoria = json.readTree(creada);

        mvc.perform(put("/api/v1/categorias/" + categoria.get("id").asLong()).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aJson(Map.of("nombre", "GPUs", "descripcion", "Tarjetas de video", "icono", "bi-pc-display"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("GPUs"))
                .andExpect(jsonPath("$.valor").value("tarjetas-graficas"));

        mvc.perform(delete("/api/v1/categorias/" + categoria.get("id").asLong()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());

        // "consolas" (id 2 en los datos iniciales, se busca por valor) tiene productos -> 409
        String lista = mvc.perform(get("/api/v1/categorias")).andReturn().getResponse().getContentAsString();
        long idConsolas = 0;
        for (JsonNode c : json.readTree(lista)) {
            if ("consolas".equals(c.get("valor").asText())) idConsolas = c.get("id").asLong();
        }
        mvc.perform(delete("/api/v1/categorias/" + idConsolas).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isConflict());
    }
}
