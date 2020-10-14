package com.sequenceiq.cloudbreak.cloud.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Volume {

    private final String mount;

    private final String type;

    private final int size;

    @JsonCreator
    public Volume(@JsonProperty("mount") String mount,
            @JsonProperty("type") String type,
            @JsonProperty("size") int size) {
        this.mount = mount;
        this.type = type;
        this.size = size;
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Volume{");
        sb.append("mount='").append(mount).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", size=").append(size);
        sb.append('}');
        return sb.toString();
    }
}
