package com.sequenceiq.freeipa.service.freeipa.backup.cloud;

public class S3BackupConfig extends CloudBackupStorageConfig {

    private final String bucket;

    public S3BackupConfig(String folderPrefix, String bucket) {
        super(folderPrefix);
        this.bucket = bucket;
    }

    public String getBucket() {
        return bucket;
    }
}
