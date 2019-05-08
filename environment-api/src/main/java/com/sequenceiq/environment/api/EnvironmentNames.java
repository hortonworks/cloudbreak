package com.sequenceiq.environment.api;

import java.util.Set;

import javax.validation.constraints.NotEmpty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class EnvironmentNames {

    @NotEmpty
    @ApiModelProperty(required = true)
    private Set<String> environmentNames;

    public Set<String> getEnvironmentNames() {
        return environmentNames;
    }

    public void setEnvironmentNames(Set<String> environmentNames) {
        this.environmentNames = environmentNames;
    }
}
