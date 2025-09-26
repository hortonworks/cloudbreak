package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationHandlerSelectors.MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
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

import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftConfigurationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftConfigurationFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftConfigurationTriggerEvent;
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
public class MigrateZookeeperToKraftConfigurationActionsTest {
    private static final Long STACK_ID = 1L;

    private Map<Object, Object> variables = new HashMap<>();

    @InjectMocks
    private MigrateZookeeperToKraftConfigurationActions underTest;

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
    void testMigrateZookeeperToKraftConfigurationAction() throws Exception {
        MigrateZookeeperToKraftConfigurationTriggerEvent event = new MigrateZookeeperToKraftConfigurationTriggerEvent(STACK_ID, new Promise<>());
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());

        AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftConfigurationTriggerEvent> action =
                (AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftConfigurationTriggerEvent>) underTest.migrateZookeeperToKraftConfigurationAction();
        initActionPrivateFields(action);
        context = new MigrateZookeeperToKraftContext(flowParameters, event);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        assertEquals(MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT.event(), captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testMigrateZookeeperToKraftConfigurationFinished() throws Exception {
        MigrateZookeeperToKraftConfigurationEvent event =
                new MigrateZookeeperToKraftConfigurationEvent(FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT.name(), STACK_ID);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());

        AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftConfigurationEvent> action =
                (AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftConfigurationEvent>) underTest.migrateZookeeperToKraftConfigurationFinished();
        initActionPrivateFields(action);
        context = new MigrateZookeeperToKraftContext(flowParameters, event);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        String selector = FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT.event();
        assertEquals(selector, captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testMigrateZookeeperToKraftConfigurationFailed() throws Exception {
        RuntimeException error = new RuntimeException("error");
        MigrateZookeeperToKraftConfigurationFailureEvent event = new MigrateZookeeperToKraftConfigurationFailureEvent(STACK_ID, error);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());

        AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftConfigurationFailureEvent> action =
                (AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftConfigurationFailureEvent>) underTest.migrateZookeeperToKraftConfigurationFailed();
        initActionPrivateFields(action);
        context = new MigrateZookeeperToKraftContext(flowParameters, event);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        assertEquals(HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT.event(), captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}
