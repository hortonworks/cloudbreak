package com.sequenceiq.cloudbreak.maintenancewindow;

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

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.maintenance.MaintenanceWindowSecretRotationSupport;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.maintenance.api.model.MaintenanceTaskKind;
import com.sequenceiq.maintenance.api.v1.task.endpoint.MaintenanceWindowTaskEndpoint;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskRequest;
import com.sequenceiq.maintenance.api.v1.task.model.response.MaintenanceWindowTaskResponse;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowSecretRotationTaskRegistrarTest {

    private static final String RESOURCE_CRN = "crn:cdp:datahub:us-west-1:acc-12345:cluster:my-dh";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc-12345:environment:env";

    private static final List<String> DUE_SECRETS = List.of("SALT_PASSWORD", "CM_ADMIN_PASSWORD");

    /** JSON keys and values the maintenance dispatcher and Core execute handler expect on the wire. */
    private static final String WIRE_SUBMITTER_SERVICE = "cloudbreak";

    private static final String WIRE_EXECUTION_REF_SUBMITTER_SERVICE = "submitter_service";

    private static final String WIRE_EXECUTION_REF_EXECUTE_PATH = "execute_path";

    private static final String WIRE_EXECUTE_PATH = "/api/v1/internal/maintenance-tasks/execute";

    private static final String WIRE_PAYLOAD_SECRET_NAMES = "secretNames";

    @Mock
    private MaintenanceWindowTaskEndpoint maintenanceWindowTaskEndpoint;

    @Mock
    private MaintenanceWindowScheduleLookupService scheduleLookupService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackDto stackDto;

    private MaintenanceWindowSecretRotationTaskRegistrar underTest;

    @BeforeEach
    void setup() {
        underTest = new MaintenanceWindowSecretRotationTaskRegistrar(
                maintenanceWindowTaskEndpoint, scheduleLookupService, stackDtoService, 3, 15);
        when(stackDto.getEnvironmentCrn()).thenReturn(ENV_CRN);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
    }

    @Test
    void registerDueRotationSkipsWhenNoSchedule() {
        when(scheduleLookupService.hasConfiguredSchedule("acc-12345", RESOURCE_CRN, ENV_CRN)).thenReturn(false);

        underTest.registerDueRotation(RESOURCE_CRN, List.of("SALT_PASSWORD"));

        verify(maintenanceWindowTaskEndpoint, never()).register(any(), any());
    }

    @Test
    void registerDueRotationRegistersOneShotTaskWithRetryPolicy() {
        when(scheduleLookupService.hasConfiguredSchedule("acc-12345", RESOURCE_CRN, ENV_CRN)).thenReturn(true);
        Response created = successfulRegistrationResponse(taskResponse(99L));
        when(maintenanceWindowTaskEndpoint.register(eq("acc-12345"), any())).thenReturn(created);

        underTest.registerDueRotation(RESOURCE_CRN, DUE_SECRETS);

        ArgumentCaptor<MaintenanceWindowTaskRequest> requestCaptor = ArgumentCaptor.forClass(MaintenanceWindowTaskRequest.class);
        verify(maintenanceWindowTaskEndpoint, times(2)).register(eq("acc-12345"), requestCaptor.capture());
        assertThat(requestCaptor.getAllValues()).hasSize(2);
        for (MaintenanceWindowTaskRequest request : requestCaptor.getAllValues()) {
            assertThat(request.getTaskType()).isEqualTo(MaintenanceWindowSecretRotationSupport.TASK_TYPE);
            assertThat(request.getTaskKind()).isEqualTo(MaintenanceTaskKind.ONE_SHOT.name());
            assertThat(request.getEnvironmentCrn()).isEqualTo(ENV_CRN);
            assertThat(request.getRetryWithinOccurrence()).isTrue();
            assertThat(request.getMaxAttemptsPerOccurrence()).isEqualTo(3);
            assertThat(request.getRetryCooldownMinutes()).isEqualTo(15);
            assertThat(request.getSubmitterService()).isEqualTo(WIRE_SUBMITTER_SERVICE);
            assertThat(request.getExecutionRef())
                    .containsEntry(WIRE_EXECUTION_REF_SUBMITTER_SERVICE, WIRE_SUBMITTER_SERVICE)
                    .containsEntry(WIRE_EXECUTION_REF_EXECUTE_PATH, WIRE_EXECUTE_PATH);
        }
        assertWireTaskPayload(requestCaptor.getAllValues(), "SALT_PASSWORD", List.of("SALT_PASSWORD"));
        assertWireTaskPayload(requestCaptor.getAllValues(), "CM_ADMIN_PASSWORD", List.of("CM_ADMIN_PASSWORD"));
    }

    /**
     * {@code logRegistrationOutcome} must surface 5xx via {@link WebApplicationException} (not
     * {@code ClientErrorException}, which rejects non-4xx with {@link IllegalArgumentException}).
     */
    @Test
    void registerDueRotationPropagatesServerErrorFromRegistrationOutcome() {
        when(scheduleLookupService.hasConfiguredSchedule("acc-12345", RESOURCE_CRN, ENV_CRN)).thenReturn(true);
        Response serverError = serverErrorRegistrationResponse();
        when(maintenanceWindowTaskEndpoint.register(eq("acc-12345"), any())).thenReturn(serverError);

        assertThatThrownBy(() -> underTest.registerDueRotation(RESOURCE_CRN, List.of("SALT_PASSWORD")))
                .isInstanceOf(CloudbreakServiceException.class)
                .satisfies(ex -> assertThat(ex.getSuppressed())
                        .hasSize(1)
                        .allSatisfy(suppressed -> {
                            assertThat(suppressed).isInstanceOf(WebApplicationException.class);
                            assertThat(((WebApplicationException) suppressed).getResponse().getStatus())
                                    .isEqualTo(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
                        }));
    }

    /**
     * A 409 means an ACTIVE task already exists for that secret with a different config — it recurs identically every
     * tick, so it must not be fatal and must not stop the other secrets.
     */
    @Test
    void registerDueRotationSkipsConflictAndStillRegistersRemainingSecrets() {
        when(scheduleLookupService.hasConfiguredSchedule("acc-12345", RESOURCE_CRN, ENV_CRN)).thenReturn(true);
        stubRegisterPerSecret("SALT_PASSWORD", conflictRegistrationResponse());
        stubRegisterPerSecret("CM_ADMIN_PASSWORD", successfulRegistrationResponse(taskResponse(99L)));

        underTest.registerDueRotation(RESOURCE_CRN, DUE_SECRETS);

        assertThat(registeredWorkItemIds()).containsExactly("SALT_PASSWORD", "CM_ADMIN_PASSWORD");
    }

    /**
     * The regression this guards: a 5xx on the first secret must not leave later due secrets unregistered, because an
     * aborting loop starves them on every subsequent tick too.
     */
    @Test
    void registerDueRotationContinuesPastServerErrorThenThrowsAggregate() {
        when(scheduleLookupService.hasConfiguredSchedule("acc-12345", RESOURCE_CRN, ENV_CRN)).thenReturn(true);
        stubRegisterPerSecret("SALT_PASSWORD", serverErrorRegistrationResponse());
        stubRegisterPerSecret("CM_ADMIN_PASSWORD", successfulRegistrationResponse(taskResponse(99L)));

        assertThatThrownBy(() -> underTest.registerDueRotation(RESOURCE_CRN, DUE_SECRETS))
                .isInstanceOf(CloudbreakServiceException.class)
                .hasMessageContaining("1 of 2")
                .hasMessageContaining("SALT_PASSWORD")
                .satisfies(ex -> assertThat(ex.getSuppressed())
                        .hasSize(1)
                        .allSatisfy(suppressed -> assertThat(suppressed).isInstanceOf(WebApplicationException.class)));

        assertThat(registeredWorkItemIds()).containsExactly("SALT_PASSWORD", "CM_ADMIN_PASSWORD");
    }

    /**
     * Transport failures are {@code ProcessingException}, not {@code WebApplicationException}; they must be collected
     * too, or one connection reset aborts the rest.
     */
    @Test
    void registerDueRotationContinuesPastTransportFailure() {
        when(scheduleLookupService.hasConfiguredSchedule("acc-12345", RESOURCE_CRN, ENV_CRN)).thenReturn(true);
        when(maintenanceWindowTaskEndpoint.register(eq("acc-12345"), argThat(workItem("SALT_PASSWORD"))))
                .thenThrow(new ProcessingException("connection reset"));
        stubRegisterPerSecret("CM_ADMIN_PASSWORD", successfulRegistrationResponse(taskResponse(99L)));

        assertThatThrownBy(() -> underTest.registerDueRotation(RESOURCE_CRN, DUE_SECRETS))
                .isInstanceOf(CloudbreakServiceException.class)
                .hasMessageContaining("connection reset")
                .satisfies(ex -> assertThat(ex.getSuppressed())
                        .hasSize(1)
                        .allSatisfy(suppressed -> assertThat(suppressed).isInstanceOf(ProcessingException.class)));

        assertThat(registeredWorkItemIds()).containsExactly("SALT_PASSWORD", "CM_ADMIN_PASSWORD");
    }

    @Test
    void registerDueRotationAggregatesEveryRetryableFailure() {
        when(scheduleLookupService.hasConfiguredSchedule("acc-12345", RESOURCE_CRN, ENV_CRN)).thenReturn(true);
        Response serverError = serverErrorRegistrationResponse();
        when(maintenanceWindowTaskEndpoint.register(eq("acc-12345"), any())).thenReturn(serverError);

        assertThatThrownBy(() -> underTest.registerDueRotation(RESOURCE_CRN, DUE_SECRETS))
                .isInstanceOf(CloudbreakServiceException.class)
                .hasMessageContaining("2 of 2")
                .satisfies(ex -> assertThat(ex.getSuppressed()).hasSize(2));
    }

    @Test
    void registerDueRotationDoesNotThrowWhenOnlyNonRetryableFailures() {
        when(scheduleLookupService.hasConfiguredSchedule("acc-12345", RESOURCE_CRN, ENV_CRN)).thenReturn(true);
        Response conflict = conflictRegistrationResponse();
        when(maintenanceWindowTaskEndpoint.register(eq("acc-12345"), any())).thenReturn(conflict);

        underTest.registerDueRotation(RESOURCE_CRN, DUE_SECRETS);

        assertThat(registeredWorkItemIds()).containsExactly("SALT_PASSWORD", "CM_ADMIN_PASSWORD");
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
        when(maintenanceWindowTaskEndpoint.register(eq("acc-12345"), argThat(workItem(secretName)))).thenReturn(response);
    }

    private static ArgumentMatcher<MaintenanceWindowTaskRequest> workItem(String secretName) {
        return request -> request != null && secretName.equals(request.getWorkItemId());
    }

    private List<String> registeredWorkItemIds() {
        ArgumentCaptor<MaintenanceWindowTaskRequest> captor = ArgumentCaptor.forClass(MaintenanceWindowTaskRequest.class);
        verify(maintenanceWindowTaskEndpoint, atLeastOnce()).register(eq("acc-12345"), captor.capture());
        return captor.getAllValues().stream().map(MaintenanceWindowTaskRequest::getWorkItemId).toList();
    }

    private static MaintenanceWindowTaskResponse taskResponse(long id) {
        MaintenanceWindowTaskResponse taskResponse = new MaintenanceWindowTaskResponse();
        taskResponse.setId(id);
        return taskResponse;
    }

    /**
     * {@link Response#status(int)} builds an outbound message; {@code readEntity} (used by the registrar) requires an inbound/client response.
     */
    private static Response successfulRegistrationResponse(MaintenanceWindowTaskResponse entity) {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(response.readEntity(MaintenanceWindowTaskResponse.class)).thenReturn(entity);
        return response;
    }

    private static Response conflictRegistrationResponse() {
        Response response = mock(Response.class);
        int status = Response.Status.CONFLICT.getStatusCode();
        when(response.getStatus()).thenReturn(status);
        when(response.hasEntity()).thenReturn(false);
        return response;
    }

    private static Response serverErrorRegistrationResponse() {
        Response response = mock(Response.class);
        int status = Response.Status.SERVICE_UNAVAILABLE.getStatusCode();
        when(response.getStatus()).thenReturn(status);
        when(response.hasEntity()).thenReturn(false);
        return response;
    }
}
