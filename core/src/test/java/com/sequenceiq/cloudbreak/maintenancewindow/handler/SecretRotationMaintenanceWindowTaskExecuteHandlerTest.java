package com.sequenceiq.cloudbreak.maintenancewindow.handler;

import static com.sequenceiq.cloudbreak.maintenancewindow.MaintenanceTaskDispatchRequestTestBuilder.ACCOUNT_ID;
import static com.sequenceiq.cloudbreak.maintenancewindow.MaintenanceTaskDispatchRequestTestBuilder.RESOURCE_CRN;
import static com.sequenceiq.cloudbreak.maintenancewindow.MaintenanceTaskDispatchRequestTestBuilder.aDispatchRequest;
import static com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties.IGNORE_PREVALIDATE_ERRORS;
import static com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties.MAINTENANCE_WINDOW_ACCOUNT_ID;
import static com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties.MAINTENANCE_WINDOW_RUN_ID;
import static com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties.MAINTENANCE_WINDOW_TASK_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
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

import com.sequenceiq.cloudbreak.api.v1.maintenance.model.MaintenanceTaskDispatchRequest;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;
import com.sequenceiq.cloudbreak.rotation.maintenance.MaintenanceWindowSecretRotationSupport;
import com.sequenceiq.cloudbreak.service.stack.flow.StackRotationService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class SecretRotationMaintenanceWindowTaskExecuteHandlerTest {

    @Mock
    private StackRotationService stackRotationService;

    @InjectMocks
    private SecretRotationMaintenanceWindowTaskExecuteHandler underTest;

    @Test
    void taskTypeIsSecretRotation() {
        assertThat(underTest.taskType()).isEqualTo(MaintenanceWindowSecretRotationSupport.TASK_TYPE);
    }

    @Test
    void executeAcceptsSecretRotationAndReturnsFlowIdentifier() {
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "pollable-1");
        when(stackRotationService.rotateSecrets(eq(RESOURCE_CRN), eq(List.of("SALT_PASSWORD")), eq(null), anyMap()))
                .thenReturn(flowIdentifier);
        MaintenanceTaskDispatchRequest request = dispatchRequest(Map.of("secretNames", List.of("SALT_PASSWORD")));

        FlowIdentifier result = underTest.execute(request);

        assertThat(result).isSameAs(flowIdentifier);
        ArgumentCaptor<Map<String, String>> propsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(stackRotationService).rotateSecrets(eq(RESOURCE_CRN), eq(List.of("SALT_PASSWORD")), eq(null), propsCaptor.capture());
        assertThat(propsCaptor.getValue())
                .containsEntry(IGNORE_PREVALIDATE_ERRORS, "true")
                .containsEntry(MAINTENANCE_WINDOW_TASK_ID, "10")
                .containsEntry(MAINTENANCE_WINDOW_RUN_ID, "20")
                .containsEntry(MAINTENANCE_WINDOW_ACCOUNT_ID, ACCOUNT_ID);
    }

    @Test
    void executePropagatesFlowsAlreadyRunningFromRotateSecrets() {
        when(stackRotationService.rotateSecrets(eq(RESOURCE_CRN), eq(List.of("SALT_PASSWORD")), eq(null), anyMap()))
                .thenThrow(new FlowsAlreadyRunningException("already running"));
        MaintenanceTaskDispatchRequest request = dispatchRequest(Map.of("secretNames", List.of("SALT_PASSWORD")));
        assertThatThrownBy(() -> underTest.execute(request)).isInstanceOf(FlowsAlreadyRunningException.class);
    }

    @Test
    void executeUsesWorkItemIdWhenTaskPayloadIsAbsent() {
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "pollable-1");
        when(stackRotationService.rotateSecrets(eq(RESOURCE_CRN), eq(List.of("SALT_PASSWORD")), eq(null), anyMap()))
                .thenReturn(flowIdentifier);
        MaintenanceTaskDispatchRequest request = aDispatchRequest().build();

        FlowIdentifier result = underTest.execute(request);

        assertThat(result).isSameAs(flowIdentifier);
        verify(stackRotationService).rotateSecrets(eq(RESOURCE_CRN), eq(List.of("SALT_PASSWORD")), eq(null), anyMap());
    }

    @Test
    void executeRejectsPayloadSecretNamesThatDisagreeWithWorkItemId() {
        MaintenanceTaskDispatchRequest request = aDispatchRequest()
                .withTaskPayload(Map.of("secretNames", List.of("CM_ADMIN_PASSWORD")))
                .build();
        assertThatThrownBy(() -> underTest.execute(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must contain only");
    }

    @Test
    void executeRejectsEmptySecretNamesListInPayload() {
        assertThatThrownBy(() -> underTest.execute(dispatchRequest(Map.of("secretNames", List.of()))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("list of one secret type name");
    }

    @Test
    void executeRejectsMultiSecretWorkItemId() {
        MaintenanceTaskDispatchRequest request = aDispatchRequest().build();
        request.setWorkItemId("CM_ADMIN_PASSWORD,SALT_PASSWORD");
        assertThatThrownBy(() -> underTest.execute(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("single secret type name");
    }

    @Test
    void executeRejectsMultiSecretPayload() {
        MaintenanceTaskDispatchRequest request = aDispatchRequest()
                .withTaskPayload(Map.of("secretNames", List.of("SALT_PASSWORD", "CM_ADMIN_PASSWORD")))
                .build();
        assertThatThrownBy(() -> underTest.execute(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("list of one secret type name");
    }

    @Test
    void executeRejectsNonStringSecretNameEntries() {
        assertThatThrownBy(() -> underTest.execute(dispatchRequest(Map.of("secretNames", List.of(123)))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("list of one secret type name");
    }

    private static MaintenanceTaskDispatchRequest dispatchRequest(Map<String, Object> payload) {
        return aDispatchRequest()
                .withTaskPayload(payload)
                .build();
    }
}
