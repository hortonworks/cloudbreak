package com.sequenceiq.cloudbreak.init.blueprint;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT_DELETED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.USER_MANAGED;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintFile;
import com.sequenceiq.cloudbreak.domain.BlueprintHybridOption;
import com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintListFilters;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.template.ClusterTemplateService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Service
public class BlueprintLoaderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintLoaderService.class);

    @Inject
    private DefaultBlueprintCache defaultBlueprintCache;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private ClusterTemplateService clusterTemplateService;

    @Inject
    private BlueprintListFilters blueprintListFilters;

    @Inject
    private EntitlementService entitlementService;

    public boolean isAddingDefaultBlueprintsNecessaryForTheUser(Collection<Blueprint> blueprints) {
        for (Blueprint blueprintFromDatabase : blueprints) {
            BlueprintFile defaultBlueprint = defaultBlueprintCache.defaultBlueprints().get(blueprintFromDatabase.getName());
            if (mustUpdateTheExistingBlueprint(blueprintFromDatabase, defaultBlueprint)) {
                return true;
            }
        }
        if (isNewUserOrDeletedEveryDefaultBlueprint(blueprints)) {
            return true;
        }
        return defaultBlueprintDoesNotExistInTheDatabase(blueprints);
    }

    @Measure(BlueprintLoaderService.class)
    public Set<Blueprint> loadBlueprintsForTheWorkspace(Set<Blueprint> blueprintsInDatabase, Workspace workspace,
            BiFunction<Iterable<Blueprint>, Workspace, Iterable<Blueprint>> saveMethod) {
        Set<Blueprint> blueprintsWhichShouldBeUpdated = updateDefaultBlueprints(blueprintsInDatabase, workspace);
        Set<Blueprint> blueprintsWhichAreMissing = addMissingBlueprints(blueprintsInDatabase, workspace);
        try {
            blueprintsWhichAreMissing.addAll(blueprintsWhichShouldBeUpdated);
            deleteOldDefaults(blueprintsInDatabase);
            if (!blueprintsWhichAreMissing.isEmpty()) {
                return Sets.newHashSet(getResultSetFromUpdateAndOriginalBlueprints(blueprintsInDatabase, blueprintsWhichAreMissing, workspace,
                        saveMethod));
            }
        } catch (Exception e) {
            LOGGER.info("Cluster definitions {} is not available for {} workspace.", collectNames(blueprintsWhichAreMissing), workspace.getId());
        }
        return blueprintsInDatabase;
    }

    public void deleteOldDefaults(Set<Blueprint> blueprintsInDatabase) {
        List<Blueprint> deletableDefaults = blueprintsInDatabase.stream()
                .filter(blueprint -> blueprint.getStatus().equals(DEFAULT))
                .filter(blueprint -> !defaultBlueprintCache.defaultBlueprints().containsKey(blueprint.getName()))
                .filter(blueprint -> clusterTemplateService.getTemplatesByBlueprint(blueprint).isEmpty())
                .collect(Collectors.toList());

        LOGGER.info("Put old default blueprints to DEFAULT_DELETED: " + deletableDefaults);

        for (Blueprint blueprint : deletableDefaults) {
            blueprint.setStatus(DEFAULT_DELETED);
        }

        blueprintService.pureSaveAll(deletableDefaults);
    }

    public boolean defaultBlueprintDoesNotExistInTheCache(Collection<Blueprint> blueprints) {
        return !collectDeviationOfExistingAndCachedBlueprints(blueprints).isEmpty();
    }

    private Iterable<Blueprint> getResultSetFromUpdateAndOriginalBlueprints(Collection<Blueprint> blueprints,
            Iterable<Blueprint> blueprintsWhichAreMissing, Workspace workspace, BiFunction<Iterable<Blueprint>, Workspace, Iterable<Blueprint>> saveMethod) {
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
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        for (Entry<String, BlueprintFile> diffBlueprint : collectDeviationOfExistingAndDefaultBlueprints(blueprintsInDatabase).entrySet()) {
            if (blueprintListFilters.isLakehouseOptimizer(diffBlueprint.getValue()) && !entitlementService.isLakehouseOptimizerEnabled(accountId)) {
                LOGGER.info("Lakehouse Optimizer blueprints are not enabled for workspace '{}', therefore not adding blueprint '{}' to the database.",
                        workspace.getId(), diffBlueprint.getKey());
                continue;
            }
            if (!BlueprintHybridOption.isNone(diffBlueprint.getValue().getHybridOption()) && !entitlementService.hybridCloudEnabled(accountId)) {
                LOGGER.info("Hybrid cloud blueprints are not enabled for workspace '{}', therefore not adding blueprint '{}' to the database.",
                        workspace.getId(), diffBlueprint.getKey());
                continue;
            }
            LOGGER.debug("Default blueprint '{}' needs to be added for the '{}' workspace because the default validation missing.",
                    diffBlueprint.getKey(), workspace.getId());
            Blueprint bp = new Blueprint();
            prepareBlueprint(bp, diffBlueprint.getValue(), workspace);
            bp.setName(diffBlueprint.getValue().getName());
            bp = setupBlueprint(bp, workspace);
            blueprintService.decorateWithCrn(bp, accountId);
            resultList.add(bp);
        }

        LOGGER.debug("Finished to add default blueprints which are missing for the user.");
        return resultList;
    }

    private Set<Blueprint> updateDefaultBlueprints(Iterable<Blueprint> blueprintsInDatabase, Workspace workspace) {
        Set<Blueprint> resultList = new HashSet<>();
        LOGGER.debug("Updating default blueprints which are contains text modifications.");
        for (Blueprint blueprintInDatabase : blueprintsInDatabase) {
            BlueprintFile defaultBlueprint = defaultBlueprintCache.defaultBlueprints().get(blueprintInDatabase.getName());
            if (isActiveBlueprintMustBeUpdatedAndNotUserManaged(blueprintInDatabase, defaultBlueprint)
                    || isNotActiveAndMustComeBack(blueprintInDatabase, defaultBlueprint)) {
                LOGGER.debug("Default blueprint '{}' needs to modify for the '{}' workspace because the validation text changed.",
                        blueprintInDatabase.getName(), workspace.getId());
                resultList.add(prepareBlueprint(blueprintInDatabase, defaultBlueprint, workspace));
            }
        }
        LOGGER.debug("Finished to Update default blueprints which are contains text modifications.");
        return resultList;
    }

    private boolean isActiveBlueprintMustBeUpdatedAndNotUserManaged(Blueprint blueprintInDatabase, BlueprintFile defaultBlueprint) {
        return isActiveDefaultBlueprint(blueprintInDatabase)
                && isBlueprintInTheDefaultCache(defaultBlueprint)
                && (defaultBlueprintsDefaultBlueprintTextNotSameAsNew(blueprintInDatabase, defaultBlueprint.getDefaultBlueprintText())
                        || defaultBlueprintContainsNewDescription(blueprintInDatabase, defaultBlueprint)
                        || isBlueprintInDBSameNameButUserManaged(blueprintInDatabase, defaultBlueprint)
                        || isUpgradeOptionModified(blueprintInDatabase, defaultBlueprint)
                        || isHybridOptionModified(blueprintInDatabase, defaultBlueprint));
    }

    private boolean isHybridOptionModified(Blueprint blueprintInDatabase, BlueprintFile defaultBlueprint) {
        return blueprintInDatabase.getHybridOption() != defaultBlueprint.getHybridOption();
    }

    private boolean isUpgradeOptionModified(Blueprint blueprintInDatabase, BlueprintFile defaultBlueprint) {
        return blueprintInDatabase.getBlueprintUpgradeOption() != defaultBlueprint.getBlueprintUpgradeOption();
    }

    private boolean isBlueprintInDBSameNameButUserManaged(Blueprint blueprintInDatabase, BlueprintFile defaultBlueprint) {
        return blueprintInDatabase.getName().equals(defaultBlueprint.getName()) && blueprintInDatabase.getStatus() == USER_MANAGED;
    }

    private Blueprint prepareBlueprint(Blueprint blueprintFromDatabase, BlueprintFile newBlueprint,
            Workspace workspace) {
        setupBlueprint(blueprintFromDatabase, workspace);
        blueprintFromDatabase.setDefaultBlueprintText(newBlueprint.getBlueprintText());
        blueprintFromDatabase.setBlueprintTextToBlankIfDefaultTextIsPresent(newBlueprint.getBlueprintText());
        blueprintFromDatabase.setDescription(newBlueprint.getDescription());
        blueprintFromDatabase.setHostGroupCount(newBlueprint.getHostGroupCount());
        blueprintFromDatabase.setStackName(newBlueprint.getStackName());
        blueprintFromDatabase.setStackType(newBlueprint.getStackType());
        blueprintFromDatabase.setStackVersion(newBlueprint.getStackVersion());
        blueprintFromDatabase.setBlueprintUpgradeOption(getBlueprintUpgradeOption(newBlueprint));
        blueprintFromDatabase.setHybridOption(newBlueprint.getHybridOption());
        blueprintFromDatabase.setLastUpdated(Instant.now().toEpochMilli());
        blueprintFromDatabase.setTags(newBlueprint.getTags());
        return blueprintFromDatabase;
    }

    private Blueprint setupBlueprint(Blueprint blueprint, Workspace workspace) {
        blueprint.setWorkspace(workspace);
        blueprint.setStatus(DEFAULT);
        return blueprint;
    }

    private BlueprintUpgradeOption getBlueprintUpgradeOption(BlueprintFile blueprint) {
        return Optional.ofNullable(blueprint.getBlueprintUpgradeOption())
                .orElse(BlueprintUpgradeOption.ENABLED);
    }

    private Map<String, BlueprintFile> collectDeviationOfExistingAndDefaultBlueprints(Collection<Blueprint> blueprintsInDatabase) {
        LOGGER.debug("Collecting blueprints which are missing from the defaults.");
        Map<String, BlueprintFile> diff = new HashMap<>();
        for (Entry<String, BlueprintFile> stringBlueprintEntry : defaultBlueprintCache.defaultBlueprints().entrySet()) {
            if (blueprintsInDatabase
                    .stream()
                    .noneMatch(bp -> bp.getName().equals(stringBlueprintEntry.getKey()))) {
                diff.put(stringBlueprintEntry.getKey(), stringBlueprintEntry.getValue());
            }
        }
        LOGGER.debug("Finished to collect the default blueprints which are missing: {}.", diff);
        return diff;
    }

    private Set<Blueprint> collectDeviationOfExistingAndCachedBlueprints(Collection<Blueprint> blueprintsInDatabase) {
        LOGGER.debug("Collecting blueprints which are missing from the cache.");
        Set<Blueprint> diff = new HashSet<>();
        for (Blueprint blueprint : blueprintsInDatabase) {
            if (blueprint.getStatus().equals(DEFAULT) && !defaultBlueprintCache.defaultBlueprints().containsKey(blueprint.getName())) {
                diff.add(blueprint);
            }
        }
        LOGGER.debug("Finished to collect the default blueprints which are missing: {}.", diff);
        return diff;
    }

    private boolean isActiveDefaultBlueprint(Blueprint bp) {
        return DEFAULT.equals(bp.getStatus()) || USER_MANAGED.equals(bp.getStatus());
    }

    private boolean defaultBlueprintsDefaultBlueprintTextNotSameAsNew(Blueprint blueprintInDatabase, String defaultBlueprintText) {
        ResourceStatus blueprintInDatabaseStatus = blueprintInDatabase.getStatus();
        if (blueprintInDatabaseStatus == DEFAULT || blueprintInDatabaseStatus == DEFAULT_DELETED) {
            return !defaultBlueprintText.equals(blueprintInDatabase.getDefaultBlueprintText());
        }
        return false;
    }

    private boolean defaultBlueprintContainsNewDescription(Blueprint cd, BlueprintFile blueprint) {
        return !cd.getDescription().equals(blueprint.getDescription());
    }

    private boolean defaultBlueprintDefaultDeleted(Blueprint cd, BlueprintFile blueprint) {
        return cd.getStatus().equals(DEFAULT_DELETED);
    }

    private boolean isNewUserOrDeletedEveryDefaultBlueprint(Collection<Blueprint> blueprints) {
        return blueprints.isEmpty();
    }

    private boolean mustUpdateTheExistingBlueprint(Blueprint blueprintFromDatabase, BlueprintFile defaultBlueprint) {
        return isActiveAndMustBeUpdated(blueprintFromDatabase, defaultBlueprint)
                || isNotActiveAndMustComeBack(blueprintFromDatabase, defaultBlueprint);
    }

    private boolean isNotActiveAndMustComeBack(Blueprint blueprintFromDatabase, BlueprintFile defaultBlueprint) {
        return defaultBlueprintDefaultDeleted(blueprintFromDatabase, defaultBlueprint)
                && isBlueprintInTheDefaultCache(defaultBlueprint);
    }

    private boolean isActiveAndMustBeUpdated(Blueprint blueprintFromDatabase, BlueprintFile defaultBlueprint) {
        return isActiveDefaultBlueprint(blueprintFromDatabase)
                && isBlueprintInTheDefaultCache(defaultBlueprint)
                && (defaultBlueprintsDefaultBlueprintTextNotSameAsNew(blueprintFromDatabase, defaultBlueprint.getDefaultBlueprintText())
                        || defaultBlueprintContainsNewDescription(blueprintFromDatabase, defaultBlueprint)
                        || isUpgradeOptionModified(blueprintFromDatabase, defaultBlueprint)
                        || isHybridOptionModified(blueprintFromDatabase, defaultBlueprint));
    }

    private boolean defaultBlueprintDoesNotExistInTheDatabase(Collection<Blueprint> blueprints) {
        return !collectDeviationOfExistingAndDefaultBlueprints(blueprints).isEmpty();
    }

    private boolean isBlueprintInTheDefaultCache(BlueprintFile actualDefaultBlueprint) {
        return actualDefaultBlueprint != null;
    }
}
