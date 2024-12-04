package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class VolumeParameterConfigV4Response implements JsonEntity {

    private String volumeParameterType;

    private Integer minimumSize;

    private Integer maximumSize;

    private Integer minimumNumber;

    private Integer maximumNumber;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<Integer> possibleSizeValues = new HashSet<>();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<Integer> possibleNumberValues = new HashSet<>();

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

    public Set<Integer> getPossibleSizeValues() {
        return possibleSizeValues;
    }

    public void setPossibleSizeValues(Set<Integer> possibleSizeValues) {
        this.possibleSizeValues = possibleSizeValues;
    }

    public Set<Integer> getPossibleNumberValues() {
        return possibleNumberValues;
    }

    public void setPossibleNumberValues(Set<Integer> possibleNumberValues) {
        this.possibleNumberValues = possibleNumberValues;
    }
}
