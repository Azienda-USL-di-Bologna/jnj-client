package it.bologna.ausl.jnjclient.firmajnj.signer;

import eu.europa.esig.dss.AbstractSignatureParameters;
import eu.europa.esig.dss.cades.CAdESSignatureParameters;
import eu.europa.esig.dss.cades.signature.CAdESService;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.enumerations.TextWrapping;
import eu.europa.esig.dss.enumerations.VisualSignatureAlignmentHorizontal;
import eu.europa.esig.dss.enumerations.VisualSignatureAlignmentVertical;
import eu.europa.esig.dss.enumerations.VisualSignatureRotation;
import eu.europa.esig.dss.model.BLevelParameters;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.SignatureFieldParameters;
import eu.europa.esig.dss.pades.SignatureImageParameters;
import eu.europa.esig.dss.pades.SignatureImageTextParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.signature.AbstractSignatureService;
import eu.europa.esig.dss.token.AbstractKeyStoreTokenConnection;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.Pkcs11SignatureToken;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParamsComponent;
import it.bologna.ausl.jnjclient.firmajnj.signer.exceptions.ConfigurationException;
import it.bologna.ausl.jnjclient.firmajnj.signer.exceptions.FirmaJnJException;
import it.bologna.ausl.jnjclient.firmajnj.signer.exceptions.PKCS11Exception;
import it.bologna.ausl.jnjclient.firmajnj.signer.exceptions.SignException;
import it.bologna.ausl.jnjclient.firmajnj.signer.exceptions.SystemAbortException;
import it.bologna.ausl.jnjclient.firmajnj.signer.exceptions.UserAbortException;
import it.bologna.ausl.jnjclient.firmajnj.utils.OSUtils;
import it.bologna.ausl.jnjclient.firmajnj.utils.swing.SelectionForm;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;

/**
 *
 * @author gdm
 */
public class Pkcs11Signer implements Signer {
    private static final Logger LOGGER = Logger.getLogger(Pkcs11Signer.class.getName());
    
    private Boolean testMode;
    
    public Pkcs11Signer(Boolean testMode) {
        this.testMode = testMode;
        passwordInputCallback = new AskPasswordInputCallBack(this.testMode);
    }
    
    private FirmaJnJPasswordInputCallBack passwordInputCallback;
    private List<String> libsList;
    
    public Pkcs11Signer(FirmaJnJPasswordInputCallBack passwordInputCallback, String[] pkcs11LibsList) {
        this.passwordInputCallback = passwordInputCallback;
    }

    public void setPasswordInputCallback(FirmaJnJPasswordInputCallBack passwordInputCallback) {
        this.passwordInputCallback = passwordInputCallback;
    }

    public void setLibsList(List<String> libsList) {
        this.libsList = libsList;
    }
    
