package com.sequenceiq.flow.service.flowlog;

import static com.sequenceiq.flow.core.FlowConstants.FINISHED_STATE;
import static com.sequenceiq.flow.core.FlowConstants.TERMINATED_STATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.json.TypedJsonUtil;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.flow.api.model.operation.OperationType;
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
import com.sequenceiq.flow.domain.FlowLogWithoutPayload;
import com.sequenceiq.flow.domain.StateStatus;
import com.sequenceiq.flow.ha.NodeConfig;
import com.sequenceiq.flow.repository.FlowLogRepository;

@ExtendWith(MockitoExtension.class)
class FlowLogDBServiceTest {

    private static final String FLOW_ID = "flowId";

    private static final long ID = 123L;

    private static final Long DATABASE_ID = 234L;

    private static final String NODE_ID = "node1";

    private static final String CLOUDBREAK_STACK_CRN = CrnTestUtil.getDatalakeCrnBuilder()
            .setAccountId("acc")
            .setResource("stack")
            .build().toString();

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

    @Captor
    private ArgumentCaptor<FlowLog> savedFlowLogCaptor;

    @Test
    void updateLastFlowLogStatus() {
        runUpdateLastFlowLogStatusTest(false, StateStatus.SUCCESSFUL);
    }

    @Test
    void updateLastFlowLogStatusFailure() {
        runUpdateLastFlowLogStatusTest(true, StateStatus.FAILED);
    }

    private void runUpdateLastFlowLogStatusTest(boolean failureEvent, StateStatus successful) {
        FlowLog flowLog = new FlowLog();
        flowLog.setId(ID);

        underTest.updateLastFlowLogStatus(flowLog, failureEvent);

        verify(flowLogRepository, times(1)).updateLastLogStatusInFlow(ID, successful);
    }

    @Test
    void getLastFlowLog() {
        FlowLogWithoutPayload flowLog = mock(FlowLogWithoutPayload.class);
        Page<FlowLogWithoutPayload> flowLogOptional = new PageImpl<>(List.of(flowLog));

        when(flowLogRepository.findByFlowIdOrderByCreatedDesc(FLOW_ID, Pageable.ofSize(1))).thenReturn(flowLogOptional);

        Optional<FlowLogWithoutPayload> lastFlowLog = underTest.getLastFlowLog(FLOW_ID);
        assertEquals(flowLogOptional.stream().findFirst(), lastFlowLog);
    }

    @Test
    void updateLastFlowLogPayload() {
        FlowLog flowLog = new FlowLog();
        flowLog.setId(ID);

        Payload payload = new TestSelectable();
        Map<Object, Object> variables = Map.of("repeated", 2);

        underTest.updateLastFlowLogPayload(flowLog, payload, variables);

        ArgumentCaptor<FlowLog> flowLogCaptor = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, times(1)).save(flowLogCaptor.capture());

        FlowLog savedFlowLog = flowLogCaptor.getValue();
        assertEquals(flowLog.getId(), savedFlowLog.getId());

