package com.sequenceiq.freeipa.flow.freeipa.binduser.create.action;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_BIND_USER_CREATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_BIND_USER_CREATE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_BIND_USER_CREATE_STARTED;
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

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserEvent;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserFailureEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class CreateBindUserActionsTest {

    @InjectMocks
    private CreateBindUserActions underTest;

    @Mock
    private EventSenderService eventSenderService;

    @Mock
    private StackService stackService;

    @Mock
    private OperationService operationService;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private Stack stack;

    private CommonContext context;

    @BeforeEach
    void setUp() {
        context = new CommonContext(new FlowParameters("flow", "user-crn"));
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), "ok")).when(reactorEventFactory).createEvent(any(), any());
        doReturn(stack).when(stackService).getStackById(1L);
    }

    @Test
    void createKerberosBindUserSendsStartedNotificationWithSuffix() throws Exception {
        CreateBindUserEvent payload = new CreateBindUserEvent("selector", 1L, "acc", "op-1", "dc=example,dc=com", "env-crn");
        Map<Object, Object> variables = new HashMap<>();

        AbstractBindUserCreateAction<CreateBindUserEvent> action =
                (AbstractBindUserCreateAction<CreateBindUserEvent>) underTest.createKerberosBindUserAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_BIND_USER_CREATE_STARTED,
                List.of("dc=example,dc=com"));
    }

    @Test
    void createBindUserFinishedSendsFinishedNotificationWithSuffix() throws Exception {
        CreateBindUserEvent payload = new CreateBindUserEvent("selector", 1L, "acc", "op-1", "dc=example,dc=com", "env-crn");
        Map<Object, Object> variables = new HashMap<>();

        AbstractBindUserCreateAction<CreateBindUserEvent> action =
                (AbstractBindUserCreateAction<CreateBindUserEvent>) underTest.createBindUserFinishedAction();
        initActionPrivateFields(action);
        ReflectionTestUtils.setField(action, null, operationService, OperationService.class);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_BIND_USER_CREATE_FINISHED,
                List.of("dc=example,dc=com"));
    }

    @Test
    void createBindUserFailedSendsFailedNotificationWithSuffixAndReason() throws Exception {
        Exception exception = new Exception("boom");
        CreateBindUserFailureEvent payload = new CreateBindUserFailureEvent(
                "selector", 1L, "acc", "op-1", "dc=example,dc=com", "env-crn", "failure", exception);
        Map<Object, Object> variables = new HashMap<>();

        AbstractBindUserCreateAction<CreateBindUserFailureEvent> action =
                (AbstractBindUserCreateAction<CreateBindUserFailureEvent>) underTest.createBindUserFailureAction();
        initActionPrivateFields(action);
        ReflectionTestUtils.setField(action, null, operationService, OperationService.class);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_BIND_USER_CREATE_FAILED,
                List.of("dc=example,dc=com", "boom"));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}


