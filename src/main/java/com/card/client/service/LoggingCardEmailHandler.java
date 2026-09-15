package com.card.client.service;

import com.card.client.model.IncomingCardClosureEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default {@link CardEmailHandler} that simply logs whatever lands in the inbox.
 * <p>
 * It is the baseline so the monitoring loop is verifiable on its own. Replace
 * or wrap it once the agent's real actions (classification, LLM reply drafting,
 * tool calls, ...) are wired in.
 */
@Component
public class LoggingCardEmailHandler implements CardEmailHandler {

    private static final Logger log = LoggerFactory.getLogger(LoggingCardEmailHandler.class);

    @Override
    public boolean handleCardClosure(IncomingCardClosureEmail email) {
        log.info("""
                === New email ===
                From    : {}
                To      : {}
                Subject : {}
                Body    :
                {}
                =================""",
                email.from(), email.to(), email.subject(), email.body());
        return true;
    }
}
