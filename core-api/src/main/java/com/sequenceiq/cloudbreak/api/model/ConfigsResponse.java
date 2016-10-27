package com.sequenceiq.cloudbreak.api.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigsResponse {

    @ApiModelProperty(value = ModelDescriptions.RESPONSE, required = true)
    private Set<BlueprintInputJson> inputs = new HashSet<>();

    public ConfigsResponse() {
    }

    public Set<BlueprintInputJson> getInputs() {
        return inputs;
    }

    public void setInputs(Set<BlueprintInputJson> inputs) {
        this.inputs = inputs;
    }
}
