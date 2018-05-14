package com.sequenceiq.cloudbreak.blueprint.template.views;

import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;

public class FileSystemConfigurationView {

    private final FileSystemConfiguration fileSystemConfiguration;

    private final boolean defaultFs;

    public FileSystemConfigurationView(FileSystemConfiguration fileSystemConfiguration) {
        this.fileSystemConfiguration = fileSystemConfiguration;
        defaultFs = false;
    }

    public FileSystemConfigurationView(FileSystemConfiguration fileSystemConfiguration, boolean defaultFs) {
        this.fileSystemConfiguration = fileSystemConfiguration;
        this.defaultFs = defaultFs;
    }

    public FileSystemConfiguration getFileSystemConfiguration() {
        return fileSystemConfiguration;
    }

    public boolean isDefaultFs() {
        return defaultFs;
    }
}
