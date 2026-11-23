package cl.nubeplay.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación Swagger / OpenAPI.
 * - UI:   http://localhost:8080/swagger-ui.html
 * - JSON: http://localhost:8080/v3/api-docs
 * El botón "Authorize" permite pegar el accessToken obtenido en /api/v1/auth/login.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI nubeplayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("NUBEPLAY API")
                        .version("v1")
                        .description("API REST de la tienda online NUBEPLAY: catálogo, usuarios, boletas y autenticación JWT por roles "
                                + "(Administrador, Vendedor, Cliente).")
                        .contact(new Contact().name("Equipo NUBEPLAY - DSY1104")))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Pega aquí el accessToken (sin la palabra Bearer).")));
    }
}
