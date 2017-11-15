package com.sequenceiq.cloudbreak.orchestrator.yarn.model.core;

import java.util.List;
import java.util.Map;

public class Configuration {
    private Map<String, String> properties;

    private Map<String, String> env;

    private List<ConfigFile> files;

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public void setEnv(Map<String, String> env) {
        this.env = env;
    }

    public List<ConfigFile> getFiles() {
        return files;
    }

    public void setFiles(List<ConfigFile> files) {
        this.files = files;
    }
}
