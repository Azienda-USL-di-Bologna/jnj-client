package it.bologna.ausl.jnjclient.firmajnj.utils.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;


/** Mosta una maschera per la selezione di una chiave di firma
 *
 * @author gdm
 */
public class SelectionForm extends JDialog implements KeyListener {
private ButtonGroup group;
private Integer selectedIndex;

    /** Crea un JDialog per la selezione di una chiave per la firma
     *
     * @param keyList mappa contenente indice e valore da mostrare di ogni chiave di firma
     */
    public SelectionForm(Map keyList) {

        this.group = new ButtonGroup();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        addWindowListener(new WindowListener() {

            @Override
            public void windowOpened(WindowEvent e) {
            }

            @Override
            public void windowClosing(WindowEvent e) {
                selectedIndex = -1;
            }

            @Override
            public void windowClosed(WindowEvent e) {
            }

            @Override
            public void windowIconified(WindowEvent e) {
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
            }

            @Override
            public void windowActivated(WindowEvent e) {
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
            }
        });

        setTitle("Selezione chiave di firma");

        setModalityType(DEFAULT_MODALITY_TYPE);
        fillForm(keyList);
    }

    // riempie il form
    private void fillForm(Map keyList) {
        // Carica i vari "RadioButton" corrispondenti alle chiavi
        Set<Map.Entry<Object,Object>> smartCardSet = keyList.entrySet();
        Iterator smartCardSetIterator = smartCardSet.iterator();

        JPanel smartCardPanel = new JPanel();
        smartCardPanel.setLayout(new BoxLayout(smartCardPanel, BoxLayout.Y_AXIS));
        JLabel labelTitle = new JLabel(" Seleziona la chiave di firma da utilizzare:            ");
        smartCardPanel.add(labelTitle);
        smartCardPanel.add(new JLabel(" "));

        for (int i=0; i<keyList.size(); i++) {
            Map.Entry<Integer, String> setElement = (Map.Entry<Integer, String>) smartCardSetIterator.next();
            JRadioButton key = new JRadioButton(setElement.getValue(), true);
            key.addKeyListener(this);
            // Assegno al nome del JRadioButton, l'indice relativo alla chiave, in modo da poterlo ottenere quando
            // l'utente clicca sul bottone "Ok"
            key.setName(setElement.getKey().toString());
            group.add(key);
            smartCardPanel.add(key);
        }

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        JButton confirmButton = new JButton("Ok");
        JButton cancelButton = new JButton("Annulla");
        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        confirmButton.addKeyListener(this);
        cancelButton.addKeyListener(this);

        // quando viene premuto il bottone "Ok" identifica la chiave selezionata e ne setta la proprieta relativa
        confirmButton.addActionListener((ActionEvent e) -> {
            Enumeration<AbstractButton> buttons = group.getElements();
            for (int i=0; i < group.getButtonCount(); i++) {
                JRadioButton button = (JRadioButton) buttons.nextElement();
                if (button.isSelected()) {
                    selectedIndex = Integer.parseInt(button.getName());
                    break;
                }
            }
            dispose();
        });

        cancelButton.addActionListener((ActionEvent e) -> {
            selectedIndex = -1;
            dispose();
        });

        // Eventuale controllo in caso non ci siano chiavi va qui

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(new JScrollPane(smartCardPanel), BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        addKeyListener(this);

        // setto il pulsante "Ok" (confirmButton) come bottone selezionato premendo "Invio" sulla tastiera
        getRootPane().setDefaultButton(confirmButton);

        pack();
        setSize(getWidth() + (int)(getWidth() * 0.2), getHeight() + (int)(getHeight() * 0.2));
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screenSize.width - getWidth()) / 2, (screenSize.height - getHeight()) / 2 );
        setVisible(true);
        setAlwaysOnTop(true);
    }

    /** Ritorna l'indice della chiave selezionata
     *
     * @return indice della chiave selezionata, "-1" se non ne è stata selazionata nessuna
     */
    public int getSelectionIndex() {
        return selectedIndex;
    }

    @Override
    public void keyPressed(KeyEvent kEvt) {
        if (kEvt.getKeyCode() != KeyEvent.VK_ENTER)
            kEvt.consume();

        if (kEvt.getKeyCode() == KeyEvent.VK_ESCAPE) {
            selectedIndex = -1;
            dispose();
        }

        if (kEvt.getKeyCode() == KeyEvent.VK_DOWN) {
            ArrayList<AbstractButton> buttonsArrayList = new ArrayList();
            Enumeration<AbstractButton> radioButtons = group.getElements();
            while (radioButtons.hasMoreElements()) {
                buttonsArrayList.add(radioButtons.nextElement());
            }
            for (int i=0; i<buttonsArrayList.size() - 1; i++) {
                if (buttonsArrayList.get(i).isSelected()) {
                    buttonsArrayList.get(i + 1).setSelected(true);
                    break;
                }
            }
        }

        if (kEvt.getKeyCode() == KeyEvent.VK_UP) {
            ArrayList<AbstractButton> buttonsArrayList = new ArrayList();
            Enumeration<AbstractButton> radioButtons = group.getElements();
            while (radioButtons.hasMoreElements()) {
                buttonsArrayList.add(radioButtons.nextElement());
            }
            for (int i=buttonsArrayList.size()-1; i>0; i--) {
                if (buttonsArrayList.get(i).isSelected()) {
                    buttonsArrayList.get(i - 1).setSelected(true);
                    break;
                }
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

}