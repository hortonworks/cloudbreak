package com.sequenceiq.datalake.maintenancewindow.handler;

import static com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties.IGNORE_PREVALIDATE_ERRORS;
import static com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties.MAINTENANCE_WINDOW_ACCOUNT_ID;
import static com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties.MAINTENANCE_WINDOW_RUN_ID;
import static com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties.MAINTENANCE_WINDOW_TASK_ID;
import static com.sequenceiq.datalake.maintenancewindow.MaintenanceTaskDispatchRequestTestBuilder.ACCOUNT_ID;
import static com.sequenceiq.datalake.maintenancewindow.MaintenanceTaskDispatchRequestTestBuilder.RESOURCE_CRN;
import static com.sequenceiq.datalake.maintenancewindow.MaintenanceTaskDispatchRequestTestBuilder.aDispatchRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;
import com.sequenceiq.cloudbreak.rotation.maintenance.MaintenanceWindowSecretRotationSupport;
import com.sequenceiq.datalake.service.rotation.SdxRotationService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.v1.maintenance.model.MaintenanceTaskDispatchRequest;

@ExtendWith(MockitoExtension.class)
class SecretRotationMaintenanceWindowTaskExecuteHandlerTest {

    @Mock
    private SdxRotationService sdxRotationService;

    @InjectMocks
    private SecretRotationMaintenanceWindowTaskExecuteHandler underTest;

    @Test
    void taskTypeIsSecretRotation() {
        assertThat(underTest.taskType()).isEqualTo(MaintenanceWindowSecretRotationSupport.TASK_TYPE);
    }

    @Test
    void executeAcceptsSecretRotationAndReturnsFlowIdentifier() {
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "pollable-1");
        when(sdxRotationService.triggerSecretRotation(eq(RESOURCE_CRN), eq(List.of("SALT_PASSWORD")), isNull(), anyMap()))
                .thenReturn(flowIdentifier);

        FlowIdentifier result = underTest.execute(aDispatchRequest()
                .withTaskPayload(Map.of(MaintenanceWindowSecretRotationSupport.PAYLOAD_SECRET_NAMES, List.of("SALT_PASSWORD")))
                .build());

        assertThat(result).isSameAs(flowIdentifier);
        ArgumentCaptor<Map<String, String>> propsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(sdxRotationService).triggerSecretRotation(eq(RESOURCE_CRN), eq(List.of("SALT_PASSWORD")), isNull(), propsCaptor.capture());
        assertThat(propsCaptor.getValue())
                .containsEntry(IGNORE_PREVALIDATE_ERRORS, "true")
                .containsEntry(MAINTENANCE_WINDOW_TASK_ID, "10")
                .containsEntry(MAINTENANCE_WINDOW_RUN_ID, "20")
                .containsEntry(MAINTENANCE_WINDOW_ACCOUNT_ID, ACCOUNT_ID);
    }

    @Test
    void executePropagatesFlowsAlreadyRunningFromTriggerSecretRotation() {
        when(sdxRotationService.triggerSecretRotation(eq(RESOURCE_CRN), eq(List.of("SALT_PASSWORD")), isNull(), anyMap()))
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
}
