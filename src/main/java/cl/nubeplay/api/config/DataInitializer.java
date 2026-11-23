package cl.nubeplay.api.config;

import cl.nubeplay.api.model.Categoria;
import cl.nubeplay.api.model.Producto;
import cl.nubeplay.api.model.Rol;
import cl.nubeplay.api.model.Usuario;
import cl.nubeplay.api.repository.CategoriaRepository;
import cl.nubeplay.api.repository.ProductoRepository;
import cl.nubeplay.api.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDate;

/**
 * Carga datos iniciales la primera vez que se levanta el backend (tablas vacías):
 * categorías y productos del catálogo (datos/semillas.json) y un usuario por rol.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public DataInitializer(CategoriaRepository categoriaRepository, ProductoRepository productoRepository,
                           UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, ObjectMapper objectMapper) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (categoriaRepository.count() == 0) {
            cargarCatalogo();
        }
        crearUsuarioSiNoExiste("111111111", "Administrador", "NUBEPLAY", "admin@duoc.cl", "admin", Rol.ADMIN);
        crearUsuarioSiNoExiste("222222222", "Valentina", "Vendedora", "vendedor@duoc.cl", "vendedor", Rol.VENDEDOR);
        crearUsuarioSiNoExiste("333333333", "Carlos", "Cliente", "cliente@gmail.com", "cliente", Rol.CLIENTE);
    }

    private void cargarCatalogo() throws Exception {
        try (InputStream entrada = new ClassPathResource("datos/semillas.json").getInputStream()) {
            JsonNode raiz = objectMapper.readTree(entrada);

            for (JsonNode c : raiz.get("categorias")) {
                Categoria categoria = new Categoria();
                categoria.setValor(c.get("valor").asText());
                categoria.setNombre(c.get("nombre").asText());
                categoria.setDescripcion(c.path("descripcion").asText(null));
                categoria.setIcono(c.path("icono").asText("bi-tag"));
                categoriaRepository.save(categoria);
            }

            int total = 0;
            for (JsonNode p : raiz.get("productos")) {
                Categoria categoria = categoriaRepository.findByValor(p.get("categoria").asText()).orElse(null);
                if (categoria == null) continue;
                Producto producto = new Producto();
                producto.setCodigo(p.get("codigo").asText());
                producto.setNombre(p.get("nombre").asText());
                producto.setDescripcion(p.path("descripcion").asText(null));
                producto.setPrecio(p.get("precio").asInt());
                producto.setStock(p.get("stock").asInt());
                producto.setStockCritico(p.path("stockCritico").asInt(0));
                producto.setImagen(p.path("imagen").asText(null));
                producto.setCategoria(categoria);
                productoRepository.save(producto);
                total++;
            }
            log.info("Catálogo inicial cargado: {} categorías y {} productos.", categoriaRepository.count(), total);
        }
    }

    private void crearUsuarioSiNoExiste(String run, String nombre, String apellidos, String correo, String password, Rol rol) {
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) return;
        Usuario usuario = new Usuario();
        usuario.setRun(run);
        usuario.setNombre(nombre);
        usuario.setApellidos(apellidos);
        usuario.setCorreo(correo);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        usuario.setRegion("Metropolitana de Santiago");
        usuario.setComuna("Santiago");
        usuario.setDireccion("Av. Siempre Viva 742, Santiago");
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
        log.info("Usuario de prueba creado: {} ({})", correo, rol.getEtiqueta());
    }
}
