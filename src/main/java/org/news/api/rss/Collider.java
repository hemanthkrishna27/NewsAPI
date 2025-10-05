package org.news.api.rss;

import com.rometools.modules.content.ContentItem;
import com.rometools.modules.content.ContentModule;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class Collider {

    public static void main(String ... args) throws IOException, FeedException {
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

        System.out.println(articles);
    }
}
