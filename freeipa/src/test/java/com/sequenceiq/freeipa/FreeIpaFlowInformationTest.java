package com.sequenceiq.freeipa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class FreeIpaFlowInformationTest {

    @Mock
    private StackService stackService;

    @InjectMocks
    private FreeIpaFlowInformation freeIpaFlowInformation;

    @Test
    void handleFlowFailTest() {
        FlowLog flowLog = mock(FlowLog.class);
        when(flowLog.getResourceId()).thenReturn(1L);
        Stack stack = new Stack();
        StackStatus stackStatus = mock(StackStatus.class);
        when(stackStatus.getStatus()).thenReturn(Status.UPGRADE_CCM_IN_PROGRESS);
        stack.setStackStatus(stackStatus);
        when(stackService.getStackById(1L)).thenReturn(stack);
        freeIpaFlowInformation.handleFlowFail(flowLog);
        ArgumentCaptor<Stack> stackArgumentCaptor = ArgumentCaptor.forClass(Stack.class);
        verify(stackService, times(1)).save(stackArgumentCaptor.capture());
        assertEquals(Status.UPGRADE_CCM_FAILED, stackArgumentCaptor.getValue().getStackStatus().getStatus());
    }

}