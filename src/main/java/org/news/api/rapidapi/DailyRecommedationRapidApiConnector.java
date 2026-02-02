package org.news.api.rapidapi;



import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.news.api.model.RapidMedia;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;


public class DailyRecommedationRapidApiConnector {




    public RapidMedia getRecommendation(String API_KEY) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://rottentomato.p.rapidapi.com/today-recomendations"))
                .header("x-rapidapi-key", API_KEY)
                .header("x-rapidapi-host", "rottentomato.p.rapidapi.com")
                .GET()
                .build();

        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());


        String jsonResponse = response.body();

        System.out.println(jsonResponse);

        ObjectMapper mapper = new ObjectMapper();

        RapidMedia mediaList =
                mapper.readValue(jsonResponse, new TypeReference<RapidMedia>() {});



        return mediaList;
    }


}
