package cl.nubeplay.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Flujo de compra: boleta, stock, cupón y permisos sobre las boletas")
class BoletaIntegrationTest extends IntegracionBase {

    private Map<String, Object> compra(long productoId, int cantidad, String cupon) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("comprador", Map.of("nombre", "Carlos", "apellidos", "Cliente", "correo", "cliente@gmail.com"));
        datos.put("envio", Map.of("calle", "Av. Siempre Viva 742", "region", "Metropolitana de Santiago", "comuna", "Santiago"));
        datos.put("items", List.of(Map.of("productoId", productoId, "cantidad", cantidad)));
        if (cupon != null) datos.put("cupon", cupon);
        return datos;
    }

    private JsonNode producto(long id) throws Exception {
        return json.readTree(mvc.perform(get("/api/v1/productos/" + id)).andReturn().getResponse().getContentAsString());
    }

    @Test
    @DisplayName("El cliente compra: el backend calcula totales, aplica el cupón y descuenta stock")
    void compraExitosa() throws Exception {
        JsonNode antes = producto(1);
        int precio = antes.get("precio").asInt();
        int stockAntes = antes.get("stock").asInt();

        String respuesta = mvc.perform(post("/api/v1/boletas").header(HttpHeaders.AUTHORIZATION, tokenCliente())
                        .contentType(MediaType.APPLICATION_JSON).content(aJson(compra(1, 2, "nubeplay123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("Pagada"))
                .andExpect(jsonPath("$.subtotal").value(precio * 2))
                .andExpect(jsonPath("$.descuento").value(Math.round(precio * 2 * 0.10)))
                .andExpect(jsonPath("$.cupon").value("NUBEPLAY123"))
                .andExpect(jsonPath("$.items[0].cantidad").value(2))
                .andReturn().getResponse().getContentAsString();

        assertThat(producto(1).get("stock").asInt()).isEqualTo(stockAntes - 2);

        long idBoleta = json.readTree(respuesta).get("id").asLong();
        // El dueño puede ver su boleta; el vendedor también
        mvc.perform(get("/api/v1/boletas/" + idBoleta).header(HttpHeaders.AUTHORIZATION, tokenCliente())).andExpect(status().isOk());
        mvc.perform(get("/api/v1/boletas/" + idBoleta).header(HttpHeaders.AUTHORIZATION, tokenVendedor())).andExpect(status().isOk());
        mvc.perform(get("/api/v1/boletas/mis-boletas").header(HttpHeaders.AUTHORIZATION, tokenCliente()))
                .andExpect(jsonPath("$[0].id").value(idBoleta));
    }

    @Test
    @DisplayName("Sin stock suficiente la compra se rechaza (409) y el stock no cambia")
    void sinStock() throws Exception {
        int stock = producto(2).get("stock").asInt();
        mvc.perform(post("/api/v1/boletas").header(HttpHeaders.AUTHORIZATION, tokenCliente())
                        .contentType(MediaType.APPLICATION_JSON).content(aJson(compra(2, stock + 1, null))))
                .andExpect(status().isConflict());
        assertThat(producto(2).get("stock").asInt()).isEqualTo(stock);
    }

    @Test
    @DisplayName("Cupón inválido (400) y carrito vacío (400)")
    void validaciones() throws Exception {
        String cliente = tokenCliente();
        mvc.perform(post("/api/v1/boletas").header(HttpHeaders.AUTHORIZATION, cliente)
                        .contentType(MediaType.APPLICATION_JSON).content(aJson(compra(1, 1, "FALSO"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("cupon"));

        Map<String, Object> vacio = compra(1, 1, null);
        vacio.put("items", List.of());
        mvc.perform(post("/api/v1/boletas").header(HttpHeaders.AUTHORIZATION, cliente)
                        .contentType(MediaType.APPLICATION_JSON).content(aJson(vacio)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Un cliente no puede ver la boleta de otro cliente (403)")
    void boletaAjena() throws Exception {
        String respuesta = mvc.perform(post("/api/v1/boletas").header(HttpHeaders.AUTHORIZATION, tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(aJson(compra(3, 1, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(respuesta).get("id").asLong();

        mvc.perform(get("/api/v1/boletas/" + id).header(HttpHeaders.AUTHORIZATION, tokenCliente()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Al anular una boleta (Administrador) se devuelve el stock; el Vendedor no puede anular")
    void anular() throws Exception {
        int stockAntes = producto(4).get("stock").asInt();
        String respuesta = mvc.perform(post("/api/v1/boletas").header(HttpHeaders.AUTHORIZATION, tokenCliente())
                        .contentType(MediaType.APPLICATION_JSON).content(aJson(compra(4, 3, null))))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(respuesta).get("id").asLong();
        assertThat(producto(4).get("stock").asInt()).isEqualTo(stockAntes - 3);

        mvc.perform(put("/api/v1/boletas/" + id + "/estado").header(HttpHeaders.AUTHORIZATION, tokenVendedor())
                        .contentType(MediaType.APPLICATION_JSON).content(aJson(Map.of("estado", "Anulada"))))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/v1/boletas/" + id + "/estado").header(HttpHeaders.AUTHORIZATION, tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(aJson(Map.of("estado", "Anulada"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("Anulada"));
        assertThat(producto(4).get("stock").asInt()).isEqualTo(stockAntes);
    }
}
