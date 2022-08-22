package it.bologna.ausl.jnjclient.firmajnj.signer;

import eu.europa.esig.dss.token.PasswordInputCallback;

/**
 *
 * @author gdm
 */
public interface FirmaJnJPasswordInputCallBack extends PasswordInputCallback {
    
    public boolean isAborted();
    
    public void reset();
    
    public void setInputMessage(String message);
}
