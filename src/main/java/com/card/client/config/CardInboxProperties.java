package com.card.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "card-agent.inbox")
public record CardInboxProperties(
        String baseUrl,
        String address,
        long pollInterval,
        int batchSize,
        String closureTeam
) {
    public CardInboxProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8025";
        }
        if (address == null || address.isBlank()) {
            address = "card-closure-support@mybank.com";
        }
        if (batchSize <= 0) {
            batchSize = 5;
        }
    }
}
