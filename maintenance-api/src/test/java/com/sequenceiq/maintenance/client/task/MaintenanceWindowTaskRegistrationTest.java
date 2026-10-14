package com.sequenceiq.maintenance.client.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.maintenance.api.model.MaintenanceSubmitterService;
import com.sequenceiq.maintenance.api.model.MaintenanceTaskKind;
import com.sequenceiq.maintenance.api.v1.task.endpoint.MaintenanceWindowTaskEndpoint;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskRequest;
import com.sequenceiq.maintenance.api.v1.task.model.response.MaintenanceWindowTaskResponse;
import com.sequenceiq.maintenance.client.schedule.MaintenanceWindowScheduleLookup;
import com.sequenceiq.maintenance.client.task.MaintenanceWindowTaskRegistration.WorkItem;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowTaskRegistrationTest {

    private static final String ACCOUNT_ID = "acc-12345";

    private static final String RESOURCE_CRN = "crn:cdp:datahub:us-west-1:acc-12345:cluster:my-dh";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc-12345:environment:env";

    private static final String TASK_TYPE = "SECRET_ROTATION";

    private static final String WIRE_PAYLOAD_SECRET_NAMES = "secretNames";

    private static final List<WorkItem> WORK_ITEMS = List.of(workItemFor("SALT_PASSWORD"), workItemFor("CM_ADMIN_PASSWORD"));

    private static final String WIRE_EXECUTION_REF_SUBMITTER_SERVICE = "submitter_service";

    private static final String WIRE_EXECUTION_REF_EXECUTE_PATH = "execute_path";

    private static final String WIRE_EXECUTE_PATH = "/api/v1/internal/maintenance-tasks/execute";

    @Mock
    private MaintenanceWindowTaskEndpoint maintenanceWindowTaskEndpoint;

    @Mock
    private MaintenanceWindowScheduleLookup scheduleLookup;

    @Test
    void registerSkipsWhenNoSchedule() {
        MaintenanceWindowTaskRegistration underTest = registration(MaintenanceSubmitterService.CLOUDBREAK);
        when(scheduleLookup.hasConfiguredSchedule(ACCOUNT_ID, RESOURCE_CRN, ENV_CRN)).thenReturn(false);

        underTest.register(ACCOUNT_ID, RESOURCE_CRN, ENV_CRN, TASK_TYPE, List.of(workItemFor("SALT_PASSWORD")));

        verify(maintenanceWindowTaskEndpoint, never()).register(any(), any());
    }

    @ParameterizedTest
    @MethodSource("submitters")
    void registerRegistersOneShotTaskWithRetryPolicy(
            MaintenanceSubmitterService submitter, String wireSubmitterService) {
        MaintenanceWindowTaskRegistration underTest = registration(submitter);
        when(scheduleLookup.hasConfiguredSchedule(ACCOUNT_ID, RESOURCE_CRN, ENV_CRN)).thenReturn(true);
        Response created = successfulRegistrationResponse(taskResponse(99L));
        when(maintenanceWindowTaskEndpoint.register(eq(ACCOUNT_ID), any())).thenReturn(created);

        underTest.register(ACCOUNT_ID, RESOURCE_CRN, ENV_CRN, TASK_TYPE, WORK_ITEMS);

        ArgumentCaptor<MaintenanceWindowTaskRequest> requestCaptor = ArgumentCaptor.forClass(MaintenanceWindowTaskRequest.class);
        verify(maintenanceWindowTaskEndpoint, times(2)).register(eq(ACCOUNT_ID), requestCaptor.capture());
        for (MaintenanceWindowTaskRequest request : requestCaptor.getAllValues()) {
            assertThat(request.getTaskType()).isEqualTo(TASK_TYPE);
            assertThat(request.getTaskKind()).isEqualTo(MaintenanceTaskKind.ONE_SHOT.name());
            assertThat(request.getEnvironmentCrn()).isEqualTo(ENV_CRN);
            assertThat(request.getRetryWithinOccurrence()).isTrue();
            assertThat(request.getMaxAttemptsPerOccurrence()).isEqualTo(3);
            assertThat(request.getRetryCooldownMinutes()).isEqualTo(15);
            assertThat(request.getSubmitterService()).isEqualTo(wireSubmitterService);
            assertThat(request.getExecutionRef())
                    .containsEntry(WIRE_EXECUTION_REF_SUBMITTER_SERVICE, wireSubmitterService)
                    .containsEntry(WIRE_EXECUTION_REF_EXECUTE_PATH, WIRE_EXECUTE_PATH);
        }
        assertWireTaskPayload(requestCaptor.getAllValues(), "SALT_PASSWORD", List.of("SALT_PASSWORD"));
        assertWireTaskPayload(requestCaptor.getAllValues(), "CM_ADMIN_PASSWORD", List.of("CM_ADMIN_PASSWORD"));
    }

    @Test
    void registerPropagatesServerErrorFromRegistrationOutcome() {
        MaintenanceWindowTaskRegistration underTest = registration(MaintenanceSubmitterService.CLOUDBREAK);
        when(scheduleLookup.hasConfiguredSchedule(ACCOUNT_ID, RESOURCE_CRN, ENV_CRN)).thenReturn(true);
        Response response = serverErrorRegistrationResponse();
        when(maintenanceWindowTaskEndpoint.register(eq(ACCOUNT_ID), any())).thenReturn(response);

        assertThatThrownBy(() -> underTest.register(ACCOUNT_ID, RESOURCE_CRN, ENV_CRN, TASK_TYPE, List.of(workItemFor("SALT_PASSWORD"))))
                .isInstanceOf(CloudbreakServiceException.class)
                .satisfies(ex -> assertThat(ex.getSuppressed())
                        .hasSize(1)
                        .allSatisfy(suppressed -> {
                            assertThat(suppressed).isInstanceOf(WebApplicationException.class);
                            assertThat(((WebApplicationException) suppressed).getResponse().getStatus())
                                    .isEqualTo(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
                        }));
    }

    @Test
    void registerSkipsConflictAndStillRegistersRemainingWorkItems() {
        MaintenanceWindowTaskRegistration underTest = registration(MaintenanceSubmitterService.CLOUDBREAK);
        when(scheduleLookup.hasConfiguredSchedule(ACCOUNT_ID, RESOURCE_CRN, ENV_CRN)).thenReturn(true);
        stubRegisterPerSecret("SALT_PASSWORD", conflictRegistrationResponse());
        stubRegisterPerSecret("CM_ADMIN_PASSWORD", successfulRegistrationResponse(taskResponse(99L)));

        underTest.register(ACCOUNT_ID, RESOURCE_CRN, ENV_CRN, TASK_TYPE, WORK_ITEMS);

        assertThat(registeredWorkItemIds()).containsExactly("SALT_PASSWORD", "CM_ADMIN_PASSWORD");
    }

    @Test
    void registerContinuesPastServerErrorThenThrowsAggregate() {
        MaintenanceWindowTaskRegistration underTest = registration(MaintenanceSubmitterService.CLOUDBREAK);
        when(scheduleLookup.hasConfiguredSchedule(ACCOUNT_ID, RESOURCE_CRN, ENV_CRN)).thenReturn(true);
        stubRegisterPerSecret("SALT_PASSWORD", serverErrorRegistrationResponse());
        stubRegisterPerSecret("CM_ADMIN_PASSWORD", successfulRegistrationResponse(taskResponse(99L)));

        assertThatThrownBy(() -> underTest.register(ACCOUNT_ID, RESOURCE_CRN, ENV_CRN, TASK_TYPE, WORK_ITEMS))
                .isInstanceOf(CloudbreakServiceException.class)
                .hasMessageContaining("1 of 2")
                .hasMessageContaining("SALT_PASSWORD");

        assertThat(registeredWorkItemIds()).containsExactly("SALT_PASSWORD", "CM_ADMIN_PASSWORD");
    }

    @Test
    void registerContinuesPastTransportFailure() {
        MaintenanceWindowTaskRegistration underTest = registration(MaintenanceSubmitterService.CLOUDBREAK);
        when(scheduleLookup.hasConfiguredSchedule(ACCOUNT_ID, RESOURCE_CRN, ENV_CRN)).thenReturn(true);
        when(maintenanceWindowTaskEndpoint.register(eq(ACCOUNT_ID), argThat(workItem("SALT_PASSWORD"))))
                .thenThrow(new ProcessingException("connection reset"));
        stubRegisterPerSecret("CM_ADMIN_PASSWORD", successfulRegistrationResponse(taskResponse(99L)));

        assertThatThrownBy(() -> underTest.register(ACCOUNT_ID, RESOURCE_CRN, ENV_CRN, TASK_TYPE, WORK_ITEMS))
                .isInstanceOf(CloudbreakServiceException.class)
                .hasMessageContaining("connection reset");

        assertThat(registeredWorkItemIds()).containsExactly("SALT_PASSWORD", "CM_ADMIN_PASSWORD");
    }

    @Test
    void registerAggregatesEveryRetryableFailure() {
        MaintenanceWindowTaskRegistration underTest = registration(MaintenanceSubmitterService.CLOUDBREAK);
        when(scheduleLookup.hasConfiguredSchedule(ACCOUNT_ID, RESOURCE_CRN, ENV_CRN)).thenReturn(true);
        Response response = serverErrorRegistrationResponse();
        when(maintenanceWindowTaskEndpoint.register(eq(ACCOUNT_ID), any())).thenReturn(response);

        assertThatThrownBy(() -> underTest.register(ACCOUNT_ID, RESOURCE_CRN, ENV_CRN, TASK_TYPE, WORK_ITEMS))
                .isInstanceOf(CloudbreakServiceException.class)
                .hasMessageContaining("2 of 2")
                .satisfies(ex -> assertThat(ex.getSuppressed()).hasSize(2));
    }

    @Test
    void registerDoesNotThrowWhenOnlyNonRetryableFailures() {
        MaintenanceWindowTaskRegistration underTest = registration(MaintenanceSubmitterService.CLOUDBREAK);
        when(scheduleLookup.hasConfiguredSchedule(ACCOUNT_ID, RESOURCE_CRN, ENV_CRN)).thenReturn(true);
        Response response = conflictRegistrationResponse();
        when(maintenanceWindowTaskEndpoint.register(eq(ACCOUNT_ID), any())).thenReturn(response);

        underTest.register(ACCOUNT_ID, RESOURCE_CRN, ENV_CRN, TASK_TYPE, WORK_ITEMS);

        assertThat(registeredWorkItemIds()).containsExactly("SALT_PASSWORD", "CM_ADMIN_PASSWORD");
    }

    private MaintenanceWindowTaskRegistration registration(MaintenanceSubmitterService submitter) {
        return new MaintenanceWindowTaskRegistration(
                maintenanceWindowTaskEndpoint, scheduleLookup, submitter, 3, 15);
    }

    private static WorkItem workItemFor(String secretName) {
        return new WorkItem(secretName, Map.of(WIRE_PAYLOAD_SECRET_NAMES, List.of(secretName)));
    }

    private static Stream<Arguments> submitters() {
        return Stream.of(
                Arguments.of(MaintenanceSubmitterService.CLOUDBREAK, "cloudbreak"),
                Arguments.of(MaintenanceSubmitterService.DATALAKE, "datalake"));
    }

    private static void assertWireTaskPayload(
            List<MaintenanceWindowTaskRequest> requests, String workItemId, List<String> secretNames) {
        MaintenanceWindowTaskRequest request = requests.stream()
                .filter(r -> workItemId.equals(r.getWorkItemId()))
                .findFirst()
                .orElseThrow();
        assertThat(request.getTaskPayload())
                .isEqualTo(Map.of(WIRE_PAYLOAD_SECRET_NAMES, secretNames));
    }

    private void stubRegisterPerSecret(String secretName, Response response) {
        when(maintenanceWindowTaskEndpoint.register(eq(ACCOUNT_ID), argThat(workItem(secretName)))).thenReturn(response);
    }

    private static ArgumentMatcher<MaintenanceWindowTaskRequest> workItem(String secretName) {
        return request -> request != null && secretName.equals(request.getWorkItemId());
    }

    private List<String> registeredWorkItemIds() {
        ArgumentCaptor<MaintenanceWindowTaskRequest> captor = ArgumentCaptor.forClass(MaintenanceWindowTaskRequest.class);
        verify(maintenanceWindowTaskEndpoint, atLeastOnce()).register(eq(ACCOUNT_ID), captor.capture());
        return captor.getAllValues().stream().map(MaintenanceWindowTaskRequest::getWorkItemId).toList();
    }

    private static MaintenanceWindowTaskResponse taskResponse(long id) {
        MaintenanceWindowTaskResponse taskResponse = new MaintenanceWindowTaskResponse();
        taskResponse.setId(id);
        return taskResponse;
    }

    private static Response successfulRegistrationResponse(MaintenanceWindowTaskResponse entity) {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(response.readEntity(MaintenanceWindowTaskResponse.class)).thenReturn(entity);
        return response;
    }

    private static Response conflictRegistrationResponse() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(Response.Status.CONFLICT.getStatusCode());
        when(response.hasEntity()).thenReturn(false);
        return response;
    }

    private static Response serverErrorRegistrationResponse() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
        when(response.hasEntity()).thenReturn(false);
        return response;
    }
}
