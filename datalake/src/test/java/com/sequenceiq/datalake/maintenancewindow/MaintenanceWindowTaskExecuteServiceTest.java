package com.sequenceiq.datalake.maintenancewindow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.rotation.maintenance.MaintenanceWindowSecretRotationSupport;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.maintenance.api.execution.MaintenanceWindowTaskExecuteHandler;
import com.sequenceiq.sdx.api.v1.maintenance.model.MaintenanceTaskDispatchRequest;

/**
 * {@link MaintenanceWindowTaskExecuteService} uses field injection ({@code NoInjectCtorService}), so tests wire the
 * handlers through {@link #serviceWith(List)} the way Spring does.
 */
@ExtendWith(MockitoExtension.class)
class MaintenanceWindowTaskExecuteServiceTest {

    @Mock
    private MaintenanceWindowTaskExecuteHandler<MaintenanceTaskDispatchRequest> secretRotationHandler;

    private MaintenanceWindowTaskExecuteService underTest;

    @BeforeEach
    void setUp() throws IllegalAccessException {
        when(secretRotationHandler.taskType()).thenReturn(MaintenanceWindowSecretRotationSupport.TASK_TYPE);
        underTest = serviceWith(List.of(secretRotationHandler));
    }

    @Test
    void executeDelegatesToHandlerForKnownTaskType() {
        MaintenanceTaskDispatchRequest request = MaintenanceTaskDispatchRequestTestBuilder.aDispatchRequest().build();
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "flow-1");
        when(secretRotationHandler.execute(request)).thenReturn(flowIdentifier);

        FlowIdentifier result = underTest.execute(request);

        assertThat(result).isSameAs(flowIdentifier);
        verify(secretRotationHandler).execute(request);
    }

    @Test
    void executeRejectsUnsupportedTaskType() {
        MaintenanceTaskDispatchRequest request = MaintenanceTaskDispatchRequestTestBuilder.aDispatchRequest()
                .withTaskType("OTHER")
                .build();

        assertThatThrownBy(() -> underTest.execute(request)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void buildHandlerMapRejectsDuplicateTaskTypeHandlers() {
        MaintenanceWindowTaskExecuteHandler<MaintenanceTaskDispatchRequest> duplicate = mock(MaintenanceWindowTaskExecuteHandler.class);
        when(duplicate.taskType()).thenReturn(MaintenanceWindowSecretRotationSupport.TASK_TYPE);
        assertThatThrownBy(() -> serviceWith(List.of(secretRotationHandler, duplicate)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate key");
    }

    private static MaintenanceWindowTaskExecuteService serviceWith(List<MaintenanceWindowTaskExecuteHandler<MaintenanceTaskDispatchRequest>> handlers)
            throws IllegalAccessException {
        MaintenanceWindowTaskExecuteService service = new MaintenanceWindowTaskExecuteService();
        FieldUtils.writeField(service, "handlers", handlers, true);
        service.buildHandlerMap();
        return service;
    }
}
