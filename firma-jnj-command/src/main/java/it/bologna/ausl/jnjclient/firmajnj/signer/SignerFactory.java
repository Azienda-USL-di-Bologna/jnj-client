package it.bologna.ausl.jnjclient.firmajnj.signer;

/**
 *
 * @author gdm
 */
public class SignerFactory {
    public static enum PKCSStandards {
        PKCS_11
    }
    
    public static Signer getSigner(PKCSStandards pKCSStandard, Boolean testMode) {
        Signer signer = null;
        switch (pKCSStandard) {
            case PKCS_11:
                signer = new Pkcs11Signer(testMode);
                break;
        }
        return signer;
    }
}
