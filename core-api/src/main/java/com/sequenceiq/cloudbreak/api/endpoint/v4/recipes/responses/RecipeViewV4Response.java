package com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses;

import jakarta.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.CompactViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RecipeModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class RecipeViewV4Response extends CompactViewV4Response {
    @NotNull
    @Schema(description = RecipeModelDescription.TYPE)
    private RecipeV4Type type;

    private Long created;

    public RecipeV4Type getType() {
        return type;
    }

    public void setType(RecipeV4Type type) {
        this.type = type;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }
}
