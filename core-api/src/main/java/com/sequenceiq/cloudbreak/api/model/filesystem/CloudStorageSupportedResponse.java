package com.sequenceiq.cloudbreak.api.model.filesystem;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudStorageSupportedResponse {
    private String provider;

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
