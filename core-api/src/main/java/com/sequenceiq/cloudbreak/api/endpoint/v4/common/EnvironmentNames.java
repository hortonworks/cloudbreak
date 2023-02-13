package com.sequenceiq.cloudbreak.api.endpoint.v4.common;

import static io.swagger.v3.oas.annotations.media.Schema.*;

import java.util.Set;

import javax.validation.constraints.NotEmpty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class EnvironmentNames {

    @NotEmpty
    @Schema(required = true)
    private Set<String> environmentNames;

    public Set<String> getEnvironmentNames() {
        return environmentNames;
    }

    public void setEnvironmentNames(Set<String> environmentNames) {
        this.environmentNames = environmentNames;
    }
}
