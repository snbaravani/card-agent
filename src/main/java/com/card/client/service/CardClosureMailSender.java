package com.card.client.service;

import com.card.client.config.CardInboxProperties;
import com.card.client.model.CardAgentResponse;
import com.card.client.model.IncomingCardClosureEmail;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Sends the agent's drafted reply back to the customer over SMTP (the same
 * Mailpit instance the inbox is monitored from). The reply is threaded onto the
 * original message via the {@code In-Reply-To}/{@code References} headers so it
 * shows up as a proper response rather than a fresh email.
 */
@Service
public class CardClosureMailSender {

    private static final Logger log = LoggerFactory.getLogger(CardClosureMailSender.class);

    private static final DateTimeFormatter QUOTE_DATE =
            DateTimeFormatter.ofPattern("EEE, d MMM yyyy 'at' HH:mm", Locale.ENGLISH)
                    .withZone(ZoneId.systemDefault());

    private final JavaMailSender mailSender;
    private final CardInboxProperties inbox;

    public CardClosureMailSender(JavaMailSender mailSender, CardInboxProperties inbox) {
        this.mailSender = mailSender;
        this.inbox = inbox;
    }

    /**
     *
     * @param original
     * @param response
     * @return
     * @throws Exception
     */
    public boolean sendReply(IncomingCardClosureEmail original, CardAgentResponse response) throws Exception {
        String recipient = original.from();
        if (recipient == null || recipient.isBlank() || "(unknown)".equals(recipient)) {
            log.warn("No usable sender address on email \"{}\"; skipping reply", original.subject());
            return false;
        }
        if (response.body() == null || response.body().isBlank()) {
            log.warn("Agent produced no reply body for email from {}; skipping reply", recipient);
            return false;
        }

        MimeMessage mime = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
        helper.setFrom(inbox.address());
        helper.setTo(recipient);
        helper.setSubject(replySubject(original, response));
        helper.setText(quoteOriginal(original, response.body()));

        // Thread the reply onto the original message when we know its Message-ID.
        String messageId = original.messageId();
        if (messageId != null && !messageId.isBlank()) {
            String ref = messageId.startsWith("<") ? messageId : "<" + messageId + ">";
            mime.setHeader("In-Reply-To", ref);
            mime.setHeader("References", ref);
        }

        mailSender.send(mime);
        log.info("Replied to {} (subject: \"{}\")", recipient, mime.getSubject());
        return true;
    }


    private String replySubject(IncomingCardClosureEmail original, CardAgentResponse response) {
        String base = original.subject() != null && !original.subject().isBlank()
                ? original.subject()
                : (response.subject() != null ? response.subject() : "");
        return base.regionMatches(true, 0, "Re:", 0, 3) ? base : "Re: " + base;
    }


    private String quoteOriginal(IncomingCardClosureEmail original, String replyBody) {
        String body = original.body() != null ? original.body() : "";
        String quoted = body.isBlank()
                ? ""
                : body.stripTrailing().lines().map(line -> "> " + line).reduce((a, b) -> a + "\n" + b).orElse("");
        return """
                %s

                On %s, %s wrote:
                %s""".formatted(
                replyBody.stripTrailing(),
                QUOTE_DATE.format(original.receivedAt()),
                original.from(),
                quoted);
    }
}
