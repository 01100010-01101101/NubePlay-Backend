package cl.nubeplay.api.security;

import cl.nubeplay.api.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Genera y verifica los JWT firmados con HMAC-SHA256.
 * - accessToken: vida corta (15 min por defecto), se usa en cada petición.
 * - refreshToken: vida larga (7 días), solo sirve en /auth/refresh para obtener un nuevo par de tokens.
 * El claim "typ" impide usar un refresh token como access token y viceversa.
 */
@Service
public class JwtService {

    public static final String TIPO_ACCESS = "access";
    public static final String TIPO_REFRESH = "refresh";

    private final SecretKey clave;
    private final long expiracionAccessMs;
    private final long expiracionRefreshMs;
    private final String emisor;

    public JwtService(@Value("${app.jwt.secret}") String secretoBase64,
                      @Value("${app.jwt.access-expiration-ms}") long expiracionAccessMs,
                      @Value("${app.jwt.refresh-expiration-ms}") long expiracionRefreshMs,
                      @Value("${app.jwt.issuer:nubeplay-api}") String emisor) {
        byte[] bytes = Decoders.BASE64.decode(secretoBase64);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret debe tener al menos 256 bits (32 bytes en Base64).");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.expiracionAccessMs = expiracionAccessMs;
        this.expiracionRefreshMs = expiracionRefreshMs;
        this.emisor = emisor;
    }

    public String generarAccessToken(Usuario usuario) {
        return generar(usuario, TIPO_ACCESS, expiracionAccessMs);
    }

    public String generarRefreshToken(Usuario usuario) {
        return generar(usuario, TIPO_REFRESH, expiracionRefreshMs);
    }

    private String generar(Usuario usuario, String tipo, long duracionMs) {
        Date ahora = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(emisor)
                .subject(usuario.getCorreo())
                .claim("uid", usuario.getId())
                .claim("rol", usuario.getRol().name())
                .claim("nombre", usuario.getNombre())
                .claim("typ", tipo)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + duracionMs))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Verifica firma, emisor, expiración y tipo. Lanza JwtException (o ExpiredJwtException) si no es válido.
     */
    public Claims validar(String token, String tipoEsperado) {
        Claims claims = Jwts.parser()
                .verifyWith(clave)
                .requireIssuer(emisor)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!tipoEsperado.equals(claims.get("typ", String.class))) {
            throw new JwtException("Tipo de token incorrecto.");
        }
        return claims;
    }

    public long getExpiracionAccessSegundos() {
        return expiracionAccessMs / 1000;
    }
}
