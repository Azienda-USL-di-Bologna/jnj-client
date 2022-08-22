package it.bologna.ausl.jnjclient.firmajnj.signer.exceptions;

/**
 *
 * @author gdm
 */
public class SystemAbortException extends FirmaJnJException {

    public SystemAbortException(String message) {
        super(message);
    }

    public SystemAbortException(String message, Throwable cause) {
        super(message, cause);
    }

    public SystemAbortException(Throwable cause) {
        super(cause);
    }
}
