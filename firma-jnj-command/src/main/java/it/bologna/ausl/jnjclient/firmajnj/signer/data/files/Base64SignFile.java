package it.bologna.ausl.jnjclient.firmajnj.signer.data.files;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author gdm
 */
public class Base64SignFile extends SignFile {
    private static final Logger LOGGER = Logger.getLogger(Base64SignFile.class.getName());
    
    private final String base64File;

    public <T extends String> Base64SignFile(String base64File) {
        this.base64File = base64File;
    }
            
    @Override
    protected File retrieveFile() {
        File toSignTempFile = null;
        try {
            byte[] fileDecoded = Base64.getDecoder().decode(base64File);
            toSignTempFile = File.createTempFile("to_sign_", ".tmp");
            try (FileOutputStream fileOs = new FileOutputStream(toSignTempFile)) {
                IOUtils.write(fileDecoded, fileOs);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "error creating to sign file from base64", ex);
            if (toSignTempFile != null && toSignTempFile.exists()) {
                toSignTempFile.delete();
            }
        }
        return toSignTempFile;
    }
}
