package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.common.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class VmTypeMetaJson implements JsonEntity {
    private List<VolumeParameterConfigV4Response> configs = new ArrayList<>();

    private Map<String, Object> properties = new HashMap<>();

    public List<VolumeParameterConfigV4Response> getConfigs() {
        return configs;
    }

    public void setConfigs(List<VolumeParameterConfigV4Response> configs) {
        this.configs = configs;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
