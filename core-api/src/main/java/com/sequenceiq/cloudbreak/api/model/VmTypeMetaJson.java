package com.sequenceiq.cloudbreak.api.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class VmTypeMetaJson implements JsonEntity {
    private List<VolumeParameterConfigJson> configs = new ArrayList<>();

    private Map<String, String> properties = new HashMap<>();

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
