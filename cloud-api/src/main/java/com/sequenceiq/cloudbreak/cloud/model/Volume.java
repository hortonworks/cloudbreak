package com.sequenceiq.cloudbreak.cloud.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Volume {
    private final String mount;

    private final String type;

    private final int size;

    private final CloudVolumeUsageType volumeUsageType;

    @JsonCreator
    public Volume(@JsonProperty("mount") String mount,
            @JsonProperty("type") String type,
            @JsonProperty("size") int size,
            @JsonProperty("volumeUsageType") CloudVolumeUsageType volumeUsageType) {
        this.mount = mount;
        this.type = type;
        this.size = size;
        this.volumeUsageType = volumeUsageType;
    }

    public String getMount() {
        return mount;
    }

    public String getType() {
        return type;
    }

    public int getSize() {
        return size;
    }

    public CloudVolumeUsageType getVolumeUsageType() {
        return volumeUsageType == null ? CloudVolumeUsageType.GENERAL : volumeUsageType;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Volume{");
        sb.append("mount='").append(mount).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", size=").append(size);
        sb.append(", volumeUsageType=").append(volumeUsageType);
        sb.append('}');
        return sb.toString();
    }
}
