package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

public final class OpenStackVolume {

    private String mount;
    private String device;
    private int size;

    public OpenStackVolume(String mount, String device, int size) {
        this.mount = mount;
        this.device = device;
        this.size = size;
    }

    public String getMount() {
        return mount;
    }

    public String getDevice() {
        return device;
    }

    public int getSize() {
        return size;
    }

}
