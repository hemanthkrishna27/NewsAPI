package org.news.api.firebase;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import org.news.api.rss.NewsItem;
import org.news.api.rss.TrailerItem;
import org.news.api.rss.Trivia;
import org.news.api.rss.Upcoming;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class FirebaseWriter {
    private static final String DB_URL =
            "https://cinepulse-54397-default-rtdb.asia-southeast1.firebasedatabase.app/";

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

//            try {
//                ref.child(uniqueId).setValueAsync(json).get(2, TimeUnit.SECONDS); // push each item separately
//                System.out.println(" News pushed: " + item.getHead());
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
                    System.out.println(" via api News pushed: -->" + item.getHead());
                }
            } catch (Exception e) {
                System.out.println(" Failed to push news: -->" + item.getHead());
                e.printStackTrace();
            }
        }

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
            try{
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
            }catch (Exception e){
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

            try{
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
            }catch (Exception e){
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

            try{
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
            }catch (Exception e){

            }
        }

    }
}