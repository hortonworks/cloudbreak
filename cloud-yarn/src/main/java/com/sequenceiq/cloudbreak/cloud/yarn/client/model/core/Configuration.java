package com.sequenceiq.cloudbreak.cloud.yarn.client.model.core;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

@JsonInclude(Include.NON_NULL)
public class Configuration  implements JsonEntity {
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
