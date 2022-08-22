package it.bologna.ausl.jnjclient.firmajnj;

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
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.SignatureFieldParameters;
import eu.europa.esig.dss.pades.SignatureImageParameters;
import eu.europa.esig.dss.pades.SignatureImageTextParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.pdf.openpdf.ITextDocumentReader;
import eu.europa.esig.dss.token.AbstractKeyStoreTokenConnection;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.Pkcs11SignatureToken;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParamsComponent;
import it.bologna.ausl.jnjclient.firmajnj.signer.Pkcs11Signer;
import it.bologna.ausl.jnjclient.firmajnj.signer.SignToken;
import it.bologna.ausl.jnjclient.firmajnj.signer.Signer;
import it.bologna.ausl.jnjclient.firmajnj.signer.SignerFactory;
import it.bologna.ausl.jnjclient.firmajnj.signer.exceptions.FirmaJnJException;
import it.bologna.ausl.jnjclient.firmajnj.signer.exceptions.UserAbortException;
import it.bologna.ausl.jnjclient.firmajnj.utils.OSUtils;
import it.bologna.ausl.jnjclient.firmajnj.utils.swing.SelectionForm;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.smartcardio.CardTerminal;

/**
 *
 * @author gdm
 * 
 * docs: https://ec.europa.eu/cefdigital/DSS/webapp-demo/doc/dss-documentation.html#_pades_visible_signature
 * 
 */
public class Test {
    public static void main(String[] args) throws FirmaJnJException, IOException {
//        main1(null);
//        System.exit(0);
        Signer signer = SignerFactory.getSigner(SignerFactory.PKCSStandards.PKCS_11, false);
        try {
            
            SignToken signToken = signer.getSignToken();

            SignParamsComponent.SignFileAttributes sfa = new SignParamsComponent.SignFileAttributes();
            SignParamsComponent.SignFileAttributesPosition signFileAttributesPosition = new SignParamsComponent.SignFileAttributesPosition();
            signFileAttributesPosition.setFieldOriginX(10);
            signFileAttributesPosition.setFieldOriginY(10);
            signFileAttributesPosition.setFieldWidth(200);
            signFileAttributesPosition.setFieldHeight(100);
            signFileAttributesPosition.setAlignmentHorizontal(SignParamsComponent.SignFileAttributesPosition.AlignmentHorizontalPositions.LEFT);
            signFileAttributesPosition.setAlignmentVertical(SignParamsComponent.SignFileAttributesPosition.AlignmentVerticalPositions.TOP);
            sfa.setPosition(signFileAttributesPosition);
            sfa.setVisible(true);
            sfa.setTextTemplate("firmato da [COMMONNAME] per azienda  [ORGANIZATIONNAME]....[GIVENNAME]...[SURNAME]...[SERIALNUMBER]..[COUNTRYNAME]..[DNQUALIFIER]");
            DSSDocument Psign = signer.padesSign(signToken, new FileDocument("test-pdf.pdf"), sfa);
            Psign.save("test-pdf-signed.pdf");
            DSSDocument Csign = signer.cadesSign(signToken, new FileDocument("test-pdf.pdf"));
            Csign.save("test-pdf-signed.pdf.p7m");
        } catch (UserAbortException ex) {
            System.out.println("Annullato dall'utente");
        } catch (Exception ex) {
            System.out.println("errore");
            ex.printStackTrace();
        }
    }

    
    public static void main1(String[] args) throws IOException {
        
        System.out.println(System.getenv("WINDIR"));
        System.out.println(System.getenv("SYSTEMROOT"));
//        System.exit(0);
        
        String winDir = System.getenv("WINDIR") + "/system32";
        Pkcs11SignatureToken pkcs11SignatureToken = new Pkcs11SignatureToken(winDir + "/" + "bit4xpki.dll", new KeyStore.PasswordProtection("mypin".toCharArray()), 1);
//        Pkcs11SignatureToken pkcs11SignatureToken = new Pkcs11SignatureToken(winDir + "/" + "bit4xpki.dll", new  KeyStore.PasswordProtection("mypin".toCharArray()));
        System.out.println(pkcs11SignatureToken);
        List<DSSPrivateKeyEntry> keys = pkcs11SignatureToken.getKeys().stream().filter(k -> isSigningKey(k)).collect(Collectors.toList());
        System.out.println(keys.size());
        keys.stream().forEach(e -> {
                System.out.println(e.getCertificate().getSubject().getPrettyPrintRFC2253());
        });
        
        DSSPrivateKeyEntry key = keys.get(0);
        
        DSSDocument signedDocumentPdf = pdfSign(pkcs11SignatureToken, key, "test-pdf.pdf");
        signedDocumentPdf.save("test-pdf-signed.pdf");
        
        
        
//        DSSDocument signedDocumentPdf = pdfSign(pkcs11SignatureToken, key, "test-pdf-signed.pdf");
//        signedDocumentPdf.save("test-pdf-signed-signed.pdf");
//        DSSDocument signedDocumentP7m = p7mSign(pkcs11SignatureToken, key, "test-pdf.pdf");
//        signedDocumentP7m.save("test-pdf-signed.pdf.p7m");
//        DSSDocument signedDocumentP7m = p7mSign(pkcs11SignatureToken, key, "test-pdf-signed.pdf.p7m");
//        signedDocumentP7m.save("test-pdf-signed.pdf2.p7m");
        
        
        
    }
    
