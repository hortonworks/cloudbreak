package com.sequenceiq.environment.environment.flow.verticalscale.freeipa;

import static com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleHandlerSelectors.VERTICAL_SCALING_FREEIPA_HANDLER;
import static com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleHandlerSelectors.VERTICAL_SCALING_FREEIPA_VALIDATION_HANDLER;
import static com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleStateSelectors.FINALIZE_VERTICAL_SCALING_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleStateSelectors.HANDLED_FAILED_VERTICAL_SCALING_FREEIPA_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import com.sequenceiq.cloudbreak.eventbus.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.common.api.type.OrchestratorType;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleEvent;
import com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.metrics.EnvironmentMetricService;
import com.sequenceiq.environment.metrics.MetricType;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.MessageFactory;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
class EnvironmentVerticalScaleActionsTest {

    private static final Long ENVIRONMENT_ID = 1234L;

    private static final String ENVIRONMENT_NAME = "environmentName";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String FLOW_ID = "flowId";

    private static final String FLOW_TRIGGER_USER_CRN = "flowTriggerUserCrn";

    private static final String SELECTOR = "selector";

    private static final String INSTANCE_GROUP = "master";

    private static final String INSTANCE_TYPE = "m5.xlarge";

    private static final String FAILURE_MESSAGE = "something went wrong";

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

    @Mock
    private FreeIpaService freeIpaService;

    @InjectMocks
    private EnvironmentVerticalScaleActions underTest;

    private Map<Object, Object> variables;

