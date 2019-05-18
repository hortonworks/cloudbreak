package com.sequenceiq.environment.api.platformresource.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class VmTypeMetaJson implements Serializable {
    private List<VolumeParameterConfigV1Response> configs = new ArrayList<>();

    private Map<String, Object> properties = new HashMap<>();

    public List<VolumeParameterConfigV1Response> getConfigs() {
        return configs;
    }

    public void setConfigs(List<VolumeParameterConfigV1Response> configs) {
        this.configs = configs;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
