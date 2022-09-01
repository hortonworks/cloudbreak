package com.sequenceiq.freeipa.service.recipe;

import static com.sequenceiq.authorization.resource.AuthorizationResourceType.RECIPE;

import java.util.Arrays;
import java.util.Collection;
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
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;
import com.sequenceiq.cloudbreak.recipe.RecipeCrnListProviderService;
import com.sequenceiq.freeipa.api.v1.recipe.model.RecipeAttachDetachRequest;
import com.sequenceiq.freeipa.entity.FreeIpaStackRecipe;
import com.sequenceiq.freeipa.repository.FreeIpaStackRecipeRepository;
import com.sequenceiq.freeipa.repository.StackRepository;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class FreeIpaRecipeService implements AuthorizationResourceCrnListProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaRecipeService.class);

    @Inject
    private RecipeV4Endpoint recipeV4Endpoint;

    @Inject
    private FreeIpaStackRecipeRepository freeIpaStackRecipeRepository;

    @Inject
    private RecipeCrnListProviderService recipeCrnListProviderService;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private StackService stackService;

    @Override
    public List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
        return recipeCrnListProviderService.getResourceCrnListByResourceNameList(resourceNames);
    }

    public List<String> getUsedRecipeNamesForAccount(String accountId) {
        List<Long> stacksForAccount = stackRepository.findStackIdsByAccountId(accountId);
        return freeIpaStackRecipeRepository.findByStackIdIn(stacksForAccount).stream().map(FreeIpaStackRecipe::getRecipe).collect(Collectors.toList());
    }

    public Set<String> getRecipeNamesForStack(Long stackId) {
        return freeIpaStackRecipeRepository.findByStackId(stackId).stream().map(FreeIpaStackRecipe::getRecipe).collect(Collectors.toSet());
    }

    public boolean hasRecipeType(List<RecipeModel> recipeModelList, RecipeType... recipeTypes) {
        return recipeModelList.stream().anyMatch(recipeModel -> Arrays.stream(recipeTypes)
                .anyMatch(recipeType -> recipeType.equals(recipeModel.getRecipeType())));
    }

    public boolean hasRecipeType(Long stackId, RecipeType... recipeTypes) {
        return hasRecipeType(getRecipes(stackId), recipeTypes);
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

    public void saveRecipes(Collection<String> recipes, Long stackId) {
        if (recipes != null) {
            freeIpaStackRecipeRepository.saveAll(recipes.stream().map(recipe -> new FreeIpaStackRecipe(stackId, recipe)).collect(Collectors.toSet()));
        }
    }

    public void deleteRecipes(Long stackId) {
        freeIpaStackRecipeRepository.deleteFreeIpaStackRecipesByStackId(stackId);
    }

    public void attachRecipes(String accountId, RecipeAttachDetachRequest recipeAttach) {
        String environmentCrn = recipeAttach.getEnvironmentCrn();
        Long stackId = stackService.getIdByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        List<String> newRecipes = recipeAttach.getRecipes();
        LOGGER.info("Attach {} recipes, env crn: {}", newRecipes, environmentCrn);
        recipeCrnListProviderService.validateRequestedRecipesExistsByName(newRecipes);
        Set<String> existingRecipes = getRecipeNamesForStack(stackId);
        List<String> recipesToSave = newRecipes.stream().filter(newRecipe -> !existingRecipes.contains(newRecipe)).collect(Collectors.toList());
        saveRecipes(recipesToSave, stackId);
    }

    public void detachRecipes(String accountId, RecipeAttachDetachRequest recipeDetach) {
        String environmentCrn = recipeDetach.getEnvironmentCrn();
        Long stackId = stackService.getIdByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        Collection<String> existingRecipes = getRecipeNamesForStack(stackId);
        List<String> recipesToRemove = recipeDetach.getRecipes();
        LOGGER.info("Detach {} recipes, env crn: {}", recipesToRemove, environmentCrn);
        List<String> notAttachedRecipes = recipesToRemove.stream().filter(recipe -> !existingRecipes.contains(recipe)).collect(Collectors.toList());
        if (!notAttachedRecipes.isEmpty()) {
            LOGGER.info("{} recipes are not attached to freeipa, env crn: {}", notAttachedRecipes, environmentCrn);
            throw new BadRequestException(String.join(", ", notAttachedRecipes) + " recipe(s) are not attached to freeipa stack!");
        }
        freeIpaStackRecipeRepository.deleteFreeIpaStackRecipeByStackIdAndRecipeIn(stackId, recipesToRemove);
    }

    @Override
    public AuthorizationResourceType getSupportedAuthorizationResourceType() {
        return RECIPE;
    }

    private RecipeType recipeType(RecipeV4Type recipeType) {
        if (recipeType.equals(RecipeV4Type.POST_AMBARI_START)) {
            return RecipeType.POST_CLOUDERA_MANAGER_START;
        } else if (recipeType.equals(RecipeV4Type.PRE_AMBARI_START)) {
            return RecipeType.PRE_SERVICE_DEPLOYMENT;
        } else if (recipeType.equals(RecipeV4Type.POST_CLUSTER_INSTALL)) {
            return RecipeType.POST_SERVICE_DEPLOYMENT;
        } else if (recipeType.equals(RecipeV4Type.PRE_CLOUDERA_MANAGER_START)) {
            return RecipeType.PRE_SERVICE_DEPLOYMENT;
        }
        return RecipeType.valueOf(recipeType.name());
    }

}
