package com.card.client.service;

import com.card.client.config.CardInboxProperties;
import com.card.client.model.MailPitAddress;
import com.card.client.model.MailPitMessage;
import com.card.client.model.MailPitSummary;
import com.card.client.model.IncomingCardClosureEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;


/**
 * Periodically polls the Mailpit inbox for unread card-closure requests and hands
 * each one to the active {@link CardEmailHandler}.
 * <p>
 * Runs on a fixed delay via {@link org.springframework.scheduling.annotation.Scheduled},
 * ignores mail that isn't addressed from the configured closure-team address, and
 * leaves a message unread (so it is retried on the next poll) whenever fetching or
 * handling it fails.
 */
@Service
public class CardInboxMonitor {

    private static final Logger log = LoggerFactory.getLogger(CardInboxMonitor.class);

    private final MailpitClient mailpit;
    private final CardEmailHandler handler;
    private final CardInboxProperties props;
    @Value("${card-agent.inbox.closure-team}")
    private  String fromAddress;

    public CardInboxMonitor(MailpitClient mailpit, CardEmailHandler handler, CardInboxProperties props) {
        this.mailpit = mailpit;
        this.handler = handler;
        this.props = props;
    }

    @Scheduled(fixedDelayString = "${card-agent.inbox.poll-interval:15000}")
    public void poll() {
        try {
            List<MailPitSummary> unread = mailpit.listUnread(props.batchSize());
            if (unread.isEmpty()) {
                log.debug("No new mail");
                return;
            }
            log.info("Found {} new message(s)", unread.size());
            for (MailPitSummary summary : unread) {
                processOne(summary.id());
            }
        } catch (Exception e) {
            // Mailpit may be starting up or briefly unreachable. Log and let the
            // next scheduled poll retry rather than killing the scheduler.
            log.warn("Inbox poll failed: {}", e.getMessage(), e);
        }
    }

    private void processOne(String id) {
        try {
            // Fetching the full message marks it read on the server.
            MailPitMessage message = mailpit.getMessage(id);
            IncomingCardClosureEmail email = toIncomingEmail(message);
            if(email == null){
                return;
            }
            boolean handled = handler.handleCardClosure(email);
            if (!handled) {
                // Leave it unread so the next poll picks it up again.
                mailpit.setRead(id, false);
            }
        } catch (Exception e) {
            log.error("Failed to process message {}; resetting to unread for retry", id, e);
            try {
                mailpit.setRead(id, false);
            } catch (Exception reset) {
                log.warn("Could not reset message {} to unread: {}", id, reset.getMessage());
            }
        }
    }

    private IncomingCardClosureEmail toIncomingEmail(MailPitMessage message) {
        String from = message.from() != null ? message.from().address() : "(unknown)";
        if(from != null && !from.equals(fromAddress)){
            log.info("Ignoring this mail from:{}", from);
            return null;
        }
        List<String> to = message.to().stream()
                .map(MailPitAddress::address)
                .toList();

        String subject = message.subject() != null ? message.subject() : "";
        String body = bestBody(message);
        Instant receivedAt = parseDate(message.date());
        return new IncomingCardClosureEmail(message.messageId(), from, to, subject, body, receivedAt);
    }

    /** Prefer the plain-text body; fall back to HTML if that is all there is. */
    private String bestBody(MailPitMessage message) {
        if (message.text() != null && !message.text().isBlank()) {
            return message.text().strip();
        }
        return message.html() != null ? message.html().strip() : "";
    }

    private Instant parseDate(String date) {
        if (date == null || date.isBlank()) {
            return Instant.now();
        }
        try {
            return OffsetDateTime.parse(date).toInstant();
        } catch (Exception e) {
            try {
                return Instant.parse(date);
            } catch (Exception ex) {
                return Instant.now();
            }
        }
    }
}
