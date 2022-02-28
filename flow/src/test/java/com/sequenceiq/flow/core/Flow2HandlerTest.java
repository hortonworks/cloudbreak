package com.sequenceiq.flow.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.cleanup.InMemoryCleanup;
import com.sequenceiq.flow.core.cache.FlowStatCache;
import com.sequenceiq.flow.core.chain.FlowChainHandler;
import com.sequenceiq.flow.core.chain.FlowChains;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.core.exception.FlowNotTriggerableException;
import com.sequenceiq.flow.core.helloworld.config.HelloWorldFlowConfig;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;
import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;
import com.sequenceiq.flow.ha.NodeConfig;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import reactor.bus.Event;
import reactor.bus.Event.Headers;
import reactor.rx.Promise;

@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
public class Flow2HandlerTest {

    private static final String FLOW_ID = "flowId";

    private static final String FLOW_CHAIN_ID = "flowChainId";

    private static final String FLOW_TRIGGER_USERCRN = "flowTriggerUserCrn";

    private static final Long STACK_ID = 1L;

    private static final String NEXT_EVENT = "NEXT_EVENT";

    private static final String UNKNOWN_OP_TYPE = "UNKNOWN";

    @InjectMocks
    private Flow2Handler underTest;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private FlowChainLogService flowChainLogService;

    @Mock
    private Map<String, FlowConfiguration<?>> flowConfigurationMap;

    @Mock
    private Set<String> retryableEvents;

    @Mock
    private Set<String> failHandledEvents;

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
    private ApplicationFlowInformation applicationFlowInformation;

    @Mock
    private Tracer tracer;

    @Mock
    private Tracer.SpanBuilder spanBuilder;

    @Mock
    private Span span;

    @Mock
    private Scope scope;

    @Mock
    private SpanContext spanContext;

    @Mock
    private NodeConfig nodeConfig;

    @Mock
    private InMemoryCleanup inMemoryCleanup;

    @Mock
    private FlowStatCache flowStatCache;

    @Mock
    private List<FlowConfiguration<?>> flowConfigs;

    private FlowState flowState;

    private Event<? extends Payload> dummyEvent;

    private final Payload payload = () -> 1L;

