package com.sequenceiq.freeipa.maintenancewindow;

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
import java.util.Optional;
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
import com.sequenceiq.freeipa.entity.projection.FreeIpaListView;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.maintenance.api.internal.schedule.endpoint.MaintenanceWindowScheduleInternalEndpoint;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleResponse;
import com.sequenceiq.maintenance.api.v1.task.endpoint.MaintenanceWindowTaskEndpoint;
import com.sequenceiq.maintenance.api.v1.task.model.request.MaintenanceWindowTaskRequest;
import com.sequenceiq.maintenance.api.v1.task.model.response.MaintenanceWindowTaskResponse;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowSecretRotationTaskRegistrarTest {

    private static final String RESOURCE_CRN = "crn:cdp:freeipa:us-west-1:acc-12345:freeipa:ipa-1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc-12345:environment:env";

    private static final String SECRET = "SALT_PASSWORD";

    @Mock
    private MaintenanceWindowTaskEndpoint maintenanceWindowTaskEndpoint;

    @Mock
    private MaintenanceWindowScheduleInternalEndpoint scheduleInternalEndpoint;

    @Mock
    private FreeIpaService freeIpaService;

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

    private void givenFreeIpaExists() {
        FreeIpaListView view = new FreeIpaListView("domain", "name", RESOURCE_CRN, ENV_CRN, null);
        when(freeIpaService.getViewByResourceCrn(RESOURCE_CRN)).thenReturn(Optional.of(view));
    }

    private void givenScheduleExists() {
        when(scheduleInternalEndpoint.get("acc-12345", MaintenanceScopeType.FREEIPA.name(), RESOURCE_CRN))
                .thenReturn(new MaintenanceWindowScheduleResponse());
    }

    @AfterEach
    void tearDown() {
        userCrnProvider.close();
    }

    @Test
    void registerDueRotationThrowsWhenFreeIpaMissing() {
        when(freeIpaService.getViewByResourceCrn(RESOURCE_CRN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.registerDueRotation(RESOURCE_CRN, List.of(SECRET)))
                .isInstanceOf(NotFoundException.class);

        verify(maintenanceWindowTaskEndpoint, never()).register(any(), any());
    }

    @Test
    void registerDueRotationUsesEnvironmentCrnFromFreeIpaAndFreeipaSubmitter() {
        givenFreeIpaExists();
        givenScheduleExists();
        Response created = createdResponse(1L);
        when(maintenanceWindowTaskEndpoint.register(eq("acc-12345"), any())).thenReturn(created);

        underTest.registerDueRotation(RESOURCE_CRN, List.of(SECRET));

        MaintenanceWindowTaskRequest request = captureSingleRequest();
        assertThat(request.getEnvironmentCrn()).isEqualTo(ENV_CRN);
        assertThat(request.getSubmitterService()).isEqualTo("freeipa");
        assertThat(request.getExecutionRef()).containsEntry("submitter_service", "freeipa");
    }

    @Test
    void registerDueRotationMapsSecretNameToWorkItemIdAndPayload() {
        givenFreeIpaExists();
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
