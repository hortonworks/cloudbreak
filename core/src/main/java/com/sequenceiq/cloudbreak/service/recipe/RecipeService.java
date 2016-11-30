package com.sequenceiq.cloudbreak.service.recipe;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.CbUserRole;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.RecipeRepository;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;

@Service
@Transactional
public class RecipeService {

    @Inject
    private RecipeRepository recipeRepository;

    @Inject
    private HostGroupRepository hostGroupRepository;

    @Inject
    private RecipeMigration recipeMigration;

    @PostConstruct
    public void migrate() {
        recipeMigration.migrate();
    }

    @Transactional(Transactional.TxType.NEVER)
    public Recipe create(CbUser user, Recipe recipe) {
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

    public Set<Recipe> retrievePrivateRecipes(CbUser user) {
        return recipeRepository.findForUser(user.getUserId());
    }

    public Set<Recipe> retrieveAccountRecipes(CbUser user) {
        if (user.getRoles().contains(CbUserRole.ADMIN)) {
            return recipeRepository.findAllInAccount(user.getAccount());
        } else {
            return recipeRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
        }
    }

    public Recipe getPrivateRecipe(String name, CbUser user) {
        Recipe recipe = recipeRepository.findByNameForUser(name, user.getUserId());
        if (recipe == null) {
            throw new NotFoundException(String.format("Recipe '%s' not found.", name));
        }
        return recipe;
    }

    public Recipe getPublicRecipe(String name, CbUser user) {
        Recipe recipe = recipeRepository.findByNameInAccount(name, user.getAccount());
        if (recipe == null) {
            throw new NotFoundException(String.format("Recipe '%s' not found.", name));
        }
        return recipe;
    }

    public void delete(Long id, CbUser user) {
        Recipe recipe = get(id);
        if (recipe == null) {
            throw new NotFoundException(String.format("Recipe '%s' not found.", id));
        }
        delete(recipe, user);
    }

    public void delete(String name, CbUser user) {
        Recipe recipe = recipeRepository.findByNameInAccount(name, user.getAccount());
        if (recipe == null) {
            throw new NotFoundException(String.format("Recipe '%s' not found.", name));
        }
        delete(recipe, user);
    }

    private void delete(Recipe recipe, CbUser user) {
        if (hostGroupRepository.findAllHostGroupsByRecipe(recipe.getId()).isEmpty()) {
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
