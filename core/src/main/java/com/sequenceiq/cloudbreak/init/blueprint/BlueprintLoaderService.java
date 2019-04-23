package com.sequenceiq.cloudbreak.init.blueprint;

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
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintArchived;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;

@Service
public class BlueprintLoaderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintLoaderService.class);

    @Inject
    private DefaultBlueprintCache defaultBlueprintCache;

    public boolean isAddingDefaultBlueprintsNecessaryForTheUser(Collection<Blueprint> defaultsActive, Collection<BlueprintArchived> defaultsDeleted) {
        Map<String, Blueprint> defaultBlueprints = defaultBlueprintCache.defaultBlueprints();
        for (Blueprint blueprintFromDatabase : defaultsActive) {
            Blueprint defaultBlueprint = defaultBlueprints.get(blueprintFromDatabase.getName());
            if (mustUpdateTheExistingBlueprint(blueprintFromDatabase, defaultBlueprint)) {
                return true;
            }
        }
        if (isNewUserOrDeletedEveryDefaultBlueprint(defaultsActive)) {
            return true;
        }
        return isDefaultBlueprintPresentInTheDatabase(defaultsActive, defaultsDeleted);
    }

    public Set<Blueprint> loadBlueprintsForTheWorkspace(Set<Blueprint> defaultsActive, Collection<BlueprintArchived> defaultsDeleted,
            Workspace workspace, BiFunction<Iterable<Blueprint>, Workspace, Iterable<Blueprint>> saveMethod) {
        Set<Blueprint> blueprintsWhichShouldBeUpdated = updateDefaultBlueprints(defaultsActive, workspace);
        Set<Blueprint> blueprintsWhichAreMissing = addMissingBlueprints(defaultsActive, defaultsDeleted, workspace);
        try {
            blueprintsWhichAreMissing.addAll(blueprintsWhichShouldBeUpdated);
            if (!blueprintsWhichAreMissing.isEmpty()) {
                return Sets.newHashSet(getResultSetFromUpdateAndOriginalBlueprints(defaultsActive, blueprintsWhichAreMissing, workspace,
                        saveMethod));
            }
        } catch (Exception e) {
            LOGGER.info("Cluster definitions {} is not available for {} workspace.", collectNames(blueprintsWhichAreMissing), workspace.getId());
        }
        return defaultsActive;
    }

    private Iterable<Blueprint> getResultSetFromUpdateAndOriginalBlueprints(Collection<Blueprint> blueprintsUnchangedFromDatabase,
            Iterable<Blueprint> blueprintsToChangeOrAdd, Workspace workspace, BiFunction<Iterable<Blueprint>, Workspace,
            Iterable<Blueprint>> saveMethod) {
        LOGGER.debug("Updating blueprints which should be modified.");
        Iterable<Blueprint> savedBlueprints = saveMethod.apply(blueprintsToChangeOrAdd, workspace);
        LOGGER.debug("Finished to update blueprints which should be modified.");
        Map<String, Blueprint> resultBlueprints = new HashMap<>();
        for (Blueprint blueprint : blueprintsUnchangedFromDatabase) {
            resultBlueprints.put(blueprint.getName(), blueprint);
        }
        for (Blueprint savedBlueprint : savedBlueprints) {
            resultBlueprints.put(savedBlueprint.getName(), savedBlueprint);
        }
        return resultBlueprints.values();
    }

    private Set<String> collectNames(Collection<Blueprint> failedToUpdate) {
        return failedToUpdate.stream().map(Blueprint::getName).collect(Collectors.toSet());
    }

    private Set<Blueprint> addMissingBlueprints(Collection<Blueprint> defaultsActive, Collection<BlueprintArchived> defaultsDeleted, Workspace workspace) {
        Set<Blueprint> resultList = new HashSet<>();
        LOGGER.debug("Adding default blueprints which are missing for the user.");
        for (Entry<String, Blueprint> diffBlueprint : collectDeviationOfExistingAndDefaultBlueprints(defaultsActive, defaultsDeleted).entrySet()) {
            LOGGER.debug("Default blueprint '{}' needs to be added for the '{}' workspace because the default validation missing.",
                    diffBlueprint.getKey(), workspace.getId());
            resultList.add(setupBlueprint(diffBlueprint.getValue(), workspace));
        }
        LOGGER.debug("Finished to add default blueprints which are missing for the user.");
        return resultList;
    }

    private Set<Blueprint> updateDefaultBlueprints(Iterable<Blueprint> defaultsActive, Workspace workspace) {
        Set<Blueprint> resultList = new HashSet<>();
        LOGGER.debug("Updating default blueprints which are contains text modifications.");
        Map<String, Blueprint> defaultBlueprints = defaultBlueprintCache.defaultBlueprints();
        for (Blueprint blueprintActiveInDatabase : defaultsActive) {
            Blueprint defaultBlueprint = defaultBlueprints.get(blueprintActiveInDatabase.getName());
            if (isBlueprintInTheDefaultCache(defaultBlueprint)
                    && (defaultBlueprintNotSameAsNewTexts(blueprintActiveInDatabase, defaultBlueprint.getBlueprintText())
                    || defaultBlueprintContainsNewDescription(blueprintActiveInDatabase, defaultBlueprint))) {
                LOGGER.debug("Default blueprint '{}' needs to modify for the '{}' workspace because the validation text changed.",
                        blueprintActiveInDatabase.getName(), workspace.getId());
                resultList.add(prepareBlueprint(blueprintActiveInDatabase, defaultBlueprint, workspace));
            }
        }
        LOGGER.debug("Finished to Update default blueprints which are contains text modifications.");
        return resultList;
    }

    private Blueprint prepareBlueprint(Blueprint blueprintFromDatabase, Blueprint newBlueprint,
            Workspace workspace) {
        setupBlueprint(blueprintFromDatabase, workspace);
        blueprintFromDatabase.setBlueprintText(newBlueprint.getBlueprintText());
        blueprintFromDatabase.setDescription(newBlueprint.getDescription());
        blueprintFromDatabase.setHostGroupCount(newBlueprint.getHostGroupCount());
        blueprintFromDatabase.setStackName(newBlueprint.getStackName());
        blueprintFromDatabase.setStackType(newBlueprint.getStackType());
        blueprintFromDatabase.setStackVersion(newBlueprint.getStackVersion());
        return blueprintFromDatabase;
    }

    private Blueprint setupBlueprint(Blueprint blueprint, Workspace workspace) {
        blueprint.setWorkspace(workspace);
        blueprint.setStatus(DEFAULT);
        return blueprint;
    }

    private Map<String, Blueprint> collectDeviationOfExistingAndDefaultBlueprints(Collection<Blueprint> defaultsActive,
            Collection<BlueprintArchived> defaultsDeleted) {
        LOGGER.debug("Collecting blueprints which are missing from the defaults.");
        Map<String, Blueprint> diff = new HashMap<>();
        for (Entry<String, Blueprint> stringBlueprintEntry : defaultBlueprintCache.defaultBlueprints().entrySet()) {
            if (defaultsDeleted.stream().noneMatch(bp -> bp.getName().equals(stringBlueprintEntry.getKey()))
                    && defaultsActive.stream().noneMatch(bp -> bp.getName().equals(stringBlueprintEntry.getKey()))) {
                diff.put(stringBlueprintEntry.getKey(), stringBlueprintEntry.getValue());
            }
        }
        LOGGER.debug("Finished to collect the default blueprints which are missing: {}.", diff);
        return diff;
    }

    private boolean defaultBlueprintNotSameAsNewTexts(Blueprint blueprintFromDatabase, String blueprintsText) {
        String blueprintText = blueprintFromDatabase.getBlueprintText();
        return blueprintText == null || !blueprintText.equals(blueprintsText);
    }

    private boolean defaultBlueprintContainsNewDescription(Blueprint cd, Blueprint blueprint) {
        return !cd.getDescription().equals(blueprint.getDescription());
    }

    private boolean isNewUserOrDeletedEveryDefaultBlueprint(Collection<Blueprint> blueprints) {
        return blueprints.isEmpty();
    }

    private boolean mustUpdateTheExistingBlueprint(Blueprint blueprintsActiveFromDatabase, Blueprint defaultBlueprint) {
        return isBlueprintInTheDefaultCache(defaultBlueprint)
                && (defaultBlueprintNotSameAsNewTexts(blueprintsActiveFromDatabase, defaultBlueprint.getBlueprintText())
                || defaultBlueprintContainsNewDescription(blueprintsActiveFromDatabase, defaultBlueprint));
    }

    private boolean isDefaultBlueprintPresentInTheDatabase(Collection<Blueprint> defaults, Collection<BlueprintArchived> defaultsDeleted) {
        return !collectDeviationOfExistingAndDefaultBlueprints(defaults, defaultsDeleted).isEmpty();
    }

    private boolean isBlueprintInTheDefaultCache(Blueprint actualDefaultBlueprint) {
        return actualDefaultBlueprint != null;
    }
}
