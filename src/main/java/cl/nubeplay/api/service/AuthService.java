package cl.nubeplay.api.service;


import cl.nubeplay.api.dto.AuthDto.AuthResponse;
import cl.nubeplay.api.dto.AuthDto.LoginRequest;
import cl.nubeplay.api.dto.AuthDto.RegistroRequest;
import cl.nubeplay.api.dto.UsuarioDto.UsuarioResponse;
import cl.nubeplay.api.exception.ApiException;
import cl.nubeplay.api.model.Rol;
import cl.nubeplay.api.model.Usuario;
import cl.nubeplay.api.repository.UsuarioRepository;
import cl.nubeplay.api.dto.UsuarioDto;
import cl.nubeplay.api.security.GoogleTokenService;
import cl.nubeplay.api.security.GoogleTokenService.UsuarioGoogle;
import cl.nubeplay.api.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.UUID;


/** Inicio de sesión, registro y renovación de tokens. */
@Service
public class AuthService {


    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenService googleTokenService;


    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       GoogleTokenService googleTokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleTokenService = googleTokenService;
    }


    @Transactional
    public AuthResponse loginGoogle(String credencial) {
        UsuarioGoogle google = googleTokenService.verificar(credencial);


        if (!google.correoVerificado() || google.correo() == null) {
            throw ApiException.noAutorizado("Tu cuenta de Google no tiene el correo verificado.");
        }
        String correo = google.correo().trim().toLowerCase();
        if (!correo.matches(UsuarioDto.REGEX_CORREO)) {
            throw ApiException.prohibido("Solo se aceptan cuentas @duoc.cl, @profesor.duoc.cl o @gmail.com.");
        }


        Usuario usuario = usuarioRepository.findByGoogleId(google.id())
                .or(() -> usuarioRepository.findByCorreoIgnoreCase(correo))
                .orElseGet(() -> {
                    Usuario nuevo = new Usuario();
                    nuevo.setCorreo(correo);
                    nuevo.setNombre(recortar(google.nombre(), 50));
                    nuevo.setApellidos(recortar(google.apellidos(), 100));
                    // Contraseña aleatoria: la cuenta entra con Google; no se puede adivinar ni usar en el login normal.
                    nuevo.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                    nuevo.setRol(Rol.CLIENTE);
                    nuevo.setActivo(true);
                    return nuevo;
                });


        if (!usuario.isActivo()) {
            throw ApiException.prohibido("Tu cuenta se encuentra inactiva. Contacta al soporte.");
        }
        if (usuario.getGoogleId() == null) {
            usuario.setGoogleId(google.id()); // vincula la cuenta de Google
        } else if (!usuario.getGoogleId().equals(google.id())) {
            throw ApiException.conflicto("Este correo ya está vinculado a otra cuenta de Google.", null);
        }
        return emitirTokens(usuarioRepository.save(usuario));
    }


    private static String recortar(String texto, int maximo) {
        String limpio = texto == null ? "" : texto.trim();
        return limpio.length() > maximo ? limpio.substring(0, maximo) : limpio;
    }


    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(request.correo().trim())
                .filter(u -> passwordEncoder.matches(request.password().trim(), u.getPassword()))
                // Mismo mensaje para correo o contraseña incorrectos: no se revela qué cuentas existen.
                .orElseThrow(() -> ApiException.noAutorizado("Correo o contraseña incorrectos."));


        if (!usuario.isActivo()) {
            throw ApiException.prohibido("Tu cuenta se encuentra inactiva. Contacta al soporte.");
        }
        return emitirTokens(usuario);
    }


    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {
        String correo = request.correo().trim().toLowerCase();
        String run = Mapper.limpiarRun(request.run());
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw ApiException.conflicto("El correo electrónico ya se encuentra registrado.", "correo");
        }
        if (usuarioRepository.existsByRunIgnoreCase(run)) {
            throw ApiException.conflicto("El RUN ya se encuentra registrado.", "run");
        }


        Usuario usuario = new Usuario();
        usuario.setRun(run);
        usuario.setNombre(request.nombre().trim());
        usuario.setApellidos(request.apellidos().trim());
        usuario.setCorreo(correo);
        usuario.setPassword(passwordEncoder.encode(request.password().trim()));
        usuario.setFechaNacimiento(Mapper.fecha(request.fechaNacimiento(), "fechaNacimiento"));
        usuario.setRegion(request.region());
        usuario.setComuna(request.comuna());
        usuario.setDireccion(request.direccion().trim());
        usuario.setRol(Rol.CLIENTE);
        usuario.setActivo(true);
        return Mapper.usuario(usuarioRepository.save(usuario), List.of());
    }


    @Transactional(readOnly = true)
    public AuthResponse refrescar(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.validar(refreshToken, JwtService.TIPO_REFRESH);
        } catch (ExpiredJwtException ex) {
            throw ApiException.noAutorizado("La sesión expiró. Inicia sesión nuevamente.");
        } catch (JwtException | IllegalArgumentException ex) {
            throw ApiException.noAutorizado("Refresh token inválido.");
        }


        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(claims.getSubject())
                .orElseThrow(() -> ApiException.noAutorizado("El usuario ya no existe."));
        if (!usuario.isActivo()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tu cuenta se encuentra inactiva.", null);
        }
        return emitirTokens(usuario);
    }


    private AuthResponse emitirTokens(Usuario usuario) {
        return new AuthResponse(
                jwtService.generarAccessToken(usuario),
                jwtService.generarRefreshToken(usuario),
                "Bearer",
                jwtService.getExpiracionAccessSegundos(),
                Mapper.usuario(usuario, List.of()));
    }
}

