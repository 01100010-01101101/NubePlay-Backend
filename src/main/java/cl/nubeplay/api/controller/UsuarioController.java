package cl.nubeplay.api.controller;

import cl.nubeplay.api.dto.UsuarioDto.PerfilRequest;
import cl.nubeplay.api.dto.UsuarioDto.UsuarioRequest;
import cl.nubeplay.api.dto.UsuarioDto.UsuarioResponse;
import cl.nubeplay.api.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "4. Usuarios", description = "Gestión de usuarios (Administrador) y perfil propio (cualquier usuario autenticado)")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Ver mi perfil")
    @ApiResponse(responseCode = "200", description = "Éxito")
    @ApiResponse(responseCode = "401", description = "Sin token")
    public UsuarioResponse miPerfil(@AuthenticationPrincipal UserDetails actual) {
        return usuarioService.perfil(actual.getUsername());
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Editar mi perfil", description = "Nombre, apellidos y, opcionalmente, la contraseña (pide la actual).")
    @ApiResponse(responseCode = "200", description = "Actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o contraseña actual incorrecta")
    @ApiResponse(responseCode = "401", description = "Sin token")
    public UsuarioResponse editarPerfil(@AuthenticationPrincipal UserDetails actual, @Valid @RequestBody PerfilRequest request) {
        return usuarioService.actualizarPerfil(actual.getUsername(), request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar usuarios (Administrador)", description = "Incluye el historial de compras de cada usuario.")
    @ApiResponse(responseCode = "200", description = "Éxito")
    @ApiResponse(responseCode = "401", description = "Sin token")
    @ApiResponse(responseCode = "403", description = "Rol sin permiso")
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Detalle de un usuario (Administrador)")
    @ApiResponse(responseCode = "200", description = "Éxito")
    @ApiResponse(responseCode = "403", description = "Rol sin permiso")
    @ApiResponse(responseCode = "404", description = "No encontrado")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear usuario con cualquier rol (Administrador)")
    @ApiResponse(responseCode = "201", description = "Creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "403", description = "Rol sin permiso")
    @ApiResponse(responseCode = "409", description = "Correo o RUN duplicado")
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest request) {
        return usuarioService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar usuario (Administrador)", description = "En su propia cuenta no puede cambiar el rol ni el estado.")
    @ApiResponse(responseCode = "200", description = "Actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "403", description = "Rol sin permiso")
    @ApiResponse(responseCode = "404", description = "No encontrado")
    @ApiResponse(responseCode = "409", description = "Correo o RUN duplicado")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request,
                                      @AuthenticationPrincipal UserDetails actual) {
        return usuarioService.actualizar(id, request, actual.getUsername());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar usuario (Administrador)", description = "No permite eliminar la propia cuenta.")
    @ApiResponse(responseCode = "204", description = "Eliminado")
    @ApiResponse(responseCode = "403", description = "Rol sin permiso")
    @ApiResponse(responseCode = "404", description = "No encontrado")
    @ApiResponse(responseCode = "409", description = "Intento de eliminar la propia cuenta")
    public void eliminar(@PathVariable Long id, @AuthenticationPrincipal UserDetails actual) {
        usuarioService.eliminar(id, actual.getUsername());
    }
}
