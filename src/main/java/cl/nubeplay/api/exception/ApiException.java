package cl.nubeplay.api.exception;


import org.springframework.http.HttpStatus;



public class ApiException extends RuntimeException {


    private final HttpStatus status;
    private final String campo;


    public ApiException(HttpStatus status, String mensaje, String campo) {
        super(mensaje);
        this.status = status;
        this.campo = campo;
    }


    public static ApiException noEncontrado(String mensaje) {
        return new ApiException(HttpStatus.NOT_FOUND, mensaje, null);
    }


    public static ApiException conflicto(String mensaje, String campo) {
        return new ApiException(HttpStatus.CONFLICT, mensaje, campo);
    }


    public static ApiException solicitudInvalida(String mensaje, String campo) {
        return new ApiException(HttpStatus.BAD_REQUEST, mensaje, campo);
    }


    public static ApiException noAutorizado(String mensaje) {
        return new ApiException(HttpStatus.UNAUTHORIZED, mensaje, null);
    }


    public static ApiException prohibido(String mensaje) {
        return new ApiException(HttpStatus.FORBIDDEN, mensaje, null);
    }


    public HttpStatus getStatus() {
        return status;
    }


    public String getCampo() {
        return campo;
    }
}

