package org.news.api.rss;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.news.api.firebase.FirebaseWriter;
import org.news.api.gemini.GeminiService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class UpcomingMovies implements CommandLineRunner {
    final GeminiService geminiService;
    final ObjectMapper objectMapper;
    FirebaseWriter writer = new FirebaseWriter();

    public UpcomingMovies(GeminiService geminiService, ObjectMapper objectMapper) {
        this.geminiService = geminiService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) {

        System.out.println("Starting facts generation at " + new Date());
    }

    @Scheduled(fixedRate = 30 * 24 * 60 * 60 * 1000L, initialDelay = 0)
    public void generateTrailers() throws Exception {
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

        System.out.println(jsonResponse);

        JsonNode rootNode = objectMapper.readTree(jsonResponse);
        List<Upcoming> trivList = objectMapper.convertValue(rootNode, new TypeReference<List<Upcoming>>() {
        });

        writer.writeUpcoming(trivList);
    }


}
