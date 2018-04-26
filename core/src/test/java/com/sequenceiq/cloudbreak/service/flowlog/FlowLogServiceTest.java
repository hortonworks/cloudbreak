package com.sequenceiq.cloudbreak.service.flowlog;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
    public void updateLastFlowLogStatus() {
        runUpdateLastFlowLogStatusTest(false, StateStatus.SUCCESSFUL);
    }

    private void runUpdateLastFlowLogStatusTest(boolean failureEvent, StateStatus successful) {
        FlowLog flowLog = new FlowLog();
        flowLog.setId(ID);
        when(flowLogRepository.findFirstByFlowIdOrderByCreatedDesc(FLOW_ID)).thenReturn(flowLog);

        underTest.updateLastFlowLogStatus(FLOW_ID, failureEvent);

        verify(flowLogRepository, times(1)).updateLastLogStatusInFlow(ID, successful);
    }

    @Test
    public void updateLastFlowLogStatusFailure() {
        runUpdateLastFlowLogStatusTest(true, StateStatus.FAILED);
    }
}