package com.sequenceiq.mock.service;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

public class CloudInstanceUtil {

    private CloudInstanceUtil() {
    }

    public static boolean isFailedVm(CloudVmMetaDataStatus vm) {
        return Set.of(InstanceStatus.FAILED, InstanceStatus.TERMINATED,
                InstanceStatus.TERMINATED_BY_PROVIDER).contains(vm.getCloudVmInstanceStatus().getStatus());
    }
}
