package cl.nubeplay.api.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


/** Datos de entrada y salida de /api/v1/auth. */
public final class AuthDto {


    private AuthDto() {
    }


    public record LoginRequest(
            @Schema(example = "admin@duoc.cl") @NotBlank(message = "El correo es obligatorio.") String correo,
            @Schema(example = "admin") @NotBlank(message = "La contraseña es obligatoria.") String password) {
    }


    /** Credencial (ID token JWT) que entrega el botón "Iniciar sesión con Google". */
    public record GoogleRequest(
            @Schema(description = "ID token de Google (campo credential de Google Identity Services)")
            @NotBlank(message = "Falta la credencial de Google.") String credential) {
    }


    public record RefreshRequest(
            @NotBlank(message = "El refresh token es obligatorio.") String refreshToken) {
    }


    public record AuthResponse(
            @Schema(description = "JWT de acceso; se envía en el header Authorization: Bearer <token>") String accessToken,
            @Schema(description = "JWT de larga duración que solo sirve para pedir un nuevo accessToken") String refreshToken,
            @Schema(example = "Bearer") String tipo,
            @Schema(description = "Segundos de vigencia del accessToken", example = "900") long expiraEn,
            UsuarioDto.UsuarioResponse usuario) {
    }


    public record RegistroRequest(
            @Schema(example = "19011022K") @NotBlank(message = "El RUN es obligatorio.")
            @Size(min = 7, max = 9, message = "El RUN debe tener entre 7 y 9 caracteres (sin puntos ni guion).") String run,
            @Schema(example = "Juan") @NotBlank(message = "El nombre es obligatorio.")
            @Size(max = 50, message = "El nombre admite máximo 50 caracteres.") String nombre,
            @Schema(example = "Pérez Soto") @NotBlank(message = "Los apellidos son obligatorios.")
            @Size(max = 100, message = "Los apellidos admiten máximo 100 caracteres.") String apellidos,
            @Schema(example = "juan@gmail.com") @NotBlank(message = "El correo es obligatorio.")
            @jakarta.validation.constraints.Pattern(regexp = UsuarioDto.REGEX_CORREO,
                    message = "El correo debe ser @duoc.cl, @profesor.duoc.cl, @duocuc.cl o @gmail.com.") String correo,
            @Schema(example = "clave123") @NotBlank(message = "La contraseña es obligatoria.")
            @Size(min = 4, max = 10, message = "La contraseña debe tener entre 4 y 10 caracteres.") String password,
            @Schema(example = "2000-05-20") String fechaNacimiento,
            @Schema(example = "Metropolitana de Santiago") @NotBlank(message = "La región es obligatoria.") String region,
            @Schema(example = "Santiago") @NotBlank(message = "La comuna es obligatoria.") String comuna,
            @Schema(example = "Av. Siempre Viva 123") @NotBlank(message = "La dirección es obligatoria.")
            @Size(max = 300, message = "La dirección admite máximo 300 caracteres.") String direccion) {
    }
}



