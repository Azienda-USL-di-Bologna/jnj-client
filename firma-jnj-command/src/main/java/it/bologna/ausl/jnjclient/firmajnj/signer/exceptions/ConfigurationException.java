package it.bologna.ausl.jnjclient.firmajnj.signer.exceptions;

/**
 *
 * @author gdm
 */
public class ConfigurationException extends FirmaJnJException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigurationException(Throwable cause) {
        super(cause);
    }
}
