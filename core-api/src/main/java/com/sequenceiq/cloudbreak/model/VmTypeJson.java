package com.sequenceiq.cloudbreak.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VmTypeJson {
    private String value;
    private Integer maxEphemeralVolumeCount;
    private String ephemeralVolumeSize;

    public VmTypeJson() {

    }

    public VmTypeJson(String value) {
        this.value = value;
    }

    public VmTypeJson(String value, Integer maxEphemeralVolumeCount, String ephemeralVolumeSize) {
        this.value = value;
        this.maxEphemeralVolumeCount = maxEphemeralVolumeCount;
        this.ephemeralVolumeSize = ephemeralVolumeSize;
    }

    public String getValue() {
        return value;
    }

    public Integer getMaxEphemeralVolumeCount() {
        return maxEphemeralVolumeCount;
    }

    public String getEphemeralVolumeSize() {
        return ephemeralVolumeSize;
    }
}
