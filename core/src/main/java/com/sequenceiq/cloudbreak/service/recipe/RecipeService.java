package com.sequenceiq.cloudbreak.service.recipe;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
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
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;

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
            throw new DuplicateKeyValueException(APIResourceType.RECIPE, recipe.getName(), ex);
        }
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    public Recipe get(Long id) {
        Recipe recipe = recipeRepository.findOne(id);
        if (recipe == null) {
            throw new NotFoundException(String.format("Recipe '%s' not found", id));
        }
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
