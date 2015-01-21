package com.sequenceiq.cloudbreak.service.recipe;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.CbUserRole;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.RecipeRepository;

@Component
public class SimpleRecipeService implements RecipeService {

    @Autowired
    private RecipeRepository recipeRepository;

    @Override
    public Recipe create(CbUser user, Recipe recipe) {
        recipe.setOwner(user.getUserId());
        recipe.setAccount(user.getAccount());
        return recipeRepository.save(recipe);
    }

    @Override
    public Recipe get(Long id) {
        Recipe recipe = recipeRepository.findOne(id);
        MDCBuilder.buildMdcContext(recipe);
        if (recipe == null) {
            throw new NotFoundException(String.format("Recipe '%s' not found", id));
        }
        return recipe;
    }

    @Override
    public Set<Recipe> retrievePrivateRecipes(CbUser user) {
        return recipeRepository.findForUser(user.getUserId());
    }

    @Override
    public Set<Recipe> retrieveAccountRecipes(CbUser user) {
        if (user.getRoles().contains(CbUserRole.ADMIN)) {
            return recipeRepository.findAllInAccount(user.getAccount());
        } else {
            return recipeRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
        }
    }
}
