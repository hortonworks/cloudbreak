package com.sequenceiq.environment.api.v1.environment.model.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.api.cloudstorage.CloudStorageV1Base;
import com.sequenceiq.common.api.filesystem.FileSystemType;
import com.sequenceiq.environment.api.v1.environment.model.request.CloudStorageRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.CloudStorageResponse;

import io.swagger.annotations.ApiModel;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(subTypes = {CloudStorageRequest.class, CloudStorageResponse.class})
public class CloudStorageBase extends CloudStorageV1Base {

    private FileSystemType fileSystemType;

    private String baseLocation;

    public FileSystemType getFileSystemType() {
        return fileSystemType;
    }

    public void setFileSystemType(FileSystemType fileSystemType) {
        this.fileSystemType = fileSystemType;
    }

    public String getBaseLocation() {
        return baseLocation;
    }

    public void setBaseLocation(String baseLocation) {
        this.baseLocation = baseLocation;
    }
}
