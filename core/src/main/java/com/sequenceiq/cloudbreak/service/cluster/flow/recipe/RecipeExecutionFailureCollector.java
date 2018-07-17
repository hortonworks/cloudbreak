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

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

@Component
public class RecipeExecutionFailureCollector {

    public boolean canProcessExecutionFailure(Exception e) {
        return getNodesWithErrors(e).isPresent();
    }

    public Set<RecipeExecutionFailure> collectErrors(CloudbreakOrchestratorException exception,
            Map<HostGroup, List<RecipeModel>> hostgroupToRecipeMap, Set<InstanceGroup> instanceGroups) {

        if (!getNodesWithErrors(exception).isPresent()) {
            throw new CloudbreakServiceException("Failed to collect recipe execution failures. Cause exception contains no information.", exception);
        }

        return getNodesWithErrors(exception).get().asMap().entrySet().stream().flatMap((Entry<String, Collection<String>> nodeWithErrors) -> {
            Map<String, Optional<String>> errorsWithPhase = nodeWithErrors.getValue().stream().collect(Collectors.toMap(
                    error -> error,
                    this::getInstallPhase,
                    (phase1, phase2) -> phase1.map(Optional::of).orElse(phase2)));
            return errorsWithPhase.entrySet().stream()
                    .filter(errorWithPhase -> errorWithPhase.getValue().isPresent())
                    .map(this::errorWithPhaseToRecipeName)
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

    private Optional<Multimap<String, String>> getNodesWithErrors(Throwable throwable) {
        if (throwable instanceof CloudbreakOrchestratorException) {
            CloudbreakOrchestratorException exception = (CloudbreakOrchestratorException) throwable;
            if (exception.getNodesWithErrors().isEmpty()) {
                if (throwable.getCause() != null) {
                    return getNodesWithErrors(throwable.getCause());
                } else {
                    return Optional.empty();
                }
            }
            return Optional.of(exception.getNodesWithErrors());
        } else {
            return getNodesWithErrors(throwable.getCause());
        }
    }

    private Optional<String> getInstallPhase(String message) {
        String phaseString = StringUtils.substringAfter(message, "/opt/scripts/recipe-runner.sh ");
        if (phaseString.startsWith("pre-termination")) {
            return Optional.of("pre-termination");
        } else if (phaseString.startsWith("post-cluster-install")) {
            return Optional.of("post-cluster-install");
        } else if (phaseString.startsWith("post-ambari-start")) {
            return Optional.of("post-ambari-start");
        } else if (phaseString.startsWith("pre-ambari-start")) {
            return Optional.of("pre-ambari-start");
        } else if (phaseString.startsWith("post")) {
            return Optional.of("post");
        } else if (phaseString.startsWith("pre")) {
            return Optional.of("pre");
        }
        return Optional.empty();
    }

    private String errorWithPhaseToRecipeName(Entry<String, Optional<String>> errorWithPhase) {
        return StringUtils.substringBetween(errorWithPhase.getKey(), ' ' + errorWithPhase.getValue().get() + ' ', "\" run");
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
