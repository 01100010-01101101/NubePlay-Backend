package cl.nubeplay.api.controller;

import cl.nubeplay.api.dto.BoletaDto.BoletaRequest;
import cl.nubeplay.api.dto.BoletaDto.BoletaResponse;
import cl.nubeplay.api.dto.BoletaDto.EstadoRequest;
import cl.nubeplay.api.security.UsuarioActual;
import cl.nubeplay.api.service.BoletaService;
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
@RequestMapping("/api/v1/boletas")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "5. Boletas (órdenes)", description = "Compras de la tienda. Administrador y Vendedor ven todas; el Cliente solo las suyas")
public class BoletaController {

    private final BoletaService boletaService;

    public BoletaController(BoletaService boletaService) {
        this.boletaService = boletaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Registrar una compra",
            description = "Valida stock, calcula precios y cupón en el servidor y descuenta el inventario.")
    @ApiResponse(responseCode = "201", description = "Compra registrada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos, carrito vacío o cupón inválido")
    @ApiResponse(responseCode = "401", description = "Sin token")
    @ApiResponse(responseCode = "409", description = "Stock insuficiente")
    public BoletaResponse crear(@Valid @RequestBody BoletaRequest request, @AuthenticationPrincipal UserDetails actual) {
        return boletaService.crear(request, actual.getUsername());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @Operation(summary = "Listar todas las órdenes (Administrador, Vendedor)")
    @ApiResponse(responseCode = "200", description = "Éxito")
    @ApiResponse(responseCode = "401", description = "Sin token")
    @ApiResponse(responseCode = "403", description = "Rol sin permiso (Cliente)")
    public List<BoletaResponse> listar() {
        return boletaService.listar();
    }

    @GetMapping("/mis-boletas")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mis compras (usuario autenticado)")
    @ApiResponse(responseCode = "200", description = "Éxito")
    @ApiResponse(responseCode = "401", description = "Sin token")
    public List<BoletaResponse> misBoletas(@AuthenticationPrincipal UserDetails actual) {
        return boletaService.misBoletas(actual.getUsername());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Detalle de una boleta", description = "El Cliente solo puede ver sus propias boletas.")
    @ApiResponse(responseCode = "200", description = "Éxito")
    @ApiResponse(responseCode = "403", description = "La boleta no pertenece al cliente")
    @ApiResponse(responseCode = "404", description = "No encontrada")
    public BoletaResponse obtener(@PathVariable Long id, @AuthenticationPrincipal UserDetails actual) {
        return boletaService.obtener(id, actual.getUsername(), UsuarioActual.rol(actual));
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cambiar estado de una boleta (Administrador)",
            description = "Pagada, Enviada, Entregada o Anulada. Al anular se devuelve el stock.")
    @ApiResponse(responseCode = "200", description = "Actualizada")
    @ApiResponse(responseCode = "400", description = "Estado inválido")
    @ApiResponse(responseCode = "403", description = "Rol sin permiso")
    @ApiResponse(responseCode = "404", description = "No encontrada")
    @ApiResponse(responseCode = "409", description = "La boleta ya está anulada")
    public BoletaResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequest request) {
        return boletaService.cambiarEstado(id, request.estado());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar boleta (Administrador)")
    @ApiResponse(responseCode = "204", description = "Eliminada")
    @ApiResponse(responseCode = "403", description = "Rol sin permiso")
    @ApiResponse(responseCode = "404", description = "No encontrada")
    public void eliminar(@PathVariable Long id) {
        boletaService.eliminar(id);
    }
}
