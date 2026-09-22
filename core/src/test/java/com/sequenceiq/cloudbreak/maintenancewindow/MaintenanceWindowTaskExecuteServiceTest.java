package com.sequenceiq.cloudbreak.maintenancewindow;

import static com.sequenceiq.cloudbreak.maintenancewindow.MaintenanceTaskDispatchRequestTestBuilder.aDispatchRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.v1.maintenance.model.MaintenanceTaskDispatchRequest;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.maintenancewindow.handler.MaintenanceWindowTaskExecuteHandler;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowTaskExecuteServiceTest {

    @Mock
    private MaintenanceWindowTaskExecuteHandler secretRotationHandler;

    private MaintenanceWindowTaskExecuteService underTest;

    @BeforeEach
    void setup() {
        when(secretRotationHandler.taskType()).thenReturn(MaintenanceWindowSecretRotationSupport.TASK_TYPE);
        underTest = new MaintenanceWindowTaskExecuteService(List.of(secretRotationHandler));
    }

    @Test
    void executeDelegatesToHandlerForKnownTaskType() {
        MaintenanceTaskDispatchRequest request = dispatchRequest(MaintenanceWindowSecretRotationSupport.TASK_TYPE);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "pollable-1");
        when(secretRotationHandler.execute(request)).thenReturn(flowIdentifier);

        FlowIdentifier result = underTest.execute(request);

        assertThat(result).isSameAs(flowIdentifier);
        verify(secretRotationHandler).execute(request);
    }

    @Test
    void executeRejectsUnsupportedTaskType() {
        MaintenanceTaskDispatchRequest request = dispatchRequest("OTHER");
        assertThatThrownBy(() -> underTest.execute(request)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void constructorRejectsDuplicateTaskTypeHandlers() {
        MaintenanceWindowTaskExecuteHandler duplicate = mock(MaintenanceWindowTaskExecuteHandler.class);
        when(duplicate.taskType()).thenReturn(MaintenanceWindowSecretRotationSupport.TASK_TYPE);
        assertThatThrownBy(() -> new MaintenanceWindowTaskExecuteService(List.of(secretRotationHandler, duplicate)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate key");
    }

    private static MaintenanceTaskDispatchRequest dispatchRequest(String taskType) {
        return aDispatchRequest()
                .withTaskType(taskType)
                .withTaskPayload(Map.of())
                .build();
    }
}
