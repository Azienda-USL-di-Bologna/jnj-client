package it.bologna.ausl.jnjclient.firmajnj.signer;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParamsComponent;
import it.bologna.ausl.jnjclient.firmajnj.signer.exceptions.FirmaJnJException;

/**
 * Interfaccia da implementare per ogni standard di firma (es. pkcs11, pkcs12, ecc)
 * @author gdm
 */
public interface Signer {

    /**
     * possibili tipologie di fime
     */
    public static enum SignTypes {
        CADES, // p7m
        PADES, // pdf
        XADES  // xml
    }

    /**
     * questa funzione deve tornare l'ogetto SignToken che contiene tutto quello che serve per poter accedere al keystore e alla chiave di firma
     * @return
     * @throws FirmaJnJException 
     */
    public SignToken getSignToken() throws FirmaJnJException;

    /**
     * deve implementare la firma pades (pdf firmati)
     * @param signToken oggetto tramite il quale accedere al keystore e la chiave di firma
     * @param file il file da firmare in formato DSSDocument
     * @param signFileAttributes gli attributi del file (es. se la firma è visibile e nel caso il testo che contiene, la posizione e la grandezza)
     * @return il file firmato in formato DSSDocument, tramite il quale si potrà salvare su disco o leggerne lo stream
     * @throws FirmaJnJException 
     */
    public DSSDocument padesSign(SignToken signToken, DSSDocument file, SignParamsComponent.SignFileAttributes signFileAttributes) throws FirmaJnJException;
    
    /**
     * deve implementare la firma cades (p7m)
     * @param signToken oggetto tramite il quale accedere al keystore e la chiave di firma
     * @param file il file da firmare in formato DSSDocument
     * @return il file firmato in formato DSSDocument, tramite il quale si potrà salvare su disco o leggerne lo stream
     * @throws FirmaJnJException 
     */
    public DSSDocument cadesSign(SignToken signToken, DSSDocument file) throws FirmaJnJException;
    
    /**
     * deve implementare la firma xades (xml firmati)
     * @param signToken oggetto tramite il quale accedere al keystore e la chiave di firma
     * @param file il file da firmare in formato DSSDocument
     * @return il file firmato in formato DSSDocument, tramite il quale si potrà salvare su disco o leggerne lo stream
     * @throws FirmaJnJException 
     */
    public DSSDocument xadesSign(SignToken signToken, DSSDocument file) throws FirmaJnJException;

    /**
     * torna true se la chiave passata è una chiave di firma, cioè se ha il KeyUsage = nonRepudiation
     * @param key la chiave da controllare
     * @return 
     */
    public default boolean isSigningKey(DSSPrivateKeyEntry key) {
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