    /**
     * Rileva la chiave di firma (il token) e ne torna un oggetto che ti permette di accederci
     * Se vengono rilevati più lettori, con smartcard presente viene chiesta la scelta del lettore
     * Se una volta selezionato il lettore, nella smartcard sono presenti più chiavi di firma, allora viene chiesta la scelta
     * @return un oggetto SignToken, che contiene la chiave di firma e il token tramite il quale è possibile accedere alla chiave di firma tramite la smartcard
     * @throws FirmaJnJException 
     */
    @Override
    public SignToken getSignToken() throws FirmaJnJException {
        if (this.libsList == null) {
            this.libsList = OSUtils.readLibsListFromLibrariesFile();
        }
        Pkcs11SignatureToken pkcs11SignatureToken = null;
        String dllDirectory = OSUtils.getDllDirectory();
        
        if (this.testMode) {
            return null;
        }
        
        boolean logged = false;
        SignToken signToken = null;
        while (!logged) { // ciclo fino a che non viene eseguito il login sulla smartcard (o fino a che  non viene lanciata un'eccezione)
            boolean retry = false;
            int selectedReader = 0;
            
            // rileva tutti i lettori di smartcard con card inserita
            Map<Integer, String> cardReaders = OSUtils.getCardTerminals();
            if (cardReaders.isEmpty()) { // se non si sono lettori collegati torno eccezione
                String message = "nessun lettore di smatcard collegato, collega un lettore e riprova";
                LOGGER.log(Level.SEVERE, message);
                throw new SystemAbortException(message);
            } else if (cardReaders.size() > 1) { // se c'è più di un lettore di smartcard con card inserita, chiedo all'utetne di scegliere
                SelectionForm form = new SelectionForm(cardReaders);
                selectedReader = form.getSelectionIndex();
                if (selectedReader == -1) {
                    throw new UserAbortException("annullamento dopo selezione reader");
                } 
            } else { // se è presente un solo lettore di smartcard con card inserita, prendo direttametne quella senza chiedere
                selectedReader = cardReaders.keySet().toArray(new Integer[0])[0];
            }
            // una volta selezionato il lettore provo ad accedere alla smartcard con tutte le librerie indicate nel file di configurazione
            int i=0;
            boolean tryAll = false;
            while (!logged && !retry && (i <= libsList.size())) {
                try {
                    // se ho provato tutte le librerie e non ho trovato nessuna chiave è possibile che la smartcart non sia inserita, chiedo di inserirla e riaprto da capo
                    if (i == libsList.size()) {
                        // lancio una DSSException con messaggio Reason: PKCS11 not found in modo da andare nel caso gestito nel catch sotto e mostrare il messaggio smartcard non trovata
                        throw new DSSException("Reason: PKCS11 not found");
                    }
                    String lib = libsList.get(i);
                    String libPath = dllDirectory + "/" + lib;
                    LOGGER.log(Level.INFO, String.format("provo con %s", libPath));
                    pkcs11SignatureToken = new Pkcs11SignatureToken(libPath, this.passwordInputCallback, selectedReader);

                    // lo faccio per costringere la libreria a chiedere il pin
                    List<DSSPrivateKeyEntry> keys = pkcs11SignatureToken.getKeys();
                    Map<Integer, String> keysMap = keys.stream().filter(k -> isSigningKey(k)).collect(
                            Collectors.toMap((k -> k.hashCode()), (v -> ((eu.europa.esig.dss.token.KSPrivateKeyEntry) v).getAlias())));

                    int selectionIndex;
                    // se non trovo chiavi di firma lancio una DSSException con messaggio Reason: PKCS11 not found, in modo da andare nel caso gestito nel catch sotto
                    // e mostrare il messaggio smartcard non trovata
                    if (keysMap == null || keysMap.isEmpty()) {
                        throw new DSSException("Reason: PKCS11 not found");
                    } else if (keysMap.size() > 1) { // se trovo più di una chiave di firma ne chiedo la selezione all'utente
                        SelectionForm form = new SelectionForm(keysMap);
                        selectionIndex = form.getSelectionIndex();
                        if (selectionIndex == -1) {
                            throw new UserAbortException("annullamento dopo richiesta selezione chiave");
                        }
                    } else { // se è solo una, la seleziono direttamente
                        selectionIndex = keysMap.entrySet().stream().findFirst().get().getKey();
                    }
                    DSSPrivateKeyEntry selectedKey = keys.stream().filter(k -> k.hashCode() == selectionIndex).findFirst().get();
                    signToken = new SignToken(selectedKey, pkcs11SignatureToken);
                    logged = true;
                    i++;
                } catch (DSSException | FirmaJnJException ex) {
                    LOGGER.log(Level.WARNING, "errore nel login al token, vedo cos'è...", ex);
                    if (UserAbortException.class.isAssignableFrom(ex.getClass())) {
                        throw ex;
                    }
                    if (this.passwordInputCallback.isAborted()) {
                        LOGGER.log(Level.INFO, "annullamento inserimento pin");
                        throw new UserAbortException("annullamento inserimento pin");
                    
                    // se l'eccezione è di tipo DSSException allora l'errorMessage conterrà la stringa Reason: motivazione dell'errore
                    } else if (DSSException.class.isAssignableFrom(ex.getClass()) && ex.getMessage().contains("Reason:")) {
                        String message = ex.getMessage();
                        String reason = message.substring(message.indexOf("Reason")).split(":")[1].trim();
                        if (reason.startsWith("PKCS11 not found")) { // questa reason indica che probabilmente non è stata inserita la smartcard
                            // chiedo di inserirla e riparto da capo
                            LOGGER.log(Level.WARNING, "smartcard non inserita");
                            String[] optionsButtons = {"Riprova","Annulla"};
                            int choice = JOptionPane.showOptionDialog(null, "SmartCard non trovata. Inserisci la SmartCard!", "Errore SmartCard", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, optionsButtons, optionsButtons[0]);
                            if (choice != 0) {
                                throw new UserAbortException("annullamento dopo smartcard non trovata");
                            } else {
                                retry = true;
                            }
                        } else if (DSSException.class.isAssignableFrom(ex.getClass()) && reason.startsWith("load failed")) { // in questo caso è stato sbagliato il pin
                            // chiedo di reinserirlo
                            LOGGER.log(Level.WARNING, "pin errato");
                            this.passwordInputCallback.setInputMessage("il pin è errato, riprova. Attenzione se sbagli troppe volte la firma potrebbe essere bloccata");
                            this.passwordInputCallback.reset();
                        } else if (DSSException.class.isAssignableFrom(ex.getClass()) && reason.startsWith("Unable to instantiate PKCS11")) { // in questo caso sto usando la libreria pkcs11 sbagliata
                            // riparto da capo, provando con la prossima indicata nel file di configurazione
                            LOGGER.log(Level.WARNING, "libreria non valida, provo la prossima");
                            i++;
                        } else { // anche nel caso di un errore non identificato, provo con la prossima libreria
                            LOGGER.log(Level.WARNING, "errore non identificato, provo con la prossima libreria");
                            i++;
                        }
                    }
                } catch (Exception ex) { // in questo caso non so che errore ho avuto, per cui torno eccezione ed esco
                    String message = "errore non identificato nell'instanziare la libreria pkcs11";
                    LOGGER.log(Level.SEVERE, message, ex);
                    throw new ConfigurationException(message, ex);
                }
            }
        }
        if (pkcs11SignatureToken == null) {
            String message = "non ho trovato nessuna libreria pkcs11 compatibile con la smartcard inserita";
            LOGGER.log(Level.SEVERE, message);
            throw new SystemAbortException(message);
        }
        
        return signToken;
    }
    