    @BeforeEach
    public void setUp() throws TransactionExecutionException {
        underTest = new Flow2Handler();
        MockitoAnnotations.initMocks(this);
        Map<String, Object> headers = new HashMap<>();
        headers.put(FlowConstants.FLOW_ID, FLOW_ID);
        dummyEvent = new Event<>(new Headers(headers), payload);
        flowState = new OwnFlowState();
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(transactionService).required(any(Runnable.class));
        when(tracer.buildSpan(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.addReference(anyString(), any())).thenReturn(spanBuilder);
        when(spanBuilder.ignoreActiveSpan()).thenReturn(spanBuilder);
        when(spanBuilder.start()).thenReturn(span);
        when(tracer.activateSpan(span)).thenReturn(scope);
        when(span.context()).thenReturn(spanContext);
    }

    @Test
    public void testNewFlow() {
        BDDMockito.<FlowConfiguration<?>>given(flowConfigurationMap.get(any())).willReturn(flowConfig);
        given(flowConfig.createFlow(anyString(), any(), anyLong(), any())).willReturn(flow);
        given(flowConfig.getFlowTriggerCondition()).willReturn(flowTriggerCondition);
        given(flowConfig.getFlowOperationType()).willReturn(OperationType.UNKNOWN);
        given(flowTriggerCondition.isFlowTriggerable(anyLong())).willReturn(FlowTriggerConditionResult.OK);
        given(flow.getCurrentState()).willReturn(flowState);
        Event<Payload> event = new Event<>(payload);
        event.setKey("KEY");
        underTest.accept(event);
        verify(flowConfigurationMap, times(1)).get(anyString());
        verify(runningFlows, times(1)).put(eq(flow), isNull(String.class));
        verify(flowLogService, times(1))
                .save(any(FlowParameters.class), nullable(String.class), eq("KEY"), any(Payload.class), any(), eq(flowConfig.getClass()), eq(flowState));
        verify(flow, times(1)).sendEvent(anyString(), isNull(), any(), any(), eq(UNKNOWN_OP_TYPE));
    }

    @Test
    public void testFlowCanNotBeSaved() {
        BDDMockito.<FlowConfiguration<?>>given(flowConfigurationMap.get(any())).willReturn(flowConfig);
        given(flowConfig.createFlow(anyString(), any(), anyLong(), any())).willReturn(flow);
        given(flowConfig.getFlowTriggerCondition()).willReturn(flowTriggerCondition);
        given(flowConfig.getFlowOperationType()).willReturn(OperationType.UNKNOWN);
        given(flowTriggerCondition.isFlowTriggerable(anyLong())).willReturn(FlowTriggerConditionResult.OK);
        given(flow.getCurrentState()).willReturn(flowState);
        Event<Payload> event = new Event<>(payload);
        event.setKey("KEY");
        when(flowLogService.save(any(FlowParameters.class), nullable(String.class), anyString(), any(Payload.class), any(),
                eq(flowConfig.getClass()), eq(flowState))).thenThrow(new RuntimeException("Can't save flow log"));
        assertThrows(CloudbreakServiceException.class,
                () -> underTest.accept(event));
        verify(flowConfigurationMap, times(1)).get(anyString());
        verify(runningFlows, times(1)).put(eq(flow), isNull(String.class));
        verify(flowLogService, times(1))
                .save(any(FlowParameters.class), nullable(String.class), eq("KEY"), any(Payload.class), any(), eq(flowConfig.getClass()), eq(flowState));
        verify(runningFlows, times(1)).remove(anyString());
        verify(flow, times(0)).sendEvent(anyString(), isNull(), any(), any(), eq(UNKNOWN_OP_TYPE));
    }

    @Test
    public void testFlowRejectedBecauseNotTriggerable() {
        BDDMockito.<FlowConfiguration<?>>given(flowConfigurationMap.get(any())).willReturn(flowConfig);
        given(flowConfig.createFlow(anyString(), any(), anyLong(), any())).willReturn(flow);
        given(flowConfig.getFlowTriggerCondition()).willReturn(flowTriggerCondition);
        given(flow.getCurrentState()).willReturn(flowState);
        when(flowTriggerCondition.isFlowTriggerable(anyLong()))
                .thenReturn(new FlowTriggerConditionResult("It's not triggerable!"));

        Promise accepted = mock(Promise.class);
        Payload mockPayload = new MockPayload(accepted);
        Event<Payload> event = new Event<>(mockPayload);
        event.setKey("KEY");

        underTest.accept(event);

        verify(accepted, times(1)).onError(any(FlowNotTriggerableException.class));

        verify(flowConfigurationMap, times(1)).get(anyString());
        verify(runningFlows, times(0)).put(eq(flow), isNull(String.class));
        verify(flowLogService, times(0))
                .save(any(FlowParameters.class), nullable(String.class), eq("KEY"), any(Payload.class), any(), eq(flowConfig.getClass()), eq(flowState));
        verify(runningFlows, times(0)).remove(anyString());
        verify(flow, times(0)).sendEvent(anyString(), anyString(), isNull(), any(), any());
    }

    @Test
    public void testNewSyncFlowMaintenanceActive() {
        HelloWorldFlowConfig helloWorldFlowConfig = mock(HelloWorldFlowConfig.class);
        given(helloWorldFlowConfig.getFlowTriggerCondition()).willReturn(new DefaultFlowTriggerCondition());

        BDDMockito.<FlowConfiguration<?>>given(flowConfigurationMap.get(any())).willReturn(helloWorldFlowConfig);
        given(helloWorldFlowConfig.createFlow(anyString(), any(), anyLong(), any())).willReturn(flow);
        given(helloWorldFlowConfig.getFlowTriggerCondition()).willReturn(flowTriggerCondition);
        given(helloWorldFlowConfig.getFlowOperationType()).willReturn(OperationType.UNKNOWN);
        given(flowTriggerCondition.isFlowTriggerable(anyLong())).willReturn(FlowTriggerConditionResult.OK);
        given(flow.getCurrentState()).willReturn(flowState);
        Event<Payload> event = new Event<>(payload);
        event.setKey("KEY");
        event.getHeaders().set(FlowConstants.FLOW_TRIGGER_USERCRN, FLOW_TRIGGER_USERCRN);
        underTest.accept(event);
        verify(flowConfigurationMap, times(1)).get(anyString());
        verify(runningFlows, times(1)).put(eq(flow), isNull(String.class));
        verify(flowLogService, times(1)).save(any(FlowParameters.class), nullable(String.class), eq("KEY"), any(Payload.class), any(),
                ArgumentMatchers.eq(helloWorldFlowConfig.getClass()), eq(flowState));
        verify(flow, times(1)).sendEvent(anyString(), anyString(), any(), any(), eq(UNKNOWN_OP_TYPE));
    }

    @Test
    public void testNewFlowButNotHandled() {
        Event<Payload> event = new Event<>(payload);
        event.setKey("KEY");
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.accept(event));
        assertEquals("Couldn't start process.", exception.getMessage());
        verify(flowConfigurationMap, times(1)).get(anyString());
        verify(runningFlows, never()).put(any(Flow.class), isNull(String.class));
        verify(flowLogService, never()).save(any(FlowParameters.class), anyString(), anyString(), any(Payload.class), anyMap(), any(), any(FlowState.class));
    }

