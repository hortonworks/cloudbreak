package com.sequenceiq.cloudbreak.core.flow2;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.access.StateMachineAccess;
import org.springframework.statemachine.access.StateMachineAccessor;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.test.util.ReflectionTestUtils;

import com.cedarsoftware.util.io.JsonWriter;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainHandler;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChains;
import com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.config.FlowConfiguration;
import com.sequenceiq.cloudbreak.core.flow2.restart.DefaultRestartAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartFlowConfig;
import com.sequenceiq.cloudbreak.domain.FlowLog;
import com.sequenceiq.cloudbreak.domain.StateStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.repository.FlowLogRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import reactor.bus.Event;
import reactor.bus.Event.Headers;

@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
public class Flow2HandlerTest {

    private static final String FLOW_ID = "flowId";

    private static final String FLOW_CHAIN_ID = "flowChainId";

    private static final Long STACK_ID = 1L;

    private static final String NEXT_EVENT = "NEXT_EVENT";

    @InjectMocks
    private Flow2Handler underTest;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private FlowLogRepository flowLogRepository;

    @Mock
    private Map<String, FlowConfiguration<?>> flowConfigurationMap;

    @Mock
    private List<String> failHandledEvents;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private FlowConfiguration<?> flowConfig;

    @Mock
    private FlowChains flowChains;

    @Mock
    private FlowTriggerCondition flowTriggerCondition;

    @Mock
    private Flow flow;

    @Mock
    private StateMachineFactory<? extends FlowState, ? extends FlowEvent> stateMachineFactory;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private StateMachine<? extends FlowState, ? extends FlowEvent> stateMachine;

    @Mock
    private StateMachineAccessor<? extends FlowState, ? extends FlowEvent> stateMachineAccessor;

    @Mock
    private StateMachineAccess<? extends FlowState, ? extends FlowEvent> stateMachineAccess;

    @Mock
    private ExtendedState extendedState;

    @Mock
    private DefaultRestartAction defaultRestartAction;

    @Mock
    private FlowChainHandler flowChainHandler;

    @Mock
    private TransactionService transactionService;

    @Mock
    private ClusterService clusterService;

    private FlowState flowState;

    private Event<? extends Payload> dummyEvent;

    private final Payload payload = () -> 1L;

