package it.bologna.ausl.jnjclient.library;

import it.bologna.ausl.jnjclient.library.exceptions.JnJClientCommandException;

/**
 *
 * @author gdm
 */
public interface JnJClientCommand {

    public String getCommand();
    
    public <T> T executeCommand(JnJProgressBar progressBar, String... params) throws JnJClientCommandException;
}
