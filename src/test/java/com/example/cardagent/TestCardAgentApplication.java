package com.example.cardagent;

import com.card.client.CardAgentApplication;
import org.springframework.boot.SpringApplication;
public class TestCardAgentApplication {

    public static void main(String[] args) {
        SpringApplication.from(CardAgentApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
