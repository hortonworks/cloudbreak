package com.sequenceiq.cloudbreak.clusterdefinition;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ConfigProperty {

    private final String name;

    private final String prefix;

    private final String directory;

    public ConfigProperty(String name, String directory, String prefix) {
        this.name = name;
        this.directory = directory;
        this.prefix = prefix;
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getDirectory() {
        return directory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConfigProperty that = (ConfigProperty) o;

        return new EqualsBuilder()
                .append(name, that.name)
                .append(prefix, that.prefix)
                .append(directory, that.directory)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(name)
                .append(prefix)
                .append(directory)
                .toHashCode();
    }
}
