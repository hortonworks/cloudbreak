package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint;

public class BlueprintConfigurationEntry {

    private String configFile;

    private String key;

    private String value;

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
