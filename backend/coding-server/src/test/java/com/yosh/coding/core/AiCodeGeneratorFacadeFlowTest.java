package com.yosh.coding.core;

import com.yosh.coding.agent.subagent.resource.ResourceCollectionAgent;
import com.yosh.coding.agent.model.image.query.ResourceCollectionResult;
import com.yosh.exception.BusinessException;
import com.yosh.model.enums.CodeGenTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class AiCodeGeneratorFacadeFlowTest {

    @Test
    void modificationSkipsResourceCollectionAndKeepsModifyMode() {
        AiCodeGeneratorFacade facade = spy(new AiCodeGeneratorFacade());
        ResourceCollectionAgent resourceCollectionAgent = mock(ResourceCollectionAgent.class);
        ReflectionTestUtils.setField(facade, "resourceCollectionAgent", resourceCollectionAgent);
        doReturn(Flux.just("修改完成"))
                .when(facade)
                .generateAndSaveCodeStreamInternal(
                        "修改标题", CodeGenTypeEnum.MULTI_FILE, 10L, 2L, true, List.of());

        List<String> result = facade.generateAndSaveCodeStream(
                        "修改标题", CodeGenTypeEnum.MULTI_FILE, 10L, 2L, true)
                .collectList()
                .block();

        assertEquals(List.of("修改完成"), result);
        verifyNoInteractions(resourceCollectionAgent);
        verify(facade).generateAndSaveCodeStreamInternal(
                "修改标题", CodeGenTypeEnum.MULTI_FILE, 10L, 2L, true, List.of());
    }

    @Test
    void incompleteMultiFileStreamPropagatesAnError() {
        AiCodeGeneratorFacade facade = new AiCodeGeneratorFacade();
        Flux<String> incompleteStream = Flux.just(
                "```html\n<html></html>\n```\n",
                "```css\nbody { color: #222; }");

        assertThrows(
                BusinessException.class,
                () -> facade.processCodeStream(
                                incompleteStream, CodeGenTypeEnum.MULTI_FILE, -1L, -1L)
                        .blockLast());
    }

    @Test
    void initialGenerationReportsDegradedResourceSources() {
        AiCodeGeneratorFacade facade = spy(new AiCodeGeneratorFacade());
        ResourceCollectionAgent resourceCollectionAgent = mock(ResourceCollectionAgent.class);
        ResourcePromptAssembler resourcePromptAssembler = mock(ResourcePromptAssembler.class);
        ResourceCollectionResult collectionResult = new ResourceCollectionResult(
                List.of(), List.of(), List.of("logo collection returned no resources"));
        when(resourceCollectionAgent.collectResources("生成商城")).thenReturn(collectionResult);
        when(resourcePromptAssembler.assemble("生成商城", collectionResult)).thenReturn("assembled prompt");
        ReflectionTestUtils.setField(facade, "resourceCollectionAgent", resourceCollectionAgent);
        ReflectionTestUtils.setField(facade, "resourcePromptAssembler", resourcePromptAssembler);
        doReturn(Flux.just("generated"))
                .when(facade)
                .generateAndSaveCodeStreamInternal(
                        "assembled prompt", CodeGenTypeEnum.MULTI_FILE, 11L, 1L, false, List.of());

        List<String> result = facade.generateAndSaveCodeStream(
                        "生成商城", CodeGenTypeEnum.MULTI_FILE, 11L, 1L, false)
                .collectList()
                .block();

        assertEquals(3, result.size());
        assertTrue(result.get(1).contains("1 个资源来源已自动降级"));
        assertEquals("generated", result.get(2));
    }

    @Test
    void resourceCollectionFailureFallsBackToOriginalPrompt() {
        AiCodeGeneratorFacade facade = spy(new AiCodeGeneratorFacade());
        ResourceCollectionAgent resourceCollectionAgent = mock(ResourceCollectionAgent.class);
        ResourcePromptAssembler resourcePromptAssembler = mock(ResourcePromptAssembler.class);
        when(resourceCollectionAgent.collectResources("生成商城"))
                .thenThrow(new IllegalStateException("resource service unavailable"));
        ReflectionTestUtils.setField(facade, "resourceCollectionAgent", resourceCollectionAgent);
        ReflectionTestUtils.setField(facade, "resourcePromptAssembler", resourcePromptAssembler);
        doReturn(Flux.just("generated"))
                .when(facade)
                .generateAndSaveCodeStreamInternal(
                        "生成商城", CodeGenTypeEnum.MULTI_FILE, 12L, 1L, false, List.of());

        List<String> result = facade.generateAndSaveCodeStream(
                        "生成商城", CodeGenTypeEnum.MULTI_FILE, 12L, 1L, false)
                .collectList()
                .block();

        assertEquals(3, result.size());
        assertTrue(result.get(1).contains("资源服务暂时不可用"));
        assertEquals("generated", result.get(2));
        verifyNoInteractions(resourcePromptAssembler);
        verify(facade).generateAndSaveCodeStreamInternal(
                "生成商城", CodeGenTypeEnum.MULTI_FILE, 12L, 1L, false, List.of());
    }

    @Test
    void codeGenerationFailureIsNotMisclassifiedAsResourceCollectionFailure() {
        AiCodeGeneratorFacade facade = spy(new AiCodeGeneratorFacade());
        ResourceCollectionAgent resourceCollectionAgent = mock(ResourceCollectionAgent.class);
        ResourcePromptAssembler resourcePromptAssembler = mock(ResourcePromptAssembler.class);
        ResourceCollectionResult collectionResult = new ResourceCollectionResult(
                List.of(), List.of(), List.of());
        when(resourceCollectionAgent.collectResources("生成商城")).thenReturn(collectionResult);
        when(resourcePromptAssembler.assemble("生成商城", collectionResult)).thenReturn("assembled prompt");
        ReflectionTestUtils.setField(facade, "resourceCollectionAgent", resourceCollectionAgent);
        ReflectionTestUtils.setField(facade, "resourcePromptAssembler", resourcePromptAssembler);
        doReturn(Flux.error(new IllegalStateException("model stream disconnected")))
                .when(facade)
                .generateAndSaveCodeStreamInternal(
                        "assembled prompt", CodeGenTypeEnum.MULTI_FILE, 13L, 1L, false, List.of());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> facade.generateAndSaveCodeStream(
                                "生成商城", CodeGenTypeEnum.MULTI_FILE, 13L, 1L, false)
                        .blockLast());

        assertEquals("model stream disconnected", exception.getMessage());
        verify(facade).generateAndSaveCodeStreamInternal(
                "assembled prompt", CodeGenTypeEnum.MULTI_FILE, 13L, 1L, false, List.of());
        verify(facade, never()).generateAndSaveCodeStreamInternal(
                "生成商城", CodeGenTypeEnum.MULTI_FILE, 13L, 1L, false, List.of());
    }

    @Test
    void retriesTransientConnectionFailureBeforeFirstChunkOnce() {
        AiCodeGeneratorFacade facade = new AiCodeGeneratorFacade();
        AtomicInteger attempts = new AtomicInteger();

        List<String> result = facade.retryInitialConnectionFailure(() -> {
                    if (attempts.incrementAndGet() == 1) {
                        return Flux.error(new ResourceAccessException(
                                "Unexpected end of file from server", new IOException("EOF")));
                    }
                    return Flux.just("complete response");
                })
                .collectList()
                .block();

        assertEquals(List.of("complete response"), result);
        assertEquals(2, attempts.get());
    }

    @Test
    void doesNotRetryAfterAnyResponseChunkWasReceived() {
        AiCodeGeneratorFacade facade = new AiCodeGeneratorFacade();
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(
                ResourceAccessException.class,
                () -> facade.retryInitialConnectionFailure(() -> {
                            attempts.incrementAndGet();
                            return Flux.concat(
                                    Flux.just("partial response"),
                                    Flux.error(new ResourceAccessException(
                                            "stream disconnected", new IOException("EOF"))));
                        })
                        .blockLast());

        assertEquals(1, attempts.get());
    }
}
