package com.sequenceiq.datalake.maintenancewindow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.rotation.maintenance.MaintenanceWindowSecretRotationSupport;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.maintenance.api.internal.schedule.endpoint.MaintenanceWindowScheduleInternalEndpoint;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleResponse;
import com.sequenceiq.maintenance.api.v1.task.endpoint.MaintenanceWindowTaskEndpoint;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskRequest;
import com.sequenceiq.maintenance.api.v1.task.model.response.MaintenanceWindowTaskResponse;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowSecretRotationTaskRegistrarTest {

    private static final String RESOURCE_CRN = "crn:cdp:datalake:us-west-1:acc-12345:datalake:dl-1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc-12345:environment:env";

    private static final String SECRET = "SALT_PASSWORD";

    @Mock
    private MaintenanceWindowTaskEndpoint maintenanceWindowTaskEndpoint;

    @Mock
    private MaintenanceWindowScheduleInternalEndpoint scheduleInternalEndpoint;

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxCluster cluster;

    @InjectMocks
    private MaintenanceWindowSecretRotationTaskRegistrar underTest;

    private MockedStatic<ThreadBasedUserCrnProvider> userCrnProvider;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "maxAttemptsPerOccurrence", 3);
        ReflectionTestUtils.setField(underTest, "retryCooldownMinutes", 15);
        underTest.initRegistration();
        userCrnProvider = mockStatic(ThreadBasedUserCrnProvider.class);
        userCrnProvider.when(() -> ThreadBasedUserCrnProvider.doAsInternalActor(any(Supplier.class), eq("acc-12345")))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
    }

    private void givenClusterExists() {
        when(cluster.getEnvCrn()).thenReturn(ENV_CRN);
        when(cluster.getAccountId()).thenReturn("acc-12345");
        when(sdxService.getByCrn(RESOURCE_CRN)).thenReturn(cluster);
    }

    private void givenScheduleExists() {
        when(scheduleInternalEndpoint.get("acc-12345", MaintenanceScopeType.DATALAKE.name(), RESOURCE_CRN))
                .thenReturn(new MaintenanceWindowScheduleResponse());
    }

    @AfterEach
    void tearDown() {
        userCrnProvider.close();
    }

    @Test
    void registerDueRotationThrowsWhenClusterMissingOrDeleted() {
        when(sdxService.getByCrn(RESOURCE_CRN)).thenThrow(NotFoundException.notFound("SDX cluster", RESOURCE_CRN).get());

        assertThatThrownBy(() -> underTest.registerDueRotation(RESOURCE_CRN, List.of(SECRET)))
                .isInstanceOf(NotFoundException.class);

        verify(maintenanceWindowTaskEndpoint, never()).register(any(), any());
    }

    @Test
    void registerDueRotationUsesEnvironmentCrnFromClusterAndDatalakeSubmitter() {
        givenClusterExists();
        givenScheduleExists();
        Response created = createdResponse(1L);
        when(maintenanceWindowTaskEndpoint.register(eq("acc-12345"), any())).thenReturn(created);

        underTest.registerDueRotation(RESOURCE_CRN, List.of(SECRET));

        MaintenanceWindowTaskRequest request = captureSingleRequest();
        assertThat(request.getEnvironmentCrn()).isEqualTo(ENV_CRN);
        assertThat(request.getSubmitterService()).isEqualTo("datalake");
        assertThat(request.getExecutionRef()).containsEntry("submitter_service", "datalake");
    }

    @Test
    void registerDueRotationMapsSecretNameToWorkItemIdAndPayload() {
        givenClusterExists();
        givenScheduleExists();
        Response created = createdResponse(1L);
        when(maintenanceWindowTaskEndpoint.register(eq("acc-12345"), any())).thenReturn(created);

        underTest.registerDueRotation(RESOURCE_CRN, List.of(SECRET));

        MaintenanceWindowTaskRequest request = captureSingleRequest();
        assertThat(request.getWorkItemId()).isEqualTo(MaintenanceWindowSecretRotationSupport.workItemIdForSecretName(SECRET));
        assertThat(request.getTaskPayload())
                .isEqualTo(MaintenanceWindowSecretRotationSupport.taskPayload(SECRET));
        assertThat(request.getTaskPayload())
                .isEqualTo(Map.of(MaintenanceWindowSecretRotationSupport.PAYLOAD_SECRET_NAMES, List.of(SECRET)));
    }

    private MaintenanceWindowTaskRequest captureSingleRequest() {
        ArgumentCaptor<MaintenanceWindowTaskRequest> captor = ArgumentCaptor.forClass(MaintenanceWindowTaskRequest.class);
        verify(maintenanceWindowTaskEndpoint).register(eq("acc-12345"), captor.capture());
        return captor.getValue();
    }

    private static Response createdResponse(long taskId) {
        MaintenanceWindowTaskResponse entity = new MaintenanceWindowTaskResponse();
        entity.setId(taskId);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(response.readEntity(MaintenanceWindowTaskResponse.class)).thenReturn(entity);
        return response;
    }
}
