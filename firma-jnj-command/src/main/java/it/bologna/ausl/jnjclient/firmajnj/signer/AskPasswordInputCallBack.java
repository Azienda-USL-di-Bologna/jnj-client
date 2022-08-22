package it.bologna.ausl.jnjclient.firmajnj.signer;

import eu.europa.esig.dss.token.PasswordInputCallback;
import it.bologna.ausl.jnjclient.firmajnj.utils.swing.MyPasswordPane;

/**
 *
 * @author gdm
 */
public class AskPasswordInputCallBack implements FirmaJnJPasswordInputCallBack {

    private char[] pin = null;
    private boolean aborted = false;
    private boolean testMode = false;
    private Object[] message = new Object[]{"Inserisci il PIN di firma della SmartCard"};

    public AskPasswordInputCallBack(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public void reset() {
       this.pin = null;
    }

    @Override
    public void setInputMessage(String message) {
        this.message[0] = message;
    }
    
    @Override
    public boolean isAborted() {
        return this.aborted;
    }
    
    @Override
    public char[] getPassword() {
        aborted = false;
        if (this.testMode) {
            return null;
        }
        int tries = 0;
        
        while(!aborted && (pin == null || pin.length == 0)) {
            tries ++;
            Object[] options = new Object[2];
            options[0] = "OK";
            options[1] = "Annulla";

            MyPasswordPane pinGetter = new MyPasswordPane("PIN", message, options);
            if (pinGetter.getSelection() != 0) {
                aborted = true;
                pin = null;
            } else {
                pin = pinGetter.getPassword();
                message[0] = "Non ha inserito il PIN! Inserisci il PIN di firma della SmartCard";
            }
            
            pinGetter.dispose();
        }
        return this.pin;
    }
}
