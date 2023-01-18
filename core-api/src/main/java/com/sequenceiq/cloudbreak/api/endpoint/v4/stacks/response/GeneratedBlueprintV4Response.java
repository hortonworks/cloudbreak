package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.BlueprintModelDescription;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Deserializer;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Serializer;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class GeneratedBlueprintV4Response implements JsonEntity {

    @Schema(description = BlueprintModelDescription.BLUEPRINT)
    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String blueprintText;

    public String getBlueprintText() {
        return blueprintText;
    }

    public void setBlueprintText(String blueprintText) {
        this.blueprintText = blueprintText;
    }
}
