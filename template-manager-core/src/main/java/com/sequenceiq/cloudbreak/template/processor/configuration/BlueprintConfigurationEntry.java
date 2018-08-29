package com.sequenceiq.cloudbreak.template.processor.configuration;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class BlueprintConfigurationEntry {

    private final String configFile;

    private final String key;

    private final String value;

    public BlueprintConfigurationEntry(String configFile, String key, String value) {
        this.configFile = configFile;
        this.key = key;
        this.value = value;
    }

    public String getConfigFile() {
        return configFile;
    }

    public String getKey() {
        return key;
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

        BlueprintConfigurationEntry that = (BlueprintConfigurationEntry) o;

        return new EqualsBuilder()
                .append(configFile, that.configFile)
                .append(key, that.key)
                .append(value, that.value)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(configFile)
                .append(key)
                .append(value)
                .toHashCode();
    }
}
