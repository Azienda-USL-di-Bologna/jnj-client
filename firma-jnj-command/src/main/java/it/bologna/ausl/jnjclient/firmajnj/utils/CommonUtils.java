/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.jnjclient.firmajnj.utils;

import it.bologna.ausl.jnjclient.library.JnJProgressBar;

/**
 *
 * @author gdm
 */
public class CommonUtils {
    public static void safelyIncrementProgessBar(JnJProgressBar progressBar, int amount) {
        if (progressBar != null) {
            progressBar.addStep(amount);
        }
    }
    
   public static int getProgressBarAmountForEachStep(JnJProgressBar progressBar, int steps) {
        if (progressBar != null) {
            int remaining = progressBar.getMaxValue() - progressBar.getCurrentValue();
            return remaining / steps;
        } else {
            return 0;
        }
   }
}
