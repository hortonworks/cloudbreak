package com.sequenceiq.cloudbreak.core.flow2.validate.datalake;

import static com.sequenceiq.cloudbreak.core.flow2.validate.datalake.config.DatalakeReachabilityValidationEvent.DATALAKE_REACHABILITY_VALIDATION_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.core.flow2.event.DatalakeReachabilityValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.StackUpdaterService;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.validate.datalake.config.DatalakeReachabilityValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.validate.datalake.config.DatalakeReachabilityValidationState;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.DatalakeReachabilityValidationRequest;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;

@ExtendWith(MockitoExtension.class)
class DatalakeReachabilityValidationActionsTest {

    private static final String FAILURE_STATUS = "DATALAKE_REACHABILITY_VALIDATION_FAILURE_STATUS";

    private static final Long STACK_ID = 1L;

    private static final String ERROR_MESSAGE = "Data Lake is not reachable";

    @InjectMocks
    private DatalakeReachabilityValidationActions underTest;

    @Mock
    private StackUpdaterService stackUpdaterService;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private Stack stack;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private Event<Object> event;

    @Mock
    private CloudbreakMetricService metricService;

    @Mock
    private StackDtoService stackDtoService;

    @Test
    void testValidationActionStoresFailureStatusAndSendsRequest() throws Exception {
        AbstractDatalakeReachabilityValidationAction<DatalakeReachabilityValidationTriggerEvent> action =
                (AbstractDatalakeReachabilityValidationAction<DatalakeReachabilityValidationTriggerEvent>) underTest.datalakeReachabilityValidationAction();
        initActionPrivateFields(action);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);
        DatalakeReachabilityValidationTriggerEvent payload =
                new DatalakeReachabilityValidationTriggerEvent(DATALAKE_REACHABILITY_VALIDATION_EVENT.event(), STACK_ID, DetailedStackStatus.REPAIR_FAILED);
        Map<Object, Object> variables = new HashMap<>();

        new AbstractActionTestSupport<>(action).doExecute(new CommonContext(flowParameters), payload, variables);

        assertThat(variables).containsEntry(FAILURE_STATUS, DetailedStackStatus.REPAIR_FAILED);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), captor.capture());
        verify(eventBus).notify("DATALAKEREACHABILITYVALIDATIONREQUEST", event);
        assertThat(captor.getValue()).isInstanceOf(DatalakeReachabilityValidationRequest.class);
    }

    @Test
    void testFailureActionUsesFailureStatusOfTrigger() throws Exception {
        AbstractStackFailureAction<DatalakeReachabilityValidationState, DatalakeReachabilityValidationEvent> action =
                (AbstractStackFailureAction<DatalakeReachabilityValidationState, DatalakeReachabilityValidationEvent>)
                        underTest.datalakeReachabilityValidationFailureAction();
        initActionPrivateFields(action);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);
        StackFailureContext context = new StackFailureContext(flowParameters, stack, STACK_ID);
        StackFailureEvent payload = new StackFailureEvent(STACK_ID, new RuntimeException(ERROR_MESSAGE));

        new AbstractActionTestSupport<>(action).doExecute(context, payload,
                Map.of(FAILURE_STATUS, DetailedStackStatus.UPSCALE_FAILED));

        verify(stackUpdaterService).updateStatusAndSendEventWithArgs(STACK_ID, DetailedStackStatus.UPSCALE_FAILED,
                ResourceEvent.DATALAKE_REACHABILITY_VALIDATION_FAILED, ERROR_MESSAGE, ERROR_MESSAGE);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), captor.capture());
        verify(eventBus).notify(DatalakeReachabilityValidationEvent.DATALAKE_REACHABILITY_VALIDATION_FAILURE_HANDLED_EVENT.event(), event);
        assertThat(captor.getValue()).isInstanceOf(StackEvent.class);
    }

    @Test
    void testValidationActionCreatesFlowContextFromTheStack() {
        AbstractDatalakeReachabilityValidationAction<DatalakeReachabilityValidationTriggerEvent> action =
                (AbstractDatalakeReachabilityValidationAction<DatalakeReachabilityValidationTriggerEvent>) underTest.datalakeReachabilityValidationAction();
        ReflectionTestUtils.setField(action, "stackDtoService", stackDtoService);
        StackView stackView = mock(StackView.class);
        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stackView);
        DatalakeReachabilityValidationTriggerEvent payload =
                new DatalakeReachabilityValidationTriggerEvent(DATALAKE_REACHABILITY_VALIDATION_EVENT.event(), STACK_ID, DetailedStackStatus.REPAIR_FAILED);

        CommonContext context = action.createFlowContext(flowParameters, null, payload);

        assertThat(context.getFlowParameters()).isSameAs(flowParameters);
        verify(stackDtoService).getStackViewById(STACK_ID);
    }

    @Test
    void testValidationActionFailurePayloadCarriesTheException() {
        AbstractDatalakeReachabilityValidationAction<DatalakeReachabilityValidationTriggerEvent> action =
                (AbstractDatalakeReachabilityValidationAction<DatalakeReachabilityValidationTriggerEvent>) underTest.datalakeReachabilityValidationAction();
        RuntimeException exception = new RuntimeException(ERROR_MESSAGE);
        DatalakeReachabilityValidationTriggerEvent payload =
                new DatalakeReachabilityValidationTriggerEvent(DATALAKE_REACHABILITY_VALIDATION_EVENT.event(), STACK_ID, DetailedStackStatus.REPAIR_FAILED);

        Object result = action.getFailurePayload(payload, Optional.empty(), exception);

        assertThat(result).isInstanceOfSatisfying(StackFailureEvent.class, failure -> {
            assertThat(failure.getResourceId()).isEqualTo(STACK_ID);
            assertThat(failure.getException()).isSameAs(exception);
        });
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, metricService, MetricService.class);
    }
}
