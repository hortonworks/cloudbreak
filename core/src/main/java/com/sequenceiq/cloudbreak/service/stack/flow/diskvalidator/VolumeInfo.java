package com.sequenceiq.cloudbreak.service.stack.flow.diskvalidator;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class VolumeInfo {

    private final String id;

    private final String device;

    private final String size;

    private final boolean databaseType;

    public VolumeInfo(String id, String device, String size, boolean databaseType) {
        this.id = id;
        this.device = device;
        this.size = size;
        this.databaseType = databaseType;
    }

    public String getId() {
        return id;
    }

    public String getDevice() {
        return device;
    }

    public String getSize() {
        return size;
    }

    public boolean isDatabaseType() {
        return databaseType;
    }

    @Override
    public String toString() {
        return "VolumeInfo{" +
                "id='" + id + '\'' +
                ", device='" + device + '\'' +
                ", size='" + size + '\'' +
                ", databaseType=" + databaseType +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof VolumeInfo volumeInfo)) {
            return false;
        }

        return new EqualsBuilder().append(databaseType, volumeInfo.databaseType).append(id, volumeInfo.id).append(device, volumeInfo.device)
                .append(size, volumeInfo.size).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(id).append(device).append(size).append(databaseType).toHashCode();
    }
}