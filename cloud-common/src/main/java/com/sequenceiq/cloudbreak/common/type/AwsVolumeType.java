package com.sequenceiq.cloudbreak.common.type;

public enum AwsVolumeType {

    Standard("standard"),
    Ephemeral("ephemeral"),
    Gp2("gp2");

    private String value;

    private AwsVolumeType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
