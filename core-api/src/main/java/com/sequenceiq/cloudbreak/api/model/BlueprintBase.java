package com.sequenceiq.cloudbreak.api.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.BlueprintModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.HashSet;
import java.util.Set;

@ApiModel
public abstract class BlueprintBase implements JsonEntity {

    @Size(max = 100, min = 1, message = "The length of the blueprint's name has to be in range of 1 to 100")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;

    @ApiModelProperty(value = BlueprintModelDescription.AMBARI_BLUEPRINT)
    private String ambariBlueprint;

    @Size(max = 1000)
    @ApiModelProperty(value = ModelDescriptions.DESCRIPTION)
    private String description;

    @ApiModelProperty(value = BlueprintModelDescription.INPUTS)
    private Set<BlueprintParameterJson> inputs = new HashSet<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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
