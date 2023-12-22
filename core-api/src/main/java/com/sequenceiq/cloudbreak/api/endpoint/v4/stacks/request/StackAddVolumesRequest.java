package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.StringJoiner;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackAddVolumesRequest implements JsonEntity {

    @NotNull
    @ApiModelProperty("Instance Group being modified")
    private String instanceGroup;

    @NotNull
    @ApiModelProperty("Number of block storages to be added to each instance")
    private Long numberOfDisks;

    @NotNull
    @ApiModelProperty("Type of block storage")
    private String type;

    @NotNull
    @ApiModelProperty("Size of each black storage being added")
    private Long size;

    @NotNull
    @ApiModelProperty("Usage type of the clock storage")
    private String cloudVolumeUsageType;

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public void setInstanceGroup(String instanceGroup) {
        this.instanceGroup = instanceGroup;
    }

    public Long getNumberOfDisks() {
        return numberOfDisks;
    }

    public void setNumberOfDisks(Long numberOfDisks) {
        this.numberOfDisks = numberOfDisks;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getCloudVolumeUsageType() {
        return cloudVolumeUsageType;
    }

    public void setCloudVolumeUsageType(String cloudVolumeUsageType) {
        this.cloudVolumeUsageType = cloudVolumeUsageType;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", StackAddVolumesRequest.class.getSimpleName() + "[", "]")
                .add("Type=" + type)
                .add("Size=" + size)
                .add("NumberOfDisks=" + numberOfDisks)
                .add("CloudVolumeUsageType=" + cloudVolumeUsageType)
                .add("instanceGroup=" + instanceGroup)
                .toString();
    }
}
