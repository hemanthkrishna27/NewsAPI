package org.news.api.rss;


import com.google.firebase.database.*;
import com.google.gson.Gson;
import org.news.api.firebase.FirebaseInitializer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;


public class Main {

    // Helper method to simulate your existing initialization logic
    private static final String DB_URL =
            "https://cinepulse-54397-default-rtdb.asia-southeast1.firebasedatabase.app/movie_news/data/";


    public static void main(String[] args) {
        // Ensure you have FirebaseInitializer and necessary imports (TimeUnit, ApiFuture, etc.)
        // 1. INITIALIZE: Set up the Firebase App
        try {

            Map<String, String> item = new HashMap<>();
            item.put("movie", "name");
            String uniqueId = java.util.UUID.randomUUID().toString();
            String json = new Gson().toJson(item);
            String url = DB_URL +  ".json";


            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> res = HttpClient.newHttpClient()
                    .send(req, HttpResponse.BodyHandlers.ofString());

            System.out.println("Response: " + res.statusCode() + " " + res.body());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }
}
