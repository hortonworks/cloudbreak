package com.sequenceiq.environment.environment.flow.upgrade.ccm;

import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmHandlerSelectors.UPGRADE_CCM_DATAHUB_HANDLER;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmHandlerSelectors.UPGRADE_CCM_DATALAKE_HANDLER;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmHandlerSelectors.UPGRADE_CCM_FREEIPA_HANDLER;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmHandlerSelectors.UPGRADE_CCM_VALIDATION_HANDLER;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors.FINALIZE_UPGRADE_CCM_EVENT;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors.HANDLED_FAILED_UPGRADE_CCM_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmEvent;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.metrics.EnvironmentMetricService;
import com.sequenceiq.environment.metrics.MetricType;
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
    private EnvironmentStatusUpdateService environmentStatusUpdateService;

    @Mock
    private EnvironmentMetricService metricService;

    @InjectMocks
    private UpgradeCcmActions underTest;

    private UpgradeCcmEvent actionPayload;

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

    @BeforeEach
    void setUp() {
        FlowParameters flowParameters = new FlowParameters(FLOW_ID, FLOW_TRIGGER_USER_CRN, null);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.FLOW_PARAMETERS.name())).thenReturn(flowParameters);
        actionPayload = new UpgradeCcmEvent(ACTION_PAYLOAD_SELECTOR, ENVIRONMENT_ID, ENVIRONMENT_NAME, ENVIRONMENT_CRN);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.DATA.name())).thenReturn(actionPayload);
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
    void validationHappyPath() {
        testUpgradeActionHappyPath(underTest::upgradeCcmValidationAction, UPGRADE_CCM_VALIDATION_HANDLER.selector());
        verify(environmentStatusUpdateService)
                .updateEnvironmentStatusAndNotify(any(), any(),
                        eq(EnvironmentStatus.UPGRADE_CCM_VALIDATION_IN_PROGRESS),
                        eq(ResourceEvent.ENVIRONMENT_UPGRADE_CCM_VALIDATION_STARTED),
                        any());
    }

    @Test
    void validationNoEnvironment() {
        testNoEnvironment(underTest::upgradeCcmValidationAction, FAILURE_EVENT);
    }

    @Test
    void upgradeFreeIpaHappyPath() {
        testUpgradeActionHappyPath(underTest::upgradeCcmInFreeIpaAction, UPGRADE_CCM_FREEIPA_HANDLER.selector());
        verify(environmentStatusUpdateService)
                .updateEnvironmentStatusAndNotify(any(), any(),
                        eq(EnvironmentStatus.UPGRADE_CCM_ON_FREEIPA_IN_PROGRESS),
                        eq(ResourceEvent.ENVIRONMENT_UPGRADE_CCM_ON_FREEIPA_STARTED),
                        any());
    }

    @Test
    void upgradeFreeIpaNoEnvironment() {
        testNoEnvironment(underTest::upgradeCcmInFreeIpaAction, FAILURE_EVENT);
    }

    @Test
    void upgradeDatalakeHappyPath() {
        testUpgradeActionHappyPath(underTest::upgradeCcmInDataLakeAction, UPGRADE_CCM_DATALAKE_HANDLER.selector());
        verify(environmentStatusUpdateService)
                .updateEnvironmentStatusAndNotify(any(), any(),
                        eq(EnvironmentStatus.UPGRADE_CCM_ON_DATALAKE_IN_PROGRESS),
                        eq(ResourceEvent.ENVIRONMENT_UPGRADE_CCM_ON_DATALAKE_STARTED),
                        any());
    }

    @Test
    void upgradeDatalakeNoEnvironment() {
        testNoEnvironment(underTest::upgradeCcmInDataLakeAction, FAILURE_EVENT);
    }

    @Test
    void upgradeDatahubHappyPath() {
        testUpgradeActionHappyPath(underTest::upgradeCcmInDataHubAction, UPGRADE_CCM_DATAHUB_HANDLER.selector());
        verify(environmentStatusUpdateService)
                .updateEnvironmentStatusAndNotify(any(), any(),
                        eq(EnvironmentStatus.UPGRADE_CCM_ON_DATAHUB_IN_PROGRESS),
                        eq(ResourceEvent.ENVIRONMENT_UPGRADE_CCM_ON_DATAHUB_STARTED),
                        any());
    }

    @Test
    void upgradeDatahubNoEnvironment() {
        testNoEnvironment(underTest::upgradeCcmInDataHubAction, FAILURE_EVENT);
    }

    @Test
    void finishedHappyPath() {
        testUpgradeActionHappyPath(underTest::finishedAction, FINALIZE_UPGRADE_CCM_EVENT.selector());
        verify(metricService).incrementMetricCounter(eq(MetricType.ENV_UPGRADE_CCM_FINISHED), any(EnvironmentDto.class));
        verify(environmentStatusUpdateService)
                .updateEnvironmentStatusAndNotify(any(), any(),
                        eq(EnvironmentStatus.AVAILABLE),
                        eq(ResourceEvent.ENVIRONMENT_UPGRADE_CCM_FINISHED),
                        any());
    }

    @Test
    void finishedNoEnvironment() {
        testNoEnvironment(underTest::finishedAction, FAILURE_EVENT);
    }

    @Test
    void failureAction() {
        EnvironmentDto environmentDto = mock(EnvironmentDto.class);
        IllegalStateException failureException = new IllegalStateException(MESSAGE);
        UpgradeCcmFailedEvent failedActionPayload =
                new UpgradeCcmFailedEvent(environmentDto, failureException, EnvironmentStatus.UPGRADE_CCM_FAILED);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.DATA.name())).thenReturn(failedActionPayload);
        testUpgradeActionHappyPath(underTest::failedAction, HANDLED_FAILED_UPGRADE_CCM_EVENT.selector());
        verify(environmentStatusUpdateService)
                .updateFailedEnvironmentStatusAndNotify(any(), any(),
                        eq(EnvironmentStatus.UPGRADE_CCM_FAILED),
                        eq(ResourceEvent.ENVIRONMENT_UPGRADE_CCM_FAILED),
                        any());
        verify(metricService).incrementMetricCounter(eq(MetricType.ENV_UPGRADE_CCM_FAILED), (EnvironmentDto) any(), eq(failureException));
    }

    private void testNoEnvironment(Supplier<Action<?, ?>> upgradeAction, String selector) {
        when(environmentStatusUpdateService.updateEnvironmentStatusAndNotify(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException(MESSAGE));
        when(failureEvent.event()).thenReturn(FAILURE_EVENT);
        Action<?, ?> action = configureAction(upgradeAction);

        action.execute(stateContext);

        verify(metricService, never()).incrementMetricCounter(any(MetricType.class), (EnvironmentDto) any());
        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(reactorEventFactory).createEvent(headersArgumentCaptor.capture(), payloadArgumentCaptor.capture());
        verifyFailureEvent();
        assertThat(selectorArgumentCaptor.getValue()).isEqualTo(selector);
    }

    private void testUpgradeActionHappyPath(Supplier<Action<?, ?>> upgradeAction, String selector) {

        Action<?, ?> action = configureAction(upgradeAction);

        EnvironmentDto environmentDto = mock(EnvironmentDto.class);
        lenient().when(environmentStatusUpdateService.updateEnvironmentStatusAndNotify(any(), any(), any(), any(), any())).thenReturn(environmentDto);
        lenient().when(environmentStatusUpdateService.updateFailedEnvironmentStatusAndNotify(any(), any(), any(), any(), any())).thenReturn(environmentDto);

        action.execute(stateContext);

        verify(eventBus).notify(selectorArgumentCaptor.capture(), eventArgumentCaptor.capture());
        verify(reactorEventFactory).createEvent(headersArgumentCaptor.capture(), payloadArgumentCaptor.capture());
        assertThat(selectorArgumentCaptor.getValue()).isEqualTo(selector);
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

}
