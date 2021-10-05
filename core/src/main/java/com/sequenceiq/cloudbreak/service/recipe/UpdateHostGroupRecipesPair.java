package com.sequenceiq.cloudbreak.service.recipe;

import java.util.Optional;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.UpdateHostGroupRecipes;

/**
 * Holds recipes to be attached and detached for specific host group / recipe pairs.
 */
public class UpdateHostGroupRecipesPair {

    private final UpdateHostGroupRecipes recipesToAttach;

    private final UpdateHostGroupRecipes recipesToDetach;

    public UpdateHostGroupRecipesPair(UpdateHostGroupRecipes recipesToAttach, UpdateHostGroupRecipes recipesToDetach) {
        this.recipesToAttach = recipesToAttach;
        this.recipesToDetach = recipesToDetach;
    }

    public Optional<UpdateHostGroupRecipes> getRecipesToAttach() {
        return Optional.ofNullable(recipesToAttach);
    }

    public Optional<UpdateHostGroupRecipes> getRecipesToDetach() {
        return Optional.ofNullable(recipesToDetach);
    }
}
