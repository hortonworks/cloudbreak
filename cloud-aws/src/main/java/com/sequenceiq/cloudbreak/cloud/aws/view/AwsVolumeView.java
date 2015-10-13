package com.sequenceiq.cloudbreak.cloud.aws.view;

import com.sequenceiq.cloudbreak.cloud.model.Volume;

public class AwsVolumeView {

    private static final char[] DEVICE_CHAR = {'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k'};

    private Volume volume;

    private String device;

    private int index;

    public AwsVolumeView(Volume volume, int index) {
        this.volume = volume;
        this.device = "" + DEVICE_CHAR[index];
    }

    public int getIndex() {
        return index;
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