package org.news.api.firebase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import com.google.firebase.database.*;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.news.api.rss.NewsItem;
import org.news.api.rss.TrailerItem;
import org.news.api.rss.Trivia;
import org.news.api.rss.Upcoming;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class FirebaseWriter {
    private static final String DB_URL =
            "https://cinepulse-54397-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private static final long TWO_DAYS_MS = 2 * 24 * 60 * 60 * 1000L;




    public void deleteOldNews() throws Exception {
        String url = DB_URL + "movie_news/data.json"; // base Firebase path
        HttpClient client = HttpClient.newHttpClient();

        // 1️⃣ Fetch all data
        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> getRes = client.send(getReq, HttpResponse.BodyHandlers.ofString());

        if (getRes.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch data: " + getRes.body());
        }

        // 2️⃣ Parse JSON into a map
        Type type = new TypeToken<Map<String, NewsItem>>() {}.getType();
        Map<String, NewsItem> newsMap = new Gson().fromJson(getRes.body(), type);

        if (newsMap == null || newsMap.isEmpty()) {
            System.out.println("✅ No news to clean up.");
            return;
        }

        long now = Instant.now().toEpochMilli();
        long twoDaysMillis = 2 * 24 * 60 * 60 * 1000;
        int deletedCount = 0;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                .withZone(ZoneId.of("UTC"));

        // 3️⃣ Iterate through items
        for (Map.Entry<String, NewsItem> entry : newsMap.entrySet()) {
            String id = entry.getKey();
            NewsItem item = entry.getValue();

            if (item.getDateTime() == null) continue;

            try {
                Instant itemInstant = Instant.from(formatter.parse(item.getDateTime()));
                long itemTime = itemInstant.toEpochMilli();

                // 4️⃣ If older than 2 days → DELETE
                if (now - itemTime > twoDaysMillis) {
                    String deleteUrl = DB_URL + "movie_news/data/" + id + ".json";
                    HttpRequest deleteReq = HttpRequest.newBuilder()
                            .uri(URI.create(deleteUrl))
                            .DELETE()
                            .build();

                    HttpResponse<String> deleteRes = client.send(deleteReq, HttpResponse.BodyHandlers.ofString());

                    if (deleteRes.statusCode() == 200) {
                        System.out.println("🗑️ Deleted old news: " + item.getHead());
                        deletedCount++;
                    } else {
                        System.out.println("⚠️ Failed to delete " + id + ": " + deleteRes.body());
                    }
                }

            } catch (Exception e) {
                System.out.println("❌ Error parsing/deleting: " + e.getMessage());
            }
        }

        System.out.println("✅ Cleanup done. Deleted " + deletedCount + " old items.");
    }


    public void write(List<NewsItem> items) throws Exception {
        FirebaseInitializer.init();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("movie_news/data");


        for (NewsItem item : items) {
            String uniqueId = java.util.UUID.randomUUID().toString();
            String url = DB_URL + "movie_news/data" + ".json";
            String formattedDate = Instant.ofEpochMilli(Long.parseLong(item.getDateTime()))
                    .atZone(ZoneId.of("UTC"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

            item.setDateTime(formattedDate);


            item.setId(uniqueId);
            String json = new Gson().toJson(item);

            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> res = HttpClient.newHttpClient()
                        .send(req, HttpResponse.BodyHandlers.ofString());

                if (res.statusCode() == 200) {
                    System.out.println(" via api News pushed: -->" + item.getHead());
                }
            } catch (Exception e) {
                System.out.println(" Failed to push news: -->" + item.getHead());
                e.printStackTrace();
            }
        }

    }


    public Map<String, Long> loadSeenArticlesFromFirebase() throws Exception {
        FirebaseInitializer.init();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DB_URL + "seenArticles.json"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();
        String body = response.body();

        if (body == null || body.equals("null") || body.isBlank()) {
            System.out.println("No seen articles yet in Firebase.");
            return new HashMap<>();
        }

        Map<String, Long> rawMap = mapper.readValue(response.body(), new TypeReference<>() {
        });

        Map<String, Long> seenArticles = new HashMap<>();
        for (Map.Entry<String, Long> entry : rawMap.entrySet()) {
            String decodedUrl = decodeUrl(entry.getKey());
            seenArticles.put(decodedUrl, entry.getValue());
        }

        System.out.println("Loaded " + (seenArticles != null ? seenArticles.size() : 0) + " articles from Firebase.");
        return seenArticles != null ? seenArticles : new HashMap<>();
    }

    public  void cleanupOldSeenArticles() throws Exception {
        String firebaseDbUrl = DB_URL + "seenArticles.json";
        HttpClient client = HttpClient.newHttpClient();

        // Step 1: Fetch all seen articles
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(firebaseDbUrl))
                .GET()
                .build();

        HttpResponse<String> response = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            System.err.println("Failed to fetch data: " + response.statusCode());
            return;
        }

        // Step 2: Parse JSON
        JsonObject allArticles = JsonParser.parseString(response.body()).getAsJsonObject();
        if (allArticles == null) {
            System.out.println("No data found.");
            return;
        }

        long now = System.currentTimeMillis();
        int deleted = 0;

        // Step 3: Loop + delete old entries
        for (Map.Entry<String, JsonElement> entry : allArticles.entrySet()) {
            long ts = entry.getValue().getAsLong();
            if (now - ts > TWO_DAYS_MS) {
                String delUrl = DB_URL + "seenArticles/" + entry.getKey() + ".json";
                HttpRequest delRequest = HttpRequest.newBuilder()
                        .uri(URI.create(delUrl))
                        .DELETE()
                        .build();
                client.send(delRequest, HttpResponse.BodyHandlers.discarding());
                deleted++;
            }
        }

        System.out.println("Cleanup done ✅ Deleted " + deleted + " old entries.");
    }

    public void saveSeenArticleToFirebase(String urlKey) throws Exception {
        String firebaseDbUrl = DB_URL + "seenArticles/"
                + encodeUrl(urlKey) + ".json";

        long timestamp = System.currentTimeMillis();
        String json = String.valueOf(timestamp); // just the number

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(firebaseDbUrl))
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            System.out.println("Saved article: " + urlKey);
        } else {
            System.err.println("Failed to save article: " + urlKey + " | HTTP code: " + response.statusCode());
        }
    }

    private String encodeUrl(String url) {
        return Base64.getUrlEncoder().encodeToString(url.getBytes());
    }

    private String decodeUrl(String encoded) {
        return new String(Base64.getUrlDecoder().decode(encoded));
    }

    public boolean isUpcomingPresent() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DB_URL + "upcoming/releases.json"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();
        if (body == null || body.equals("null") || body.isBlank()) {
            return false;
        }

        return true;
    }

    public void writeTrailer(List<TrailerItem> items) throws Exception {
        FirebaseInitializer.init();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("movie_trailer/data");

        for (TrailerItem item : items) {
            String uniqueId = java.util.UUID.randomUUID().toString();
            String url = DB_URL + "movie_trailer/data" + ".json";
            item.setId(uniqueId);
            String json = new Gson().toJson(item);
//            try {
//                ref.child(uniqueId).setValueAsync(item).get(2, TimeUnit.SECONDS);
//                System.out.println(" Trailer pushed: " + item.getTitle());
//            } catch (Exception e)
//
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> res = HttpClient.newHttpClient()
                        .send(req, HttpResponse.BodyHandlers.ofString());

                if (res.statusCode() == 200) {
                    System.out.println(" via api Trailer pushed: -->" + item.getTitle());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void writeUpcoming(List<Upcoming> items) throws Exception {
        FirebaseInitializer.init();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("upcoming/releases");


        for (Upcoming item : items) {
            String uniqueId = java.util.UUID.randomUUID().toString();
            String url = DB_URL + "upcoming/releases" + ".json";
            String videoId = "";
            String trailer_url = item.getTrailerUrl();

            if (trailer_url != null && trailer_url.contains("v=")) {
                videoId = trailer_url.substring(trailer_url.indexOf("v=") + 2);
                int ampIndex = videoId.indexOf('&');
                if (ampIndex != -1) {
                    videoId = videoId.substring(0, ampIndex);
                }
            }
            item.setTrailerUrl(videoId);

            String json = new Gson().toJson(item);

//            try {
//                ref.child(uniqueId).setValueAsync(item).get(2, TimeUnit.SECONDS);
//                ; // push each item separately
//                System.out.println(" Upcoming pushed: " + item.getMovieName());
//            } catch (Exception e)

            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> res = HttpClient.newHttpClient()
                        .send(req, HttpResponse.BodyHandlers.ofString());

                if (res.statusCode() == 200) {
                    System.out.println(" via api Upcoming pushed: -->" + item.getMovieName());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void writeFacts(List<Trivia> items) throws Exception {
        FirebaseInitializer.init();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("trivia/data");

        for (Trivia item : items) {
            String uniqueId = java.util.UUID.randomUUID().toString();
            String url = DB_URL + "trivia/data" + ".json";
            item.setId(uniqueId);
            item.setImgUrl("url_dummy");
            String json = new Gson().toJson(item);

//            try {
//                ref.child(uniqueId).setValueAsync(item).get(2, TimeUnit.SECONDS);
//                ; // push each item separately
//                System.out.println(" Trivia pushed: " + item.getMovie_name());
//            } catch (Exception e)

            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> res = HttpClient.newHttpClient()
                        .send(req, HttpResponse.BodyHandlers.ofString());

                if (res.statusCode() == 200) {
                    System.out.println(" via api Trivia pushed: -->" + item.getMovie_name());
                }
            } catch (Exception e) {

            }
        }

    }

}