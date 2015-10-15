package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

public class DiskType extends StringType {

    private DiskType(String diskType) {
        super(diskType);
    }

    public static DiskType diskType(String diskType) {
        return new DiskType(diskType);
    }
}
