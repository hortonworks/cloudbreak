package com.sequenceiq.datalake.flow.upgrade.ccm;

import static com.sequenceiq.datalake.flow.upgrade.ccm.UpgradeCcmStateSelectors.UPGRADE_CCM_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.ccm.UpgradeCcmStateSelectors.UPGRADE_CCM_FINALIZED_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.state.State;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.upgrade.ccm.event.UpgradeCcmFailedEvent;
import com.sequenceiq.datalake.flow.upgrade.ccm.event.UpgradeCcmStackEvent;
import com.sequenceiq.datalake.flow.upgrade.ccm.event.UpgradeCcmStackRequest;
import com.sequenceiq.datalake.flow.upgrade.ccm.event.UpgradeCcmSuccessEvent;
import com.sequenceiq.datalake.metric.MetricType;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.MessageFactory;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import reactor.bus.Event;
import reactor.bus.EventBus;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
class UpgradeCcmActionsTest {

    private static final String FLOW_ID = "flowId";

    private static final String FLOW_TRIGGER_USER_CRN = "flowTriggerUserCrn";

    private static final String ACTION_PAYLOAD_SELECTOR = "selector";

    private static final Long ENVIRONMENT_ID = 1234L;

    private static final String ENVIRONMENT_NAME = "environmentName";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String MESSAGE = "Houston, we have a problem.";

    private static final String FAILURE_EVENT = "failureEvent";

    private static final long SDX_ID = 12321L;

    private static final String USER_ID = "user";

    @Mock
    private StateContext stateContext;

    @Mock
    private ExtendedState extendedState;

    @Mock
    private StateMachine stateMachine;

    @Mock
    private State state;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private FlowEvent failureEvent;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private Event<Object> event;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private SdxMetricService metricService;

    @InjectMocks
    private UpgradeCcmActions underTest;

    private Object actionPayload;

    @Captor
    private ArgumentCaptor<String> selectorArgumentCaptor;

    @Captor
    private ArgumentCaptor<Event<?>> eventArgumentCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> headersArgumentCaptor;

    @Captor
    private ArgumentCaptor<Object> payloadArgumentCaptor;

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
    private FlowEvent flowEvent;

    private SdxCluster sdxCluster;

