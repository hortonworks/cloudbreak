package com.sequenceiq.datalake.service.sdx;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.CompactViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.AttachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.DetachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.UpdateRecipesV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.AttachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.DetachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.UpdateRecipesV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.sdx.api.model.SdxRecipe;

@Service
public class RecipeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeService.class);

    private static final long WORKSPACE_ID_DEFAULT = 0L;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private RecipeV4Endpoint recipeV4Endpoint;

    public UpdateRecipesV4Response refreshRecipes(SdxCluster sdxCluster, UpdateRecipesV4Request request) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> stackV4Endpoint.refreshRecipesInternal(WORKSPACE_ID_DEFAULT, request, sdxCluster.getName(), userCrn));
    }

    public AttachRecipeV4Response attachRecipe(SdxCluster sdxCluster, AttachRecipeV4Request request) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> stackV4Endpoint.attachRecipeInternal(WORKSPACE_ID_DEFAULT, request, sdxCluster.getName(), userCrn));
    }

    public DetachRecipeV4Response detachRecipe(SdxCluster sdxCluster, DetachRecipeV4Request request) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> stackV4Endpoint.detachRecipeInternal(WORKSPACE_ID_DEFAULT, request, sdxCluster.getName(), userCrn));
    }

    public void validateRecipes(Set<SdxRecipe> recipes, StackV4Request stackV4Request) {
        if (CollectionUtils.isNotEmpty(recipes)) {
            List<InstanceGroupV4Request> igs = stackV4Request.getInstanceGroups();
            Set<String> recipeNames = fetchRecipesFromCore();
            recipes.forEach(recipe -> {
                validateRecipeExists(recipeNames, recipe);
                validateInstanceGroupExistsForRecipe(igs, recipe);
            });
        }
    }

    private Set<String> fetchRecipesFromCore() {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        RecipeViewV4Responses recipeResponses = ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> recipeV4Endpoint.listInternal(
                        WORKSPACE_ID_DEFAULT, initiatorUserCrn));
        return recipeResponses.getResponses()
                .stream()
                .map(CompactViewV4Response::getName).collect(Collectors.toSet());
    }

    private void validateRecipeExists(Set<String> recipeNames, SdxRecipe recipe) {
        if (!recipeNames.contains(recipe.getName())) {
            throw new NotFoundException(String.format("Not found recipe with name %s", recipe.getName()));
        }
        LOGGER.debug("Found recipe with name {}", recipe.getName());
    }

    private void validateInstanceGroupExistsForRecipe(List<InstanceGroupV4Request> igs, SdxRecipe recipe) {
        boolean foundIg = igs.stream().anyMatch(ig -> recipe.getHostGroup().equals(ig.getName()));
        if (!foundIg) {
            throw new NotFoundException(String.format("Not found instance group with name %s for recipe %s",
                    recipe.getHostGroup(), recipe.getName()));
        }
    }
}