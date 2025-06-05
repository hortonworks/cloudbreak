package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.StackUpdaterService;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.CreateExternalDatabaseRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.CreateExternalDatabaseResult;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class CreateExternalDatabaseHandlerTest {

    private static final Event.Headers EVENT_HEADERS = new Event.Headers(Map.of("header_key", "header_value"));

    private static final long STACK_ID = 1L;

    private static final String STACK_NAME = "stackName";

    private static final String DATABASE_CRN = "databaseCrn";

    @Mock
    private EventBus eventBus;

    @Mock
    private ExternalDatabaseService provisionService;

    @Mock
    private StackUpdaterService stackUpdaterService;

    @Mock
    private EnvironmentService environmentClientService;

    @Mock
    private EnvironmentValidator environmentValidator;

    @Mock
    private StackService stackService;

    @InjectMocks
    private CreateExternalDatabaseHandler underTest;

    @Test
    void selector() {
        assertThat(underTest.selector()).isEqualTo("CreateExternalDatabaseRequest");
    }

    @ParameterizedTest
    @ValueSource(classes = { UserBreakException.class, PollerStoppedException.class, PollerException.class, Exception.class})
    @MockitoSettings(strictness = Strictness.LENIENT)
    void acceptCatchErrors(Class<? extends Exception> exceptionClass) {
        doAnswer(a -> {
            throw exceptionClass.getDeclaredConstructor().newInstance();
        }).when(provisionService).provisionDatabase(any(), any());
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("cloudplatform");
        when(environmentClientService.getByCrn(anyString())).thenReturn(environment);

        when(stackService.getById(anyLong())).thenReturn(buildStack(DatabaseAvailabilityType.HA));
        CreateExternalDatabaseRequest request =
                new CreateExternalDatabaseRequest(STACK_ID, "selector", "resourceName", "crn");
        Event<CreateExternalDatabaseRequest> event = new Event<>(EVENT_HEADERS, request);

        underTest.accept(event);

        verify(provisionService).provisionDatabase(any(), eq(environment));

        verify(environmentValidator).checkValidEnvironment(eq(STACK_NAME), eq(DatabaseAvailabilityType.HA), eq(environment));

        verify(stackUpdaterService).updateStatus(eq(STACK_ID), eq(DetailedStackStatus.EXTERNAL_DATABASE_CREATION_IN_PROGRESS),
                eq(ResourceEvent.CLUSTER_EXTERNAL_DATABASE_CREATION_STARTED), eq("External database creation in progress"));

        verify(stackUpdaterService, never()).updateStatus(eq(STACK_ID), eq(DetailedStackStatus.PROVISION_REQUESTED),
                eq(ResourceEvent.CLUSTER_EXTERNAL_DATABASE_CREATION_FINISHED), anyString());

        verify(eventBus).notify(eq("CreateExternalDatabaseFailed"), any(Event.class));
    }

    @Test
    void accept() {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("cloudplatform");
        when(environmentClientService.getByCrn(anyString())).thenReturn(environment);

        doAnswer(a -> {
            Stack stack = a.getArgument(0);
            stack.getCluster().setDatabaseServerCrn(DATABASE_CRN);
            return null;
        }).when(provisionService).provisionDatabase(any(), any());

        when(stackService.getById(anyLong())).thenReturn(buildStack(DatabaseAvailabilityType.HA));
        CreateExternalDatabaseRequest request =
                new CreateExternalDatabaseRequest(STACK_ID, "selector", "resourceName", "crn");
        Event<CreateExternalDatabaseRequest> event = new Event<>(EVENT_HEADERS, request);

        underTest.accept(event);

        verify(provisionService).provisionDatabase(any(), eq(environment));

        verify(environmentValidator).checkValidEnvironment(eq(STACK_NAME), eq(DatabaseAvailabilityType.HA), eq(environment));

        verify(stackUpdaterService).updateStatus(eq(STACK_ID), eq(DetailedStackStatus.EXTERNAL_DATABASE_CREATION_IN_PROGRESS),
                eq(ResourceEvent.CLUSTER_EXTERNAL_DATABASE_CREATION_STARTED), eq("External database creation in progress"));

        verify(stackUpdaterService).updateStatus(eq(STACK_ID), eq(DetailedStackStatus.PROVISION_REQUESTED),
                eq(ResourceEvent.CLUSTER_EXTERNAL_DATABASE_CREATION_FINISHED), eq("External database creation finished"));

        ArgumentCaptor<Event<CreateExternalDatabaseResult>> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq("CreateExternalDatabaseResult"), eventCaptor.capture());
        Event<CreateExternalDatabaseResult> value = eventCaptor.getValue();
        assertThat(value.getHeaders()).isEqualTo(EVENT_HEADERS);
        assertThat(value.getData().getResourceCrn()).isEqualTo(DATABASE_CRN);
    }

    @Test
    void acceptDbNone() {
        when(stackService.getById(anyLong())).thenReturn(buildStack(DatabaseAvailabilityType.NONE));
        CreateExternalDatabaseRequest request =
                new CreateExternalDatabaseRequest(STACK_ID, "selector", "resourceName", "crn");
        Event<CreateExternalDatabaseRequest> event = new Event<>(EVENT_HEADERS, request);

        underTest.accept(event);

        verifyNoMoreInteractions(provisionService);
        verifyNoMoreInteractions(stackUpdaterService);
        verifyNoMoreInteractions(environmentValidator);

        ArgumentCaptor<Event<CreateExternalDatabaseResult>> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq("CreateExternalDatabaseResult"), eventCaptor.capture());
        Event<CreateExternalDatabaseResult> value = eventCaptor.getValue();
        assertThat(value.getHeaders()).isEqualTo(EVENT_HEADERS);
        assertThat(value.getData().getResourceCrn()).isNull();
    }

    private Stack buildStack(DatabaseAvailabilityType databaseAvailabilityType) {
        StackStatus status = new StackStatus();
        status.setStatus(Status.REQUESTED);
        Cluster cluster = new Cluster();
        Stack stack = new Stack();
        stack.setStackStatus(status);
        stack.setId(STACK_ID);
        stack.setName(STACK_NAME);
        stack.setEnvironmentCrn("envCrn");
        stack.setCluster(cluster);
        Database database = new Database();
        database.setExternalDatabaseAvailabilityType(databaseAvailabilityType);
        stack.setDatabase(database);
        return stack;
    }
}
