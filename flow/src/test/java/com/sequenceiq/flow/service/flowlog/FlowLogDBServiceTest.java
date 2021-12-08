package com.sequenceiq.flow.service.flowlog;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

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
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.ResourceIdProvider;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;
import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.FlowLogIdWithTypeAndTimestamp;
import com.sequenceiq.flow.domain.StateStatus;
import com.sequenceiq.flow.ha.NodeConfig;
import com.sequenceiq.flow.repository.FlowLogRepository;

@RunWith(MockitoJUnitRunner.class)
public class FlowLogDBServiceTest {

    private static final String FLOW_ID = "flowId";

    private static final long ID = 1L;

    private static final String CLOUDBREAK_STACK_CRN = CrnTestUtil.getDatalakeCrnBuilder()
            .setAccountId("acc")
            .setResource("stack")
            .build().toString();

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private FlowLogDBService underTest;

    @Mock
    private FlowLogRepository flowLogRepository;

    @Mock
    private ResourceIdProvider resourceIdProvider;

    @Mock
    private ApplicationFlowInformation applicationFlowInformation;

    @Mock
    private TransactionService transactionService;

    @Mock
    private NodeConfig nodeConfig;

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
        when(flowLogRepository.findFirstByResourceIdOrderByCreatedDesc(anyLong())).thenReturn(Optional.of(createFlowLog("1")));
        when(flowLogRepository.findAllByFlowIdOrderByCreatedDesc(anyString())).thenReturn(Lists.newArrayList(new FlowLog()));

        assertEquals(1, underTest.getFlowLogsByResourceCrnOrName(CLOUDBREAK_STACK_CRN).size());

