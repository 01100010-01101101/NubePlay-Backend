package cl.nubeplay.api.controller;

import cl.nubeplay.api.dto.CatalogoDto.CategoriaRequest;
import cl.nubeplay.api.dto.CatalogoDto.CategoriaResponse;
import cl.nubeplay.api.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias")
@Tag(name = "2. Categorías", description = "Lectura pública; creación, edición y eliminación solo Administrador")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    @Operation(summary = "Listar categorías (público)")
    @ApiResponse(responseCode = "200", description = "Éxito")
    public List<CategoriaResponse> listar() {
        return categoriaService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una categoría (público)")
    @ApiResponse(responseCode = "200", description = "Éxito")
    @ApiResponse(responseCode = "404", description = "No encontrada")
    public CategoriaResponse obtener(@PathVariable Long id) {
        return categoriaService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Crear categoría (Administrador)")
    @ApiResponse(responseCode = "201", description = "Creada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "401", description = "Sin token")
    @ApiResponse(responseCode = "403", description = "Rol sin permiso")
    @ApiResponse(responseCode = "409", description = "Nombre duplicado")
    public CategoriaResponse crear(@Valid @RequestBody CategoriaRequest request) {
        return categoriaService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Actualizar categoría (Administrador)")
    @ApiResponse(responseCode = "200", description = "Actualizada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "403", description = "Rol sin permiso")
    @ApiResponse(responseCode = "404", description = "No encontrada")
    public CategoriaResponse actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {
        return categoriaService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Eliminar categoría (Administrador)", description = "No permite eliminar categorías con productos.")
    @ApiResponse(responseCode = "204", description = "Eliminada")
    @ApiResponse(responseCode = "403", description = "Rol sin permiso")
    @ApiResponse(responseCode = "404", description = "No encontrada")
    @ApiResponse(responseCode = "409", description = "La categoría tiene productos")
    public void eliminar(@PathVariable Long id) {
        categoriaService.eliminar(id);
    }
}
