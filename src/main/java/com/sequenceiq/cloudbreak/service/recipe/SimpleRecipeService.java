package com.sequenceiq.cloudbreak.service.recipe;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.APIResourceType;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.CbUserRole;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.RecipeRepository;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;

@Component
public class SimpleRecipeService implements RecipeService {

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private ClusterRepository clusterRepository;

    @Override
    public Recipe create(CbUser user, Recipe recipe) {
        recipe.setOwner(user.getUserId());
        recipe.setAccount(user.getAccount());
        try {
            return recipeRepository.save(recipe);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateKeyValueException(APIResourceType.RECIPE, recipe.getName(), ex);
        }
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

    @Override
    public Recipe getPrivateRecipe(String name, CbUser user) {
        Recipe recipe = recipeRepository.findByNameForUser(name, user.getUserId());
        if (recipe == null) {
            throw new NotFoundException(String.format("Recipe '%s' not found.", name));
        }
        return recipe;
    }

    @Override
    public Recipe getPublicRecipe(String name, CbUser user) {
        Recipe recipe = recipeRepository.findByNameInAccount(name, user.getAccount());
        if (recipe == null) {
            throw new NotFoundException(String.format("Recipe '%s' not found.", name));
        }
        return recipe;
    }

    @Override
    public void delete(Long id, CbUser user) {
        Recipe recipe = recipeRepository.findOne(id);
        if (recipe == null) {
            throw new NotFoundException(String.format("Recipe '%s' not found.", id));
        }
        delete(recipe, user);
    }

    @Override
    public void delete(String name, CbUser user) {
        Recipe recipe = recipeRepository.findByNameInAccount(name, user.getAccount());
        if (recipe == null) {
            throw new NotFoundException(String.format("Recipe '%s' not found.", name));
        }
        delete(recipe, user);
    }

    private void delete(Recipe recipe, CbUser user) {
        MDCBuilder.buildMdcContext(recipe);
        if (clusterRepository.findAllClustersByRecipe(recipe.getId()).isEmpty()) {
            if (!user.getUserId().equals(recipe.getOwner()) && !user.getRoles().contains(CbUserRole.ADMIN)) {
                throw new BadRequestException("Public recipes can only be deleted by owners or account admins.");
            } else {
                recipeRepository.delete(recipe);
            }
        } else {
            throw new BadRequestException(String.format(
                    "There are clusters associated with recipe '%s'. Please remove these before deleting the recipe.", recipe.getId()));
        }
    }
}
