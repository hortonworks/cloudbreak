package com.sequenceiq.cloudbreak.api.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigsResponse {

    @ApiModelProperty(value = ModelDescriptions.FIXINPUTS, required = true)
    private Map<String, Object> fixInputs = new HashMap<>();

    @ApiModelProperty(value = ModelDescriptions.DATALAKEINPUTS, required = true)
    private Map<String, Object> datalakeInputs = new HashMap<>();

    @ApiModelProperty(value = ModelDescriptions.RESPONSE, required = true)
    private Set<BlueprintInputJson> inputs = new HashSet<>();

    public Set<BlueprintInputJson> getInputs() {
        return inputs;
    }

    public void setInputs(Set<BlueprintInputJson> inputs) {
        this.inputs = inputs;
    }

    public Map<String, Object> getFixInputs() {
        return fixInputs;
    }

    public void setFixInputs(Map<String, Object> fixInputs) {
        this.fixInputs = fixInputs;
    }

    public Map<String, Object> getDatalakeInputs() {
        return datalakeInputs;
    }

    public void setDatalakeInputs(Map<String, Object> datalakeInputs) {
        this.datalakeInputs = datalakeInputs;
    }
}
