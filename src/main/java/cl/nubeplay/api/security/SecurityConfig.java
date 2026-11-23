package cl.nubeplay.api.security;

import cl.nubeplay.api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración de Spring Security:
 * - API sin estado (STATELESS): no hay sesión en el servidor, cada petición trae su JWT.
 * - CSRF deshabilitado porque no se usan cookies de sesión (el token va en el header Authorization).
 * - CORS solo para los orígenes del frontend configurados en app.cors.allowed-origins.
 * - Reglas de acceso por rol (ADMIN, VENDEDOR, CLIENTE). Además, cada controlador usa @PreAuthorize.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String API = "/api/v1";

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter, RestAuthEntryPoint restAuth) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(restAuth).accessDeniedHandler(restAuth))
                .authorizeHttpRequests(auth -> auth
                        // Preflight CORS y Swagger
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()

                        // Autenticación pública
                        .requestMatchers(HttpMethod.POST, API + "/auth/login", API + "/auth/registro", API + "/auth/refresh", API + "/auth/google").permitAll()

                        // Catálogo: lectura pública (tienda), escritura solo ADMIN
                        .requestMatchers(HttpMethod.GET, API + "/productos", API + "/productos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, API + "/categorias", API + "/categorias/**").permitAll()
                        .requestMatchers(API + "/productos/**", API + "/productos").hasRole("ADMIN")
                        .requestMatchers(API + "/categorias/**", API + "/categorias").hasRole("ADMIN")

                        // Usuarios: el perfil propio para cualquier autenticado, el resto solo ADMIN
                        .requestMatchers(API + "/usuarios/me").authenticated()
                        .requestMatchers(API + "/usuarios", API + "/usuarios/**").hasRole("ADMIN")

                        // Boletas (órdenes)
                        .requestMatchers(HttpMethod.GET, API + "/boletas").hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers(HttpMethod.GET, API + "/boletas/**").authenticated() // mis-boletas y {id} (se valida el dueño)
                        .requestMatchers(HttpMethod.POST, API + "/boletas").authenticated()
                        .requestMatchers(API + "/boletas/**").hasRole("ADMIN") // cambiar estado y eliminar

                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** El filtro JWT solo debe ejecutarse dentro de la cadena de Spring Security, no como filtro global. */
    @Bean
    public FilterRegistrationBean<JwtAuthFilter> registroJwtFilter(JwtAuthFilter filtro) {
        FilterRegistrationBean<JwtAuthFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    /** Carga el usuario desde MySQL; el rol queda como autoridad "ROLE_ADMIN", "ROLE_VENDEDOR" o "ROLE_CLIENTE". */
    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository usuarioRepository) {
        return correo -> usuarioRepository.findByCorreoIgnoreCase(correo)
                .map(u -> User.withUsername(u.getCorreo())
                        .password(u.getPassword())
                        .roles(u.getRol().name())
                        .disabled(!u.isActivo())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    /** BCrypt: hash con sal, las contraseñas nunca se guardan en texto plano. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") String origenes) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origenes.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
