package com.sequenceiq.mock.spi;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;

public class SpiDto {

    private final CloudStack cloudStack;

    private final String mockuuid;

    private List<CloudVmMetaDataStatus> vmMetaDataStatuses;

    public SpiDto(String mockuuid, CloudStack cloudStack) {
        this.cloudStack = cloudStack;
        this.mockuuid = mockuuid;
        this.vmMetaDataStatuses = new ArrayList<>();
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
}
