package org.news.api.rss;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.rometools.modules.content.ContentModule;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.jsoup.Jsoup;
import org.news.api.gemini.GeminiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class Generator {
    private static final Logger logger = LoggerFactory.getLogger(Generator.class);
    private static final long ARTICLE_TTL_MS = 48 * 60 * 60 * 1000; // 48 hours
    private final Map<String, Long> seenArticles = new ConcurrentHashMap<>();
    private final Map<String, Long> seenTrailers = new ConcurrentHashMap<>();
    final GeminiService geminiService;
    final ObjectMapper objectMapper;

    public Generator(GeminiService geminiService, ObjectMapper objectMapper) {
        this.geminiService = geminiService;
        this.objectMapper = objectMapper;
    }

    public List<Trivia> generateFacts() {
        try {
            System.out.println("Starting facts generation at " + new Date());
            String jsonResponse = geminiService.getResult(geminiService.getPromptForTrivia());
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            List<Trivia> trivList = objectMapper.convertValue(rootNode, new TypeReference<List<Trivia>>() {
            });

            return trivList;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }


    public List<NewsItem> runScheduledTask(String FEED_URL) {
        logger.info("Scheduled task started for {}", FEED_URL);

        try {
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(new URL(FEED_URL)));
            List<SyndEntry> entries = feed.getEntries();

            cleanupOldArticles();

            if (entries.isEmpty()) {
                logger.info("No entries found for {}", FEED_URL);
                return null;
            }

            List<Map<String, Object>> articles = new ArrayList<>();

            for (SyndEntry entry : entries) {
                String link = entry.getLink();
                if (link == null || seenArticles.containsKey(link)) {
                    logger.debug("Skipping duplicate or invalid entry: {}", entry.getTitle());
                    continue;
                }

                seenArticles.put(link, System.currentTimeMillis());

                String html = "";
                try {
                    ContentModule contentModule = (ContentModule) entry.getModule(ContentModule.URI);
                    if (contentModule != null && contentModule.getContents() != null) {
                        html = contentModule.getContents().toString();
                    }
                } catch (Exception ignored) {
                }

                String plainText = Jsoup.parse(html).text();
                String imgUrl = entry.getEnclosures() != null && !entry.getEnclosures().isEmpty()
                        ? entry.getEnclosures().get(0).getUrl()
                        : null;

                Map<String, Object> article = new LinkedHashMap<>();
                article.put("title", entry.getTitle());
                article.put("link", link);
                article.put("author", entry.getAuthor());
                article.put("imgUrl", imgUrl);
                article.put("dateTime", entry.getPublishedDate());
                article.put("description", (entry.getDescription() != null ? entry.getDescription().getValue() : "") + plainText);

                articles.add(article);
            }

            if (articles.isEmpty()) {
                logger.info("No new unique articles found for {}", FEED_URL);
                return null;
            }

            // continue with Gemini and deserialization logic...
            String prompt = geminiService.createComprehensivePrompt(Map.of(
                    "source", feed.getTitle(),
                    "link", feed.getLink(),
                    "description", feed.getDescription(),
                    "language", feed.getLanguage(),
                    "articleCount", articles.size(),
                    "articles", articles
            ));

            String jsonResponse = geminiService.getResult(prompt);
            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                logger.warn("Empty response from Gemini. Skipping Firebase write.");
                return null;
            }

            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode targetNode;

            if (rootNode.isArray()) {
                targetNode = rootNode;
            } else if (rootNode.has("articles")) {
                JsonNode articlesNode = rootNode.get("articles");
                targetNode = articlesNode.isArray()
                        ? articlesNode
                        : objectMapper.createArrayNode().add(articlesNode);
            } else if (rootNode.isObject()) {
                targetNode = objectMapper.createArrayNode().add(rootNode);
            } else {
                throw new IllegalArgumentException("Unexpected JSON format");
            }

            List<NewsItem> newsList = objectMapper.convertValue(targetNode, new TypeReference<List<NewsItem>>() {
            });
            if (newsList == null || newsList.isEmpty()) {
                logger.warn("Failed to parse Gemini response for {}", FEED_URL);
                return null;
            }

            return newsList;

        } catch (Exception e) {
            logger.error("Error in scheduled task for {}:", FEED_URL, e);
            return null;
        }
    }

    /**
     * 🧹 Removes old entries beyond TTL
     */
    private void cleanupOldArticles() {
        long now = System.currentTimeMillis();
        seenArticles.entrySet().removeIf(entry -> (now - entry.getValue()) > ARTICLE_TTL_MS);
        logger.debug("Cleaned up old seen articles. Remaining cache size: {}", seenArticles.size());
    }


    public List<Upcoming> generateTrailers() throws Exception {
        System.out.println("Starting trailer at " + new Date());
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM");
        String monthName = sdf.format(cal.getTime());
        sdf = new SimpleDateFormat("YYYY");
        String yearName = sdf.format(cal.getTime());

        String prompt = "I want the list of hollywood movies that are yet to be released this month" + monthName + "-" + yearName + "with movie name youtube trailer url and date of release in json format i only want the json output also director name the youtube please find the exact url from the youtube{\n" +
                "\n" +
                "    \"movieName\":\n" +
                "\n" +
                "    \"directorName\": \n" +
                "\n" +
                "    \"releaseDate\": \n" +
                "\n" +
                "    \"trailerUrl\": \n" +
                "\n" +
                "  }  i want in this format no extra wordings only json please. you are giving the movie release dates and trailer url always wrong please proof check and give it right ";
//        String jsonResponse = geminiService.getResult(prompt);

        String jsonResponse = "[\n" +
                "  {\n" +
                "    \"movieName\": \"The Smashing Machine\",\n" +
                "    \"directorName\": \"Benny Safdie\",\n" +
                "    \"releaseDate\": \"October 3\",\n" +
                "    \"trailerUrl\": \"https://www.youtube.com/watch?v=aRpnP3LZ99g\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"movieName\": \"TRON: Ares\",\n" +
                "    \"directorName\": \"Joachim Rønning\",\n" +
                "    \"releaseDate\": \"October 10\",\n" +
                "    \"trailerUrl\": \"https://www.youtube.com/watch?v=YShVEXb7-ic\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"movieName\": \"The Black Phone 2\",\n" +
                "    \"directorName\": \"Scott Derrickson\",\n" +
                "    \"releaseDate\": \"October 17\",\n" +
                "    \"trailerUrl\": \"https://www.youtube.com/watch?v=r7hr2oo9Hks\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"movieName\": \"Good Fortune\",\n" +
                "    \"directorName\": \"Aziz Ansari\",\n" +
                "    \"releaseDate\": \"October 17\",\n" +
                "    \"trailerUrl\": \"https://www.youtube.com/watch?v=jHIJNHOOEvA\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"movieName\": \"Bugonia\",\n" +
                "    \"directorName\": \"Yorgos Lanthimos\",\n" +
                "    \"releaseDate\": \"October 31\",\n" +
                "    \"trailerUrl\": \"https://www.youtube.com/watch?v=pAEKMce0jk0\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"movieName\": \"Predator: Badlands\",\n" +
                "    \"directorName\": \"Dan Trachtenberg\",\n" +
                "    \"releaseDate\": \"November 7\",\n" +
                "    \"trailerUrl\": \"https://www.youtube.com/watch?v=43R9l7EkJwE\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"movieName\": \"The Running Man\",\n" +
                "    \"directorName\": \"Edgar Wright\",\n" +
                "    \"releaseDate\": \"November 7\",\n" +
                "    \"trailerUrl\": \"https://www.youtube.com/watch?v=9fqusNzkmzs\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"movieName\": \"Now You See Me: Now You Don't\",\n" +
                "    \"directorName\": \"Ruben Fleischer\",\n" +
                "    \"releaseDate\": \"November 14\",\n" +
                "    \"trailerUrl\": \"https://www.youtube.com/watch?v=vIi8uwkqsaU\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"movieName\": \"Wicked: For Good\",\n" +
                "    \"directorName\": \"Jon M. Chu\",\n" +
                "    \"releaseDate\": \"November 21\",\n" +
                "    \"trailerUrl\": \"https://www.youtube.com/watch?v=vt98AlBDI9Y\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"movieName\": \"Zootopia 2\",\n" +
                "    \"directorName\": \"Jared Bush and Byron Howard\",\n" +
                "    \"releaseDate\": \"November 26\",\n" +
                "    \"trailerUrl\": \"https://www.youtube.com/watch?v=BjkIOU5PhyQ\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"movieName\": \"Five Nights at Freddy's 2\",\n" +
                "    \"directorName\": \"Emma Tammi\",\n" +
                "    \"releaseDate\": \"December 5\",\n" +
                "    \"trailerUrl\": \"https://redcarpetcrash.com/watch-trailer-for-five-nights-at-freddys-2-in-theaters-december-5th/\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"movieName\": \"Avatar: Fire and Ash\",\n" +
                "    \"directorName\": \"James Cameron\",\n" +
                "    \"releaseDate\": \"December 19\",\n" +
                "    \"trailerUrl\": \"https://www.youtube.com/watch?v=nb_fFj_0rq8\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"movieName\": \"The SpongeBob Movie: Search for SquarePants\",\n" +
                "    \"directorName\": \"Derek Drymon\",\n" +
                "    \"releaseDate\": \"December 19\",\n" +
                "    \"trailerUrl\": \"No official trailer available yet.\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"movieName\": \"Anaconda\",\n" +
                "    \"directorName\": \"Tom Gormican\",\n" +
                "    \"releaseDate\": \"December 25\",\n" +
                "    \"trailerUrl\": \"https://www.youtube.com/watch?v=cZyU7mBXwLc\"\n" +
                "  }\n" +
                "]";

        JsonNode rootNode = objectMapper.readTree(jsonResponse);
        List<Upcoming> trivList = objectMapper.convertValue(rootNode, new TypeReference<List<Upcoming>>() {
        });
        return trivList;
    }


    public List<TrailerItem> generateActTrailers() {

        System.out.println("Running trailer scheduler");
        String rssUrl = "https://www.moviefone.com/feeds/movie-trailers.rss";
        try {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new URL(rssUrl).openStream());
            doc.getDocumentElement().normalize();

            NodeList items = doc.getElementsByTagName("item");
            List<TrailerItem> trailers = new ArrayList<>();

            for (int i = 0; i < items.getLength(); i++) {
                Node node = items.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String title = element.getElementsByTagName("title").item(0).getTextContent().trim();
                    String trailerUrl = element.getElementsByTagName("guid").item(0).getTextContent().trim();
                    String description = element.getElementsByTagName("description").item(0).getTextContent().trim();
                    Element enclosure = (Element) element.getElementsByTagName("enclosure").item(0);
                    String imgUrl = enclosure.getAttribute("url");

                    if (!seenTrailers.containsKey(trailerUrl)) {
                        trailers.add(new TrailerItem(title, imgUrl, trailerUrl, description));
                        seenTrailers.put(trailerUrl, System.currentTimeMillis());
                    }

                }
            }

            if (trailers.isEmpty())
                return null;


            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            String jsonString = mapper.writeValueAsString(trailers);
            String prompt = "Please rephrase the description alone and give the output in json format no extra wordings please" +
                    "also i want in this format {\n" +
                    "  \"title\": \"title\",\n" +
                    "  \"imgUrl\": \"imgUrl\",\n" +
                    "  \"trailerUrl\": \"trailerUrl\",\n" +
                    "  \"description\": \"descrition\"\n" +
                    "} " +
                    " here i want the descriton alone to be repharsed and othres should be intact and give the json response alone please for this given data " + jsonString;

            String responseJson = geminiService.getResult(prompt);
            JsonNode node = mapper.readTree(responseJson);

            List<TrailerItem> trailersList = new ArrayList<>();
            if (node.isArray()) {
                trailersList = mapper.readValue(jsonString, new TypeReference<List<TrailerItem>>() {
                });
            } else if (node.isObject()) {
                trailersList.add(mapper.treeToValue(node, TrailerItem.class));
            }

            return trailersList;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
