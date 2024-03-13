package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class SdxDatabaseComputeStorageRequest {

    @Schema(description = ModelDescriptions.DATABASE_INSTANCE_TYPE)
    private String instanceType;

    @Schema(description = ModelDescriptions.DATABASE_INSTANCE_STORAGE)
    private Long storageSize;

    public Long getStorageSize() {
        return storageSize;
    }

    public void setStorageSize(Long storageSize) {
        this.storageSize = storageSize;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    @Override
    public String toString() {
        return "SdxDatabaseComputeStorageRequest{" +
                "instanceType='" + instanceType + '\'' +
                ", storageSize=" + storageSize +
                '}';
    }
}