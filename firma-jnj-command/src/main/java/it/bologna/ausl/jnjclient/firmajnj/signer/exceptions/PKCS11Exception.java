package it.bologna.ausl.jnjclient.firmajnj.signer.exceptions;

/**
 *
 * @author gdm
 */
public class PKCS11Exception extends PKCSException {

    public PKCS11Exception(String message) {
        super(message);
    }

    public PKCS11Exception(String message, Throwable cause) {
        super(message, cause);
    }

    public PKCS11Exception(Throwable cause) {
        super(cause);
    }
}
