//package org.news.api.rss;
//
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ArrayNode;
//import com.rometools.modules.content.ContentModule;
//import com.rometools.rome.feed.synd.SyndEnclosure;
//import com.rometools.rome.feed.synd.SyndEntry;
//import com.rometools.rome.feed.synd.SyndFeed;
//import com.rometools.rome.io.SyndFeedInput;
//import com.rometools.rome.io.XmlReader;
//import jakarta.annotation.PostConstruct;
//import jakarta.annotation.PreDestroy;
//import org.jsoup.Jsoup;
//import org.news.api.firebase.FirebaseWriter;
//import org.news.api.gemini.GeminiService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.boot.context.event.ApplicationReadyEvent;
//import org.springframework.context.event.EventListener;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.net.URL;
//import java.util.*;
//
//@Component
//public class MyScheduledJob {
//
//    private static final Logger logger = LoggerFactory.getLogger(MyScheduledJob.class);
//    private static final String FEED_URLS[] = {"https://screenrant.com/feed/", "https://collider.com/feed/"};
//
//    private final Set<String> seenArticles = new HashSet<>();
//    private final GeminiService service;
//    private final ObjectMapper objectMapper;
//    private final FirebaseWriter firebaseWriter;
//
//    public MyScheduledJob(GeminiService service) {
//        this.service = service;
//        this.objectMapper = new ObjectMapper();
//        this.firebaseWriter = new FirebaseWriter();
//    }
//
//    @PreDestroy
//    public void shutdown() {
//        System.out.println("NewsGenerator shutting down...");
//    }
//
//    @EventListener(ApplicationReadyEvent.class)
//    public void onStartup() {
//        new Thread(() -> {
//            try {
//                System.out.println("Running FactsGenerator immediately after app start...");
//                runScheduledTask();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }).start();
//    }
//
//
//    @Scheduled(cron = "0 0/30 * * * *")
//    public void runScheduledTask() {
//        for (String FEED_URL : FEED_URLS) {
//            logger.info("Scheduled task started for " + FEED_URL);
//            try {
//                SyndFeedInput input = new SyndFeedInput();
//                SyndFeed feed = input.build(new XmlReader(new URL(FEED_URL)));
//                List<SyndEntry> entries = feed.getEntries();
//                boolean allNew = entries.stream().map(SyndEntry::getLink).noneMatch(seenArticles::contains);
//
//                if (allNew) {
//                    logger.info("All articles are new. Clearing cache of seen articles.");
//                    seenArticles.clear();
//                }
//                Map<String, Object> result = new LinkedHashMap<>();
//                result.put("source", feed.getTitle());
//                result.put("link", feed.getLink());
//                result.put("description", feed.getDescription());
//                result.put("language", feed.getLanguage());
//                result.put("articleCount", feed.getEntries().size());
//                List<Map<String, Object>> articles = new ArrayList<>();
//                for (SyndEntry entry : entries) {
//                    String link = entry.getLink();
//
//                    logger.debug("Processing article: {}", entry.getTitle());
//                    if (link == null || seenArticles.contains(link)) {
//                        logger.info("Skipping duplicate or invalid entry: {}", entry.getTitle());
//                        continue;
//                    }
//                    seenArticles.add(link);
//                    String html = "";
//                    try {
//                        ContentModule contentModule = (ContentModule) entry.getModule(ContentModule.URI);
//                        if (contentModule != null && contentModule.getContents() != null) {
//                            html = contentModule.getContents().toString();
//                        }
//                    } catch (Exception ignored) {
//                    }
//
//                    String plainText = Jsoup.parse(html).text();
//
//                    String imgUrl = null;
//                    List<SyndEnclosure> enclosures = entry.getEnclosures();
//                    if (enclosures != null && !enclosures.isEmpty()) {
//                        imgUrl = enclosures.get(0).getUrl();
//                    }
//
//                    Map<String, Object> article = new LinkedHashMap<>();
//                    article.put("title", entry.getTitle());
//                    article.put("link", link);
//                    article.put("author", entry.getAuthor());
//                    article.put("imgUrl", imgUrl);
//                    article.put("dateTime", entry.getPublishedDate());
//                    article.put("description", (entry.getDescription() != null ? entry.getDescription().getValue() : "") + plainText);
//
//                    articles.add(article);
//                }
//                result.put("articles", articles);
//                if (articles.isEmpty()) {
//                    logger.info("No new articles to process.");
//                    return;
//                }
//                String prompt = service.createComprehensivePrompt(result);
//                String jsonResponse = service.getResult(prompt);
//                if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
//                    logger.warn("Received null or empty response from Gemini service. Skipping Firebase write.");
//                    return;
//                }
//                JsonNode rootNode = objectMapper.readTree(jsonResponse);
//
//                List<NewsItem> newsList = null;
//                JsonNode targetNode;
//                if (rootNode.isArray()) {
//                    targetNode = rootNode;
//                } else if (rootNode.has("articles")) {
//                    JsonNode articlesNode = rootNode.get("articles");
//                    if (articlesNode.isArray()) {
//                        targetNode = articlesNode;
//                    } else if (articlesNode.isObject()) {
//                        ArrayNode arrayNode = objectMapper.createArrayNode();
//                        arrayNode.add(articlesNode);
//                        targetNode = arrayNode;
//                    } else {
//                        throw new IllegalArgumentException("Invalid JSON: 'articles' must be array or object");
//                    }
//                } else if (rootNode.isObject()) {
//                    ArrayNode arrayNode = objectMapper.createArrayNode();
//                    arrayNode.add(rootNode);
//                    targetNode = arrayNode;
//                } else {
//                    throw new IllegalArgumentException("Invalid JSON: expected array or object");
//                }
//                newsList = objectMapper.convertValue(targetNode, new TypeReference<List<NewsItem>>() {
//                });
//                if (newsList == null) {
//                    logger.warn("Could not deserialize articles from Gemini response. The 'articles' field might be missing or malformed. Response: {}", jsonResponse);
//                    return;
//                }
//                firebaseWriter.write(newsList);
//                logger.info("Scheduled task finished. Processed {} new articles.", newsList.size());
//            } catch (Exception e) {
//                logger.error("Error in scheduled task:", e);
//            }
//
//        }
//    }
//
//
//}
