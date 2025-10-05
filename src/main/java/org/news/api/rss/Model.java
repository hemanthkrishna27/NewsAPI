package org.news.api.rss;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rometools.modules.content.ContentModule;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.jsoup.Jsoup;
import org.news.api.gemini.GeminiService;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Model {

    public static void main(String... args) throws IOException, FeedException {

        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(new URL("https://collider.com/feed/")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", feed.getTitle());
        result.put("link", feed.getLink());
        result.put("description", feed.getDescription());
        result.put("language", feed.getLanguage());
        result.put("articleCount", feed.getEntries().size());

        List<Map<String, Object>> articles = new ArrayList<>();

        for (SyndEntry entry : feed.getEntries()) {
            String link = entry.getLink();


            // Extract content safely
            String html = "";
            try {
                ContentModule contentModule = (ContentModule) entry.getModule(ContentModule.URI);
                if (contentModule != null && contentModule.getContents() != null) {
                    html = contentModule.getContents().toString();
                }
            } catch (Exception ignored) {
            }

            String plainText = Jsoup.parse(html).text();

            String imgUrl = null;
            List<SyndEnclosure> enclosures = entry.getEnclosures();
            if (enclosures != null && !enclosures.isEmpty()) {
                imgUrl = enclosures.get(0).getUrl();
            }

            Map<String, Object> article = new LinkedHashMap<>();
            article.put("title", entry.getTitle());
            article.put("link", link);
            article.put("author", entry.getAuthor());
            article.put("imgUrl", imgUrl);
            article.put("dateTime", entry.getPublishedDate());
            article.put("description",
                    (entry.getDescription() != null ? entry.getDescription().getValue() : "") + plainText);

            articles.add(article);
        }

        result.put("articles", articles);
        ObjectMapper objectMapper = new ObjectMapper();

        GeminiService service = new GeminiService();
        // Convert to NewsItem list
        String jsonResponse = service.getResult("");

        JsonNode rootNode = objectMapper.readTree(jsonResponse);
        System.out.println(rootNode.toString());
        List<NewsItem> newsList = null;

        JsonNode targetNode;

        if (rootNode.isArray()) {

            targetNode = rootNode;
        } else if (rootNode.has("articles") && rootNode.get("articles").isArray()) {

            targetNode = rootNode.get("articles");
        } else {
            throw new IllegalArgumentException("Invalid JSON structure: expected array or 'articles'");
        }

        newsList = objectMapper.convertValue(targetNode, new TypeReference<List<NewsItem>>() {
        });


        for (NewsItem newsItem : newsList) {
            System.out.println(newsItem);
        }

    }
}
