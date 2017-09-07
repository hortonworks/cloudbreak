package com.sequenceiq.cloudbreak.cloud.azure.view;

import com.sequenceiq.cloudbreak.cloud.model.Volume;

public class AzureVolumeView {

    private static final String KVM_DEVICE_PREFIX = "/dev/vd";

    private static final char[] DEVICE_CHAR = {'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
            's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

    private final Volume volume;

    private final String device;

    public AzureVolumeView(Volume volume, int index) {
        this.volume = volume;
        device = KVM_DEVICE_PREFIX + DEVICE_CHAR[index];
    }

    public int getIndex() {
        return 0;
    }

    public String getMount() {
        return volume.getMount();
    }

    public String getType() {
        return volume.getType();
    }

    public int getSize() {
        return volume.getSize();
    }

    public String getDevice() {
        return device;
    }
}