    @Test
    public void testExistingFlow() {
        FlowLog lastFlowLog = new FlowLog();
        lastFlowLog.setNextEvent("KEY");
        Optional<FlowLog> flowLogOptional = Optional.of(lastFlowLog);
        BDDMockito.<FlowConfiguration<?>>given(flowConfigurationMap.get(any())).willReturn(flowConfig);
        given(runningFlows.get(anyString())).willReturn(flow);
        given(flow.getCurrentState()).willReturn(flowState);
        given(flow.getFlowId()).willReturn(FLOW_ID);
        given(flowLogService.getLastFlowLog(FLOW_ID)).willReturn(flowLogOptional);

        dummyEvent.setKey("KEY");
        ArgumentCaptor<FlowParameters> flowParamsCaptor = ArgumentCaptor.forClass(FlowParameters.class);
        underTest.accept(dummyEvent);
        verify(flowLogService, times(1))
                .save(flowParamsCaptor.capture(), nullable(String.class), eq("KEY"), any(Payload.class), anyMap(), nullable(Class.class), eq(flowState));
        verify(flow, times(1)).sendEvent(eq("KEY"), isNull(), any(), any(), any());
        FlowParameters flowParameters = flowParamsCaptor.getValue();
        assertEquals(FLOW_ID, flowParameters.getFlowId());
        assertNull(flowParameters.getFlowTriggerUserCrn());
    }

    @Test
    public void testFinalizedFlow() {
        FlowLog lastFlowLog = new FlowLog();
        lastFlowLog.setFinalized(true);
        Optional<FlowLog> flowLogOptional = Optional.of(lastFlowLog);
        BDDMockito.<FlowConfiguration<?>>given(flowConfigurationMap.get(any())).willReturn(flowConfig);
        given(runningFlows.get(anyString())).willReturn(flow);
        given(flow.getCurrentState()).willReturn(flowState);
        given(flow.getFlowId()).willReturn(FLOW_ID);
        given(flowLogService.getLastFlowLog(FLOW_ID)).willReturn(flowLogOptional);

        dummyEvent.setKey("KEY");
        ArgumentCaptor<FlowParameters> flowParamsCaptor = ArgumentCaptor.forClass(FlowParameters.class);
        underTest.accept(dummyEvent);
        verify(flowLogService, times(1))
                .save(flowParamsCaptor.capture(), nullable(String.class), eq("KEY"), any(Payload.class), anyMap(), nullable(Class.class), eq(flowState));
        verify(flow, times(1)).sendEvent(eq("KEY"), isNull(), any(), any(), any());
        FlowParameters flowParameters = flowParamsCaptor.getValue();
        assertEquals(FLOW_ID, flowParameters.getFlowId());
        assertNull(flowParameters.getFlowTriggerUserCrn());
    }

    @Test
    public void testChangedNodeId() {
        FlowLog lastFlowLog = new FlowLog();
        lastFlowLog.setNextEvent("KEY");
        lastFlowLog.setCloudbreakNodeId("OtherNode");
        Optional<FlowLog> flowLogOptional = Optional.of(lastFlowLog);
        BDDMockito.<FlowConfiguration<?>>given(flowConfigurationMap.get(any())).willReturn(flowConfig);
        given(nodeConfig.getId()).willReturn("CurrentNode");
        given(runningFlows.get(anyString())).willReturn(flow);
        given(flow.getCurrentState()).willReturn(flowState);
        given(flow.getFlowId()).willReturn(FLOW_ID);
        given(flowLogService.getLastFlowLog(FLOW_ID)).willReturn(flowLogOptional);

        dummyEvent.setKey("KEY");
        underTest.accept(dummyEvent);
        verify(flowLogService, never()).save(any(), any(), any(), any(), any(), any(), any());
        verify(flow, never()).sendEvent(any(), any(), any(), any(), any());
        verify(inMemoryCleanup, times(1)).cancelFlowWithoutDbUpdate(FLOW_ID);
    }

