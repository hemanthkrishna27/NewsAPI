package org.news.api.firebase;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.news.api.rss.NewsItem;
import org.news.api.rss.TrailerItem;
import org.news.api.rss.Trivia;
import org.news.api.rss.Upcoming;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class FirebaseWriter {

    public void write(List<NewsItem> items) throws Exception {
        FirebaseInitializer.init();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("movie_news/data");

        for (NewsItem item : items) {
            String formattedDate = Instant.ofEpochMilli(Long.parseLong(item.getDateTime()))
                    .atZone(ZoneId.of("UTC"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

            item.setDateTime(formattedDate);
            String uniqueId = ref.push().getKey();
            item.setId(uniqueId);

            try {
                ref.child(uniqueId).setValueAsync(item).get(); // push each item separately
                System.out.println(" News pushed: " + item.getHead());
            } catch (InterruptedException | ExecutionException e) {
                System.out.println(" Failed to push news: " + item.getHead());
                e.printStackTrace();
            }
        }

    }


    public void writeTrailer(List<TrailerItem> items) throws Exception {
        FirebaseInitializer.init();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("movie_trailer/data");
        for (TrailerItem item : items) {
            String uniqueId = ref.push().getKey();
            item.setId(uniqueId);
            try {
                ref.child(uniqueId).setValueAsync(item).get(); // push each item separately
                System.out.println(" Trailer pushed: " + item.getTitle());
            } catch (InterruptedException | ExecutionException e) {
                System.out.println(" Failed to push trailer: " + item.getTitle());
                e.printStackTrace();
            }
        }

    }

    public void writeUpcoming(List<Upcoming> items) throws Exception {
        FirebaseInitializer.init();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("upcoming/releases");

        for (Upcoming item : items) {

            String uniqueId = ref.push().getKey();
            String videoId = "";
            String url = item.getTrailerUrl();

            if (url != null && url.contains("v=")) {
                videoId = url.substring(url.indexOf("v=") + 2);
                int ampIndex = videoId.indexOf('&');
                if (ampIndex != -1) {
                    videoId = videoId.substring(0, ampIndex);
                }
            }
            item.setTrailerUrl(videoId);
            System.out.println("video id: " + videoId);

            try {
                ref.child(uniqueId).setValueAsync(item).get(); // push each item separately
                System.out.println(" Upcoming pushed: " + item.getMovieName());
            } catch (InterruptedException | ExecutionException e) {
                System.out.println(" Failed to push news: " + item.getMovieName());
                e.printStackTrace();
            }
        }
    }

    public void writeFacts(List<Trivia> items) throws Exception {
        FirebaseInitializer.init();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("trivia/data");

        for (Trivia item : items) {
            String uniqueId = ref.push().getKey();
            item.setId(uniqueId);
            item.setImgUrl("url_dummy");

            try {
                ref.child(uniqueId).setValueAsync(item).get(); // push each item separately
                System.out.println(" Trivia pushed: " + item.getMovie_name());
            } catch (InterruptedException | ExecutionException e) {
                System.out.println(" Failed to push news: " + item.getMovie_name());
                e.printStackTrace();
            }
        }

    }
}
