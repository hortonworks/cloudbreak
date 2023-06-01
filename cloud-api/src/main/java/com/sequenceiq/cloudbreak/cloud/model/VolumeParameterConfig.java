package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Set;

import com.sequenceiq.cloudbreak.validation.VolumeParameterConstants;

public class VolumeParameterConfig {

    public static final VolumeParameterConfig EMPTY = new VolumeParameterConfig();

    private VolumeParameterType volumeParameterType;

    private Integer minimumSize;

    private Integer maximumSize;

    private Set<Integer> possibleSizeValues;

    private Integer minimumNumber;

    private Integer maximumNumber;

    private Set<Integer> possibleNumberValues;

    private VolumeParameterConfig() {

    }

    public VolumeParameterConfig(VolumeParameterType volumeParameterType,
            Integer minimumSize,
            Integer maximumSize,
            Set<Integer> possibleSizeValues,
            Integer minimumNumber,
            Integer maximumNumber,
            Set<Integer> possibleNumberValues) {
        this.volumeParameterType = volumeParameterType;
        this.minimumSize = minimumSize;
        this.maximumSize = maximumSize;
        this.minimumNumber = minimumNumber;
        this.maximumNumber = calculateSupportedMaximumNumber(maximumNumber);
        this.possibleSizeValues = possibleSizeValues;
        this.possibleNumberValues = possibleNumberValues;
    }

    public VolumeParameterConfig(VolumeParameterType volumeParameterType,
        Integer minimumSize,
        Integer maximumSize,
        Integer minimumNumber,
        Integer maximumNumber) {
        this(volumeParameterType, minimumSize, maximumSize, Set.of(), minimumNumber, maximumNumber, Set.of());
    }

    public VolumeParameterConfig(VolumeParameterType volumeParameterType,
        Set<Integer> possibleSizeValues,
        Set<Integer> possibleNumberValues) {
        this(volumeParameterType, 0, 0, possibleSizeValues, 0, 0, possibleNumberValues);
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

    public Set<Integer> possibleSizeValues() {
        return possibleSizeValues;
    }

    public Set<Integer> possibleNumberValues() {
        return possibleNumberValues;
    }

    @Override
    public String toString() {
        return "VolumeParameterConfig{"
                + "volumeParameterType=" + volumeParameterType
                + ", minimumSize=" + minimumSize
                + ", maximumSize=" + maximumSize
                + ", minimumNumber=" + minimumNumber
                + ", maximumNumber=" + maximumNumber
                + ", possibleNumberValues=" + possibleNumberValues
                + ", possibleSizeValues=" + possibleSizeValues
                + '}';
    }

    private Integer calculateSupportedMaximumNumber(Integer maximumNumber) {
        Integer result = maximumNumber;
        if (maximumNumber != null && maximumNumber > VolumeParameterConstants.MAXIMUM_NUMBER_OF_VOLUMES) {
            result = VolumeParameterConstants.MAXIMUM_NUMBER_OF_VOLUMES;
        }
        return result;
    }
}
