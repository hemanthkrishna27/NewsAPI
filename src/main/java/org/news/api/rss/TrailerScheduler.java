//package org.news.api.rss;
//
//
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import jakarta.annotation.PostConstruct;
//import jakarta.annotation.PreDestroy;
//import org.news.api.firebase.FirebaseWriter;
//import org.news.api.gemini.GeminiService;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.boot.context.event.ApplicationReadyEvent;
//import org.springframework.context.event.EventListener;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;
//import org.w3c.dom.Node;
//import org.w3c.dom.NodeList;
//
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//
//@Service
//public class TrailerScheduler {
//
//    final GeminiService geminiService;
//    final ObjectMapper objectMapper;
//    FirebaseWriter writer = new FirebaseWriter();
//
//    public TrailerScheduler(GeminiService geminiService, ObjectMapper objectMapper) {
//        this.geminiService = geminiService;
//        this.objectMapper = objectMapper;
//    }
//
//
//    @EventListener(ApplicationReadyEvent.class)
//    public void onStartup() {
//        new Thread(() -> {
//            try {
//                System.out.println("Running FactsGenerator immediately after app start...");
//                generateTrailers();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }).start();
//    }
//
//
//    @PreDestroy
//    public void shutdown() {
//        System.out.println("TrailerSchedulerrr shutting down...");
//    }
//
//
//    @Scheduled(fixedRate = 12 * 60 * 60 * 1000L)
//    public void generateTrailers() {
//
//        System.out.println("Running trailer scheduler");
//        String rssUrl = "https://www.moviefone.com/feeds/movie-trailers.rss";
//        try {
//
//            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//            DocumentBuilder builder = factory.newDocumentBuilder();
//            Document doc = builder.parse(new URL(rssUrl).openStream());
//            doc.getDocumentElement().normalize();
//
//            NodeList items = doc.getElementsByTagName("item");
//            List<TrailerItem> trailers = new ArrayList<>();
//
//            for (int i = 0; i < items.getLength(); i++) {
//                Node node = items.item(i);
//                if (node.getNodeType() == Node.ELEMENT_NODE) {
//                    Element element = (Element) node;
//                    String title = element.getElementsByTagName("title").item(0).getTextContent().trim();
//                    String trailerUrl = element.getElementsByTagName("guid").item(0).getTextContent().trim();
//                    String description = element.getElementsByTagName("description").item(0).getTextContent().trim();
//                    Element enclosure = (Element) element.getElementsByTagName("enclosure").item(0);
//                    String imgUrl = enclosure.getAttribute("url");
//
//                    trailers.add(new TrailerItem(title, imgUrl, trailerUrl, description));
//                }
//            }
//
//
//            ObjectMapper mapper = new ObjectMapper();
//            mapper.enable(SerializationFeature.INDENT_OUTPUT);
//            String jsonString = mapper.writeValueAsString(trailers);
//            String prompt = "Please rephrase the description alone and give the output in json format no extra wordings please" +
//                    "also i want in this format {\n" +
//                    "  \"title\": \"title\",\n" +
//                    "  \"imgUrl\": \"imgUrl\",\n" +
//                    "  \"trailerUrl\": \"trailerUrl\",\n" +
//                    "  \"description\": \"descrition\"\n" +
//                    "} " +
//                    " here i want the descriton alone to be repharsed and othres should be intact and give the json response alone please for this given data " + jsonString;
//
//            String responseJson = geminiService.getResult(prompt);
//            JsonNode node = mapper.readTree(responseJson);
//
//            List<TrailerItem> trailersList = new ArrayList<>();
//            if (node.isArray()) {
//                trailersList = mapper.readValue(jsonString, new TypeReference<List<TrailerItem>>() {
//                });
//            } else if (node.isObject()) {
//                trailersList.add(mapper.treeToValue(node, TrailerItem.class));
//            }
//
//
//            writer.writeTrailer(trailersList);
//
//            System.out.println("Trailer generation completed");
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//
//}
