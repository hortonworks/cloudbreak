package com.sequenceiq.cloudbreak.api.endpoint.v4.recipes;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RecipeModelDescription;
import com.sequenceiq.cloudbreak.validation.ValidBase64;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

public abstract class RecipeV4Base implements JsonEntity {

    @Size(max = 100, min = 5, message = "The length of the recipe's name has to be in range of 5 to 100")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The recipe's name can only contain lowercase alphanumeric characters and "
                    + "hyphens and has start with an alphanumeric character")
    @Schema(description = ModelDescriptions.NAME)
    private String name;

    @Size(max = 1000)
    @Schema(description = ModelDescriptions.DESCRIPTION)
    private String description;

    @NotNull
    @Schema(description = RecipeModelDescription.TYPE, requiredMode = Schema.RequiredMode.REQUIRED)
    private RecipeV4Type type = RecipeV4Type.PRE_CLOUDERA_MANAGER_START;

    @Schema(description = RecipeModelDescription.CONTENT)
    @ValidBase64
    private String content;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RecipeV4Type getType() {
        return type;
    }

    public void setType(RecipeV4Type type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
