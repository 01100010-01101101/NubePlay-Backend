    package cl.nubeplay.api.service;


    import cl.nubeplay.api.dto.UsuarioDto.CompraResumen;
    import cl.nubeplay.api.dto.UsuarioDto.PerfilRequest;
    import cl.nubeplay.api.dto.UsuarioDto.UsuarioRequest;
    import cl.nubeplay.api.dto.UsuarioDto.UsuarioResponse;
    import cl.nubeplay.api.exception.ApiException;
    import cl.nubeplay.api.model.Boleta;
    import cl.nubeplay.api.model.Rol;
    import cl.nubeplay.api.model.Usuario;
    import cl.nubeplay.api.repository.BoletaRepository;
    import cl.nubeplay.api.repository.UsuarioRepository;
    import org.springframework.data.domain.Sort;
    import org.springframework.security.crypto.password.PasswordEncoder;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;


    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;


    @Service
    public class UsuarioService {


        private final UsuarioRepository usuarioRepository;
        private final BoletaRepository boletaRepository;
        private final PasswordEncoder passwordEncoder;


        public UsuarioService(UsuarioRepository usuarioRepository, BoletaRepository boletaRepository, PasswordEncoder passwordEncoder) {
            this.usuarioRepository = usuarioRepository;
            this.boletaRepository = boletaRepository;
            this.passwordEncoder = passwordEncoder;
        }


        /** Lista para el administrador, con el historial de compras de cada usuario. */
        @Transactional(readOnly = true)
        public List<UsuarioResponse> listar() {
            Map<Long, List<CompraResumen>> compras = new HashMap<>();
            for (Boleta boleta : boletaRepository.findAll(Sort.by("id"))) {
                if (boleta.getUsuario() == null) continue;
                compras.computeIfAbsent(boleta.getUsuario().getId(), k -> new ArrayList<>()).addAll(resumir(boleta));
            }
            return usuarioRepository.findAll(Sort.by("id")).stream()
                    .map(u -> Mapper.usuario(u, compras.getOrDefault(u.getId(), List.of())))
                    .toList();
        }


        @Transactional(readOnly = true)
        public UsuarioResponse obtener(Long id) {
            Usuario usuario = buscar(id);
            return Mapper.usuario(usuario, comprasDe(id));
        }


        @Transactional
        public UsuarioResponse crear(UsuarioRequest request) {
            validarDuplicados(request, null);
            if (request.password() == null || request.password().trim().length() < 4) {
                throw ApiException.solicitudInvalida("La contraseña debe tener entre 4 y 10 caracteres.", "password");
            }
            Usuario usuario = new Usuario();
            aplicar(usuario, request);
            usuario.setPassword(passwordEncoder.encode(request.password().trim()));
            usuario.setRol(rolDesde(request.rol(), Rol.CLIENTE));
            usuario.setActivo(!"Inactivo".equalsIgnoreCase(request.estado()));
            return Mapper.usuario(usuarioRepository.save(usuario), List.of());
        }


        /**
         * Edición por el administrador. En su propia cuenta no puede cambiarse el rol ni el estado,
         * para no quedar fuera del panel.
         */
        @Transactional
        public UsuarioResponse actualizar(Long id, UsuarioRequest request, String correoActual) {
            Usuario usuario = buscar(id);
            validarDuplicados(request, id);
            boolean esPropio = usuario.getCorreo().equalsIgnoreCase(correoActual);


            aplicar(usuario, request);
            if (request.password() != null && !request.password().isBlank()) {
                if (request.password().trim().length() < 4) {
                    throw ApiException.solicitudInvalida("La contraseña debe tener entre 4 y 10 caracteres.", "password");
                }
                usuario.setPassword(passwordEncoder.encode(request.password().trim()));
            }
            if (!esPropio) {
                usuario.setRol(rolDesde(request.rol(), usuario.getRol()));
                if (request.estado() != null && !request.estado().isBlank()) {
                    usuario.setActivo(!"Inactivo".equalsIgnoreCase(request.estado()));
                }
            }
            return Mapper.usuario(usuarioRepository.save(usuario), comprasDe(id));
        }


        /** Un administrador no puede eliminar su propia cuenta. Las boletas del usuario se conservan. */
        @Transactional
        public void eliminar(Long id, String correoActual) {
            Usuario usuario = buscar(id);
            if (usuario.getCorreo().equalsIgnoreCase(correoActual)) {
                throw ApiException.conflicto("No puedes eliminar tu propia cuenta.", null);
            }
            boletaRepository.desvincularUsuario(id);
            usuarioRepository.delete(usuario);
        }


        // ------------------------------------------------------------------
        // Perfil propio (/usuarios/me)
        // ------------------------------------------------------------------


        @Transactional(readOnly = true)
        public UsuarioResponse perfil(String correo) {
            Usuario usuario = buscarPorCorreo(correo);
            return Mapper.usuario(usuario, comprasDe(usuario.getId()));
        }


        @Transactional
        public UsuarioResponse actualizarPerfil(String correo, PerfilRequest request) {
            Usuario usuario = buscarPorCorreo(correo);
            usuario.setNombre(request.nombre().trim());
            usuario.setApellidos(request.apellidos().trim());


            String nueva = request.passwordNueva();
            if (nueva != null && !nueva.isBlank()) {
                if (request.passwordActual() == null || !passwordEncoder.matches(request.passwordActual(), usuario.getPassword())) {
                    throw ApiException.solicitudInvalida("La contraseña actual no es correcta.", "passwordActual");
                }
                if (nueva.trim().length() < 4 || nueva.trim().length() > 10) {
                    throw ApiException.solicitudInvalida("La nueva contraseña debe tener entre 4 y 10 caracteres.", "passwordNueva");
                }
                usuario.setPassword(passwordEncoder.encode(nueva.trim()));
            }
            return Mapper.usuario(usuarioRepository.save(usuario), comprasDe(usuario.getId()));
        }


        // ------------------------------------------------------------------


        private void aplicar(Usuario usuario, UsuarioRequest request) {
            usuario.setRun(Mapper.limpiarRun(request.run()));
            usuario.setNombre(request.nombre().trim());
            usuario.setApellidos(request.apellidos().trim());
            usuario.setCorreo(request.correo().trim().toLowerCase());
            usuario.setFechaNacimiento(Mapper.fecha(request.fechaNacimiento(), "fechaNacimiento"));
            usuario.setRegion(Mapper.limpiar(request.region()));
            usuario.setComuna(Mapper.limpiar(request.comuna()));
            usuario.setDireccion(Mapper.limpiar(request.direccion()));
        }


        private void validarDuplicados(UsuarioRequest request, Long id) {
            String correo = request.correo().trim();
            String run = Mapper.limpiarRun(request.run());
            boolean correoDuplicado = id == null
                    ? usuarioRepository.existsByCorreoIgnoreCase(correo)
                    : usuarioRepository.existsByCorreoIgnoreCaseAndIdNot(correo, id);
            if (correoDuplicado) {
                throw ApiException.conflicto("Ya existe un usuario con ese correo.", "correo");
            }
            boolean runDuplicado = id == null
                    ? usuarioRepository.existsByRunIgnoreCase(run)
                    : usuarioRepository.existsByRunIgnoreCaseAndIdNot(run, id);
            if (runDuplicado) {
                throw ApiException.conflicto("Ya existe un usuario con ese RUN.", "run");
            }
        }


        private Rol rolDesde(String texto, Rol porDefecto) {
            if (texto == null || texto.isBlank()) return porDefecto;
            Rol rol = Rol.desdeTexto(texto);
            if (rol == null) {
                throw ApiException.solicitudInvalida("Rol inválido. Usa Administrador, Vendedor o Cliente.", "rol");
            }
            return rol;
        }


        private List<CompraResumen> comprasDe(Long usuarioId) {
            List<CompraResumen> compras = new ArrayList<>();
            for (Boleta boleta : boletaRepository.findByUsuarioIdOrderByIdDesc(usuarioId)) {
                compras.addAll(resumir(boleta));
            }
            return compras;
        }


        private List<CompraResumen> resumir(Boleta boleta) {
            String fecha = boleta.getFecha().toLocalDate().toString();
            return boleta.getDetalles().stream()
                    .map(d -> new CompraResumen(fecha, d.getNombreProducto(), d.getCantidad(), d.calcularSubtotal()))
                    .toList();
        }


        private Usuario buscar(Long id) {
            return usuarioRepository.findById(id)
                    .orElseThrow(() -> ApiException.noEncontrado("El usuario " + id + " no existe."));
        }


        private Usuario buscarPorCorreo(String correo) {
            return usuarioRepository.findByCorreoIgnoreCase(correo)
                    .orElseThrow(() -> ApiException.noAutorizado("La sesión ya no es válida."));
        }
    }



