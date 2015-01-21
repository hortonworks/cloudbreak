package com.sequenceiq.cloudbreak.service.recipe;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Recipe;

public interface RecipeService {

    Recipe create(CbUser user, Recipe recipe);

    Recipe get(Long id);

    Set<Recipe> retrievePrivateRecipes(CbUser user);

    Set<Recipe> retrieveAccountRecipes(CbUser user);

    Recipe getPrivateRecipe(String name, CbUser user);

    Recipe getPublicRecipe(String name, CbUser user);
}
