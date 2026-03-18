package com.sequenceiq.cloudbreak.service.stack.flow.diskvalidator;

public class LsblkLine {

    private final String device;

    private final String size;

    private final String mountPoint;

    private final String volumeType;

    private final String uuid;

    private final String serial;

    private final String hctl;

    public LsblkLine(String device, String size, String mountPoint) {
        this.device = device;
        this.size = size;
        this.mountPoint = mountPoint;
        this.volumeType = null;
        this.uuid = null;
        this.serial = null;
        this.hctl = null;
    }

    public LsblkLine(String device, String size, String mountPoint, String volumeType, String uuid, String serial, String hctl) {
        this.device = device;
        this.size = size;
        this.mountPoint = mountPoint;
        this.volumeType = volumeType;
        this.uuid = uuid;
        this.serial = serial;
        this.hctl = hctl;
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

    public String getVolumeType() {
        return volumeType;
    }

    public String getUuid() {
        return uuid;
    }

    public String getSerial() {
        return serial;
    }

    public String getHctl() {
        return hctl;
    }
}
