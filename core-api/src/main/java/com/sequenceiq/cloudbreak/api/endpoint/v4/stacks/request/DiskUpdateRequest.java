package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Range;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiskUpdateRequest implements JsonEntity {

    @NotNull
    @Schema(description = "Volume Type of disks", required = true)
    @OneOfEnum(enumClass = SupportedVolumeType.class)
    private String volumeType;

    @Range(min = 1, message = "Value must be greater than 0")
    @Schema(description = "Size of disks in GB")
    private int size;

    @NotNull
    @Schema(description = "Group being updated", required = true)
    private String group;

    public String getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}
