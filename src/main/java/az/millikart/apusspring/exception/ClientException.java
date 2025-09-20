package az.millikart.apusspring.exception;

public class ClientException extends Exception {

    public ClientException() {
        super();
    }

    public ClientException(Throwable thrwbl) {
        super(thrwbl);
    }

    public ClientException(String message, Object... args) {
        super(String.format(message, args));
    }
}
