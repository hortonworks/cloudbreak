package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceTemplateParameterV4Base;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class YarnInstanceTemplateParametersV4 extends InstanceTemplateParameterV4Base {

    @ApiModelProperty
    private Integer memory;

    @ApiModelProperty
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
        map.put("memory", memory);
        map.put("cpus", cpus);
        return map;
    }

    @Override
    public <T> T toClass(Map<String, Object> parameters) {
        YarnInstanceTemplateParametersV4 ret = new YarnInstanceTemplateParametersV4();
        ret.setPlatformType(getPlatformType(parameters));
        ret.cpus = getInt(parameters, "cpus");
        ret.memory = getInt(parameters, "memory");
        return (T) ret;
    }

    private Integer getInt(Map<String, Object> parameters, String key) {
        String integer = getParameterOrNull(parameters, key);
        if (integer != null) {
            return Integer.parseInt(integer);
        }
        return null;
    }
}
