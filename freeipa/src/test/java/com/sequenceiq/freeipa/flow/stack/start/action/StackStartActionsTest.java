package com.sequenceiq.freeipa.flow.stack.start.action;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_START_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_START_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_START_STARTED;
import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
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
import com.sequenceiq.freeipa.flow.stack.HealthCheckSuccess;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureContext;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.start.StackStartContext;
import com.sequenceiq.freeipa.flow.stack.start.StackStartEvent;
import com.sequenceiq.freeipa.flow.stack.start.StackStartService;
import com.sequenceiq.freeipa.flow.stack.start.StackStartState;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@ExtendWith(MockitoExtension.class)
class StackStartActionsTest {

    @InjectMocks
    private StackStartActions underTest;

    @Mock
    private StackStartService stackStartService;

    @Mock
    private ResourceService resourceService;

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

    private StackStartContext context;

    private StackFailureContext failureContext;

    @BeforeEach
    void setUp() {
        context = new StackStartContext(new FlowParameters("flow", "user-crn"), stack, Set.of(), cloudContext, cloudCredential);
        failureContext = new StackFailureContext(new FlowParameters("flow", "user-crn"), stack);
        doReturn(1L).when(stack).getId();
    }

    @Test
    void stackStartActionSendsStartedNotification() throws Exception {
        StackEvent payload = new StackEvent("selector", 1L);
        Map<Object, Object> variables = new HashMap<>();

        AbstractStackStartAction<StackEvent> action =
                (AbstractStackStartAction<StackEvent>) underTest.stackStartAction();
        initActionPrivateFields(action);
        doReturn(Set.of()).when(stack).getNotDeletedInstanceMetaDataSet();
        doReturn(List.of()).when(resourceService).findAllByStackId(1L);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), "ok")).when(reactorEventFactory).createEvent(any(), any());

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_START_STARTED);
    }

    @Test
    void startFinishedActionSendsFinishedNotification() throws Exception {
        HealthCheckSuccess payload = new HealthCheckSuccess(1L, List.of());
        Map<Object, Object> variables = new HashMap<>();

        AbstractStackStartAction<HealthCheckSuccess> action =
                (AbstractStackStartAction<HealthCheckSuccess>) underTest.startFinishedAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_START_FINISHED);
    }

    @Test
    void stackStartFailedActionSendsFailedNotification() throws Exception {
        StackFailureEvent payload = new StackFailureEvent(1L, new Exception("boom"), ERROR);
        Map<Object, Object> variables = new HashMap<>();

        Action<?, ?> rawAction = underTest.stackStartFailedAction();
        initActionPrivateFields(rawAction);

        @SuppressWarnings("unchecked")
        AbstractStackFailureAction<StackStartState, StackStartEvent> action =
                (AbstractStackFailureAction<StackStartState, StackStartEvent>) rawAction;

        new AbstractActionTestSupport<>(action).doExecute(failureContext, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_START_FAILED, List.of("boom"));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, eventSenderService, EventSenderService.class);
    }
}




