package it.bologna.ausl.jnjclient.manager;

import it.bologna.ausl.jnjclient.library.JnJClientCommand;
import it.bologna.ausl.jnjclient.library.exceptions.JnJClientCommandException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gdm
 */
import org.reflections.Reflections;
public class JnJCommandFactory {
    private static final Logger LOGGER = Logger.getLogger(JnJCommandFactory.class.getName());

    public static JnJClientCommand getCommand(String command) throws JnJClientCommandException {
        try {
            Reflections reflections = new Reflections("it.bologna.ausl.jnjclient.commands");
            Set<Class<? extends JnJClientCommand>> classes = reflections.getSubTypesOf(JnJClientCommand.class);
            for (Class classInfo : classes){
                JnJClientCommand jnjClientCommand = (JnJClientCommand) classInfo.getDeclaredConstructor().newInstance();
                if (jnjClientCommand.getCommand().equals(command)){
                    return jnjClientCommand;
                }
            }
            String errorMessage = String.format("nessun esecutore per il comando %s trovato", command);
            LOGGER.log(Level.SEVERE, errorMessage);
            throw new JnJClientCommandException(errorMessage);
        } catch (Exception ex) {
            String errorMessage = String.format("errore nell'esecuzione del comando %s", command);
            LOGGER.log(Level.SEVERE, errorMessage);
            throw new JnJClientCommandException(errorMessage, ex);
        }
    }
}