    /**
     * estrae e torna il AbstractKeyStoreTokenConnection dall'oggetto SignToken assicurandosi che sia di tipo Pkcs11SignatureToken
     * @param signToken
     * @return il AbstractKeyStoreTokenConnection castato a Pkcs11SignatureToken
     * @throws PKCS11Exception 
     */
    private Pkcs11SignatureToken getPkcs11SignatureToken(SignToken signToken) throws PKCS11Exception {
        AbstractKeyStoreTokenConnection pkcs11SignatureToken = signToken.getToken();
        if (!pkcs11SignatureToken.getClass().isAssignableFrom(Pkcs11SignatureToken.class)) {
            throw new PKCS11Exception(pkcs11SignatureToken + " deve essere di tipo " + Pkcs11SignatureToken.class.getName() + " trovato invece " + pkcs11SignatureToken.getClass().getName());
        }
        return (Pkcs11SignatureToken) pkcs11SignatureToken;
    }
    
    /**
     * implementa la firma pades (pdf firmati)
     * @param signToken oggetto tramite il quale accedere alla smartcard e alla chiave di firma
     * @param file il file da firmare in formato DSSDocument
     * @param signFileAttributes gli attributi del file (es. se la firma è visibile e nel caso il testo che contiene, la posizione e la grandezza)
     * @return il file firmato in formato DSSDocument, tramite il quale si potrà salvare su disco o leggerne lo stream
     * @throws FirmaJnJException 
     */
    @Override
    public DSSDocument padesSign(SignToken signToken, DSSDocument file, SignParamsComponent.SignFileAttributes signFileAttributes) throws FirmaJnJException {
        Pkcs11SignatureToken pkcs11SignatureToken = null;
        DSSPrivateKeyEntry key = null;
        if (!this.testMode) {
            pkcs11SignatureToken = getPkcs11SignatureToken(signToken);
            key = signToken.getKey();
        }
        return this.pkcs11Sign(pkcs11SignatureToken, file, key, SignTypes.PADES, signFileAttributes);
    }
    
