package it.bologna.ausl.jnjclient.firmajnj.utils;

import it.bologna.ausl.jnjclient.firmajnj.signer.exceptions.ConfigurationException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author gdm
 */
public class OSUtils {
    private static Logger LOGGER = Logger.getLogger(OSUtils.class.getName());
    
    private static final TerminalFactory terminalFactory = TerminalFactory.getDefault();
    
    public static String getOsName() {
        String osName = System.getProperty("os.name"); 
        return osName;
    }
    
    public static String getDllDirectory() {
        String res = "";
        String osName = getOsName().toUpperCase();
        if (osName.contains("WINDOWS")) {
            res = System.getenv("SYSTEMROOT");
            if (StringUtils.isEmpty(res)) {
                res = System.getenv("WINDIR");
            } if (StringUtils.isEmpty(res)) {
                LOGGER.log(Level.WARNING, "impossibile trovare la cartella di sistema, uso la cartella di programma...");
                res = "";
            } else {
                res += "/SYSTEM32";
            }
        } else if (osName.contains("LINUX")) {
            res = "";
        } else if (osName.contains("MAC")) {
            res = "";
        }
        return res;
    }
    
    public static List<String> readLibsListFromLibrariesFile() throws ConfigurationException {

        // leggo il file che mappa il sistema operativo con la libreria wrapper e l'elenco dei middleware per l'accesso alla smartcard
        Properties libsMapper = new Properties();
        try {
            FileInputStream libsMapperInputStream = null;
            InputStream resource = OSUtils.class.getResourceAsStream("/pkcs11-windows-libraries.properties");
            System.out.println(resource);
            try {
                libsMapper.load(resource);
            } catch (Exception ex) {
                String errorMessage = "non ho trovato la risorsa: \"pkcs11-windows-libraries.properties\" come stream nel jar, provo a leggerlo dalla home del programma...";
                LOGGER.log(Level.WARNING, errorMessage, ex);
                libsMapperInputStream = new FileInputStream("pkcs11-windows-libraries.properties");
            }
        }
        catch (FileNotFoundException ex) {
            String errorMessage = "Impossibile trovare il file di configurazione: \"pkcs11-windows-libraries.properties\"";
            LOGGER.log(Level.SEVERE, errorMessage, ex);
            
            throw new ConfigurationException(errorMessage, ex);
        }
        catch (IOException ex) {
            String errorMessage = "Errore di lettura del file di configurazione: \"pkcs11-windows-libraries.properties\"";
            LOGGER.log(Level.SEVERE, errorMessage, ex);
            throw new ConfigurationException(errorMessage, ex);
        }
        List<String> libsList = null;
        Set<Object> osNames = libsMapper.keySet();
        String realOSName = getOsName();
        for (Object osNameObj : osNames) {
            String osNameFromFile = (String) osNameObj;
             if (realOSName.toUpperCase().contains(osNameFromFile.toUpperCase())) {
                String osFilesString = libsMapper.getProperty(osNameFromFile);
                
                libsList = new ArrayList(Arrays.asList(osFilesString.split(",")));
                break;
            }
        }
        if (libsList == null) {
            String errorMessage = String.format("Impossibile la lista delle librerie relative al sistema operativo %s nel file di configurazione: \"pkcs11-windows-libraries.properties\"", realOSName);
            LOGGER.log(Level.SEVERE, errorMessage);
            throw new ConfigurationException(errorMessage);
        }
        return libsList;
    }
 
    /**
     * Returns all card terminals of the system.
     *
     * @return Map of CardTerminal
     */
    public static Map<Integer, String> getCardTerminals() {
        Map<Integer, String> terminalsMap = new HashMap();
        try {
            List<CardTerminal> terminals = terminalFactory.terminals().list();
            if (terminals != null && !terminals.isEmpty()) {
                terminalsMap = new HashMap();
                for (int i = 0; i < terminals.size(); i++) {
                    try {
                        terminals.get(i).connect("*"); // se non ci sono smartcard inserite da errore e quindi non inserisco il lettore nella mappa
                        terminalsMap.put(i, terminals.get(i).getName());
                    } catch (CardException cardException) {
                        cardException.printStackTrace();
                    }
                }
            }
        } catch (CardException e) {
            LOGGER.log(Level.SEVERE, null, e);
            throw new RuntimeException(e);
        }
        return terminalsMap;
    }
}
