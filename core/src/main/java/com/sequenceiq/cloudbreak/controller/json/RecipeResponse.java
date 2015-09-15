package com.sequenceiq.cloudbreak.controller.json;

import org.codehaus.jackson.annotate.JsonProperty;

import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel
public class RecipeResponse extends RecipeBase {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
