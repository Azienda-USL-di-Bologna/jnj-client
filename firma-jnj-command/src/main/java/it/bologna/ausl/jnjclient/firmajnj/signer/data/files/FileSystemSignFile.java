package it.bologna.ausl.jnjclient.firmajnj.signer.data.files;

import java.io.File;

/**
 *
 * @author gdm
 */
public class FileSystemSignFile extends SignFile{    

    private final File file;
    
    public <T extends File> FileSystemSignFile(File file) {
        this.file = file;
    }
            
    @Override
    protected File retrieveFile() {
        return this.file;
    }
}
