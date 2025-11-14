package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationHandlerSelectors.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationStateSelectors.FINALIZE_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationStateSelectors.FINISH_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationStateSelectors.HANDLED_FAILED_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_KRAFT_MIGRATION_FINALIZATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_KRAFT_MIGRATION_FINALIZATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_KRAFT_MIGRATION_FINALIZATION_STARTED_EVENT;
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

import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFinalizationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFinalizationFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFinalizationTriggerEvent;
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
class MigrateZookeeperToKraftFinalizationActionsTest {
    private static final Long STACK_ID = 1L;

    private Map<Object, Object> variables = new HashMap<>();

    @InjectMocks
    private MigrateZookeeperToKraftFinalizationActions underTest;

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
    void testFinalizeZookeeperToKraftMigrationAction() throws Exception {
        MigrateZookeeperToKraftFinalizationTriggerEvent event = new MigrateZookeeperToKraftFinalizationTriggerEvent(STACK_ID, new Promise<>());
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());

        AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftFinalizationTriggerEvent> action =
                (AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftFinalizationTriggerEvent>) underTest.finalizeZookeeperToKraftMigrationAction();
        initActionPrivateFields(action);
        context = new MigrateZookeeperToKraftContext(flowParameters, event);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(stackUpdater).updateStackStatus(event.getResourceId(), FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS);
        verify(flowMessageService).fireEventAndLog(event.getResourceId(), UPDATE_IN_PROGRESS.name(), CLUSTER_KRAFT_MIGRATION_FINALIZATION_STARTED_EVENT);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        assertEquals(FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.event(), captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testFinalizeZookeeperToKraftMigrationFinished() throws Exception {
        MigrateZookeeperToKraftFinalizationEvent event =
                new MigrateZookeeperToKraftFinalizationEvent(FINISH_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.name(), STACK_ID);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());

        AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftFinalizationEvent> action =
                (AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftFinalizationEvent>) underTest.finalizeZookeeperToKraftMigrationFinished();
        initActionPrivateFields(action);
        context = new MigrateZookeeperToKraftContext(flowParameters, event);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(stackUpdater).updateStackStatus(event.getResourceId(), FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE);
        verify(flowMessageService).fireEventAndLog(event.getResourceId(), AVAILABLE.name(), CLUSTER_KRAFT_MIGRATION_FINALIZATION_FINISHED_EVENT);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        String selector = FINALIZE_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.event();
        assertEquals(selector, captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testFinalizeZookeeperToKraftMigrationFailed() throws Exception {
        String errorMessage = "Error during KRaft migration finalization";
        RuntimeException error = new RuntimeException(errorMessage);
        MigrateZookeeperToKraftFinalizationFailureEvent event = new MigrateZookeeperToKraftFinalizationFailureEvent(STACK_ID, error);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());

        AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftFinalizationFailureEvent> action =
                (AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftFinalizationFailureEvent>) underTest.finalizeZookeeperToKraftMigrationFailed();
        initActionPrivateFields(action);
        context = new MigrateZookeeperToKraftContext(flowParameters, event);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(stackUpdater).updateStackStatus(event.getResourceId(), FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED);
        verify(flowMessageService).fireEventAndLog(event.getResourceId(), UPDATE_FAILED.name(), CLUSTER_KRAFT_MIGRATION_FINALIZATION_FAILED_EVENT,
                errorMessage);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        assertEquals(HANDLED_FAILED_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.event(), captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}