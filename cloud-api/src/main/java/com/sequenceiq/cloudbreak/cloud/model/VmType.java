package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

public class VmType extends StringType {

    private VmTypeMeta metaData;

    private Boolean extended = true;

    private VmType(String vmType) {
        super(vmType);
    }

    private VmType(String vmType, VmTypeMeta meta, Boolean extended) {
        super(vmType);
        metaData = meta;
        this.extended = extended;
    }

    public static VmType vmType(String vmType) {
        return new VmType(vmType);
    }

    public static VmType vmTypeWithMeta(String vmType, VmTypeMeta meta, Boolean extended) {
        return new VmType(vmType, meta, extended);
    }

    public VolumeParameterConfig getVolumeParameterbyVolumeParameterType(VolumeParameterType volumeParameterType) {
        return volumeParameterType.getVolumeParameterByType(metaData);
    }

    public Boolean getExtended() {
        return extended;
    }

    public void setExtended(Boolean extended) {
        this.extended = extended;
    }

    public VmTypeMeta getMetaData() {
        return metaData;
    }

    public Object getMetaDataValue(String key) {
        return metaData.getProperties().get(key);
    }

    public boolean isMetaSet() {
        return metaData != null;
    }

    @Override
    public String toString() {
        return "VmType{"
                + "name=" + getValue()
                + ", metaData=" + metaData
                + ", extended=" + extended
                + '}';
    }
}
