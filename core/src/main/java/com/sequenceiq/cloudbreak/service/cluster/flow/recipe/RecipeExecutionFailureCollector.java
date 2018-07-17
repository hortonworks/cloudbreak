package com.sequenceiq.cloudbreak.service.cluster.flow.recipe;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;

@Component
public class RecipeExecutionFailureCollector {

    public boolean canProcessExecutionFailure(Exception e) {
        if (e.getCause() != null && e.getCause() instanceof CloudbreakOrchestratorException) {
            return !((CloudbreakOrchestratorException) e.getCause()).getNodesWithErrors().isEmpty();
        } else if (e.getCause() != null && e.getCause().getCause() != null && e.getCause().getCause() instanceof CloudbreakOrchestratorException) {
            return !((CloudbreakOrchestratorException) e.getCause().getCause()).getNodesWithErrors().isEmpty();
        }
        return false;
    }

    public Set<RecipeExecutionFailure> collectErrors(CloudbreakOrchestratorException exception,
            Map<HostGroup, List<RecipeModel>> hostgroupToRecipeMap, Set<InstanceGroup> instanceGroups) {

        return exception.getNodesWithErrors().asMap().entrySet().stream().flatMap((Entry<String, Collection<String>> nodeWithErrors) -> {
            Map<String, Optional<String>> errorsWithPhase = nodeWithErrors.getValue().stream().collect(Collectors.toMap(
                    e -> e,
                    this::getInstallPhase,
                    (phase1, phase2) -> phase1.map(Optional::of).orElse(phase2)));
            return errorsWithPhase.entrySet().stream()
                    .filter(errorWithPhase -> errorWithPhase.getValue().isPresent())
                    .map((Entry<String, Optional<String>> errorWithPhase) ->
                            StringUtils.substringBetween(errorWithPhase.getKey(), "sh -x /opt/scripts/" + errorWithPhase.getValue().get() + '/', " 2>&1 |")
                    )
                    .collect(Collectors.toMap(
                            recipeName -> recipeName,
                            recipeName -> getPossibleFailingHostgroupsByRecipeName(hostgroupToRecipeMap, recipeName),
                            (hg1, hg2) -> Stream.concat(hg1.stream(), hg2.stream()).collect(Collectors.toSet()))
                    )
                    .entrySet().stream()
                    .flatMap((Entry<String, Set<String>> recipeWithHostgroups) -> recipeWithHostgroups.getValue().stream()
                            .map((String hostGroup) -> {
                                Optional<Recipe> recipe = getRecipeOfHostgroupByRecipeName(hostgroupToRecipeMap, recipeWithHostgroups.getKey(), hostGroup);
                                Optional<InstanceMetaData> instanceMetaData = getInstanceMetaDataByHost(instanceGroups, nodeWithErrors.getKey(), hostGroup);
                                if (recipe.isPresent() && instanceMetaData.isPresent()) {
                                    return new RecipeExecutionFailure(recipe.get(), instanceMetaData.get());
                                }
                                return null;
                            })
                            .filter(Objects::nonNull)
                    );
        }).collect(Collectors.toSet());
    }

    private Optional<String> getInstallPhase(String message) {
        if (message.contains("pre-termination")) {
            return Optional.of("pre-termination");
        } else if (message.contains("post-cluster-install")) {
            return Optional.of("post-cluster-install");
        } else if (message.contains("post-ambari-start")) {
            return Optional.of("post-ambari-start");
        } else if (message.contains("pre-ambari-start")) {
            return Optional.of("pre-ambari-start");
        } else if (message.contains("post")) {
            return Optional.of("post");
        } else if (message.contains("pre")) {
            return Optional.of("pre");
        }
        return Optional.empty();
    }

    private Set<String> getPossibleFailingHostgroupsByRecipeName(Map<HostGroup, List<RecipeModel>> hostgroupToRecipeMap, String recipeName) {
        return hostgroupToRecipeMap.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(recipe -> recipe.getName().equals(recipeName)))
                .map(Entry::getKey)
                .map(HostGroup::getName)
                .collect(Collectors.toSet());
    }

    private Optional<Recipe> getRecipeOfHostgroupByRecipeName(Map<HostGroup, List<RecipeModel>> hostgroupToRecipeMap, String recipeName, String hostGroupName) {
        return hostgroupToRecipeMap.keySet().stream()
                .filter(hg -> hg.getName().equals(hostGroupName)).findFirst()
                .flatMap(hostGroup -> hostGroup.getRecipes().stream().filter(r -> r.getName().equals(recipeName)).findFirst());
    }

    private Optional<InstanceMetaData> getInstanceMetaDataByHost(Set<InstanceGroup> instanceGroups, String host, String hostGroupName) {
        return instanceGroups.stream()
                .filter(ig -> ig.getGroupName().equals(hostGroupName)).findFirst()
                .flatMap(instanceGroup -> instanceGroup.getNotDeletedInstanceMetaDataSet().stream()
                        .filter(imd -> imd.getDiscoveryFQDN().equals(host)).findFirst());
    }

    public static class RecipeExecutionFailure {

        private final Recipe recipe;

        private final InstanceMetaData instanceMetaData;

        public RecipeExecutionFailure(Recipe recipe, InstanceMetaData instanceMetaData) {
            this.recipe = recipe;
            this.instanceMetaData = instanceMetaData;
        }

        public Recipe getRecipe() {
            return recipe;
        }

        public InstanceMetaData getInstanceMetaData() {
            return instanceMetaData;
        }
    }
}
