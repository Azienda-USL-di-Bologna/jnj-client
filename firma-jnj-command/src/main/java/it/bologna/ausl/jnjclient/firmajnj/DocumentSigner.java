package it.bologna.ausl.jnjclient.firmajnj;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.x509.CertificateToken;
import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParamsComponent;
import it.bologna.ausl.jnjclient.firmajnj.signer.SignToken;
import it.bologna.ausl.jnjclient.firmajnj.signer.Signer;
import it.bologna.ausl.jnjclient.firmajnj.signer.SignerFactory;
import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParams;
import it.bologna.ausl.jnjclient.firmajnj.signer.data.files.SignFile;
import it.bologna.ausl.jnjclient.firmajnj.signer.data.files.SignFileFactory;
import it.bologna.ausl.jnjclient.firmajnj.signer.exceptions.SignException;
import it.bologna.ausl.jnjclient.firmajnj.signer.exceptions.SystemAbortException;
import it.bologna.ausl.jnjclient.firmajnj.signer.exceptions.UserAbortException;
import it.bologna.ausl.jnjclient.firmajnj.tools.GlobalTools;
import it.bologna.ausl.jnjclient.firmajnj.utils.CommonUtils;
import it.bologna.ausl.jnjclient.firmajnj.utils.HttpUtils;
import it.bologna.ausl.jnjclient.library.JnJProgressBar;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.swing.JOptionPane;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.ByteString;

/**
 *
 * @author gdm
 */
public class DocumentSigner {
    private static final Logger LOGGER = Logger.getLogger(DocumentSigner.class.getName());
    private final ObjectMapper objectMapper = GlobalTools.getObjectMapper();
    
    public DocumentSigner() {
    }
    
    private boolean checkSerialNumber(SignParams signParams, CertificateToken certificate) throws SignException {
        if (signParams.getUserId() != null && !signParams.getUserId().isEmpty()) {
            String[] subjectFields = certificate.getSubject().getPrettyPrintRFC2253().split(",");
            Optional<String> cfSigner = Stream.of(subjectFields).filter(a -> a.toUpperCase().startsWith("SERIALNUMBER=")).findFirst();
            if (cfSigner.isEmpty()) {
                String errorMessage = "il cerificato di firma non ha il campo SERIALNUMBER";
                LOGGER.log(Level.SEVERE, errorMessage);
                throw new SignException(errorMessage);
            }
            String cf;
            String cfComplete = cfSigner.get().split("=")[1];
            if (cfComplete.contains(":")) {
                cf = cfComplete.split(":")[1];
            } else {
                cf = cfComplete;
            }
            return cf.equalsIgnoreCase(signParams.getUserId());
        } else {
            return true;
        }
    }
    
