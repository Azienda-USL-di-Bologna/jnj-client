package it.bologna.ausl.jnjclient.firmajnj.signer.data.files;

import it.bologna.ausl.jnjclient.firmajnj.utils.HttpUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author gdm
 */
public class UriSignFile extends SignFile {
    private static final Logger LOGGER = Logger.getLogger(UriSignFile.class.getName());
    
    private final URL fileUrl;

    public UriSignFile(URL fileUrl) {
        this.fileUrl = fileUrl;
    }
    
//    public UriSignFile(URL fileUrl) {
//        this.fileUrl = fileUrl;
//    }
            
    
    
    @Override
    protected File retrieveFile() {
        File toSignTempFile = null;
        try {
            OkHttpClient httpClient = HttpUtils.getHttpClient();
            Request request = new Request.Builder()
                    .url(fileUrl)
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        throw new IllegalStateException("Response doesn't contain a file");
                    }
                    try (InputStream byteStream = responseBody.byteStream()) {
                        toSignTempFile = File.createTempFile("to_sign_", ".tmp");
                        try (FileOutputStream fileOs = new FileOutputStream(toSignTempFile)) {
                            IOUtils.copy(byteStream, fileOs);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "error downloading to sign file", ex);
            if (toSignTempFile != null && toSignTempFile.exists()) {
                toSignTempFile.delete();
            }
        }
        return toSignTempFile;
    }

}
