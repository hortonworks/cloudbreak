package com.sequenceiq.cloudbreak.template.processor.template.views;

import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;

public class FileSystemConfigurationView {

    private FileSystemConfiguration fileSystemConfiguration;

    private boolean defaultFs;

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
