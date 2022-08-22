package it.bologna.ausl.jnjclient.library;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.*;
import javax.swing.JDialog;

public class JnJProgressBar extends JFrame {

    private final JProgressBar current;

    public JnJProgressBar() {
        super("Inizializzazione...");

        current = new JProgressBar(0, 100);
        current.setValue(0);

        setAlwaysOnTop(true);
        setResizable(false);

//        setEnabled(false);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowListener() {

            @Override
            public void windowOpened(WindowEvent e) {
            }

            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("Premuto X");
                System.exit(0);
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
        JPanel pane = new JPanel();
        pane.setLayout(null);

        current.setSize(300, 30);
        current.setStringPainted(true);
        pane.add(current);
//        setContentPane(pane);
        add(pane);
//        pack();
        setLocationByPlatform(true);
        setSize(306, 69);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((int)(((screenSize.width - getWidth()) / 2) * .7),(int)(((screenSize.height - getHeight()) / 2) * .7));
        
    }

    public int getCurrentValue() {
        return current.getValue();
    }

    public int getMaxValue() {
        return current.getMaximum();
    }

    public void addStep(int amount) {

        if (current.getValue() + amount >= getMaxValue())
            current.setValue(getMaxValue());
        else
            current.setValue(getCurrentValue() + amount);
//        update(getGraphics());
    }

    public void setValue(int value) {
        current.setValue(value);
        update(getGraphics());
    }

    public void reset() {
        current.setValue(0);
        update(getGraphics());
        System.err.println("barra del progresso settata a " + getCurrentValue());
    }

    public void setColor(Color color) {
        current.setForeground(color);
    }

    public Color getColor() {
        return current.getForeground();
    }

    public void terminate() {
        setTitle("Terminato");
        current.setValue(getMaxValue());

        setVisible(false);
        dispose();
    }


}
