package com.sequenceiq.redbeams;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DBStackStatus;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@ExtendWith(MockitoExtension.class)
class RedbeamsFlowInformationTest {

    @Mock
    private Clock clock;

    @Mock
    private DBStackService dbStackService;

    @InjectMocks
    private RedbeamsFlowInformation redbeamsFlowInformation;

    @Test
    void handleFlowFailTest() {
        FlowLog flowLog = mock(FlowLog.class);
        when(flowLog.getResourceId()).thenReturn(1L);
        DBStack stack = new DBStack();
        DBStackStatus dbStackStatus = new DBStackStatus();
        dbStackStatus.setStatus(Status.CREATE_IN_PROGRESS);
        stack.setDBStackStatus(dbStackStatus);
        when(dbStackService.getById(1L)).thenReturn(stack);
        when(clock.getCurrentInstant()).thenReturn(Instant.now());
        redbeamsFlowInformation.handleFlowFail(flowLog);
        ArgumentCaptor<DBStack> stackArgumentCaptor = ArgumentCaptor.forClass(DBStack.class);
        verify(dbStackService, times(1)).save(stackArgumentCaptor.capture());
        assertEquals(Status.CREATE_FAILED, stackArgumentCaptor.getValue().getStatus());
    }

}