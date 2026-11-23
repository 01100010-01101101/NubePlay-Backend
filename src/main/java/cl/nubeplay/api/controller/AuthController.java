package cl.nubeplay.api.controller;

import cl.nubeplay.api.dto.AuthDto.AuthResponse;
import cl.nubeplay.api.dto.AuthDto.GoogleRequest;
import cl.nubeplay.api.dto.AuthDto.LoginRequest;
import cl.nubeplay.api.dto.AuthDto.RefreshRequest;
import cl.nubeplay.api.dto.AuthDto.RegistroRequest;
import cl.nubeplay.api.dto.ErrorResponse;
import cl.nubeplay.api.dto.UsuarioDto.UsuarioResponse;
import cl.nubeplay.api.service.AuthService;
import cl.nubeplay.api.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "1. Autenticación", description = "Login, registro y renovación de tokens JWT")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    public AuthController(AuthService authService, UsuarioService usuarioService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Devuelve un accessToken (15 min) y un refreshToken (7 días).")
    @ApiResponse(responseCode = "200", description = "Credenciales correctas")
    @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Cuenta inactiva",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar cliente", description = "Registro público: el usuario queda con rol Cliente.")
    @ApiResponse(responseCode = "201", description = "Usuario creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Correo o RUN ya registrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public UsuarioResponse registrar(@Valid @RequestBody RegistroRequest request) {
        return authService.registrar(request);
    }

    @PostMapping("/google")
    @Operation(summary = "Iniciar sesión o registrarse con Google",
            description = "Recibe el ID token del botón de Google, lo verifica con las claves públicas de Google y entrega los JWT de NUBEPLAY. "
                    + "Si el correo ya existe, vincula la cuenta de Google; si no, crea un Cliente.")
    @ApiResponse(responseCode = "200", description = "Sesión iniciada (cuenta creada o vinculada)")
    @ApiResponse(responseCode = "401", description = "Token de Google inválido o correo no verificado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Dominio de correo no permitido o cuenta inactiva",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "Google no configurado en el servidor (GOOGLE_CLIENT_ID)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public AuthResponse google(@Valid @RequestBody GoogleRequest request) {
        return authService.loginGoogle(request.credential());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar tokens", description = "Recibe el refreshToken y entrega un par de tokens nuevo (rotación).")
    @ApiResponse(responseCode = "200", description = "Tokens renovados")
    @ApiResponse(responseCode = "401", description = "Refresh token inválido o expirado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public AuthResponse refrescar(@Valid @RequestBody RefreshRequest request) {
        return authService.refrescar(request.refreshToken());
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Usuario de la sesión", description = "Valida el token y devuelve los datos actuales del usuario.")
    @ApiResponse(responseCode = "200", description = "Sesión válida")
    @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public UsuarioResponse yo(@AuthenticationPrincipal UserDetails usuario) {
        return usuarioService.perfil(usuario.getUsername());
    }
}
