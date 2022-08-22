package it.bologna.ausl.jnjclient.firmajnj.signer.data.files;

import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParamsComponent;
import it.bologna.ausl.jnjclient.firmajnj.signer.exceptions.SignException;
import java.io.File;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory per la creazione del file da firmare in base alla sua sorgente
 * @author gdm
 */
public class SignFileFactory {
private static final Logger LOGGER = Logger.getLogger(SignFileFactory.class.getName());
    
    /**
     * torna un SignFile creandolo sulla base della sua sorgente (source) es. Base64/URI/FileSystem
     * @param signDocument il documento dal quale creare il SignFile
     * @return
     * @throws SignException 
     */
    public static SignFile getSignFile(SignParamsComponent.SignDocument signDocument) throws SignException {
        try {
            SignFile res;
            switch (signDocument.getSource()) {
                case URI:
                    res = new UriSignFile(new URL(signDocument.getFile()));
                    break;
                case BASE_64:
                    res = new Base64SignFile(signDocument.getFile());
                    break;
                case FILE_SYSTEM:
                    res = new FileSystemSignFile(new File(signDocument.getFile()));
                    break;
                default:
                    String message = String.format("source %s non valida", signDocument.getSource());
                    LOGGER.log(Level.SEVERE, message);
                    throw new SignException(message);
            }
            res.getFile();
            return res;
        } catch (Exception ex) {
            String message = "errore nella creazione del SignFile";
            LOGGER.log(Level.SEVERE, message, ex);
            throw new SignException(message, ex);
        }
    }
}
