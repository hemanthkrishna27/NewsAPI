package org.news.api.gemini;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;


@Service
public class GeminiService {

    @Autowired
    private RestTemplate restTemplate;

    String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    @Value("${gemini.api.key}")
    private String apiKey;


    ObjectMapper mapper = new ObjectMapper();

    public String getResult(String prompt) {
        try {
            GeminiRequest request = new GeminiRequest(prompt);
            String fullApiUrl = apiUrl + "?key=" + apiKey;
            GeminiResponse response = restTemplate.postForObject(fullApiUrl, request, GeminiResponse.class);

            if (response != null && response.getCandidates() != null && !response.getCandidates().isEmpty()) {
                String jsonResponse = response.getCandidates().get(0).getContent().getParts().get(0).getText();
                jsonResponse = jsonResponse.replace("```json", "").replace("```", "").trim();
                return jsonResponse;
            }
        } catch (RestClientException e) {

            e.printStackTrace();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public String createComprehensivePrompt(Map<String, Object> articles) {
        try {
            String article = mapper.writeValueAsString(articles);
            return String.format("%s here only for the articles i want the output in json format like { \"head\": headoutput,\"content\":content,\"imgUrl\": url,\"dateTime\": date,\"url\": url,\"author\": author}\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "You are an repharsing assistant " +
                    "My requirements for headoutput rephrase the title from the above articles dont use same exact wordings and for" +
                    " content just summarize that content in 60 words not less than or greater than 60 words i want exactly 60 words please if no information is there then search for actual story and then summarise the content it should no be the same wordings " +
                    "rest of the fields should have actual data in the article itself. Also the url is the link of the article" +
                    " and give summary in 60 words only i want only the json output nothing Note:  Also please proof read the description sometimes you are giving wrong info so please", article
            );
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    public String getPromptForTrivia() {
        return "I want 10 interesting facts about movies in the json format. The json should have movie_name, director, and trivia (which should contain at least 30 words). Please make the trivia unique, and do not write extra wordings. I only want json as output. Current Unix Timestamp to ensure uniqueness: " + System.currentTimeMillis();
    }
}
