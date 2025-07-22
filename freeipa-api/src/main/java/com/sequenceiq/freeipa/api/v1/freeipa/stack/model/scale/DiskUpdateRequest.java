package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale;

import java.util.StringJoiner;

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

    public String toString() {
        return new StringJoiner(", ", DiskUpdateRequest.class.getSimpleName() + "[", "]")
                .add("volumeType=" + volumeType)
                .add("size=" + size)
                .toString();
    }
}
