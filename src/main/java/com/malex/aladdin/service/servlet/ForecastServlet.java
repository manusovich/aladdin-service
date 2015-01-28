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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;

/**
 * Created by Alex Manusovich on 1/21/15.
 */
public class ForecastServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        URL url;
        InputStream is = null;
        BufferedReader br;

        response.setHeader("Content-Type", "application/octet-stream");
        response.setHeader("Content-Length", "" + 4 * 12);

        try {
            url = new URL("https://api.forecast.io/forecast/a1b3feeab00e064da02d075a4d29e82f/30.267153,-97.743061");
            is = url.openStream();  // throws an IOException
            br = new BufferedReader(new InputStreamReader(is));

            JSONParser jsonParser = new JSONParser();
            try {
                JSONObject jsonObject = (JSONObject) jsonParser.parse(br);
                JSONArray hourly = (JSONArray) ((JSONObject) jsonObject.get("hourly")).get("data");
                for (int i = 1; i < 13; i++) {
                    JSONObject hour = (JSONObject) hourly.get(i);
                    Object temperature = hour.get("temperature");
                    int temp = 0;
                    if (temperature instanceof Long) {
                        temp = (int) ((Long) temperature * 100);
                    } else {
                        temp = (int) ((Double) temperature * 100);
                    }
                    byte[] bytes = ByteBuffer.allocate(4).putInt(temp).array();
                    response.getOutputStream().write(bytes);
                    System.out.println(temp);
                }
                response.getOutputStream().flush();
                response.getOutputStream().close();
            } catch (ParseException e) {
                e.printStackTrace();
            }

        } catch (MalformedURLException mue) {
            mue.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException ioe) {
                // nothing to see here
            }
        }
    }
}
