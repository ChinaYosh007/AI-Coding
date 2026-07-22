package com.yosh.coding.core;

import com.yosh.coding.agent.subagent.resource.ResourceCollectionAgent;
import com.yosh.coding.agent.model.image.query.ResourceCollectionResult;
import com.yosh.exception.BusinessException;
import com.yosh.model.enums.CodeGenTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import java.util.List;

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
}
