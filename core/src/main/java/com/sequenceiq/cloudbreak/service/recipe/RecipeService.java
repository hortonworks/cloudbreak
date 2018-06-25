package com.sequenceiq.cloudbreak.service.recipe;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.RecipeRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;

@Service
public class RecipeService {

    @Inject
    private RecipeRepository recipeRepository;

    @Inject
    private HostGroupRepository hostGroupRepository;

    @Inject
    private AuthorizationService authorizationService;

    public Recipe create(IdentityUser user, Recipe recipe) {
        recipe.setOwner(user.getUserId());
        recipe.setAccount(user.getAccount());
        try {
            return recipeRepository.save(recipe);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.RECIPE, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
    }

    public Recipe get(Long id) {
        Recipe recipe = recipeRepository.findById(id).orElseThrow(notFound("Recipe", id));
        authorizationService.hasReadPermission(recipe);
        return recipe;
    }

    public Set<Recipe> retrievePrivateRecipes(IdentityUser user) {
        return recipeRepository.findForUser(user.getUserId());
    }

    public Set<Recipe> retrieveAccountRecipes(IdentityUser user) {
        return user.getRoles().contains(IdentityUserRole.ADMIN) ? recipeRepository.findAllInAccount(user.getAccount())
                : recipeRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
    }

    public Recipe getPrivateRecipe(String name, IdentityUser user) {
        Recipe recipe = Optional.ofNullable(recipeRepository.findByNameForUser(name, user.getUserId()))
                .orElseThrow(notFound("Recipe", name));
        return recipe;
    }

    public Set<Recipe> getPublicRecipes(IdentityUser user, Collection<String> recipeNames) {
        Set<Recipe> recipes = recipeRepository.findByNameInAccount(recipeNames, user.getAccount());
        if (recipeNames.size() != recipes.size()) {
            Set<String> foundRecipes = recipes.stream().map(Recipe::getName).collect(Collectors.toSet());
            String missingRecipes = recipeNames.stream().filter(r -> !foundRecipes.contains(r)).collect(Collectors.joining(","));
            throw new NotFoundException(String.format("Recipes '%s' not found.", missingRecipes));
        }
        return recipes;
    }

    public Recipe getPublicRecipe(String name, IdentityUser user) {
        Recipe recipe = Optional.ofNullable(recipeRepository.findByNameInAccount(name, user.getAccount()))
                .orElseThrow(notFound("Recipe", name));
        return recipe;
    }

    public void delete(Long id, IdentityUser user) {
        delete(get(id));
    }

    public void delete(String name, IdentityUser user) {
        Recipe recipe = Optional.ofNullable(recipeRepository.findByNameInAccount(name, user.getAccount()))
                .orElseThrow(notFound("Recipe", name));
        delete(recipe);
    }

    private void delete(Recipe recipe) {
        authorizationService.hasWritePermission(recipe);
        List<HostGroup> hostGroupsWithRecipe = new ArrayList<>(hostGroupRepository.findAllHostGroupsByRecipe(recipe.getId()));
        if (!hostGroupsWithRecipe.isEmpty()) {
            if (hostGroupsWithRecipe.size() > 1) {
                String clusters = hostGroupsWithRecipe
                        .stream()
                        .map(hostGroup -> hostGroup.getCluster().getName())
                        .collect(Collectors.joining(", "));
                throw new BadRequestException(String.format(
                        "There are clusters associated with recipe '%s'. Please remove these before deleting the recipe. "
                                + "The following clusters are using this recipe: [%s]", recipe.getId(), clusters));
            } else {
                throw new BadRequestException(String.format("There is a cluster ['%s'] which uses recipe '%s'. Please remove this "
                        + "cluster before deleting the recipe", hostGroupsWithRecipe.get(0).getCluster().getName(), recipe.getName()));
            }
        }
        recipeRepository.delete(recipe);
    }
}
