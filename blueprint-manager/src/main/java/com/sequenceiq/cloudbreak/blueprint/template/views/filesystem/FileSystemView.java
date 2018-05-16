package com.sequenceiq.cloudbreak.blueprint.template.views.filesystem;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.blueprint.template.views.FileSystemConfigurationView;

public abstract class FileSystemView<T extends FileSystemConfiguration> {

    private boolean useAsDefault;

    private String defaultFs;

    private Map<String, String> properties = new HashMap<>();

    public FileSystemView(FileSystemConfigurationView fileSystemConfigurationView) {
        this.useAsDefault = fileSystemConfigurationView.isDefaultFs();
        this.properties = fileSystemConfigurationView.getFileSystemConfiguration().getDynamicProperties();
        this.defaultFs = defaultFsValue((T) fileSystemConfigurationView.getFileSystemConfiguration());
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
