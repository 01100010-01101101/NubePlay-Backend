package cl.nubeplay.api.service;


import cl.nubeplay.api.dto.BoletaDto;
import cl.nubeplay.api.dto.CatalogoDto;
import cl.nubeplay.api.dto.UsuarioDto;
import cl.nubeplay.api.exception.ApiException;
import cl.nubeplay.api.model.Boleta;
import cl.nubeplay.api.model.Categoria;
import cl.nubeplay.api.model.Producto;
import cl.nubeplay.api.model.Usuario;


import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;


public final class Mapper {


    private Mapper() {
    }


    public static UsuarioDto.UsuarioResponse usuario(Usuario u, List<UsuarioDto.CompraResumen> compras) {
        return new UsuarioDto.UsuarioResponse(
                u.getId(),
                u.getRun() != null ? u.getRun() : "",
                u.getNombre(),
                u.getApellidos(),
                u.getCorreo(),
                u.getFechaNacimiento() != null ? u.getFechaNacimiento().toString() : "",
                u.getRegion(),
                u.getComuna(),
                u.getDireccion(),
                u.getRol().getEtiqueta(),
                u.isActivo() ? "Activo" : "Inactivo",
                compras != null ? compras : List.of(),
                u.getGoogleId() != null);
    }


    public static CatalogoDto.CategoriaResponse categoria(Categoria c, long cantidadProductos) {
        return new CatalogoDto.CategoriaResponse(c.getId(), c.getValor(), c.getNombre(), c.getDescripcion(), c.getIcono(), cantidadProductos);
    }


    public static CatalogoDto.ProductoResponse producto(Producto p) {
        return new CatalogoDto.ProductoResponse(
                p.getId(),
                p.getCodigo(),
                p.getNombre(),
                p.getDescripcion(),
                p.getPrecio(),
                p.getStock(),
                p.getStockCritico(),
                p.getImagen(),
                p.getCategoria().getValor(),
                p.getCategoria().getNombre(),
                p.enStockCritico());
    }



    public static BoletaDto.BoletaResponse boleta(Boleta b) {
        var comprador = new BoletaDto.Comprador(b.getCompradorNombre(), b.getCompradorApellidos(), b.getCompradorCorreo());
        var envio = new BoletaDto.Envio(b.getCalle(), b.getDepartamento(), b.getRegion(), b.getComuna(), b.getIndicaciones());
        var items = b.getDetalles().stream()
                .map(d -> new BoletaDto.ItemResponse(
                        d.getProducto() != null ? d.getProducto().getId() : d.getId(),
                        d.getNombreProducto(),
                        d.getPrecioUnitario(),
                        d.getCantidad(),
                        d.getImagen(),
                        d.getCategoria()))
                .toList();
        return new BoletaDto.BoletaResponse(
                b.getId(),
                b.getUsuario() != null ? b.getUsuario().getId() : null,
                (b.getCompradorNombre() + " " + b.getCompradorApellidos()).trim(),
                b.getCompradorCorreo(),
                comprador,
                envio,
                b.getFecha().toLocalDate().toString(),
                b.getEstado(),
                items,
                b.getSubtotal(),
                b.getDescuento(),
                b.getCupon(),
                b.getTotal());
    }


    public static String slug(String texto) {
        String base = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return base.isEmpty() ? "categoria" : base;
    }


    public static LocalDate fecha(String texto, String campo) {
        if (texto == null || texto.isBlank()) return null;
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException ex) {
            throw ApiException.solicitudInvalida("La fecha debe tener el formato AAAA-MM-DD.", campo);
        }
    }


    public static String limpiar(String texto) {
        return texto == null ? null : texto.trim();
    }



    public static String limpiarRun(String run) {
        return run == null ? null : run.replaceAll("[.\\-\\s]", "").toUpperCase();
    }
}



