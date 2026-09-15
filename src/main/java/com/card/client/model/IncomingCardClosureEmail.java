package com.card.client.model;

import java.time.Instant;
import java.util.List;

/**
 *
 * @param messageId
 * @param from       sender address
 * @param to         recipient addresses
 * @param subject    subject line (never null; empty string if absent)
 * @param body       best-effort plain-text body
 * @param receivedAt when the mail server received the message
 */
public record IncomingCardClosureEmail(
        String messageId,
        String from,
        List<String> to,
        String subject,
        String body,
        Instant receivedAt
) {
}
