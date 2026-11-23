package cl.nubeplay.api.model;


import java.text.Normalizer;


public enum Rol {
    ADMIN("Administrador"),
    VENDEDOR("Vendedor"),
    CLIENTE("Cliente");


    private final String etiqueta;


    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }



    public String getEtiqueta() {
        return etiqueta;
    }


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

