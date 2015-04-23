package com.sequenceiq.cloudbreak.controller.json;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions.BlueprintModelDescription;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@ApiModel
public abstract class BlueprintBase implements JsonEntity {

    @Size(max = 100, min = 1, message = "The length of the blueprint's name has to be in range of 1 to 100")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "The blueprint's name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;

    @ApiModelProperty(value = BlueprintModelDescription.AMBARI_BLUEPRINT, required = true)
    private String ambariBlueprint;

    @Size(max = 1000)
    @ApiModelProperty(value = ModelDescriptions.DESCRIPTION)
    private String description;

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
}
