package com.sequenceiq.freeipa.flow.stack.modify.tags;

import static com.sequenceiq.freeipa.flow.OperationAwareAction.OPERATION_ID;
import static com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.FINALIZE_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT;
import static com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.FINISH_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT;
import static com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT;
import static com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.MODIFY_USER_DEFINED_TAGS_FREEIPA_STACK_EVENT;
import static com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.MODIFY_USER_DEFINED_TAGS_FREEIPA_START_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsCloudResourcesHandlerEvent;
import com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsEvent;
import com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsFailedEvent;
import com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsStackHandlerEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.sync.FreeipaJobService;

@ExtendWith(MockitoExtension.class)
class ModifyUserDefinedTagsActionsTest {
    private static final Map<String, String> USER_DEFINED_TAGS = Map.of("custom", "value");

    private static final long STACK_ID = 1L;

    private Map<Object, Object> variables;

    private StackContext context;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

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

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private OperationService operationService;

    @InjectMocks
    private ModifyUserDefinedTagsActions underTest;

    @BeforeEach
    void setUp() {
        variables = new HashMap<>();
        context = new StackContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
    }

    @Test
    void testModifyCloudResourcesAction() throws Exception {
        ModifyUserDefinedTagsEvent event = new ModifyUserDefinedTagsEvent(MODIFY_USER_DEFINED_TAGS_FREEIPA_START_EVENT.event(), STACK_ID,
                "Cloud resources", USER_DEFINED_TAGS);
        when(stack.getName()).thenReturn("stack-1");
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        AbstractModifyUserDefinedTagsAction<ModifyUserDefinedTagsEvent> action =
                (AbstractModifyUserDefinedTagsAction<ModifyUserDefinedTagsEvent>) underTest.modifyCloudResourcesAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.UPDATE_IN_PROGRESS,
                "Starting to update user defined tags on cloud resources of FreeIPA: stack-1");
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        String selector = EventSelectorUtil.selector(ModifyUserDefinedTagsCloudResourcesHandlerEvent.class);
        assertEquals(selector, captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testModifyStackAction() throws Exception {
        ModifyUserDefinedTagsEvent event = new ModifyUserDefinedTagsEvent(MODIFY_USER_DEFINED_TAGS_FREEIPA_STACK_EVENT.event(), STACK_ID,
                "Stack", USER_DEFINED_TAGS);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        when(stack.getName()).thenReturn("stack-1");
        AbstractModifyUserDefinedTagsAction<ModifyUserDefinedTagsEvent> action =
                (AbstractModifyUserDefinedTagsAction<ModifyUserDefinedTagsEvent>) underTest.modifyStackAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.UPDATE_IN_PROGRESS,
                "Starting to update user defined tags on FreeIPA stack: stack-1");
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        String selector = EventSelectorUtil.selector(ModifyUserDefinedTagsStackHandlerEvent.class);
        assertEquals(selector, captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testFinishedAction() throws Exception {
        ModifyUserDefinedTagsEvent event = new ModifyUserDefinedTagsEvent(FINISH_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT.event(), STACK_ID, "Finish",
                USER_DEFINED_TAGS);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        when(stack.getName()).thenReturn("stack-1");
        AbstractModifyUserDefinedTagsAction<ModifyUserDefinedTagsEvent> action =
                (AbstractModifyUserDefinedTagsAction<ModifyUserDefinedTagsEvent>) underTest.finishedAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.UPDATE_COMPLETE,
                "Update user defined tags finished for FreeIPA: stack-1");
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        String selector = FINALIZE_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT.event();
        assertEquals(selector, captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testFailedAction() throws Exception {
        ModifyUserDefinedTagsFailedEvent event = new ModifyUserDefinedTagsFailedEvent(STACK_ID, "failedPhase", new CloudbreakException("error"),
                FailureType.ERROR);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getName()).thenReturn("stack-1");
        AbstractModifyUserDefinedTagsAction<ModifyUserDefinedTagsFailedEvent> action =
                (AbstractModifyUserDefinedTagsAction<ModifyUserDefinedTagsFailedEvent>) underTest.failedAction();
        initActionPrivateFields(action);
        ReflectionTestUtils.setField(action, "jobService", mock(FreeipaJobService.class), FreeipaJobService.class);
        variables.put(OPERATION_ID, "test-op");
        doReturn("test-account").when(stack).getAccountId();
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(stackUpdater).updateStackStatus(eq(stack), eq(DetailedStackStatus.UPDATE_FAILED), eq("error"));
        verify(operationService).failOperation(eq("test-account"), eq("test-op"),
                eq("Update user defined tags failed for FreeIPA: stack-1 at phase: failedPhase"), any(), any());
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        String selector = HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT.event();
        assertEquals(selector, captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}