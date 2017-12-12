package com.sequenceiq.cloudbreak.service.recipe;

import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.RecipeRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;

@Service
@Transactional
public class RecipeService {

    @Inject
    private RecipeRepository recipeRepository;

    @Inject
    private HostGroupRepository hostGroupRepository;

    @Inject
    private AuthorizationService authorizationService;

    @Transactional(TxType.NEVER)
    public Recipe create(IdentityUser user, Recipe recipe) {
        recipe.setOwner(user.getUserId());
        recipe.setAccount(user.getAccount());
        try {
            return recipeRepository.save(recipe);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], error: [%s]", APIResourceType.RECIPE, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
    }

    public Recipe get(Long id) {
        Recipe recipe = recipeRepository.findOne(id);
        if (recipe == null) {
            throw new NotFoundException(String.format("Recipe '%s' not found", id));
        }
        authorizationService.hasReadPermission(recipe);
        return recipe;
    }

    public Set<Recipe> retrievePrivateRecipes(IdentityUser user) {
        return recipeRepository.findForUser(user.getUserId());
    }

    public Set<Recipe> retrieveAccountRecipes(IdentityUser user) {
        if (user.getRoles().contains(IdentityUserRole.ADMIN)) {
            return recipeRepository.findAllInAccount(user.getAccount());
        } else {
            return recipeRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
        }
    }

    public Recipe getPrivateRecipe(String name, IdentityUser user) {
        Recipe recipe = recipeRepository.findByNameForUser(name, user.getUserId());
        if (recipe == null) {
            throw new NotFoundException(String.format("Recipe '%s' not found.", name));
        }
        return recipe;
    }

    public Set<Recipe> getPublicRecipes(IdentityUser user, Set<String> recipeNames) {
        Set<Recipe> recipes = recipeRepository.findByNameInAccount(recipeNames, user.getAccount());
        if (recipeNames.size() != recipes.size()) {
            Set<String> foundRecipes = recipes.stream().map(Recipe::getName).collect(Collectors.toSet());
            String missingRecipes = recipeNames.stream().filter(r -> !foundRecipes.contains(r)).collect(Collectors.joining(","));
            throw new NotFoundException(String.format("Recipes '%s' not found.", missingRecipes));
        }
        return recipes;
    }

    public Recipe getPublicRecipe(String name, IdentityUser user) {
        Recipe recipe = recipeRepository.findByNameInAccount(name, user.getAccount());
        if (recipe == null) {
            throw new NotFoundException(String.format("Recipe '%s' not found.", name));
        }
        return recipe;
    }

    public void delete(Long id, IdentityUser user) {
        delete(get(id));
    }

    public void delete(String name, IdentityUser user) {
        Recipe recipe = recipeRepository.findByNameInAccount(name, user.getAccount());
        if (recipe == null) {
            throw new NotFoundException(String.format("Recipe '%s' not found.", name));
        }
        delete(recipe);
    }

    private void delete(Recipe recipe) {
        authorizationService.hasWritePermission(recipe);
        if (!hostGroupRepository.findAllHostGroupsByRecipe(recipe.getId()).isEmpty()) {
            throw new BadRequestException(String.format(
                    "There are clusters associated with recipe '%s'. Please remove these before deleting the recipe.", recipe.getId()));
        }
        recipeRepository.delete(recipe);
    }
}
