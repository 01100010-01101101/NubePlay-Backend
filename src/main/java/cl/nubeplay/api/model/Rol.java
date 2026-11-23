package cl.nubeplay.api.model;

import java.text.Normalizer;

/**
 * Roles del sistema (Anexo 1):
 * - ADMIN: acceso total.
 * - VENDEDOR: solo ve productos y órdenes (lectura).
 * - CLIENTE: solo accede a la tienda.
 */
public enum Rol {
    ADMIN("Administrador"),
    VENDEDOR("Vendedor"),
    CLIENTE("Cliente");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Nombre visible que usa el frontend ("Administrador", "Vendedor", "Cliente"). */
    public String getEtiqueta() {
        return etiqueta;
    }

    /** Acepta "ADMIN", "Administrador", "admin", "Vendedor", etc. Devuelve null si no se reconoce. */
    public static Rol desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String limpio = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase();
        return switch (limpio) {
            case "ADMIN", "ADMINISTRADOR" -> ADMIN;
            case "VENDEDOR" -> VENDEDOR;
            case "CLIENTE" -> CLIENTE;
            default -> null;
        };
    }
}
