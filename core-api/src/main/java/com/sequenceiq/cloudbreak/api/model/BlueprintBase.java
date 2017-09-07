package com.sequenceiq.cloudbreak.api.model;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.BlueprintModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public abstract class BlueprintBase implements JsonEntity {

    @ApiModelProperty(BlueprintModelDescription.AMBARI_BLUEPRINT)
    private String ambariBlueprint;

    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @ApiModelProperty(BlueprintModelDescription.INPUTS)
    private Set<BlueprintParameterJson> inputs = new HashSet<>();

    @JsonRawValue
    public String getAmbariBlueprint() {
        return ambariBlueprint;
    }

    public void setAmbariBlueprint(JsonNode ambariBlueprint) {
        this.ambariBlueprint = ambariBlueprint.toString();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<BlueprintParameterJson> getInputs() {
        return inputs;
    }

    public void setInputs(Set<BlueprintParameterJson> inputs) {
        this.inputs = inputs;
    }
}
