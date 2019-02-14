package com.sequenceiq.cloudbreak.init.clusterdefinition;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;

@Service
public class ClusterDefinitionLoaderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDefinitionLoaderService.class);

    @Inject
    private DefaultAmbariBlueprintCache defaultAmbariBlueprintCache;

    public boolean addingDefaultBlueprintsAreNecessaryForTheUser(Collection<ClusterDefinition> clusterDefinitions) {
        Map<String, ClusterDefinition> defaultBlueprints = defaultAmbariBlueprintCache.defaultBlueprints();
        for (ClusterDefinition clusterDefinitionFromDatabase : clusterDefinitions) {
            ClusterDefinition defaultClusterDefinition = defaultBlueprints.get(clusterDefinitionFromDatabase.getName());
            if (mustUpdateTheExistingBlueprint(clusterDefinitionFromDatabase, defaultClusterDefinition)) {
                return true;
            }
        }
        if (isNewUserOrDeletedEveryDefaultBlueprint(clusterDefinitions)) {
            return true;
        }
        return defaultBlueprintDoesNotExistInTheDatabase(clusterDefinitions);
    }

    public Set<ClusterDefinition> loadBlueprintsForTheWorkspace(Set<ClusterDefinition> clusterDefinitions, Workspace workspace,
            BiFunction<Iterable<ClusterDefinition>, Workspace, Iterable<ClusterDefinition>> saveMethod) {
        Set<ClusterDefinition> clusterDefinitionsWhichShouldBeUpdate = updateDefaultBlueprints(clusterDefinitions, workspace);
        Set<ClusterDefinition> clusterDefinitionsWhichAreMissing = addMissingBlueprints(clusterDefinitions, workspace);
        try {
            clusterDefinitionsWhichAreMissing.addAll(clusterDefinitionsWhichShouldBeUpdate);
            if (!clusterDefinitionsWhichAreMissing.isEmpty()) {
                return Sets.newHashSet(getResultSetFromUpdateAndOriginalBlueprints(clusterDefinitions, clusterDefinitionsWhichAreMissing, workspace,
                        saveMethod));
            }
        } catch (Exception e) {
            LOGGER.info("Blueprints {} is not available for {} workspace.", collectNames(clusterDefinitionsWhichAreMissing), workspace.getId());
        }
        return clusterDefinitions;
    }

    private Iterable<ClusterDefinition> getResultSetFromUpdateAndOriginalBlueprints(Iterable<ClusterDefinition> clusterDefinitions,
            Iterable<ClusterDefinition> clusterDefinitionsWhichAreMissing, Workspace workspace, BiFunction<Iterable<ClusterDefinition>, Workspace,
            Iterable<ClusterDefinition>> saveMethod) {
        LOGGER.debug("Updating cluster definitions which should be modified.");
        Iterable<ClusterDefinition> savedBlueprints = saveMethod.apply(clusterDefinitionsWhichAreMissing, workspace);
        LOGGER.debug("Finished to update cluster definitions which should be modified.");
        Map<String, ClusterDefinition> resultBlueprints = new HashMap<>();
        for (ClusterDefinition clusterDefinition : clusterDefinitions) {
            resultBlueprints.put(clusterDefinition.getName(), clusterDefinition);
        }
        for (ClusterDefinition savedClusterDefinition : savedBlueprints) {
            resultBlueprints.put(savedClusterDefinition.getName(), savedClusterDefinition);
        }
        return resultBlueprints.values();
    }

    private Set<String> collectNames(Collection<ClusterDefinition> failedToUpdate) {
        return failedToUpdate.stream().map(ClusterDefinition::getName).collect(Collectors.toSet());
    }

    private Set<ClusterDefinition> addMissingBlueprints(Iterable<ClusterDefinition> clusterDefinitions, Workspace workspace) {
        Set<ClusterDefinition> resultList = new HashSet<>();
        LOGGER.debug("Adding default cluster definitions which are missing for the user.");
        for (Entry<String, ClusterDefinition> diffBlueprint : collectDeviationOfExistingBlueprintsAndDefaultBlueprints(clusterDefinitions).entrySet()) {
            LOGGER.debug("Default Blueprint '{}' needs to be added for the '{}' workspace because the default validation missing.",
                    diffBlueprint.getKey(), workspace.getId());
            resultList.add(setupBlueprint(diffBlueprint.getValue(), workspace));
        }
        LOGGER.debug("Finished to add default cluster definitions which are missing for the user.");
        return resultList;
    }

    private Set<ClusterDefinition> updateDefaultBlueprints(Iterable<ClusterDefinition> clusterDefinitions, Workspace workspace) {
        Set<ClusterDefinition> resultList = new HashSet<>();
        LOGGER.debug("Updating default cluster definitions which are contains text modifications.");
        Map<String, ClusterDefinition> defaultBlueprints = defaultAmbariBlueprintCache.defaultBlueprints();
        for (ClusterDefinition clusterDefinitionFromDatabase : clusterDefinitions) {
            ClusterDefinition defaultClusterDefinition = defaultBlueprints.get(clusterDefinitionFromDatabase.getName());
            if (defaultBlueprintExistInTheCache(defaultClusterDefinition)
                    && (defaultBlueprintNotSameAsNewTexts(clusterDefinitionFromDatabase, defaultClusterDefinition.getClusterDefinitionText())
                    || defaultBlueprintContainsNewDescription(clusterDefinitionFromDatabase, defaultClusterDefinition))) {
                LOGGER.debug("Default cluster definition '{}' needs to modify for the '{}' workspace because the validation text changed.",
                        clusterDefinitionFromDatabase.getName(), workspace.getId());
                resultList.add(prepareBlueprint(clusterDefinitionFromDatabase, defaultClusterDefinition, workspace));
            }
        }
        LOGGER.debug("Finished to Update default cluster definitions which are contains text modifications.");
        return resultList;
    }

    private ClusterDefinition prepareBlueprint(ClusterDefinition clusterDefinitionFromDatabase, ClusterDefinition newClusterDefinition, Workspace workspace) {
        setupBlueprint(clusterDefinitionFromDatabase, workspace);
        clusterDefinitionFromDatabase.setClusterDefinitionText(newClusterDefinition.getClusterDefinitionText());
        clusterDefinitionFromDatabase.setDescription(newClusterDefinition.getDescription());
        clusterDefinitionFromDatabase.setHostGroupCount(newClusterDefinition.getHostGroupCount());
        clusterDefinitionFromDatabase.setStackName(newClusterDefinition.getStackName());
        clusterDefinitionFromDatabase.setStackType(newClusterDefinition.getStackType());
        clusterDefinitionFromDatabase.setStackVersion(newClusterDefinition.getStackVersion());
        return clusterDefinitionFromDatabase;
    }

    private ClusterDefinition setupBlueprint(ClusterDefinition clusterDefinition, Workspace workspace) {
        clusterDefinition.setWorkspace(workspace);
        clusterDefinition.setStatus(DEFAULT);
        return clusterDefinition;
    }

    private Map<String, ClusterDefinition> collectDeviationOfExistingBlueprintsAndDefaultBlueprints(Iterable<ClusterDefinition> clusterDefinitions) {
        LOGGER.debug("Collecting Blueprints which are missing from the defaults.");
        Map<String, ClusterDefinition> diff = new HashMap<>();
        for (Entry<String, ClusterDefinition> stringBlueprintEntry : defaultAmbariBlueprintCache.defaultBlueprints().entrySet()) {
            boolean contains = false;
            for (ClusterDefinition clusterDefinition : clusterDefinitions) {
                if (isRegisteregBlueprintAndDefaultBlueprintIsTheSame(stringBlueprintEntry, clusterDefinition)) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                diff.put(stringBlueprintEntry.getKey(), stringBlueprintEntry.getValue());
            }
        }
        LOGGER.debug("Finished to collect the default cluster definitions which are missing: {}.", diff);
        return diff;
    }

    private boolean isRegisteregBlueprintAndDefaultBlueprintIsTheSame(Entry<String, ClusterDefinition> stringBlueprintEntry,
            ClusterDefinition clusterDefinition) {
        return clusterDefinition.getName().equals(stringBlueprintEntry.getKey()) && isDefaultBlueprint(clusterDefinition);
    }

    private boolean isDefaultBlueprint(ClusterDefinition bp) {
        return DEFAULT.equals(bp.getStatus());
    }

    private boolean defaultBlueprintNotSameAsNewTexts(ClusterDefinition clusterDefinitionFromDatabase, String defaultBlueprintText) {
        String clusterDefinitionText = clusterDefinitionFromDatabase.getClusterDefinitionText();
        return clusterDefinitionText == null || !clusterDefinitionText.equals(defaultBlueprintText);
    }

    private boolean defaultBlueprintContainsNewDescription(ClusterDefinition bp, ClusterDefinition clusterDefinition) {
        return !bp.getDescription().equals(clusterDefinition.getDescription());
    }

    private boolean isNewUserOrDeletedEveryDefaultBlueprint(Collection<ClusterDefinition> clusterDefinitions) {
        return clusterDefinitions.isEmpty();
    }

    private boolean mustUpdateTheExistingBlueprint(ClusterDefinition clusterDefinitionFromDatabase, ClusterDefinition defaultClusterDefinition) {
        return isDefaultBlueprint(clusterDefinitionFromDatabase)
                && defaultBlueprintExistInTheCache(defaultClusterDefinition)
                && (defaultBlueprintNotSameAsNewTexts(clusterDefinitionFromDatabase, defaultClusterDefinition.getClusterDefinitionText())
                || defaultBlueprintContainsNewDescription(clusterDefinitionFromDatabase, defaultClusterDefinition));
    }

    private boolean defaultBlueprintDoesNotExistInTheDatabase(Iterable<ClusterDefinition> clusterDefinitions) {
        return !collectDeviationOfExistingBlueprintsAndDefaultBlueprints(clusterDefinitions).isEmpty();
    }

    private boolean defaultBlueprintExistInTheCache(ClusterDefinition actualDefaultClusterDefinition) {
        return actualDefaultClusterDefinition != null;
    }
}
