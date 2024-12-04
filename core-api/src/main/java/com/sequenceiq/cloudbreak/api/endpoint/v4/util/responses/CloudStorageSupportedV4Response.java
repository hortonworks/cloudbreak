package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudStorageSupportedV4Response {
    private String provider;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<String> fileSystemType = new HashSet<>();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Set<String> getFileSystemType() {
        return fileSystemType;
    }

    public void setFileSystemType(Set<String> fileSystemType) {
        this.fileSystemType = fileSystemType;
    }
}
