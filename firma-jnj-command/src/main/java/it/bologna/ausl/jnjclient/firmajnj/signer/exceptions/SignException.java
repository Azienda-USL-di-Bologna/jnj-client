package it.bologna.ausl.jnjclient.firmajnj.signer.exceptions;

/**
 *
 * @author gdm
 */
public class SignException extends FirmaJnJException {

    public SignException(String message) {
        super(message);
    }

    public SignException(String message, Throwable cause) {
        super(message, cause);
    }

    public SignException(Throwable cause) {
        super(cause);
    }
}
