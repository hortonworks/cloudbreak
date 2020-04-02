package com.sequenceiq.periscope.model.yarn;

import java.util.List;

public class YarnScalingServiceV1Request {

    private List<HostGroupInstanceType> instanceTypes;

    public List<HostGroupInstanceType> getInstanceTypes() {
        return instanceTypes;
    }

    public void setInstanceTypes(List<HostGroupInstanceType> instanceTypes) {
        this.instanceTypes = instanceTypes;
    }
}

