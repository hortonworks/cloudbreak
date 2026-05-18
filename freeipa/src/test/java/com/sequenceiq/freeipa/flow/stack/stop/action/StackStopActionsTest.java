package com.sequenceiq.freeipa.flow.stack.stop.action;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_STOP_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_STOP_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_STOP_STARTED;
import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.stack.AbstractStackFailureAction;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureContext;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.stop.StackStopContext;
import com.sequenceiq.freeipa.flow.stack.stop.StackStopEvent;
import com.sequenceiq.freeipa.flow.stack.stop.StackStopService;
import com.sequenceiq.freeipa.flow.stack.stop.StackStopState;

@ExtendWith(MockitoExtension.class)
class StackStopActionsTest {

    @InjectMocks
    private StackStopActions underTest;

    @Mock
    private StackStopService stackStopService;

    @Mock
    private EventSenderService eventSenderService;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private Stack stack;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    private StackStopContext context;

    private StackFailureContext failureContext;

    @BeforeEach
    void setUp() {
        context = new StackStopContext(new FlowParameters("flow", "user-crn"), stack, List.of(), cloudContext, cloudCredential);
        failureContext = new StackFailureContext(new FlowParameters("flow", "user-crn"), stack);
        doReturn(1L).when(stack).getId();
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), "ok")).when(reactorEventFactory).createEvent(any(), any());
    }

    @Test
    void stackStopActionSendsStartedNotification() throws Exception {
        StackEvent payload = new StackEvent("selector", 1L);
        Map<Object, Object> variables = new HashMap<>();

        AbstractStackStopAction<StackEvent> action =
                (AbstractStackStopAction<StackEvent>) underTest.stackStopAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_STOP_STARTED);
    }

    @Test
    void stackStopFinishedActionSendsFinishedNotification() throws Exception {
        StopInstancesResult payload = new StopInstancesResult(1L, null);
        Map<Object, Object> variables = new HashMap<>();

        AbstractStackStopAction<StopInstancesResult> action =
                (AbstractStackStopAction<StopInstancesResult>) underTest.stackStopFinishedAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_STOP_FINISHED);
    }

    @Test
    void stackStopFailedActionSendsFailedNotification() throws Exception {
        StackFailureEvent payload = new StackFailureEvent(1L, new Exception("boom"), ERROR);
        Map<Object, Object> variables = new HashMap<>();

        Action<?, ?> rawAction = underTest.stackStopFailedAction();
        initActionPrivateFields(rawAction);

        @SuppressWarnings("unchecked")
        AbstractStackFailureAction<StackStopState, StackStopEvent> action =
                (AbstractStackFailureAction<StackStopState, StackStopEvent>) rawAction;

        new AbstractActionTestSupport<>(action).doExecute(failureContext, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_STOP_FAILED, List.of("boom"));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, eventSenderService, EventSenderService.class);
    }
}


