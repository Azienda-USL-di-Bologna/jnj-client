package it.bologna.ausl.jnjclient.firmajnj.utils.swing;

import java.awt.event.KeyAdapter;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

/**
 *
 * @author gdm
 */
public class MyPasswordPane {
private final String title;
private JOptionPane pane;
private final Object[] messages;
private Object[] options_field;
private final JPasswordField pass;
private JDialog dialog;

    public MyPasswordPane(String title, Object[] messages, Object[] options) {
        this.title = title;
        this.messages = new Object[messages.length + 1];
        System.arraycopy(messages, 0, this.messages, 0, messages.length);
        pass = new JPasswordField();
        pass.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent kEvt) {
                if (kEvt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    kEvt.consume();
                    pane.setInitialValue(options_field[0]);
                    try {
                        Thread.sleep(100);
                        java.awt.Robot robot = new java.awt.Robot();
                        robot.setAutoDelay(100);
                        robot.keyPress(java.awt.event.KeyEvent.VK_ENTER);
                        robot.keyRelease(java.awt.event.KeyEvent.VK_ENTER);
                    }
                    catch (InterruptedException | java.awt.AWTException ex) {
                    }
                }
            }
        });
        this.messages[messages.length] = pass;
        this.options_field = options;
        pane = new JOptionPane(this.messages, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION,  null, options_field, this.messages);

    }
    
    public int getSelection() {
        dialog = pane.createDialog(title);
        dialog.setVisible(true);
        Object selectedValue = pane.getValue();
        if(selectedValue == null)
            return JOptionPane.CLOSED_OPTION;
        // If there is not an array of option buttons:
        if(options_field == null) {
            if(selectedValue instanceof Integer)
                return ((Integer)selectedValue);
            return JOptionPane.CLOSED_OPTION;
        }
        // If there is an array of option buttons:
        for(int counter = 0, maxCounter = options_field.length;
            counter < maxCounter; counter++) {
            if(options_field[counter].equals(selectedValue))
                return counter;
        }
        return JOptionPane.CLOSED_OPTION;
    }

    public char[] getPassword() {
        return pass.getPassword();
    }

    public void setText(String text) {
        pass.setText(text);
    }

    public void dispose() {
        dialog.dispose();
    }
}
