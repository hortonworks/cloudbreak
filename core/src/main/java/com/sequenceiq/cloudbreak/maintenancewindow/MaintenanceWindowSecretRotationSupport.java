package com.sequenceiq.cloudbreak.maintenancewindow;

import static com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties.IGNORE_PREVALIDATE_ERRORS;
import static com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties.MAINTENANCE_WINDOW_ACCOUNT_ID;
import static com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties.MAINTENANCE_WINDOW_RUN_ID;
import static com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties.MAINTENANCE_WINDOW_TASK_ID;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.maintenance.api.execution.MaintenanceTaskExecutionRefConstants;

/**
 * {@code SECRET_ROTATION} maintenance tasks: one secret type per task; {@code work_item_id} is the secret name.
 */
public final class MaintenanceWindowSecretRotationSupport {

    public static final String TASK_TYPE = "SECRET_ROTATION";

    public static final String TASK_KIND_ONE_SHOT = "ONE_SHOT";

    public static final String SUBMITTER_SERVICE = "cloudbreak";

    public static final String PAYLOAD_SECRET_NAMES = "secretNames";

    private MaintenanceWindowSecretRotationSupport() {
    }

    public static String workItemIdForSecretName(String secretName) {
        return requireSecretName(secretName, "secret name");
    }

    public static Map<String, Object> executionRef() {
        return Map.of(
                "submitter_service",
                SUBMITTER_SERVICE,
                "execute_path",
                MaintenanceTaskExecutionRefConstants.STANDARD_EXECUTE_PATH);
    }

    public static Map<String, Object> taskPayload(String secretName) {
        return Map.of(PAYLOAD_SECRET_NAMES, List.of(requireSecretName(secretName, "secret name")));
    }

    /** {@code work_item_id} is authoritative; payload {@code secretNames}, if present, must list that name only. */
    public static String resolveSecretName(String workItemId, Map<String, Object> taskPayload) {
        String secretName = requireSecretName(workItemId, "work_item_id");
        if (taskPayload == null || !taskPayload.containsKey(PAYLOAD_SECRET_NAMES)) {
            return secretName;
        }
        if (!singleSecretFromPayload(taskPayload).equals(secretName)) {
            throw new BadRequestException(
                    PAYLOAD_SECRET_NAMES + " in task_payload must contain only '" + secretName + "'");
        }
        return secretName;
    }

    public static Map<String, String> maintenanceWindowAdditionalProperties(String accountId, Long taskId, Long runId) {
        return Map.of(
                IGNORE_PREVALIDATE_ERRORS, "true",
                MAINTENANCE_WINDOW_ACCOUNT_ID, accountId,
                MAINTENANCE_WINDOW_TASK_ID, String.valueOf(taskId),
                MAINTENANCE_WINDOW_RUN_ID, String.valueOf(runId));
    }

    private static String singleSecretFromPayload(Map<String, Object> taskPayload) {
        Object raw = taskPayload.get(PAYLOAD_SECRET_NAMES);
        if (!(raw instanceof List<?> list) || list.size() != 1 || !(list.get(0) instanceof String secretName)) {
            throw new BadRequestException(PAYLOAD_SECRET_NAMES + " must be a list of one secret type name");
        }
        return requireSecretName(secretName, PAYLOAD_SECRET_NAMES);
    }

    private static String requireSecretName(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldLabel + " must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.contains(",")) {
            throw new BadRequestException(fieldLabel + " must be a single secret type name");
        }
        return trimmed;
    }
}
