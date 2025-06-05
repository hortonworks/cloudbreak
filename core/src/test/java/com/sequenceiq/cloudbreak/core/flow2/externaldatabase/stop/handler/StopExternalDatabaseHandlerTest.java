package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.conf.ExternalDatabaseConfig;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.StackUpdaterService;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.StopExternalDatabaseFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.StopExternalDatabaseRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.StopExternalDatabaseResult;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.externaldatabase.DatabaseServerParameterDecorator;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class StopExternalDatabaseHandlerTest {

    private static final Event.Headers EVENT_HEADERS = new Event.Headers(Map.of("header_key", "header_value"));

    private static final long STACK_ID = 1L;

    private static final String STACK_NAME = "stackName";

    private static final String DATABASE_CRN = "databaseCrn";

    @Mock
    private EventBus eventBus;

    @Mock
    private ExternalDatabaseService stopService;

    @Mock
    private StackUpdaterService stackUpdaterService;

    @Mock
    private EnvironmentService environmentClientService;

    @Mock
    private ExternalDatabaseConfig externalDatabaseConfig;

    @Mock
    private StackService stackService;

    @Mock
    private Map<CloudPlatform, DatabaseServerParameterDecorator> databaseServerParameterDecoratorMap;

    @Mock
    private DatabaseServerParameterDecorator databaseServerParameterDecorator;

    @InjectMocks
    private StopExternalDatabaseHandler underTest;

    @Test
    void selector() {
        assertThat(underTest.selector()).isEqualTo("StopExternalDatabaseRequest");
    }

    @ParameterizedTest
    @ValueSource(classes = { UserBreakException.class, PollerStoppedException.class, PollerException.class, Exception.class})
    @MockitoSettings(strictness = Strictness.LENIENT)
    void acceptCatchErrors(Class<? extends Exception> exceptionClass) {
        doAnswer(a -> {
            throw exceptionClass.getDeclaredConstructor().newInstance();
        }).when(stopService).stopDatabase(any(), any(DatabaseAvailabilityType.class), any());
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AWS");
        when(environmentClientService.getByCrn(anyString())).thenReturn(environment);
        when(databaseServerParameterDecoratorMap.get(CloudPlatform.AWS)).thenReturn(databaseServerParameterDecorator);
        when(externalDatabaseConfig.isExternalDatabasePauseSupportedFor(any(), any())).thenReturn(true);

        Stack stack = buildStack(DatabaseAvailabilityType.HA);
        stack.setType(StackType.WORKLOAD);
        stack.getCluster().setDatabaseServerCrn(DATABASE_CRN);
        when(stackService.getById(anyLong())).thenReturn(stack);
        StopExternalDatabaseRequest request =
                new StopExternalDatabaseRequest(STACK_ID, "selector", "resourceName", "crn");
        Event<StopExternalDatabaseRequest> event = new Event<>(EVENT_HEADERS, request);

        underTest.accept(event);

        verify(stopService).stopDatabase(eq(stack.getCluster()), eq(DatabaseAvailabilityType.HA), eq(environment));

        verify(stackUpdaterService).updateStatus(eq(STACK_ID), eq(DetailedStackStatus.EXTERNAL_DATABASE_STOP_IN_PROGRESS),
                eq(ResourceEvent.CLUSTER_EXTERNAL_DATABASE_STOP_COMMANCED), eq("External database stop in progress"));

        verify(stackUpdaterService, never()).updateStatus(eq(STACK_ID), eq(DetailedStackStatus.STOPPED),
                eq(ResourceEvent.CLUSTER_EXTERNAL_DATABASE_CREATION_FINISHED), anyString());

        ArgumentCaptor<Event<StopExternalDatabaseFailed>> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq("StopExternalDatabaseFailed"), eventCaptor.capture());
        Event<StopExternalDatabaseFailed> value = eventCaptor.getValue();
        assertThat(value.getHeaders()).isEqualTo(EVENT_HEADERS);
        assertThat(value.getData().getResourceCrn()).isEqualTo(DATABASE_CRN);
    }

    @Test
    void acceptNonDatahub() {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AWS");
        when(environmentClientService.getByCrn(anyString())).thenReturn(environment);

        Stack stack = buildStack(DatabaseAvailabilityType.HA);
        stack.setType(StackType.DATALAKE);
        stack.getCluster().setDatabaseServerCrn(DATABASE_CRN);
        when(stackService.getById(anyLong())).thenReturn(stack);
        StopExternalDatabaseRequest request =
                new StopExternalDatabaseRequest(STACK_ID, "selector", "resourceName", "crn");
        Event<StopExternalDatabaseRequest> event = new Event<>(EVENT_HEADERS, request);

        underTest.accept(event);

        verify(stopService, never()).stopDatabase(any(), any(), eq(environment));
        verify(stackUpdaterService).updateStatus(eq(STACK_ID), eq(DetailedStackStatus.STOPPED),
                eq(ResourceEvent.CLUSTER_EXTERNAL_DATABASE_STOP_NOT_REQUIRED), eq("External database stop is not required. Cluster stopped."));
        verify(eventBus).notify(eq("StopExternalDatabaseResult"), any(Event.class));
    }

    @Test
    void acceptDbNone() {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AWS");
        when(environmentClientService.getByCrn(anyString())).thenReturn(environment);

        Stack stack = buildStack(DatabaseAvailabilityType.NONE);
        stack.setType(StackType.WORKLOAD);
        when(stackService.getById(anyLong())).thenReturn(stack);
        StopExternalDatabaseRequest request =
                new StopExternalDatabaseRequest(STACK_ID, "selector", "resourceName", "crn");
        Event<StopExternalDatabaseRequest> event = new Event<>(EVENT_HEADERS, request);

        underTest.accept(event);

        verify(stopService, never()).stopDatabase(any(), any(), eq(environment));
        verify(stackUpdaterService).updateStatus(eq(STACK_ID), eq(DetailedStackStatus.STOPPED),
                eq(ResourceEvent.CLUSTER_EXTERNAL_DATABASE_STOP_NOT_REQUIRED), isNull());
        verify(eventBus).notify(eq("StopExternalDatabaseResult"), any(Event.class));
    }

    @Test
    void acceptNotSupportedCloudProvider() {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AWS");
        when(environmentClientService.getByCrn(anyString())).thenReturn(environment);
        when(databaseServerParameterDecoratorMap.get(CloudPlatform.AWS)).thenReturn(databaseServerParameterDecorator);
        when(externalDatabaseConfig.isExternalDatabasePauseSupportedFor(any(), any())).thenReturn(false);

        Stack stack = buildStack(DatabaseAvailabilityType.HA);
        stack.setType(StackType.WORKLOAD);
        stack.getCluster().setDatabaseServerCrn(DATABASE_CRN);
        when(stackService.getById(anyLong())).thenReturn(stack);
        StopExternalDatabaseRequest request =
                new StopExternalDatabaseRequest(STACK_ID, "selector", "resourceName", "crn");
        Event<StopExternalDatabaseRequest> event = new Event<>(EVENT_HEADERS, request);

        underTest.accept(event);

        verify(stopService, never()).stopDatabase(any(), any(), eq(environment));
        verify(stackUpdaterService).updateStatus(eq(STACK_ID), eq(DetailedStackStatus.STOPPED), eq(ResourceEvent.CLUSTER_EXTERNAL_DATABASE_STOP_NOT_REQUIRED),
                eq("External database stop is not supported by the cloud platform. Cluster stopped."));
        verify(eventBus).notify(eq("StopExternalDatabaseResult"), any(Event.class));
    }

    @Test
    void acceptHappyPath() {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AWS");
        when(environmentClientService.getByCrn(anyString())).thenReturn(environment);
        when(databaseServerParameterDecoratorMap.get(CloudPlatform.AWS)).thenReturn(databaseServerParameterDecorator);
        when(externalDatabaseConfig.isExternalDatabasePauseSupportedFor(any(), any())).thenReturn(true);

        Stack stack = buildStack(DatabaseAvailabilityType.HA);
        stack.setType(StackType.WORKLOAD);
        stack.getCluster().setDatabaseServerCrn(DATABASE_CRN);
        when(stackService.getById(anyLong())).thenReturn(stack);
        StopExternalDatabaseRequest request =
                new StopExternalDatabaseRequest(STACK_ID, "selector", "resourceName", "crn");
        Event<StopExternalDatabaseRequest> event = new Event<>(EVENT_HEADERS, request);

        underTest.accept(event);

        verify(stopService).stopDatabase(eq(stack.getCluster()), eq(DatabaseAvailabilityType.HA), eq(environment));

        verify(stackUpdaterService).updateStatus(eq(STACK_ID), eq(DetailedStackStatus.EXTERNAL_DATABASE_STOP_IN_PROGRESS),
                eq(ResourceEvent.CLUSTER_EXTERNAL_DATABASE_STOP_COMMANCED), eq("External database stop in progress"));

        verify(stackUpdaterService).updateStatus(eq(STACK_ID), eq(DetailedStackStatus.STOPPED),
                eq(ResourceEvent.CLUSTER_EXTERNAL_DATABASE_STOP_FINISHED), eq("External database stop finished successfully"));

        ArgumentCaptor<Event<StopExternalDatabaseResult>> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq("StopExternalDatabaseResult"), eventCaptor.capture());
        Event<StopExternalDatabaseResult> value = eventCaptor.getValue();
        assertThat(value.getHeaders()).isEqualTo(EVENT_HEADERS);
        assertThat(value.getData().getResourceCrn()).isEqualTo(DATABASE_CRN);
    }

    private Stack buildStack(DatabaseAvailabilityType databaseAvailabilityType) {
        StackStatus status = new StackStatus();
        status.setStatus(Status.AVAILABLE);
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