    /**
     * implementa la firma cades (p7m)
     * @param signToken oggetto tramite il quale accedere alla smartcard e alla chiave di firma
     * @param file il file da firmare in formato DSSDocument
     * @return il file firmato in formato DSSDocument, tramite il quale si potrà salvare su disco o leggerne lo stream
     * @throws FirmaJnJException 
     */
    @Override
    public DSSDocument cadesSign(SignToken signToken, DSSDocument file) throws FirmaJnJException {
        Pkcs11SignatureToken pkcs11SignatureToken = null;
        DSSPrivateKeyEntry key = null;
        if (!this.testMode) {
            pkcs11SignatureToken = getPkcs11SignatureToken(signToken);
            key = signToken.getKey();
        }
        return this.pkcs11Sign(pkcs11SignatureToken, file, key, SignTypes.CADES, null);
    }
    
    /**
     * implementa la firma xades (xml firmati)
     * @param signToken oggetto tramite il quale accedere alla smartcard e alla chiave di firma
     * @param file il file da firmare in formato DSSDocument
     * @return il file firmato in formato DSSDocument, tramite il quale si potrà salvare su disco o leggerne lo stream
     * @throws FirmaJnJException 
     */
    @Override
    public DSSDocument xadesSign(SignToken signToken, DSSDocument file) throws FirmaJnJException {
        Pkcs11SignatureToken pkcs11SignatureToken = null;
        DSSPrivateKeyEntry key = null;
        if (!this.testMode) {
            pkcs11SignatureToken = getPkcs11SignatureToken(signToken);
            key = signToken.getKey();
        }
        return this.pkcs11Sign(pkcs11SignatureToken, file, key, SignTypes.XADES, null);
    }
    
