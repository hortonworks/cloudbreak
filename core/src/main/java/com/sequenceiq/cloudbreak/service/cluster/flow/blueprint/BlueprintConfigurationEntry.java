package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint;

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
}
