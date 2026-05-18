package com.sequenceiq.freeipa.flow.freeipa.backup.full.action;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_FULL_BACKUP_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_FULL_BACKUP_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_FULL_BACKUP_STARTED;
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
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.freeipa.backup.full.BackupContext;
import com.sequenceiq.freeipa.flow.freeipa.backup.full.event.TriggerFullBackupEvent;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

@ExtendWith(MockitoExtension.class)
class FullBackupActionsTest {

    @InjectMocks
    private FullBackupActions underTest;

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

    private BackupContext context;

    @BeforeEach
    void setUp() {
        context = new BackupContext(new FlowParameters("flow", "user-crn"), stack);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), "ok")).when(reactorEventFactory).createEvent(any(), any());
    }

    @Test
    void backupActionSendsStartedNotification() throws Exception {
        TriggerFullBackupEvent payload = new TriggerFullBackupEvent("selector", 1L, "op-1", false, true);
        Map<Object, Object> variables = new HashMap<>();

        AbstractBackupAction<TriggerFullBackupEvent> action =
                (AbstractBackupAction<TriggerFullBackupEvent>) underTest.backupAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_FULL_BACKUP_STARTED);
    }

    @Test
    void backupFinishedActionSendsFinishedNotification() throws Exception {
        StackEvent payload = new StackEvent("selector", 1L);
        Map<Object, Object> variables = new HashMap<>();

        AbstractBackupAction<StackEvent> action =
                (AbstractBackupAction<StackEvent>) underTest.backupFinishedAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_FULL_BACKUP_FINISHED);
    }

    @Test
    void backupFailedActionSendsFailedNotification() throws Exception {
        StackFailureEvent payload = new StackFailureEvent(1L, new Exception("boom"), ERROR);
        Map<Object, Object> variables = new HashMap<>();

        AbstractBackupAction<StackFailureEvent> action =
                (AbstractBackupAction<StackFailureEvent>) underTest.backupFailedAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_FULL_BACKUP_FAILED, List.of("boom"));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}

