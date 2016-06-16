package com.sequenceiq.cloudbreak.cloud.model;

public enum VolumeParameterType {
    MAGNETIC() {
        @Override public VolumeParameterConfig getVolumeParameterbyType(VmTypeMeta metaData) {
            return metaData.getMagneticConfig();
        }
    },
    SSD() {
        @Override public VolumeParameterConfig getVolumeParameterbyType(VmTypeMeta metaData) {
            return metaData.getSsdConfig();
        }
    },
    EPHEMERAL() {
        @Override public VolumeParameterConfig getVolumeParameterbyType(VmTypeMeta metaData) {
            return metaData.getEphemeralConfig();
        }
    },
    ST1() {
        @Override public VolumeParameterConfig getVolumeParameterbyType(VmTypeMeta metaData) {
            return metaData.getSt1Config();
        }
    },
    AUTO_ATTACHED() {
        @Override public VolumeParameterConfig getVolumeParameterbyType(VmTypeMeta metaData) {
            return metaData.getAutoAttachedConfig();
        }
    };

    public abstract VolumeParameterConfig getVolumeParameterbyType(VmTypeMeta metaData);
}