    /**
     * esegue la firma di un file, viene chiamato dalle funzioni sopra con signType adeguato
     * @param pkcs11SignatureToken il token che contiene la chiave
     * @param file il file da firmare in formato DSSDocument
     * @param key la chiave di firma
     * @param signType il tipo di firma da mettere sul file (cades, pades, xades)
     * @param signFileAttributes gli attributi del file (es. se la firma è visibile e nel caso il testo che contiene, la posizione e la grandezza). Vale solo per i pdf
     * @return il file firmato in formato DSSDocument, tramite il quale si potrà salvare su disco o leggerne lo stream
     * @throws FirmaJnJException 
     */
    protected DSSDocument pkcs11Sign(Pkcs11SignatureToken pkcs11SignatureToken, DSSDocument file, DSSPrivateKeyEntry key, SignTypes signType, SignParamsComponent.SignFileAttributes signFileAttributes) throws FirmaJnJException {

        AbstractSignatureParameters parameters;
         // Create common certificate verifier
        CommonCertificateVerifier commonCertificateVerifier = new CommonCertificateVerifier(false);
        AbstractSignatureService service;
        switch (signType) {
            case CADES:
                parameters = new CAdESSignatureParameters();
                // We choose the level of the signature (-B, -T, -LT, -LTA).
                parameters.setSignatureLevel(SignatureLevel.CAdES_BASELINE_B);
                parameters.setSignWithExpiredCertificate(true);
                // We choose the type of the signature packaging (ENVELOPING, DETACHED).
                parameters.setSignaturePackaging(SignaturePackaging.ENVELOPING);
//                BLevelParameters bLevelParameters = new BLevelParameters();
                // Create CAdESService for signature
                service = new CAdESService(commonCertificateVerifier);
                break;
            case PADES:
                parameters = new PAdESSignatureParameters();
                // We choose the level of the signature (-B, -T, -LT, -LTA).
                parameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_B);
                // We choose the type of the signature packaging (ENVELOPING, DETACHED).
                parameters.setSignaturePackaging(SignaturePackaging.ENVELOPED);
                // Create CAdESService for signature
                if (signFileAttributes != null && signFileAttributes.getVisible()) {
                    ((PAdESSignatureParameters)parameters).setImageParameters(buildSignatureImageParameters(signFileAttributes.getPosition(), signFileAttributes.getTextTemplate(), key.getCertificate()));
                }
                service = new PAdESService(commonCertificateVerifier);
                break;
            case XADES:
                throw new SignException(String.format("SignType %s not implemented yet", signType.toString()));
            default:
                throw new SignException(String.format("SignType %s not supported", signType.toString()));
        }

//        bLevelParameters.set
//        parameters.setBLevelParams(bLevelParameters);
        // We set the digest algorithm to use with the signature algorithm. You must use the
        // same parameter when you invoke the method sign on the token. The default value is
        // SHA256
        parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);

        DSSDocument signedDocument;
        if (!this.testMode) {
            // We set the signing certificate
            parameters.setSigningCertificate(key.getCertificate());
            // We set the certificate chain
            parameters.setCertificateChain(key.getCertificateChain());
            //parameters.setSignWithExpiredCertificate(true);

            // Get the SignedInfo segment that need to be signed.
            ToBeSigned dataToSign = service.getDataToSign(file, parameters);

            // This function obtains the signature value for signed information using the
            // private key and specified algorithm
            DigestAlgorithm digestAlgorithm = parameters.getDigestAlgorithm();
            SignatureValue signatureValue = pkcs11SignatureToken.sign(dataToSign, digestAlgorithm, key);
            signedDocument = service.signDocument(file, parameters, signatureValue);
        } else {
            signedDocument = file;
        }

        // We invoke the CAdESService to sign the document with the signature value obtained in
        // the previous step.
        return signedDocument;
    }
    
    /**
     * a partire dagli attributi di firma crea l'oggetto SignatureImageParameters necessario alla libreria DSS per rendere la firma visibile e con gli attributi visivi richiesti
     * @param signFileAttributesPosition posizione e grandezza del campo firma
     * @param textTemplate template deltesto al quale al posto dei segnaposti saranno sostituiti i valori presi dal certificato
     * @param certificate il certificato del firmatario, necessario per popolare il textTemplate
     * @return 
     */
    private SignatureImageParameters buildSignatureImageParameters(SignParamsComponent.SignFileAttributesPosition signFileAttributesPosition, String textTemplate, CertificateToken certificate) {
        SignatureImageParameters imageParameters = new SignatureImageParameters();
        // set an image
//        imageParameters.setImage(new InMemoryDocument(getClass().getResourceAsStream("/signature-pen.png")));

        // initialize signature field parameters
        SignatureFieldParameters fieldParameters = new SignatureFieldParameters();
        // the origin is the left and top corner of the page
        fieldParameters.setOriginX(signFileAttributesPosition.getFieldOriginX());
        fieldParameters.setOriginY(signFileAttributesPosition.getFieldOriginY());
        fieldParameters.setWidth(signFileAttributesPosition.getFieldWidth());
        fieldParameters.setHeight(signFileAttributesPosition.getFieldHeight());
        
        imageParameters.setFieldParameters(fieldParameters);
        
        imageParameters.setAlignmentVertical(VisualSignatureAlignmentVertical.valueOf(signFileAttributesPosition.getAlignmentVertical().toString()));
        imageParameters.setAlignmentHorizontal(VisualSignatureAlignmentHorizontal.valueOf(signFileAttributesPosition.getAlignmentHorizontal().toString()));
        
        SignatureImageTextParameters textParameters = new SignatureImageTextParameters();
        
        String[] subjectFields = certificate.getSubject().getPrettyPrintRFC2253().split(",");
        String text = textTemplate;
//        String text = "firmato da [COMMONNAME] per azienda  [ORGANIZATIONNAME]....[GIVENNAME]...[SURNAME]...[SERIALNUMBER]..[COUNTRYNAME]..[DNQUALIFIER]";
        for (String couple  : subjectFields) {
            String[] attributes = couple.split("=");
            String attributeKey = attributes[0].toUpperCase();
            text = text.replace(String.format("[%s]", attributeKey), attributes[1]);
        }
        
        textParameters.setText(text);
        textParameters.setPadding(20);
        textParameters.setTextWrapping(TextWrapping.FONT_BASED);
        imageParameters.setTextParameters(textParameters);
        
        imageParameters.setRotation(VisualSignatureRotation.AUTOMATIC);
        return imageParameters;
    }
}
