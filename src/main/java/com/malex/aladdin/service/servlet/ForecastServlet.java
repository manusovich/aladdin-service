package com.malex.aladdin.service.servlet;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.ByteBuffer;

/**
 * Created by Alex Manusovich on 1/21/15.
 */
public class ForecastServlet extends HttpServlet {

    public static final int START_HOUR = 1;
    public static final int END_HOUR = 13;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        URL url;
        InputStream is = null;
        BufferedReader br;

        response.setHeader("Content-Type", "application/octet-stream");
        response.setHeader("Content-Length", "" + 3 * 4 * 12);

        try {
            // austin
            //url = new URL("https://api.forecast.io/forecast/a1b3feeab00e064da02d075a4d29e82f/30.267153,-97.743061");
            // jakarta
            url = new URL("https://api.forecast.io/forecast/a1b3feeab00e064da02d075a4d29e82f/-6.208763,106.845599");

            is = url.openStream();
            br = new BufferedReader(new InputStreamReader(is));

            JSONParser jsonParser = new JSONParser();
            try {
                JSONObject jsonObject = (JSONObject) jsonParser.parse(br);
                JSONArray hourly = (JSONArray) ((JSONObject) jsonObject.get("hourly")).get("data");

                for (int i = START_HOUR; i < END_HOUR; i++) {
                    JSONObject hour = (JSONObject) hourly.get(i);
                    writeParameter(response, hour, "apparentTemperature", 100);
                    writeParameter(response, hour, "windSpeed", 100);
                    writeParameter(response, hour, "precipIntensity", 1000);
                }

                response.getOutputStream().flush();
                response.getOutputStream().close();
            } catch (ParseException e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private void writeParameter(final HttpServletResponse response,
                                final JSONObject hour,
                                final String dataKey,
                                final int q) throws IOException {
        int temp;

        Object temperature = hour.get(dataKey);
        if (temperature instanceof Long) {
            temp = (int) ((Long) temperature * q);
        } else {
            temp = (int) ((Double) temperature * q);
        }

        byte[] bytes = ByteBuffer.allocate(4).putInt(temp).array();
        response.getOutputStream().write(bytes);
    }
}
