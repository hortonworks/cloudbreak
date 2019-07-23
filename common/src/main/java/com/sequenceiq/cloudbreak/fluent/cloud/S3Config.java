package com.sequenceiq.cloudbreak.fluent.cloud;

public class S3Config extends CloudStorageConfig {

    private final String bucket;

    public S3Config(String folderPrefix, String bucket) {
        super(folderPrefix);
        this.bucket = bucket;
    }

    public String getBucket() {
        return bucket;
    }
}
