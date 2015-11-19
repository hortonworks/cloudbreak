package com.sequenceiq.cloudbreak.cloud.model;

public class VmTypeMeta {

    private Integer maxEphemeralVolumeCount;
    private String ephemeralVolumeSize;

    private VmTypeMeta(Integer maxEphemeralVolumeCount) {
        this.maxEphemeralVolumeCount = maxEphemeralVolumeCount;
    }

    private VmTypeMeta(Integer maxEphemeralVolumeCount, String ephemeralVolumeSize) {
        this.maxEphemeralVolumeCount = maxEphemeralVolumeCount;
        this.ephemeralVolumeSize = ephemeralVolumeSize;
    }

    public static VmTypeMeta meta(Integer maxEphemeralVolumeCount) {
        return new VmTypeMeta(maxEphemeralVolumeCount);
    }

    public static VmTypeMeta meta(Integer maxEphemeralVolumeCount, String ephemeralVolumeSize) {
        return new VmTypeMeta(maxEphemeralVolumeCount, ephemeralVolumeSize);
    }

    public String ephemeralVolumeSize() {
        return ephemeralVolumeSize;
    }

    public Integer maxEphemeralVolumeCount() {
        return maxEphemeralVolumeCount;
    }
}
