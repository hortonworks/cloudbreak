package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VmTypeMetaJson implements JsonEntity {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<VolumeParameterConfigResponse> configs = new ArrayList<>();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> properties = new HashMap<>();

    public List<VolumeParameterConfigResponse> getConfigs() {
        return configs;
    }

    public void setConfigs(List<VolumeParameterConfigResponse> configs) {
        this.configs = configs;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "VmTypeMetaJson{" +
                "configs=" + configs +
                ", properties=" + properties +
                '}';
    }
}