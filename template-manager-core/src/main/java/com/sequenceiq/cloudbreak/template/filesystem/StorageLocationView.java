package com.sequenceiq.cloudbreak.template.filesystem;

import com.sequenceiq.cloudbreak.domain.StorageLocation;

public class StorageLocationView {

    private final String configFile;

    private final String property;

    private final String value;

    public StorageLocationView(StorageLocation storageLocation) {
        configFile = storageLocation.getConfigFile();
        property = storageLocation.getProperty();
        value = storageLocation.getValue();
    }

    public String getConfigFile() {
        return configFile;
    }

    public String getProperty() {
        return property;
    }

    public String getValue() {
        return value;
    }

}