    public SignParamsComponent.EndSign.EndSignResults sign(JnJProgressBar progressBar, SignParams signParams) throws SignException {
        SignParamsComponent.EndSign endSign = signParams.getEndSign();
        SignParamsComponent.EndSign.EndSignResults endSignResult;
        int pbAmount = 0;
        try {
            List<SignParamsComponent.SignDocument> signDocumentList = signParams.getSignFileList();
            String signedFileUploaderUrl = signParams.getSignedFileUploaderUrl();
            
            if (signDocumentList != null && !signDocumentList.isEmpty()) {
                Signer signer = SignerFactory.getSigner(SignerFactory.PKCSStandards.PKCS_11, signParams.getTestMode());
                CommonUtils.safelyIncrementProgessBar(progressBar, 5);
                SignToken signToken = signer.getSignToken();
                CommonUtils.safelyIncrementProgessBar(progressBar, 5);
                
                if (!signParams.getTestMode()) {
                    if (!checkSerialNumber(signParams, signToken.getKey().getCertificate())) {
                        String message = "il codice fiscale sul certificato di firma non corrisponde all'attuale firmatario";
                        JOptionPane.showMessageDialog(null, message, "Errore Certificato", JOptionPane.ERROR_MESSAGE);
                        throw new SystemAbortException(message);
                    }
                }
                pbAmount = CommonUtils.getProgressBarAmountForEachStep(progressBar, signDocumentList.size() + 2);
                for (SignParamsComponent.SignDocument signDocument : signDocumentList) {
                    CommonUtils.safelyIncrementProgessBar(progressBar, pbAmount / 2);
                    SignFile signFile = SignFileFactory.getSignFile(signDocument);
                    DSSDocument toSignDSSDocument = signFile.toDSSDocument();
                    DSSDocument signedDSSDocument;
                    String signedExt;
                    switch (signDocument.getSignType()) {
                        case CADES:
                            try {
                                signedDSSDocument = signer.cadesSign(signToken, toSignDSSDocument);
                            } catch (Exception ex) {
                                signDocument.setSignDocumentResult(SignParamsComponent.SignDocument.SignDocumentResults.ERROR);
                                throw ex;
                            }
                            signedExt = "p7m";
                            break;
                        case PADES:
                            try {
                                signedDSSDocument = signer.padesSign(signToken, toSignDSSDocument, signDocument.getSignAttributes());
                            } catch (Exception ex) {
                                signDocument.setSignDocumentResult(SignParamsComponent.SignDocument.SignDocumentResults.ERROR);
                                throw ex;
                            }
                            signedExt = "pdf";
                            break;
                        default:
                            signDocument.setSignDocumentResult(SignParamsComponent.SignDocument.SignDocumentResults.ERROR);
                            throw new SignException(String.format("signType %s non valido o non supportato", signDocument.getSignType().toString()));
                    }
                    CommonUtils.safelyIncrementProgessBar(progressBar, pbAmount / 2);
                    String uploadSignedFileRes;
                    try {
                        uploadSignedFileRes = uploadSignedFile(signedFileUploaderUrl, signDocument.getId(), signDocument.getName(), signedExt, objectMapper.writeValueAsString(signDocument), signedDSSDocument);
                    } catch (Exception ex) {
                        signDocument.setSignDocumentResult(SignParamsComponent.SignDocument.SignDocumentResults.ERROR);
                        throw new SignException("errore nell'upload del file firmato", ex);
                    }
                    CommonUtils.safelyIncrementProgessBar(progressBar, pbAmount);
                    signDocument.setUploaderResult(uploadSignedFileRes);
                    signDocument.setSignDocumentResult(SignParamsComponent.SignDocument.SignDocumentResults.SIGNED);
                }
                endSignResult = SignParamsComponent.EndSign.EndSignResults.ALL_SIGNED;
                endSign.setSignedFileList(signDocumentList);
                LOGGER.log(Level.INFO, "tutto ok");
            } else {
                throw new SignException("nessun file da firmare passato");
            }
        } catch (UserAbortException ex) {
            LOGGER.log(Level.WARNING, "Annullato dall'utente", ex);
            endSignResult = SignParamsComponent.EndSign.EndSignResults.ABORT;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "errore nella firma", ex);
            endSignResult = SignParamsComponent.EndSign.EndSignResults.ERROR;
        }
        
        endSign.setEndSignResult(endSignResult);
        
