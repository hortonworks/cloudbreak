package com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags;

import static com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.FINALIZE_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.FINISH_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.MODIFY_USER_DEFINED_TAGS_REDBEAMS_STACK_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.MODIFY_USER_DEFINED_TAGS_REDBEAMS_START_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
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
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsCloudResourcesHandlerEvent;
import com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsEvent;
import com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsFailedEvent;
import com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsStackHandlerEvent;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@ExtendWith(MockitoExtension.class)
class ModifyUserDefinedTagsActionsTest {
    private static final Map<String, String> USER_DEFINED_TAGS = Map.of("custom", "value");

    private static final long STACK_ID = 1L;

    private Map<Object, Object> variables;

    private RedbeamsContext context;

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
    private DatabaseStack databaseStack;

    @Mock
    private DBStack dbStack;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private DBStackStatusUpdater dbStackStatusUpdater;

    @InjectMocks
    private ModifyUserDefinedTagsActions underTest;

    @BeforeEach
    void setUp() {
        variables = new HashMap<>();
        context = new RedbeamsContext(flowParameters, cloudContext, cloudCredential, databaseStack, dbStack);
    }

    @Test
    void testModifyCloudResourcesAction() throws Exception {
        ModifyUserDefinedTagsEvent event = new ModifyUserDefinedTagsEvent(MODIFY_USER_DEFINED_TAGS_REDBEAMS_START_EVENT.event(), STACK_ID,
                USER_DEFINED_TAGS);
        when(dbStack.getId()).thenReturn(STACK_ID);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        AbstractModifyUserDefinedTagsAction<ModifyUserDefinedTagsEvent> action =
                (AbstractModifyUserDefinedTagsAction<ModifyUserDefinedTagsEvent>) underTest.modifyCloudResourcesAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(dbStackStatusUpdater).updateStatus(STACK_ID, DetailedDBStackStatus.MODIFY_USER_DEFINED_TAGS_IN_PROGRESS);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        String selector = EventSelectorUtil.selector(ModifyUserDefinedTagsCloudResourcesHandlerEvent.class);
        assertEquals(selector, captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "resourceId"));
    }

    @Test
    void testModifyStackAction() throws Exception {
        ModifyUserDefinedTagsEvent event = new ModifyUserDefinedTagsEvent(MODIFY_USER_DEFINED_TAGS_REDBEAMS_STACK_EVENT.event(), STACK_ID, USER_DEFINED_TAGS);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        AbstractModifyUserDefinedTagsAction<ModifyUserDefinedTagsEvent> action =
                (AbstractModifyUserDefinedTagsAction<ModifyUserDefinedTagsEvent>) underTest.modifyStackAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        String selector = EventSelectorUtil.selector(ModifyUserDefinedTagsStackHandlerEvent.class);
        assertEquals(selector, captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "resourceId"));
    }

    @Test
    void testFinishedAction() throws Exception {
        ModifyUserDefinedTagsEvent event = new ModifyUserDefinedTagsEvent(FINISH_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT.event(), STACK_ID, USER_DEFINED_TAGS);
        when(dbStack.getId()).thenReturn(STACK_ID);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        AbstractModifyUserDefinedTagsAction<ModifyUserDefinedTagsEvent> action =
                (AbstractModifyUserDefinedTagsAction<ModifyUserDefinedTagsEvent>) underTest.finishedAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(dbStackStatusUpdater).updateStatus(STACK_ID, DetailedDBStackStatus.MODIFY_USER_DEFINED_TAGS_COMPLETED);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        String selector = FINALIZE_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT.event();
        assertEquals(selector, captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "resourceId"));
    }

    @Test
    void testFailedAction() throws Exception {
        ModifyUserDefinedTagsFailedEvent event = new ModifyUserDefinedTagsFailedEvent(STACK_ID, new CloudbreakException("error"));
        when(dbStack.getId()).thenReturn(STACK_ID);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        AbstractModifyUserDefinedTagsAction<ModifyUserDefinedTagsFailedEvent> action =
                (AbstractModifyUserDefinedTagsAction<ModifyUserDefinedTagsFailedEvent>) underTest.failedAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(dbStackStatusUpdater).updateStatus(STACK_ID, DetailedDBStackStatus.MODIFY_USER_DEFINED_TAGS_FAILED,
                "error");
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        String selector = HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT.event();
        assertEquals(selector, captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "resourceId"));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}