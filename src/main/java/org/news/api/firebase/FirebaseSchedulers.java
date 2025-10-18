package org.news.api.firebase;

import org.news.api.config.SchedulerLock;
import org.news.api.rss.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

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
        }).start();
    }

    @Scheduled(fixedRate = 30 * 60 * 1000L) // every 15 min
    public void pushNews() {
        String FEED_URLS[] = {"https://screenrant.com/feed/", "https://collider.com/feed/"};
        for (String url : FEED_URLS) {
            runSafely("News", () -> {
                List<NewsItem> news = generator.runScheduledTask(url);
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
            List<TrailerItem> trailers = generator.generateActTrailers();
            try {
                if (trailers != null && !trailers.isEmpty())
                    firebaseWriter.writeTrailer(trailers);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Scheduled(fixedRate = 30 * 24 * 60 * 60 * 1000L)
    public void pushUpcoming() {
        runSafely("Upcoming", () -> {
            List<Upcoming> upcoming = null;
            try {
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
                System.out.println("🚀 Starting " + taskName + " push...");
                task.run();
                System.out.println("✅ Completed " + taskName + " push!");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                SchedulerLock.LOCK.unlock();
            }
        } else {
            System.out.println("⚠️ Skipped " + taskName + " — another task still running");
        }
    }


}
