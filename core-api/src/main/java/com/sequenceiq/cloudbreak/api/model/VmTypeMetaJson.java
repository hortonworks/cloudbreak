package com.sequenceiq.cloudbreak.api.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VmTypeMetaJson {
    private List<VolumeParameterConfigJson> configs = new ArrayList<>();

    private Map<String, String> properties = new HashMap<>();

    public VmTypeMetaJson() {

    }

    public List<VolumeParameterConfigJson> getConfigs() {
        return configs;
    }

    public void setConfigs(List<VolumeParameterConfigJson> configs) {
        this.configs = configs;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
