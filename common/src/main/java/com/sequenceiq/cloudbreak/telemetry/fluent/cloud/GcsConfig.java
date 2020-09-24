package com.sequenceiq.cloudbreak.telemetry.fluent.cloud;

public class GcsConfig extends CloudStorageConfig {

    private final String bucket;

    private final String projectId;

    public GcsConfig(String folderPrefix, String bucket, String projectId) {
        super(folderPrefix);
        this.bucket = bucket;
        this.projectId = projectId;
    }

    public String getBucket() {
        return bucket;
    }

    public String getProjectId() {
        return projectId;
    }
}
