package com.sequenceiq.cloudbreak.service.cluster;

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
}
