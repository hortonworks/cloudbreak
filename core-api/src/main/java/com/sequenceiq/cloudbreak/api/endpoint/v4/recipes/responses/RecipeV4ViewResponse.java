package com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.api.model.CompactViewResponse;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

@ApiModel
public class RecipeV4ViewResponse extends CompactViewResponse {
    @NotNull
    @ApiModelProperty(ModelDescriptions.RecipeModelDescription.TYPE)
    private RecipeV4Type type;

    public RecipeV4Type Type() {
        return type;
    }

    public void setType(RecipeV4Type type) {
        this.type = type;
    }
}
