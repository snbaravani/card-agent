package com.card.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single email address as Mailpit represents it, pairing the display name
 * with the raw address. Used for the {@code From}/{@code To} fields on both
 * {@link MailPitSummary} and {@link MailPitMessage}.
 */
public record MailPitAddress(
        @JsonProperty("Name") String name,
        @JsonProperty("Address") String address
) {
}
