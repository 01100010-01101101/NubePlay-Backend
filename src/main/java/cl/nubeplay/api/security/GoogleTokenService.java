package cl.nubeplay.api.security;

import cl.nubeplay.api.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;

/**
 * Verifica el "ID token" que entrega el botón de Google (Google Identity Services).
 * <ul>
 *   <li>Firma RS256 contra las claves públicas de Google (JWKS, se descargan y cachean).</li>
 *   <li>Emisor: accounts.google.com.</li>
 *   <li>Audiencia: debe ser NUESTRO client ID (evita aceptar tokens emitidos para otra aplicación).</li>
 *   <li>Fecha de expiración vigente.</li>
 * </ul>
 * Nunca se confía en datos enviados por el navegador sin esta verificación.
 */
@Service
public class GoogleTokenService {

    private static final String JWKS_GOOGLE = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> EMISORES = Set.of("accounts.google.com", "https://accounts.google.com");

    private final String clientId;
    private volatile JwtDecoder decoder;

    public GoogleTokenService(@Value("${app.google.client-id:}") String clientId) {
        this.clientId = clientId == null ? "" : clientId.trim();
    }

    /** Datos de la cuenta de Google que nos interesan. */
    public record UsuarioGoogle(String id, String correo, boolean correoVerificado, String nombre, String apellidos) {
    }

    public boolean estaConfigurado() {
        return !clientId.isEmpty();
    }

    public UsuarioGoogle verificar(String credencial) {
        if (!estaConfigurado()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "El inicio de sesión con Google no está configurado en el servidor.", null);
        }
        try {
            Jwt jwt = decoder().decode(credencial);
            String nombreCompleto = jwt.getClaimAsString("name");
            String nombre = jwt.getClaimAsString("given_name");
            String apellidos = jwt.getClaimAsString("family_name");
            return new UsuarioGoogle(
                    jwt.getSubject(),
                    jwt.getClaimAsString("email"),
                    Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")),
                    nombre != null ? nombre : (nombreCompleto != null ? nombreCompleto : "Usuario"),
                    apellidos != null ? apellidos : "");
        } catch (JwtException | IllegalArgumentException ex) {
            throw ApiException.noAutorizado("No se pudo validar la cuenta de Google. Intenta nuevamente.");
        }
    }

    private JwtDecoder decoder() {
        if (decoder == null) {
            synchronized (this) {
                if (decoder == null) {
                    NimbusJwtDecoder nuevo = NimbusJwtDecoder.withJwkSetUri(JWKS_GOOGLE).build();
                    OAuth2TokenValidator<Jwt> emisor = new JwtClaimValidator<Object>(JwtClaimNames.ISS,
                            iss -> iss != null && EMISORES.contains(iss.toString()));
                    OAuth2TokenValidator<Jwt> audiencia = new JwtClaimValidator<Object>(JwtClaimNames.AUD,
                            aud -> aud instanceof Collection<?> lista ? lista.contains(clientId) : clientId.equals(String.valueOf(aud)));
                    nuevo.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), emisor, audiencia));
                    decoder = nuevo;
                }
            }
        }
        return decoder;
    }
}
