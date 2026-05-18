package com.sequenceiq.freeipa.flow.stack.dynamicentitlement;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_REFRESH_ENTITLEMENT_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_REFRESH_ENTITLEMENT_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_REFRESH_ENTITLEMENT_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_UPGRADE_FAILED;
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

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.sync.dynamicentitlement.DynamicEntitlementRefreshService;

@ExtendWith(MockitoExtension.class)
class RefreshEntitlementParamsActionsTest {

    @InjectMocks
    private RefreshEntitlementParamsActions underTest;

    @Mock
    private DynamicEntitlementRefreshService dynamicEntitlementRefreshService;

    @Mock
    private StackUpdater stackUpdater;

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

    private RefreshEntitlementParamsContext context;

    @BeforeEach
    void setUp() {
        context = new RefreshEntitlementParamsContext(new FlowParameters("flow", "user-crn"), stack, null, null, null);
    }

    @Test
    void refreshFreeIpaEntitlementActionSendsStartedAndFinishedNotifications() throws Exception {
        @SuppressWarnings("unchecked")
        AbstractRefreshEntitlementParamsAction<RefreshEntitlementParamsTriggerEvent> action =
                (AbstractRefreshEntitlementParamsAction<RefreshEntitlementParamsTriggerEvent>) underTest.refreshFreeIPAEntitlement();
        initActionPrivateFields(action);

        RefreshEntitlementParamsTriggerEvent payload = new RefreshEntitlementParamsTriggerEvent(
                "selector", 1L, Map.of("entitlement", Boolean.TRUE), null, false, false, "op-1");
        Map<Object, Object> variables = new HashMap<>();
        doReturn(1L).when(stack).getId();
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), "ok")).when(reactorEventFactory).createEvent(any(), any());

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_REFRESH_ENTITLEMENT_STARTED);
        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_REFRESH_ENTITLEMENT_FINISHED);
    }

    @Test
    void refreshEntitlementFailedActionSendsFailedNotifications() throws Exception {
        @SuppressWarnings("unchecked")
        AbstractRefreshEntitlementParamsAction<StackFailureEvent> action =
                (AbstractRefreshEntitlementParamsAction<StackFailureEvent>) underTest.refreshEntitlementFailedAction();
        initActionPrivateFields(action);

        StackFailureEvent payload = new StackFailureEvent(1L, new Exception("boom"), ERROR);
        Map<Object, Object> variables = new HashMap<>();
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), "ok")).when(reactorEventFactory).createEvent(any(), any());

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.UPGRADE_FAILED, "Refresh entitlement based configurations failed: boom");
        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_REFRESH_ENTITLEMENT_FAILED, List.of("boom"));
        verify(eventSenderService).sendEventAndNotification(
                stack,
                "user-crn",
                FREEIPA_UPGRADE_FAILED,
                List.of("Refresh entitlement based configurations failed: boom")
        );
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, eventSenderService, EventSenderService.class);
    }
}


