package it.bologna.ausl.jnjclient.firmajnj.signer.exceptions;

/**
 *
 * @author gdm
 */
public abstract class PKCSException extends FirmaJnJException {

    public PKCSException(String message) {
        super(message);
    }

    public PKCSException(String message, Throwable cause) {
        super(message, cause);
    }

    public PKCSException(Throwable cause) {
        super(cause);
    }
}
