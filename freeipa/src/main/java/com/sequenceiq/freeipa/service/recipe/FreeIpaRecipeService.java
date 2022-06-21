package com.sequenceiq.freeipa.service.recipe;

import static com.sequenceiq.authorization.resource.AuthorizationResourceType.RECIPE;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.AuthorizationResourceCrnListProvider;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;
import com.sequenceiq.cloudbreak.recipe.RecipeCrnListProviderService;
import com.sequenceiq.freeipa.entity.FreeIpaStackRecipe;
import com.sequenceiq.freeipa.repository.FreeIpaStackRecipeRepository;

@Service
public class FreeIpaRecipeService implements AuthorizationResourceCrnListProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaRecipeService.class);

    @Inject
    private RecipeV4Endpoint recipeV4Endpoint;

    @Inject
    private FreeIpaStackRecipeRepository freeIpaStackRecipeRepository;

    @Inject
    private RecipeCrnListProviderService recipeCrnListProviderService;

    @Override
    public List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
        return recipeCrnListProviderService.getResourceCrnListByResourceNameList(resourceNames);
    }

    public Set<String> getRecipeNamesForStack(Long stackId) {
        return freeIpaStackRecipeRepository.findByStackId(stackId).stream().map(FreeIpaStackRecipe::getRecipe).collect(Collectors.toSet());
    }

    public List<RecipeModel> getRecipes(Long stackId) {
        Set<String> recipes = getRecipeNamesForStack(stackId);
        LOGGER.info("Get recipes from core: {}", recipes);
        try {
            if (!recipes.isEmpty()) {
                Set<RecipeV4Request> recipesByNames = recipeV4Endpoint.getRequestsByNames(0L, recipes);
                return recipesByNames.stream().map(recipe ->
                                new RecipeModel(recipe.getName(), recipeType(recipe.getType()), new String(Base64.decodeBase64(recipe.getContent()))))
                        .collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }
        } catch (NotFoundException e) {
            String errorMessage;
            try {
                errorMessage = e.getResponse().readEntity(ExceptionResponse.class).getMessage();
                LOGGER.error("Missing recipe(s): {}", errorMessage);
            } catch (Exception exception) {
                LOGGER.error("Missing recipe(s), can't parse into ExceptionResponse entity", e);
                errorMessage = exception.getMessage();
            }
            throw new CloudbreakServiceException(String.format("Missing recipe(s): %s", errorMessage));
        }
    }

    public void saveRecipes(Set<String> recipes, Long stackId) {
        if (recipes != null) {
            freeIpaStackRecipeRepository.saveAll(recipes.stream().map(recipe -> new FreeIpaStackRecipe(stackId, recipe)).collect(Collectors.toSet()));
        }
    }

    public void deleteRecipes(Long stackId) {
        freeIpaStackRecipeRepository.deleteFreeIpaStackRecipesByStackId(stackId);
    }

    @Override
    public AuthorizationResourceType getSupportedAuthorizationResourceType() {
        return RECIPE;
    }

    private RecipeType recipeType(RecipeV4Type recipeType) {
        if (recipeType.equals(RecipeV4Type.POST_AMBARI_START)) {
            return RecipeType.POST_CLOUDERA_MANAGER_START;
        } else if (recipeType.equals(RecipeV4Type.PRE_AMBARI_START)) {
            return RecipeType.PRE_CLOUDERA_MANAGER_START;
        }
        return RecipeType.valueOf(recipeType.name());
    }

}
