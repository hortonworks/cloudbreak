package com.sequenceiq.cloudbreak.cloud.model;

public class VolumeParameterConfig {

    public static final VolumeParameterConfig EMPTY = new VolumeParameterConfig();

    private VolumeParameterType volumeParameterType;

    private Integer minimumSize;

    private Integer maximumSize;

    private Integer minimumNumber;

    private Integer maximumNumber;

    private VolumeParameterConfig() {

    }

    public VolumeParameterConfig(VolumeParameterType volumeParameterType,
            Integer minimumSize,
            Integer maximumSize,
            Integer minimumNumber,
            Integer maximumNumber) {
        this.volumeParameterType = volumeParameterType;
        this.minimumSize = minimumSize;
        this.maximumSize = maximumSize;
        this.minimumNumber = minimumNumber;
        this.maximumNumber = maximumNumber;
    }

    public VolumeParameterType volumeParameterType() {
        return volumeParameterType;
    }

    public Integer minimumSize() {
        return minimumSize;
    }

    public Integer minimumNumber() {
        return minimumNumber;
    }

    public Integer maximumSize() {
        return maximumSize;
    }

    public Integer maximumNumber() {
        return maximumNumber;
    }

    @Override
    public String toString() {
        return "VolumeParameterConfig{"
                + "volumeParameterType=" + volumeParameterType
                + ", minimumSize=" + minimumSize
                + ", maximumSize=" + maximumSize
                + ", minimumNumber=" + minimumNumber
                + ", maximumNumber=" + maximumNumber
                + '}';
    }
}
