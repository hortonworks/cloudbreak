package com.sequenceiq.freeipa.service.freeipa.backup.cloud;

public class CloudBackupStorageConfig {

    private final String folderPrefix;

    public CloudBackupStorageConfig(String folderPrefix) {
        this.folderPrefix = folderPrefix;
    }

    public String getFolderPrefix() {
        return folderPrefix;
    }

}
