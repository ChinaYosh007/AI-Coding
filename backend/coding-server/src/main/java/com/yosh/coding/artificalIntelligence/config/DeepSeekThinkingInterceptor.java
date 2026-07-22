package com.yosh.coding.artificalIntelligence.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Adds DeepSeek V4's thinking-mode switch, which is not supported by the
 * LangChain4j 1.1.0 OpenAI request model yet.
 */
public class DeepSeekThinkingInterceptor implements ClientHttpRequestInterceptor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final boolean thinkingEnabled;

    public DeepSeekThinkingInterceptor(boolean thinkingEnabled) {
        this.thinkingEnabled = thinkingEnabled;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {
        return execution.execute(request, addThinkingMode(body));
    }

    byte[] addThinkingMode(byte[] body) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(body);
        if (!(root instanceof ObjectNode objectNode)) {
            return body;
        }
        objectNode.putObject("thinking").put("type", thinkingEnabled ? "enabled" : "disabled");
        return OBJECT_MAPPER.writeValueAsBytes(objectNode);
    }
}
