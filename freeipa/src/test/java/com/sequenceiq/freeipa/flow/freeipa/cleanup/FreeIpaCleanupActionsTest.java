package com.sequenceiq.freeipa.flow.freeipa.cleanup;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_CLEANUP_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_CLEANUP_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_CLEANUP_STARTED;
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

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.failure.CleanupFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.roles.RemoveRolesResponse;
import com.sequenceiq.freeipa.service.operation.OperationService;

@ExtendWith(MockitoExtension.class)
class FreeIpaCleanupActionsTest {

    @InjectMocks
    private FreeIpaCleanupActions underTest;

    @Mock
    private EventSenderService eventSenderService;

    @Mock
    private OperationService operationService;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private FreeIpa freeIpa;

    @Mock
    private Stack stack;

    private FreeIpaContext context;

    @BeforeEach
    void setUp() {
        context = new FreeIpaContext(new FlowParameters("flow", "user-crn"), freeIpa);
        doReturn(stack).when(freeIpa).getStack();
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), "ok")).when(reactorEventFactory).createEvent(any(), any());
    }

    @Test
    void revokeCertsStateSendsCleanupStartedWithSortedHosts() throws Exception {
        CleanupEvent payload = new CleanupEvent(1L, Set.of(), Set.of("master1", "master0"), Set.of(), Set.of(), Set.of(), "acc", "op-1",
                "cluster", "env-crn");
        Map<Object, Object> variables = new HashMap<>();

        AbstractFreeIpaCleanupAction<CleanupEvent> action =
                (AbstractFreeIpaCleanupAction<CleanupEvent>) underTest.revokeCertsAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_CLEANUP_STARTED,
                List.of("2", "master0,master1"));
    }

    @Test
    void cleanupFinishedStateSendsCleanupFinishedWithSortedHosts() throws Exception {
        CleanupEvent cleanupEvent = new CleanupEvent(1L, Set.of(), Set.of("master1", "master0"), Set.of(), Set.of(), Set.of(), "acc", "op-1",
                "cluster", "env-crn");
        RemoveRolesResponse payload = new RemoveRolesResponse(cleanupEvent, Set.of(), Map.of());
        Map<Object, Object> variables = new HashMap<>();

        AbstractFreeIpaCleanupAction<RemoveRolesResponse> action =
                (AbstractFreeIpaCleanupAction<RemoveRolesResponse>) underTest.cleanupFinishedAction();
        initActionPrivateFields(action);
        ReflectionTestUtils.setField(action, null, operationService, OperationService.class);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_CLEANUP_FINISHED,
                List.of("2", "master0,master1"));
    }

    @Test
    void cleanupFailureStateSendsCleanupFailedNotificationWithPhaseAndReason() throws Exception {
        CleanupEvent cleanupEvent = new CleanupEvent(1L, Set.of(), Set.of("master1", "master0"), Set.of(), Set.of(), Set.of(), "acc", "op-1",
                "cluster", "env-crn");
        CleanupFailureEvent payload = new CleanupFailureEvent(cleanupEvent, "REMOVE_DNS", Map.of(), Set.of("master0"));
        Map<Object, Object> variables = new HashMap<>();

        AbstractFreeIpaCleanupAction<CleanupFailureEvent> action =
                (AbstractFreeIpaCleanupAction<CleanupFailureEvent>) underTest.cleanupFailureAction();
        initActionPrivateFields(action);
        ReflectionTestUtils.setField(action, null, operationService, OperationService.class);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_CLEANUP_FAILED,
                List.of("REMOVE_DNS", "Unknown error"));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, eventSenderService, EventSenderService.class);
    }
}



