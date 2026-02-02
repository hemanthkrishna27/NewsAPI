package org.news.api.firebase;

import org.news.api.config.SchedulerLock;
import org.news.api.rapidapi.DailyRecommedationRapidApiConnector;
import org.news.api.rss.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;


@Component
public class FirebaseSchedulers {

    private final FirebaseWriter firebaseWriter = new FirebaseWriter();
    private final Generator generator;

    public FirebaseSchedulers(Generator generator) {
        this.generator = generator;
    }


    @Value("${rapid.api.key}")
    private String API_KEY;


    // ✅ Runs immediately on startup + every 30 minutes
    @Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000L)
    public void pushNews() {

        String[] FEED_URLS = {
                "https://screenrant.com/feed/",
                "https://collider.com/feed/"
        };

        for (String url : FEED_URLS) {
            runSafely("News", () -> {
                Map<String, Long> existingUrls = null;
                try {
                    existingUrls = firebaseWriter.loadSeenArticlesFromFirebase();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                List<NewsItem> news = generator.runScheduledTask(url, existingUrls, firebaseWriter);

                if (news != null && !news.isEmpty()) {
                    try {
                        firebaseWriter.write(news);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    // ✅ Runs immediately + once per day
    @Scheduled(initialDelay = 0, fixedRate = 24 * 60 * 60 * 1000L)
    public void pushTrailers() {
        runSafely("Trailer", () -> {
            Map<String, Long> existingUrls = null;
            try {
                existingUrls = firebaseWriter.loadSeenTrailersFromFirebase();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            List<TrailerItem> trailers =
                    generator.generateActTrailers(existingUrls, firebaseWriter);

            if (trailers != null && !trailers.isEmpty()) {
                try {
                    firebaseWriter.writeTrailer(trailers);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

//    // ✅ Runs only once (guarded by DB check)
//    public void pushUpcoming() {
//        runSafely("Upcoming", () -> {
//            List<Upcoming> upcoming = null;
//            try {
//                if (firebaseWriter.isUpcomingPresent()) return;
//                upcoming = generator.generateTrailers();
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//            try {
//                if (upcoming != null && !upcoming.isEmpty()) firebaseWriter.writeUpcoming(upcoming);
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        });
//    }

    // ✅ Midnight UTC cleanup
    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    public void runDailyCleanup() {
        runSafely("DailyCleanup", () -> {
            try {
                firebaseWriter.deleteOldNews();
                firebaseWriter.cleanupOldSeenArticles();
                firebaseWriter.cleanupOldSeenTrailers();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            System.out.println("🧹 Scheduled cleanup completed.");
        });
    }

    // ✅ Runs immediately + daily
    @Scheduled(initialDelay = 0, fixedRate = 24 * 60 * 60 * 1000L)
    public void pushFacts() {
        runSafely("Facts", () -> {
            List<Trivia> facts = generator.generateFacts();
            if (facts != null && !facts.isEmpty()) {
                try {
                    firebaseWriter.writeFacts(facts);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Scheduled(initialDelay = 0, fixedDelay = Long.MAX_VALUE)
    public void runOnStartup() {
        runDailyRecommendations();
    }
    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    public void runAtMidnight() {
        runDailyRecommendations();
    }


    public void runDailyRecommendations() {
        runSafely("DailyRecommendations", () -> {
            try {
                firebaseWriter.addRecommendations(API_KEY);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("✨ Recommendations updated");
        });
    }

    // 🔐 Centralized locking (good design)
    private void runSafely(String taskName, Runnable task) {
        if (SchedulerLock.LOCK.tryLock()) {
            try {
                System.out.println("▶ Starting " + taskName);
                task.run();
                System.out.println("✔ Completed " + taskName);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                SchedulerLock.LOCK.unlock();
            }
        } else {
            System.out.println("⏭ Skipped " + taskName + " — another task running");
        }
    }
}
