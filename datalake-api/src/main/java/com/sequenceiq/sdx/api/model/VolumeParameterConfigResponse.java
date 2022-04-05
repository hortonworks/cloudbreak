package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VolumeParameterConfigResponse {

    @ApiModelProperty(ModelDescriptions.VOLUME_PARAMETER_TYPE)
    private String volumeParameterType;

    @ApiModelProperty(ModelDescriptions.VOLUME_MINIMUM_SIZE)
    private Integer minimumSize;

    @ApiModelProperty(ModelDescriptions.VOLUME_MAXIMUM_SIZE)
    private Integer maximumSize;

    @ApiModelProperty(ModelDescriptions.VOLUME_MINIMUM_NUMBER)
    private Integer minimumNumber;

    @ApiModelProperty(ModelDescriptions.VOLUME_MAXIMUM_NUMBER)
    private Integer maximumNumber;

    public VolumeParameterConfigResponse() {
    }

    public VolumeParameterConfigResponse(String volumeParameterType, Integer minimumSize, Integer maximumSize, Integer minimumNumber, Integer maximumNumber) {
        this.volumeParameterType = volumeParameterType;
        this.minimumSize = minimumSize;
        this.maximumSize = maximumSize;
        this.minimumNumber = minimumNumber;
        this.maximumNumber = maximumNumber;
    }

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

    @Override
    public String toString() {
        return "VolumeParameterConfigResponse{" +
                "volumeParameterType='" + volumeParameterType + '\'' +
                ", minimumSize=" + minimumSize +
                ", maximumSize=" + maximumSize +
                ", minimumNumber=" + minimumNumber +
                ", maximumNumber=" + maximumNumber +
                '}';
    }
}
