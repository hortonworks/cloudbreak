package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationHandlerSelectors.MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationHandlerSelectors.RESTART_KAFKA_BROKER_NODES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationHandlerSelectors.RESTART_KAFKA_CONNECT_NODES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.START_RESTART_KAFKA_CONNECT_NODES_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_KRAFT_MIGRATION_COMMAND_IN_PROGRESS_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_KRAFT_MIGRATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_KRAFT_MIGRATION_FINISHED_EVENT;
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

import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftTriggerEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class MigrateZookeeperToKraftMigrationActionsTest {

    private static final Long STACK_ID = 1L;

    private Map<Object, Object> variables = new HashMap<>();

    @InjectMocks
    private MigrateZookeeperToKraftMigrationActions underTest;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private FlowMessageService flowMessageService;

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
    void testRestartKafkaBrokerNodesAction() throws Exception {
        MigrateZookeeperToKraftTriggerEvent event = new MigrateZookeeperToKraftTriggerEvent(STACK_ID, new Promise<>());
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());

        AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftTriggerEvent> action =
                (AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftTriggerEvent>) underTest.restartKafkaBrokerNodesAction();
        initActionPrivateFields(action);
        context = new MigrateZookeeperToKraftContext(flowParameters, event);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(stackUpdater).updateStackStatus(event.getResourceId(), ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS);
        verify(flowMessageService).fireEventAndLog(event.getResourceId(), UPDATE_IN_PROGRESS.name(), CLUSTER_KRAFT_MIGRATION_COMMAND_IN_PROGRESS_EVENT);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        assertEquals(RESTART_KAFKA_BROKER_NODES_EVENT.event(), captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testRestartKafkaConnectNodesAction() throws Exception {
        MigrateZookeeperToKraftEvent event = new MigrateZookeeperToKraftEvent(START_RESTART_KAFKA_CONNECT_NODES_EVENT.name(), STACK_ID);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());

        AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftEvent> action =
                (AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftEvent>) underTest.restartKafkaConnectNodesAction();
        initActionPrivateFields(action);
        context = new MigrateZookeeperToKraftContext(flowParameters, event);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        assertEquals(RESTART_KAFKA_CONNECT_NODES_EVENT.event(), captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testMigrateZookeeperToKraftAction() throws Exception {
        MigrateZookeeperToKraftEvent event = new MigrateZookeeperToKraftEvent(START_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT.name(), STACK_ID);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());

        AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftEvent> action =
                (AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftEvent>) underTest.migrateZookeeperToKraftAction();
        initActionPrivateFields(action);
        context = new MigrateZookeeperToKraftContext(flowParameters, event);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        assertEquals(MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT.event(), captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testMigrateZookeeperToKraftFinished() throws Exception {
        MigrateZookeeperToKraftEvent event = new MigrateZookeeperToKraftEvent(FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT.name(), STACK_ID);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());

        AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftEvent> action =
                (AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftEvent>) underTest.migrateZookeeperToKraftFinished();
        initActionPrivateFields(action);
        context = new MigrateZookeeperToKraftContext(flowParameters, event);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(stackUpdater).updateStackStatus(event.getResourceId(), ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE);
        verify(flowMessageService).fireEventAndLog(event.getResourceId(), AVAILABLE.name(), CLUSTER_KRAFT_MIGRATION_FINISHED_EVENT);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        String selector = FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT.event();
        assertEquals(selector, captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testMigrateZookeeperToKraftFailed() throws Exception {
        String errorMessage = "Error during KRaft migration";
        RuntimeException error = new RuntimeException(errorMessage);
        MigrateZookeeperToKraftFailureEvent event = new MigrateZookeeperToKraftFailureEvent(STACK_ID, error);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());

        AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftFailureEvent> action =
                (AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftFailureEvent>) underTest.migrateZookeeperToKraftFailed();
        initActionPrivateFields(action);
        context = new MigrateZookeeperToKraftContext(flowParameters, event);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(stackUpdater).updateStackStatus(event.getResourceId(), ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED);
        verify(flowMessageService).fireEventAndLog(event.getResourceId(), UPDATE_FAILED.name(), CLUSTER_KRAFT_MIGRATION_FAILED_EVENT,
                errorMessage);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        assertEquals(HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT.event(), captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}