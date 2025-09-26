package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftValidationHandlerSelectors.MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftValidationStateSelectors.FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftValidationStateSelectors.FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftValidationStateSelectors.HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftValidationFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftValidationTriggerEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
public class MigrateZookeeperToKraftValidationActionsTest {
    private static final Long STACK_ID = 1L;

    private Map<Object, Object> variables = new HashMap<>();

    @InjectMocks
    private MigrateZookeeperToKraftValidationActions underTest;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    private MigrateZookeeperToKraftContext context;

    @Mock
    private FlowParameters flowParameters;

    @Captor
    private ArgumentCaptor<String> captor;

    @Test
    void testMigrateZookeeperToKraftValidationAction() throws Exception {
        MigrateZookeeperToKraftValidationTriggerEvent event = new MigrateZookeeperToKraftValidationTriggerEvent(STACK_ID, new Promise<>());
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());

        AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftValidationTriggerEvent> action =
                (AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftValidationTriggerEvent>) underTest.migrateZookeeperToKraftValidationAction();
        initActionPrivateFields(action);
        context = new MigrateZookeeperToKraftContext(flowParameters, event);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        assertEquals(MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT.event(), captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testMigrateZookeeperToKraftValidationFinished() throws Exception {
        MigrateZookeeperToKraftValidationEvent event = new MigrateZookeeperToKraftValidationEvent(FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT.name(),
                STACK_ID);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());

        AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftValidationEvent> action =
                (AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftValidationEvent>) underTest.migrateZookeeperToKraftValidationFinished();
        initActionPrivateFields(action);
        context = new MigrateZookeeperToKraftContext(flowParameters, event);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        String selector = FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT.event();
        assertEquals(selector, captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testMigrateZookeeperToKraftValidationFailed() throws Exception {
        RuntimeException error = new RuntimeException("error");
        MigrateZookeeperToKraftValidationFailureEvent event = new MigrateZookeeperToKraftValidationFailureEvent(STACK_ID, error);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());

        AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftValidationFailureEvent> action =
                (AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftValidationFailureEvent>) underTest.migrateZookeeperToKraftValidationFailed();
        initActionPrivateFields(action);
        context = new MigrateZookeeperToKraftContext(flowParameters, event);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        assertEquals(HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT.event(), captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}
