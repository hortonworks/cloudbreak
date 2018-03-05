package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.BlueprintModelDescription;
import com.sequenceiq.cloudbreak.json.Base64Deserializer;
import com.sequenceiq.cloudbreak.json.Base64Serializer;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("GeneratedBlueprintResponse")
public class GeneratedBlueprintResponse extends BlueprintBase {

    @ApiModelProperty(BlueprintModelDescription.AMBARI_BLUEPRINT)
    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String ambariBlueprint;

    public GeneratedBlueprintResponse(String ambariBlueprint) {
        this.ambariBlueprint = ambariBlueprint;
    }

    @Override
    public String getAmbariBlueprint() {
        return ambariBlueprint;
    }

    @Override
    public void setAmbariBlueprint(String ambariBlueprint) {
        this.ambariBlueprint = ambariBlueprint;
    }
}
