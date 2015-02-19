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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;

/**
 * Created by Alex Manusovich on 1/21/15.
 */
public class ForecastServlet extends HttpServlet {
    private static final String API_KEY = System.getenv("AL_API_KEY");
    private static final int REQUEST_PERIOD = 5 * 60 * 1000;
    private static final int START_HOUR = 1;
    private static final int END_HOUR = 13;
    private static final int DATA_SIZE = 3 * 4 * (END_HOUR - START_HOUR);

    private final String mutex = "";
    private final ByteArrayOutputStream data = new ByteArrayOutputStream(DATA_SIZE);
    private long lastRequestTime;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        synchronized (mutex) {

            if ((System.currentTimeMillis() - lastRequestTime) > REQUEST_PERIOD) {
                try {
                    updateForecast();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            response.setHeader("Content-Type", "application/octet-stream");
            response.setHeader("Content-Length", "" + data.size());
            response.getOutputStream().write(data.toByteArray());
            response.getOutputStream().flush();
            response.getOutputStream().close();
            lastRequestTime = System.currentTimeMillis();
        }
    }

    private void updateForecast() throws IOException {
        BufferedReader reader = null;
        try {
            String urlTemplate = "https://api.forecast.io/forecast/%s/%s,%s";
            URL url = new URL(String.format(urlTemplate, API_KEY, System.getenv("AL_LAT"), System.getenv("AL_LON")));
            InputStreamReader streamReader = new InputStreamReader(url.openStream());
            reader = new BufferedReader(streamReader);

            JSONParser jsonParser = new JSONParser();
            try {
                JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
                JSONArray hourly = (JSONArray) ((JSONObject) jsonObject.get("hourly")).get("data");

                for (int i = START_HOUR; i < END_HOUR; i++) {
                    JSONObject hour = (JSONObject) hourly.get(i);
                    writeParameter(data, hour, "apparentTemperature", 100);
                    writeParameter(data, hour, "windSpeed", 100);
                    writeParameter(data, hour, "precipIntensity", 1000);
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private void writeParameter(final OutputStream target,
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
        target.write(bytes);
    }
}
