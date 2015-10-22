package com.sequenceiq.cloudbreak.cloud.model;

public class Volume {

    private final String mount;

    private final String type;

    private final int size;

    public Volume(String mount, String type, int size) {
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
        final StringBuilder sb = new StringBuilder("Volume{");
        sb.append("mount='").append(mount).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", size=").append(size);
        sb.append('}');
        return sb.toString();
    }
}