    @BeforeEach
    void setUp() {
        sdxCluster = new SdxCluster();
        sdxCluster.setId(SDX_ID);

        FlowParameters flowParameters = new FlowParameters(FLOW_ID, FLOW_TRIGGER_USER_CRN, null);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.FLOW_PARAMETERS.name())).thenReturn(flowParameters);
        when(stateContext.getExtendedState()).thenReturn(extendedState);
        when(extendedState.getVariables()).thenReturn(new HashMap<>());
        when(stateContext.getStateMachine()).thenReturn(stateMachine);
        when(stateMachine.getState()).thenReturn(state);
        lenient().when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        when(stateContext.getEvent()).thenReturn(flowEvent);
        when(tracer.buildSpan(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.addReference(anyString(), any())).thenReturn(spanBuilder);
        when(spanBuilder.ignoreActiveSpan()).thenReturn(spanBuilder);
        when(spanBuilder.start()).thenReturn(span);
        when(tracer.activateSpan(span)).thenReturn(scope);
        when(span.context()).thenReturn(spanContext);
        when(flowEvent.name()).thenReturn("eventName");
    }

    @Test
    void stackUpgradeHappyPath() {
        actionPayload = new UpgradeCcmStackEvent(ACTION_PAYLOAD_SELECTOR, sdxCluster, USER_ID);
        setupContextWithPayload();
        testUpgradeActionHappyPath(underTest::upgradeCcmStack);
        UpgradeCcmStackRequest payload = (UpgradeCcmStackRequest) payloadArgumentCaptor.getValue();
        assertThat(selectorArgumentCaptor.getValue()).isEqualTo(UpgradeCcmStackRequest.class.getSimpleName());
        assertThat(payload.getResourceId()).isEqualTo(SDX_ID);
        assertThat(payload.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void finishedHappyPath() {
        actionPayload = new UpgradeCcmSuccessEvent(SDX_ID, USER_ID);
        setupContextWithPayload();

        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxStatusService.setStatusForDatalakeAndNotify(any(), any(), anyLong())).thenReturn(sdxCluster);

        testUpgradeActionHappyPath(underTest::finishedAction);
        verify(metricService).incrementMetricCounter(MetricType.UPGRADE_CCM_FINISHED, sdxCluster);
        verify(sdxStatusService).setStatusForDatalakeAndNotify(
                DatalakeStatusEnum.RUNNING, "Cluster Connectivity Manager upgrade completed successfully", SDX_ID);
        UpgradeCcmSuccessEvent payload = (UpgradeCcmSuccessEvent) payloadArgumentCaptor.getValue();
        assertThat(selectorArgumentCaptor.getValue()).isEqualTo(UPGRADE_CCM_FINALIZED_EVENT.event());
        assertThat(payload.getResourceId()).isEqualTo(SDX_ID);
        assertThat(payload.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void finishedFailPath() {
        actionPayload = new UpgradeCcmSuccessEvent(SDX_ID, USER_ID);
        setupContextWithPayload();

        testErrorInSdxStatusService(underTest::finishedAction);
        verify(sdxStatusService).setStatusForDatalakeAndNotify(
                DatalakeStatusEnum.RUNNING, "Cluster Connectivity Manager upgrade completed successfully", SDX_ID);
        UpgradeCcmSuccessEvent payload = (UpgradeCcmSuccessEvent) payloadArgumentCaptor.getValue();
        assertThat(payload.getResourceId()).isEqualTo(SDX_ID);
        assertThat(payload.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void failureAction() {
        IllegalStateException failureException = new IllegalStateException(MESSAGE);
        actionPayload = new UpgradeCcmFailedEvent(SDX_ID, USER_ID, failureException);
        setupContextWithPayload();
        testUpgradeActionHappyPath(underTest::failedAction);
        UpgradeCcmFailedEvent payload = (UpgradeCcmFailedEvent) payloadArgumentCaptor.getValue();
        assertThat(selectorArgumentCaptor.getValue()).isEqualTo(UPGRADE_CCM_FAILED_HANDLED_EVENT.event());
        assertThat(payload.getResourceId()).isEqualTo(SDX_ID);
        assertThat(payload.getUserId()).isEqualTo(USER_ID);
        verify(sdxStatusService).setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_UPGRADE_CCM_FAILED, MESSAGE, SDX_ID);
        verify(metricService).incrementMetricCounter(eq(MetricType.UPGRADE_CCM_FAILED), (SdxCluster) any());
    }

    private Action<?, ?> configureAction(Supplier<Action<?, ?>> actionSupplier) {
        Action<?, ?> action = actionSupplier.get();
        assertThat(action).isNotNull();
        setActionPrivateFields(action);
        AbstractAction abstractAction = (AbstractAction) action;
        abstractAction.setFailureEvent(failureEvent);
        return action;
    }

    private void setActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, tracer, Tracer.class);
    }

    private void verifyFailureEvent() {
        assertThat(selectorArgumentCaptor.getValue()).isEqualTo(FAILURE_EVENT);

        verifyEventFactoryAndHeaders();

        assertThat(payloadArgumentCaptor.getValue()).isSameAs(actionPayload);
    }

    private void verifyEventFactoryAndHeaders() {
        assertThat(eventArgumentCaptor.getValue()).isSameAs(event);

        Map<String, Object> headers = headersArgumentCaptor.getValue();
        assertThat(headers).isNotNull();
        assertThat(headers.get(FlowConstants.FLOW_ID)).isEqualTo(FLOW_ID);
        assertThat(headers.get(FlowConstants.FLOW_TRIGGER_USERCRN)).isEqualTo(FLOW_TRIGGER_USER_CRN);
    }

    private void testUpgradeActionHappyPath(Supplier<Action<?, ?>> upgradeAction) {
        Action<?, ?> action = configureAction(upgradeAction);
        action.execute(stateContext);

        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(reactorEventFactory).createEvent(headersArgumentCaptor.capture(), payloadArgumentCaptor.capture());
    }

    private void testErrorInSdxStatusService(Supplier<Action<?, ?>> upgradeAction) {
        when(sdxStatusService.setStatusForDatalakeAndNotify(any(), any(), anyLong())).thenThrow(new IllegalStateException(MESSAGE));
        when(failureEvent.event()).thenReturn(FAILURE_EVENT);
        Action<?, ?> action = configureAction(upgradeAction);

        action.execute(stateContext);

        verify(metricService, never()).incrementMetricCounter(any(MetricType.class), (SdxCluster) any());
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(reactorEventFactory).createEvent(headersArgumentCaptor.capture(), payloadArgumentCaptor.capture());
        verifyFailureEvent();
        assertThat(selectorArgumentCaptor.getValue()).isEqualTo(FAILURE_EVENT);
    }

    private void setupContextWithPayload() {
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.DATA.name())).thenReturn(actionPayload);
    }
}
