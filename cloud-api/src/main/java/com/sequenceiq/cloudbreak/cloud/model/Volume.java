package com.sequenceiq.cloudbreak.cloud.model;

public class Volume {

    private String mount;

    private String type;

    private int size;

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
}
