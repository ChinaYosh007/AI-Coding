package com.yosh.coding.core;

import com.yosh.coding.agent.subagent.resource.ResourceCollectionAgent;
import com.yosh.exception.BusinessException;
import com.yosh.model.enums.CodeGenTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
}
