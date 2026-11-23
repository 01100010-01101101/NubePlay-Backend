package cl.nubeplay.api.controller;

import cl.nubeplay.api.dto.CatalogoDto.ProductoRequest;
import cl.nubeplay.api.dto.CatalogoDto.ProductoResponse;
import cl.nubeplay.api.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/productos")
@Tag(name = "3. Productos", description = "Lectura pública (tienda, vendedor); creación, edición y eliminación solo Administrador")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    @Operation(summary = "Listar productos (público)")
    @ApiResponse(responseCode = "200", description = "Éxito")
    public List<ProductoResponse> listar(
            @Parameter(description = "Filtra por el valor de la categoría (ej: consolas)") @RequestParam(required = false) String categoria,
            @Parameter(description = "true = solo productos en stock crítico") @RequestParam(defaultValue = "false") boolean criticos) {
        return productoService.listar(categoria, criticos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un producto (público)")
    @ApiResponse(responseCode = "200", description = "Éxito")
    @ApiResponse(responseCode = "404", description = "No encontrado")
    public ProductoResponse obtener(@PathVariable Long id) {
        return productoService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Crear producto (Administrador)")
    @ApiResponse(responseCode = "201", description = "Creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o categoría inexistente")
    @ApiResponse(responseCode = "401", description = "Sin token")
    @ApiResponse(responseCode = "403", description = "Rol sin permiso (Vendedor / Cliente)")
    @ApiResponse(responseCode = "409", description = "Código duplicado")
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest request) {
        return productoService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Actualizar producto (Administrador)")
    @ApiResponse(responseCode = "200", description = "Actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "403", description = "Rol sin permiso")
    @ApiResponse(responseCode = "404", description = "No encontrado")
    @ApiResponse(responseCode = "409", description = "Código duplicado")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return productoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Eliminar producto (Administrador)")
    @ApiResponse(responseCode = "204", description = "Eliminado")
    @ApiResponse(responseCode = "403", description = "Rol sin permiso")
    @ApiResponse(responseCode = "404", description = "No encontrado")
    public void eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
    }
}
