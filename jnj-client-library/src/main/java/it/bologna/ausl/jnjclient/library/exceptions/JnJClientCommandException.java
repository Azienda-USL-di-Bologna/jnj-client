package it.bologna.ausl.jnjclient.library.exceptions;

/**
 *
 * @author gdm
 */
public class JnJClientCommandException extends Exception {

    public JnJClientCommandException(String string) {
        super(string);
    }

    public JnJClientCommandException(String string, Throwable thrwbl) {
        super(string, thrwbl);
    }

    public JnJClientCommandException(Throwable thrwbl) {
        super(thrwbl);
    }
    
}
