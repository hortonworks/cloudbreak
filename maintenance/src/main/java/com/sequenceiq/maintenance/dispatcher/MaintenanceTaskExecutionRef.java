package com.sequenceiq.maintenance.dispatcher;

import static com.sequenceiq.maintenance.api.execution.MaintenanceTaskExecutionRefConstants.EXECUTE_PATH_KEY;
import static com.sequenceiq.maintenance.api.execution.MaintenanceTaskExecutionRefConstants.SUBMITTER_SERVICE_KEY;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.common.json.Json;

/**
 * Parsed {@link com.sequenceiq.maintenance.domain.MaintenanceWindowTask#getExecutionRef() execution_ref}
 * for outbound HTTP execute callbacks ({@code execute_path}).
 */
public record MaintenanceTaskExecutionRef(String submitterService, String executePath) {

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
