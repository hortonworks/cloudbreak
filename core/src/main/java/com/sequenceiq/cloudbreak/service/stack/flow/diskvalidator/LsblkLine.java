package com.sequenceiq.cloudbreak.service.stack.flow.diskvalidator;

public class LsblkLine {

    private final String device;

    private final String size;

    private final String mountPoint;

    public LsblkLine(String device, String size, String mountPoint) {
        this.device = device;
        this.size = size;
        this.mountPoint = mountPoint;
    }

    public String getDevice() {
        return device;
    }

    public String getSize() {
        return size;
    }

    public String getMountPoint() {
        return mountPoint;
    }
}
