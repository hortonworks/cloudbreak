package com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.gcs;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfiguration;

public class GcsFileSystemConfiguration extends FileSystemConfiguration {
    @NotNull
    private String projectId;
    @NotNull
    private String serviceAccountEmail;
    @NotNull
    private String privateKeyEncoded;
    @NotNull
    private String defaultBucketName;

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getServiceAccountEmail() {
        return serviceAccountEmail;
    }

    public void setServiceAccountEmail(String serviceAccountEmail) {
        this.serviceAccountEmail = serviceAccountEmail;
    }

    public String getPrivateKeyEncoded() {
        return privateKeyEncoded;
    }

    public void setPrivateKeyEncoded(String privateKeyEncoded) {
        this.privateKeyEncoded = privateKeyEncoded;
    }

    public String getDefaultBucketName() {
        return defaultBucketName;
    }

    public void setDefaultBucketName(String defaultBucketName) {
        this.defaultBucketName = defaultBucketName;
    }
}
