package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.INSTALLING_KRAFT_SERVICE_FOR_MIGRATION_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_CONFIGURATION_COMPLETE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_CONFIGURATION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_CONFIGURATION_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationHandlerSelectors.MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationHandlerSelectors.MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationHandlerSelectors.MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALL_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_KRAFT_MIGRATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_KRAFT_MIGRATION_STARTED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Bean;
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

    private static final boolean NO_KRAFT_INSTALL_NEEDED = false;

    private static final Long STACK_ID = 1L;

    private final Map<Object, Object> variables = new HashMap<>();

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
    void testMigrateZookeeperToKraftConfigurationValidationAction() throws Exception {
        MigrateZookeeperToKraftConfigurationTriggerEvent event = new MigrateZookeeperToKraftConfigurationTriggerEvent(STACK_ID, false, new Promise<>());
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());

        AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftConfigurationTriggerEvent> action =
                (AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftConfigurationTriggerEvent>)
                        underTest.migrateZookeeperToKraftConfigurationValidationAction();
        initActionPrivateFields(action);
        context = new MigrateZookeeperToKraftContext(flowParameters, event);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        assertEquals(MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_EVENT.event(), captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testMigrateZookeeperToKraftConfigurationAction() throws Exception {
        MigrateZookeeperToKraftConfigurationEvent event =
                new MigrateZookeeperToKraftConfigurationEvent(START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT.name(), STACK_ID, false);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());

        AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftConfigurationEvent> action =
                (AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftConfigurationEvent>) underTest.migrateZookeeperToKraftConfigurationAction();
        initActionPrivateFields(action);
        context = new MigrateZookeeperToKraftContext(flowParameters, event);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(stackUpdater).updateStackStatus(event.getResourceId(), ZOOKEEPER_TO_KRAFT_MIGRATION_CONFIGURATION_IN_PROGRESS);
        verify(flowMessageService).fireEventAndLog(event.getResourceId(), UPDATE_IN_PROGRESS.name(), CLUSTER_KRAFT_MIGRATION_STARTED_EVENT);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        assertEquals(MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT.event(), captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testMigrateZookeeperToKraftConfigurationFinished() throws Exception {
        MigrateZookeeperToKraftConfigurationEvent event =
                new MigrateZookeeperToKraftConfigurationEvent(FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT.name(), STACK_ID, NO_KRAFT_INSTALL_NEEDED);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());

        AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftConfigurationEvent> action =
                (AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftConfigurationEvent>) underTest.migrateZookeeperToKraftConfigurationFinished();
        initActionPrivateFields(action);
        context = new MigrateZookeeperToKraftContext(flowParameters, event);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(stackUpdater).updateStackStatus(event.getResourceId(), ZOOKEEPER_TO_KRAFT_MIGRATION_CONFIGURATION_COMPLETE);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        String selector = FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT.event();
        assertEquals(selector, captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testMigrateZookeeperToKraftConfigurationFailed() throws Exception {
        String errorMessage = "Error KRaft migration configuration";
        RuntimeException error = new RuntimeException(errorMessage);
        MigrateZookeeperToKraftConfigurationFailureEvent event = new MigrateZookeeperToKraftConfigurationFailureEvent(STACK_ID, error);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());

        AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftConfigurationFailureEvent> action =
                (AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftConfigurationFailureEvent>)
                        underTest.migrateZookeeperToKraftConfigurationFailed();
        initActionPrivateFields(action);
        context = new MigrateZookeeperToKraftContext(flowParameters, event);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(stackUpdater).updateStackStatus(event.getResourceId(), ZOOKEEPER_TO_KRAFT_MIGRATION_CONFIGURATION_FAILED);
        verify(flowMessageService).fireEventAndLog(event.getResourceId(), UPDATE_FAILED.name(), CLUSTER_KRAFT_MIGRATION_FAILED_EVENT,
                errorMessage);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        assertEquals(HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT.event(), captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    @DisplayName("migrateZookeeperToKraftInstallAction.createFlowContext should propagate payload.isKraftInstallNeeded into the context")
    void testMigrateZookeeperToKraftInstallActionCreateFlowContext() {
        boolean kraftInstallNeeded = true;
        MigrateZookeeperToKraftConfigurationEvent payload =
                new MigrateZookeeperToKraftConfigurationEvent(START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT.name(), STACK_ID, kraftInstallNeeded);

        @SuppressWarnings("unchecked")
        AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftConfigurationEvent> action =
                (AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftConfigurationEvent>) underTest.migrateZookeeperToKraftInstallAction();

        MigrateZookeeperToKraftContext created =
                ReflectionTestUtils.invokeMethod(action, "createFlowContext", flowParameters, null, payload);

        assertNotNull(created, "createFlowContext must return a non-null context");
        Object actualKraftInstallNeeded = ReflectionTestUtils.getField(created, "kraftInstallNeeded");
        assertEquals(kraftInstallNeeded, actualKraftInstallNeeded, "Context should store kraftInstallNeeded from the payload");
    }

    @Test
    @DisplayName("migrateZookeeperToKraftInstallAction.doExecute should update stack status and send MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALL_EVENT")
    void testMigrateZookeeperToKraftInstallActionDoExecuteUpdatesStatusAndSendsEvent() throws Exception {
        boolean kraftInstallNeeded = false;
        MigrateZookeeperToKraftConfigurationEvent event =
                new MigrateZookeeperToKraftConfigurationEvent(START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT.name(), STACK_ID, kraftInstallNeeded);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());

        @SuppressWarnings("unchecked")
        AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftConfigurationEvent> action =
                (AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftConfigurationEvent>) underTest.migrateZookeeperToKraftInstallAction();
        initActionPrivateFields(action);

        context = new MigrateZookeeperToKraftContext(flowParameters, event);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(stackUpdater).updateStackStatus(event.getResourceId(), INSTALLING_KRAFT_SERVICE_FOR_MIGRATION_IN_PROGRESS);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        assertEquals(MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALL_EVENT.event(), captor.getValue());
        assertEquals(STACK_ID, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    @DisplayName("migrateZookeeperToKraftInstallAction.getFailurePayload should create MigrateZookeeperToKraftConfigurationFailureEvent with resourceId")
    void testMigrateZookeeperToKraftInstallActionGetFailurePayload() {
        MigrateZookeeperToKraftConfigurationEvent payload =
                new MigrateZookeeperToKraftConfigurationEvent(START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT.name(), STACK_ID, false);
        RuntimeException ex = new RuntimeException("boom");

        @SuppressWarnings("unchecked")
        AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftConfigurationEvent> action =
                (AbstractMigrateZookeeperToKraftAction<MigrateZookeeperToKraftConfigurationEvent>) underTest.migrateZookeeperToKraftInstallAction();

        Object failurePayload = ReflectionTestUtils.invokeMethod(action, "getFailurePayload", payload, Optional.empty(), ex);

        assertNotNull(failurePayload, "getFailurePayload must return a non-null payload");
        assertInstanceOf(MigrateZookeeperToKraftConfigurationFailureEvent.class, failurePayload, "Expected a MigrateZookeeperToKraftConfigurationFailureEvent");
        assertEquals(STACK_ID, ReflectionTestUtils.getField(failurePayload, "stackId"));
    }

    @Test
    @DisplayName("Verifying that all Beans have the expected names and count")
    void testBeanAnnotatedMethodsHaveExpectedBeans() {
        Set<String> expectedBeanNames = new LinkedHashSet<>(Arrays.asList(
                "MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_STATE",
                "MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_FINISHED_STATE",
                "MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_FAILED_STATE",
                "MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_STATE",
                "MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALL_STATE"
        ));

        Set<String> actualBeanNames = Arrays.stream(MigrateZookeeperToKraftConfigurationActions.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Bean.class))
                .map(m -> m.getAnnotation(Bean.class))
                .map(bean -> {
                    if (bean.name().length > 0) {
                        return bean.name()[0];
                    }
                    if (bean.value().length > 0) {
                        return bean.value()[0];
                    }
                    return "";
                })
                .filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        assertEquals(expectedBeanNames.size(), actualBeanNames.size(), "Unexpected number of Bean methods");
        assertEquals(expectedBeanNames, actualBeanNames, "Unexpected Bean names on configuration action methods");
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }

}
