package com.sequenceiq.cloudbreak.service.flowlog;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cedarsoftware.util.io.JsonWriter;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageRequest;
import com.sequenceiq.cloudbreak.domain.FlowLog;
import com.sequenceiq.cloudbreak.domain.StateStatus;
import com.sequenceiq.cloudbreak.repository.FlowLogRepository;

@RunWith(MockitoJUnitRunner.class)
public class FlowLogDBServiceTest {

    private static final String FLOW_ID = "flowId";

    private static final long ID = 1L;

    @InjectMocks
    private FlowLogDBService underTest;

    @Mock
    private FlowLogRepository flowLogRepository;

    @Test
    public void updateLastFlowLogStatus() {
        runUpdateLastFlowLogStatusTest(false, StateStatus.SUCCESSFUL);
    }

    @Test
    public void updateLastFlowLogStatusFailure() {
        runUpdateLastFlowLogStatusTest(true, StateStatus.FAILED);
    }

    private void runUpdateLastFlowLogStatusTest(boolean failureEvent, StateStatus successful) {
        FlowLog flowLog = new FlowLog();
        flowLog.setId(ID);

        underTest.updateLastFlowLogStatus(flowLog, failureEvent);

        verify(flowLogRepository, times(1)).updateLastLogStatusInFlow(ID, successful);
    }

    @Test
    public void getLastFlowLog() {
        FlowLog flowLog = new FlowLog();
        flowLog.setId(ID);
        Optional<FlowLog> flowLogOptional = Optional.of(flowLog);

        Mockito.when(flowLogRepository.findFirstByFlowIdOrderByCreatedDesc(FLOW_ID)).thenReturn(flowLogOptional);

        Optional<FlowLog> lastFlowLog = underTest.getLastFlowLog(FLOW_ID);
        assertEquals(flowLogOptional, lastFlowLog);
    }

    @Test
    public void updateLastFlowLogPayload() {
        FlowLog flowLog = new FlowLog();
        flowLog.setId(ID);

        Payload payload = new CheckImageRequest<>(null, null, null, null);
        Map<Object, Object> variables = Map.of("repeated", 2);

        underTest.updateLastFlowLogPayload(flowLog, payload, variables);

        ArgumentCaptor<FlowLog> flowLogCaptor = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, times(1)).save(flowLogCaptor.capture());

        FlowLog savedFlowLog = flowLogCaptor.getValue();
        assertEquals(flowLog.getId(), savedFlowLog.getId());

        String payloadJson = JsonWriter.objectToJson(payload, Map.of());
        String variablesJson = JsonWriter.objectToJson(variables, Map.of());
        assertEquals(payloadJson, savedFlowLog.getPayload());
        assertEquals(variablesJson, savedFlowLog.getVariables());
    }
}