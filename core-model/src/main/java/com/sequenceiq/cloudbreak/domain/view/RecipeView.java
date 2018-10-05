package com.sequenceiq.cloudbreak.domain.view;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.api.model.RecipeType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;

@Entity
@Table(name = "Recipe")
public class RecipeView extends CompactViewWithOwner {
    @Enumerated(EnumType.STRING)
    private RecipeType recipeType;

    public RecipeType getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(RecipeType recipeType) {
        this.recipeType = recipeType;
    }

    @Override
    public WorkspaceResource getResource() {
        return WorkspaceResource.RECIPE;
    }
}
