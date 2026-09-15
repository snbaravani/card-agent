package com.card.client.controller;

import com.card.client.config.CardInboxProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Please use this to create e-mails manually....
 */
@RestController
public class CardCloseMailController {

    private final JavaMailSender mailSender;
    private final CardInboxProperties inbox;

    public CardCloseMailController(JavaMailSender mailSender, CardInboxProperties inbox) {
        this.mailSender = mailSender;
        this.inbox = inbox;
    }

    @PostMapping("/email")
    public ResponseEntity<SeedResult> seed(
            @RequestParam(defaultValue = "card-closure-support@mybank.com") String from,
            @RequestParam(defaultValue = "Card closure request") String subject,
            @RequestParam(defaultValue = " Kindly close this customer card.") String body) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(inbox.address());
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
        } catch (MailException e) {
            // Usually means the Mailpit SMTP server (compose.yaml) isn't up yet.
            return ResponseEntity.status(502).body(SeedResult.failed(from, inbox.address(), subject, e));
        }

        return ResponseEntity.ok(SeedResult.sent(from, inbox.address(), subject, body));
    }

    public record SeedResult(
            String status,
            String message,
            String from,
            String to,
            String subject,
            String body,
            String error,
            Instant timestamp
    ) {
        static SeedResult sent(String from, String to, String subject, String body) {
            return new SeedResult("sent",
                    "You email was delivered to %s; we will look into it as soon as possible.".formatted(to),
                    from, to, subject, body, null, Instant.now());
        }

        static SeedResult failed(String from, String to, String subject, Exception e) {
            return new SeedResult("failed",
                    "We were unable to deliver your email to %s. Please try again later...Thanks !".formatted(to),
                    from, to, subject, null, e.getMessage(), Instant.now());
        }
    }
}
