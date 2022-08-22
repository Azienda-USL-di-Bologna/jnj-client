package it.bologna.ausl.jnjclient.firmajnj.signer.data.files;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import java.io.File;

/**
 *
 * @author gdm
 */
public abstract class SignFile {
    private File _file;
    
    public File getFile() {
        this._file = this.retrieveFile();
        this._file.deleteOnExit();
        return this._file;
    }
    
    protected abstract File retrieveFile();
    
    public void deleteFile() {
        if (_file.exists()) {
            _file.delete();
        }
    }
    
    public DSSDocument toDSSDocument() {
        return new FileDocument(_file);
    }
}
