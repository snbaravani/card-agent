package com.card.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Full message detail returned by Mailpit's {@code /api/v1/message/{ID}}
 * endpoint. Note: fetching this endpoint marks the message as read server-side.
 */
public record MailPitMessage(
        @JsonProperty("ID") String id,
        @JsonProperty("MessageID") String messageId,
        @JsonProperty("From") MailPitAddress from,
        @JsonProperty("To") List<MailPitAddress> to,
        @JsonProperty("Subject") String subject,
        @JsonProperty("Date") String date,
        @JsonProperty("Text") String text,
        @JsonProperty("HTML") String html
) {
    public List<MailPitAddress> to() {
        return to != null ? to : List.of();
    }
}
