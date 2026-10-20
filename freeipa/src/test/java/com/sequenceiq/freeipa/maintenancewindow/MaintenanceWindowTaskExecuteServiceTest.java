package com.sequenceiq.freeipa.maintenancewindow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.rotation.maintenance.MaintenanceWindowSecretRotationSupport;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.freeipa.api.v1.maintenance.model.MaintenanceTaskDispatchRequest;
import com.sequenceiq.maintenance.api.execution.MaintenanceWindowTaskExecuteHandler;

class MaintenanceWindowTaskExecuteServiceTest {

    @Test
    void executeRoutesToHandlerForTaskType() throws IllegalAccessException {
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "flow-1");
        MaintenanceWindowTaskExecuteHandler<MaintenanceTaskDispatchRequest> secretRotationHandler =
                mock(MaintenanceWindowTaskExecuteHandler.class);
        when(secretRotationHandler.taskType()).thenReturn(MaintenanceWindowSecretRotationSupport.TASK_TYPE);
        MaintenanceTaskDispatchRequest request = MaintenanceTaskDispatchRequestTestBuilder.aDispatchRequest().build();
        when(secretRotationHandler.execute(request)).thenReturn(flowIdentifier);

        MaintenanceWindowTaskExecuteService underTest = serviceWith(List.of(secretRotationHandler));

        assertThat(underTest.execute(request)).isSameAs(flowIdentifier);
    }

    @Test
    void executeRejectsUnknownTaskType() throws IllegalAccessException {
        MaintenanceWindowTaskExecuteHandler<MaintenanceTaskDispatchRequest> secretRotationHandler =
                mock(MaintenanceWindowTaskExecuteHandler.class);
        when(secretRotationHandler.taskType()).thenReturn(MaintenanceWindowSecretRotationSupport.TASK_TYPE);
        MaintenanceWindowTaskExecuteService underTest = serviceWith(List.of(secretRotationHandler));
        MaintenanceTaskDispatchRequest request = MaintenanceTaskDispatchRequestTestBuilder.aDispatchRequest()
                .withTaskType("OTHER")
                .build();

        assertThatThrownBy(() -> underTest.execute(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported task_type");
    }

    @Test
    void duplicateTaskTypesFailAtStartup() throws IllegalAccessException {
        MaintenanceWindowTaskExecuteHandler<MaintenanceTaskDispatchRequest> first = mock(MaintenanceWindowTaskExecuteHandler.class);
        MaintenanceWindowTaskExecuteHandler<MaintenanceTaskDispatchRequest> duplicate = mock(MaintenanceWindowTaskExecuteHandler.class);
        when(first.taskType()).thenReturn(MaintenanceWindowSecretRotationSupport.TASK_TYPE);
        when(duplicate.taskType()).thenReturn(MaintenanceWindowSecretRotationSupport.TASK_TYPE);

        MaintenanceWindowTaskExecuteService underTest = new MaintenanceWindowTaskExecuteService();
        FieldUtils.writeField(underTest, "handlers", List.of(first, duplicate), true);

        assertThatThrownBy(underTest::buildHandlerMap).isInstanceOf(IllegalStateException.class);
    }

    private static MaintenanceWindowTaskExecuteService serviceWith(
            List<MaintenanceWindowTaskExecuteHandler<MaintenanceTaskDispatchRequest>> handlers) throws IllegalAccessException {
        MaintenanceWindowTaskExecuteService service = new MaintenanceWindowTaskExecuteService();
        FieldUtils.writeField(service, "handlers", handlers, true);
        service.buildHandlerMap();
        return service;
    }
}
