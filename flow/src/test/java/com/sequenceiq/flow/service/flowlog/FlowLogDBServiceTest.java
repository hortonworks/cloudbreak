package com.sequenceiq.flow.service.flowlog;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.assertj.core.util.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cedarsoftware.util.io.JsonWriter;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.flow.core.ResourceIdProvider;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;
import com.sequenceiq.flow.repository.FlowLogRepository;

@RunWith(MockitoJUnitRunner.class)
public class FlowLogDBServiceTest {

    private static final String FLOW_ID = "flowId";

    private static final long ID = 1L;

    private static final String CLOUDBREAK_STACK_CRN = Crn.builder()
            .setAccountId("acc")
            .setPartition(Crn.Partition.CDP)
            .setResource("stack")
            .setResourceType(Crn.ResourceType.DATALAKE)
            .setService(Crn.Service.DATALAKE)
            .build().toString();

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private FlowLogDBService underTest;

    @Mock
    private FlowLogRepository flowLogRepository;

    @Mock
    private ResourceIdProvider resourceIdProvider;

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

        when(flowLogRepository.findFirstByFlowIdOrderByCreatedDesc(FLOW_ID)).thenReturn(flowLogOptional);

        Optional<FlowLog> lastFlowLog = underTest.getLastFlowLog(FLOW_ID);
        assertEquals(flowLogOptional, lastFlowLog);
    }

    @Test
    public void updateLastFlowLogPayload() {
        FlowLog flowLog = new FlowLog();
        flowLog.setId(ID);

        Payload payload = mock(Selectable.class);
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

    @Test
    public void testGetResourceIdIfTheInputIsCrn() {
        when(resourceIdProvider.getResourceIdByResourceCrn(anyString())).thenReturn(1L);

        underTest.getResourceIdByCrnOrName(CLOUDBREAK_STACK_CRN);

        verify(resourceIdProvider, times(1)).getResourceIdByResourceCrn(anyString());
        verify(resourceIdProvider, never()).getResourceIdByResourceName(anyString());
    }

    @Test
    public void testGetResourceIdIfTheInputIsNotCrn() {
        when(resourceIdProvider.getResourceIdByResourceName(anyString())).thenReturn(1L);

        underTest.getResourceIdByCrnOrName("stackName");

        verify(resourceIdProvider, times(1)).getResourceIdByResourceName(anyString());
        verify(resourceIdProvider, never()).getResourceIdByResourceCrn(anyString());
    }

    @Test
    public void testGetFlowLogs() {
        when(resourceIdProvider.getResourceIdByResourceCrn(anyString())).thenReturn(1L);
        when(flowLogRepository.findAllByResourceIdOrderByCreatedDesc(anyLong())).thenReturn(Lists.newArrayList(new FlowLog()));

        assertEquals(1, underTest.getFlowLogsByResourceCrnOrName(CLOUDBREAK_STACK_CRN).size());

        verify(resourceIdProvider, times(1)).getResourceIdByResourceCrn(anyString());
        verify(resourceIdProvider, never()).getResourceIdByResourceName(anyString());
    }

    @Test
    public void testGetLastFlowLogWhenThereIsNoFlow() {
        when(resourceIdProvider.getResourceIdByResourceCrn(anyString())).thenReturn(1L);
        when(flowLogRepository.findAllByResourceIdOrderByCreatedDesc(anyLong())).thenReturn(Lists.newArrayList());

        thrown.expect(NotFoundException.class);
        thrown.expectMessage("Flow log for resource not found!");

        underTest.getLastFlowLogByResourceCrnOrName(CLOUDBREAK_STACK_CRN);
    }

    @Test
    public void testGetLastFlowLog() {
        when(resourceIdProvider.getResourceIdByResourceCrn(anyString())).thenReturn(1L);
        when(flowLogRepository.findAllByResourceIdOrderByCreatedDesc(anyLong())).thenReturn(Lists.newArrayList(createFlowLog("1"), createFlowLog("2")));

        assertEquals("1", underTest.getLastFlowLogByResourceCrnOrName(CLOUDBREAK_STACK_CRN).getFlowId());

        verify(resourceIdProvider, times(1)).getResourceIdByResourceCrn(anyString());
        verify(resourceIdProvider, never()).getResourceIdByResourceName(anyString());
    }

    private FlowLog createFlowLog(String flowId) {
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowId(flowId);
        return flowLog;
    }
}