package com.sequenceiq.cloudbreak.service.recipe;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.UpdateHostGroupRecipes;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.AttachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.DetachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.UpdateRecipesV4Response;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;

@Service
public class UpdateRecipeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateRecipeService.class);

    private final RecipeService recipeService;

    private final HostGroupService hostGroupService;

    public UpdateRecipeService(RecipeService recipeService, HostGroupService hostGroupService) {
        this.recipeService = recipeService;
        this.hostGroupService = hostGroupService;
    }

    /**
     * Updating recipes for an existing cluster. The input should contain host group - recipes mapping
     * If a host group key from the mappings is missing from the input, that is not going to be updated.
     * (or both - that is the default). Output is the newly attached/detached recipes in db.
     */
    public UpdateRecipesV4Response refreshRecipesForCluster(Long workspaceId, Stack stack, List<UpdateHostGroupRecipes> recipesPerHostGroup) {
        Set<String> recipesToFind = recipesPerHostGroup.stream().flatMap(rphg -> rphg.getRecipeNames().stream())
                .collect(Collectors.toSet());
        Map<String, Set<String>> recipesToUpdate = recipesPerHostGroup.stream()
                .collect(Collectors.toMap(UpdateHostGroupRecipes::getHostGroupName, UpdateHostGroupRecipes::getRecipeNames, (n1, n2) -> n1));
        LOGGER.debug("Update recipes {}", recipesToUpdate);
        Set<Recipe> recipes = recipeService.getByNamesForWorkspaceId(recipesToFind, workspaceId);
        validate(recipesToFind, recipes);
        Set<HostGroup> hostGroups = hostGroupService.getByClusterWithRecipes(stack.getCluster().getId());
        UpdateRecipesV4Response result = updateRecipesForHostGroups(recipesToUpdate, recipes, hostGroups);
        LOGGER.debug("Update recipes result: {}", result);
        return result;
    }

    private UpdateRecipesV4Response updateRecipesForHostGroups(Map<String, Set<String>> recipesToUpdate, Set<Recipe> recipes, Set<HostGroup> hostGroups) {
        UpdateRecipesV4Response result = new UpdateRecipesV4Response();
        for (HostGroup hostGroup : hostGroups) {
            UpdateHostGroupRecipesPair updatePairs = doHostGroupRecipeUpdate(recipesToUpdate, recipes, hostGroup);
            updatePairs.getRecipesToAttach().ifPresent(a -> result.getRecipesAttached().add(a));
            updatePairs.getRecipesToDetach().ifPresent(d -> result.getRecipesDetached().add(d));
        }
        return result;
    }

    private UpdateHostGroupRecipesPair doHostGroupRecipeUpdate(Map<String, Set<String>> recipesToUpdate, Set<Recipe> recipes, HostGroup hostGroup) {
        String hostGroupName = hostGroup.getName();
        Optional<UpdateHostGroupRecipes> attachHostGroupRecipesOpt = Optional.empty();
        Optional<UpdateHostGroupRecipes> detachHostGroupRecipesOpt = Optional.empty();
        if (recipesToUpdate.containsKey(hostGroupName)) {
            LOGGER.debug("Checking host group '{}' needs any refresh.", hostGroupName);
            Set<String> recipesForHostGroup = recipesToUpdate.get(hostGroupName);
            Set<Recipe> existingRecipes = hostGroup.getRecipes();
            Set<String> existingRecipeNames = existingRecipes
                    .stream().map(Recipe::getName)
                    .collect(Collectors.toSet());
            LOGGER.debug("Current recipes: {}", existingRecipes);
            boolean allExists = existingRecipes.stream().allMatch(r -> recipesForHostGroup.contains(r.getName()));
            if (allExists && recipesForHostGroup.size() == existingRecipes.size()) {
                LOGGER.debug("No need for any recipe update for '{}' host group", hostGroupName);
            } else {
                Set<Recipe> updateRecipes = recipes.stream()
                        .filter(r -> recipesForHostGroup.contains(r.getName()))
                        .collect(Collectors.toSet());
                attachHostGroupRecipesOpt = collectAttachHostGroupRecipes(hostGroupName, existingRecipeNames, updateRecipes);
                detachHostGroupRecipesOpt = collectDetachHostGroupRecipes(hostGroupName, recipesForHostGroup, existingRecipes);
                hostGroup.setRecipes(updateRecipes);
                hostGroupService.save(hostGroup);
            }
        }
        return new UpdateHostGroupRecipesPair(attachHostGroupRecipesOpt.orElse(null), detachHostGroupRecipesOpt.orElse(null));
    }

    private Optional<UpdateHostGroupRecipes> collectAttachHostGroupRecipes(String hostGroupName, Set<String> existingRecipeNames, Set<Recipe> updateRecipes) {
        Set<String> recipesToAddToHostGroup = updateRecipes.stream()
                .map(Recipe::getName)
                .filter(r -> !existingRecipeNames.contains(r))
                .collect(Collectors.toSet());
        Optional<UpdateHostGroupRecipes> result = Optional.empty();
        if (!recipesToAddToHostGroup.isEmpty()) {
            UpdateHostGroupRecipes attachHostGroupRecipes = new UpdateHostGroupRecipes();
            attachHostGroupRecipes.setHostGroupName(hostGroupName);
            attachHostGroupRecipes.setRecipeNames(recipesToAddToHostGroup);
            result = Optional.of(attachHostGroupRecipes);
        }
        return result;
    }

    private Optional<UpdateHostGroupRecipes> collectDetachHostGroupRecipes(String hostGroupName, Set<String> recipesForHostGroup, Set<Recipe> existingRecipes) {
        Set<String> recipesToDeleteFromHostGroup = existingRecipes.stream()
                .map(Recipe::getName)
                .filter(name -> !recipesForHostGroup.contains(name))
                .collect(Collectors.toSet());
        Optional<UpdateHostGroupRecipes> result = Optional.empty();
        if (!recipesToDeleteFromHostGroup.isEmpty()) {
            UpdateHostGroupRecipes detachHostGroupRecipes = new UpdateHostGroupRecipes();
            detachHostGroupRecipes.setHostGroupName(hostGroupName);
            detachHostGroupRecipes.setRecipeNames(recipesToDeleteFromHostGroup);
            result = Optional.of(detachHostGroupRecipes);
        }
        return result;
    }

    public AttachRecipeV4Response attachRecipeToCluster(Long workspaceId, Stack stack, String recipeName, String hostGroupName) {
        updateRecipeForCluster(workspaceId, stack, recipeName, hostGroupName, false);
        AttachRecipeV4Response response = new AttachRecipeV4Response();
        response.setRecipeName(recipeName);
        response.setHostGroupName(hostGroupName);
        return response;
    }

    public DetachRecipeV4Response detachRecipeFromCluster(Long workspaceId, Stack stack, String recipeName, String hostGroupName) {
        updateRecipeForCluster(workspaceId, stack, recipeName, hostGroupName, true);
        DetachRecipeV4Response response = new DetachRecipeV4Response();
        response.setRecipeName(recipeName);
        response.setHostGroupName(hostGroupName);
        return response;
    }

    private void updateRecipeForCluster(Long workspaceId, Stack stack, String recipeName, String hostGroupName, boolean detach) {
        Recipe recipe = recipeService.getByNameForWorkspaceId(recipeName, workspaceId);
        HostGroup hostGroup = hostGroupService.getByClusterIdAndNameWithRecipes(stack.getCluster().getId(), hostGroupName);
        if (hostGroup == null) {
            throw new NotFoundException(String.format("Host group '%s' not found for workspace", hostGroupName));
        }
        Set<Recipe> existingRecipes = hostGroup.getRecipes();
        Set<String> existingRecipeNames = existingRecipes
                .stream().map(Recipe::getName)
                .collect(Collectors.toSet());
        if (detach) {
            detachRecipeFromHostGroup(recipe, hostGroup, existingRecipeNames);
        } else {
            attachRecipeToHostGroup(recipe, hostGroup, existingRecipeNames);
        }
    }

    private void attachRecipeToHostGroup(Recipe recipe, HostGroup hostGroup, Set<String> recipeNames) {
        String recipeName = recipe.getName();
        String hostGroupName = hostGroup.getName();
        if (recipeNames.contains(recipeName)) {
            LOGGER.debug("Recipe {} already attached to host group {}. ", recipeName, hostGroupName);
        } else {
            hostGroup.getRecipes().add(recipe);
            hostGroupService.save(hostGroup);
        }
    }

    private void detachRecipeFromHostGroup(Recipe recipe, HostGroup hostGroup, Set<String> recipeNames) {
        String recipeName = recipe.getName();
        String hostGroupName = hostGroup.getName();
        if (recipeNames.contains(recipeName)) {
            hostGroup.setRecipes(
                    hostGroup.getRecipes().stream()
                            .filter(r -> !r.getName().equals(recipeName))
                            .collect(Collectors.toSet())
            );
            hostGroupService.save(hostGroup);
        } else {
            LOGGER.debug("Recipe {} already detached from host group {}. ", recipeName, hostGroupName);
        }
    }

    private void validate(Set<String> recipesToFind, Set<Recipe> recipes) {
        Set<String> existingRecipesNames = recipes
                .stream().map(Recipe::getName).collect(Collectors.toSet());
        LOGGER.debug("Recipes found in database: {}, expected update related recipes: {}", existingRecipesNames, recipesToFind);
        Set<String> invalidRecipes = recipesToFind.stream().filter(r -> !existingRecipesNames.contains(r))
                .collect(Collectors.toSet());
        if (!invalidRecipes.isEmpty()) {
            throw new BadRequestException(String.format("Following recipes do not exist in workspace: %s", Joiner.on(",").join(invalidRecipes)));
        }
    }
}
