package cl.nubeplay.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del backend de la tienda NUBEPLAY.
 * Swagger UI: http://localhost:8080/swagger-ui.html
 */
@SpringBootApplication
public class NubeplayApplication {

    public static void main(String[] args) {
        SpringApplication.run(NubeplayApplication.class, args);
    }
}
