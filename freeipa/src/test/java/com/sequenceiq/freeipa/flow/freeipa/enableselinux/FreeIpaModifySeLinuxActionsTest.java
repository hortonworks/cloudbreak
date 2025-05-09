package com.sequenceiq.freeipa.flow.freeipa.enableselinux;

import static com.sequenceiq.freeipa.flow.OperationAwareAction.OPERATION_ID;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxStateSelectors.FINALIZE_MODIFY_SELINUX_FREEIPA_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxStateSelectors.FINISH_MODIFY_SELINUX_FREEIPA_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxStateSelectors.HANDLED_FAILED_MODIFY_SELINUX_FREEIPA_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxStateSelectors.MODIFY_SELINUX_FREEIPA_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxStateSelectors.MODIFY_SELINUX_START_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxEvent;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxFailedEvent;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxHandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaValidateModifySeLinuxHandlerEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.sync.FreeipaJobService;

@ExtendWith(MockitoExtension.class)
public class FreeIpaModifySeLinuxActionsTest {

    private Map<Object, Object> variables;

    @Mock
    private FlowMessageService flowMessageService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private OperationService operationService;

    @InjectMocks
    private FreeIpaModifySeLinuxActions underTest;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    private StackContext context;

    @Mock
    private FlowParameters flowParameters;

    @Captor
    private ArgumentCaptor<String> captor;

    @Mock
    private Stack stack;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @BeforeEach
    void setUp() {
        variables = new HashMap<>();
        context = new StackContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
    }

    @Test
    void testModifySeLinuxValidationAction() throws Exception {
        FreeIpaModifySeLinuxEvent event = new FreeIpaModifySeLinuxEvent(MODIFY_SELINUX_START_EVENT.event(), 1L, "test-op", SeLinux.ENFORCING);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        AbstractFreeIpaModifySeLinuxAction<FreeIpaModifySeLinuxEvent> action =
                (AbstractFreeIpaModifySeLinuxAction<FreeIpaModifySeLinuxEvent>) underTest.modifySeLinuxValidationAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(stackUpdater).updateStackStatus(eq(stack), eq(DetailedStackStatus.UPDATE_IN_PROGRESS), eq("Starting to validate SELinux mode change."));
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        String selector = EventSelectorUtil.selector(FreeIpaValidateModifySeLinuxHandlerEvent.class);
        assertEquals(selector, captor.getValue());
        assertEquals(1L, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testModifySeLinuxInFreeIpaAction() throws Exception {
        FreeIpaModifySeLinuxEvent event = new FreeIpaModifySeLinuxEvent(MODIFY_SELINUX_FREEIPA_EVENT.event(), 1L, "test-op", SeLinux.ENFORCING);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        AbstractFreeIpaModifySeLinuxAction<FreeIpaModifySeLinuxEvent> action =
                (AbstractFreeIpaModifySeLinuxAction<FreeIpaModifySeLinuxEvent>) underTest.modifySeLinuxInFreeIpaAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(stackUpdater).updateStackStatus(eq(stack), eq(DetailedStackStatus.UPDATE_IN_PROGRESS), eq("Starting to modify SELinux mode change."));
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        String selector = EventSelectorUtil.selector(FreeIpaModifySeLinuxHandlerEvent.class);
        assertEquals(selector, captor.getValue());
        assertEquals(1L, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testFinishedAction() throws Exception {
        FreeIpaModifySeLinuxEvent event = new FreeIpaModifySeLinuxEvent(FINISH_MODIFY_SELINUX_FREEIPA_EVENT.event(), 1L, "test-op", SeLinux.ENFORCING);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        doReturn("test-crn").when(stack).getResourceCrn();
        doReturn("test-env-crn").when(stack).getEnvironmentCrn();
        doReturn("test-account").when(stack).getAccountId();
        AbstractFreeIpaModifySeLinuxAction<FreeIpaModifySeLinuxEvent> action =
                (AbstractFreeIpaModifySeLinuxAction<FreeIpaModifySeLinuxEvent>) underTest.finishedAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(stackUpdater).updateStackStatus(eq(stack), eq(DetailedStackStatus.UPDATE_COMPLETE), eq("Finished setting SELinux mode to " +
                "ENFORCING for stack test-crn"));
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        verify(operationService).completeOperation(eq("test-account"), eq("test-op"), anySet(), anySet());
        String selector = FINALIZE_MODIFY_SELINUX_FREEIPA_EVENT.event();
        assertEquals(selector, captor.getValue());
        assertEquals(1L, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testFailedAction() throws Exception {
        FreeIpaModifySeLinuxFailedEvent event = new FreeIpaModifySeLinuxFailedEvent(1L, "test-op", new CloudbreakException("test"));
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        AbstractFreeIpaModifySeLinuxAction<FreeIpaModifySeLinuxFailedEvent> action =
                (AbstractFreeIpaModifySeLinuxAction<FreeIpaModifySeLinuxFailedEvent>) underTest.failedAction();
        initActionPrivateFields(action);
        ReflectionTestUtils.setField(action, "jobService", mock(FreeipaJobService.class), FreeipaJobService.class);
        variables.put(OPERATION_ID, "test-op");
        doReturn("test-account").when(stack).getAccountId();
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(stackUpdater).updateStackStatus(eq(stack), eq(DetailedStackStatus.UPDATE_FAILED), eq("test"));
        verify(operationService).failOperation(eq("test-account"), eq("test-op"), eq("Updating SELinux failed during test-op"),
                any(), any());
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        String selector = HANDLED_FAILED_MODIFY_SELINUX_FREEIPA_EVENT.event();
        assertEquals(selector, captor.getValue());
        assertEquals(1L, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}
