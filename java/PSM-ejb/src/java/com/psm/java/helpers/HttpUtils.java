/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.psm.java.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 * @author Sean
 */
public class HttpUtils {
    
    
    public static String openUrl(String Url)
    {
        
        try {
            URL URl = new URL(Url);
            HttpURLConnection conn = (HttpURLConnection) URl.openConnection();
            conn.setRequestProperty("User-Agent", System.getProperties().
                    getProperty("http.agent") + " agent");
            String response = read(conn.getInputStream());
            return response;
        } catch (Exception ex) {
        }
        return null;
    }

    public static String read(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(in), 1000);
        for (String line = r.readLine(); line != null; line = r.readLine()) {
            sb.append(line);
        }
        in.close();
        return sb.toString();
    }
    
}
