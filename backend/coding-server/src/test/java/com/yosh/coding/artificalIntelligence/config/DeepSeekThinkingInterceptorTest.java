package com.yosh.coding.artificalIntelligence.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeepSeekThinkingInterceptorTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void disablesThinkingForStreamingCodeGeneration() throws Exception {
        byte[] requestBody = """
                {"model":"deepseek-v4-flash","stream":true,"messages":[]}
                """.getBytes(StandardCharsets.UTF_8);

        byte[] customizedBody = new DeepSeekThinkingInterceptor(false).addThinkingMode(requestBody);
        JsonNode result = OBJECT_MAPPER.readTree(customizedBody);

        assertEquals("disabled", result.path("thinking").path("type").asText());
        assertEquals("deepseek-v4-flash", result.path("model").asText());
        assertEquals(true, result.path("stream").asBoolean());
    }
}
