package com.sequenceiq.environment.api;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;

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
