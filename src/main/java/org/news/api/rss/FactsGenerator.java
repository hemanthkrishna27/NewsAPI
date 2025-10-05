package org.news.api.rss;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.news.api.firebase.FirebaseWriter;
import org.news.api.gemini.GeminiService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;


@Service
public class FactsGenerator implements CommandLineRunner {
    final GeminiService geminiService;
    final ObjectMapper objectMapper;
     FirebaseWriter writer=new FirebaseWriter();

    public FactsGenerator(GeminiService geminiService, ObjectMapper objectMapper) {
        this.geminiService = geminiService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) {

        System.out.println("Starting facts generation at " + new Date());
    }


    @Scheduled(fixedRate = 24 * 60 * 60 * 1000, initialDelay = 0)
    public void generateFacts() {
        try {
            String jsonResponse = geminiService.getResult(geminiService.getPromptForTrivia());
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            List<Trivia> trivList = objectMapper.convertValue(rootNode, new TypeReference<List<Trivia>>() {
            });

            writer.writeFacts(trivList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
