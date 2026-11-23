package cl.nubeplay.api.dto;


import io.swagger.v3.oas.annotations.media.Schema;


import java.time.LocalDateTime;



public record ErrorResponse(
        LocalDateTime timestamp,
        @Schema(example = "400") int status,
        @Schema(example = "Bad Request") String error,
        @Schema(example = "El precio no puede ser negativo.") String mensaje,
        @Schema(example = "precio", description = "Campo del formulario que provocó el error (si aplica)") String campo,
        @Schema(example = "/api/v1/productos") String ruta) {
}

