package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

public class VmType extends StringType {

    private VmType(String vmType) {
        super(vmType);
    }

    public static VmType vmType(String vmType) {
        return new VmType(vmType);
    }
}
