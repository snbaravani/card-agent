package com.card.client.controller;

import com.card.client.advisor.TokenUsageAdvisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;


import java.util.Vector;

@RestController
@RequestMapping("/api")
public class MCPClientController {

    private  final ChatClient chatClient;

    @Value("classpath:/prompts/card-agent-system.st")
    private Resource promptResource;

    private static final Logger logger = LoggerFactory.getLogger(MCPClientController.class);


    public MCPClientController(ChatClient.Builder chatClientBuilder,
            ToolCallbackProvider toolCallbackProvider, VectorStore vectorStore) {
        QuestionAnswerAdvisor questionAnswerAdvisor =  QuestionAnswerAdvisor.builder(vectorStore).build();
        this.chatClient = chatClientBuilder.defaultTools(toolCallbackProvider)
                .defaultAdvisors( new SimpleLoggerAdvisor(), questionAnswerAdvisor,  new TokenUsageAdvisor())
                .build();

    }

    @GetMapping("/cards")
    public String customer(@RequestHeader(value = "email",required = false) String email,
            @RequestParam("message") String message) {
        /** email is needed for customer related queries , not for product related**/
        Prompt prompt = new PromptTemplate(promptResource).create();
        return chatClient.prompt(prompt).user(message+" my email is"+email).call().content();
    }


}
