package com.card.client.service;


import com.card.client.advisor.TokenUsageAdvisor;
import com.card.client.config.CardInboxProperties;
import com.card.client.model.CardAgentResponse;
import com.card.client.model.IncomingCardClosureEmail;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * The Cards Closure through e-mail agent. This agent reads the e-mail box and filters them based on
 * if it has come from customer_support@mybank.com to card-closure-support@mybank.com. These are from the customer support
 * team after a customer
 * has a request through the support channel through a phone call
 */
@Service
public class CardSupportAgent {

    private final ChatClient chatClient;

    public CardSupportAgent(ChatClient.Builder chatClientBuilder,
                            ToolCallbackProvider mcpTools,
                            CardInboxProperties inbox,
                            @Value("classpath:/prompts/card-closure-agent-system.st") Resource systemPrompt) {

        Map params = new HashMap();
        params.put("closure_team_email",inbox.closureTeam());
        params.put("support_inbox",inbox.address());

        this.chatClient = chatClientBuilder
                .defaultSystem(sys -> sys.text(systemPrompt)
                .params(params))
                .defaultAdvisors(new TokenUsageAdvisor())
                .defaultTools(mcpTools)
                .build();
    }


    public CardAgentResponse cardClosure(IncomingCardClosureEmail email) {
        return chatClient.prompt()
                .user(u -> u.text("""
                        A new email has  arrived in the card support inbox. Please look into it.

                        From       : {from}
                        To         : {to}
                        Received   : {receivedAt}
                        Subject    : {subject}

                        Body:
                        {body}
                        """)
                        .param("from", email.from())
                        .param("to", String.join(", ", email.to()))
                        .param("receivedAt", email.receivedAt())
                        .param("subject", email.subject())
                        .param("body", email.body()))
                .call()
                .entity(CardAgentResponse.class);
    }
}
