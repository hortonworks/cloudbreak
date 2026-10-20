package com.sequenceiq.freeipa.maintenancewindow.handler;

import static com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties.IGNORE_PREVALIDATE_ERRORS;
import static com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties.MAINTENANCE_WINDOW_ACCOUNT_ID;
import static com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties.MAINTENANCE_WINDOW_RUN_ID;
import static com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties.MAINTENANCE_WINDOW_TASK_ID;
import static com.sequenceiq.freeipa.maintenancewindow.MaintenanceTaskDispatchRequestTestBuilder.ACCOUNT_ID;
import static com.sequenceiq.freeipa.maintenancewindow.MaintenanceTaskDispatchRequestTestBuilder.RESOURCE_CRN;
import static com.sequenceiq.freeipa.maintenancewindow.MaintenanceTaskDispatchRequestTestBuilder.aDispatchRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;
import com.sequenceiq.cloudbreak.rotation.maintenance.MaintenanceWindowSecretRotationSupport;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeIpaSecretRotationRequest;
import com.sequenceiq.freeipa.api.v1.maintenance.model.MaintenanceTaskDispatchRequest;
import com.sequenceiq.freeipa.entity.projection.FreeIpaListView;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.rotation.FreeIpaSecretRotationService;

@ExtendWith(MockitoExtension.class)
class SecretRotationMaintenanceWindowTaskExecuteHandlerTest {

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc-12345:environment:env";

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private FreeIpaSecretRotationService freeIpaSecretRotationService;

    @InjectMocks
    private SecretRotationMaintenanceWindowTaskExecuteHandler underTest;

    @Test
    void taskTypeIsSecretRotation() {
        assertThat(underTest.taskType()).isEqualTo(MaintenanceWindowSecretRotationSupport.TASK_TYPE);
    }

    @Test
    void executeAcceptsSecretRotationAndReturnsFlowIdentifier() {
        givenFreeIpaExists();
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "pollable-1");
        when(freeIpaSecretRotationService.rotateSecretsByCrn(eq(ACCOUNT_ID), eq(ENV_CRN), any(FreeIpaSecretRotationRequest.class)))
                .thenReturn(flowIdentifier);

        FlowIdentifier result = underTest.execute(aDispatchRequest()
                .withTaskPayload(Map.of(MaintenanceWindowSecretRotationSupport.PAYLOAD_SECRET_NAMES, List.of("SALT_PASSWORD")))
                .build());

        assertThat(result).isSameAs(flowIdentifier);
        ArgumentCaptor<FreeIpaSecretRotationRequest> requestCaptor = ArgumentCaptor.forClass(FreeIpaSecretRotationRequest.class);
        verify(freeIpaSecretRotationService).rotateSecretsByCrn(eq(ACCOUNT_ID), eq(ENV_CRN), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getSecrets()).containsExactly("SALT_PASSWORD");
        assertThat(requestCaptor.getValue().getAdditionalProperties())
                .containsEntry(IGNORE_PREVALIDATE_ERRORS, "true")
                .containsEntry(MAINTENANCE_WINDOW_TASK_ID, "10")
                .containsEntry(MAINTENANCE_WINDOW_RUN_ID, "20")
                .containsEntry(MAINTENANCE_WINDOW_ACCOUNT_ID, ACCOUNT_ID);
    }

    @Test
    void executePropagatesFlowsAlreadyRunningFromRotateSecretsByCrn() {
        givenFreeIpaExists();
        when(freeIpaSecretRotationService.rotateSecretsByCrn(eq(ACCOUNT_ID), eq(ENV_CRN), any(FreeIpaSecretRotationRequest.class)))
                .thenThrow(new FlowsAlreadyRunningException("already running"));
        MaintenanceTaskDispatchRequest request = aDispatchRequest().build();

        assertThatThrownBy(() -> underTest.execute(request)).isInstanceOf(FlowsAlreadyRunningException.class);
    }

    @Test
    void executeRejectsPayloadSecretNamesThatDisagreeWithWorkItemId() {
        MaintenanceTaskDispatchRequest request = aDispatchRequest()
                .withTaskPayload(Map.of(MaintenanceWindowSecretRotationSupport.PAYLOAD_SECRET_NAMES, List.of("CM_ADMIN_PASSWORD")))
                .build();

        assertThatThrownBy(() -> underTest.execute(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must contain only");
    }

    private void givenFreeIpaExists() {
        FreeIpaListView view = new FreeIpaListView("domain", "name", RESOURCE_CRN, ENV_CRN, null);
        when(freeIpaService.getViewByResourceCrn(RESOURCE_CRN)).thenReturn(Optional.of(view));
    }
}
