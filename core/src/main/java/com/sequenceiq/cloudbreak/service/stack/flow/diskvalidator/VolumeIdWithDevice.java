package com.sequenceiq.cloudbreak.service.stack.flow.diskvalidator;

public class VolumeIdWithDevice {

    private final String volumeId;

    private final String device;

    public VolumeIdWithDevice(String volumeId, String device) {
        this.volumeId = volumeId;
        this.device = device;
    }

    public String getVolumeId() {
        return volumeId;
    }

    public String getDevice() {
        return device;
    }
}
