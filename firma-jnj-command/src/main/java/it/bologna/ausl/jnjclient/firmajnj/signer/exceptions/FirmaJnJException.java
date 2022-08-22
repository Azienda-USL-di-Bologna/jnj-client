package it.bologna.ausl.jnjclient.firmajnj.signer.exceptions;

/**
 *
 * @author gdm
 */
public abstract class FirmaJnJException extends Exception {

    public FirmaJnJException() {
    }

    public FirmaJnJException(String message) {
        super(message);
    }

    public FirmaJnJException(String message, Throwable cause) {
        super(message, cause);
    }

    public FirmaJnJException(Throwable cause) {
        super(cause);
    }

    public FirmaJnJException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
    
}
