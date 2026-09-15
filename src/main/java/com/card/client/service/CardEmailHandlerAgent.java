package com.card.client.service;

import com.card.client.model.CardAgentResponse;
import com.card.client.model.IncomingCardClosureEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;


/**
 * Primary {@link CardEmailHandler} that resolves card-closure emails end to end.
 * <p>
 * Delegates to {@link CardSupportAgent} to have the LLM draft a reply, then uses
 * {@link CardClosureMailSender} to send it back to the customer. Any failure during
 * that flow is logged and reported as unhandled, so {@link CardInboxMonitor} leaves
 * the message unread and retries it on the next poll.
 */
@Component
@Primary
public class CardEmailHandlerAgent implements CardEmailHandler {

    private static final Logger log = LoggerFactory.getLogger(CardEmailHandlerAgent.class);

    private final CardSupportAgent agent;
    private final CardClosureMailSender mailSender;

    public CardEmailHandlerAgent(CardSupportAgent agent, CardClosureMailSender mailSender) {
        this.agent = agent;
        this.mailSender = mailSender;
    }

    @Override
    public boolean handleCardClosure(IncomingCardClosureEmail email) {
        log.info("Handing email for card closure from {} (subject: \"{}\") to the support agent", email.from(), email.subject());
        try {
            CardAgentResponse response = agent.cardClosure(email);
            mailSender.sendReply(email, response);
            return true;
        } catch (Exception e) {
            log.error("Card Agent failed to resolve an e-mail  from {} (subject: \"{}\"); will be tried again",
                    email.from(), email.subject(), e);
            return false;
        }
    }
}
