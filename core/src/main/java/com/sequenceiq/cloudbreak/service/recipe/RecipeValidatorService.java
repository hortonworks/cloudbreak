package com.sequenceiq.cloudbreak.service.recipe;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.SetUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@Service
public class RecipeValidatorService {

    @Inject
    private RecipeService recipeService;

    public void validateRecipeExistenceOnInstanceGroups(List<InstanceGroupV4Request> instanceGroupV4Requests, Long workspaceId) {
        Set<String> uniqueRecipeNames = new HashSet<>();
        instanceGroupV4Requests.forEach(instanceGroupV4Request -> uniqueRecipeNames.addAll(getRecipeNamesFromGroup(instanceGroupV4Request)));
        Set<String> namesInTheDb = recipeService.getResourceNamesByNamesAndWorkspaceId(List.copyOf(uniqueRecipeNames), workspaceId);
        if (namesInTheDb.size() != uniqueRecipeNames.size()) {
            instanceGroupV4Requests.forEach(instanceGroupV4Request -> {
                Set<String> recipeNamesInGroup = getRecipeNamesFromGroup(instanceGroupV4Request);
                Set<String> missingRecipes = SetUtils.difference(recipeNamesInGroup, namesInTheDb);
                throwBadRequestIfHaveMissingRecipe(missingRecipes, instanceGroupV4Request.getName());
            });
        }
    }

    private void throwBadRequestIfHaveMissingRecipe(Set<String> missingRecipes, final String instanceGroupName) {
        if (!missingRecipes.isEmpty()) {
            if (missingRecipes.size() > 1) {
                throw new BadRequestException(String.format("The given recipes do not exist for the instance group \"%s\": %s",
                        instanceGroupName, String.join(", ", missingRecipes.stream().sorted().collect(Collectors.toList()))));
            } else {
                throw new BadRequestException(String.format("The given recipe does not exist for the instance group \"%s\": %s",
                        instanceGroupName, missingRecipes.stream().findFirst().get()));
            }
        }
    }

    private Set<String> getRecipeNamesFromGroup(InstanceGroupV4Request instanceGroupV4Request) {
        return Optional.ofNullable(instanceGroupV4Request.getRecipeNames()).orElse(new HashSet<>());
    }
}
