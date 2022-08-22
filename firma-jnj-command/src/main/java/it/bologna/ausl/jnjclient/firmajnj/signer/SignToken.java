package it.bologna.ausl.jnjclient.firmajnj.signer;

import eu.europa.esig.dss.token.AbstractKeyStoreTokenConnection;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;

/**
 *
 * @author gdm
 */
public class SignToken {
    private DSSPrivateKeyEntry key;
    private AbstractKeyStoreTokenConnection token;

    public SignToken(DSSPrivateKeyEntry key, AbstractKeyStoreTokenConnection token) {
        this.key = key;
        this.token = token;
    }

    public DSSPrivateKeyEntry getKey() {
        return key;
    }

    public AbstractKeyStoreTokenConnection getToken() {
        return token;
    }
}
