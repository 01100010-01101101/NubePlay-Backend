package cl.nubeplay.api.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


import java.util.List;



public final class BoletaDto {


    private BoletaDto() {
    }


    public record Comprador(
            @Schema(example = "Juan") @NotBlank(message = "El nombre del comprador es obligatorio.")
            @Size(max = 50, message = "El nombre admite máximo 50 caracteres.") String nombre,
            @Schema(example = "Pérez") @NotBlank(message = "Los apellidos del comprador son obligatorios.")
            @Size(max = 100, message = "Los apellidos admiten máximo 100 caracteres.") String apellidos,
            @Schema(example = "juan@gmail.com") @NotBlank(message = "El correo del comprador es obligatorio.")
            @jakarta.validation.constraints.Pattern(regexp = UsuarioDto.REGEX_CORREO,
                    message = "El correo debe ser @duoc.cl, @profesor.duoc.cl, @duocuc.cl o @gmail.com.") String correo) {
    }


    public record Envio(
            @Schema(example = "Av. Siempre Viva 123") @NotBlank(message = "La calle es obligatoria.")
            @Size(max = 300, message = "La calle admite máximo 300 caracteres.") String calle,
            @Size(max = 50, message = "El departamento admite máximo 50 caracteres.") String departamento,
            @Schema(example = "Metropolitana de Santiago") @NotBlank(message = "La región es obligatoria.") String region,
            @Schema(example = "Santiago") @NotBlank(message = "La comuna es obligatoria.") String comuna,
            @Size(max = 300, message = "Las indicaciones admiten máximo 300 caracteres.") String indicaciones) {
    }


    public record ItemRequest(
            @Schema(example = "1") @NotNull(message = "El producto es obligatorio.") Long productoId,
            @Schema(example = "2") @NotNull(message = "La cantidad es obligatoria.")
            @Min(value = 1, message = "La cantidad mínima es 1.") Integer cantidad) {
    }



    public record BoletaRequest(
            @NotNull(message = "Faltan los datos del comprador.") @Valid Comprador comprador,
            @NotNull(message = "Faltan los datos de envío.") @Valid Envio envio,
            @NotEmpty(message = "El carrito está vacío.") List<@Valid ItemRequest> items,
            @Schema(example = "NUBEPLAY123", description = "Cupón de descuento (opcional)") String cupon) {
    }


    public record EstadoRequest(
            @Schema(example = "Enviada", allowableValues = {"Pagada", "Enviada", "Entregada", "Anulada"})
            @NotBlank(message = "El estado es obligatorio.") String estado) {
    }


    public record ItemResponse(Long id, String nombre, int precio, int cantidad, String imagen, String categoria) {
    }


    public record BoletaResponse(
            Long id,
            Long usuarioId,
            String cliente,
            String correo,
            Comprador comprador,
            Envio envio,
            @Schema(example = "2026-11-17") String fecha,
            @Schema(example = "Pagada") String estado,
            List<ItemResponse> items,
            int subtotal,
            int descuento,
            String cupon,
            int total) {
    }
}

