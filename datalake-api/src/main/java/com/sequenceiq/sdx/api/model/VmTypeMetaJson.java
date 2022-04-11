package com.sequenceiq.sdx.api.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VmTypeMetaJson {

    public static final String CPU = "Cpu";

    public static final String MEMORY = "Memory";

    @ApiModelProperty(ModelDescriptions.VM_TYPE_CONFIGS)
    private List<VolumeParameterConfigResponse> configs = new ArrayList<>();

    @ApiModelProperty(ModelDescriptions.VM_TYPE_PROPERTIES)
    private Map<String, Object> properties = new HashMap<>();

    public VmTypeMetaJson() {
    }

    public VmTypeMetaJson(List<VolumeParameterConfigResponse> configs, Map<String, Object> properties) {
        this.configs = configs;
        this.properties = properties;
    }

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

    public Integer getCPU() {
        Object cpuAsObject = properties.get(CPU);
        return cpuAsObject != null ? Integer.valueOf(cpuAsObject.toString()) : null;
    }

    public Float getMemoryInGb() {
        Object memoryAsObject = properties.get(MEMORY);
        return memoryAsObject != null ? Float.valueOf(memoryAsObject.toString()) : null;
    }

    @Override
    public String toString() {
        return "VmTypeMetaJson{" +
                "configs=" + configs +
                ", properties=" + properties +
                '}';
    }
}
