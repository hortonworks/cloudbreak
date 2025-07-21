package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.StringJoiner;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.util.OneOfEnum;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiskUpdateRequest implements JsonEntity {

    @Schema(description = "Volume Type of disks")
    @OneOfEnum(enumClass = SupportedVolumeType.class, message = "Value must be one of the followings %s", fieldName = "volumeType")
    private String volumeType;

    @Schema(description = "Size of disks in GB")
    private int size;

    @NotNull
    @Schema(description = "Group being updated", required = true)
    private String group;

    @Schema(description = "Type of disk being updated")
    private DiskType diskType;

    public DiskUpdateRequest() { }

    public DiskUpdateRequest(String volumeType, int size, String group, DiskType diskType) {
        this.volumeType = volumeType;
        this.size = size;
        this.group = group;
        this.diskType = diskType;
    }

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

    public DiskType getDiskType() {
        return diskType;
    }

    public void setDiskType(DiskType diskType) {
        this.diskType = diskType;
    }

    public String toString() {
        return new StringJoiner(", ", DiskUpdateRequest.class.getSimpleName() + "[", "]")
                .add("volumeType=" + volumeType)
                .add("size=" + size)
                .add("group=" + group)
                .add("diskType=" + diskType)
                .toString();
    }
}