        String payloadJackson = JsonUtil.writeValueAsStringSilent(payload);
        String variablesJackson = TypedJsonUtil.writeValueAsStringSilent(variables);
        assertNull(savedFlowLog.getPayload());
        assertEquals("{}", savedFlowLog.getVariables());
        assertEquals(payloadJackson, savedFlowLog.getPayloadJackson());
        assertEquals(variablesJackson, savedFlowLog.getVariablesJackson());
    }

    @Test
    void testGetResourceIdIfTheInputIsCrn() {
        when(resourceIdProvider.getResourceIdByResourceCrn(anyString())).thenReturn(1L);

        underTest.getResourceIdByCrnOrName(CLOUDBREAK_STACK_CRN);

        verify(resourceIdProvider, times(1)).getResourceIdByResourceCrn(anyString());
        verify(resourceIdProvider, never()).getResourceIdByResourceName(anyString());
    }

    @Test
    void testGetResourceIdIfTheInputIsNotCrn() {
        when(resourceIdProvider.getResourceIdByResourceName(anyString())).thenReturn(1L);

        underTest.getResourceIdByCrnOrName("stackName");

        verify(resourceIdProvider, times(1)).getResourceIdByResourceName(anyString());
        verify(resourceIdProvider, never()).getResourceIdByResourceCrn(anyString());
    }

    @Test
    void testGetFlowLogs() {
        when(resourceIdProvider.getResourceIdByResourceCrn(anyString())).thenReturn(1L);
        when(flowLogRepository.findFirstByResourceIdOrderByCreatedDesc(anyLong())).thenReturn(Optional.of(createFlowLog("1")));
        when(flowLogRepository.findAllByFlowIdOrderByCreatedDesc(anyString())).thenReturn(Lists.newArrayList(new FlowLog()));

        assertEquals(1, underTest.getFlowLogsByResourceCrnOrName(CLOUDBREAK_STACK_CRN).size());

        verify(resourceIdProvider, times(1)).getResourceIdByResourceCrn(anyString());
        verify(resourceIdProvider, never()).getResourceIdByResourceName(anyString());
    }

    @Test
    void testGetFlowLogsWithChainId() {
        when(flowLogRepository.findAllByFlowIdsCreatedDesc(any())).thenReturn(List.of(createFlowLog("flow")));

        assertEquals(1, underTest.getFlowLogsByFlowIdsCreatedDesc(Set.of("flowchain")).size());
    }

    @Test
    void testGetLastFlowLogWhenThereIsNoFlow() {
        when(resourceIdProvider.getResourceIdByResourceCrn(anyString())).thenReturn(1L);

        assertThatThrownBy(() -> underTest.getLastFlowLogByResourceCrnOrName(CLOUDBREAK_STACK_CRN))
                .hasMessage("Flow log for resource not found!")
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testGetLastFlowLog() {
        when(resourceIdProvider.getResourceIdByResourceCrn(anyString())).thenReturn(1L);
        when(flowLogRepository.findFirstByResourceIdOrderByCreatedDesc(anyLong())).thenReturn(Optional.of(createFlowLog("1")));
        when(flowLogRepository.findAllByFlowIdOrderByCreatedDesc(anyString())).thenReturn(Lists.newArrayList(createFlowLog("1"), createFlowLog("2")));

        assertEquals("1", underTest.getLastFlowLogByResourceCrnOrName(CLOUDBREAK_STACK_CRN).getFlowId());

        verify(resourceIdProvider, times(1)).getResourceIdByResourceCrn(anyString());
        verify(resourceIdProvider, never()).getResourceIdByResourceName(anyString());
    }

    @Test
    void cancelTooOldTerminationFlowForResourceTest() throws TransactionService.TransactionExecutionException {
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
        when(nodeConfig.getId()).thenReturn(NODE_ID);
        underTest.cancelTooOldTerminationFlowForResource(1L, 10000L);
        verify(flowLogRepository).finalizeByFlowId(eq("flow1"));
        verify(flowLogRepository, times(0)).finalizeByFlowId(eq("flow2"));
        verify(flowLogRepository).updateLastLogStatusInFlow(eq(10L), eq(StateStatus.SUCCESSFUL));
    }

    @Test
    void doNotCancelTooYoungTerminationFlowForResourceTest() {
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
    void testNoPendingFlowEvent() {
        Boolean actual = underTest.hasPendingFlowEvent(Lists.newArrayList(createFlowLog(false, "1"), createFlowLog(false, "2")));
        assertEquals(Boolean.FALSE, actual);
    }

    @Test
    void testHasPendingFlowEvent() {
        Boolean actual = underTest.hasPendingFlowEvent(Lists.newArrayList(createFlowLog(true, "1"), createFlowLog(false, "2")));
        assertEquals(Boolean.TRUE, actual);
    }

    @Test
    void testTerminate() throws TransactionExecutionException {
        prepareFinalization();
        FlowLog flowLog = underTest.terminate(ID, FLOW_ID);

        verifyFinalization();
        verify(applicationFlowInformation).handleFlowFail(flowLog);
        FlowLog savedFlowLog = savedFlowLogCaptor.getValue();
        assertThat(savedFlowLog.getResourceId()).isEqualTo(ID);
        assertThat(savedFlowLog.getFlowId()).isEqualTo(FLOW_ID);
        assertThat(savedFlowLog.getCurrentState()).isEqualTo(TERMINATED_STATE);
        assertThat(savedFlowLog.getFinalized()).isTrue();
        assertThat(savedFlowLog.getStateStatus()).isEqualTo(StateStatus.SUCCESSFUL);
        assertThat(savedFlowLog.getOperationType()).isEqualTo(OperationType.DIAGNOSTICS);
        assertThat(savedFlowLog.getCloudbreakNodeId()).isEqualTo(NODE_ID);
        assertThat(savedFlowLog.getVariables()).isNull();
        assertThat(savedFlowLog.getVariablesJackson()).isNull();
    }

    @Test
    void testClose() throws TransactionExecutionException {
        prepareFinalization();
        Map<Object, Object> params = Map.of("param1", StateStatus.SUCCESSFUL, "param2", 234L, "param3", "true");
        underTest.close(ID, FLOW_ID, false, params);

        verifyFinalization();
        FlowLog savedFlowLog = savedFlowLogCaptor.getValue();
        assertThat(savedFlowLog.getResourceId()).isEqualTo(ID);
        assertThat(savedFlowLog.getFlowId()).isEqualTo(FLOW_ID);
        assertThat(savedFlowLog.getCurrentState()).isEqualTo(FINISHED_STATE);
        assertThat(savedFlowLog.getFinalized()).isTrue();
        assertThat(savedFlowLog.getStateStatus()).isEqualTo(StateStatus.SUCCESSFUL);
        assertThat(savedFlowLog.getOperationType()).isEqualTo(OperationType.DIAGNOSTICS);
        assertThat(savedFlowLog.getCloudbreakNodeId()).isEqualTo(NODE_ID);
        assertThat(savedFlowLog.getVariables()).isEqualTo(null);
        assertThat(savedFlowLog.getVariablesJackson()).isEqualTo(TypedJsonUtil.writeValueAsStringSilent(params));
    }

    private void prepareFinalization() throws TransactionExecutionException {
        when(transactionService.required(any(Supplier.class))).thenAnswer(invocation -> ((Supplier) invocation.getArguments()[0]).get());
        FlowLog lastFlowLog = new FlowLog(ID, FLOW_ID, "currentState", false, StateStatus.SUCCESSFUL, OperationType.DIAGNOSTICS);
        lastFlowLog.setId(DATABASE_ID);
        when(flowLogRepository.findFirstByFlowIdOrderByCreatedDesc(FLOW_ID)).thenReturn(Optional.of(lastFlowLog));
        when(nodeConfig.getId()).thenReturn(NODE_ID);
    }

    private void verifyFinalization() {
        verify(flowLogRepository).finalizeByFlowId(FLOW_ID);
        verify(flowLogRepository).updateLastLogStatusInFlow(DATABASE_ID, StateStatus.SUCCESSFUL);
        verify(flowLogRepository).save(savedFlowLogCaptor.capture());
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

    @Entity
    public static class TestEntity {
        @Id
        private String name;

        @OneToOne
        private TestClass testClass;

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public TestClass getTestClass() {
            return testClass;
        }

        public void setTestClass(TestClass testClass) {
            this.testClass = testClass;
        }
    }

    @Entity
    public static class TestClass {

        @Id
        private String value;

        @OneToOne
        private TestEntity entity;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public void setEntity(TestEntity entity) {
            this.entity = entity;
        }

        public TestEntity getEntity() {
            return entity;
        }
    }

    private static class TestSelectable implements Selectable {

        @Override
        public String selector() {
            return "selector";
        }

        @Override
        public Long getResourceId() {
            return ID;
        }
    }
}
