package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Range;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiskUpdateRequest implements JsonEntity {

    @NotNull
    @ApiModelProperty(value = "Volume Type of disks", required = true)
    @OneOfEnum(enumClass = SupportedVolumeTypes.class)
    private String volumeType;

    @Range(min = 1, message = "Value must be greater than 0")
    @ApiModelProperty(value = "Size of disks")
    private int size;

    @NotNull
    @ApiModelProperty(value = "Group being updated", required = true)
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
