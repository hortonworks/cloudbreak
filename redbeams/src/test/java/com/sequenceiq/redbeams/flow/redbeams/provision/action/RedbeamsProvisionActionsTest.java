package com.sequenceiq.redbeams.flow.redbeams.provision.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
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
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.NetworkV4StackRequest;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.provision.AbstractRedbeamsProvisionAction;
import com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsProvisionEvent;
import com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsProvisionState;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.TriggerRedbeamsProvisionEvent;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerSuccess;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.register.UpdateDatabaseServerRegistrationRequest;
import com.sequenceiq.redbeams.service.stack.DBResourceService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@ExtendWith(MockitoExtension.class)
class RedbeamsProvisionActionsTest {

    private static final Long RESOURCE_ID = 1234L;

    @Mock
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Mock
    private DBResourceService dbResourceService;

    @InjectMocks
    private RedbeamsProvisionActions underTest;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private Event<Object> event;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private DBStack dbStack;

    private RedbeamsContext context;

    @Mock
    private CloudResource cloudResource;

    @Mock
    private CloudResourceStatus cloudResourceStatus;

    @Captor
    private ArgumentCaptor<Object> payloadArgumentCaptor;

    @BeforeEach
    void setUp() {
        context = new RedbeamsContext(flowParameters, cloudContext, cloudCredential, databaseStack, dbStack);
    }

    private AbstractRedbeamsProvisionAction<RedbeamsEvent> getAllocateDatabaseServerAction() {
        return (AbstractRedbeamsProvisionAction<RedbeamsEvent>) underTest.allocateDatabaseServer();
    }

    @Test
    void allocateDatabaseServerTestPrepareExecution() {
        TriggerRedbeamsProvisionEvent payload = new TriggerRedbeamsProvisionEvent("selector", RESOURCE_ID, new NetworkV4StackRequest());

        new AbstractActionTestSupport<>(getAllocateDatabaseServerAction()).prepareExecution(payload, Map.of());

        verify(dbStackStatusUpdater).updateStatus(RESOURCE_ID, DetailedDBStackStatus.CREATING_INFRASTRUCTURE);
    }

    @Test
    void allocateDatabaseServerTestCreateRequest() throws Exception {
        when(cloudContext.getId()).thenReturn(RESOURCE_ID);
        AbstractRedbeamsProvisionAction<RedbeamsEvent> allocateDatabaseServerAction = getAllocateDatabaseServerAction();
        initActionPrivateFields(allocateDatabaseServerAction);
        TriggerRedbeamsProvisionEvent payload = new TriggerRedbeamsProvisionEvent("selector", RESOURCE_ID, new NetworkV4StackRequest());

        AbstractActionTestSupport<RedbeamsProvisionState, RedbeamsProvisionEvent, RedbeamsContext, RedbeamsEvent> underTest
                = new AbstractActionTestSupport<>(allocateDatabaseServerAction);
        underTest.doExecute(context, payload, new HashMap<>());

        verify(reactorEventFactory).createEvent(anyMap(), payloadArgumentCaptor.capture());
        AllocateDatabaseServerRequest request = (AllocateDatabaseServerRequest) payloadArgumentCaptor.getValue();
        assertEquals(payload.getResourceId(), request.getResourceId());
        assertEquals(payload.getNetworkParameters(), request.getNetworkParameters());
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }

    private AbstractRedbeamsProvisionAction<AllocateDatabaseServerSuccess> getUpdateDatabaseServerRegistrationAction() {
        AbstractRedbeamsProvisionAction<AllocateDatabaseServerSuccess> action =
                (AbstractRedbeamsProvisionAction<AllocateDatabaseServerSuccess>) underTest.updateDatabaseServerRegistration();
        initActionPrivateFields(action);
        return action;
    }

    @Test
    void updateDatabaseServerRegistrationTestDoExecute() throws Exception {
        when(cloudContext.getId()).thenReturn(RESOURCE_ID);
        when(dbResourceService.getAllAsCloudResource(RESOURCE_ID)).thenReturn(List.of(cloudResource));
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        AllocateDatabaseServerSuccess payload = new AllocateDatabaseServerSuccess(RESOURCE_ID);

        new AbstractActionTestSupport<>(getUpdateDatabaseServerRegistrationAction()).doExecute(context, payload, Map.of());

        verify(dbStackStatusUpdater).updateStatus(RESOURCE_ID, DetailedDBStackStatus.PROVISIONED);
        verify(reactorEventFactory).createEvent(anyMap(), payloadArgumentCaptor.capture());
        verify(eventBus).notify("UPDATEDATABASESERVERREGISTRATIONREQUEST", event);

        Object responsePayload = payloadArgumentCaptor.getValue();
        assertThat(responsePayload).isInstanceOf(UpdateDatabaseServerRegistrationRequest.class);

        UpdateDatabaseServerRegistrationRequest updateDatabaseServerRegistrationRequest = (UpdateDatabaseServerRegistrationRequest) responsePayload;
        assertThat(updateDatabaseServerRegistrationRequest.getResourceId()).isEqualTo(RESOURCE_ID);
        assertThat(updateDatabaseServerRegistrationRequest.getCloudContext()).isSameAs(cloudContext);
        assertThat(updateDatabaseServerRegistrationRequest.getCloudCredential()).isSameAs(cloudCredential);
        assertThat(updateDatabaseServerRegistrationRequest.getDatabaseStack()).isSameAs(databaseStack);
        assertThat(updateDatabaseServerRegistrationRequest.getDbResources()).isEqualTo(List.of(cloudResource));
    }

}