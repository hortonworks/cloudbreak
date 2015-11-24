package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

public class VmType extends StringType {

    private VmTypeMeta metaData;

    private VmType(String vmType) {
        super(vmType);
    }

    private VmType(String vmType, VmTypeMeta meta) {
        super(vmType);
        this.metaData = meta;
    }

    public static VmType vmType(String vmType) {
        return new VmType(vmType);
    }

    public static VmType vmTypeWithMeta(String vmType, VmTypeMeta meta) {
        return new VmType(vmType, meta);
    }

    public VmTypeMeta getMetaData() {
        return metaData;
    }

    public boolean isMetaSet() {
        return metaData != null;
    }
}
