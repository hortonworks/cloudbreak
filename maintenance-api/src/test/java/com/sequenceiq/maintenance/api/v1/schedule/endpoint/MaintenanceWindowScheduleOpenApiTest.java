package com.sequenceiq.maintenance.api.v1.schedule.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.annotations.Operation;

class MaintenanceWindowScheduleOpenApiTest {

    private static final Set<String> EXPECTED_OPERATION_IDS = Set.of(
            "listMaintenanceWindowSchedulesV1",
            "getEffectiveMaintenanceWindowScheduleV1",
            "previewMaintenanceWindowScheduleV1",
            "getMaintenanceWindowScheduleV1",
            "createMaintenanceWindowScheduleV1",
            "updateMaintenanceWindowScheduleV1",
            "deleteMaintenanceWindowScheduleV1",
            "skipNextMaintenanceWindowV1",
            "cancelSkipNextMaintenanceWindowV1");

    @Test
    void publicScheduleOperationsExposeStableOperationIds() {
        Set<String> operationIds = Arrays.stream(MaintenanceWindowScheduleEndpoint.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Operation.class))
                .map(method -> method.getAnnotation(Operation.class).operationId())
                .collect(Collectors.toSet());

        assertThat(operationIds).containsExactlyInAnyOrderElementsOf(EXPECTED_OPERATION_IDS);
    }
}