        verify(resourceIdProvider, times(1)).getResourceIdByResourceCrn(anyString());
        verify(resourceIdProvider, never()).getResourceIdByResourceName(anyString());
    }

    @Test
    public void testGetFlowLogsWithChainId() {
        when(flowLogRepository.findAllByFlowIdsCreatedDesc(any())).thenReturn(List.of(createFlowLog("flow")));

        assertEquals(1, underTest.getFlowLogsByFlowIdsCreatedDesc(Set.of("flowchain")).size());
    }

    @Test
    public void testGetLastFlowLogWhenThereIsNoFlow() {
        when(resourceIdProvider.getResourceIdByResourceCrn(anyString())).thenReturn(1L);

        thrown.expect(NotFoundException.class);
        thrown.expectMessage("Flow log for resource not found!");

        underTest.getLastFlowLogByResourceCrnOrName(CLOUDBREAK_STACK_CRN);
    }

    @Test
    public void testGetLastFlowLog() {
        when(resourceIdProvider.getResourceIdByResourceCrn(anyString())).thenReturn(1L);
        when(flowLogRepository.findFirstByResourceIdOrderByCreatedDesc(anyLong())).thenReturn(Optional.of(createFlowLog("1")));
        when(flowLogRepository.findAllByFlowIdOrderByCreatedDesc(anyString())).thenReturn(Lists.newArrayList(createFlowLog("1"), createFlowLog("2")));

        assertEquals("1", underTest.getLastFlowLogByResourceCrnOrName(CLOUDBREAK_STACK_CRN).getFlowId());

        verify(resourceIdProvider, times(1)).getResourceIdByResourceCrn(anyString());
        verify(resourceIdProvider, never()).getResourceIdByResourceName(anyString());
    }

    @Test
    public void cancelTooOldTerminationFlowForResourceTest() throws TransactionService.TransactionExecutionException {
        Set<FlowLogIdWithTypeAndTimestamp> flowLogs = new LinkedHashSet<>();
        FlowLogIdWithTypeAndTimestamp flowLog2 = mock(FlowLogIdWithTypeAndTimestamp.class);
        when(flowLog2.getFlowType()).thenReturn(ClassValue.of(Class.class));
        flowLogs.add(flowLog2);
        FlowLogIdWithTypeAndTimestamp flowLog1 = mock(FlowLogIdWithTypeAndTimestamp.class);
        when(flowLog1.getFlowType()).thenReturn(ClassValue.of(TerminationFlowConfig.class));
        when(flowLog1.getCreated()).thenReturn(9000L);
        when(flowLog1.getFlowId()).thenReturn("flow1");
        flowLogs.add(flowLog1);
        when(flowLogRepository.findAllRunningFlowLogByResourceId(eq(1L))).thenReturn(flowLogs);
        FlowLog realFlowLog1 = mock(FlowLog.class);
        when(realFlowLog1.getId()).thenReturn(10L);
        when(flowLogRepository.findFirstByFlowIdOrderByCreatedDesc(eq("flow1"))).thenReturn(Optional.of(realFlowLog1));
        when(applicationFlowInformation.getTerminationFlow()).thenReturn(Collections.singletonList(TerminationFlowConfig.class));
        when(transactionService.required(any(Supplier.class))).thenAnswer(invocation -> ((Supplier) invocation.getArguments()[0]).get());
        when(nodeConfig.getId()).thenReturn("node1");
        underTest.cancelTooOldTerminationFlowForResource(1L, 10000L);
        verify(flowLogRepository).finalizeByFlowId(eq("flow1"));
        verify(flowLogRepository, times(0)).finalizeByFlowId(eq("flow2"));
        verify(flowLogRepository).updateLastLogStatusInFlow(eq(10L), eq(StateStatus.SUCCESSFUL));
    }

    @Test
    public void doNotCancelTooYoungTerminationFlowForResourceTest() {
        Set<FlowLogIdWithTypeAndTimestamp> flowLogs = new HashSet<>();
        FlowLogIdWithTypeAndTimestamp flowLog1 = mock(FlowLogIdWithTypeAndTimestamp.class);
        when(flowLog1.getFlowType()).thenReturn(ClassValue.of(TerminationFlowConfig.class));
        when(flowLog1.getCreated()).thenReturn(11000L);
        flowLogs.add(flowLog1);
        FlowLogIdWithTypeAndTimestamp flowLog2 = mock(FlowLogIdWithTypeAndTimestamp.class);
        when(flowLog2.getFlowType()).thenReturn(ClassValue.of(Class.class));
        flowLogs.add(flowLog2);
        when(flowLogRepository.findAllRunningFlowLogByResourceId(eq(1L))).thenReturn(flowLogs);
        when(applicationFlowInformation.getTerminationFlow()).thenReturn(Collections.singletonList(TerminationFlowConfig.class));
        underTest.cancelTooOldTerminationFlowForResource(1L, 10000L);
        verify(flowLogRepository, times(0)).finalizeByFlowId(eq("flow1"));
        verify(flowLogRepository, times(0)).finalizeByFlowId(eq("flow2"));
        verify(flowLogRepository, times(0)).updateLastLogStatusInFlow(eq(10L), eq(StateStatus.SUCCESSFUL));
    }

    @Test
    public void testNoPendingFlowEvent() {
        Boolean actual = underTest.hasPendingFlowEvent(Lists.newArrayList(createFlowLog(false, "1"), createFlowLog(false, "2")));
        assertEquals(Boolean.FALSE, actual);
    }

    @Test
    public void testHasPendingFlowEvent() {
        Boolean actual = underTest.hasPendingFlowEvent(Lists.newArrayList(createFlowLog(true, "1"), createFlowLog(false, "2")));
        assertEquals(Boolean.TRUE, actual);
    }

    private FlowLog createFlowLog(boolean pending, String flowId) {
        FlowLog flowLog = createFlowLog(flowId);
        flowLog.setFinalized(!pending);
        flowLog.setStateStatus(pending ? StateStatus.PENDING : StateStatus.SUCCESSFUL);
        return flowLog;
    }

    private FlowLog createFlowLog(String flowId) {
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowId(flowId);
        flowLog.setFlowChainId(flowId + "chain");
        return flowLog;
    }

    public static class TerminationFlowConfig extends AbstractFlowConfiguration<MockFlowState, MockFlowEvent> {
        protected TerminationFlowConfig(Class<MockFlowState> stateType, Class<MockFlowEvent> eventType) {
            super(stateType, eventType);
        }

        @Override
        protected List<Transition<MockFlowState, MockFlowEvent>> getTransitions() {
            return null;
        }

        @Override
        protected FlowEdgeConfig<MockFlowState, MockFlowEvent> getEdgeConfig() {
            return null;
        }

        @Override
        public MockFlowEvent[] getEvents() {
            return new MockFlowEvent[0];
        }

        @Override
        public MockFlowEvent[] getInitEvents() {
            return new MockFlowEvent[0];
        }

        @Override
        public String getDisplayName() {
            return null;
        }
    }

    public static class MockFlowState implements FlowState {

        @Override
        public String name() {
            return null;
        }

        @Override
        public Class<? extends RestartAction> restartAction() {
            return DefaultRestartAction.class;
        }
    }

    public static class MockFlowEvent implements FlowEvent {
        @Override
        public String name() {
            return null;
        }

        @Override
        public String event() {
            return null;
        }
    }
}