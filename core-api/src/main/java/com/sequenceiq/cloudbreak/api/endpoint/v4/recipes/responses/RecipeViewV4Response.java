package com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.CompactViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RecipeModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class RecipeViewV4Response extends CompactViewV4Response {
    @NotNull
    @ApiModelProperty(RecipeModelDescription.TYPE)
    private RecipeV4Type type;

    public RecipeV4Type getType() {
        return type;
    }

    public void setType(RecipeV4Type type) {
        this.type = type;
    }
}
