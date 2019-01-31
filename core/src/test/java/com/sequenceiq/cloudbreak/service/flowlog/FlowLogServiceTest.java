package com.sequenceiq.cloudbreak.service.flowlog;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.reflect.internal.WhiteboxImpl;

import com.sequenceiq.cloudbreak.domain.FlowLog;
import com.sequenceiq.cloudbreak.domain.StateStatus;
import com.sequenceiq.cloudbreak.repository.FlowLogRepository;

@RunWith(MockitoJUnitRunner.class)
public class FlowLogServiceTest {

    private static final String FLOW_ID = "flowId";

    private static final long ID = 1L;

    @InjectMocks
    private FlowLogService underTest;

    @Mock
    private FlowLogRepository flowLogRepository;

    @Test
    public void updateLastFlowLogStatus() throws Exception {
        runUpdateLastFlowLogStatusTest(false, StateStatus.SUCCESSFUL);
    }

    @Test
    public void updateLastFlowLogStatusFailure() throws Exception {
        runUpdateLastFlowLogStatusTest(true, StateStatus.FAILED);
    }

    private void runUpdateLastFlowLogStatusTest(boolean failureEvent, StateStatus successful) throws Exception {
        FlowLog flowLog = new FlowLog();
        flowLog.setId(ID);
        when(flowLogRepository.findFirstByFlowIdOrderByCreatedDesc(FLOW_ID)).thenReturn(Optional.of(flowLog));

        WhiteboxImpl.invokeMethod(underTest, "updateLastFlowLogStatus", FLOW_ID, failureEvent);

        verify(flowLogRepository, times(1)).updateLastLogStatusInFlow(ID, successful);
    }
}