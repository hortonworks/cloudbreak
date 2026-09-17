package com.sequenceiq.maintenance.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sequenceiq.maintenance.api.v1.task.model.response.MaintenanceWindowRunResponse;
import com.sequenceiq.maintenance.domain.MaintenanceRunStatus;
import com.sequenceiq.maintenance.domain.MaintenanceWindowRun;

class MaintenanceWindowRunConverterTest {

    private final MaintenanceWindowRunConverter underTest = new MaintenanceWindowRunConverter();

    @Test
    void toResponseUsesCallerTaskIdWithoutInitializingTaskAssociation() {
        MaintenanceWindowRun run = new MaintenanceWindowRun();
        run.setId(10L);
        run.setStatus(MaintenanceRunStatus.COMPLETED);
        run.setWindowStart(100L);
        run.setWindowEnd(200L);
        run.setWindowExecutionEnd(150L);
        run.setAttemptCount(2);
        run.setVersion(3);

        MaintenanceWindowRunResponse response = underTest.toResponse(run, 5L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTaskId()).isEqualTo(5L);
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getWindowStart()).isEqualTo(100L);
        assertThat(response.getAttemptCount()).isEqualTo(2);
        assertThat(response.getVersion()).isEqualTo(3);
    }
}