    @Before
    public void setUp() throws TransactionExecutionException {
        underTest = new Flow2Handler();
        MockitoAnnotations.initMocks(this);
        Map<String, Object> headers = new HashMap<>();
        headers.put(Flow2Handler.FLOW_ID, FLOW_ID);
        dummyEvent = new Event<>(new Headers(headers), payload);
        flowState = new OwnFlowState();
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any());
    }

    @Test
    public void testNewFlow() {
        BDDMockito.<FlowConfiguration<?>>given(flowConfigurationMap.get(any())).willReturn(flowConfig);
        given(flowConfig.createFlow(anyString(), anyLong())).willReturn(flow);
        given(flowConfig.getFlowTriggerCondition()).willReturn(flowTriggerCondition);
        given(flowTriggerCondition.isFlowTriggerable(anyLong())).willReturn(true);
        Cluster cluster = new Cluster();
        cluster.setStatus(Status.AVAILABLE);
        given(clusterService.findOneByStackId(anyLong())).willReturn(cluster);
        given(flow.getCurrentState()).willReturn(flowState);
        Event<Payload> event = new Event<>(payload);
        event.setKey("KEY");
        underTest.accept(event);
        verify(flowConfigurationMap, times(1)).get(anyString());
        verify(runningFlows, times(1)).put(eq(flow), isNull(String.class));
        verify(flowLogService, times(1))
                .save(anyString(), nullable(String.class), eq("KEY"), any(Payload.class), any(), eq(flowConfig.getClass()), eq(flowState));
        verify(flow, times(1)).sendEvent(anyString(), any());
    }

    @Test
    public void testNewSyncFlowMaintenanceActive() {
        ClusterSyncFlowConfig syncFlowConfig = Mockito.mock(ClusterSyncFlowConfig.class);
        given(syncFlowConfig.getFlowTriggerCondition()).willReturn(new DefaultFlowTriggerCondition());

        BDDMockito.<FlowConfiguration<?>>given(flowConfigurationMap.get(any())).willReturn(syncFlowConfig);
        given(syncFlowConfig.createFlow(anyString(), anyLong())).willReturn(flow);
        given(syncFlowConfig.getFlowTriggerCondition()).willReturn(flowTriggerCondition);
        given(flowTriggerCondition.isFlowTriggerable(anyLong())).willReturn(true);
        Cluster cluster = new Cluster();
        cluster.setStatus(Status.MAINTENANCE_MODE_ENABLED);
        given(clusterService.findOneByStackId(anyLong())).willReturn(cluster);
        given(flow.getCurrentState()).willReturn(flowState);
        Event<Payload> event = new Event<>(payload);
        event.setKey("KEY");
        underTest.accept(event);
        verify(flowConfigurationMap, times(1)).get(anyString());
        verify(runningFlows, times(1)).put(eq(flow), isNull(String.class));
        verify(flowLogService, times(1))
                .save(anyString(), nullable(String.class), eq("KEY"), any(Payload.class), any(), eq(syncFlowConfig.getClass()), eq(flowState));
        verify(flow, times(1)).sendEvent(anyString(), any());
    }

    @Test
    public void testNewFlowButNotHandled() {
        Event<Payload> event = new Event<>(payload);
        event.setKey("KEY");
        underTest.accept(event);
        verify(flowConfigurationMap, times(1)).get(anyString());
        verify(runningFlows, times(0)).put(any(Flow.class), isNull(String.class));
        verify(flowLogService, times(0)).save(anyString(), anyString(), anyString(), any(Payload.class), anyMap(), any(), any(FlowState.class));
    }

    @Test
    public void testExistingFlow() {
        BDDMockito.<FlowConfiguration<?>>given(flowConfigurationMap.get(any())).willReturn(flowConfig);
        given(runningFlows.get(anyString())).willReturn(flow);
        given(flow.getCurrentState()).willReturn(flowState);
        given(flow.getFlowId()).willReturn(FLOW_ID);
        dummyEvent.setKey("KEY");
        underTest.accept(dummyEvent);
        verify(flowLogService, times(1))
                .save(eq(FLOW_ID), nullable(String.class), eq("KEY"), any(Payload.class), anyMap(), nullable(Class.class), eq(flowState));
        verify(flow, times(1)).sendEvent(eq("KEY"), any());
    }

    @Test
    public void testExistingFlowNotFound() {
        BDDMockito.<FlowConfiguration<?>>given(flowConfigurationMap.get(any())).willReturn(flowConfig);
        dummyEvent.setKey("KEY");
        underTest.accept(dummyEvent);
        verify(flowLogService, times(0)).save(anyString(), anyString(), anyString(), any(Payload.class), anyMap(), any(), any(FlowState.class));
        verify(flow, times(0)).sendEvent(anyString(), any());
    }

    @Test
    public void testFlowFinalFlowNotChained() throws TransactionExecutionException {
        given(runningFlows.remove(FLOW_ID)).willReturn(flow);
        dummyEvent.setKey(Flow2Handler.FLOW_FINAL);
        underTest.accept(dummyEvent);
        verify(flowLogService, times(1)).close(anyLong(), eq(FLOW_ID));
        verify(runningFlows, times(1)).remove(eq(FLOW_ID));
        verify(runningFlows, times(0)).get(eq(FLOW_ID));
        verify(runningFlows, times(0)).put(any(Flow.class), isNull(String.class));
        verify(flowChains, times(0)).removeFlowChain(anyString());
        verify(flowChains, times(0)).triggerNextFlow(anyString());
    }

    @Test
    public void testFlowFinalFlowChained() throws TransactionExecutionException {
        given(runningFlows.remove(FLOW_ID)).willReturn(flow);
        dummyEvent.setKey(Flow2Handler.FLOW_FINAL);
        dummyEvent.getHeaders().set(Flow2Handler.FLOW_CHAIN_ID, FLOW_CHAIN_ID);
        underTest.accept(dummyEvent);
        verify(flowLogService, times(1)).close(anyLong(), eq(FLOW_ID));
        verify(runningFlows, times(1)).remove(eq(FLOW_ID));
        verify(runningFlows, times(0)).get(eq(FLOW_ID));
        verify(runningFlows, times(0)).put(any(Flow.class), isNull(String.class));
        verify(flowChains, times(0)).removeFlowChain(anyString());
        verify(flowChains, times(1)).triggerNextFlow(eq(FLOW_CHAIN_ID));
    }

    @Test
    public void testFlowFinalFlowFailedNoChain() throws TransactionExecutionException {
        given(flow.isFlowFailed()).willReturn(Boolean.TRUE);
        given(runningFlows.remove(FLOW_ID)).willReturn(flow);
        dummyEvent.setKey(Flow2Handler.FLOW_FINAL);
        given(runningFlows.remove(anyString())).willReturn(flow);
        underTest.accept(dummyEvent);
        verify(flowLogService, times(1)).close(anyLong(), eq(FLOW_ID));
        verify(runningFlows, times(1)).remove(eq(FLOW_ID));
        verify(runningFlows, times(0)).get(eq(FLOW_ID));
        verify(runningFlows, times(0)).put(any(Flow.class), isNull(String.class));
        verify(flowChains, times(0)).removeFullFlowChain(anyString());
        verify(flowChains, times(0)).triggerNextFlow(anyString());
    }

    @Test
    public void testFlowFinalFlowFailedWithChain() throws TransactionExecutionException {
        given(flow.isFlowFailed()).willReturn(Boolean.TRUE);
        given(runningFlows.remove(FLOW_ID)).willReturn(flow);
        dummyEvent.setKey(Flow2Handler.FLOW_FINAL);
        dummyEvent.getHeaders().set(Flow2Handler.FLOW_CHAIN_ID, "FLOW_CHAIN_ID");
        given(runningFlows.remove(anyString())).willReturn(flow);
        underTest.accept(dummyEvent);
        verify(flowLogService, times(1)).close(anyLong(), eq(FLOW_ID));
        verify(runningFlows, times(1)).remove(eq(FLOW_ID));
        verify(runningFlows, times(0)).get(eq(FLOW_ID));
        verify(runningFlows, times(0)).put(any(Flow.class), isNull(String.class));
        verify(flowChains, times(1)).removeFullFlowChain(anyString());
        verify(flowChains, times(0)).triggerNextFlow(anyString());
    }

    @Test
    public void testCancelRunningFlows() throws TransactionExecutionException {
        given(flowLogRepository.findAllRunningNonTerminationFlowIdsByStackId(anyLong())).willReturn(Collections.singleton(FLOW_ID));
        given(runningFlows.remove(FLOW_ID)).willReturn(flow);
        given(runningFlows.getFlowChainId(eq(FLOW_ID))).willReturn(FLOW_CHAIN_ID);
        dummyEvent.setKey(Flow2Handler.FLOW_CANCEL);
        underTest.accept(dummyEvent);
        verify(flowLogService, times(1)).cancel(anyLong(), eq(FLOW_ID));
        verify(flowChains, times(1)).removeFullFlowChain(eq(FLOW_CHAIN_ID));
    }

    @Test
    public void testRestartFlowNotRestartable() throws TransactionExecutionException {
        FlowLog flowLog = new FlowLog(STACK_ID, FLOW_ID, "START_STATE", true, StateStatus.SUCCESSFUL);
        flowLog.setFlowType(String.class);
        when(flowLogRepository.findFirstByFlowIdOrderByCreatedDesc(FLOW_ID)).thenReturn(flowLog);
        underTest.restartFlow(FLOW_ID);

        verify(flowLogService, times(1)).terminate(STACK_ID, FLOW_ID);
    }

    @Test
    public void testRestartFlow() throws TransactionExecutionException {
        ReflectionTestUtils.setField(underTest, "flowChainHandler", flowChainHandler);

        FlowLog flowLog = createFlowLog(FLOW_CHAIN_ID);
        Payload payload = new StackEvent(STACK_ID);
        flowLog.setPayload(JsonWriter.objectToJson(payload));
        when(flowLogRepository.findFirstByFlowIdOrderByCreatedDesc(FLOW_ID)).thenReturn(flowLog);

        StackStartFlowConfig stackStartFlowConfig = new StackStartFlowConfig();
        ReflectionTestUtils.setField(stackStartFlowConfig, "defaultRestartAction", defaultRestartAction);

        setUpFlowConfigCreateFlow(stackStartFlowConfig);

        List<FlowConfiguration<?>> flowConfigs = Lists.newArrayList(stackStartFlowConfig);
        ReflectionTestUtils.setField(underTest, "flowConfigs", flowConfigs);

        underTest.restartFlow(FLOW_ID);

        ArgumentCaptor<Payload> payloadCaptor = ArgumentCaptor.forClass(Payload.class);

        verify(flowChainHandler, times(1)).restoreFlowChain(FLOW_CHAIN_ID);
        verify(flowLogService, times(0)).terminate(STACK_ID, FLOW_ID);
        verify(defaultRestartAction, times(1)).restart(eq(FLOW_ID), eq(FLOW_CHAIN_ID), eq(NEXT_EVENT), payloadCaptor.capture());

        Payload captorValue = payloadCaptor.getValue();
        assertEquals(STACK_ID, captorValue.getStackId());
    }

    @Test
    public void testRestartFlowNoRestartAction() throws TransactionExecutionException {
        ReflectionTestUtils.setField(underTest, "flowChainHandler", flowChainHandler);

        FlowLog flowLog = createFlowLog(FLOW_CHAIN_ID);
        Payload payload = new StackEvent(STACK_ID);
        flowLog.setPayload(JsonWriter.objectToJson(payload));
        when(flowLogRepository.findFirstByFlowIdOrderByCreatedDesc(FLOW_ID)).thenReturn(flowLog);

        StackStartFlowConfig stackStartFlowConfig = new StackStartFlowConfig();

        setUpFlowConfigCreateFlow(stackStartFlowConfig);

        List<FlowConfiguration<?>> flowConfigs = Lists.newArrayList(stackStartFlowConfig);
        ReflectionTestUtils.setField(underTest, "flowConfigs", flowConfigs);

        underTest.restartFlow(FLOW_ID);

        verify(flowChainHandler, times(1)).restoreFlowChain(FLOW_CHAIN_ID);
        verify(flowLogService, times(1)).terminate(STACK_ID, FLOW_ID);
        verify(defaultRestartAction, times(0)).restart(any(), any(), any(), any());
    }

    @Test
    public void testRestartFlowNoRestartActionNoFlowChainId() throws TransactionExecutionException {
        ReflectionTestUtils.setField(underTest, "flowChainHandler", flowChainHandler);

        FlowLog flowLog = createFlowLog(null);
        Payload payload = new StackEvent(STACK_ID);
        flowLog.setPayload(JsonWriter.objectToJson(payload));
        when(flowLogRepository.findFirstByFlowIdOrderByCreatedDesc(FLOW_ID)).thenReturn(flowLog);

        StackStartFlowConfig stackStartFlowConfig = new StackStartFlowConfig();

        setUpFlowConfigCreateFlow(stackStartFlowConfig);

        List<FlowConfiguration<?>> flowConfigs = Lists.newArrayList(stackStartFlowConfig);
        ReflectionTestUtils.setField(underTest, "flowConfigs", flowConfigs);

        underTest.restartFlow(FLOW_ID);

        verify(flowChainHandler, times(0)).restoreFlowChain(FLOW_CHAIN_ID);
        verify(flowLogService, times(1)).terminate(STACK_ID, FLOW_ID);
        verify(defaultRestartAction, times(0)).restart(any(), any(), any(), any());
    }

    private FlowLog createFlowLog(String flowChainId) {
        FlowLog flowLog = new FlowLog(STACK_ID, FLOW_ID, "START_STATE", true, StateStatus.SUCCESSFUL);
        flowLog.setFlowType(StackStartFlowConfig.class);
        flowLog.setVariables(JsonWriter.objectToJson(new HashMap<>()));
        flowLog.setFlowChainId(flowChainId);
        flowLog.setNextEvent(NEXT_EVENT);
        return flowLog;
    }

    private void setUpFlowConfigCreateFlow(StackStartFlowConfig stackStartFlowConfig) {
        doReturn(stateMachine).when(stateMachineFactory).getStateMachine();
        doReturn(stateMachineAccessor).when(stateMachine).getStateMachineAccessor();
        doReturn(Collections.singletonList(stateMachineAccess)).when(stateMachineAccessor).withAllRegions();

        when(stateMachine.getExtendedState()).thenReturn(extendedState);
        when(extendedState.getVariables()).thenReturn(new HashMap<>());
        ReflectionTestUtils.setField(stackStartFlowConfig, "stateMachineFactory", stateMachineFactory);
        ReflectionTestUtils.setField(stackStartFlowConfig, "applicationContext", applicationContext);
    }

    private static class OwnFlowState implements FlowState {
        @Override
        public String name() {
            return null;
        }
    }
}
