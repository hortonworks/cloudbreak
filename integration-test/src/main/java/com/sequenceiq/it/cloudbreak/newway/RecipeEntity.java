package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.api.model.RecipeType;

public class RecipeEntity extends AbstractCloudbreakEntity<RecipeRequest, RecipeResponse, RecipeEntity> {
    public static final String RECIPE = "RECIPE";

    RecipeEntity(String newId) {
        super(newId);
        setRequest(new RecipeRequest());
    }

    RecipeEntity() {
        this(RECIPE);
    }

    public RecipeEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public RecipeEntity withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public RecipeEntity withContent(String content) {
        getRequest().setContent(content);
        return this;
    }

    public RecipeEntity withRecipeType(RecipeType recipeType) {
        getRequest().setRecipeType(recipeType);
        return this;
    }

    public RecipeEntity withUri(String uri) {
        getRequest().setUri(uri);
        return this;
    }
}
