package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.action;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_UPGRADE_CCM_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_UPGRADE_CCM_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_UPGRADE_CCM_STARTED;
import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
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
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmService;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFailureEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmTriggerEvent;

@ExtendWith(MockitoExtension.class)
class UpgradeCcmActionsTest {

    @InjectMocks
    private UpgradeCcmActions underTest;

    @Mock
    private UpgradeCcmService upgradeCcmService;

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

    private UpgradeCcmContext context;

    @BeforeEach
    void setUp() {
        context = new UpgradeCcmContext(new FlowParameters("flow", "user-crn"), stack);
        doReturn(1L).when(stack).getId();
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), "ok")).when(reactorEventFactory).createEvent(any(), any());
    }

    @Test
    void checkPrerequisitesSendsStartedNotification() throws Exception {
        UpgradeCcmTriggerEvent payload = new UpgradeCcmTriggerEvent("selector", "op-1", 1L, Tunnel.CCM);
        Map<Object, Object> variables = new HashMap<>();

        AbstractUpgradeCcmAction<UpgradeCcmTriggerEvent> action =
                (AbstractUpgradeCcmAction<UpgradeCcmTriggerEvent>) underTest.checkPrerequisites();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_UPGRADE_CCM_STARTED);
    }

    @Test
    void finishedSendsFinishedNotification() throws Exception {
        UpgradeCcmEvent payload = new UpgradeCcmEvent("selector", 1L, Tunnel.CCM, LocalDateTime.now(), Boolean.TRUE);
        Map<Object, Object> variables = new HashMap<>();

        AbstractUpgradeCcmEventAction action = (AbstractUpgradeCcmEventAction) underTest.finished();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_UPGRADE_CCM_FINISHED);
    }

    @Test
    void failedRevertTunnelSendsFailedNotification() throws Exception {
        UpgradeCcmFailureEvent payload = new UpgradeCcmFailureEvent(
                "selector", 1L, Tunnel.CCM, null, new Exception("boom"), LocalDateTime.now(), "status", ERROR);
        Map<Object, Object> variables = new HashMap<>();

        AbstractUpgradeCcmAction<UpgradeCcmFailureEvent> action =
                (AbstractUpgradeCcmAction<UpgradeCcmFailureEvent>) underTest.failedRevertTunnel();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_UPGRADE_CCM_FAILED, List.of("boom"));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}

