package com.sequenceiq.maintenance.dispatcher;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.common.json.Json;

/**
 * Parsed {@link com.sequenceiq.maintenance.domain.MaintenanceWindowTask#getExecutionRef() execution_ref}
 * for outbound HTTP execute callbacks ({@code execute_path}).
 */
public record MaintenanceTaskExecutionRef(String submitterService, String executePath) {

    /**
     * Path joined onto submitter servlet context URLs (e.g. {@code /cb}, {@code /dl}).
     * Includes the JAX-RS {@code @ApplicationPath} segment ({@code /api}) plus the execute endpoint path.
     */
    public static final String STANDARD_EXECUTE_PATH = "/api/v1/internal/maintenance-tasks/execute";

    private static final String SUBMITTER_SERVICE_KEY = "submitter_service";

    private static final String EXECUTE_PATH_KEY = "execute_path";

    public static MaintenanceTaskExecutionRef parse(Json executionRef, String taskSubmitterService) {
        Map<String, Object> values = executionRefValues(executionRef);
        String executePath = stringValue(values, EXECUTE_PATH_KEY);
        if (StringUtils.isBlank(executePath)) {
            throw new IllegalArgumentException("Missing execute_path in execution_ref");
        }
        String submitterService = resolveSubmitterService(values, taskSubmitterService);
        return new MaintenanceTaskExecutionRef(submitterService, executePath);
    }

    private static String resolveSubmitterService(Map<String, Object> values, String taskSubmitterService) {
        String fromExecutionRef = stringValue(values, SUBMITTER_SERVICE_KEY);
        return StringUtils.isBlank(fromExecutionRef) ? taskSubmitterService : fromExecutionRef;
    }

    private static String stringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private static Map<String, Object> executionRefValues(Json executionRef) {
        if (executionRef == null) {
            return Map.of();
        }
        return executionRef.getMap();
    }
}
