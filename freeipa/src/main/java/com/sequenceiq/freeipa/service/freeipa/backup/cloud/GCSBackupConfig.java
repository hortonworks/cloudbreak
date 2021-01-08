package com.sequenceiq.freeipa.service.freeipa.backup.cloud;

public class GCSBackupConfig extends CloudBackupStorageConfig {

    private final String bucket;

    public GCSBackupConfig(String folderPrefix, String bucket) {
        super(folderPrefix);
        this.bucket = bucket;
    }

    public String getBucket() {
        return bucket;
    }
}
