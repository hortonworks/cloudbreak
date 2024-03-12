package com.sequenceiq.sdx.api.model;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class SdxInstanceGroupDiskRequest {

    @NotNull
    @Schema(description = ModelDescriptions.INSTANCE_GROUP_NAME)
    private String name;

    @Schema(description = ModelDescriptions.INSTANCE_DISK_SIZE)
    private Integer diskSize;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getInstanceDiskSize() {
        return diskSize;
    }

    public void setInstanceDiskSize(Integer instanceType) {
        diskSize = instanceType;
    }

    @Override
    public String toString() {
        return "SdxInstanceGroupDiskRequest{" +
                "name='" + name + '\'' +
                ", diskSize=" + diskSize +
                '}';
    }
}