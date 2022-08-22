package it.bologna.ausl.jnjclient.firmajnj.signer.exceptions;

/**
 *
 * @author gdm
 */
public class UserAbortException extends FirmaJnJException {

    public UserAbortException(String message) {
        super(message);
    }

    public UserAbortException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserAbortException(Throwable cause) {
        super(cause);
    }
}