    @Test
    public void testExistingFlowRepeatedState() {
        BDDMockito.<FlowConfiguration<?>>given(flowConfigurationMap.get(any())).willReturn(flowConfig);
        given(runningFlows.get(anyString())).willReturn(flow);
        given(flow.getCurrentState()).willReturn(flowState);
        given(flow.getFlowId()).willReturn(FLOW_ID);

        Map<Object, Object> variables = Map.of("repeated", 2);
        given(flow.getVariables()).willReturn(variables);

        FlowLog lastFlowLog = new FlowLog();
        lastFlowLog.setNextEvent("KEY");
        Optional<FlowLog> flowLogOptional = Optional.of(lastFlowLog);
        given(flowLogService.getLastFlowLog(FLOW_ID)).willReturn(flowLogOptional);
        given(flowLogService.repeatedFlowState(lastFlowLog, "KEY")).willReturn(true);
        dummyEvent.setKey("KEY");
        underTest.accept(dummyEvent);
        verify(flowLogService, times(1))
                .updateLastFlowLogPayload(lastFlowLog, payload, variables);
        verify(flow, times(1)).sendEvent(eq("KEY"), isNull(), any(), any(), any());
    }

    @Test
    public void testExistingFlowNotFound() {
        BDDMockito.<FlowConfiguration<?>>given(flowConfigurationMap.get(any())).willReturn(flowConfig);
        dummyEvent.setKey("KEY");
        underTest.accept(dummyEvent);
        verify(flowLogService, never()).save(any(FlowParameters.class), anyString(), anyString(), any(Payload.class), anyMap(), any(), any(FlowState.class));
        verify(flow, never()).sendEvent(anyString(), anyString(), any(), any(), any());
    }

    @Test
    public void testFlowFinalFlowNotChained() throws TransactionExecutionException {
        given(runningFlows.remove(FLOW_ID)).willReturn(flow);
        dummyEvent.setKey(FlowConstants.FLOW_FINAL);
        underTest.accept(dummyEvent);
        verify(flowLogService, times(1)).close(anyLong(), eq(FLOW_ID));
        verify(runningFlows, times(1)).remove(eq(FLOW_ID));
        verify(runningFlows, never()).get(eq(FLOW_ID));
        verify(runningFlows, never()).put(any(Flow.class), isNull(String.class));
        verify(flowChains, never()).removeFlowChain(anyString(), anyBoolean());
        verify(flowChains, never()).triggerNextFlow(anyString(), anyString(), any(Map.class), any(), any());
    }

    @Test
    public void testFlowFinalFlowChained() throws TransactionExecutionException {
        given(runningFlows.remove(FLOW_ID)).willReturn(flow);
        dummyEvent.setKey(FlowConstants.FLOW_FINAL);
        dummyEvent.getHeaders().set(FlowConstants.FLOW_CHAIN_ID, FLOW_CHAIN_ID);
        dummyEvent.getHeaders().set(FlowConstants.FLOW_TRIGGER_USERCRN, FLOW_TRIGGER_USERCRN);
        underTest.accept(dummyEvent);
        verify(flowLogService, times(1)).close(anyLong(), eq(FLOW_ID));
        verify(runningFlows, times(1)).remove(eq(FLOW_ID));
        verify(runningFlows, never()).get(eq(FLOW_ID));
        verify(runningFlows, never()).put(any(Flow.class), isNull(String.class));
        verify(flowChains, never()).removeFlowChain(anyString(), anyBoolean());
        verify(flowChains, times(1)).triggerNextFlow(eq(FLOW_CHAIN_ID), eq(FLOW_TRIGGER_USERCRN), any(Map.class), any(), any());
    }

    @Test
    public void testFlowFinalFlowFailedNoChain() throws TransactionExecutionException {
        given(flow.isFlowFailed()).willReturn(Boolean.TRUE);
        given(runningFlows.remove(FLOW_ID)).willReturn(flow);
        dummyEvent.setKey(FlowConstants.FLOW_FINAL);
        given(runningFlows.remove(anyString())).willReturn(flow);
        underTest.accept(dummyEvent);
        verify(flowLogService, times(1)).close(anyLong(), eq(FLOW_ID));
        verify(runningFlows, times(1)).remove(eq(FLOW_ID));
        verify(runningFlows, never()).get(eq(FLOW_ID));
        verify(runningFlows, never()).put(any(Flow.class), isNull(String.class));
        verify(flowChains, never()).removeFullFlowChain(anyString(), anyBoolean());
        verify(flowChains, never()).triggerNextFlow(anyString(), anyString(), any(Map.class), any(), any());
    }

