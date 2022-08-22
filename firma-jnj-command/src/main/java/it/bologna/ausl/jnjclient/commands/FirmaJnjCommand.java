package it.bologna.ausl.jnjclient.commands;

import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParams;
import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParamsComponent;
import it.bologna.ausl.jnjclient.firmajnj.DocumentSigner;
import it.bologna.ausl.jnjclient.firmajnj.signer.exceptions.SignException;
import it.bologna.ausl.jnjclient.firmajnj.utils.CommonUtils;
import it.bologna.ausl.jnjclient.library.JnJClientCommand;
import it.bologna.ausl.jnjclient.library.JnJProgressBar;
import it.bologna.ausl.jnjclient.library.exceptions.JnJClientCommandException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gdm
 */
public class FirmaJnjCommand implements JnJClientCommand {
    private static final Logger LOGGER = Logger.getLogger(FirmaJnjCommand.class.getName());
    
    private final String COMMAND = "FIRMA_JNJ";
    
    @Override
    public String getCommand() {
        return COMMAND;
    }

    @Override
    public SignParamsComponent.EndSign.EndSignResults executeCommand(JnJProgressBar progressBar, String... params) throws JnJClientCommandException {
        if (progressBar != null) {
            progressBar.setTitle("Firma in corso...");
        }
        if (params == null || params.length == 0) {
            String errorMessage = "manca il parametri url per lo scaricamento dei SignParams";
            LOGGER.log(Level.SEVERE, errorMessage);
            throw new JnJClientCommandException(errorMessage);
        }
        
        // controllo che sia un URI valido
        try {
            URI uri = URI.create(params[0]);
        } catch (Exception e) {
            String errorMessage = String.format("url per lo scaricamento dei SignParams: \"%s\" non è un url valido", params[0]);
            LOGGER.log(Level.SEVERE, errorMessage);
            throw new JnJClientCommandException(errorMessage);
        }
        DocumentSigner documentSigner = new DocumentSigner();
        SignParams signParams;
        try {
            signParams = documentSigner.readSignParams(params[0]);
        } catch (SignException ex) {
            String errorMessage = "errore nello scaricamento dei SignParams";
            LOGGER.log(Level.SEVERE, errorMessage);
            throw new JnJClientCommandException(errorMessage);
        }
        CommonUtils.safelyIncrementProgessBar(progressBar, 5);
        SignParamsComponent.EndSign.EndSignResults result;
        try {
            result = documentSigner.sign(progressBar, signParams);
        } catch (SignException ex) {
            String errorMessage = String.format("errore nell'esecuzione del comando %s", COMMAND);
            LOGGER.log(Level.SEVERE, errorMessage);
            throw new JnJClientCommandException(errorMessage, ex);
        }
        return result;
    }

    
    
}
