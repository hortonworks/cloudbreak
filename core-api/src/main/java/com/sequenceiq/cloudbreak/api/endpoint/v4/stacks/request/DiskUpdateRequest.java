package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.StringJoiner;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiskUpdateRequest implements JsonEntity {

    @Schema(description = "Volume Type of disks")
    @OneOfEnum(enumClass = SupportedVolumeType.class)
    private String volumeType;

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

    public String toString() {
        return new StringJoiner(", ", DiskUpdateRequest.class.getSimpleName() + "[", "]")
                .add("volumeType=" + volumeType)
                .add("size=" + size)
                .add("group=" + group)
                .toString();
    }
}
