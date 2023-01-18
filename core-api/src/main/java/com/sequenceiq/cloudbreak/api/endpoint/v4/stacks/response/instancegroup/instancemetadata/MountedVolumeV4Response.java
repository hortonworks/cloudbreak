package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MountedVolumeV4Response implements JsonEntity {

    @Schema
    private String volumeId;

    @Schema
    private String device;

    @Schema
    private String volumeType;

    @Schema
    private String volumeSize;

    public String getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
    }

    public String getVolumeSize() {
        return volumeSize;
    }

    public void setVolumeSize(String volumeSize) {
        this.volumeSize = volumeSize;
    }
}