    @Test
    public void testFlowFinalFlowFailedWithChain() throws TransactionExecutionException {
        given(flow.isFlowFailed()).willReturn(Boolean.TRUE);
        given(runningFlows.remove(FLOW_ID)).willReturn(flow);
        dummyEvent.setKey(FlowConstants.FLOW_FINAL);
        dummyEvent.getHeaders().set(FlowConstants.FLOW_CHAIN_ID, "FLOW_CHAIN_ID");
        given(runningFlows.remove(anyString())).willReturn(flow);
        underTest.accept(dummyEvent);
        verify(flowLogService, times(1)).close(anyLong(), eq(FLOW_ID));
        verify(runningFlows, times(1)).remove(eq(FLOW_ID));
        verify(runningFlows, never()).get(eq(FLOW_ID));
        verify(runningFlows, never()).put(any(Flow.class), isNull(String.class));
        verify(flowChains, times(1)).removeFullFlowChain(anyString(), anyBoolean());
        verify(flowChains, never()).triggerNextFlow(anyString(), anyString(), any(Map.class), any(), any());
    }

    @Test
    public void testCancelRunningFlows() throws TransactionExecutionException {
        given(flowLogService.findAllRunningNonTerminationFlowIdsByStackId(anyLong())).willReturn(Collections.singleton(FLOW_ID));
        given(runningFlows.remove(FLOW_ID)).willReturn(flow);
        given(runningFlows.getFlowChainId(eq(FLOW_ID))).willReturn(FLOW_CHAIN_ID);
        dummyEvent.setKey(FlowConstants.FLOW_CANCEL);
        underTest.accept(dummyEvent);
        verify(flowLogService, times(1)).cancel(anyLong(), eq(FLOW_ID));
        verify(flowChains, times(1)).removeFullFlowChain(eq(FLOW_CHAIN_ID), eq(false));
    }

    @Test
    public void testRestartFlowNotRestartable() throws TransactionExecutionException {
        FlowLog flowLog = new FlowLog(STACK_ID, FLOW_ID, "START_STATE", true, StateStatus.SUCCESSFUL, OperationType.UNKNOWN);
        flowLog.setFlowType(ClassValue.of(String.class));
        flowLog.setPayloadType(ClassValue.of(TestPayload.class));
        when(flowLogService.findFirstByFlowIdOrderByCreatedDesc(FLOW_ID)).thenReturn(Optional.of(flowLog));
        underTest.restartFlow(FLOW_ID);

        verify(flowLogService, times(1)).terminate(STACK_ID, FLOW_ID);
    }

    @Test
    public void testRestartFlow() throws TransactionExecutionException {
        FlowLog flowLog = createFlowLog(FLOW_CHAIN_ID);
        Payload payload = new TestPayload(STACK_ID);
        flowLog.setPayloadType(ClassValue.of(TestPayload.class));
        flowLog.setPayload(JsonWriter.objectToJson(payload));
        when(applicationFlowInformation.getRestartableFlows()).thenReturn(List.of(HelloWorldFlowConfig.class));
        when(flowLogService.findFirstByFlowIdOrderByCreatedDesc(FLOW_ID)).thenReturn(Optional.of(flowLog));

        HelloWorldFlowConfig helloWorldFlowConfig = new HelloWorldFlowConfig();
        ReflectionTestUtils.setField(helloWorldFlowConfig, "defaultRestartAction", defaultRestartAction);

        setUpFlowConfigCreateFlow(helloWorldFlowConfig);

        List<FlowConfiguration<?>> flowConfigs = Lists.newArrayList(helloWorldFlowConfig);
        ReflectionTestUtils.setField(underTest, "flowConfigs", flowConfigs);

        underTest.restartFlow(FLOW_ID);

        ArgumentCaptor<Payload> payloadCaptor = ArgumentCaptor.forClass(Payload.class);
        ArgumentCaptor<FlowParameters> flowParamsCaptor = ArgumentCaptor.forClass(FlowParameters.class);

        verify(flowChainHandler, times(1)).restoreFlowChain(FLOW_CHAIN_ID);
        verify(flowLogService, never()).terminate(STACK_ID, FLOW_ID);
        verify(defaultRestartAction, times(1)).restart(flowParamsCaptor.capture(), eq(FLOW_CHAIN_ID), eq(NEXT_EVENT), payloadCaptor.capture());

        Payload captorValue = payloadCaptor.getValue();
        assertEquals(STACK_ID, captorValue.getResourceId());
        FlowParameters flowParameters = flowParamsCaptor.getValue();
        assertEquals(FLOW_ID, flowParameters.getFlowId());
        assertEquals(FLOW_TRIGGER_USERCRN, flowParameters.getFlowTriggerUserCrn());
    }

