package com.sequenceiq.cloudbreak.api.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigsRequest {

    @ApiModelProperty(value = ModelDescriptions.REQUESTS, required = true)
    private Set<BlueprintParameterJson> requests = new HashSet<>();

    public ConfigsRequest() {
    }

    public Set<BlueprintParameterJson> getRequests() {
        return requests;
    }

    public void setRequests(Set<BlueprintParameterJson> requests) {
        this.requests = requests;
    }
}
