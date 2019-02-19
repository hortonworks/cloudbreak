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

    public boolean addingDefaultClusterDefinitionsAreNecessaryForTheUser(Collection<ClusterDefinition> clusterDefinitions) {
        Map<String, ClusterDefinition> defaultBlueprints = defaultAmbariBlueprintCache.defaultBlueprints();
        for (ClusterDefinition clusterDefinitionFromDatabase : clusterDefinitions) {
            ClusterDefinition defaultClusterDefinition = defaultBlueprints.get(clusterDefinitionFromDatabase.getName());
            if (mustUpdateTheExistingClusterDefinition(clusterDefinitionFromDatabase, defaultClusterDefinition)) {
                return true;
            }
        }
        if (isNewUserOrDeletedEveryDefaultClusterDefinition(clusterDefinitions)) {
            return true;
        }
        return defaultClusterDefinitionDoesNotExistInTheDatabase(clusterDefinitions);
    }

    public Set<ClusterDefinition> loadClusterDEfinitionsForTheWorkspace(Set<ClusterDefinition> clusterDefinitions, Workspace workspace,
            BiFunction<Iterable<ClusterDefinition>, Workspace, Iterable<ClusterDefinition>> saveMethod) {
        Set<ClusterDefinition> clusterDefinitionsWhichShouldBeUpdate = updateDefaultClusterDefinitions(clusterDefinitions, workspace);
        Set<ClusterDefinition> clusterDefinitionsWhichAreMissing = addMissingClusterDefinitions(clusterDefinitions, workspace);
        try {
            clusterDefinitionsWhichAreMissing.addAll(clusterDefinitionsWhichShouldBeUpdate);
            if (!clusterDefinitionsWhichAreMissing.isEmpty()) {
                return Sets.newHashSet(getResultSetFromUpdateAndOriginalClusterDefinitions(clusterDefinitions, clusterDefinitionsWhichAreMissing, workspace,
                        saveMethod));
            }
        } catch (Exception e) {
            LOGGER.info("Cluster definitions {} is not available for {} workspace.", collectNames(clusterDefinitionsWhichAreMissing), workspace.getId());
        }
        return clusterDefinitions;
    }

    private Iterable<ClusterDefinition> getResultSetFromUpdateAndOriginalClusterDefinitions(Iterable<ClusterDefinition> clusterDefinitions,
            Iterable<ClusterDefinition> clusterDefinitionsWhichAreMissing, Workspace workspace, BiFunction<Iterable<ClusterDefinition>, Workspace,
            Iterable<ClusterDefinition>> saveMethod) {
        LOGGER.debug("Updating cluster definitions which should be modified.");
        Iterable<ClusterDefinition> savedClusterDefinitions = saveMethod.apply(clusterDefinitionsWhichAreMissing, workspace);
        LOGGER.debug("Finished to update cluster definitions which should be modified.");
        Map<String, ClusterDefinition> resultClusterDefinitions = new HashMap<>();
        for (ClusterDefinition clusterDefinition : clusterDefinitions) {
            resultClusterDefinitions.put(clusterDefinition.getName(), clusterDefinition);
        }
        for (ClusterDefinition savedClusterDefinition : savedClusterDefinitions) {
            resultClusterDefinitions.put(savedClusterDefinition.getName(), savedClusterDefinition);
        }
        return resultClusterDefinitions.values();
    }

    private Set<String> collectNames(Collection<ClusterDefinition> failedToUpdate) {
        return failedToUpdate.stream().map(ClusterDefinition::getName).collect(Collectors.toSet());
    }

    private Set<ClusterDefinition> addMissingClusterDefinitions(Iterable<ClusterDefinition> clusterDefinitions, Workspace workspace) {
        Set<ClusterDefinition> resultList = new HashSet<>();
        LOGGER.debug("Adding default cluster definitions which are missing for the user.");
        for (Entry<String, ClusterDefinition> diffClusterDefinition : collectDeviationOfExistingAndDefaultClusterDefinitions(clusterDefinitions).entrySet()) {
            LOGGER.debug("Default cluster definition '{}' needs to be added for the '{}' workspace because the default validation missing.",
                    diffClusterDefinition.getKey(), workspace.getId());
            resultList.add(setupClusterDefinition(diffClusterDefinition.getValue(), workspace));
        }
        LOGGER.debug("Finished to add default cluster definitions which are missing for the user.");
        return resultList;
    }

    private Set<ClusterDefinition> updateDefaultClusterDefinitions(Iterable<ClusterDefinition> clusterDefinitions, Workspace workspace) {
        Set<ClusterDefinition> resultList = new HashSet<>();
        LOGGER.debug("Updating default cluster definitions which are contains text modifications.");
        Map<String, ClusterDefinition> defaultBlueprints = defaultAmbariBlueprintCache.defaultBlueprints();
        for (ClusterDefinition clusterDefinitionFromDatabase : clusterDefinitions) {
            ClusterDefinition defaultClusterDefinition = defaultBlueprints.get(clusterDefinitionFromDatabase.getName());
            if (defaultClusterDefinitionExistInTheCache(defaultClusterDefinition)
                    && (defaultClusterDefinitionNotSameAsNewTexts(clusterDefinitionFromDatabase, defaultClusterDefinition.getClusterDefinitionText())
                    || defaultClusterDefinitionContainsNewDescription(clusterDefinitionFromDatabase, defaultClusterDefinition))) {
                LOGGER.debug("Default cluster definition '{}' needs to modify for the '{}' workspace because the validation text changed.",
                        clusterDefinitionFromDatabase.getName(), workspace.getId());
                resultList.add(prepareClusterDefinition(clusterDefinitionFromDatabase, defaultClusterDefinition, workspace));
            }
        }
        LOGGER.debug("Finished to Update default cluster definitions which are contains text modifications.");
        return resultList;
    }

    private ClusterDefinition prepareClusterDefinition(ClusterDefinition clusterDefinitionFromDatabase, ClusterDefinition newClusterDefinition,
            Workspace workspace) {
        setupClusterDefinition(clusterDefinitionFromDatabase, workspace);
        clusterDefinitionFromDatabase.setClusterDefinitionText(newClusterDefinition.getClusterDefinitionText());
        clusterDefinitionFromDatabase.setDescription(newClusterDefinition.getDescription());
        clusterDefinitionFromDatabase.setHostGroupCount(newClusterDefinition.getHostGroupCount());
        clusterDefinitionFromDatabase.setStackName(newClusterDefinition.getStackName());
        clusterDefinitionFromDatabase.setStackType(newClusterDefinition.getStackType());
        clusterDefinitionFromDatabase.setStackVersion(newClusterDefinition.getStackVersion());
        return clusterDefinitionFromDatabase;
    }

    private ClusterDefinition setupClusterDefinition(ClusterDefinition clusterDefinition, Workspace workspace) {
        clusterDefinition.setWorkspace(workspace);
        clusterDefinition.setStatus(DEFAULT);
        return clusterDefinition;
    }

    private Map<String, ClusterDefinition> collectDeviationOfExistingAndDefaultClusterDefinitions(Iterable<ClusterDefinition> clusterDefinitions) {
        LOGGER.debug("Collecting cluster definitions which are missing from the defaults.");
        Map<String, ClusterDefinition> diff = new HashMap<>();
        for (Entry<String, ClusterDefinition> stringClusterDefinitionEntry : defaultAmbariBlueprintCache.defaultBlueprints().entrySet()) {
            boolean contains = false;
            for (ClusterDefinition clusterDefinition : clusterDefinitions) {
                if (isRegisteregClusterDefinitionAndDefaultIsTheSame(stringClusterDefinitionEntry, clusterDefinition)) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                diff.put(stringClusterDefinitionEntry.getKey(), stringClusterDefinitionEntry.getValue());
            }
        }
        LOGGER.debug("Finished to collect the default cluster definitions which are missing: {}.", diff);
        return diff;
    }

    private boolean isRegisteregClusterDefinitionAndDefaultIsTheSame(Entry<String, ClusterDefinition> stringClusterDefinitionEntry,
            ClusterDefinition clusterDefinition) {
        return clusterDefinition.getName().equals(stringClusterDefinitionEntry.getKey()) && isDefaultClusterDefinition(clusterDefinition);
    }

    private boolean isDefaultClusterDefinition(ClusterDefinition cd) {
        return DEFAULT.equals(cd.getStatus());
    }

    private boolean defaultClusterDefinitionNotSameAsNewTexts(ClusterDefinition clusterDefinitionFromDatabase, String clusterDefinitionsText) {
        String clusterDefinitionText = clusterDefinitionFromDatabase.getClusterDefinitionText();
        return clusterDefinitionText == null || !clusterDefinitionText.equals(clusterDefinitionsText);
    }

    private boolean defaultClusterDefinitionContainsNewDescription(ClusterDefinition cd, ClusterDefinition clusterDefinition) {
        return !cd.getDescription().equals(clusterDefinition.getDescription());
    }

    private boolean isNewUserOrDeletedEveryDefaultClusterDefinition(Collection<ClusterDefinition> clusterDefinitions) {
        return clusterDefinitions.isEmpty();
    }

    private boolean mustUpdateTheExistingClusterDefinition(ClusterDefinition clusterDefinitionFromDatabase, ClusterDefinition defaultClusterDefinition) {
        return isDefaultClusterDefinition(clusterDefinitionFromDatabase)
                && defaultClusterDefinitionExistInTheCache(defaultClusterDefinition)
                && (defaultClusterDefinitionNotSameAsNewTexts(clusterDefinitionFromDatabase, defaultClusterDefinition.getClusterDefinitionText())
                || defaultClusterDefinitionContainsNewDescription(clusterDefinitionFromDatabase, defaultClusterDefinition));
    }

    private boolean defaultClusterDefinitionDoesNotExistInTheDatabase(Iterable<ClusterDefinition> clusterDefinitions) {
        return !collectDeviationOfExistingAndDefaultClusterDefinitions(clusterDefinitions).isEmpty();
    }

    private boolean defaultClusterDefinitionExistInTheCache(ClusterDefinition actualDefaultClusterDefinition) {
        return actualDefaultClusterDefinition != null;
    }
}
