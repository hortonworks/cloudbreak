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
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;

@Service
public class BlueprintLoaderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintLoaderService.class);

    @Inject
    private DefaultBlueprintCache defaultBlueprintCache;

    public boolean isAddingDefaultBlueprintsNecessaryForTheUser(Collection<Blueprint> blueprints) {
        Map<String, Blueprint> defaultBlueprints = defaultBlueprintCache.defaultBlueprints();
        for (Blueprint blueprintFromDatabase : blueprints) {
            Blueprint defaultBlueprint = defaultBlueprints.get(blueprintFromDatabase.getName());
            if (mustUpdateTheExistingBlueprint(blueprintFromDatabase, defaultBlueprint)) {
                return true;
            }
        }
        if (isNewUserOrDeletedEveryDefaultBlueprint(blueprints)) {
            return true;
        }
        return defaultBlueprintDoesNotExistInTheDatabase(blueprints);
    }

    public Set<Blueprint> loadBlueprintsForTheWorkspace(Set<Blueprint> blueprintsInDatabase, Workspace workspace,
            BiFunction<Iterable<Blueprint>, Workspace, Iterable<Blueprint>> saveMethod) {
        Set<Blueprint> blueprintsWhichShouldBeUpdated = updateDefaultBlueprints(blueprintsInDatabase, workspace);
        Set<Blueprint> blueprintsWhichAreMissing = addMissingBlueprints(blueprintsInDatabase, workspace);
        try {
            blueprintsWhichAreMissing.addAll(blueprintsWhichShouldBeUpdated);
            if (!blueprintsWhichAreMissing.isEmpty()) {
                return Sets.newHashSet(getResultSetFromUpdateAndOriginalBlueprints(blueprintsInDatabase, blueprintsWhichAreMissing, workspace,
                        saveMethod));
            }
        } catch (Exception e) {
            LOGGER.info("Cluster definitions {} is not available for {} workspace.", collectNames(blueprintsWhichAreMissing), workspace.getId());
        }
        return blueprintsInDatabase;
    }

    private Iterable<Blueprint> getResultSetFromUpdateAndOriginalBlueprints(Collection<Blueprint> blueprints,
            Iterable<Blueprint> blueprintsWhichAreMissing, Workspace workspace, BiFunction<Iterable<Blueprint>, Workspace,
            Iterable<Blueprint>> saveMethod) {
        LOGGER.debug("Updating blueprints which should be modified.");
        Iterable<Blueprint> savedBlueprints = saveMethod.apply(blueprintsWhichAreMissing, workspace);
        LOGGER.debug("Finished to update blueprints which should be modified.");
        Map<String, Blueprint> resultBlueprints = new HashMap<>();
        for (Blueprint blueprint : blueprints.stream().filter(bp -> DEFAULT.equals(bp.getStatus())).collect(Collectors.toSet())) {
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

    private Set<Blueprint> addMissingBlueprints(Collection<Blueprint> blueprintsInDatabase, Workspace workspace) {
        Set<Blueprint> resultList = new HashSet<>();
        LOGGER.debug("Adding default blueprints which are missing for the user.");
        for (Entry<String, Blueprint> diffBlueprint : collectDeviationOfExistingAndDefaultBlueprints(blueprintsInDatabase).entrySet()) {
            LOGGER.debug("Default blueprint '{}' needs to be added for the '{}' workspace because the default validation missing.",
                    diffBlueprint.getKey(), workspace.getId());
            resultList.add(setupBlueprint(diffBlueprint.getValue(), workspace));
        }
        LOGGER.debug("Finished to add default blueprints which are missing for the user.");
        return resultList;
    }

    private Set<Blueprint> updateDefaultBlueprints(Iterable<Blueprint> blueprintsInDatabase, Workspace workspace) {
        Set<Blueprint> resultList = new HashSet<>();
        LOGGER.debug("Updating default blueprints which are contains text modifications.");
        Map<String, Blueprint> defaultBlueprints = defaultBlueprintCache.defaultBlueprints();
        for (Blueprint blueprintInDatabase : blueprintsInDatabase) {
            Blueprint defaultBlueprint = defaultBlueprints.get(blueprintInDatabase.getName());
            if (isActiveDefaultBlueprint(blueprintInDatabase) && isBlueprintInTheDefaultCache(defaultBlueprint)
                    && (defaultBlueprintNotSameAsNewTexts(blueprintInDatabase, defaultBlueprint.getBlueprintText())
                    || defaultBlueprintContainsNewDescription(blueprintInDatabase, defaultBlueprint))) {
                LOGGER.debug("Default blueprint '{}' needs to modify for the '{}' workspace because the validation text changed.",
                        blueprintInDatabase.getName(), workspace.getId());
                resultList.add(prepareBlueprint(blueprintInDatabase, defaultBlueprint, workspace));
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

    private Map<String, Blueprint> collectDeviationOfExistingAndDefaultBlueprints(Collection<Blueprint> blueprintsInDatabase) {
        LOGGER.debug("Collecting blueprints which are missing from the defaults.");
        Map<String, Blueprint> diff = new HashMap<>();
        for (Entry<String, Blueprint> stringBlueprintEntry : defaultBlueprintCache.defaultBlueprints().entrySet()) {
            if (blueprintsInDatabase.stream().noneMatch(bp -> bp.getName().equals(stringBlueprintEntry.getKey()))) {
                diff.put(stringBlueprintEntry.getKey(), stringBlueprintEntry.getValue());
            }
        }
        LOGGER.debug("Finished to collect the default blueprints which are missing: {}.", diff);
        return diff;
    }

    private boolean isActiveDefaultBlueprint(Blueprint bp) {
        return DEFAULT.equals(bp.getStatus());
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

    private boolean mustUpdateTheExistingBlueprint(Blueprint blueprintFromDatabase, Blueprint defaultBlueprint) {
        return isActiveDefaultBlueprint(blueprintFromDatabase)
                && isBlueprintInTheDefaultCache(defaultBlueprint)
                && (defaultBlueprintNotSameAsNewTexts(blueprintFromDatabase, defaultBlueprint.getBlueprintText())
                || defaultBlueprintContainsNewDescription(blueprintFromDatabase, defaultBlueprint));
    }

    private boolean defaultBlueprintDoesNotExistInTheDatabase(Collection<Blueprint> blueprints) {
        return !collectDeviationOfExistingAndDefaultBlueprints(blueprints).isEmpty();
    }

    private boolean isBlueprintInTheDefaultCache(Blueprint actualDefaultBlueprint) {
        return actualDefaultBlueprint != null;
    }
}
