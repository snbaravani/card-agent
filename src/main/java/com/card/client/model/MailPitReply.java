package com.card.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Envelope returned by Mailpit's {@code /api/v1/messages} and
 * {@code /api/v1/search} endpoints.
 */
public record MailPitReply(
        @JsonProperty("total") int total,
        @JsonProperty("unread") int unread,
        @JsonProperty("count") int count,
        @JsonProperty("messages") List<MailPitSummary> messages
) {
    public List<MailPitSummary> messages() {
        return messages != null ? messages : List.of();
    }
}
