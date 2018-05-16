package com.sequenceiq.cloudbreak.blueprint.template.views.filesystem;

import com.sequenceiq.cloudbreak.api.model.GcsFileSystemConfiguration;
import com.sequenceiq.cloudbreak.blueprint.template.views.FileSystemConfigurationView;

public class GcsFileSystemView extends FileSystemView<GcsFileSystemConfiguration> {

    private String defaultBucketName;

    private String projectId;

    private String serviceAccountEmail;

    public GcsFileSystemView(FileSystemConfigurationView fileSystemConfigurationView) {
        super(fileSystemConfigurationView);
        GcsFileSystemConfiguration gcsConfig = (GcsFileSystemConfiguration) fileSystemConfigurationView.getFileSystemConfiguration();
        this.defaultBucketName = gcsConfig.getDefaultBucketName();
        this.projectId = gcsConfig.getProjectId();
        this.serviceAccountEmail = gcsConfig.getServiceAccountEmail();
    }

    @Override
    public String defaultFsValue(GcsFileSystemConfiguration fileSystemConfiguration) {
        return String.format("gs://%s", fileSystemConfiguration.getDefaultBucketName());
    }

    public String getDefaultBucketName() {
        return defaultBucketName;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getServiceAccountEmail() {
        return serviceAccountEmail;
    }
}
