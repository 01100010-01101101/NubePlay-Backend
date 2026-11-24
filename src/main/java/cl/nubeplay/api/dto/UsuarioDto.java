package cl.nubeplay.api.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


import java.util.List;


public final class UsuarioDto {



    public static final String REGEX_CORREO = "(?i)^[\\w.+-]+@(duoc\\.cl|duocuc\\.cl|profesor\\.duoc\\.cl|gmail\\.com)$";


    private UsuarioDto() {
    }



    public record UsuarioResponse(
            Long id,
            String run,
            String nombre,
            String apellidos,
            String correo,
            String fechaNacimiento,
            String region,
            String comuna,
            String direccion,
            @Schema(example = "Administrador") String rol,
            @Schema(example = "Activo") String estado,
            @Schema(description = "Historial de compras (solo en las respuestas del administrador)") List<CompraResumen> compras,
            @Schema(description = "true si la cuenta tiene Google vinculado") boolean cuentaGoogle) {
    }


    public record CompraResumen(String fecha, String producto, int cantidad, int total) {
    }



    public record UsuarioRequest(
            @Schema(example = "123456785") @NotBlank(message = "El RUN es obligatorio.")
            @Size(min = 7, max = 9, message = "El RUN debe tener entre 7 y 9 caracteres (sin puntos ni guion).") String run,
            @Schema(example = "Ana") @NotBlank(message = "El nombre es obligatorio.")
            @Size(max = 50, message = "El nombre admite máximo 50 caracteres.") String nombre,
            @Schema(example = "Rojas") @NotBlank(message = "Los apellidos son obligatorios.")
            @Size(max = 100, message = "Los apellidos admiten máximo 100 caracteres.") String apellidos,
            @Schema(example = "ana@duoc.cl") @NotBlank(message = "El correo es obligatorio.")
            @Pattern(regexp = REGEX_CORREO, message = "El correo debe ser @duoc.cl, @profesor.duoc.cl, @duocuc.cl o @gmail.com.") String correo,
            @Schema(example = "clave123", description = "Obligatoria al crear; opcional al editar")
            @Size(max = 10, message = "La contraseña admite máximo 10 caracteres.") String password,
            @Schema(example = "1995-03-10") String fechaNacimiento,
            String region,
            String comuna,
            @Size(max = 300, message = "La dirección admite máximo 300 caracteres.") String direccion,
            @Schema(example = "Vendedor", allowableValues = {"Administrador", "Vendedor", "Cliente"}) String rol,
            @Schema(example = "Activo", allowableValues = {"Activo", "Inactivo"}) String estado) {
    }



    public record PerfilRequest(
            @NotBlank(message = "El nombre es obligatorio.")
            @Size(max = 50, message = "El nombre admite máximo 50 caracteres.") String nombre,
            @NotBlank(message = "Los apellidos son obligatorios.")
            @Size(max = 100, message = "Los apellidos admiten máximo 100 caracteres.") String apellidos,
            @Schema(description = "Requerida solo si se cambia la contraseña") String passwordActual,
            @Schema(description = "Nueva contraseña (opcional, 4 a 10 caracteres)") String passwordNueva) {
    }
}

