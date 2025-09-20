package az.millikart.apusspring.exception;

public class ApplicationException extends RuntimeException {

    public ApplicationException() {
        super();
    }

    public ApplicationException(String string, Object... arg) {
        super(String.format(string, arg));
    }
}
