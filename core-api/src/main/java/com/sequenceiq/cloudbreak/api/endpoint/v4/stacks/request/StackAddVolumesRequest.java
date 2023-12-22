package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.StringJoiner;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackAddVolumesRequest implements JsonEntity {

    @NotNull
    @Schema(description = "Instance Group being modified", required = true)
    private String instanceGroup;

    @NotNull
    @Schema(description = "Number of block storages to be added to each instance", required = true)
    private Long numberOfDisks;

    @NotNull
    @Schema(description = "Type of block storage", required = true)
    private String type;

    @NotNull
    @Schema(description = "Size of each black storage being added", required = true)
    private Long size;

    @NotNull
    @Schema(description = "Usage type of the clock storage", required = true)
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
