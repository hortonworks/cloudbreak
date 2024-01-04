package com.sequenceiq.cloudbreak.recipe;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.CompactViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Responses;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;

@Service
public class RecipeCrnListProviderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeCrnListProviderService.class);

    @Inject
    private RecipeV4Endpoint recipeV4Endpoint;

    public List<String> getResourceCrnListByResourceNameList(Collection<String> resourceNames) {
        LOGGER.info("Get resources crn list for recipes: {}", resourceNames);
        RecipeViewV4Responses recipes = recipeV4Endpoint.list(0L);
        validateRequestedRecipesExistsByName(resourceNames, recipes);
        List<String> resourceCrns = recipes.getResponses().stream()
                .filter(recipe -> resourceNames.contains(recipe.getName())).map(CompactViewV4Response::getCrn).collect(Collectors.toList());
        LOGGER.info("Resource crns for recipes: {}", resourceCrns);
        return resourceCrns;
    }

    public void validateRequestedRecipesExistsByName(Collection<String> resourceNames) {
        LOGGER.info("Get resources crn list for recipes: {}", resourceNames);
        RecipeViewV4Responses recipes = recipeV4Endpoint.list(0L);
        validateRequestedRecipesExistsByName(resourceNames, recipes);
    }

    private void validateRequestedRecipesExistsByName(Collection<String> resourceNames, RecipeViewV4Responses recipes) {
        List<String> recipeNamesFromCore = recipes.getResponses().stream().map(CompactViewV4Response::getName).collect(Collectors.toList());
        List<String> recipesNotFound = resourceNames.stream().filter(recipeName -> !recipeNamesFromCore.contains(recipeName)).collect(Collectors.toList());
        if (recipesNotFound.size() > 0) {
            LOGGER.info("Missing recipes: {}", recipesNotFound);
            throw new NotFoundException("Following recipes does not exist: " + recipesNotFound);
        }
    }

}
