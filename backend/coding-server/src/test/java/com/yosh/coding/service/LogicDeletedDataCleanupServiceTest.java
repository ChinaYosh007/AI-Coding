package com.yosh.coding.service;

import com.yosh.coding.mapper.AppCollaborationMapper;
import com.yosh.coding.mapper.AppMapper;
import com.yosh.coding.mapper.AppVersionMapper;
import com.yosh.coding.mapper.ChatHistoryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogicDeletedDataCleanupServiceTest {

    @Mock
    private AppMapper appMapper;
    @Mock
    private ChatHistoryMapper chatHistoryMapper;
    @Mock
    private AppVersionMapper appVersionMapper;
    @Mock
    private AppCollaborationMapper appCollaborationMapper;

    @Test
    void shouldDeleteParentAfterAllDependentRecords() {
        LogicDeletedDataCleanupService service = createService();
        when(appMapper.physicalDeleteLogicDeletedById(1L)).thenReturn(1);

        service.physicalDeleteAppData(1L);

        InOrder inOrder = inOrder(chatHistoryMapper, appCollaborationMapper, appVersionMapper, appMapper);
        inOrder.verify(chatHistoryMapper).physicalDeleteByAppId(1L);
        inOrder.verify(appCollaborationMapper).physicalDeleteByAppId(1L);
        inOrder.verify(appVersionMapper).physicalDeleteByAppId(1L);
        inOrder.verify(appMapper).physicalDeleteLogicDeletedById(1L);
    }

    @Test
    void shouldRejectParentThatIsNotLogicDeleted() {
        LogicDeletedDataCleanupService service = createService();
        when(appMapper.physicalDeleteLogicDeletedById(1L)).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> service.physicalDeleteAppData(1L));
    }

    private LogicDeletedDataCleanupService createService() {
        return new LogicDeletedDataCleanupService(
                appMapper,
                chatHistoryMapper,
                appVersionMapper,
                appCollaborationMapper
        );
    }
}
