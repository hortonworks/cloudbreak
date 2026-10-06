package com.sequenceiq.maintenance.api.execution;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sequenceiq.maintenance.api.model.MaintenanceSubmitterService;

class MaintenanceTaskExecutionRefConstantsTest {

    @Test
    void standardExecutionRefUsesSubmitterNameAndStandardPath() {
        assertThat(MaintenanceTaskExecutionRefConstants.standardExecutionRef(MaintenanceSubmitterService.DATALAKE))
                .containsEntry(MaintenanceTaskExecutionRefConstants.SUBMITTER_SERVICE_KEY, "datalake")
                .containsEntry(
                        MaintenanceTaskExecutionRefConstants.EXECUTE_PATH_KEY,
                        MaintenanceTaskExecutionRefConstants.STANDARD_EXECUTE_PATH);
    }
}