    private static DSSDocument pdfSign(Pkcs11SignatureToken pkcs11SignatureToken, DSSPrivateKeyEntry key, String file) throws IOException {
        
        DSSDocument toSignDocument = new FileDocument(file);
        
        PAdESSignatureParameters parameters = new PAdESSignatureParameters();
        // We choose the level of the signature (-B, -T, -LT, -LTA).
        parameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_B);
        // We choose the type of the signature packaging (ENVELOPING, DETACHED).
        parameters.setSignaturePackaging(SignaturePackaging.ENVELOPED);
        // We set the digest algorithm to use with the signature algorithm. You must use the
        // same parameter when you invoke the method sign on the token. The default value is
        // SHA256
        
//        parameters.set
        
        parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);

        // We set the signing certificate
        parameters.setSigningCertificate(key.getCertificate());
        // We set the certificate chain
        parameters.setCertificateChain(key.getCertificateChain());
        
        //parameters.setSignWithExpiredCertificate(true);

        // Create common certificate verifier
        CommonCertificateVerifier commonCertificateVerifier = new CommonCertificateVerifier(true);
        // Create CAdESService for signature
        PAdESService service = new PAdESService(commonCertificateVerifier);

        SignatureImageParameters imageParameters = new SignatureImageParameters();
        // set an image
//        imageParameters.setImage(new InMemoryDocument(getClass().getResourceAsStream("/signature-pen.png")));

        // initialize signature field parameters
        SignatureFieldParameters fieldParameters = new SignatureFieldParameters();
        
        // the origin is the left and top corner of the page
        fieldParameters.setOriginX(0);
        fieldParameters.setOriginY(0);
        fieldParameters.setWidth(300);
        fieldParameters.setHeight(200);
        ITextDocumentReader reader = new ITextDocumentReader(toSignDocument);
        int lastPage = reader.getNumberOfPages();
        fieldParameters.setPage(1);
