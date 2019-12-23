package com.flashlight.marsar;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.StrictMode;
import android.widget.Button;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PlanetPosition {
    HashMap<String, String> urls = new HashMap<>();

    public PlanetPosition() {
        urls.put("Mars", "https://www.calsky.com/cs.cgi/Planets/5/1");
        urls.put("Venus", "https://www.calsky.com/cs.cgi/Planets/3/1");
        urls.put("Jupiter", "https://www.calsky.com/cs.cgi/Planets/6/1");
        urls.put("Moon", "https://www.calsky.com/cs.cgi/Moon/1");
    }

    public static String convertStreamToString(InputStream inputStream) throws IOException {
        if (inputStream != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"),1024);
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                inputStream.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }

    public void getPlanetPosition(String planetName, MainActivity caller) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);

        Button skyButton = (Button) caller.findViewById(R.id.buttonUpdateAir);
        skyButton.setEnabled(false);
        skyButton.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("Processing...");
                    //if(true) throw new Exception("test");

                    URL url = new URL(urls.get(planetName));
                    URLConnection urlConnection = url.openConnection();
                    urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36");
                    InputStream in = urlConnection.getInputStream();
                    String res = convertStreamToString(in);

                    Pattern ALT_PATTERN = Pattern.compile("Altitude:</b>\\s+[\\-\\d\\.]+");
                    Pattern AZ_PATTERN = Pattern.compile("Azimuth:</b>\\s+[\\-\\d\\.]+");

                    Matcher alt_m = ALT_PATTERN.matcher(res);
                    Matcher az_m = AZ_PATTERN.matcher(res);

                    String alt = "";
                    String az = "";

                    while (alt_m.find()) {
                        alt = alt_m.group(0);
                    }
                    while (az_m.find()) {
                        az = az_m.group(0);
                    }

                    alt = alt.replaceAll("[^\\d\\-\\.]", "");
                    az = az.replaceAll("[^\\d\\-\\.]", "");

                    HashMap<String, Float> result = new HashMap<>();
                    result.put("altitude", Float.parseFloat(alt));
                    result.put("azimuth", Float.parseFloat(az));
                    caller.setPlanetPosition(result);
                } catch (Exception e) {
                    e.printStackTrace();
                    HashMap<String, Float> result = new HashMap<>();
                    result.put("altitude", 45f);
                    result.put("azimuth", 100f);
                    caller.setPlanetPosition(result);
                }
            }
        }).start();
    }
}