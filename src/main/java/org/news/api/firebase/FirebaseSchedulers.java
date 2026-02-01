package org.news.api.firebase;

import org.news.api.config.SchedulerLock;
import org.news.api.rss.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class FirebaseSchedulers {

    private FirebaseWriter firebaseWriter = new FirebaseWriter();
    private final Generator generator;

    public FirebaseSchedulers(Generator generator) {
        this.generator = generator;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runAllImmediatelyAfterStartup() {
        new Thread(() -> {
            pushNews();
            pushTrailers();
            pushUpcoming();
            pushFacts();
            runDailyCleanup();
            runDailyRecommendations();

        }).start();
    }

    @Scheduled(fixedRate = 30 * 60 * 1000L) // every 15 min
    public void pushNews() {
        String FEED_URLS[] = {"https://screenrant.com/feed/", "https://collider.com/feed/"};
        for (String url : FEED_URLS) {
            runSafely("News", () -> {
                Map<String, Long> existingUrls = null;
                try {
                    existingUrls = firebaseWriter.loadSeenArticlesFromFirebase();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                List<NewsItem> news = generator.runScheduledTask(url, existingUrls, firebaseWriter);
                try {
                    if (news != null && !news.isEmpty())
                        firebaseWriter.write(news);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Scheduled(fixedRate = 24 * 60 * 60 * 1000L)
    public void pushTrailers() {
        runSafely("Trailer", () -> {
            Map<String, Long> existingUrls = null;
            try {
                existingUrls = firebaseWriter.loadSeenTrailersFromFirebase();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            List<TrailerItem> trailers = generator.generateActTrailers(existingUrls,firebaseWriter);
            try {
                if (trailers != null && !trailers.isEmpty())
                    firebaseWriter.writeTrailer(trailers);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }


    public void pushUpcoming() {
        runSafely("Upcoming", () -> {
            List<Upcoming> upcoming = null;
            try {
                if (firebaseWriter.isUpcomingPresent())
                    return;

                upcoming = generator.generateTrailers();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            try {

                if (upcoming != null && !upcoming.isEmpty())
                    firebaseWriter.writeUpcoming(upcoming);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    public void runDailyCleanup() {

        runSafely("DailyCleanup", () -> {
            try {
                firebaseWriter.deleteOldNews();
                firebaseWriter.cleanupOldSeenArticles();
                firebaseWriter.cleanupOldSeenTrailers();
                System.out.println("🧹 Scheduled cleanup completed.");
            } catch (Exception e) {
                System.err.println("⚠️ Cleanup failed: " + e.getMessage());
            }
        });

    }

    @Scheduled(fixedRate = 24 * 60 * 60 * 1000L)
    public void pushFacts() {
        runSafely("Facts", () -> {
            List<Trivia> facts = generator.generateFacts();
            try {
                if (facts != null && !facts.isEmpty())
                    firebaseWriter.writeFacts(facts);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void runSafely(String taskName, Runnable task) {
        if (SchedulerLock.LOCK.tryLock()) {
            try {
                System.out.println(" Starting " + taskName + " push...");
                task.run();
                System.out.println(" Completed " + taskName + " push!");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                SchedulerLock.LOCK.unlock();
            }
        } else {
            System.out.println(" Skipped " + taskName + " — another task still running");
        }
    }

        @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    public void runDailyRecommendations() {

        runSafely("DailyRecommendations", () -> {
            try {
                firebaseWriter.addRecommendations();
                System.out.println(" Scheduled cleanup + new recommedations addeed");
            } catch (Exception e) {
                System.err.println(" Cleanup failed + addition: " + e.getMessage());
            }
        });

    }


}