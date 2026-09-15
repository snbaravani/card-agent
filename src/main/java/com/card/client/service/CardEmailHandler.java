package com.card.client.service;

import com.card.client.model.IncomingCardClosureEmail;


/**
 * Strategy for reacting to a single incoming card-closure email.
 * <p>
 * {@link CardInboxMonitor} pulls messages from the inbox and hands each one to the
 * active implementation of this interface — whether that just logs the email or
 * runs it through the full LLM-backed agent. Returning {@code false} tells the
 * monitor that handling failed, so the message is left unread and retried on the
 * next poll.
 */
@FunctionalInterface
public interface CardEmailHandler {

    boolean handleCardClosure(IncomingCardClosureEmail email);
}
