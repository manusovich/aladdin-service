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
import java.net.URL;
import java.nio.ByteBuffer;

/**
 * Created by Alex Manusovich on 1/21/15.
 */
public class ForecastServlet extends HttpServlet {
    private static final String API_KEY = System.getenv("AL_API_KEY");
    private static final int REQUEST_PERIOD = 5 * 60 * 1000;
    private static final int START_HOUR = 1;
    private static final int END_HOUR = 15;
    private static final int DATA_SIZE = 3 * 4 * (END_HOUR - START_HOUR);
    private static final int TEMP_MULTIPLY = 100;
    private static final int WIND_MULTIPLY = 100;
    private static final int PRECIP_MULTIPLY = 1000;

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
        int maxTemp = Integer.valueOf(System.getenv("AL_MAX_TEMP")) * TEMP_MULTIPLY;
        int minTemp = Integer.valueOf(System.getenv("AL_MIN_TEMP")) * TEMP_MULTIPLY;

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

                    int temperature = safeIntFromJson(hour, "apparentTemperature", TEMP_MULTIPLY);

                    if (temperature > maxTemp) {
                        temperature = maxTemp;
                    } else if (temperature < minTemp) {
                        temperature = minTemp;
                    } else {
                        float tempFloat = (float) 100 / (maxTemp - minTemp) * temperature;
                        temperature = (int) (tempFloat * TEMP_MULTIPLY);
                    }

                    int wind = safeIntFromJson(hour, "windSpeed", WIND_MULTIPLY);
                    int precip = safeIntFromJson(hour, "precipIntensity", PRECIP_MULTIPLY);

                    data.write(intToBytes(temperature));
                    data.write(intToBytes(wind));
                    data.write(intToBytes(precip));
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

    private byte[] intToBytes(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    private int safeIntFromJson(final JSONObject data,
                                final String dataKey,
                                final int multiply) throws IOException {
        Object jsonAttrValue = data.get(dataKey);
        if (jsonAttrValue instanceof Long) {
            return (int) ((Long) jsonAttrValue * multiply);
        } else {
            return (int) ((Double) jsonAttrValue * multiply);
        }
    }
}
