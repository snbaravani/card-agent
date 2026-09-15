package com.card.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Lightweight listing entry for one message, as returned inside a
 * {@link MailPitReply} from Mailpit's {@code /api/v1/search} endpoint. Carries
 * just enough to identify and triage a message before fetching the full
 * {@link MailPitMessage} body.
 */
public record MailPitSummary(
        @JsonProperty("ID") String id,
        @JsonProperty("MessageID") String messageId,
        @JsonProperty("Read") boolean read,
        @JsonProperty("From") MailPitAddress from,
        @JsonProperty("Subject") String subject,
        @JsonProperty("Created") String created
) {
}
