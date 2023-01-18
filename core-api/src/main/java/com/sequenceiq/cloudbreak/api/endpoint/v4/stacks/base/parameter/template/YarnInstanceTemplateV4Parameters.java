package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceTemplateV4ParameterBase;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class YarnInstanceTemplateV4Parameters extends InstanceTemplateV4ParameterBase {

    @Schema
    private Integer memory;

    @Schema
    private Integer cpus;

    public Integer getMemory() {
        return memory;
    }

    public void setMemory(Integer memory) {
        this.memory = memory;
    }

    public Integer getCpus() {
        return cpus;
    }

    public void setCpus(Integer cpus) {
        this.cpus = cpus;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        putIfValueNotNull(map, "memory", memory);
        putIfValueNotNull(map, "cpus", cpus);
        return map;
    }

    @Override
    @JsonIgnore
    @Schema(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.YARN;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        cpus = getInt(parameters, "cpus");
        memory = getInt(parameters, "memory");
    }
}
