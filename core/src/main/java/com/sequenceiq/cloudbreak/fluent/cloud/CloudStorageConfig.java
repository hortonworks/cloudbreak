package com.sequenceiq.cloudbreak.fluent.cloud;

public abstract class CloudStorageConfig {

    private final String folderPrefix;

    public CloudStorageConfig(String folderPrefix) {
        this.folderPrefix = folderPrefix;
    }

    public String getFolderPrefix() {
        return folderPrefix;
    }
}