//        fieldParameters.setFieldId("campoFirma");
        
        imageParameters.setFieldParameters(fieldParameters);
        
        imageParameters.setAlignmentVertical(VisualSignatureAlignmentVertical.TOP);
        imageParameters.setAlignmentHorizontal(VisualSignatureAlignmentHorizontal.LEFT);
        
        String[] split = key.getCertificate().getSubject().getPrettyPrintRFC2253().split(",");
        String str = "firmato da [COMMONNAME] per azienda  [ORGANIZATIONNAME]....[GIVENNAME]...[SURNAME]...[SERIALNUMBER]..[COUNTRYNAME]..[DNQUALIFIER]";
        for (String couple  : split) {
            String[] attributes = couple.split("=");
            String attributeKey = attributes[0].toUpperCase();
            str = str.replace(String.format("[%s]", attributeKey), attributes[1]);
        }
        System.out.println(str);
        
        SignatureImageTextParameters textParameters = new SignatureImageTextParameters();
        textParameters.setText("My visual signature \n #2");
        textParameters.setPadding(0);
        textParameters.setTextWrapping(TextWrapping.FONT_BASED);
        imageParameters.setTextParameters(textParameters);
        
        imageParameters.setRotation(VisualSignatureRotation.AUTOMATIC);
        
        
        parameters.setImageParameters(imageParameters);

       
        //InMemoryDocument toSignDocument = new InMemoryDocument(documentRetriever.retrieveDocument());

        // Get the SignedInfo segment that need to be signed.
        ToBeSigned dataToSign = service.getDataToSign(toSignDocument, parameters);

        // This function obtains the signature value for signed information using the
        // private key and specified algorithm
        DigestAlgorithm digestAlgorithm = parameters.getDigestAlgorithm();
        SignatureValue signatureValue = pkcs11SignatureToken.sign(dataToSign, digestAlgorithm, key);


        // We invoke the CAdESService to sign the document with the signature value obtained in
        // the previous step.
        DSSDocument signedDocument = service.signDocument(toSignDocument, parameters, signatureValue);
        return signedDocument;
    }

    private static DSSDocument p7mSign(Pkcs11SignatureToken pkcs11SignatureToken, DSSPrivateKeyEntry key, String file) {
        
        CAdESSignatureParameters parameters = new CAdESSignatureParameters();
        // We choose the level of the signature (-B, -T, -LT, -LTA).
        parameters.setSignatureLevel(SignatureLevel.CAdES_BASELINE_B);
        parameters.setSignWithExpiredCertificate(true);
        // We choose the type of the signature packaging (ENVELOPING, DETACHED).
        parameters.setSignaturePackaging(SignaturePackaging.ENVELOPING);
        BLevelParameters bLevelParameters = new BLevelParameters();
//        bLevelParameters.set
//        parameters.setBLevelParams(bLevelParameters);
        // We set the digest algorithm to use with the signature algorithm. You must use the
        // same parameter when you invoke the method sign on the token. The default value is
        // SHA256
        
//        parameters.set
        
        parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);

        // We set the signing certificate
        parameters.setSigningCertificate(key.getCertificate());
        // We set the certificate chain
        parameters.setCertificateChain(key.getCertificateChain());
        
        //parameters.setSignWithExpiredCertificate(true);

        // Create common certificate verifier
        CommonCertificateVerifier commonCertificateVerifier = new CommonCertificateVerifier(false);
//        commonCertificateVerifier.getAIASource();
        // Create CAdESService for signature
        CAdESService service = new CAdESService(commonCertificateVerifier);

        // set an image
//        imageParameters.setImage(new InMemoryDocument(getClass().getResourceAsStream("/signature-pen.png")));


        DSSDocument toSignDocument = new FileDocument(file);
        //InMemoryDocument toSignDocument = new InMemoryDocument(documentRetriever.retrieveDocument());

        // Get the SignedInfo segment that need to be signed.
        ToBeSigned dataToSign = service.getDataToSign(toSignDocument, parameters);

        // This function obtains the signature value for signed information using the
        // private key and specified algorithm
        DigestAlgorithm digestAlgorithm = parameters.getDigestAlgorithm();
        SignatureValue signatureValue = pkcs11SignatureToken.sign(dataToSign, digestAlgorithm, key);


        // We invoke the CAdESService to sign the document with the signature value obtained in
        // the previous step.
        DSSDocument signedDocument = service.signDocument(toSignDocument, parameters, signatureValue);
        return signedDocument;
    }
    
    private static boolean isSigningKey(DSSPrivateKeyEntry key){
        /*
        Da DOC DSS: tramite keyUsage si ha array di byte il cui significato è il seguente
        KeyUsage ::= BIT STRING {
            digitalSignature        (0),
            nonRepudiation          (1),
            keyEncipherment         (2),
            dataEncipherment        (3),
            keyAgreement            (4),
            keyCertSign             (5),
            cRLSign                 (6),
            encipherOnly            (7),
            decipherOnly            (8) }
        Le chiavi di firma sono quelle che hanno nonRepudiation = true
        */
        return key.getCertificate().getCertificate().getKeyUsage()[1] == true;
    }
}
