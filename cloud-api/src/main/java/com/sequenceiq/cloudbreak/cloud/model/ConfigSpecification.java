package com.sequenceiq.cloudbreak.cloud.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConfigSpecification {

    private static final Integer LIMIT = 24;

    @JsonProperty("volumeParameterType")
    private String volumeParameterType;
    @JsonProperty("minimumSize")
    private String minimumSize;
    @JsonProperty("maximumSize")
    private String maximumSize;
    @JsonProperty("minimumNumber")
    private String minimumNumber;
    @JsonProperty("maximumNumber")
    private String maximumNumber;

    public ConfigSpecification() {
    }

    public String getVolumeParameterType() {
        return volumeParameterType;
    }

    public void setVolumeParameterType(String volumeParameterType) {
        this.volumeParameterType = volumeParameterType;
    }

    public String getMinimumSize() {
        return minimumSize;
    }

    public void setMinimumSize(String minimumSize) {
        this.minimumSize = minimumSize;
    }

    public String getMaximumSize() {
        return maximumSize;
    }

    public void setMaximumSize(String maximumSize) {
        this.maximumSize = maximumSize;
    }

    public String getMinimumNumber() {
        return minimumNumber;
    }

    public void setMinimumNumber(String minimumNumber) {
        this.minimumNumber = minimumNumber;
    }

    public String getMaximumNumber() {
        return maximumNumber;
    }

    public Integer getMaximumNumberWithLimit() {
        int maxNumber = Integer.valueOf(maximumNumber);
        return maxNumber > LIMIT ? LIMIT : maxNumber;
    }

    public void setMaximumNumber(String maximumNumber) {
        this.maximumNumber = maximumNumber;
    }
}
