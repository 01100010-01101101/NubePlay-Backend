package cl.nubeplay.api.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;



public final class CatalogoDto {


    private CatalogoDto() {
    }


    public record CategoriaRequest(
            @Schema(example = "Tarjetas Gráficas") @NotBlank(message = "El nombre es obligatorio.")
            @Size(max = 50, message = "El nombre admite máximo 50 caracteres.") String nombre,
            @Schema(example = "Tarjetas de video para PC gamer")
            @Size(max = 200, message = "La descripción admite máximo 200 caracteres.") String descripcion,
            @Schema(example = "bi-pc-display") String icono) {
    }


    public record CategoriaResponse(
            Long id,
            @Schema(example = "consolas") String valor,
            @Schema(example = "Consolas") String nombre,
            String descripcion,
            @Schema(example = "bi-controller") String icono,
            @Schema(description = "Cantidad de productos asociados") long cantidadProductos) {
    }


    public record ProductoRequest(
            @Schema(example = "PS5-001") @NotBlank(message = "El código es obligatorio.")
            @Size(min = 3, max = 20, message = "El código debe tener entre 3 y 20 caracteres.") String codigo,
            @Schema(example = "PlayStation 5") @NotBlank(message = "El nombre es obligatorio.")
            @Size(max = 100, message = "El nombre admite máximo 100 caracteres.") String nombre,
            @Size(max = 500, message = "La descripción admite máximo 500 caracteres.") String descripcion,
            @Schema(example = "549990") @NotNull(message = "El precio es obligatorio.")
            @Min(value = 0, message = "El precio no puede ser negativo.") Integer precio,
            @Schema(example = "10") @NotNull(message = "El stock es obligatorio.")
            @Min(value = 0, message = "El stock no puede ser negativo.") Integer stock,
            @Schema(example = "3") @Min(value = 0, message = "El stock crítico no puede ser negativo.") Integer stockCritico,
            @Size(max = 1000, message = "La URL de la imagen admite máximo 1000 caracteres.") String imagen,
            @Schema(example = "consolas", description = "Valor (identificador) de la categoría")
            @NotBlank(message = "La categoría es obligatoria.") String categoria) {
    }


    public record ProductoResponse(
            Long id,
            String codigo,
            String nombre,
            String descripcion,
            int precio,
            int stock,
            int stockCritico,
            String imagen,
            @Schema(example = "consolas") String categoria,
            @Schema(example = "Consolas") String categoriaNombre,
            boolean stockBajo) {
    }
}

