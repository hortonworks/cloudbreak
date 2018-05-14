package com.sequenceiq.cloudbreak.blueprint.template.views.filesystem;

import com.sequenceiq.cloudbreak.api.model.GcsFileSystemConfiguration;
import com.sequenceiq.cloudbreak.blueprint.template.views.FileSystemConfigurationView;

public class GcsFileSystemView extends FileSystemView<GcsFileSystemConfiguration> {

    private final String defaultBucketName;

    private final String projectId;

    private final String serviceAccountEmail;

    public GcsFileSystemView(FileSystemConfigurationView fileSystemConfigurationView) {
        super(fileSystemConfigurationView);
        GcsFileSystemConfiguration gcsConfig = (GcsFileSystemConfiguration) fileSystemConfigurationView.getFileSystemConfiguration();
        defaultBucketName = gcsConfig.getDefaultBucketName();
        projectId = gcsConfig.getProjectId();
        serviceAccountEmail = gcsConfig.getServiceAccountEmail();
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
