package it.bologna.ausl.jnjclient.firmajnj.tools;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 *
 * @author gdm
 */
public class GlobalTools {
    
    // Oggetto della libreria Jackson per manipolare il json. Singleton e ottenibile tramite la sua get
    private static ObjectMapper objectMapper = null;
    
    static {
        // per rendere l'istanza sigleton creo l'istanza in un blocco static, in modo che venga inizializzata all'avvio dell'applicazione
////        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
//        }
//        return objectMapper;
    }
    
    /**
     * Torna l'ObjetMapper della libreria Jackson. ObjetMapper è sigleton.
     * @return ObjetMapper configurato
     */
    public static ObjectMapper getObjectMapper() {
        // per rendere l'istanza sigleton, controllo prima se è null. Se è null la creo e la torno, altrimenti torno quella già esistente.
//        if (objectMapper == null) {
//            objectMapper = new ObjectMapper();
//            objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
//            objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
//        }
        return objectMapper;
    }
}
