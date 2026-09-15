package com.card.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class CardAgentApplication {

    public static void main(String[] args) {
        System.out.println("Hello from the card agent !");
        SpringApplication.run(CardAgentApplication.class, args);
    }

}
