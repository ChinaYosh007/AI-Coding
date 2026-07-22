package com.yosh.coding.core.handle;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yosh.coding.artificalIntelligence.skill.BaseTool;
import com.yosh.model.enums.StreamMessageTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonMessageStreamHandlerTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldParseAndDeduplicateToolMessages() {
        JsonMessageStreamHandler handler = new JsonMessageStreamHandler();
        Map<String, BaseTool> tools = (Map<String, BaseTool>) ReflectionTestUtils.invokeMethod(
                handler, "createToolMap", 1L, 1L);
        assertNotNull(tools);

        StringBuilder history = new StringBuilder();
        Set<String> seenToolIds = new HashSet<>();
        JSONObject request = JSONUtil.createObj()
                .set("type", StreamMessageTypeEnum.TOOL_REQUEST.getValue())
                .set("id", "tool-1")
                .set("name", "writeToFile");

        String firstRequest = ReflectionTestUtils.invokeMethod(handler, "handleJsonMessageChunk",
                request.toString(), history, seenToolIds, tools);
        String duplicateRequest = ReflectionTestUtils.invokeMethod(handler, "handleJsonMessageChunk",
                request.toString(), history, seenToolIds, tools);

        assertNotNull(firstRequest);
        assertTrue(firstRequest.contains("Write to File"));
        assertEquals("", duplicateRequest);

        JSONObject arguments = JSONUtil.createObj()
                .set("relativePath", "src/App.vue")
                .set("content", "<template>ok</template>");
        JSONObject executed = JSONUtil.createObj()
                .set("type", StreamMessageTypeEnum.TOOL_EXECUTED.getValue())
                .set("id", "tool-1")
                .set("name", "writeToFile")
                .set("arguments", arguments.toString());

        String output = ReflectionTestUtils.invokeMethod(handler, "handleJsonMessageChunk",
                executed.toString(), history, seenToolIds, tools);

        assertNotNull(output);
        assertTrue(output.contains("src/App.vue"));
        assertTrue(output.contains("<template>ok</template>"));
        assertEquals(output, history.toString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldKeepProcessingWhenToolArgumentsAreInvalid() {
        JsonMessageStreamHandler handler = new JsonMessageStreamHandler();
        Map<String, BaseTool> tools = (Map<String, BaseTool>) ReflectionTestUtils.invokeMethod(
                handler, "createToolMap", 1L, 1L);
        assertNotNull(tools);

        JSONObject executed = JSONUtil.createObj()
                .set("type", StreamMessageTypeEnum.TOOL_EXECUTED.getValue())
                .set("id", "tool-2")
                .set("name", "writeToFile")
                .set("arguments", "not-json");

        String output = ReflectionTestUtils.invokeMethod(handler, "handleJsonMessageChunk",
                executed.toString(), new StringBuilder(), new HashSet<>(), tools);

        assertNotNull(output);
        assertTrue(output.contains("Write to File"));
    }
}
