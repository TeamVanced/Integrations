package fi.vanced.utils.requests;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Requester {
    private Requester() {}

    public static HttpURLConnection getConnectionFromRoute(String apiUrl, Route route, String... params) throws IOException {
        String url = apiUrl + route.compile(params).getCompiledRoute();
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(route.getMethod().name());
        return connection;
    }

    public static String parseJson(HttpURLConnection connection) throws IOException {
        StringBuilder jsonBuilder = new StringBuilder();
        InputStream inputStream = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            jsonBuilder.append(line);
        }
        inputStream.close();
        return jsonBuilder.toString();
    }

    public static JSONObject getJSONObject(HttpURLConnection connection) throws Exception {
        return new JSONObject(parseJsonAndDisconnect(connection));
    }

    public static JSONArray getJSONArray(HttpURLConnection connection) throws Exception {
        return new JSONArray(parseJsonAndDisconnect(connection));
    }

    private static String parseJsonAndDisconnect(HttpURLConnection connection) throws IOException {
        String json = parseJson(connection);
        connection.disconnect();
        return json;
    }
}