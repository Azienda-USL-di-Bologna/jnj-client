/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.jnjclient.firmajnj.utils;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;

/**
 *
 * @author gdm
 */
public class HttpUtils {
    /**
     * Costruisce l'HttpClient per fare le chiamate http con timeout impostato a 15 minuti.
     * @return 
     */
    public static OkHttpClient getHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        OkHttpClient client = builder
                .connectTimeout(15, TimeUnit.MINUTES)
                .readTimeout(15, TimeUnit.MINUTES)
                .writeTimeout(15, TimeUnit.MINUTES)
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
        return client;
    }
}
