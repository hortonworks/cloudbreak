package com.sequenceiq.cloudbreak.blueprint.template.views.filesystem;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.blueprint.template.views.FileSystemConfigurationView;

public abstract class FileSystemView<T extends FileSystemConfiguration> {

    private final boolean useAsDefault;

    private final String defaultFs;

    private Map<String, String> properties;

    public FileSystemView(FileSystemConfigurationView fileSystemConfigurationView) {
        useAsDefault = fileSystemConfigurationView.isDefaultFs();
        properties = fileSystemConfigurationView.getFileSystemConfiguration().getDynamicProperties();
        defaultFs = defaultFsValue((T) fileSystemConfigurationView.getFileSystemConfiguration());
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String getDefaultFs() {
        return defaultFs;
    }

    public boolean isUseAsDefault() {
        return useAsDefault;
    }

    public abstract String defaultFsValue(T fileSystemConfiguration);
}
