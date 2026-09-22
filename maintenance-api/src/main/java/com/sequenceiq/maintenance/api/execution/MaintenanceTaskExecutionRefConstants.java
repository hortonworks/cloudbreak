package com.sequenceiq.maintenance.api.execution;

import com.sequenceiq.maintenance.api.MaintenanceApi;

/**
 * Wire values for {@code execution_ref} on maintenance window tasks ({@code execute_path}, etc.).
 */
public final class MaintenanceTaskExecutionRefConstants {

    /**
     * Path joined onto submitter servlet context URLs (e.g. {@code /cb}, {@code /dl}).
     * Includes the JAX-RS application path ({@code /api}) and the internal execute endpoint.
     */
    public static final String STANDARD_EXECUTE_PATH =
            MaintenanceApi.API_ROOT_CONTEXT + "/v1/internal/maintenance-tasks/execute";

    private MaintenanceTaskExecutionRefConstants() {
    }
}