    @Test
    public void testRestartFlowNoRestartAction() throws TransactionExecutionException {
        FlowLog flowLog = createFlowLog(FLOW_CHAIN_ID);
        Payload payload = new TestPayload(STACK_ID);
        flowLog.setPayloadType(ClassValue.of(TestPayload.class));
        flowLog.setPayload(JsonWriter.objectToJson(payload));
        when(flowLogService.findFirstByFlowIdOrderByCreatedDesc(FLOW_ID)).thenReturn(Optional.of(flowLog));
        when(applicationFlowInformation.getRestartableFlows()).thenReturn(List.of(HelloWorldFlowConfig.class));

        HelloWorldFlowConfig helloWorldFlowConfig = new HelloWorldFlowConfig();

        setUpFlowConfigCreateFlow(helloWorldFlowConfig);

        List<FlowConfiguration<?>> flowConfigs = Lists.newArrayList(helloWorldFlowConfig);
        ReflectionTestUtils.setField(underTest, "flowConfigs", flowConfigs);

        underTest.restartFlow(FLOW_ID);

        verify(flowChainHandler, times(1)).restoreFlowChain(FLOW_CHAIN_ID);
        verify(flowLogService, times(1)).terminate(STACK_ID, FLOW_ID);
        verify(defaultRestartAction, never()).restart(any(), any(), any(), any());
    }

    @Test
    public void testRestartFlowNoRestartActionNoFlowChainId() throws TransactionExecutionException {
        FlowLog flowLog = createFlowLog(null);
        Payload payload = new TestPayload(STACK_ID);
        flowLog.setPayloadType(ClassValue.of(TestPayload.class));
        flowLog.setPayload(JsonWriter.objectToJson(payload));
        when(flowLogService.findFirstByFlowIdOrderByCreatedDesc(FLOW_ID)).thenReturn(Optional.of(flowLog));
        when(applicationFlowInformation.getRestartableFlows()).thenReturn(List.of(HelloWorldFlowConfig.class));
        HelloWorldFlowConfig helloWorldFlowConfig = new HelloWorldFlowConfig();

        setUpFlowConfigCreateFlow(helloWorldFlowConfig);

        List<FlowConfiguration<?>> flowConfigs = Lists.newArrayList(helloWorldFlowConfig);
        ReflectionTestUtils.setField(underTest, "flowConfigs", flowConfigs);

        underTest.restartFlow(FLOW_ID);

        verify(flowChainHandler, never()).restoreFlowChain(FLOW_CHAIN_ID);
        verify(flowLogService, times(1)).terminate(STACK_ID, FLOW_ID);
        verify(defaultRestartAction, never()).restart(any(), any(), any(), any());
    }

    private FlowLog createFlowLog(String flowChainId) {
        FlowLog flowLog = new FlowLog(STACK_ID, FLOW_ID, "START_STATE", true, StateStatus.SUCCESSFUL, OperationType.UNKNOWN);
        flowLog.setFlowType(ClassValue.of(HelloWorldFlowConfig.class));
        flowLog.setVariables(JsonWriter.objectToJson(new HashMap<>()));
        flowLog.setFlowChainId(flowChainId);
        flowLog.setNextEvent(NEXT_EVENT);
        flowLog.setFlowTriggerUserCrn(FLOW_TRIGGER_USERCRN);
        return flowLog;
    }

    private void setUpFlowConfigCreateFlow(HelloWorldFlowConfig stackStartFlowConfig) {
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

        @Override
        public Class<? extends RestartAction> restartAction() {
            return DefaultRestartAction.class;
        }

    }

    private static class MockPayload implements Payload, Acceptable {

        private Promise<AcceptResult> accepted;

        MockPayload(Promise<AcceptResult> promise) {
            accepted = promise;
        }

        @Override
        public Promise<AcceptResult> accepted() {
            return accepted;
        }

        @Override
        public Long getResourceId() {
            return 1L;
        }
    }
}
