package com.sequenceiq.cloudbreak.cloud.azure.util;

import java.util.List;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

public class VmStatusErrorMessageUtils {

    private VmStatusErrorMessageUtils() {
    }

    /**
     * Error messages that are already present in the per-VM status reasons are not repeated.
     * The rest (e.g. an error thrown before any VM operation was subscribed) are included.
     */
    public static String createErrorMessage(String messagePrefix, Exception e, List<CloudVmInstanceStatus> statuses) {
        String statusText = "statuses: " + convertToStatusText(statuses);
        String missingErrorMessages = ReactiveUtils.getErrorMessages(e).stream()
                .filter(message -> !statusText.contains(message))
                .collect(Collectors.joining(" "));
        if (missingErrorMessages.isEmpty()) {
            return messagePrefix + statusText;
        } else {
            return messagePrefix + missingErrorMessages + ", " + statusText;
        }
    }

    public static String convertToStatusText(List<CloudVmInstanceStatus> statuses) {
        return statuses.stream()
                .map(status -> status.getCloudInstance().getInstanceId() + " - " + status.getStatus()
                        + (status.getStatusReason() == null ? "" : " - " + status.getStatusReason()))
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