        try {
            endSignCall(endSign.getCallBackUrl(), objectMapper.writeValueAsString(endSign));
            CommonUtils.safelyIncrementProgessBar(progressBar, pbAmount);
        } catch (Exception ex) {
            String errorMessage = String.format("errore nella chiamata alla servlet di endsign: %s", endSign.getCallBackUrl());
            LOGGER.log(Level.SEVERE, errorMessage, ex);
            throw new SignException(errorMessage, ex);
        }
        return endSignResult;
    }
    
    /**
     * fa una richiesta post multipart all'url passato per inviare il file firmato e il json che lo descrive
     * @param url url al quale mandare il file
     * @param fileId id che indentifica il file (verrà rimpiazzato con il segnaposto [fileId] nei query-params) se non c'è il segnaposto nell'url, questo parametro è inutile
     * @param signedFileName il nome del file da mettere nel nome della part
     * @param SignedFileExt estensione del file da mettere nel nome della part
     * @param signDocumentJson la stringa json del SignDocument che si è firmato
     * @param signedDSSDocument il file firmato
     * @return il risultato della chiamata in stringa
     * @throws IOException 
     */
    private String uploadSignedFile(String url, String fileId ,String signedFileName, String SignedFileExt, String signDocumentJson, DSSDocument signedDSSDocument) throws SignException {
        File signedTempFile = null;
        try {
            OkHttpClient httpClient = HttpUtils.getHttpClient();
            signedTempFile = File.createTempFile("signed_", ".tmp");
            signedDSSDocument.save(signedTempFile.getAbsolutePath());
            RequestBody dataBody = RequestBody.create(signedTempFile, okhttp3.MultipartBody.FORM);
            MultipartBody multipartBody = new MultipartBody.Builder()
                    .addPart(MultipartBody.Part.createFormData("file", String.format("%s.%s", signedFileName, SignedFileExt), dataBody))
                    .addPart(MultipartBody.Part.createFormData("params", signDocumentJson))
                    .build();
            Request request = new Request.Builder()
                    .url(url.replace("[fileId]", fileId))
                    .post(multipartBody)
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        String errorMessage = String.format("la servlet per il caricamento del file firmato all'url: %s ha risposto http %s con body null", url, response.code());
                        LOGGER.log(Level.SEVERE, errorMessage);
                        throw new IllegalStateException(errorMessage);
                    } else if (!response.isSuccessful()) {
                        String errorMessage = String.format("la servlet per il caricamento del file firmato all'url: %s ha risposto http %s con body %s", url, response.code(), responseBody.string());
                        LOGGER.log(Level.SEVERE, errorMessage);
                        throw new IllegalStateException(errorMessage);
                    } else {
                        ByteString byteStream = responseBody.byteString();
                        String res = byteStream.string(Charset.forName("UTF-8"));
                        return res;
                    }
                }
            }
        } catch (Exception ex) {
            String errorMessage = String.format("errore nella chiamata alla servlet per il caricamento del file firmato all'url: %s", url);
            LOGGER.log(Level.SEVERE, errorMessage, ex);
            throw new SignException(errorMessage, ex);
        } finally {
            if (signedTempFile != null && signedTempFile.exists()) {
                signedTempFile.delete();
            }
        }
    }
    
    /**
     * Chiama la servlet di fine firma all'url passato in post, mandando la stringa passata come json nel body
     * @param url l'url della servlet di fine firma da chiamare
     * @param endSignJson il json da mandare. Sarebbe l'oggetto EndSign, dopo che gli è stato settato fileList e 
     * @return il risultato della chiamata alla servlet
     * @throws SignException 
     */
    private String endSignCall(String url, String endSignJson) throws SignException {
        try {
            OkHttpClient httpClient = HttpUtils.getHttpClient();
            RequestBody body = RequestBody.create(endSignJson, MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        String errorMessage = String.format("la servlet di endSign all'url: %s ha risposto http %s con body null", url, response.code());
                        LOGGER.log(Level.SEVERE, errorMessage);
                        throw new IllegalStateException(errorMessage);
                    } else if (!response.isSuccessful()) {
                        String errorMessage = String.format("la di endSign all'url: %s ha risposto http %s con body %s", url, response.code(), responseBody.string());
                        LOGGER.log(Level.SEVERE, errorMessage);
                        throw new IllegalStateException(errorMessage);
                    } else {
                        ByteString byteStream = responseBody.byteString();
                        String res = byteStream.string(Charset.forName("UTF-8"));
                        return res;
                    }
                }
            }
        } catch (Exception ex) {
            String errorMessage = String.format("errore nella chiamata alla servlet di endSign all'url: %s", url);
            LOGGER.log(Level.SEVERE, errorMessage, ex);
            throw new SignException(errorMessage, ex);
        }
    }
    
    public SignParams readSignParams(String getRequestParamterUrl) throws SignException {
        try {
            OkHttpClient httpClient = HttpUtils.getHttpClient();
            Request request = new Request.Builder()
                    .url(getRequestParamterUrl).get().build();
            try (Response response = httpClient.newCall(request).execute()) {
                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        throw new IllegalStateException("Response is empty");
                    }
                    String signParamsString = responseBody.string();
                    if (response.isSuccessful()) {
                        SignParams signParams = objectMapper.readValue(signParamsString, SignParams.class);
                        return signParams;
                    } else {
                        throw new IllegalStateException(signParamsString);
                    }
                }
            }
        } catch (Exception ex) {
            String errorMessage = String.format("errore nella chiamata per lo scaricamento dei SignParams all'url: %s", getRequestParamterUrl);
            LOGGER.log(Level.SEVERE, errorMessage, ex);
            throw new SignException(errorMessage, ex);
        }
    }
}
