package com.sequenceiq.mock.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;

public class SpiDto {

    private final CloudStack cloudStack;

    private final String mockuuid;

    private List<CloudVmMetaDataStatus> vmMetaDataStatuses;

    private AtomicBoolean addInstanceDisabled;

    private AtomicInteger failedScalingInstanceCount;

    public SpiDto(String mockuuid, CloudStack cloudStack) {
        this.cloudStack = cloudStack;
        this.mockuuid = mockuuid;
        this.vmMetaDataStatuses = new ArrayList<>();
        this.addInstanceDisabled = new AtomicBoolean(false);
        this.failedScalingInstanceCount = new AtomicInteger(0);
    }

    public List<CloudVmMetaDataStatus> getVmMetaDataStatuses() {
        return vmMetaDataStatuses;
    }

    public void setVmMetaDataStatuses(List<CloudVmMetaDataStatus> vmMetaDataStatuses) {
        this.vmMetaDataStatuses = vmMetaDataStatuses;
    }

    public CloudStack getCloudStack() {
        return cloudStack;
    }

    public boolean isAddInstanceDisabled() {
        return addInstanceDisabled.get();
    }

    public void setAddInstanceDisabled(boolean addInstanceDisabled) {
        this.addInstanceDisabled.set(addInstanceDisabled);
    }

    public void setFailedScalingInstanceCount(int failedScalingInstanceCount) {
        this.failedScalingInstanceCount.set(failedScalingInstanceCount);
    }

    public int getFailedScalingInstanceCount() {
        return failedScalingInstanceCount.get();
    }
}
