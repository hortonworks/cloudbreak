package com.sequenceiq.cloudbreak.maintenancewindow;

import static com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties.IGNORE_PREVALIDATE_ERRORS;
import static com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties.MAINTENANCE_WINDOW_ACCOUNT_ID;
import static com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties.MAINTENANCE_WINDOW_RUN_ID;
import static com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties.MAINTENANCE_WINDOW_TASK_ID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

/**
 * Constants and helpers for {@code SECRET_ROTATION} maintenance task execution on Cloudbreak.
 */
public final class MaintenanceWindowSecretRotationSupport {

    public static final String TASK_TYPE = "SECRET_ROTATION";

    public static final String PAYLOAD_SECRET_NAMES = "secretNames";

    private MaintenanceWindowSecretRotationSupport() {
    }

    /**
     * Encodes secret type names for {@code work_item_id}, matching maintenance task registration.
     */
    public static String workItemIdForSecretNames(List<String> secretNames) {
        return secretNames.stream().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.joining(","));
    }

    /**
     * Decodes {@code work_item_id} registered for {@link #TASK_TYPE} tasks.
     */
    public static List<String> secretNamesFromWorkItemId(String workItemId) {
        if (workItemId == null || workItemId.isBlank()) {
            throw new BadRequestException("work_item_id must not be blank");
        }
        String[] segments = workItemId.split(",");
        List<String> names = new ArrayList<>();
        for (String segment : segments) {
            String name = segment.trim();
            if (name.isEmpty()) {
                throw new BadRequestException("work_item_id must not contain empty segments");
            }
            names.add(name);
        }
        if (names.isEmpty()) {
            throw new BadRequestException("work_item_id must not be empty");
        }
        return names;
    }

    /**
     * {@code work_item_id} is the source of truth (required at registration); optional {@code task_payload.secretNames}
     * must agree when present.
     */
    public static List<String> resolveSecretNames(String workItemId, Map<String, Object> taskPayload) {
        List<String> fromWorkItem = secretNamesFromWorkItemId(workItemId);
        if (taskPayload == null || !taskPayload.containsKey(PAYLOAD_SECRET_NAMES)) {
            return fromWorkItem;
        }
        List<String> fromPayload = parseSecretNamesFromPayload(taskPayload);
        if (!workItemIdForSecretNames(fromPayload).equalsIgnoreCase(workItemIdForSecretNames(fromWorkItem))) {
            throw new BadRequestException(String.format(
                    "%s in task_payload must match work_item_id '%s'", PAYLOAD_SECRET_NAMES, workItemId));
        }
        return fromWorkItem;
    }

    /**
     * Rotation {@code additionalProperties} for maintenance-window dispatch (same prevalidate behavior as periodic rotation).
     */
    public static Map<String, String> maintenanceWindowAdditionalProperties(
            String accountId,
            Long taskId,
            Long runId) {
        Map<String, String> properties = new HashMap<>();
        properties.put(IGNORE_PREVALIDATE_ERRORS, "true");
        properties.put(MAINTENANCE_WINDOW_ACCOUNT_ID, accountId);
        properties.put(MAINTENANCE_WINDOW_TASK_ID, String.valueOf(taskId));
        properties.put(MAINTENANCE_WINDOW_RUN_ID, String.valueOf(runId));
        return properties;
    }

    private static List<String> parseSecretNamesFromPayload(Map<String, Object> taskPayload) {
        Object raw = taskPayload.get(PAYLOAD_SECRET_NAMES);
        if (!(raw instanceof List<?> list)) {
            throw new BadRequestException(PAYLOAD_SECRET_NAMES + " must be a list of secret type names");
        }
        List<String> names = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof String secretName)) {
                throw new BadRequestException(PAYLOAD_SECRET_NAMES + " must contain only string secret type names");
            }
            if (secretName.isBlank()) {
                throw new BadRequestException(PAYLOAD_SECRET_NAMES + " must not contain blank entries");
            }
            names.add(secretName);
        }
        if (names.isEmpty()) {
            throw new BadRequestException(PAYLOAD_SECRET_NAMES + " must not be empty");
        }
        return names;
    }
}
