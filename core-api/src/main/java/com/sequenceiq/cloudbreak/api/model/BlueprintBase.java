package com.sequenceiq.cloudbreak.api.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.BlueprintModelDescription;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Deserializer;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Serializer;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public abstract class BlueprintBase implements JsonEntity {

    @ApiModelProperty(BlueprintModelDescription.AMBARI_BLUEPRINT)
    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String ambariBlueprint;

    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @ApiModelProperty(BlueprintModelDescription.INPUTS)
    private Set<BlueprintParameterJson> inputs = new HashSet<>();

    @ApiModelProperty(BlueprintModelDescription.TAGS)
    private Map<String, Object> tags = new HashMap<>();

    public Map<String, Object> getTags() {
        return tags;
    }

    public void setTags(Map<String, Object> tags) {
        this.tags = tags;
    }

    public String getAmbariBlueprint() {
        return ambariBlueprint;
    }

    public void setAmbariBlueprint(String ambariBlueprint) {
        this.ambariBlueprint = ambariBlueprint;
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
