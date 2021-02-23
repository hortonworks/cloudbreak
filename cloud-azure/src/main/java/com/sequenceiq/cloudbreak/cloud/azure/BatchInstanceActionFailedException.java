package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

public class BatchInstanceActionFailedException extends Exception {

    private final List<CloudVmInstanceStatus> instanceStatuses;

    public BatchInstanceActionFailedException(List<CloudVmInstanceStatus> instanceStatuses) {
        this.instanceStatuses = instanceStatuses;
    }

    public BatchInstanceActionFailedException(List<CloudVmInstanceStatus> instanceStatuses, Throwable cause) {
        super(cause);
        this.instanceStatuses = instanceStatuses;
    }

    public List<CloudVmInstanceStatus> getInstanceStatuses() {
        return instanceStatuses;
    }

    public String getFailedInstanceStatusReasons() {
        return instanceStatuses.stream()
                .filter(instance -> InstanceStatus.FAILED.equals(instance.getStatus()))
                .map(instance -> StringUtils.join(instance.getCloudInstance().getInstanceId(), ":", instance.getStatusReason()))
                .collect(Collectors.joining(" - "));
    }
}