    @BeforeEach
    void setUp() {
        FlowParameters flowParameters = new FlowParameters(FLOW_ID, FLOW_TRIGGER_USER_CRN);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.FLOW_PARAMETERS.name())).thenReturn(flowParameters);
        variables = new HashMap<>();
        when(stateContext.getExtendedState()).thenReturn(extendedState);
        when(extendedState.getVariables()).thenReturn(variables);
        when(stateContext.getStateMachine()).thenReturn(stateMachine);
        when(stateMachine.getState()).thenReturn(state);
        lenient().when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);
        lenient().when(freeIpaService.describe(any())).thenReturn(Optional.empty());
    }

    @Test
    void validationNonRollingUpdatesStatus() {
        setPayload(nonRollingRequest());
        EnvironmentDto environmentDto = mock(EnvironmentDto.class);
        when(environmentStatusUpdateService.updateEnvironmentStatusAndNotify(any(), any(), any(), any(), any()))
                .thenReturn(environmentDto);

        executeAction(underTest::verticalScaleValidationAction);

        verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(any(), any(),
                eq(EnvironmentStatus.VERTICAL_SCALE_VALIDATION_IN_PROGRESS),
                eq(ResourceEvent.ENVIRONMENT_VERTICAL_SCALE_VALIDATION_STARTED),
                any());
        assertThat(variables).containsEntry("IS_ROLLING_SCALE", false);
        verifySentEvent(VERTICAL_SCALING_FREEIPA_VALIDATION_HANDLER.selector());
    }

    @Test
    void validationRollingNotifiesWithoutStatusUpdate() {
        setPayload(rollingRequest());

        executeAction(underTest::verticalScaleValidationAction);

        verify(environmentStatusUpdateService, never()).updateEnvironmentStatusAndNotify(any(), any(), any(), any(), any());
        verify(environmentStatusUpdateService).notifyOnly(any(), any(),
                eq(ResourceEvent.ENVIRONMENT_VERTICAL_SCALE_VALIDATION_STARTED), any());
        assertThat(variables).containsEntry("IS_ROLLING_SCALE", true);
        verifySentEvent(VERTICAL_SCALING_FREEIPA_VALIDATION_HANDLER.selector());
    }

    @Test
    void inProgressNonRollingUpdatesStatus() {
        variables.put("IS_ROLLING_SCALE", false);
        setPayload(nonRollingRequest());
        EnvironmentDto environmentDto = mock(EnvironmentDto.class);
        when(environmentStatusUpdateService.updateEnvironmentStatusAndNotify(any(), any(), any(), any(), any(), any()))
                .thenReturn(environmentDto);

        executeAction(underTest::verticalScaleInFreeIpaAction);

        verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(any(), any(),
                eq(EnvironmentStatus.VERTICAL_SCALE_ON_FREEIPA_IN_PROGRESS),
                eq(ResourceEvent.ENVIRONMENT_VERTICAL_SCALE_ON_FREEIPA_STARTED),
                any(), any());
        verifySentEvent(VERTICAL_SCALING_FREEIPA_HANDLER.selector());
    }

    @Test
    void inProgressRollingNotifiesWithoutStatusUpdate() {
        variables.put("IS_ROLLING_SCALE", true);
        setPayload(rollingRequest());

        executeAction(underTest::verticalScaleInFreeIpaAction);

        verify(environmentStatusUpdateService, never()).updateEnvironmentStatusAndNotify(any(), any(),
                eq(EnvironmentStatus.VERTICAL_SCALE_ON_FREEIPA_IN_PROGRESS), any(), any(), any());
        verify(environmentStatusUpdateService).notifyOnly(any(), any(),
                eq(ResourceEvent.ENVIRONMENT_VERTICAL_SCALE_ON_FREEIPA_STARTED), any(), any());
        verifySentEvent(VERTICAL_SCALING_FREEIPA_HANDLER.selector());
    }

    @Test
    void finishedNonRollingGoesToEnvStopped() {
        variables.put("IS_ROLLING_SCALE", false);
        setPayload(nonRollingRequest());
        EnvironmentDto environmentDto = mock(EnvironmentDto.class);
        when(environmentStatusUpdateService.updateEnvironmentStatusAndNotify(any(), any(), any(), any(), any(), any()))
                .thenReturn(environmentDto);

        executeAction(underTest::finishedAction);

        verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(any(), any(),
                eq(EnvironmentStatus.ENV_STOPPED),
                eq(ResourceEvent.ENVIRONMENT_VERTICAL_SCALE_FINISHED),
                any(), any());
        verify(metricService).incrementMetricCounter(eq(MetricType.ENV_VERTICAL_SCALE_FINISHED), any(EnvironmentDto.class));
        verifySentEvent(FINALIZE_VERTICAL_SCALING_FREEIPA_EVENT.event());
    }

    @Test
    void finishedRollingNotifiesWithoutStatusUpdate() {
        variables.put("IS_ROLLING_SCALE", true);
        setPayload(rollingRequest());
        EnvironmentDto environmentDto = mock(EnvironmentDto.class);
        when(environmentStatusUpdateService.notifyOnly(any(), any(), any(), any(), any()))
                .thenReturn(environmentDto);

        executeAction(underTest::finishedAction);

        verify(environmentStatusUpdateService, never()).updateEnvironmentStatusAndNotify(any(), any(), any(), any(), any(), any());
        verify(environmentStatusUpdateService).notifyOnly(any(), any(),
                eq(ResourceEvent.ENVIRONMENT_VERTICAL_SCALE_FINISHED),
                any(), any());
        verify(metricService).incrementMetricCounter(eq(MetricType.ENV_VERTICAL_SCALE_FINISHED), any(EnvironmentDto.class));
        verifySentEvent(FINALIZE_VERTICAL_SCALING_FREEIPA_EVENT.event());
    }

    @Test
    void failedNonRollingUsesFailedStatusUpdate() {
        variables.put("IS_ROLLING_SCALE", false);
        RuntimeException exception = new RuntimeException(FAILURE_MESSAGE);
        setFailedPayload(nonRollingRequest(), exception, EnvironmentStatus.VERTICAL_SCALE_ON_FREEIPA_FAILED);
        EnvironmentDto environmentDto = mock(EnvironmentDto.class);
        when(environmentStatusUpdateService.updateFailedEnvironmentStatusAndNotify(any(), any(), any(), any(), any(), any()))
                .thenReturn(environmentDto);

        executeAction(underTest::failedAction);

        verify(environmentStatusUpdateService).updateFailedEnvironmentStatusAndNotify(any(), any(),
                eq(EnvironmentStatus.VERTICAL_SCALE_ON_FREEIPA_FAILED),
                eq(ResourceEvent.ENVIRONMENT_VERTICAL_SCALE_ON_FREEIPA_FAILED),
                any(), any());
        verify(environmentStatusUpdateService, never()).updateEnvironmentStatusAndNotify(any(), any(),
                eq(EnvironmentStatus.AVAILABLE), any(), any(), any());
        verify(metricService).incrementMetricCounter(eq(MetricType.ENV_VERTICAL_SCALE_FAILED), any(EnvironmentDto.class), eq(exception));
        verifySentEvent(HANDLED_FAILED_VERTICAL_SCALING_FREEIPA_EVENT.event());
    }

    @Test
    void failedRollingNotifiesWithoutStatusUpdate() {
        variables.put("IS_ROLLING_SCALE", true);
        RuntimeException exception = new RuntimeException(FAILURE_MESSAGE);
        setFailedPayload(rollingRequest(), exception, EnvironmentStatus.VERTICAL_SCALE_ON_FREEIPA_FAILED);
        EnvironmentDto environmentDto = mock(EnvironmentDto.class);
        when(environmentStatusUpdateService.notifyOnly(any(), any(), any(), any(), any()))
                .thenReturn(environmentDto);

        executeAction(underTest::failedAction);

        verify(environmentStatusUpdateService).notifyOnly(any(), any(),
                eq(ResourceEvent.ENVIRONMENT_VERTICAL_SCALE_ON_FREEIPA_FAILED),
                any(), any());
        verify(environmentStatusUpdateService, never()).updateFailedEnvironmentStatusAndNotify(any(), any(), any(), any(), any(), any());
        verify(metricService).incrementMetricCounter(eq(MetricType.ENV_VERTICAL_SCALE_FAILED), any(EnvironmentDto.class), eq(exception));
        verifySentEvent(HANDLED_FAILED_VERTICAL_SCALING_FREEIPA_EVENT.event());
    }

    private VerticalScaleRequest nonRollingRequest() {
        InstanceTemplateRequest template = new InstanceTemplateRequest();
        template.setInstanceType(INSTANCE_TYPE);
        VerticalScaleRequest req = new VerticalScaleRequest();
        req.setGroup(INSTANCE_GROUP);
        req.setTemplate(template);
        return req;
    }

    private VerticalScaleRequest rollingRequest() {
        VerticalScaleRequest req = nonRollingRequest();
        req.setOrchestratorType(OrchestratorType.ONE_BY_ONE.name());
        return req;
    }

    private void setPayload(VerticalScaleRequest request) {
        EnvironmentVerticalScaleEvent payload = EnvironmentVerticalScaleEvent.builder()
                .withSelector(SELECTOR)
                .withResourceId(ENVIRONMENT_ID)
                .withResourceName(ENVIRONMENT_NAME)
                .withResourceCrn(ENVIRONMENT_CRN)
                .withFreeIPAVerticalScaleRequest(request)
                .build();
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.DATA.name())).thenReturn(payload);
    }

    private void setFailedPayload(VerticalScaleRequest request, Exception exception, EnvironmentStatus status) {
        EnvironmentVerticalScaleEvent inner = EnvironmentVerticalScaleEvent.builder()
                .withSelector(SELECTOR)
                .withResourceId(ENVIRONMENT_ID)
                .withResourceName(ENVIRONMENT_NAME)
                .withResourceCrn(ENVIRONMENT_CRN)
                .withFreeIPAVerticalScaleRequest(request)
                .build();
        EnvironmentVerticalScaleFailedEvent payload = new EnvironmentVerticalScaleFailedEvent(inner, exception, status);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.DATA.name())).thenReturn(payload);
    }

    private void executeAction(Supplier<Action<?, ?>> actionSupplier) {
        Action<?, ?> action = actionSupplier.get();
        assertThat(action).isNotNull();
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ((AbstractAction) action).setFailureEvent(failureEvent);
        action.execute(stateContext);
    }

    private void verifySentEvent(String expectedSelector) {
        verify(eventBus).notify(eq(expectedSelector), any());
    }
}
