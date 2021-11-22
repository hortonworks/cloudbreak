package com.sequenceiq.cloudbreak.template.filesystem;

import java.io.Serializable;
import java.util.Objects;

import com.sequenceiq.cloudbreak.domain.StorageLocation;

public class StorageLocationView implements Serializable {

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StorageLocationView that = (StorageLocationView) o;
        return Objects.equals(configFile, that.configFile)
                && Objects.equals(property, that.property)
                && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configFile, property, value);
    }
}
