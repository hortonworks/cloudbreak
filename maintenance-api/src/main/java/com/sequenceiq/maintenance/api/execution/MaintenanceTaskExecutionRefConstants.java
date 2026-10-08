package com.sequenceiq.maintenance.api.execution;

import java.util.Map;

import com.sequenceiq.maintenance.api.MaintenanceApi;
import com.sequenceiq.maintenance.api.model.MaintenanceSubmitterService;

/**
 * Wire values for {@code execution_ref} on maintenance window tasks ({@code execute_path}, etc.).
 */
public final class MaintenanceTaskExecutionRefConstants {

    /** Map key naming the submitter the dispatcher calls back ({@code cloudbreak}, {@code datalake}, {@code freeipa}). */
    public static final String SUBMITTER_SERVICE_KEY = "submitter_service";

    /** Map key holding the submitter-relative execute path. */
    public static final String EXECUTE_PATH_KEY = "execute_path";

    /**
     * Path joined onto submitter servlet context URLs (e.g. {@code /cb}, {@code /dl}).
     * Includes the JAX-RS application path ({@code /api}) and the internal execute endpoint.
     */
    public static final String STANDARD_EXECUTE_PATH =
            MaintenanceApi.API_ROOT_CONTEXT + "/v1/internal/maintenance-tasks/execute";

    private MaintenanceTaskExecutionRefConstants() {
    }

    /**
     * {@code execution_ref} for submitters that expose {@link #STANDARD_EXECUTE_PATH} on their servlet context.
     */
    public static Map<String, Object> standardExecutionRef(MaintenanceSubmitterService submitterService) {
        return Map.of(
                SUBMITTER_SERVICE_KEY, submitterService.serviceName(),
                EXECUTE_PATH_KEY, STANDARD_EXECUTE_PATH);
    }
}
