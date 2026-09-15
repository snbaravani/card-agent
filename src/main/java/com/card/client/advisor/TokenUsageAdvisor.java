package com.card.client.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;

public class TokenUsageAdvisor implements CallAdvisor {

    private static final Logger log = LoggerFactory.getLogger(TokenUsageAdvisor.class);

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
        ChatResponse chatResponse = chatClientResponse.chatResponse();
        if(chatResponse.getMetadata() != null) {
            Usage usage = chatResponse.getMetadata().getUsage();
            if(usage != null) {

                log.info("--- LLM Token Usage Analytics ---");
                log.info("Model Used: {}", chatResponse.getMetadata().getModel());
                log.info("Prompt Tokens: {}", usage.getPromptTokens());
                log.info("Completion Tokens: {}", usage.getCompletionTokens());
                log.info("Total Tokens Used: {}", usage.getTotalTokens());
                log.info("---------------------------------");
            }
        }
        return chatClientResponse;
    }

    @Override
    public String getName() {
        return "TokenUsageAdvisor";
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
