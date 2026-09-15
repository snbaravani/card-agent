package com.card.client.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**

 * Reply to the customer explaining the outcome of the customer request.
 * If it successful, a closure confirmation
 * If not, explaining why card can't eb closed
 */
@JsonClassDescription("The outcome of handling card closure support email: the reply to send to the customer plus an internal summary.")
public record CardAgentResponse(

        @JsonPropertyDescription("Subject for the reply email to the customer, e.g. \"Re: Your card closure request\".")
        String subject,

        @JsonPropertyDescription("The complete, ready-to-send reply to the customer, written in their language and tone. "
                + "Plain text, properly greeting and signing off. No placeholders or bracketed TODOs.")
        String body,

        @JsonPropertyDescription("A short internal summary for the human operator: who the customer was, what they "
                + "wanted, what you found, and what action you took (include any refund id / ticket id).")
        String operatorSummary
) {
}
