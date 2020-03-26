package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.common.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class VolumeParameterConfigV4Response implements JsonEntity {

    private String volumeParameterType;

    private Integer minimumSize;

    private Integer maximumSize;

    private Integer minimumNumber;

    private Integer maximumNumber;

    public String getVolumeParameterType() {
        return volumeParameterType;
    }

    public void setVolumeParameterType(String volumeParameterType) {
        this.volumeParameterType = volumeParameterType;
    }

    public Integer getMinimumSize() {
        return minimumSize;
    }

    public void setMinimumSize(Integer minimumSize) {
        this.minimumSize = minimumSize;
    }

    public Integer getMaximumSize() {
        return maximumSize;
    }

    public void setMaximumSize(Integer maximumSize) {
        this.maximumSize = maximumSize;
    }

    public Integer getMinimumNumber() {
        return minimumNumber;
    }

    public void setMinimumNumber(Integer minimumNumber) {
        this.minimumNumber = minimumNumber;
    }

    public Integer getMaximumNumber() {
        return maximumNumber;
    }

    public void setMaximumNumber(Integer maximumNumber) {
        this.maximumNumber = maximumNumber;
    }
}
