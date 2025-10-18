package org.news.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@SpringBootApplication
public class NewsApiApplication {


    public static void main(String[] args) {
        SpringApplication.run(NewsApiApplication.class, args);
    }


}
